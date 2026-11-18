package com.example.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.util.CurrencyFormatter

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onSkip: () -> Unit = {},
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Optional ViewModel integration for persistent language switching
    val mainViewModel = viewModel ?: runCatching {
        androidx.lifecycle.viewmodel.compose.viewModel<MainViewModel>()
    }.getOrNull()

    val settings by mainViewModel?.settings?.collectAsState() ?: remember { mutableStateOf(null) }

    var currentLanguage by remember(settings?.language) {
        mutableStateOf(settings?.language ?: CurrencyFormatter.currentLanguage)
    }

    val isBangla = currentLanguage == AppLanguage.BANGLA

    val isManagerSigningIn by AuthManager.isSigningInFlow.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val showSigningIn = isLoading || isManagerSigningIn

    androidx.compose.runtime.LaunchedEffect(isManagerSigningIn) {
        if (!isManagerSigningIn && isLoading) {
            isLoading = false
        }
    }


    // Full-screen root container extending edge-to-edge behind system bars
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F1FD))
            .testTag("auth_screen")
    ) {
        // 1. Full-screen background image extending edge-to-edge behind all UI elements
        Image(
            painter = painterResource(id = R.drawable.bg_welcome_screen),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // 2. Overlay UI positioned above the background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Constrain inner content width for responsive presentation on tablets and foldables
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header: Language selector pill placed at the upper-right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, end = 20.dp, start = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Anchor Box strictly wrapping the pill so DropdownMenu opens directly underneath it
                    Box(
                        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 1.5.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showLanguageMenu = true }
                                .testTag("language_selector_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = "Language",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "বাংলা" else "English",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier
                                .background(Color.White)
                                .testTag("language_dropdown_menu")
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "English",
                                        fontWeight = if (!isBangla) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isBangla) Color(0xFF0075FF) else Color(0xFF0F172A)
                                    )
                                },
                                onClick = {
                                    currentLanguage = AppLanguage.ENGLISH
                                    CurrencyFormatter.currentLanguage = AppLanguage.ENGLISH
                                    mainViewModel?.setLanguage(AppLanguage.ENGLISH)
                                    showLanguageMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "বাংলা (Bangla)",
                                        fontWeight = if (isBangla) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isBangla) Color(0xFF0075FF) else Color(0xFF0F172A)
                                    )
                                },
                                onClick = {
                                    currentLanguage = AppLanguage.BANGLA
                                    CurrencyFormatter.currentLanguage = AppLanguage.BANGLA
                                    mainViewModel?.setLanguage(AppLanguage.BANGLA)
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.18f))

                // 2. Branding (NO logo above title)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Hishab Pati",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isBangla) "আপনার অর্থ, আপনার নিয়ন্ত্রণ।" else "Your Money, Your Control.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }

                // 3. Unobstructed open space for the background image's 3D wallet artwork and coin stack
                Spacer(modifier = Modifier.weight(1.50f))

                // 4. Action Buttons & Error Message
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Error message banner if authentication encounters an issue
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { errorText ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Text(
                                    text = errorText,
                                    color = Color(0xFF991B1B),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Primary Blue Button: Google G logo + "Sign in with Google" + right arrow
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("google_sign_in_button"),
                        onClick = {
                            if (!showSigningIn) {
                                isLoading = true
                                errorMessage = null
                                AuthManager.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { error ->
                                        isLoading = false
                                        errorMessage = error
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF007DFE),
                                            Color(0xFF005BF5)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Left-aligned Google G badge & Right-aligned Arrow
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // White circular badge containing official Google G logo
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_google_g),
                                            contentDescription = "Google Logo",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(22.dp)
                                )
                            }

                            // Centered text or loading indicator
                            if (showSigningIn) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBangla) "সাইন ইন হচ্ছে..." else "Signing in...",
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isBangla) "গুগল দিয়ে সাইন ইন করুন" else "Sign in with Google",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(13.dp))

                    // Secondary White Outlined Button: "Skip for now • Use without login" + right arrow
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        shadowElevation = 0.5.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("skip_login_button"),
                        onClick = onSkip
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = if (isBangla) "এখনই এড়িয়ে যান • লগইন ছাড়াই ব্যবহার করুন" else "Skip for now • Use without login",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(0.20f))

                // 5. Footer: Developed by Khan Istiak Shahabuddin / Made with ❤️ in Bangladesh
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    // Compact centered developer credit group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Developed by",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Khan Istiak Shahabuddin",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.2).sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Single centered line with thin horizontal divider lines on both sides
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(1.dp)
                                .background(Color(0xFF94A3B8).copy(alpha = 0.75f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Made with ❤️ in Bangladesh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(1.dp)
                                .background(Color(0xFF94A3B8).copy(alpha = 0.75f))
                        )
                    }
                }
            }
        }
    }
}
