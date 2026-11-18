package com.example.ui.screens.settings

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.util.AppHaptics
import com.example.ui.util.AppStrings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.credentials.CredentialManager
import com.example.ui.auth.AuthManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.local.UserSettingsPreferences
import com.example.data.model.AppLanguage
import com.example.data.update.UpdateState
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.AvatarBadge
import com.example.ui.components.TranslucentCard
import com.example.ui.screens.home.CustomizeHomeBottomSheet
import com.example.ui.screens.transaction.CategorySelectionSheetStyle
import com.example.ui.screens.transaction.SheetHeaderCircleActionButton
import com.example.ui.screens.transaction.WalletSelectionBottomSheet
import com.example.ui.theme.ExpenseRed
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsRowItem(
    val testTag: String,
    val targetSubScreen: SubScreen?
) {
    NOTIFICATIONS("settings_row_notifications", null),
    THEME("settings_row_theme", SubScreen.THEME_SETTINGS),
    LANGUAGE("settings_row_language", null),
    DEFAULT_CURRENCY("settings_row_default_currency", SubScreen.CURRENCY_SELECTION),
    CATEGORIES("settings_row_categories", SubScreen.CATEGORIES_MANAGEMENT),
    ACCOUNTS("settings_row_accounts", SubScreen.ACCOUNTS_MANAGEMENT),
    BACKUP_RESTORE("settings_row_backup_restore", SubScreen.BACKUP_RESTORE),
    ABOUT("settings_row_about", SubScreen.ABOUT)
}

data class LanguagePopupOption(
    val language: AppLanguage,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val isSelected: Boolean,
    val testTag: String,
    val selectedIndicatorTestTag: String
)

object SettingsAndAboutSpec {
    val DIALOG_CORNER_RADIUS = 28.dp
    val OPTION_CORNER_RADIUS = 16.dp
    val SELECTED_BORDER_WIDTH = 1.75.dp
    val UNSELECTED_BORDER_WIDTH = 1.dp

    val orderedRows: List<SettingsRowItem> = listOf(
        SettingsRowItem.NOTIFICATIONS,
        SettingsRowItem.THEME,
        SettingsRowItem.LANGUAGE,
        SettingsRowItem.DEFAULT_CURRENCY,
        SettingsRowItem.CATEGORIES,
        SettingsRowItem.ACCOUNTS,
        SettingsRowItem.BACKUP_RESTORE,
        SettingsRowItem.ABOUT
    )

    fun rowTitle(item: SettingsRowItem, language: AppLanguage): String {
        val isBangla = language == AppLanguage.BANGLA
        return when (item) {
            SettingsRowItem.NOTIFICATIONS -> if (isBangla) "বিজ্ঞপ্তি" else "Notifications"
            SettingsRowItem.THEME -> if (isBangla) "থিম" else "Theme"
            SettingsRowItem.LANGUAGE -> if (isBangla) "ভাষা" else "Language"
            SettingsRowItem.DEFAULT_CURRENCY -> if (isBangla) "ডিফল্ট মুদ্রা" else "Default currency"
            SettingsRowItem.CATEGORIES -> if (isBangla) "ক্যাটাগরি" else "Categories"
            SettingsRowItem.ACCOUNTS -> if (isBangla) "অ্যাকাউন্টসমূহ" else "Accounts"
            SettingsRowItem.BACKUP_RESTORE -> if (isBangla) "ব্যাকআপ ও রিস্টোর" else "Backup & Restore"
            SettingsRowItem.ABOUT -> if (isBangla) "সম্পর্কে" else "About"
        }
    }

    fun aboutAppName(language: AppLanguage): String = AppStrings.t("app_name", language)

    fun aboutSecondarySubtitle(): String? = null

    fun languagePopupOptions(currentLanguage: AppLanguage): List<LanguagePopupOption> {
        val isBangla = currentLanguage == AppLanguage.BANGLA
        return listOf(
            LanguagePopupOption(
                language = AppLanguage.ENGLISH,
                title = "English",
                subtitle = if (isBangla) "ইংরেজি ভাষা" else "Default language",
                badgeText = "EN",
                isSelected = currentLanguage == AppLanguage.ENGLISH,
                testTag = "language_option_english",
                selectedIndicatorTestTag = "language_selected_english"
            ),
            LanguagePopupOption(
                language = AppLanguage.BANGLA,
                title = "বাংলা (Bangla)",
                subtitle = if (isBangla) "বাংলা ভাষায় অ্যাপ ব্যবহার করুন" else "Bangla language",
                badgeText = "বাং",
                isSelected = currentLanguage == AppLanguage.BANGLA,
                testTag = "language_option_bangla",
                selectedIndicatorTestTag = "language_selected_bangla"
            )
        )
    }

    fun optionContainerColor(primary: Color, surfaceVariant: Color, isSelected: Boolean, isDark: Boolean): Color {
        return if (isSelected) {
            primary.copy(alpha = if (isDark) 0.22f else 0.10f)
        } else {
            if (isDark) Color(0xFF23283B) else surfaceVariant.copy(alpha = 0.45f)
        }
    }

    fun optionBorderColor(primary: Color, outlineVariant: Color, isSelected: Boolean, isDark: Boolean): Color {
        return if (isSelected) {
            primary
        } else {
            if (isDark) outlineVariant.copy(alpha = 0.30f) else outlineVariant.copy(alpha = 0.50f)
        }
    }
}

@Composable
fun ProfileSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = viewModel.rememberScreenLazyListState(
        screenOverride = SubScreen.SETTINGS
    )
) {
    SettingsScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier,
        listState = listState
    )
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = viewModel.rememberScreenLazyListState(
        screenOverride = SubScreen.SETTINGS
    )
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val extendedSettings by viewModel.extendedSettings.collectAsState()
    val homeCustomization by viewModel.homeCustomization.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val restoreUiState by viewModel.restoreUiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val lastUpdateCheckTimestamp by viewModel.lastUpdateCheckTimestamp.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val isBangla = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val hapticsEnabled = extendedSettings.hapticFeedbackEnabled

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCustomizeHomeSheet by remember { mutableStateOf(false) }
    var showDefaultWalletSheet by remember { mutableStateOf(false) }
    var showStartOfWeekSheet by remember { mutableStateOf(false) }
    var showNoDeviceLockDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var pendingExportExtension by remember { mutableStateOf("json") }

    var showEraseStep1Dialog by remember { mutableStateOf(false) }
    var showEraseStep2Dialog by remember { mutableStateOf(false) }
    var preserveCloudOnErase by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }

    val resolvedDefaultWallet = remember(accounts, extendedSettings.defaultWalletId) {
        UserSettingsPreferences.resolveDefaultWallet(accounts, extendedSettings.defaultWalletId)
    }

    val confirmScreenLockEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setUseScreenLock(true)
            viewModel.userNotificationMessage.value = if (isBangla) {
                "স্ক্রিন লক সক্রিয় করা হয়েছে"
            } else {
                "Screen Lock enabled"
            }
        }
    }

    // Storage Access Framework: Export Document Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = if (pendingExportExtension == "csv") {
                        viewModel.backupManager.createBackupCsv()
                    } else {
                        viewModel.backupManager.createBackupJson()
                    }
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.userNotificationMessage.value = if (isBangla) "ব্যাকআপ সফলভাবে সংরক্ষণ করা হয়েছে!" else "Backup successfully exported!"
                } catch (e: Exception) {
                    viewModel.userNotificationMessage.value = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    // Storage Access Framework: Import Document Launcher
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
                    if (!resolvedName.contains('.')) {
                        resolvedName = "$resolvedName.json"
                    }
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        viewModel.reportBackupReadError(resolvedName, "Unable to open input stream for URI.")
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
                    viewModel.reportBackupReadError(resolvedName, e.localizedMessage ?: "Failed to read backup file.")
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            viewModel.updateSettingsScrollPosition(index, offset)
        }
    }

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 32.dp
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("settings_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. Profile Section
            item(key = "settings_section_profile") {
                SettingsGroupHeader(title = if (isBangla) "প্রোফাইল" else "PROFILE")
            val userState by viewModel.currentUser.collectAsState()
            val user = userState
            val currentDisplayName = if (user != null) {
                user.displayName?.takeIf { it.isNotBlank() } ?: settings?.userName ?: "Google User"
            } else {
                settings?.userName?.takeIf { it.isNotBlank() } ?: "Guest User"
            }
            val userInitials = currentDisplayName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .ifEmpty { "U" }
                .uppercase()

            val avatarPhoto = if (user != null && !user.photoUrl.toString().isNullOrBlank()) {
                user.photoUrl.toString()
            } else {
                settings?.profilePhotoUri?.takeIf { it.isNotBlank() }
            }
            val avatarSymbol = if (settings?.profileAvatarType == "SYMBOL") settings?.profileAvatarSymbol else null
            val avatarBgColor = try {
                Color(android.graphics.Color.parseColor(settings?.profileAvatarColorHex ?: "#4F46E5"))
            } catch (_: Exception) {
                Color(0xFF4F46E5)
            }

            val isSyncing by viewModel.isSyncingCloud.collectAsState()
            val credentialManager = remember { CredentialManager.create(context) }

            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Profile Info (Tapping opens editable profile)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_row_user_profile")
                            .clickable { viewModel.openSubScreen(SubScreen.USER_PROFILE) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBadge(
                            initials = userInitials,
                            photoUrl = avatarPhoto,
                            symbolName = avatarSymbol,
                            backgroundColor = avatarBgColor,
                            size = 52.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentDisplayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "${settings?.country ?: "Bangladesh"} · ${settings?.currencyCode ?: "BDT"} (${settings?.currencySymbol ?: "৳"})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000)
                    )

                    // Account Status & Actions
                    if (user != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4).copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Cloud Connected",
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "গুগল অ্যাকাউন্ট সংযুক্ত" else "Google Account Connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = user.email ?: user.displayName ?: "Authenticated with Google",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncWithCloud() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isBangla) "সিঙ্ক করুন" else "Sync Cloud",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    AuthManager.signOut(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onSignOutComplete = {
                                            viewModel.onUserSignedOut()
                                            viewModel.userNotificationMessage.value = if (isBangla) "গুগল অ্যাকাউন্ট থেকে সাইন আউট সম্পন্ন হয়েছে" else "Signed out of Google account"
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "লগ আউট" else "Sign Out",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4).copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Cloud Backup",
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "গেস্ট মোড (অফলাইন)" else "Guest Mode (Local)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBangla) "ক্লাউড ব্যাকআপ ও মাল্টি-ডিভাইস সিঙ্ক চালু করতে গুগল সাইন ইন করুন" else "Sign in with Google to enable cloud backup & multi-device sync",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                isSigningIn = true
                                AuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        viewModel.onUserSignedIn()
                                        viewModel.userNotificationMessage.value = if (isBangla) "সফলভাবে সাইন ইন হয়েছে!" else "Signed in successfully!"
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
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "গুগল দিয়ে সাইন ইন করুন" else "Sign in with Google",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 2. GENERAL & CUSTOMIZATION Card (Theme, Language, Notification Manager, Customize Home Dashboard, Subcategories, Auto-Open Keypad)
        item(key = "settings_section_general") {
            SettingsGroupHeader(title = if (isBangla) "সাধারণ ও কাস্টমাইজেশন" else "GENERAL & CUSTOMIZATION")
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsClickableRow(
                        icon = Icons.Default.ColorLens,
                        iconColor = Color(0xFF8B5CF6),
                        title = SettingsAndAboutSpec.rowTitle(SettingsRowItem.THEME, language),
                        value = "${settings?.themeColor?.displayName ?: "Indigo"} · ${settings?.themeMode?.displayName ?: "Auto"}",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.THEME_SETTINGS)
                        },
                        testTag = SettingsRowItem.THEME.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsClickableRow(
                        icon = Icons.Default.Language,
                        iconColor = Color(0xFF2563EB),
                        title = SettingsAndAboutSpec.rowTitle(SettingsRowItem.LANGUAGE, language),
                        value = settings?.language?.displayName ?: "English",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            showLanguageDialog = true
                        },
                        testTag = SettingsRowItem.LANGUAGE.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsClickableRow(
                        icon = Icons.Default.Notifications,
                        iconColor = Color(0xFFF59E0B),
                        title = if (isBangla) "নোটিফিকেশন ম্যানেজার" else "Notification Manager",
                        value = if (isBangla) "দৈনিক অনুস্মারক এবং পেমেন্ট সতর্কতা পরিচালনা করুন" else "Manage daily reminders and payment alerts",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.NOTIFICATION_MANAGER)
                        },
                        testTag = SettingsRowItem.NOTIFICATIONS.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsClickableRow(
                        icon = Icons.Default.DashboardCustomize,
                        iconColor = Color(0xFF0EA5E9),
                        title = if (isBangla) "হোম ড্যাশবোর্ড কাস্টমাইজ করুন" else "Customize Home Dashboard",
                        value = if (isBangla) "আপনার হোম ড্যাশবোর্ডে প্রদর্শিত সেকশনগুলো সাজান ও টগল করুন" else "Reorder and toggle sections displayed on your home dashboard",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            showCustomizeHomeSheet = true
                        },
                        testTag = "settings_row_customize_home"
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsSwitchRow(
                        icon = Icons.Default.SubdirectoryArrowRight,
                        iconColor = Color(0xFF10B981),
                        title = if (isBangla) "সাবক্যাটাগরি সক্রিয় করুন" else "Enable Subcategories",
                        subtitle = if (isBangla) "আপনার ব্যয়ের অভ্যাস আরও বিস্তারিতভাবে ট্র্যাক করুন" else "Track your spending habits in more detail",
                        checked = extendedSettings.enableSubcategories,
                        onCheckedChange = { enabled ->
                            AppHaptics.performToggle(view, hapticsEnabled)
                            viewModel.setEnableSubcategories(enabled)
                        },
                        testTag = "settings_toggle_subcategories"
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsSwitchRow(
                        icon = Icons.Default.Dialpad,
                        iconColor = Color(0xFF6366F1),
                        title = if (isBangla) "স্বয়ংক্রিয়ভাবে অ্যামাউন্ট কীপ্যাড খুলুন" else "Auto-Open Amount Keypad",
                        subtitle = if (isBangla) "নতুন লেনদেন যোগ করার সময় স্বয়ংক্রিয়ভাবে অ্যামাউন্ট কীপ্যাড খুলুন" else "Automatically open the amount keypad when adding a new transaction",
                        checked = extendedSettings.autoOpenAmountKeypad,
                        onCheckedChange = { enabled ->
                            AppHaptics.performToggle(view, hapticsEnabled)
                            viewModel.setAutoOpenAmountKeypad(enabled)
                        },
                        testTag = "settings_toggle_auto_open_keypad"
                    )
                }
            }
        }

        // 3. WALLET, CURRENCY & DATA Card
        item(key = "settings_section_wallet_data") {
            SettingsGroupHeader(title = if (isBangla) "ওয়ালেট, মুদ্রা ও ডাটা" else "WALLET, CURRENCY & DATA")
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val walletName = resolvedDefaultWallet?.name ?: (if (isBangla) "ওয়ালেট ক্যাশ" else "Wallet Cash")
                    SettingsClickableRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = Color(0xFF0284C7),
                        title = if (isBangla) "ডিফল্ট ওয়ালেট" else "Default Wallet",
                        value = if (isBangla) "আপনার বর্তমান ডিফল্ট হলো $walletName" else "Your current default is $walletName",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            showDefaultWalletSheet = true
                        },
                        testTag = "settings_row_default_wallet"
                    )

                    SettingsRowDivider(isDark = isDark)

                    val currencyCode = settings?.currencyCode ?: "BDT"
                    val currencyDisplay = if (currencyCode == "BDT") {
                        if (isBangla) "বাংলাদেশী টাকা (BDT)" else "Bangladeshi Taka (BDT)"
                    } else {
                        "$currencyCode (${settings?.currencySymbol ?: "৳"})"
                    }
                    SettingsClickableRow(
                        icon = Icons.Default.Payments,
                        iconColor = Color(0xFF059669),
                        title = if (isBangla) "প্রধান মুদ্রা" else "Main Currency",
                        value = currencyDisplay,
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.CURRENCY_SELECTION)
                        },
                        testTag = SettingsRowItem.DEFAULT_CURRENCY.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsClickableRow(
                        icon = Icons.Default.Category,
                        iconColor = Color(0xFF10B981),
                        title = SettingsAndAboutSpec.rowTitle(SettingsRowItem.CATEGORIES, language),
                        value = if (isBangla) "ক্যাটাগরি ও সাব-ক্যাটাগরি পরিচালনা করুন" else "Manage categories & subcategories",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.CATEGORIES_MANAGEMENT)
                        },
                        testTag = SettingsRowItem.CATEGORIES.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsClickableRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = Color(0xFF0EA5E9),
                        title = SettingsAndAboutSpec.rowTitle(SettingsRowItem.ACCOUNTS, language),
                        value = if (isBangla) "ওয়ালেট, ব্যাংক ও ব্যালেন্স পরিচালনা করুন" else "Manage wallets, banks & balances",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.ACCOUNTS_MANAGEMENT)
                        },
                        testTag = SettingsRowItem.ACCOUNTS.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsClickableRow(
                        icon = Icons.Default.Sync,
                        iconColor = Color(0xFF4F46E5),
                        title = SettingsAndAboutSpec.rowTitle(SettingsRowItem.BACKUP_RESTORE, language),
                        value = if (isBangla) "এক্সপোর্ট, রিস্টোর, ক্লাউড ব্যাকআপ ও সিঙ্ক" else "Export, restore, cloud backup & sync",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.BACKUP_RESTORE)
                        },
                        testTag = SettingsRowItem.BACKUP_RESTORE.testTag
                    )
                }
            }
        }

        // 4. DATE & TIME Card (Start of Week)
        item(key = "settings_section_date_time") {
            SettingsGroupHeader(title = if (isBangla) "তারিখ ও সময়" else "DATE & TIME")
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    val startOfWeekLabel = UserSettingsPreferences.dayName(extendedSettings.startOfWeek, language)
                    SettingsClickableRow(
                        icon = Icons.Default.CalendarMonth,
                        iconColor = Color(0xFF8B5CF6),
                        title = if (isBangla) "সপ্তাহের শুরু" else "Start of Week",
                        value = startOfWeekLabel,
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            showStartOfWeekSheet = true
                        },
                        testTag = "settings_row_start_of_week"
                    )
                }
            }
        }

        // 5. SECURITY & SYSTEM Card (Use Screen Lock, Haptic Feedback)
        item(key = "settings_section_security") {
            SettingsGroupHeader(title = if (isBangla) "নিরাপত্তা ও সিস্টেম" else "SECURITY & SYSTEM")
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsSwitchRow(
                        icon = Icons.Default.Lock,
                        iconColor = Color(0xFFF59E0B),
                        title = if (isBangla) "স্ক্রিন লক ব্যবহার করুন" else "Use Screen Lock",
                        subtitle = if (isBangla) "অ্যাপটি খুলতে আপনার ডিভাইসের স্ক্রিন লক প্রয়োজন।" else "Require your device's screen lock to open the app.",
                        checked = extendedSettings.useScreenLock,
                        onCheckedChange = { requestedEnabled ->
                            AppHaptics.performToggle(view, hapticsEnabled)
                            if (requestedEnabled) {
                                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                                val isDeviceSecure = km?.isDeviceSecure == true
                                if (!isDeviceSecure) {
                                    showNoDeviceLockDialog = true
                                } else {
                                    @Suppress("DEPRECATION")
                                    val confirmIntent = km?.createConfirmDeviceCredentialIntent(
                                        if (isBangla) "হিসাব পাতি স্ক্রিন লক" else "Enable Screen Lock",
                                        if (isBangla) "স্ক্রিন লক সক্রিয় করতে আপনার ডিভাইসের পিন, প্যাটার্ন বা বায়োমেট্রিক যাচাই করুন" else "Verify your device screen lock to enable app protection"
                                    )
                                    if (confirmIntent != null) {
                                        try {
                                            confirmScreenLockEnableLauncher.launch(confirmIntent)
                                        } catch (_: Exception) {
                                            viewModel.setUseScreenLock(true)
                                        }
                                    } else {
                                        viewModel.setUseScreenLock(true)
                                    }
                                }
                            } else {
                                viewModel.setUseScreenLock(false)
                            }
                        },
                        testTag = "settings_toggle_screen_lock"
                    )

                    SettingsRowDivider(isDark = isDark)

                    SettingsSwitchRow(
                        icon = Icons.Default.Vibration,
                        iconColor = Color(0xFF10B981),
                        title = if (isBangla) "হ্যাপটিক ফিডব্যাক" else "Haptic Feedback",
                        subtitle = if (isBangla) "বিভিন্ন ইন্টারঅ্যাকশনে ভাইব্রেশন সক্রিয় করুন" else "Enable vibration on various interactions",
                        checked = extendedSettings.hapticFeedbackEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                AppHaptics.performToggle(view, true)
                            }
                            viewModel.setHapticFeedbackEnabled(enabled)
                        },
                        testTag = "settings_toggle_haptic_feedback"
                    )
                }
            }
        }

        // 6. ABOUT Card
        item(key = "settings_section_about") {
            SettingsGroupHeader(title = if (isBangla) "অ্যাপ সম্পর্কে" else "ABOUT")
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsClickableRow(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF6366F1),
                        title = SettingsAndAboutSpec.rowTitle(SettingsRowItem.ABOUT, language),
                        value = if (isBangla) "হিসাব পাতি সম্পর্কে তথ্য" else "App info & developer details",
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            viewModel.openSubScreen(SubScreen.ABOUT)
                        },
                        testTag = SettingsRowItem.ABOUT.testTag
                    )

                    SettingsRowDivider(isDark = isDark)

                    val lastCheckedLabel = remember(lastUpdateCheckTimestamp, isBangla) {
                        if (lastUpdateCheckTimestamp <= 0L) {
                            if (isBangla) "এখনো যাচাই করা হয়নি" else "Never checked"
                        } else {
                            val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH)
                            val formatted = fmt.format(Date(lastUpdateCheckTimestamp))
                            if (isBangla) "সর্বশেষ যাচাই: $formatted" else "Last checked: $formatted"
                        }
                    }
                    val updateRowSubtitle = when (val uState = updateState) {
                        is UpdateState.Checking -> if (isBangla) "আপডেট খোঁজা হচ্ছে..." else "Checking for updates..."
                        is UpdateState.UpToDate -> if (isBangla) {
                            "v${BuildConfig.VERSION_NAME} · আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন · $lastCheckedLabel"
                        } else {
                            "v${BuildConfig.VERSION_NAME} · You're on the latest version · $lastCheckedLabel"
                        }
                        is UpdateState.Available -> if (isBangla) {
                            "v${BuildConfig.VERSION_NAME} → v${uState.releaseInfo.versionName} উপলব্ধ"
                        } else {
                            "v${BuildConfig.VERSION_NAME} · Update v${uState.releaseInfo.versionName} available"
                        }
                        is UpdateState.Downloading -> if (isBangla) {
                            "আপডেট ডাউনলোড হচ্ছে..."
                        } else {
                            "Downloading update..."
                        }
                        is UpdateState.ReadyToInstall -> if (isBangla) {
                            "ইনস্টলের জন্য প্রস্তুত"
                        } else {
                            "Ready to install"
                        }
                        else -> if (isBangla) {
                            "বর্তমান সংস্করণ v${BuildConfig.VERSION_NAME} · $lastCheckedLabel"
                        } else {
                            "Current v${BuildConfig.VERSION_NAME} · $lastCheckedLabel"
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("settings_row_check_updates")
                            .clickable {
                                AppHaptics.performTap(view, hapticsEnabled)
                                viewModel.checkForUpdatesManually()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    color = Color(0xFF0EA5E9).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF0EA5E9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "আপডেট যাচাই করুন" else "Check for Updates",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = updateRowSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = if (isBangla) "আপডেট যাচাই" else "Check for Updates",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Footer Privacy Notice
        item(key = "settings_section_footer") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isBangla)
                    "গোপনীয় ও নিরাপদ · আপনার সমস্ত তথ্য ডিভাইসে নিরাপদে থাকে"
                else
                    "Private & Secure · All your data stays locally on device",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // Shared Fixed Scroll-Aware Frosted Header for Settings Page
    ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "settings_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF262B3D) else Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("settings_back_button"),
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

                Text(
                    text = if (isBangla) "সেটিংস" else "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }

    // Language Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            onSelectLanguage = { selectedLanguage ->
                viewModel.setLanguage(selectedLanguage)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Export Format Selector Dialog (JSON / CSV)
    if (showExportFormatDialog) {
        val dateStamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) }
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            title = {
                Text(
                    text = if (isBangla) "এক্সপোর্ট ফরম্যাট নির্বাচন করুন" else "Choose Export Format",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBangla) "আপনি কোন ফরম্যাটে ডাটা এক্সপোর্ট করতে চান?" else "Select which format you want to export your financial data in:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportFormatDialog = false
                                pendingExportExtension = "json"
                                exportLauncher.launch("HishabPati_Backup_$dateStamp.json")
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "JSON ব্যাকআপ ফাইল (.json)" else "JSON Backup File (.json)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBangla) "সম্পূর্ণ ডাটাবেজ ব্যাকআপ (ওয়ালেট, বাজেট, ঋণ)" else "Full database backup including wallets, budgets, debts",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportFormatDialog = false
                                pendingExportExtension = "csv"
                                exportLauncher.launch("HishabPati_Export_$dateStamp.csv")
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "CSV স্প্রেডশিট (.csv)" else "CSV Spreadsheet (.csv)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBangla) "এক্সেল ও গুগল শিটস উপযোগী লেনদেন ও হিসাব" else "Compatible with Excel, Google Sheets and other apps",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportFormatDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Erase Confirmation Step 1
    if (showEraseStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showEraseStep1Dialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            title = {
                Text(
                    text = if (isBangla) "সমস্ত স্থানীয় ডাটা মুছে ফেলতে চান?" else "Erase All Data?",
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isBangla)
                            "এটি আপনার ডিভাইসে সংরক্ষিত সমস্ত অ্যাকাউন্ট, লেনদেন, ঋণ ও বাজেট স্থায়ীভাবে মুছে ফেলবে। এই পদক্ষেপটি ফিরিয়ে আনা যাবে না।"
                        else
                            "This will permanently delete all your accounts, transactions, debts, and budgets from this device. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { preserveCloudOnErase = !preserveCloudOnErase }
                    ) {
                        Checkbox(
                            checked = preserveCloudOnErase,
                            onCheckedChange = { preserveCloudOnErase = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "ফায়ারবেস ক্লাউড ব্যাকআপ অক্ষত রাখুন" else "Preserve cloud backup in Firebase",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEraseStep1Dialog = false
                        showEraseStep2Dialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(if (isBangla) "পরবর্তী ধাপ" else "Continue to Erase", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseStep1Dialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Erase Confirmation Step 2 (Final Warning)
    if (showEraseStep2Dialog) {
        AlertDialog(
            onDismissRequest = { showEraseStep2Dialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            title = {
                Text(
                    text = if (isBangla) "চূড়ান্ত নিশ্চিতকরণ" else "Final Confirmation",
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            },
            text = {
                Text(
                    text = if (isBangla)
                        "আপনি কি নিশ্চিত? একবার মুছে ফেললে স্থানীয় ডাটা আর পুনরুদ্ধার করা যাবে না।"
                    else
                        "Are you absolutely certain? Once erased, local records cannot be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEraseStep2Dialog = false
                        viewModel.eraseAllData(preserveCloud = preserveCloudOnErase)
                        viewModel.userNotificationMessage.value = if (isBangla)
                            "সমস্ত স্থানীয় তথ্য মুছে ফেলা হয়েছে।"
                        else
                            "All local data has been erased."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(if (isBangla) "স্থায়ীভাবে মুছুন" else "Permanently Erase", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseStep2Dialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }



    // Customize Home Dashboard Bottom Sheet (opened from Settings)
    if (showCustomizeHomeSheet) {
        CustomizeHomeBottomSheet(
            current = homeCustomization,
            isBangla = isBangla,
            onDismiss = { showCustomizeHomeSheet = false },
            onUpdate = { updated ->
                viewModel.updateHomeCustomization(updated)
            }
        )
    }

    // Default Wallet Selection Bottom Sheet (reuses app's WalletSelectionBottomSheet)
    if (showDefaultWalletSheet) {
        WalletSelectionBottomSheet(
            title = if (isBangla) "ডিফল্ট ওয়ালেট নির্বাচন করুন" else "Select Default Wallet",
            selectedAccountId = resolvedDefaultWallet?.id ?: extendedSettings.defaultWalletId,
            accounts = accounts,
            currencySymbol = currencySymbol,
            onSelectAccount = { selectedAccount ->
                AppHaptics.performTap(view, hapticsEnabled)
                viewModel.setDefaultWalletId(selectedAccount.id)
                showDefaultWalletSheet = false
            },
            onDismiss = { showDefaultWalletSheet = false }
        )
    }

    // Start of the Week Selection Bottom Sheet (all 7 days of the week)
    if (showStartOfWeekSheet) {
        StartOfWeekBottomSheet(
            selectedDay = extendedSettings.startOfWeek,
            language = language,
            isDark = isDark,
            hapticsEnabled = hapticsEnabled,
            onSelectDay = { day ->
                viewModel.setStartOfWeek(day)
                showStartOfWeekSheet = false
            },
            onDismiss = { showStartOfWeekSheet = false }
        )
    }

    // Device Screen Lock Not Configured Dialog
    if (showNoDeviceLockDialog) {
        AlertDialog(
            onDismissRequest = { showNoDeviceLockDialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            title = {
                Text(
                    text = if (isBangla) "ডিভাইস স্ক্রিন লক সেট করা নেই" else "Device Screen Lock Required",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = if (isBangla) {
                        "হিসাব পাতিতে স্ক্রিন লক ব্যবহার করতে প্রথমে আপনার ফোনের সিকিউরিটি সেটিংসে পিন, প্যাটার্ন, পাসওয়ার্ড বা বায়োমেট্রিক লক সেট করুন।"
                    } else {
                        "To use Screen Lock in Hishab Pati, please set up a PIN, pattern, password, or biometric screen lock in your device's Security settings first."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNoDeviceLockDialog = false
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: Exception) {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            } catch (_: Exception) {
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(if (isBangla) "সিকিউরিটি সেটিংস খুলুন" else "Open Security Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoDeviceLockDialog = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Multi-step Backup Restore Modal (Selection Summary -> Caution Confirmation -> Progress -> Completion)
    com.example.ui.components.BackupRestoreModal(
        state = restoreUiState,
        isBangla = isBangla,
        currencySymbol = currencySymbol,
        onSelectMode = { mode -> viewModel.selectRestoreMode(mode) },
        onProceedToConfirm = { viewModel.proceedToRestoreConfirmation() },
        onBackToPreview = { viewModel.backToRestorePreview() },
        onExecuteRestore = { mode -> viewModel.executeRestoreWithMode(mode) },
        onExportFreshBackup = {
            exportLauncher.launch("HishabPati_pre_restore_${System.currentTimeMillis()}.json")
        },
        onPickAnotherFile = {
            viewModel.cancelRestore()
            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onGoToDashboard = {
            viewModel.completeRestoreAndGoToDashboard()
        },
        onDismiss = {
            viewModel.cancelRestore()
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val isBangla = currentLanguage == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 380.dp)
                .testTag("language_selection_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF1A1E2E) else MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(
                width = 1.25.dp,
                color = if (isDark) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = primaryColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ভাষা নির্বাচন করুন" else "Choose Language",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBangla) "অ্যাপের ভাষা পরিবর্তন করুন" else "Select your preferred app language",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsAndAboutSpec.languagePopupOptions(currentLanguage).forEach { option ->
                        LanguageOptionCard(
                            title = option.title,
                            subtitle = option.subtitle,
                            badgeText = option.badgeText,
                            isSelected = option.isSelected,
                            isDark = isDark,
                            testTag = option.testTag,
                            selectedIndicatorTestTag = option.selectedIndicatorTestTag,
                            onClick = { onSelectLanguage(option.language) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("language_dialog_cancel")
                    ) {
                        Text(
                            text = if (isBangla) "বাতিল" else "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    isSelected: Boolean,
    isDark: Boolean,
    testTag: String,
    selectedIndicatorTestTag: String,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = SettingsAndAboutSpec.optionContainerColor(
        primary = primaryColor,
        surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
        isSelected = isSelected,
        isDark = isDark
    )
    val borderColor = SettingsAndAboutSpec.optionBorderColor(
        primary = primaryColor,
        outlineVariant = MaterialTheme.colorScheme.outlineVariant,
        isSelected = isSelected,
        isDark = isDark
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SettingsAndAboutSpec.OPTION_CORNER_RADIUS),
        color = containerColor,
        border = BorderStroke(
            width = if (isSelected) SettingsAndAboutSpec.SELECTED_BORDER_WIDTH else SettingsAndAboutSpec.UNSELECTED_BORDER_WIDTH,
            color = borderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = if (isSelected) {
                    primaryColor
                } else {
                    primaryColor.copy(alpha = if (isDark) 0.16f else 0.10f)
                },
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badgeText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = primaryColor,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag(selectedIndicatorTestTag)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(24.dp)
                ) {}
            }
        }
    }
}

@Composable
fun AboutScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBangla = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = SubScreen.ABOUT
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 32.dp
    )

    Box(modifier = modifier.fillMaxSize().testTag("about_screen")) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .recordFrostedBackdrop(
                    scrollLayout.backdropState,
                    listState,
                    scrollLayout.contentGraphicsLayer
                )
                .testTag("about_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_app_logo),
                            contentDescription = "Hishab Pati Logo",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = SettingsAndAboutSpec.aboutAppName(language),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("about_app_name")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBangla) "আপনার অর্থ, আপনার নিয়ন্ত্রণ।" else "Your Money, Your Control.",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = if (isBangla) "সংস্করণ ${BuildConfig.VERSION_NAME}" else "Version ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .testTag("about_app_version")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isBangla)
                                "হিসাব পাতি একটি আধুনিক ও সুরক্ষিত ব্যক্তিগত অর্থ ব্যবস্থাপনা অ্যাপ। সম্পূর্ণ অফলাইনে আপনার আয়, ব্যয়, ঋণ, বাজেট ও ওয়ালেট নিয়ন্ত্রণ করুন সহজ ও স্বচ্ছভাবে।"
                            else
                                "Hishab Pati is a personal finance management app that helps you track income, expenses, accounts, budgets, debts, and financial records in one place. It helps you organize daily transactions, monitor spending, and manage your money more efficiently.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 22.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.5f),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isBangla) "ডেভেলপার: খান ইশতিয়াক শাহাবুদ্দিন" else "Developer: Khan Istiak Shahabuddin",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBangla) "বাংলাদেশে তৈরি ❤️" else "Made with ❤️ in Bangladesh",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isBangla)
                        "গোপনীয় ও নিরাপদ · আপনার সমস্ত তথ্য ডিভাইসে নিরাপদে থাকে"
                    else
                        "Private & Secure · All your data stays locally on device",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "about_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF262B3D) else Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("about_back_button"),
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

                Text(
                    text = if (isBangla) "সম্পর্কে" else "About",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 1.sp,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconColor.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsRowDivider(isDark: Boolean) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = if (isDark) Color(0x16FFFFFF) else Color(0x0D000000)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartOfWeekBottomSheet(
    selectedDay: Int,
    language: AppLanguage,
    isDark: Boolean,
    hapticsEnabled: Boolean,
    onSelectDay: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val isBangla = language == AppLanguage.BANGLA
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBgUnselected = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val cardBgSelected = if (isDark) Color(0xFF0F253E) else Color(0xFFEFF6FF)
    val borderUnselected = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val accentBlue = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .testTag("start_of_week_bottom_sheet")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) "সপ্তাহের শুরু নির্বাচন করুন" else "Start of the Week",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = if (isBangla) {
                            "ক্যালেন্ডার এবং সাপ্তাহিক সারাংশের জন্য প্রথম দিন বেছে নিন"
                        } else {
                            "Choose the first day of the week for calendars and summaries"
                        },
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                SheetHeaderCircleActionButton(
                    onClick = {
                        AppHaptics.performTap(view, hapticsEnabled)
                        onDismiss()
                    },
                    icon = CategorySelectionSheetStyle.HighContrastCloseIcon,
                    contentDescription = "Close",
                    isDark = isDark,
                    testTag = "btn_close_start_of_week_sheet"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                UserSettingsPreferences.allWeekDays.forEach { dayOption ->
                    val isSelected = dayOption.calendarDay == selectedDay
                    Surface(
                        onClick = {
                            AppHaptics.performTap(view, hapticsEnabled)
                            onSelectDay(dayOption.calendarDay)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) cardBgSelected else cardBgUnselected,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accentBlue else borderUnselected
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("start_of_week_option_${dayOption.englishName.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isBangla) dayOption.banglaName else dayOption.englishName,
                                fontSize = 14.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentBlue else textPrimary
                            )

                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentBlue,
                                    unselectedColor = textSecondary
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

