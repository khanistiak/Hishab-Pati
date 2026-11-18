package com.example.ui.screens.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.MainViewModel
import com.example.ui.components.FrostedGlassSurface
import com.example.ui.components.TranslucentCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GoalColorOption(
    val name: String,
    val color: Color,
    val hex: String
)

data class GoalIconItem(
    val name: String,
    val label: String,
    val icon: ImageVector
) {
    constructor(name: String, icon: ImageVector) : this(
        name = name,
        label = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() },
        icon = icon
    )
}

val DefaultGoalThemeColors: List<GoalColorOption> = listOf(
    GoalColorOption("Amber", Color(0xFFF59E0B), "#F59E0B"),
    GoalColorOption("Emerald", Color(0xFF10B981), "#10B981"),
    GoalColorOption("Blue", Color(0xFF3B82F6), "#3B82F6"),
    GoalColorOption("Purple", Color(0xFF8B5CF6), "#8B5CF6"),
    GoalColorOption("Orange", Color(0xFFF97316), "#F97316"),
    GoalColorOption("Rose", Color(0xFFF43F5E), "#F43F5E"),
    GoalColorOption("Cyan", Color(0xFF06B6D4), "#06B6D4"),
    GoalColorOption("Indigo", Color(0xFF6366F1), "#6366F1"),
    GoalColorOption("Teal", Color(0xFF14B8A6), "#14B8A6"),
    GoalColorOption("Pink", Color(0xFFEC4899), "#EC4899"),
    GoalColorOption("Lime", Color(0xFF84CC16), "#84CC16"),
    GoalColorOption("Red", Color(0xFFEF4444), "#EF4444"),
    GoalColorOption("Sky", Color(0xFF0EA5E9), "#0EA5E9"),
    GoalColorOption("Violet", Color(0xFFA855F7), "#A855F7"),
    GoalColorOption("Gold", Color(0xFFEAB308), "#EAB308"),
    GoalColorOption("Slate", Color(0xFF64748B), "#64748B")
)

val DefaultGoalIcons: List<GoalIconItem> = listOf(
    GoalIconItem("savings", "Savings", Icons.Default.Savings),
    GoalIconItem("flight", "Travel", Icons.Default.Flight),
    GoalIconItem("category", "General", Icons.Default.Category),
    GoalIconItem("home", "Home", Icons.Default.Home),
    GoalIconItem("school", "Education", Icons.Default.School),
    GoalIconItem("hospital", "Health", Icons.Default.LocalHospital),
    GoalIconItem("car", "Vehicle", Icons.Default.DirectionsCar),
    GoalIconItem("apartment", "Property", Icons.Default.Apartment),
    GoalIconItem("cart", "Shopping", Icons.Default.ShoppingCart),
    GoalIconItem("restaurant", "Dining", Icons.Default.Restaurant),
    GoalIconItem("laptop", "Laptop", Icons.Default.Laptop),
    GoalIconItem("phone", "Gadget", Icons.Default.Smartphone),
    GoalIconItem("diamond", "Luxury", Icons.Default.Diamond),
    GoalIconItem("gift", "Gift", Icons.Default.CardGiftcard),
    GoalIconItem("fitness", "Fitness", Icons.Default.FitnessCenter),
    GoalIconItem("pets", "Pet", Icons.Default.Pets),
    GoalIconItem("family", "Family", Icons.Default.ChildCare),
    GoalIconItem("beach", "Vacation", Icons.Default.BeachAccess),
    GoalIconItem("bike", "Bike", Icons.AutoMirrored.Filled.DirectionsBike),
    GoalIconItem("music", "Music", Icons.Default.MusicNote),
    GoalIconItem("camera", "Camera", Icons.Default.CameraAlt),
    GoalIconItem("build", "Renovate", Icons.Default.Build),
    GoalIconItem("security", "Emergency", Icons.Default.Security),
    GoalIconItem("trending", "Invest", Icons.AutoMirrored.Filled.TrendingUp)
)

fun resolveGoalIcon(iconName: String): ImageVector {
    return DefaultGoalIcons.firstOrNull { it.name.equals(iconName, ignoreCase = true) }?.icon
        ?: Icons.Default.Savings
}

fun isValidGoalDeadline(candidateMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val localCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val minAllowedMillis = minOf(localCal.timeInMillis, utcCal.timeInMillis)
    return candidateMillis >= minAllowedMillis
}

@Composable
fun NewSavingsGoalScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialCustomPickerDateMillis: Long? = null
) {
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val scrollState = viewModel.rememberScreenScrollState(
        screenOverride = com.example.ui.SubScreen.NEW_SAVINGS_GOAL
    )

    NewSavingsGoalContent(
        currencySymbol = currencySymbol,
        onBack = onBack,
        onCreateGoal = { newGoal ->
            viewModel.insertSavingsGoal(newGoal)
            onBack()
        },
        modifier = modifier,
        initialCustomPickerDateMillis = initialCustomPickerDateMillis,
        scrollState = scrollState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSavingsGoalContent(
    currencySymbol: String = "৳",
    onBack: () -> Unit,
    onCreateGoal: (SavingsGoalEntity) -> Unit,
    modifier: Modifier = Modifier,
    initialCustomPickerDateMillis: Long? = null,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState()
) {

    var goalName by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }

    val themeColors = DefaultGoalThemeColors
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var showAllColorsSheet by remember { mutableStateOf(false) }

    val goalIcons = DefaultGoalIcons
    var selectedIconIndex by remember { mutableIntStateOf(0) }
    var showAllIconsSheet by remember { mutableStateOf(false) }

    var selectedDeadlineMillis by remember { mutableStateOf<Long?>(null) }
    var selectedPresetLabel by remember { mutableStateOf<String?>(null) }
    var deadlineError by remember { mutableStateOf<String?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val deadlineFormatter = remember { SimpleDateFormat("MMMM d, yyyy", Locale.US) }
    val deadlineDisplay = selectedDeadlineMillis?.let { deadlineFormatter.format(Date(it)) } ?: "Set target date"

    val activeColor = themeColors[selectedColorIndex].color
    val activeColorHex = themeColors[selectedColorIndex].hex
    val activeIcon = goalIcons[selectedIconIndex].icon
    val activeIconName = goalIcons[selectedIconIndex].name

    val colorListState = rememberLazyListState()
    val iconListState = rememberLazyListState()

    LaunchedEffect(selectedColorIndex) {
        colorListState.animateScrollToItem(selectedColorIndex)
    }

    LaunchedEffect(selectedIconIndex) {
        iconListState.animateScrollToItem(selectedIconIndex)
    }

    if (showDatePickerDialog) {
        val defaultPickerMillis = remember(selectedDeadlineMillis, initialCustomPickerDateMillis) {
            initialCustomPickerDateMillis ?: selectedDeadlineMillis ?: run {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 30)
                cal.timeInMillis
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = defaultPickerMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMillis = datePickerState.selectedDateMillis
                        if (pickedMillis == null) {
                            deadlineError = "Please select a valid date"
                        } else if (!isValidGoalDeadline(pickedMillis)) {
                            deadlineError = "Deadline cannot be in the past"
                            showDatePickerDialog = false
                        } else {
                            selectedDeadlineMillis = pickedMillis
                            selectedPresetLabel = "Custom date"
                            deadlineError = null
                            showDatePickerDialog = false
                        }
                    },
                    modifier = Modifier.testTag("date_picker_confirm_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedDeadlineMillis != null) {
                        TextButton(
                            onClick = {
                                selectedDeadlineMillis = null
                                selectedPresetLabel = null
                                deadlineError = null
                                showDatePickerDialog = false
                            },
                            modifier = Modifier.testTag("date_picker_clear_button")
                        ) {
                            Text("Clear")
                        }
                    }
                    TextButton(
                        onClick = { showDatePickerDialog = false },
                        modifier = Modifier.testTag("date_picker_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAllColorsSheet) {
        Dialog(onDismissRequest = { showAllColorsSheet = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("all_colors_dialog")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select theme color",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = { showAllColorsSheet = false },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_all_colors_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close color picker"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .testTag("all_colors_grid")
                    ) {
                        itemsIndexed(themeColors) { index, colorOption ->
                            val isSelected = selectedColorIndex == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedColorIndex = index
                                        showAllColorsSheet = false
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp)
                                    .testTag("all_colors_item_${colorOption.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colorOption.color)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    2.5.dp,
                                                    MaterialTheme.colorScheme.onSurface,
                                                    CircleShape
                                                )
                                            } else {
                                                Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    CircleShape
                                                )
                                            }
                                        )
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected ${colorOption.name}",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = colorOption.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        }
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllIconsSheet) {
        Dialog(onDismissRequest = { showAllIconsSheet = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("all_icons_dialog")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select goal icon",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = { showAllIconsSheet = false },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_all_icons_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close icon picker"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .testTag("all_icons_grid")
                    ) {
                        itemsIndexed(goalIcons) { index, iconItem ->
                            val isSelected = selectedIconIndex == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedIconIndex = index
                                        showAllIconsSheet = false
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .testTag("all_icons_item_${iconItem.name}")
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        activeColor.copy(alpha = 0.22f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(1.8.dp, activeColor)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconItem.icon,
                                            contentDescription = iconItem.label,
                                            tint = if (isSelected) {
                                                activeColor
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = iconItem.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        }
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    ScaffoldWithBottomAction(
        onBack = onBack,
        title = "New savings goal",
        bottomAction = {
            Button(
                onClick = {
                    val amount = targetAmountStr.toDoubleOrNull() ?: 0.0
                    val name = if (goalName.isBlank()) "My Savings Goal" else goalName.trim()
                    val validDeadline = selectedDeadlineMillis?.takeIf { isValidGoalDeadline(it) }
                    val validDeadlineStr = if (validDeadline != null) deadlineFormatter.format(Date(validDeadline)) else ""
                    val newGoal = SavingsGoalEntity(
                        id = "goal_${System.currentTimeMillis()}",
                        name = name,
                        targetAmount = amount,
                        savedAmount = 0.0,
                        themeColorHex = activeColorHex,
                        iconName = activeIconName,
                        targetDeadline = validDeadline,
                        targetDeadlineStr = validDeadlineStr,
                        createdAt = System.currentTimeMillis()
                    )
                    onCreateGoal(newGoal)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_goal_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Create goal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .testTag("new_savings_goal_form"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Top Preview Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("goal_preview_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = activeColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, activeColor.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = activeIcon,
                            contentDescription = "Goal Preview: $activeIconName",
                            tint = activeColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Section 1: GOAL DETAILS
            Text(
                text = "GOAL DETAILS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
            )

            TranslucentCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Goal name",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    BasicTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .testTag("goal_name_input"),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (goalName.isEmpty()) {
                                    Text(
                                        text = "e.g. Dream Vacation",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            TranslucentCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Target amount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$currencySymbol ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        BasicTextField(
                            value = targetAmountStr,
                            onValueChange = { targetAmountStr = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .testTag("goal_amount_input"),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (targetAmountStr.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Section 2: CUSTOMIZATION
            Text(
                text = "CUSTOMIZATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
            )

            // Theme color row + Show all
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Theme color",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Show all",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAllColorsSheet = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("theme_color_show_all")
                    )
                }

                LazyRow(
                    state = colorListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_color_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(themeColors) { index, colorOption ->
                        val isSelected = selectedColorIndex == index
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colorOption.color)
                                .clickable { selectedColorIndex = index }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            2.5.dp,
                                            MaterialTheme.colorScheme.onBackground,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            CircleShape
                                        )
                                    }
                                )
                                .testTag("theme_color_option_${colorOption.name.lowercase()}")
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color ${colorOption.name}",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Goal icon row + Show all
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Goal icon",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Show all",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showAllIconsSheet = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("goal_icon_show_all")
                    )
                }

                LazyRow(
                    state = iconListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_icon_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(goalIcons) { index, iconItem ->
                        val isSelected = selectedIconIndex == index
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = if (isSelected) {
                                activeColor.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                            },
                            border = if (isSelected) {
                                BorderStroke(1.8.dp, activeColor)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .clickable { selectedIconIndex = index }
                                .testTag("goal_icon_option_${iconItem.name}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = iconItem.icon,
                                    contentDescription = iconItem.name,
                                    tint = if (isSelected) {
                                        activeColor
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: TIMELINE
            Text(
                text = "TIMELINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
            )

            TranslucentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePickerDialog = true }
                    .testTag("deadline_card"),
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target deadline (optional)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = deadlineDisplay,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (selectedDeadlineMillis != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                }
                            ),
                            modifier = Modifier.testTag("deadline_display_text")
                        )
                    }

                    if (selectedDeadlineMillis != null) {
                        IconButton(
                            onClick = {
                                selectedDeadlineMillis = null
                                selectedPresetLabel = null
                                deadlineError = null
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("clear_deadline_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear deadline",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (deadlineError != null) {
                Text(
                    text = deadlineError!!,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.testTag("deadline_error_text")
                )
            }

            // Quick preset chips + Custom date option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickOptions = listOf(
                    "3 Months" to 90,
                    "6 Months" to 180,
                    "1 Year" to 365
                )
                quickOptions.forEach { (label, days) ->
                    val isSelected = selectedPresetLabel == label && selectedDeadlineMillis != null
                    val tagSuffix = label.lowercase(Locale.US).replace(" ", "_")
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, days)
                            selectedDeadlineMillis = cal.timeInMillis
                            selectedPresetLabel = label
                            deadlineError = null
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("deadline_preset_$tagSuffix"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeColor.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                val isCustomSelected = selectedPresetLabel == "Custom date" && selectedDeadlineMillis != null
                FilterChip(
                    selected = isCustomSelected,
                    onClick = { showDatePickerDialog = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Custom date", fontSize = 12.sp) },
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("deadline_preset_custom_date"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = activeColor.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
fun ScaffoldWithBottomAction(
    onBack: () -> Unit,
    title: String,
    bottomAction: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // Content underneath the fixed frosted bars
        content(
            PaddingValues(
                top = 64.dp + topInset,
                bottom = 76.dp + bottomInset
            )
        )

        // Frosted Fixed Top Bar
        FrostedGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            blurRadius = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("close_goal_screen_button"),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        // Frosted Fixed Bottom Action Bar
        FrostedGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            blurRadius = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                bottomAction()
            }
        }
    }
}
