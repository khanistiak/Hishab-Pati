package com.example.ui.screens.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.credentials.CredentialManager
import com.example.data.backup.AutoBackupFrequency
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthManager
import com.example.ui.theme.ExpenseRed
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isBangla = settings?.language == AppLanguage.BANGLA

    // Reactive Auth & Cloud states from ViewModel
    val currentUser by viewModel.currentUser.collectAsState()
    val isCloudSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()
    val isSyncing by viewModel.isSyncingCloud.collectAsState()
    val isBackingUpCloud by viewModel.isBackingUpCloud.collectAsState()
    val isRestoringCloud by viewModel.isRestoringCloud.collectAsState()
    val lastBackupTime by viewModel.lastCloudBackupTimestamp.collectAsState()
    val lastBackupSize by viewModel.lastCloudBackupSize.collectAsState()
    val lastSyncTime by viewModel.lastCloudSyncTimestamp.collectAsState()

    var isSigningIn by remember { mutableStateOf(false) }

    // Dialog & Flow States
    var pendingExportType by remember { mutableStateOf<String?>(null) } // "JSON" or "CSV" or "SAMPLE_JSON"
    var showSyncEnableDialog by remember { mutableStateOf(false) }

    // Two-step Erase Confirmation States (Preserved Danger Zone)
    var showEraseStep1Dialog by remember { mutableStateOf(false) }
    var showEraseStep2Dialog by remember { mutableStateOf(false) }
    var preserveCloudDataOnErase by remember { mutableStateOf(true) }

    val credentialManager = remember { CredentialManager.create(context) }
    val timeFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val dividerColor = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000)

    // Storage Access Framework: Create Document Launcher
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = when (pendingExportType) {
                        "CSV" -> viewModel.backupManager.createBackupCsv()
                        "SAMPLE_JSON" -> viewModel.getSampleJsonTemplate()
                        else -> viewModel.backupManager.createBackupJson()
                    }
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.userNotificationMessage.value = if (isBangla)
                        "ফাইল সফলভাবে সংরক্ষণ করা হয়েছে!"
                    else
                        "File successfully exported and saved!"
                } catch (e: Exception) {
                    viewModel.userNotificationMessage.value = "Export failed: ${e.localizedMessage}"
                } finally {
                    pendingExportType = null
                }
            }
        } else {
            pendingExportType = null
        }
    }

    // Storage Access Framework: Import Document Launcher (JSON or CSV)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                var resolvedName = uri.lastPathSegment?.substringAfterLast('/') ?: "wallet_backup.json"
                var resolvedSize = 0L
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIdx >= 0) {
                                val displayName = cursor.getString(nameIdx)
                                if (!displayName.isNullOrBlank()) resolvedName = displayName
                            }
                            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                                resolvedSize = cursor.getLong(sizeIdx)
                            }
                        }
                    }
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        viewModel.reportBackupReadError(resolvedName, "Unable to open input stream.")
                        return@launch
                    }
                    inputStream.use { stream ->
                        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                        val content = reader.readText()
                        val effectiveSize = if (resolvedSize > 0L) resolvedSize else content.toByteArray(Charsets.UTF_8).size.toLong()
                        viewModel.prepareBackupPreview(
                            jsonString = content,
                            fileName = resolvedName,
                            fileSizeBytes = effectiveSize
                        )
                    }
                } catch (e: Exception) {
                    viewModel.reportBackupReadError(resolvedName, e.localizedMessage ?: "Failed to read file.")
                }
            }
        }
    }

    fun shareExportFile(content: String, fileName: String, mimeType: String) {
        try {
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, if (isBangla) "ফাইল শেয়ার করুন" else "Share Backup File"))
        } catch (_: Exception) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, content)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Backup"))
        }
    }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.BACKUP_RESTORE
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("backup_restore_lazy_column"),
        contentPadding = PaddingValues(bottom = navBarBottomPadding + 48.dp)
    ) {
        // Top Header with Back Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF262B3D) else Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("backup_back_button"),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isBangla) "ব্যাকআপ ও রিস্টোর" else "Backup & Restore",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = if (isBangla) "ডাটা ব্যাকআপ, রিস্টোর ও সিঙ্ক" else "Backup, restore & sync",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // =========================================================================
        // CARD A: LOCAL BACKUP & RESTORE (Merged Export, Import, and Sample)
        // =========================================================================
        item {
            CompactSectionLabel(
                title = if (isBangla) "লোকাল ব্যাকআপ ও রিস্টোর" else "LOCAL BACKUP & RESTORE"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.75f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Action 1: Export Data (JSON & CSV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6366F1).copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "ডাটা এক্সপোর্ট" else "Export Data",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBangla) "JSON বা CSV ফাইল সংরক্ষণ" else "Export JSON or CSV file",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    pendingExportType = "JSON"
                                    createDocLauncher.launch("HishabPati_Backup_${System.currentTimeMillis()}.json")
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("export_json_button")
                            ) {
                                Text("JSON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    pendingExportType = "CSV"
                                    createDocLauncher.launch("HishabPati_Export_${System.currentTimeMillis()}.csv")
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("export_csv_button")
                            ) {
                                Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Action 2: Import & Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "ইমপোর্ট ও রিস্টোর" else "Import & Restore",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBangla) "JSON বা CSV ফাইল থেকে রিস্টোর" else "Restore from JSON or CSV",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Button(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "text/plain",
                                        "*/*"
                                    )
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("select_backup_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isBangla) "বাছাই" else "Select", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Action 3: Sample JSON Template
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "নমুনা টেমপ্লেট" else "Sample Template",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBangla) "নমুনা আর্থিক ডাটা" else "Sample financial template",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    pendingExportType = "SAMPLE_JSON"
                                    createDocLauncher.launch("HishabPati_Sample_Template_${System.currentTimeMillis()}.json")
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("sample_template_download")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isBangla) "ডাউনলোড" else "Save", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val json = viewModel.getSampleJsonTemplate()
                                    shareExportFile(json, "HishabPati_Sample_Template.json", "application/json")
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("sample_template_share")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isBangla) "শেয়ার" else "Share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CARD B: CLOUD BACKUP & SYNC (Merged Cloud Backup and Multi-Device Sync)
        // =========================================================================
        item {
            Spacer(modifier = Modifier.height(10.dp))
            CompactSectionLabel(
                title = if (isBangla) "ক্লাউড ব্যাকআপ ও সিঙ্ক" else "CLOUD BACKUP & SYNC"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.88f else 0.75f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. Google Account Connection Status
                    if (currentUser == null) {
                        // Unauthenticated / Guest state
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "গুগল সাইন-ইন আবশ্যক" else "Google Sign-In Required",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isBangla)
                                        "ক্লাউড ব্যাকআপ ও সিঙ্কের জন্য সাইন-ইন করুন"
                                    else
                                        "Sign in to backup & sync across devices",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isSigningIn = true
                                AuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        viewModel.onUserSignedIn()
                                        viewModel.userNotificationMessage.value = if (isBangla)
                                            "গুগল সাইন-ইন সফল হয়েছে!"
                                        else
                                            "Signed in with Google successfully!"
                                    },
                                    onAuthError = { err ->
                                        isSigningIn = false
                                        viewModel.userNotificationMessage.value = "Sign in error: $err"
                                    },
                                    scope = coroutineScope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(21.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "লগইন হচ্ছে..." else "Signing in...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "গুগল সাইন-ইন করুন" else "Sign In with Google",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Authenticated State: Google Account Info
                        val user = currentUser!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.email ?: user.displayName ?: "Google Account",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isBangla)
                                        "সংযুক্ত ক্লাউড স্টোরেজ"
                                    else
                                        "Connected · UID: ${user.uid.take(8)}...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    AuthManager.signOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {
                                            viewModel.onUserSignedOut()
                                            viewModel.userNotificationMessage.value = if (isBangla) "লগআউট সম্পন্ন হয়েছে" else "Signed out"
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(if (isBangla) "লগআউট" else "Sign Out", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Last Backup Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "সর্বশেষ ব্যাকআপ" else "Last Backup",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                val lastBackupFormatted = if (lastBackupTime > 0L) {
                                    timeFormatter.format(Date(lastBackupTime)) + (if (lastBackupSize > 0) " (${lastBackupSize / 1024} KB)" else "")
                                } else {
                                    if (isBangla) "এখনও কোনো ব্যাকআপ নেওয়া হয়নি" else "Never backed up"
                                }
                                Text(
                                    text = lastBackupFormatted,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (lastBackupTime > 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. Auto Backup Setting (Off / Daily / Weekly)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBangla) "স্বয়ংক্রিয় ব্যাকআপ" else "Auto Backup",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (isBangla) "স্বয়ংক্রিয় ব্যাকআপ শিডিউল" else "Scheduled automatic backup",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Segmented pill control: Off / Daily / Weekly
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF1E2230) else Color(0xFFF1F5F9))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AutoBackupFrequency.values().forEach { freq ->
                                    val isSelected = autoBackupFrequency == freq
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setAutoBackupFrequency(freq) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shadowElevation = if (isSelected) 1.dp else 0.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isBangla) freq.labelBn else freq.labelEn,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Back Up Now & Restore from Cloud Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.backupToCloud() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("cloud_backup_now_button"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isBackingUpCloud && !isRestoringCloud,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                if (isBackingUpCloud) {
                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isBangla) "এখন ব্যাকআপ নিন" else "Back Up Now",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { viewModel.restoreFromCloud() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("cloud_restore_button"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isBackingUpCloud && !isRestoringCloud,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                if (isRestoringCloud) {
                                    CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isBangla) "ক্লাউড রিস্টোর" else "Restore Cloud",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        // 5. Multi-Device Sync Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "মাল্টি-ডিভাইস সিঙ্ক" else "Multi-Device Sync",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isBangla)
                                        "সব ডিভাইসে স্বয়ংক্রিয় সমন্বয়"
                                    else
                                        "Sync changes across your devices",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Switch(
                                checked = isCloudSyncEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        showSyncEnableDialog = true
                                    } else {
                                        viewModel.setCloudSyncEnabled(false)
                                    }
                                },
                                modifier = Modifier.testTag("cloud_sync_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        if (isCloudSyncEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val syncText = if (lastSyncTime > 0L) timeFormatter.format(Date(lastSyncTime)) else if (isBangla) "এখনও সিঙ্ক হয়নি" else "Not synced yet"
                                Text(
                                    text = "${if (isBangla) "সর্বশেষ সিঙ্ক: " else "Last synced: "}$syncText",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.syncWithCloud(force = true) },
                                    enabled = !isSyncing,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(if (isBangla) "সিঙ্ক" else "Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CARD C: DANGER ZONE (Unchanged, Clear & Safe Confirmation Flow)
        // =========================================================================
        item {
            Spacer(modifier = Modifier.height(14.dp))
            CompactSectionLabel(
                title = if (isBangla) "বিপদ অঞ্চল" else "DANGER ZONE"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ExpenseRed.copy(alpha = if (isDark) 0.12f else 0.07f)
                ),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ExpenseRed.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "সমস্ত আর্থিক তথ্য মুছুন" else "Erase All Financial Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ExpenseRed
                            )
                            Text(
                                text = if (isBangla)
                                    "ডিভাইসের সব ওয়ালেট ও লেনদেন স্থায়ীভাবে ডিলিট হবে।"
                                else
                                    "Permanently removes local accounts & transactions.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showEraseStep1Dialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("erase_all_data_button"),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "সমস্ত তথ্য মুছে ফেলুন" else "Erase All Data",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // Enable Cloud Sync Confirmation Dialog
    if (showSyncEnableDialog) {
        AlertDialog(
            onDismissRequest = { showSyncEnableDialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            title = {
                Text(
                    text = if (isBangla) "ক্লাউড সিঙ্ক চালু করবেন?" else "Enable Multi-Device Cloud Sync?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "সিঙ্ক চালু করলে আপনার এই ডিভাইসের স্থানীয় লেনদেনগুলো এবং গুগল ক্লাউডে থাকা তথ্য একে অপরের সাথে সমন্বয় (Merge) হবে।\n\nকোনো বিদ্যমান রেকর্ড মোছা হবে না এবং ডুপ্লিকেট প্রতিরোধ করা হবে।"
                    else
                        "Enabling sync will bidirectionally synchronize transactions between this device and your Google Cloud account.\n\nExisting local data will never be erased, and duplicate signatures are automatically prevented.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSyncEnableDialog = false
                        viewModel.setCloudSyncEnabled(true)
                    }
                ) {
                    Text(if (isBangla) "সিঙ্ক চালু করুন" else "Enable Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncEnableDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Erase Step 1 Dialog
    if (showEraseStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showEraseStep1Dialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isBangla) "ধাপ ১/২: আপনি কি নিশ্চিত?" else "Step 1/2: Erase All Data?",
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "ডিভাইসের সব অ্যাকাউন্ট, লেনদেন ও বাজেট স্থায়ীভাবে মুছে যাবে।\n\nএটি আর ফিরিয়ে আনা যাবে না।"
                    else
                        "Permanently deletes all local accounts, transactions, and budgets from this device.\n\nThis action cannot be undone.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEraseStep1Dialog = false
                        showEraseStep2Dialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(
                        text = if (isBangla) "পরবর্তী ধাপ" else "Proceed to Step 2",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseStep1Dialog = false }) {
                    Text(
                        text = if (isBangla) "বাতিল" else "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Erase Step 2 Dialog (Final explicit confirmation + preserve cloud choice)
    if (showEraseStep2Dialog) {
        AlertDialog(
            onDismissRequest = { showEraseStep2Dialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isBangla) "ধাপ ২/২: চূড়ান্ত নিশ্চিতকরণ" else "Step 2/2: Final Confirmation",
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBangla)
                            "স্থানীয় ডাটা মোছার আগে ক্লাউড ব্যাকআপের অপশন নির্বাচন করুন:"
                        else
                            "Review cloud backup protection before deleting local records:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (preserveCloudDataOnErase) Color(0xFF10B981).copy(alpha = if (isDark) 0.18f else 0.10f) else ExpenseRed.copy(alpha = if (isDark) 0.18f else 0.10f),
                        border = BorderStroke(1.dp, if (preserveCloudDataOnErase) Color(0xFF10B981).copy(alpha = 0.4f) else ExpenseRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { preserveCloudDataOnErase = !preserveCloudDataOnErase }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = preserveCloudDataOnErase,
                                onCheckedChange = { preserveCloudDataOnErase = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isBangla) "ক্লাউড ডাটা সুরক্ষিত রাখুন (প্রস্তাবিত)" else "Preserve Cloud Data (Recommended)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (preserveCloudDataOnErase) Color(0xFF10B981) else ExpenseRed
                                )
                                Text(
                                    text = if (isBangla)
                                        "গুগল ক্লাউড ব্যাকআপ অক্ষত থাকবে"
                                    else
                                        "Keeps your Google cloud backup safe",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEraseStep2Dialog = false
                        viewModel.eraseAllData(preserveCloud = preserveCloudDataOnErase)
                        onBack()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(
                        text = if (isBangla) "স্থায়ীভাবে মুছে ফেলুন" else "Permanently Erase",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseStep2Dialog = false }) {
                    Text(
                        text = if (isBangla) "বাতিল" else "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun CompactSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
}
