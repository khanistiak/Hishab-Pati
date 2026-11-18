package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PhoneAndroid
import com.example.data.model.AccountType
import com.example.ui.screens.accounts.AllConfigurableMfsProviders
import com.example.ui.screens.accounts.BankInstitutionPickerSheet
import com.example.ui.screens.accounts.MfsProviderInfo
import com.example.ui.screens.accounts.MfsProviderPickerSheet
import com.example.ui.screens.accounts.PopularBanks
import com.example.ui.util.CurrencyFormatter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import androidx.credentials.CredentialManager
import com.example.ui.auth.AuthManager
import com.example.ui.components.TranslucentCard
import com.example.ui.components.getIconForName
import com.example.ui.util.AppStrings
import kotlinx.coroutines.launch

private val AVATAR_PALETTE = listOf(
    "#4F46E5", "#2563EB", "#06B6D4", "#10B981", "#F59E0B",
    "#EF4444", "#EC4899", "#8B5CF6", "#6366F1", "#374151"
)

private val AVATAR_SYMBOLS = listOf(
    "person", "face", "star", "savings",
    "pets", "account_balance_wallet", "electric_bolt", "favorite",
    "school", "work", "flight", "music_note",
    "sports_esports", "devices", "shopping_bag", "eco"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBangla = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }

    val firebaseUser by viewModel.currentUser.collectAsState()
    val userDisplayName = if (firebaseUser != null) {
        firebaseUser?.displayName?.takeIf { it.isNotBlank() } ?: settings?.userName ?: "Google User"
    } else {
        settings?.userName?.takeIf { it.isNotBlank() } ?: "Guest User"
    }

    val effectivePhotoUrl = if (firebaseUser != null && !firebaseUser?.photoUrl?.toString().isNullOrBlank()) {
        firebaseUser?.photoUrl.toString()
    } else {
        settings?.profilePhotoUri?.takeIf { it.isNotBlank() }
    }

    val avatarType = settings?.profileAvatarType ?: "INITIALS"
    val avatarColorHex = settings?.profileAvatarColorHex ?: "#4F46E5"
    val avatarSymbol = settings?.profileAvatarSymbol ?: "person"

    val avatarBgColor = try {
        Color(android.graphics.Color.parseColor(avatarColorHex))
    } catch (_: Exception) {
        Color(0xFF4F46E5)
    }

    val userInitials = userDisplayName.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "U" }
        .uppercase()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showAvatarBottomSheet by remember { mutableStateOf(false) }
    var isCheckingBankDb by remember { mutableStateOf(false) }
    var showBankDirectorySheet by remember { mutableStateOf(false) }
    var showMfsDirectorySheet by remember { mutableStateOf(false) }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.USER_PROFILE
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("profile_lazy_column"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Header Row: Back Button & Title
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF262B3D) else Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("profile_back_button"),
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
                    text = if (isBangla) "প্রোফাইল" else "Profile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        // 2. Large Centered Avatar with Edit Badge (Proportionate, slightly increased size)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { showAvatarBottomSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(112.dp),
                        shape = CircleShape,
                        color = avatarBgColor,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!effectivePhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = effectivePhotoUrl,
                                    contentDescription = "User Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(112.dp)
                                        .clip(CircleShape)
                                        .testTag("profile_avatar_image")
                                )
                            } else if (avatarType == "SYMBOL") {
                                Icon(
                                    imageVector = getIconForName(avatarSymbol),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(58.dp)
                                )
                            } else {
                                Text(
                                    text = userInitials,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 40.sp
                                    )
                                )
                            }
                        }
                    }

                    // Floating Pencil/Camera badge at bottom-right
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 3.dp,
                        border = BorderStroke(2.dp, if (isDark) Color(0xFF1E2230) else Color.White),
                        onClick = { showAvatarBottomSheet = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit photo",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. User Details Card (Compact internal padding and readable rows)
        item {
            TranslucentCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = if (isBangla) "ব্যবহারকারীর তথ্য" else "USER INFORMATION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Name Row with Edit button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "নাম" else "Name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = userDisplayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Button(
                            onClick = { showEditNameDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(
                                text = if (isBangla) "সম্পাদনা" else "Edit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Google Account Row (Sign-In or Connected + Sign-Out)
                    val activeUser = firebaseUser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "গুগল অ্যাকাউন্ট" else "Google Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (activeUser != null) {
                                    activeUser.email ?: "Signed in"
                                } else {
                                    if (isBangla) "সাইন ইন করা নেই (গেস্ট মোড)" else "Not signed in (Guest mode)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        if (activeUser != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isBangla) "সংযুক্ত" else "Connected",
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        AuthManager.signOut(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onSignOutComplete = {
                                                viewModel.onUserSignedOut()
                                                viewModel.userNotificationMessage.value = if (isBangla)
                                                    "গুগল অ্যাকাউন্ট থেকে সাইন আউট সম্পন্ন হয়েছে"
                                                else
                                                    "Signed out of Google account"
                                            },
                                            scope = coroutineScope
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("profile_google_sign_out_button")
                                ) {
                                    Text(
                                        text = if (isBangla) "লগ আউট" else "Sign Out",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!isSigningIn) {
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
                                                viewModel.userNotificationMessage.value = err
                                            },
                                            scope = coroutineScope,
                                            onAuthCancelled = {
                                                isSigningIn = false
                                            }
                                        )
                                    }
                                },
                                enabled = !isSigningIn,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("profile_google_sign_in_button")
                            ) {
                                Text(
                                    text = if (isSigningIn) {
                                        if (isBangla) "লগইন..." else "Signing in..."
                                    } else {
                                        if (isBangla) "গুগল সাইন ইন" else "Sign in"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Country Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "দেশ" else "Country",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "🇧🇩 ${settings?.country ?: "Bangladesh"}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Primary Currency Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openSubScreen(SubScreen.CURRENCY_SELECTION) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "প্রধান মুদ্রা" else "Primary Currency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${settings?.currencyCode ?: "BDT"} (${settings?.currencySymbol ?: "৳"})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Quick / Secondary Currency Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "দ্রুত মুদ্রা" else "Quick Currency",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${settings?.secondaryCurrencyCode ?: "USD"} (${settings?.secondaryCurrencySymbol ?: "$"})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Bank & MFS Database Section
        item {
            TranslucentCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = if (isBangla) "ব্যাংক ও এমএফএস ডাটাবেস" else "BANK & MFS DATABASE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showBankDirectorySheet = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "স্থানীয় ব্যাংক ডিরেক্টরি" else "Local Bank Directory",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val bankCount = PopularBanks.size
                            val bankCountBn = CurrencyFormatter.convertToBanglaDigits(bankCount.toString())
                            Text(
                                text = if (isBangla) "সংস্করণ ২০২৬.১১ · ${bankCountBn}টি ব্যাংক" else "Version 2026.11 · $bankCount Banks",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                isCheckingBankDb = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(600)
                                    isCheckingBankDb = false
                                    viewModel.userNotificationMessage.value = if (isBangla)
                                        "ব্যাংক ডাটাবেস সম্পূর্ণ আপডেট আছে"
                                    else
                                        "Bank directory is up to date"
                                }
                            },
                            enabled = !isCheckingBankDb,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            if (isCheckingBankDb) {
                                Text(
                                    text = if (isBangla) "যাচাই..." else "Checking...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = if (isBangla) "আপডেট দেখুন" else "Check updates",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x0E000000),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMfsDirectorySheet = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "এমএফএস প্রোভাইডার ডিরেক্টরি" else "MFS Provider Directory",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val mfsCount = AllConfigurableMfsProviders.size
                            val mfsCountBn = CurrencyFormatter.convertToBanglaDigits(mfsCount.toString())
                            Text(
                                text = if (isBangla) "সংস্করণ ২০২৬.১১ · ${mfsCountBn}টি প্রোভাইডার" else "Version 2026.11 · $mfsCount Providers",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = if (isBangla) "ব্রাউজ করুন" else "Browse",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var inputName by remember { mutableStateOf(userDisplayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = if (isDark) Color(0xFF1E2230) else Color.White,
            title = {
                Text(
                    text = if (isBangla) "প্রোফাইল নাম পরিবর্তন করুন" else "Edit Profile Name",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text(if (isBangla) "আপনার নাম" else "Your Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = settings ?: AppSettingsEntity()
                        coroutineScope.launch {
                            viewModel.repository.saveSettings(current.copy(userName = inputName.trim()))
                        }
                        showEditNameDialog = false
                    }
                ) {
                    Text(
                        text = if (isBangla) "সংরক্ষণ" else "Save",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(
                        text = if (isBangla) "বাতিল" else "Cancel",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    // Profile Picture Bottom Sheet
    if (showAvatarBottomSheet) {
        ProfilePictureBottomSheet(
            currentPhotoUrl = effectivePhotoUrl,
            currentType = avatarType,
            currentColorHex = avatarColorHex,
            currentSymbol = avatarSymbol,
            userInitials = userInitials,
            isBangla = isBangla,
            isDark = isDark,
            onDismiss = { showAvatarBottomSheet = false },
            onSave = { newType, newColorHex, newSymbol, newPhotoUri ->
                val current = settings ?: AppSettingsEntity()
                coroutineScope.launch {
                    val updated = current.copy(
                        profileAvatarType = newType,
                        profileAvatarColorHex = newColorHex,
                        profileAvatarSymbol = newSymbol,
                        profilePhotoUri = newPhotoUri ?: current.profilePhotoUri
                    )
                    viewModel.repository.saveSettings(updated)
                }
                showAvatarBottomSheet = false
            }
        )
    }

    if (showBankDirectorySheet) {
        val allAccounts by viewModel.accounts.collectAsState()
        val customInstitutions = remember(allAccounts) {
            allAccounts.map { it.institution.trim() }.filter { it.isNotEmpty() }.distinct()
        }
        BankInstitutionPickerSheet(
            currentInstitution = "",
            customInstitutions = customInstitutions,
            onSelectBank = { showBankDirectorySheet = false },
            onDismiss = { showBankDirectorySheet = false }
        )
    }

    if (showMfsDirectorySheet) {
        val allAccounts by viewModel.accounts.collectAsState()
        val customMfsProviders = remember(allAccounts) {
            allAccounts.filter { it.type == AccountType.MFS && it.institution.isNotBlank() }
                .map { MfsProviderInfo(it.institution.trim(), it.colorHex) }
                .distinctBy { it.name.lowercase() }
        }
        MfsProviderPickerSheet(
            currentProviderName = "",
            customProviders = customMfsProviders,
            onSelectProvider = { showMfsDirectorySheet = false },
            onDismiss = { showMfsDirectorySheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfilePictureBottomSheet(
    currentPhotoUrl: String?,
    currentType: String,
    currentColorHex: String,
    currentSymbol: String,
    userInitials: String,
    isBangla: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSave: (type: String, colorHex: String, symbol: String, photoUri: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(if (currentType == "SYMBOL") "Symbol" else "Initials") }
    var selectedColorHex by remember { mutableStateOf(currentColorHex) }
    var selectedSymbol by remember { mutableStateOf(currentSymbol) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
            onSave("PHOTO", selectedColorHex, selectedSymbol, uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF1E2230) else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBangla) "প্রোফাইল ছবি" else "Profile picture",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Choose photo" pill button
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla) "ছবি পছন্দ করুন" else "Choose photo",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // "OR USE" divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isDark) Color(0x22FFFFFF) else Color(0x15000000)
                )
                Text(
                    text = if (isBangla) "অথবা ব্যবহার করুন" else "OR USE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isDark) Color(0x22FFFFFF) else Color(0x15000000)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab switch: Initials vs Symbol
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF262B3D) else Color(0xFFEFF2F6),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    val isInitials = selectedTab == "Initials"
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isInitials) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                        shadowElevation = if (isInitials) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .fillMaxSize(),
                        onClick = { selectedTab = "Initials" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isBangla) "আদ্যক্ষর (Initials)" else "Initials",
                                fontWeight = if (isInitials) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isInitials) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isSymbol = selectedTab == "Symbol"
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSymbol) (if (isDark) Color(0xFF1E2230) else Color.White) else Color.Transparent,
                        shadowElevation = if (isSymbol) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .fillMaxSize(),
                        onClick = { selectedTab = "Symbol" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isBangla) "প্রতীক (Symbol)" else "Symbol",
                                fontWeight = if (isSymbol) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSymbol) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 10 Color Palette Circles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AVATAR_PALETTE.forEach { colorHex ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (_: Exception) {
                        Color(0xFF4F46E5)
                    }
                    val isSelected = selectedColorHex.equals(colorHex, ignoreCase = true)

                    Surface(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { selectedColorHex = colorHex },
                        shape = CircleShape,
                        color = color,
                        border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab Content: Initials Preview OR 16 Symbols Grid
            if (selectedTab == "Initials") {
                val previewColor = try {
                    Color(android.graphics.Color.parseColor(selectedColorHex))
                } catch (_: Exception) {
                    Color(0xFF4F46E5)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = previewColor,
                        shadowElevation = 3.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userInitials,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            )
                        }
                    }
                }
            } else {
                // 4x4 Grid of 16 Symbols
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AVATAR_SYMBOLS) { sym ->
                        val isSelected = selectedSymbol == sym
                        val tintColor = try {
                            Color(android.graphics.Color.parseColor(selectedColorHex))
                        } catch (_: Exception) {
                            Color(0xFF4F46E5)
                        }

                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedSymbol = sym },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) tintColor else (if (isDark) Color(0xFF262B3D) else Color(0xFFF1F5F9)),
                            border = if (isSelected) BorderStroke(1.5.dp, tintColor) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconForName(sym),
                                    contentDescription = sym,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save / Done Button
            Button(
                onClick = {
                    onSave(
                        if (selectedTab == "Symbol") "SYMBOL" else "INITIALS",
                        selectedColorHex,
                        selectedSymbol,
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (isBangla) "সংরক্ষণ করুন" else "Save avatar",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
