package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.data.update.GitHubReleaseInfo
import com.example.data.update.UpdateState
import java.io.File
import java.util.Locale

@Composable
fun UpdateDialog(
    visible: Boolean,
    state: UpdateState,
    isBangla: Boolean,
    permissionNotice: String? = null,
    onUpdateNow: (GitHubReleaseInfo) -> Unit,
    onInstallNow: (File) -> Unit,
    onCancelDownload: (GitHubReleaseInfo?) -> Unit,
    onSkipVersion: (String) -> Unit,
    onRetryCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val showDialogForState = state is UpdateState.Available ||
        state is UpdateState.Downloading ||
        state is UpdateState.ReadyToInstall ||
        state is UpdateState.Error ||
        state is UpdateState.Checking
    if (!showDialogForState) return

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val dialogShape = RoundedCornerShape(28.dp)

    val glassSurfaceColor = if (isDark) {
        Color(0xFF141826).copy(alpha = 0.94f)
    } else {
        Color(0xFFFDFEFF).copy(alpha = 0.94f)
    }
    val glassOverlayBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.36f),
                primaryColor.copy(alpha = 0.04f)
            )
        )
    }
    val borderBrush = Brush.linearGradient(
        if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.24f),
                Color.White.copy(alpha = 0.08f),
                primaryColor.copy(alpha = 0.25f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.92f),
                Color.Black.copy(alpha = 0.06f),
                primaryColor.copy(alpha = 0.18f)
            )
        }
    )

    val canDismissOnBack = state !is UpdateState.Downloading

    Dialog(
        onDismissRequest = { if (canDismissOnBack) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = canDismissOnBack,
            dismissOnClickOutside = canDismissOnBack,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 440.dp)
                .testTag("update_dialog")
                .crystalOuterShadow(
                    shape = dialogShape,
                    color = if (isDark) Color.Black.copy(alpha = 0.42f) else Color(0x240F172A),
                    blurSpread = 16.dp,
                    offsetY = 6.dp
                )
                .clip(dialogShape)
                .background(glassSurfaceColor)
                .background(glassOverlayBrush)
                .border(BorderStroke(1.dp, borderBrush), dialogShape)
        ) {
            when (state) {
                is UpdateState.Checking -> CheckingContent(isBangla = isBangla, onDismiss = onDismiss)
                is UpdateState.Available -> AvailableContent(
                    releaseInfo = state.releaseInfo,
                    isBangla = isBangla,
                    isDark = isDark,
                    onUpdateNow = { onUpdateNow(state.releaseInfo) },
                    onSkipVersion = { onSkipVersion(state.releaseInfo.versionName) },
                    onDismiss = onDismiss
                )
                is UpdateState.Downloading -> DownloadingContent(
                    state = state,
                    isBangla = isBangla,
                    isDark = isDark,
                    onCancel = { onCancelDownload(state.releaseInfo) }
                )
                is UpdateState.ReadyToInstall -> ReadyToInstallContent(
                    state = state,
                    isBangla = isBangla,
                    isDark = isDark,
                    permissionNotice = permissionNotice,
                    onInstallNow = { onInstallNow(state.apkFile) },
                    onDismiss = onDismiss
                )
                is UpdateState.Error -> ErrorContent(
                    message = state.message,
                    isBangla = isBangla,
                    isDark = isDark,
                    onRetry = onRetryCheck,
                    onDismiss = onDismiss
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun CheckingContent(
    isBangla: Boolean,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isBangla) "আপডেট খোঁজা হচ্ছে..." else "Checking for updates...",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isBangla) "সর্বশেষ রিলিজ যাচাই করা হচ্ছে" else "Querying GitHub Releases for Hishab Pati",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("update_close_button")
        ) {
            Text(if (isBangla) "লুকান" else "Hide")
        }
    }
}

@Composable
private fun AvailableContent(
    releaseInfo: GitHubReleaseInfo,
    isBangla: Boolean,
    isDark: Boolean,
    onUpdateNow: () -> Unit,
    onSkipVersion: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val notesScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = primary.copy(alpha = 0.14f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBangla) "নতুন আপডেট উপলব্ধ" else "Update Available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (releaseInfo.releaseName.isNotBlank() && releaseInfo.releaseName != releaseInfo.tagName) {
                    Text(
                        text = releaseInfo.releaseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version comparison + APK file size pill row
        VersionAndSizeBanner(
            currentVersion = BuildConfig.VERSION_NAME,
            newVersion = releaseInfo.versionName,
            apkSizeBytes = releaseInfo.apkSizeBytes,
            isDark = isDark
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isBangla) "নতুন যা যুক্ত হয়েছে" else "What's New",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        val notesContainerColor = if (isDark) {
            Color.White.copy(alpha = 0.05f)
        } else {
            Color.Black.copy(alpha = 0.035f)
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = notesContainerColor,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp, max = 200.dp)
        ) {
            Text(
                text = releaseInfo.releaseNotes.ifBlank {
                    if (isBangla) {
                        "স্থায়িত্ব ও পারফরম্যান্স উন্নতি সহ নতুন সংস্করণ।"
                    } else {
                        "Bug fixes and performance improvements."
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 19.sp,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .verticalScroll(notesScroll)
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_later_button")
            ) {
                Text(
                    text = if (isBangla) "পরে" else "Later",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onUpdateNow,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                modifier = Modifier
                    .weight(1.35f)
                    .height(48.dp)
                    .testTag("update_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "এখনই আপডেট" else "Update Now",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onSkipVersion,
                modifier = Modifier.testTag("update_skip_version_button")
            ) {
                Text(
                    text = if (isBangla) "এই সংস্করণ এড়িয়ে যান" else "Skip this version",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    state: UpdateState.Downloading,
    isBangla: Boolean,
    isDark: Boolean,
    onCancel: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        label = "apk_download_progress"
    )
    val percent = (state.progress.coerceIn(0f, 1f) * 100).toInt()
    val downloadedText = formatMb(state.downloadedBytes)
    val totalText = if (state.totalBytes > 0L) formatMb(state.totalBytes) else "—"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = primary.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBangla) "আপডেট ডাউনলোড হচ্ছে..." else "Downloading Update...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                state.releaseInfo?.let { info ->
                    Text(
                        text = "Hishab Pati v${info.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (state.totalBytes > 0L) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (state.totalBytes > 0L) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("update_progress_bar"),
                color = primary,
                trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else primary.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("update_progress_bar"),
                color = primary,
                trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else primary.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$downloadedText / $totalText",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isBangla) "অনুগ্রহ করে অপেক্ষা করুন" else "Please keep the app open",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("update_cancel_download_button")
        ) {
            Text(
                text = if (isBangla) "বাতিল করুন" else "Cancel",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReadyToInstallContent(
    state: UpdateState.ReadyToInstall,
    isBangla: Boolean,
    isDark: Boolean,
    permissionNotice: String?,
    onInstallNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val successColor = Color(0xFF10B981)
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = successColor.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = successColor,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBangla) "ইনস্টলের জন্য প্রস্তুত" else "Ready to Install",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = state.releaseInfo?.let {
                    if (isBangla) "সংস্করণ v${it.versionName} ডাউনলোড সম্পন্ন" else "Version v${it.versionName} downloaded"
                } ?: if (isBangla) "APK ফাইল ডাউনলোড সম্পন্ন হয়েছে" else "Update package downloaded"
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!permissionNotice.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.18f else 0.12f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = permissionNotice,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_later_button")
            ) {
                Text(
                    text = if (isBangla) "পরে" else "Later",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onInstallNow,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                modifier = Modifier
                    .weight(1.35f)
                    .height(48.dp)
                    .testTag("update_install_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.InstallMobile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "এখনই ইনস্টল" else "Install Now",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    isBangla: Boolean,
    isDark: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val errorColor = MaterialTheme.colorScheme.error
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = errorColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isBangla) "আপডেট ত্রুটি" else "Update Failed",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.035f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_close_button")
            ) {
                Text(
                    text = if (isBangla) "বন্ধ করুন" else "Close",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("update_retry_button")
            ) {
                Text(
                    text = if (isBangla) "আবার চেষ্টা" else "Retry",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun VersionAndSizeBanner(
    currentVersion: String,
    newVersion: String,
    apkSizeBytes: Long,
    isDark: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val bgColor = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        primary.copy(alpha = 0.06f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.09f) else primary.copy(alpha = 0.14f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "v$currentVersion",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "v$newVersion",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (apkSizeBytes > 0L) {
                Text(
                    text = formatMb(apkSizeBytes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 0.1) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes.toDouble() / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}
