package com.example.data.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkDownloadUrl: String,
    val apkAssetName: String,
    val apkSizeBytes: Long
) {
    fun toJsonString(): String {
        val obj = org.json.JSONObject()
        obj.put("tag_name", tagName)
        obj.put("name", releaseName)
        obj.put("body", releaseNotes)
        obj.put("published_at", publishedAt)
        val asset = org.json.JSONObject()
        asset.put("name", apkAssetName)
        asset.put("browser_download_url", apkDownloadUrl)
        asset.put("size", apkSizeBytes)
        val assetsArray = org.json.JSONArray()
        assetsArray.put(asset)
        obj.put("assets", assetsArray)
        return obj.toString()
    }
}

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class Available(val releaseInfo: GitHubReleaseInfo) : UpdateState()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val releaseInfo: GitHubReleaseInfo? = null
    ) : UpdateState()
    data class ReadyToInstall(
        val apkFile: File,
        val releaseInfo: GitHubReleaseInfo? = null
    ) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

enum class InstallApkResult {
    LAUNCHED,
    NEEDS_UNKNOWN_SOURCES_PERMISSION,
    FILE_MISSING,
    ERROR
}

class GitHubUpdateManager {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val downloadCancelled = AtomicBoolean(false)

    fun resetToIdle() {
        _updateState.value = UpdateState.Idle
    }

    fun setAvailable(releaseInfo: GitHubReleaseInfo) {
        _updateState.value = UpdateState.Available(releaseInfo)
    }

    fun cancelDownload(fallbackRelease: GitHubReleaseInfo? = null) {
        downloadCancelled.set(true)
        if (fallbackRelease != null) {
            _updateState.value = UpdateState.Available(fallbackRelease)
        } else {
            _updateState.value = UpdateState.Idle
        }
    }

    fun cleanupOldApks(context: Context, keepFileName: String? = null) {
        val updatesDir = File(context.cacheDir, UPDATES_DIR_NAME)
        if (!updatesDir.exists() || !updatesDir.isDirectory) return
        updatesDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".apk", ignoreCase = true) || file.name.endsWith(".tmp", ignoreCase = true))) {
                if (keepFileName == null || file.name != keepFileName) {
                    runCatching { file.delete() }
                }
            }
        }
    }

    suspend fun checkForUpdate(context: Context): UpdateState = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                cleanupOldApks(context)
                val upToDate = UpdateState.UpToDate
                _updateState.value = upToDate
                return@withContext upToDate
            }

            if (responseCode !in 200..299) {
                val errorMessage = "Unable to check for updates (HTTP $responseCode)."
                val errorState = UpdateState.Error(errorMessage)
                _updateState.value = errorState
                return@withContext errorState
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val releaseInfo = parseLatestRelease(body)
            if (releaseInfo == null) {
                cleanupOldApks(context)
                val upToDate = UpdateState.UpToDate
                _updateState.value = upToDate
                return@withContext upToDate
            }

            val nextState = if (isNewerVersion(releaseInfo.tagName, BuildConfig.VERSION_NAME)) {
                UpdateState.Available(releaseInfo)
            } else {
                cleanupOldApks(context)
                UpdateState.UpToDate
            }
            _updateState.value = nextState
            nextState
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val message = e.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Network error while checking for updates."
            val errorState = UpdateState.Error(message)
            _updateState.value = errorState
            errorState
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadApk(
        context: Context,
        releaseInfo: GitHubReleaseInfo
    ): File? = withContext(Dispatchers.IO) {
        downloadCancelled.set(false)
        cleanupOldApks(context)

        val updatesDir = File(context.cacheDir, UPDATES_DIR_NAME).apply { mkdirs() }
        val safeVersion = releaseInfo.versionName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "latest" }
        val targetFile = File(updatesDir, "HishabPati-$safeVersion.apk")
        val tempFile = File(updatesDir, "HishabPati-$safeVersion.apk.tmp")

        var connection: HttpURLConnection? = null
        try {
            _updateState.value = UpdateState.Downloading(
                progress = 0f,
                downloadedBytes = 0L,
                totalBytes = releaseInfo.apkSizeBytes.coerceAtLeast(0L),
                releaseInfo = releaseInfo
            )

            connection = openFollowingRedirects(releaseInfo.apkDownloadUrl)
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Download failed with HTTP $responseCode")
            }

            val headerLength = connection.contentLengthLong
            val totalBytes = when {
                headerLength > 0L -> headerLength
                releaseInfo.apkSizeBytes > 0L -> releaseInfo.apkSizeBytes
                else -> -1L
            }

            var downloadedBytes = 0L
            var lastEmittedBytes = 0L

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        ensureActive()
                        if (downloadCancelled.get()) {
                            throw CancellationException("Download cancelled by user")
                        }
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        if (downloadedBytes - lastEmittedBytes >= 64 * 1024 ||
                            (totalBytes > 0L && downloadedBytes >= totalBytes)
                        ) {
                            lastEmittedBytes = downloadedBytes
                            val progress = if (totalBytes > 0L) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            _updateState.value = UpdateState.Downloading(
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = if (totalBytes > 0L) totalBytes else downloadedBytes,
                                releaseInfo = releaseInfo
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (downloadCancelled.get()) {
                runCatching { tempFile.delete() }
                _updateState.value = UpdateState.Available(releaseInfo)
                return@withContext null
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            _updateState.value = UpdateState.ReadyToInstall(
                apkFile = targetFile,
                releaseInfo = releaseInfo
            )
            targetFile
        } catch (ce: CancellationException) {
            runCatching { tempFile.delete() }
            if (_updateState.value is UpdateState.Downloading) {
                _updateState.value = UpdateState.Available(releaseInfo)
            }
            if (!downloadCancelled.get()) throw ce
            null
        } catch (e: Exception) {
            runCatching { tempFile.delete() }
            _updateState.value = UpdateState.Error(
                e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Failed to download update."
            )
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun installApk(context: Context, apkFile: File): InstallApkResult {
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            _updateState.value = UpdateState.Error("Downloaded APK file is missing or incomplete.")
            return InstallApkResult.FILE_MISSING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return try {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(settingsIntent)
                InstallApkResult.NEEDS_UNKNOWN_SOURCES_PERMISSION
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(
                    e.localizedMessage ?: "Unable to open install permission settings."
                )
                InstallApkResult.ERROR
            }
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(installIntent)
            InstallApkResult.LAUNCHED
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(
                e.localizedMessage ?: "Unable to launch package installer."
            )
            InstallApkResult.ERROR
        }
    }

    private fun openFollowingRedirects(initialUrl: String): HttpURLConnection {
        var currentUrl = initialUrl
        repeat(MAX_REDIRECTS) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/octet-stream")
            }
            val code = conn.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308
                )
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect missing Location header")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while downloading update")
    }

    companion object {
        const val GITHUB_REPO = "khanistiak/Hishab-Pati"
        const val GITHUB_LATEST_RELEASE_URL =
            "https://api.github.com/repos/khanistiak/Hishab-Pati/releases/latest"
        private const val UPDATES_DIR_NAME = "updates"
        private const val USER_AGENT = "HishabPati-Android"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 30_000
        private const val MAX_REDIRECTS = 6

        fun normalizeVersionTag(tag: String): String {
            val trimmed = tag.trim()
            val withoutPrefix = if (trimmed.startsWith("v", ignoreCase = true)) {
                trimmed.substring(1)
            } else {
                trimmed
            }
            return withoutPrefix.substringBefore('-').substringBefore('+').trim()
        }

        internal fun parseSemanticVersion(tag: String): Triple<Int, Int, Int>? {
            val cleaned = normalizeVersionTag(tag)
            if (cleaned.isBlank()) return null
            val parts = cleaned.split('.')
            if (parts.isEmpty()) return null
            val numbers = parts.map { part ->
                part.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
            val major = numbers.getOrElse(0) { 0 }
            val minor = numbers.getOrElse(1) { 0 }
            val patch = numbers.getOrElse(2) { 0 }
            return Triple(major, minor, patch)
        }

        fun isNewerVersion(
            remoteTag: String,
            currentVersion: String = BuildConfig.VERSION_NAME
        ): Boolean {
            val remote = parseSemanticVersion(remoteTag)
            val current = parseSemanticVersion(currentVersion)
            if (remote != null && current != null) {
                if (remote.first != current.first) return remote.first > current.first
                if (remote.second != current.second) return remote.second > current.second
                if (remote.third != current.third) return remote.third > current.third
                return false
            }
            val remoteDigits = remoteTag.filter { it.isDigit() }.toIntOrNull() ?: 0
            val currentDigits = currentVersion.filter { it.isDigit() }.toIntOrNull() ?: 0
            return remoteDigits > currentDigits
        }

        @Suppress("UNCHECKED_CAST")
        fun parseLatestRelease(jsonString: String): GitHubReleaseInfo? {
            if (jsonString.isBlank()) return null
            val root = runCatching { SimpleJsonParser.parse(jsonString) as? Map<String, Any?> }
                .getOrNull() ?: return null

            val tagName = (root["tag_name"] as? String)?.trim().orEmpty()
            if (tagName.isBlank()) return null

            val releaseName = (root["name"] as? String)?.trim().takeUnless { it.isNullOrBlank() } ?: tagName
            val releaseNotes = (root["body"] as? String)?.trim().orEmpty()
            val publishedAt = (root["published_at"] as? String)?.trim().orEmpty()
            val assets = root["assets"] as? List<Any?> ?: emptyList()

            var matchedUrl = ""
            var matchedName = ""
            var matchedSize = 0L

            for (item in assets) {
                val assetMap = item as? Map<String, Any?> ?: continue
                val assetName = (assetMap["name"] as? String)?.trim().orEmpty()
                val downloadUrl = (assetMap["browser_download_url"] as? String)?.trim().orEmpty()
                if (assetName.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotBlank()) {
                    matchedName = assetName
                    matchedUrl = downloadUrl
                    matchedSize = (assetMap["size"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
                    break
                }
            }

            if (matchedUrl.isBlank()) return null

            val normalizedVersion = normalizeVersionTag(tagName).ifBlank { tagName }
            return GitHubReleaseInfo(
                tagName = tagName,
                versionName = normalizedVersion,
                releaseName = releaseName,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt,
                apkDownloadUrl = matchedUrl,
                apkAssetName = matchedName,
                apkSizeBytes = matchedSize
            )
        }
    }
}

/**
 * Pure-Kotlin JSON parser used for GitHub Releases API payloads so parsing and unit tests
 * work identically on Android devices and pure JVM test environments without Android stub issues.
 */
internal object SimpleJsonParser {

    fun parse(json: String): Any? {
        val parser = Cursor(json)
        parser.skipWhitespace()
        val value = parser.readValue()
        parser.skipWhitespace()
        return value
    }

    private class Cursor(private val input: String) {
        private var index = 0

        fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) {
                index++
            }
        }

        fun readValue(): Any? {
            skipWhitespace()
            if (index >= input.length) throw IllegalArgumentException("Unexpected end of JSON")
            return when (val ch = input[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't', 'f' -> readBoolean()
                'n' -> readNull()
                '-', in '0'..'9' -> readNumber()
                else -> throw IllegalArgumentException("Unexpected character '$ch' at $index")
            }
        }

        private fun readObject(): Map<String, Any?> {
            val result = LinkedHashMap<String, Any?>()
            index++ // consume '{'
            skipWhitespace()
            if (index < input.length && input[index] == '}') {
                index++
                return result
            }
            while (index < input.length) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                if (index >= input.length || input[index] != ':') {
                    throw IllegalArgumentException("Expected ':' at $index")
                }
                index++ // consume ':'
                val value = readValue()
                result[key] = value
                skipWhitespace()
                if (index < input.length && input[index] == ',') {
                    index++
                    continue
                }
                if (index < input.length && input[index] == '}') {
                    index++
                    return result
                }
                throw IllegalArgumentException("Expected ',' or '}' at $index")
            }
            throw IllegalArgumentException("Unterminated JSON object")
        }

        private fun readArray(): List<Any?> {
            val result = ArrayList<Any?>()
            index++ // consume '['
            skipWhitespace()
            if (index < input.length && input[index] == ']') {
                index++
                return result
            }
            while (index < input.length) {
                val value = readValue()
                result.add(value)
                skipWhitespace()
                if (index < input.length && input[index] == ',') {
                    index++
                    continue
                }
                if (index < input.length && input[index] == ']') {
                    index++
                    return result
                }
                throw IllegalArgumentException("Expected ',' or ']' at $index")
            }
            throw IllegalArgumentException("Unterminated JSON array")
        }

        private fun readString(): String {
            if (index >= input.length || input[index] != '"') {
                throw IllegalArgumentException("Expected '\"' at $index")
            }
            index++ // consume opening quote
            val sb = StringBuilder()
            while (index < input.length) {
                val ch = input[index++]
                if (ch == '"') return sb.toString()
                if (ch == '\\') {
                    if (index >= input.length) throw IllegalArgumentException("Unterminated escape")
                    when (val esc = input[index++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 > input.length) throw IllegalArgumentException("Invalid unicode escape")
                            val hex = input.substring(index, index + 4)
                            sb.append(hex.toInt(16).toChar())
                            index += 4
                        }
                        else -> throw IllegalArgumentException("Invalid escape '\\$esc'")
                    }
                } else {
                    sb.append(ch)
                }
            }
            throw IllegalArgumentException("Unterminated JSON string")
        }

        private fun readBoolean(): Boolean {
            return if (input.startsWith("true", index)) {
                index += 4
                true
            } else if (input.startsWith("false", index)) {
                index += 5
                false
            } else {
                throw IllegalArgumentException("Invalid boolean at $index")
            }
        }

        private fun readNull(): Any? {
            if (input.startsWith("null", index)) {
                index += 4
                return null
            }
            throw IllegalArgumentException("Invalid null at $index")
        }

        private fun readNumber(): Number {
            val start = index
            if (input[index] == '-') index++
            while (index < input.length && input[index].isDigit()) index++
            var isFloating = false
            if (index < input.length && input[index] == '.') {
                isFloating = true
                index++
                while (index < input.length && input[index].isDigit()) index++
            }
            if (index < input.length && (input[index] == 'e' || input[index] == 'E')) {
                isFloating = true
                index++
                if (index < input.length && (input[index] == '+' || input[index] == '-')) index++
                while (index < input.length && input[index].isDigit()) index++
            }
            val token = input.substring(start, index)
            return if (isFloating) token.toDouble() else token.toLong()
        }
    }
}
