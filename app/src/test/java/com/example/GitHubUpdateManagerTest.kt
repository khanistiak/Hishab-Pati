package com.example

import com.example.data.update.GitHubUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateManagerTest {

    @Test
    fun isNewerVersion_standardAndMultiDigitComparisons() {
        assertTrue(GitHubUpdateManager.isNewerVersion("v1.2.0", "1.1.9"))
        assertTrue(GitHubUpdateManager.isNewerVersion("v1.10.0", "v1.2.0"))
        assertTrue(GitHubUpdateManager.isNewerVersion("V2.0.0", "1.9.9"))
        assertTrue(GitHubUpdateManager.isNewerVersion("1.0.1", "1.0"))
        assertTrue(GitHubUpdateManager.isNewerVersion("v1.1", "1.0.5"))
    }

    @Test
    fun isNewerVersion_equalAndOlderVersionsReturnFalse() {
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.0", "1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.0", "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("1.1.9", "v1.2.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v0.9.9", "1.0.0"))
    }

    @Test
    fun isNewerVersion_malformedTagsReturnFalseSafely() {
        assertFalse(GitHubUpdateManager.isNewerVersion("", "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("latest", "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.a.0", "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.3.4", "1.0.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.2.0", "bad-version"))
    }

    @Test
    fun parseLatestRelease_withApkAsset_extractsAllFields() {
        val json = """
            {
              "tag_name": "v1.2.0",
              "name": "Hishab Pati v1.2.0",
              "body": "- Added GitHub in-app updates\n- Fixed \"glass\" contrast",
              "published_at": "2026-11-15T10:30:00Z",
              "assets": [
                {
                  "name": "checksums.sha256",
                  "size": 128,
                  "browser_download_url": "https://github.com/khanistiak/Hishab-Pati/releases/download/v1.2.0/checksums.sha256"
                },
                {
                  "name": "HishabPati-v1.2.0-release.apk",
                  "size": 24576000,
                  "browser_download_url": "https://github.com/khanistiak/Hishab-Pati/releases/download/v1.2.0/HishabPati-v1.2.0-release.apk"
                }
              ]
            }
        """.trimIndent()

        val release = GitHubUpdateManager.parseLatestRelease(json)
        assertNotNull(release)
        requireNotNull(release)
        assertEquals("v1.2.0", release.tagName)
        assertEquals("1.2.0", release.versionName)
        assertEquals("Hishab Pati v1.2.0", release.releaseName)
        assertEquals("- Added GitHub in-app updates\n- Fixed \"glass\" contrast", release.releaseNotes)
        assertEquals("2026-11-15T10:30:00Z", release.publishedAt)
        assertEquals("HishabPati-v1.2.0-release.apk", release.apkAssetName)
        assertEquals(24576000L, release.apkSizeBytes)
        assertEquals(
            "https://github.com/khanistiak/Hishab-Pati/releases/download/v1.2.0/HishabPati-v1.2.0-release.apk",
            release.apkDownloadUrl
        )
    }

    @Test
    fun parseLatestRelease_withoutApkAsset_returnsNull() {
        val jsonWithoutApk = """
            {
              "tag_name": "v1.2.0",
              "name": "Source Only Release",
              "body": "Source tarball only",
              "published_at": "2026-11-15T10:30:00Z",
              "assets": [
                {
                  "name": "source-code.zip",
                  "size": 500000,
                  "browser_download_url": "https://github.com/khanistiak/Hishab-Pati/releases/download/v1.2.0/source-code.zip"
                }
              ]
            }
        """.trimIndent()

        assertNull(GitHubUpdateManager.parseLatestRelease(jsonWithoutApk))
    }

    @Test
    fun parseLatestRelease_emptyAssetsOrMalformedJson_returnsNull() {
        val emptyAssetsJson = """
            {
              "tag_name": "v1.3.0",
              "name": "Empty Assets",
              "body": "No files attached",
              "assets": []
            }
        """.trimIndent()

        assertNull(GitHubUpdateManager.parseLatestRelease(emptyAssetsJson))
        assertNull(GitHubUpdateManager.parseLatestRelease(""))
        assertNull(GitHubUpdateManager.parseLatestRelease("{ invalid json"))
    }

    @Test
    fun appVersion_isUpdatedTo212_andDefaultComparatorUsesBuildConfig() {
        assertEquals("2.7.0", BuildConfig.VERSION_NAME)
        assertEquals(270, BuildConfig.VERSION_CODE)
        assertFalse(GitHubUpdateManager.isNewerVersion("v2.7.0"))
        assertFalse(GitHubUpdateManager.isNewerVersion("2.1.1"))
        assertFalse(GitHubUpdateManager.isNewerVersion("v1.0.0"))
        assertTrue(GitHubUpdateManager.isNewerVersion("v2.1.3"))
        assertTrue(GitHubUpdateManager.isNewerVersion("v2.2.0"))
    }
}
