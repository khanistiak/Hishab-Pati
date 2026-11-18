package com.example.ui.screens.settings

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.testTag
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.ui.MainViewModel
import com.example.ui.components.TranslucentCard
import com.example.ui.util.CurrencyFormatter

data class AppColourTheme(
    val option: ThemeColorOption,
    val name: String,
    val subtitle: String,
    val gradient: List<Color>,
    val primaryColor: Color
)

val ColourThemesList = listOf(
    AppColourTheme(
        option = ThemeColorOption.INDIGO_NIGHT,
        name = "Indigo",
        subtitle = "The Hishab Pati brand",
        gradient = listOf(Color(0xFF5B21B6), Color(0xFF6366F1), Color(0xFF7C3AED)),
        primaryColor = Color(0xFF6366F1)
    ),
    AppColourTheme(
        option = ThemeColorOption.EMERALD_GREEN,
        name = "Bangla Green",
        subtitle = "Deep green, red accent",
        gradient = listOf(Color(0xFF047857), Color(0xFF059669)),
        primaryColor = Color(0xFF059669)
    ),
    AppColourTheme(
        option = ThemeColorOption.SKY_CYAN,
        name = "Ocean",
        subtitle = "Calm blues and teal",
        gradient = listOf(Color(0xFF0284C7), Color(0xFF0D9488)),
        primaryColor = Color(0xFF0284C7)
    ),
    AppColourTheme(
        option = ThemeColorOption.SUNSET_ORANGE,
        name = "Sunset",
        subtitle = "Warm orange to coral",
        gradient = listOf(Color(0xFFEA580C), Color(0xFFDC2626)),
        primaryColor = Color(0xFFEA580C)
    ),
    AppColourTheme(
        option = ThemeColorOption.ROSE_PINK,
        name = "Rose",
        subtitle = "Pink to violet",
        gradient = listOf(Color(0xFFE11D48), Color(0xFF9333EA)),
        primaryColor = Color(0xFFE11D48)
    ),
    AppColourTheme(
        option = ThemeColorOption.GRAPHITE,
        name = "Graphite",
        subtitle = "Monochrome and quiet",
        gradient = listOf(Color(0xFF1E293B), Color(0xFF334155)),
        primaryColor = Color(0xFF334155)
    )
)

@Composable
fun ThemeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val currentMode = settings?.themeMode ?: ThemeMode.AUTO
    val currentColor = settings?.themeColor ?: ThemeColorOption.INDIGO_NIGHT
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val userName = settings?.userName ?: "Khan Istiak"

    var useWallpaperColours by remember { mutableStateOf(false) }

    val activeThemeItem = ColourThemesList.firstOrNull { it.option == currentColor } ?: ColourThemesList[0]

    // Determine dark appearance for container backgrounds
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkAppearance = when (currentMode) {
        ThemeMode.AUTO -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    BackHandler {
        onBack()
    }

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.THEME_SETTINGS
    )
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
                .testTag("theme_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // 1. Interactive Live Theme Preview Card (matching Screenshot)
            item {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    cornerRadius = 24.dp,
                    elevation = 2.dp
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview Header: Avatar + Greeting + Bell
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = activeThemeItem.primaryColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "KI",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Good evening,",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Total Balance Mini Card in Selected Theme Gradient
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = activeThemeItem.primaryColor,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(activeThemeItem.gradient))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TOTAL BALANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        letterSpacing = 0.8.sp,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Text(
                                    text = "${currencySymbol}2,48,350.00",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 24.sp
                                    )
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+${currencySymbol}12,400 this month",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // October budget row
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "October budget",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = "62%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = activeThemeItem.primaryColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { 0.62f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = activeThemeItem.primaryColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }

                    // Bottom Navigation Bar Preview Capsule
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = if (isDarkAppearance) Color(0xFF1E293B) else Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = activeThemeItem.primaryColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Home",
                                        tint = activeThemeItem.primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Activity",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )

                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Plan",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )

                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = "Insights",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )

                            // Mini FAB button
                            Surface(
                                shape = CircleShape,
                                color = activeThemeItem.primaryColor,
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(activeThemeItem.gradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. MODE Section (Auto, Light, Dark)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "MODE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // Segmented pill container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkAppearance) Color(0xFF1E293B) else Color(0xFFF1F5F9)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val modeItems = listOf(
                        Triple(ThemeMode.AUTO, "Auto", Icons.Default.AutoAwesome),
                        Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                        Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode)
                    )

                    modeItems.forEach { (mode, label, icon) ->
                        val isSelected = currentMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                if (isDarkAppearance) Color(0xFF334155) else Color.White
                            } else {
                                Color.Transparent
                            },
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            onClick = { viewModel.setThemeMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) {
                                        when (mode) {
                                            ThemeMode.AUTO -> Color(0xFF6366F1)
                                            ThemeMode.LIGHT -> Color(0xFFF59E0B)
                                            ThemeMode.DARK -> Color(0xFF818CF8)
                                        }
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    },
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Subtitle caption below Mode
            Text(
                text = when (currentMode) {
                    ThemeMode.AUTO -> "Auto follows your phone's dark-mode setting."
                    ThemeMode.LIGHT -> "Light mode is always active."
                    ThemeMode.DARK -> "Dark mode is always active."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // 4. COLOUR THEME Section (6 2-column cards matching Screenshot)
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "COLOUR THEME",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ColourThemesList.chunked(2).forEach { rowThemes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowThemes.forEach { themeItem ->
                            val isSelected = currentColor == themeItem.option

                            ColourThemeCard(
                                theme = themeItem,
                                isSelected = isSelected,
                                isDark = isDarkAppearance,
                                onClick = { viewModel.setThemeColor(themeItem.option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowThemes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 5. Wallpaper colours (Material You) Toggle Card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF475569).copy(alpha = 0.85f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ColorLens,
                                contentDescription = "Wallpaper colours",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wallpaper colours",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Takes the colour from your wallpaper (Material You) instead of a theme.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Switch(
                        checked = useWallpaperColours,
                        onCheckedChange = {
                            useWallpaperColours = it
                            viewModel.userNotificationMessage.value = if (it) "Wallpaper colours enabled" else "Theme colours active"
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = activeThemeItem.primaryColor
                        )
                    )
                }
            }
        }

        // 6. Informational Note Card at Bottom
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Money colours never change: income stays green and spending stays red in every theme, so amounts read the same everywhere.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    )
                }
            }
        }
    }

    // Shared Fixed Scroll-Aware Frosted Header for Theme Page
    ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "theme_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("theme_back_button"),
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
                    text = if (settings?.language == com.example.data.model.AppLanguage.BANGLA) "থিম" else "Theme",
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
private fun ColourThemeCard(
    theme: AppColourTheme,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(162.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) Color(0xFF1E293B) else Color.White,
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        border = if (isSelected) BorderStroke(2.dp, theme.primaryColor) else BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Preview Rectangle with Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(theme.gradient))
                    .padding(8.dp)
            ) {
                // Bottom-left preview dots (filled dot + ring dot)
                Row(
                    modifier = Modifier.align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filled white circle
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    // Outlined white circle ring
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .border(1.6.dp, Color.White, CircleShape)
                    )
                }

                // Top-right checkmark circle if selected
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.TopEnd),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = theme.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = theme.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                maxLines = 1
            )

            // Subtitle
            Text(
                text = theme.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                ),
                maxLines = 1
            )
        }
    }
}
