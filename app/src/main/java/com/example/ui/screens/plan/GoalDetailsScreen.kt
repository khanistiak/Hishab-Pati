package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.SavingsGoalContribution
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.navigation.rememberScreenLazyListState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val GoalRoyalIndigo = Color(0xFF4F46E5)
private val GoalElectricViolet = Color(0xFF7C3AED)
private val GoalAmber = Color(0xFFF59E0B)
private val GoalCoralRose = Color(0xFFF43F5E)

internal fun resolveGoalDeadlineMillis(goal: SavingsGoalEntity): Long? {
    goal.targetDeadline?.let { return it }
    val raw = goal.targetDeadlineStr.trim()
    if (raw.isBlank()) return null
    val patterns = listOf("yyyy-MM-dd", "MMMM d, yyyy", "MMM d, yyyy", "dd MMM yyyy", "dd/MM/yyyy")
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsed = sdf.parse(raw)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
        }
    }
    return null
}

private fun calculateDaysRemaining(deadlineMillis: Long): Int {
    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val deadlineCal = Calendar.getInstance().apply {
        timeInMillis = deadlineMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffMillis = deadlineCal.timeInMillis - todayCal.timeInMillis
    return Math.round(diffMillis / 86_400_000.0).toInt()
}

private enum class GoalFundSheetMode {
    ADD,
    WITHDRAW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    BackHandler(
        enabled = currentSubScreen == SubScreen.GOAL_DETAILS,
        onBack = onBack
    )

    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val selectedGoalState by viewModel.selectedSavingsGoal.collectAsState()
    val allContributions by viewModel.savingsGoalContributions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val liveGoal = savingsGoals.find { it.id == selectedGoalState?.id } ?: selectedGoalState
    var lastKnownGoal by remember { mutableStateOf(liveGoal) }
    if (liveGoal != null) {
        lastKnownGoal = liveGoal
    }
    val goal = liveGoal ?: lastKnownGoal

    if (goal == null) {
        LaunchedEffect(currentSubScreen) {
            if (currentSubScreen == SubScreen.GOAL_DETAILS) {
                onBack()
            }
        }
        return
    }

    var activeSheetMode by remember { mutableStateOf<GoalFundSheetMode?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val activeAccounts = remember(accounts) {
        accounts.filter { !it.isHidden }
    }

    val goalContributions = remember(allContributions, goal.id) {
        allContributions
            .filter { it.goalId == goal.id }
            .sortedByDescending { it.timestamp }
    }

    val goalColor = remember(goal.themeColorHex) {
        try {
            Color(android.graphics.Color.parseColor(goal.themeColorHex))
        } catch (_: Exception) {
            GoalAmber
        }
    }

    val targetAmount = goal.targetAmount.coerceAtLeast(0.0)
    val savedAmount = goal.savedAmount.coerceAtLeast(0.0)
    val remainingAmount = (targetAmount - savedAmount).coerceAtLeast(0.0)
    val rawProgress = if (targetAmount > 0.0) (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 450),
        label = "goal_progress_animation"
    )
    val pct = if (targetAmount > 0.0) ((savedAmount / targetAmount) * 100.0).toInt().coerceIn(0, 999) else 0
    val isCompleted = targetAmount > 0.0 && savedAmount >= targetAmount

    val deadlineMillis = remember(goal.targetDeadline, goal.targetDeadlineStr) {
        resolveGoalDeadlineMillis(goal)
    }
    val daysRemaining = remember(deadlineMillis) {
        deadlineMillis?.let { calculateDaysRemaining(it) }
    }
    val deadlineDisplayStr = remember(goal.targetDeadlineStr, deadlineMillis, language) {
        val base = when {
            goal.targetDeadlineStr.isNotBlank() -> goal.targetDeadlineStr
            deadlineMillis != null -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(deadlineMillis))
            else -> ""
        }
        if (base.isBlank()) "" else DateFormatter.localizeText(base, language)
    }

    val monthlyPace = remember(remainingAmount, daysRemaining) {
        if (remainingAmount <= 0.0 || daysRemaining == null || daysRemaining <= 0) null
        else if (daysRemaining <= 30) remainingAmount
        else remainingAmount / (daysRemaining / 30.0)
    }
    val weeklyPace = remember(remainingAmount, daysRemaining) {
        if (remainingAmount <= 0.0 || daysRemaining == null || daysRemaining <= 0) null
        else if (daysRemaining <= 7) remainingAmount
        else remainingAmount / (daysRemaining / 7.0)
    }

    val indigoVioletGradient = remember {
        Brush.horizontalGradient(listOf(GoalRoyalIndigo, GoalElectricViolet))
    }

    val topBarBg = if (isDark) Color(0xEB0E1426) else Color(0xEBFFFFFF)
    val cardBorderColor = if (isDark) Color(0x28A5B4FC) else Color(0x1A6366F1)
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = viewModel.rememberScreenLazyListState(
        subKey = goal.id,
        screenOverride = SubScreen.GOAL_DETAILS
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 64.dp,
        extraTopGap = 8.dp,
        bottomNavClearance = 28.dp
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = if (isDark) CardBackgroundDark else MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = AppStrings.t("delete_goal_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = AppStrings.t("delete_goal_body", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSavingsGoal(goal.id)
                        onBack()
                    },
                    modifier = Modifier.testTag("goal_delete_confirm_button")
                ) {
                    Text(
                        text = AppStrings.t("delete", language),
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    modifier = Modifier.testTag("goal_delete_cancel_button")
                ) {
                    Text(text = AppStrings.t("cancel", language))
                }
            }
        )
    }

    activeSheetMode?.let { mode ->
        GoalFundAdjustmentSheet(
            mode = mode,
            goal = goal,
            accounts = activeAccounts,
            currencySymbol = currencySymbol,
            language = language,
            isDark = isDark,
            indigoVioletGradient = indigoVioletGradient,
            onDismiss = { activeSheetMode = null },
            onConfirm = { amount, note, selectedWallet ->
                viewModel.recordSavingsGoalEntry(
                    goalId = goal.id,
                    amount = amount,
                    isWithdrawal = mode == GoalFundSheetMode.WITHDRAW,
                    note = note,
                    account = selectedWallet
                )
                activeSheetMode = null
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("goal_details_screen"),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostedBackdrop(
                        scrollLayout.backdropState,
                        listState,
                        scrollLayout.contentGraphicsLayer
                    )
                    .testTag("goal_details_lazy_column"),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = scrollLayout.contentPadding.calculateTopPadding(),
                    bottom = scrollLayout.contentPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 1. Hero Progress Bento Card
            item(key = "hero_progress_card") {
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_hero_bento_card"),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Squircle Goal Icon
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = goalColor.copy(alpha = if (isDark) 0.22f else 0.16f),
                                border = BorderStroke(1.5.dp, goalColor.copy(alpha = 0.45f)),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = resolveGoalIcon(goal.iconName),
                                        contentDescription = goal.name,
                                        tint = goalColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.t("savings_progress", language),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.format(savedAmount, currencySymbol, language = language),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("goal_saved_amount_text")
                                )
                                Text(
                                    text = "/ ${CurrencyFormatter.format(targetAmount, currencySymbol, language = language)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.testTag("goal_target_amount_text")
                                )
                            }

                            // Completion Percentage Badge
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCompleted) {
                                    IncomeGreen.copy(alpha = if (isDark) 0.22f else 0.14f)
                                } else {
                                    goalColor.copy(alpha = if (isDark) 0.22f else 0.14f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    (if (isCompleted) IncomeGreen else goalColor).copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.testTag("goal_completion_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    val pctLabel = CurrencyFormatter.formatPercent(pct, language)
                                    Text(
                                        text = pctLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (isCompleted) IncomeGreen else goalColor
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Smooth Progress Bar
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .testTag("goal_progress_bar"),
                            color = if (isCompleted) IncomeGreen else goalColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = if (isDark) 0.5f else 0.85f
                            ),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Remaining amount + Status row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val remainingLabel = if (isCompleted) {
                                AppStrings.t("goal_completed", language)
                            } else {
                                "${CurrencyFormatter.format(remainingAmount, currencySymbol, language = language)} ${AppStrings.t("left_suffix", language)}"
                            }
                            Text(
                                text = remainingLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) IncomeGreen else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("goal_remaining_text")
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCompleted) {
                                    IncomeGreen.copy(alpha = 0.14f)
                                } else {
                                    GoalRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.10f)
                                }
                            ) {
                                Text(
                                    text = if (isCompleted) {
                                        AppStrings.t("goal_completed", language)
                                    } else {
                                        AppStrings.t("goal_in_progress", language)
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCompleted) {
                                            IncomeGreen
                                        } else if (isDark) {
                                            Color(0xFFA5B4FC)
                                        } else {
                                            GoalRoyalIndigo
                                        }
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Deadline + Suggested Pace Bento sub-tiles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Target Deadline Tile
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) {
                                    Color.White.copy(alpha = 0.05f)
                                } else {
                                    GoalRoyalIndigo.copy(alpha = 0.05f)
                                },
                                border = BorderStroke(1.dp, cardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = GoalAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = AppStrings.t("target_deadline", language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val dueDisplay = if (deadlineDisplayStr.isNotBlank()) {
                                        if (isBn) "মেয়াদ: $deadlineDisplayStr" else "Due: $deadlineDisplayStr"
                                    } else {
                                        AppStrings.t("no_deadline_set", language)
                                    }
                                    Text(
                                        text = dueDisplay,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("goal_deadline_text")
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val daysChipText = when {
                                        daysRemaining == null -> if (isBn) "ঐচ্ছিক" else "Optional"
                                        daysRemaining > 1 -> "${CurrencyFormatter.formatNumber(daysRemaining, language)} ${AppStrings.t("days_left", language)}"
                                        daysRemaining == 1 -> "${CurrencyFormatter.formatNumber(1, language)} ${AppStrings.t("day_left", language)}"
                                        daysRemaining == 0 -> AppStrings.t("due_today", language)
                                        else -> "${AppStrings.t("overdue_by", language)} ${CurrencyFormatter.formatNumber(abs(daysRemaining), language)} ${AppStrings.t("days_label", language)}"
                                    }
                                    val daysChipColor = when {
                                        daysRemaining == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        daysRemaining < 0 && !isCompleted -> GoalCoralRose
                                        daysRemaining <= 14 && !isCompleted -> GoalAmber
                                        else -> if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo
                                    }
                                    Text(
                                        text = daysChipText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = daysChipColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("goal_days_remaining_text")
                                    )
                                }
                            }

                            // Suggested Saving Pace Tile
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) {
                                    Color.White.copy(alpha = 0.05f)
                                } else {
                                    GoalElectricViolet.copy(alpha = 0.05f)
                                },
                                border = BorderStroke(1.dp, cardBorderColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFC4B5FD) else GoalElectricViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = AppStrings.t("suggested_pace", language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    when {
                                        isCompleted -> {
                                            Text(
                                                text = AppStrings.t("goal_completed", language),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = IncomeGreen
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = CurrencyFormatter.format(0.0, currencySymbol, language = language) + " " + AppStrings.t("left_suffix", language),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                        monthlyPace != null && weeklyPace != null -> {
                                            Text(
                                                text = "${CurrencyFormatter.format(monthlyPace, currencySymbol, language = language)} ${AppStrings.t("per_month_short", language)}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.testTag("goal_monthly_pace_text")
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "${CurrencyFormatter.format(weeklyPace, currencySymbol, language = language)} ${AppStrings.t("per_week_short", language)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isDark) Color(0xFFC4B5FD) else GoalElectricViolet,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.testTag("goal_weekly_pace_text")
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = AppStrings.t("set_deadline_for_pace", language),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 15.sp
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Add / Withdraw Funds Bento Action Row
            item(key = "fund_actions_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // + Add Savings (Royal Indigo-to-Electric Violet Gradient)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(indigoVioletGradient)
                            .clickable { activeSheetMode = GoalFundSheetMode.ADD }
                            .testTag("goal_details_add_savings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = AppStrings.t("add_savings", language),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // - Withdraw Bento Action
                    val canWithdraw = savedAmount > 0.0
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .alpha(if (canWithdraw) 1f else 0.48f)
                            .testTag("goal_details_withdraw_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDark) {
                            CardBackgroundDark.copy(alpha = 0.85f)
                        } else {
                            Color.White.copy(alpha = 0.92f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (canWithdraw) {
                                GoalCoralRose.copy(alpha = if (isDark) 0.45f else 0.35f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            }
                        ),
                        onClick = {
                            if (canWithdraw) {
                                activeSheetMode = GoalFundSheetMode.WITHDRAW
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = null,
                                tint = GoalCoralRose,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.t("withdraw_funds", language),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GoalCoralRose
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. Contribution / Activity History Header
            item(key = "contribution_history_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = AppStrings.t("contribution_history", language),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    val count = goalContributions.size
                    val countText = "${CurrencyFormatter.formatNumber(count, language)} ${
                        if (count == 1) AppStrings.t("entry_count_singular", language)
                        else AppStrings.t("entries_count_suffix", language)
                    }"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            GoalRoyalIndigo.copy(alpha = 0.08f)
                        }
                    ) {
                        Text(
                            text = countText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 4. Contribution / Activity History List or Empty State
            if (goalContributions.isEmpty()) {
                item(key = "contribution_history_empty") {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_contributions_empty_card"),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = GoalRoyalIndigo.copy(alpha = if (isDark) 0.22f else 0.12f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = AppStrings.t("no_contributions_yet", language),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = AppStrings.t("no_contributions_hint", language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                    lineHeight = 18.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            if (savedAmount > 0.0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = goalColor.copy(alpha = if (isDark) 0.18f else 0.10f)
                                ) {
                                    Text(
                                        text = "${AppStrings.t("already_saved_note", language)}: ${CurrencyFormatter.format(savedAmount, currencySymbol, language = language)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = goalColor
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(goalContributions, key = { it.id }) { entry ->
                    GoalContributionHistoryCard(
                        entry = entry,
                        currencySymbol = currencySymbol,
                        language = language,
                        isDark = isDark
                    )
                }
            }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "goal_details_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        GoalRoyalIndigo.copy(alpha = 0.08f)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("goal_details_back_button"),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.t("back", language),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("goal_details_title")
                    )
                    Text(
                        text = AppStrings.t("savings_goal_subtitle", language),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = if (isDark) {
                        Color.White.copy(alpha = 0.08f)
                    } else {
                        GoalRoyalIndigo.copy(alpha = 0.08f)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("goal_details_edit_button"),
                    onClick = { viewModel.openEditGoal(goal) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = AppStrings.t("edit", language),
                            tint = if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = GoalCoralRose.copy(alpha = if (isDark) 0.16f else 0.10f),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("goal_details_delete_button"),
                    onClick = { showDeleteConfirm = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppStrings.t("delete", language),
                            tint = GoalCoralRose,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun GoalContributionHistoryCard(
    entry: SavingsGoalContribution,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean
) {
    val isWithdrawal = entry.isWithdrawal
    val accentColor = if (isWithdrawal) ExpenseRed else IncomeGreen
    val title = entry.note.ifBlank {
        if (isWithdrawal) AppStrings.t("savings_withdrawal", language)
        else AppStrings.t("savings_deposit", language)
    }
    val dateStr = DateFormatter.formatFull(entry.timestamp, language)
    val signedAmount = CurrencyFormatter.format(
        amount = if (isWithdrawal) -entry.amount else entry.amount,
        symbol = currencySymbol,
        showSign = true,
        language = language
    )

    TranslucentCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_contribution_item_${entry.id}"),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = accentColor.copy(alpha = if (isDark) 0.20f else 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isWithdrawal) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.walletName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) {
                                Color.White.copy(alpha = 0.08f)
                            } else {
                                GoalRoyalIndigo.copy(alpha = 0.08f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = entry.walletName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFA5B4FC) else GoalRoyalIndigo
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = signedAmount,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accentColor
                ),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalFundAdjustmentSheet(
    mode: GoalFundSheetMode,
    goal: SavingsGoalEntity,
    accounts: List<AccountEntity>,
    currencySymbol: String,
    language: AppLanguage,
    isDark: Boolean,
    indigoVioletGradient: Brush,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, account: AccountEntity?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isWithdraw = mode == GoalFundSheetMode.WITHDRAW

    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickAmounts = remember(isWithdraw) {
        if (isWithdraw) listOf(500.0, 1000.0, 5000.0)
        else listOf(500.0, 1000.0, 5000.0, 10000.0)
    }

    val primaryActionColor = if (isWithdraw) GoalCoralRose else GoalRoyalIndigo

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) CardBackgroundDark else Color(0xFFFDFDFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .padding(bottom = 20.dp)
                .testTag(if (isWithdraw) "withdraw_funds_sheet" else "add_savings_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Compact Header with Saved Progress Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isWithdraw) {
                            AppStrings.t("withdraw_funds_title", language)
                        } else {
                            AppStrings.t("add_savings", language)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primaryActionColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                    border = BorderStroke(1.dp, primaryActionColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${AppStrings.t("saved", language)}: ${CurrencyFormatter.format(goal.savedAmount, currencySymbol, language = language)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryActionColor
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("goal_fund_sheet_close_button"),
                    onClick = onDismiss
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = AppStrings.t("close", language),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Centered Tabular-Numeral Amount Card with Quick-Add Glass Chips
            TranslucentCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            errorMessage = null
                        },
                        label = { Text(AppStrings.t("amount", language)) },
                        prefix = {
                            Text(
                                text = "$currencySymbol ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = primaryActionColor
                            )
                        },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { msg ->
                            { Text(text = msg, color = ExpenseRed) }
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Start
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryActionColor,
                            focusedLabelColor = primaryActionColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("goal_fund_amount_input")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickAmounts) { preset ->
                            val label = if (isWithdraw) {
                                CurrencyFormatter.format(preset, currencySymbol, language = language)
                            } else {
                                "+${CurrencyFormatter.format(preset, currencySymbol, language = language)}"
                            }
                            FilterChip(
                                selected = CurrencyFormatter.parseLocalizedDouble(amountInput) == preset,
                                onClick = {
                                    amountInput = preset.toLong().toString()
                                    errorMessage = null
                                },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryActionColor.copy(alpha = 0.2f),
                                    selectedLabelColor = primaryActionColor
                                )
                            )
                        }
                        if (isWithdraw && goal.savedAmount > 0.0) {
                            item(key = "withdraw_all_chip") {
                                FilterChip(
                                    selected = CurrencyFormatter.parseLocalizedDouble(amountInput) == goal.savedAmount,
                                    onClick = {
                                        val maxStr = if (goal.savedAmount % 1.0 == 0.0) {
                                            goal.savedAmount.toLong().toString()
                                        } else {
                                            String.format(Locale.US, "%.2f", goal.savedAmount)
                                        }
                                        amountInput = maxStr
                                        errorMessage = null
                                    },
                                    label = {
                                        Text(
                                            text = AppStrings.t("max_label", language),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoalCoralRose.copy(alpha = 0.2f),
                                        selectedLabelColor = GoalCoralRose
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Bento Wallet Selector with Live Balances
            if (accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = AppStrings.t("select_wallet_optional", language),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "wallet_none") {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedAccount == null) {
                                    primaryActionColor.copy(alpha = if (isDark) 0.25f else 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.4f else 0.6f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedAccount == null) primaryActionColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.clickable { selectedAccount = null }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = if (selectedAccount == null) primaryActionColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = AppStrings.t("no_wallet_direct", language),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (selectedAccount == null) primaryActionColor else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Text(
                                            text = AppStrings.t("direct_unbound", language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        items(accounts, key = { it.id }) { acc ->
                            val isSelected = selectedAccount?.id == acc.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    primaryActionColor.copy(alpha = if (isDark) 0.25f else 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.4f else 0.6f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) primaryActionColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.clickable { selectedAccount = acc }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (isSelected) primaryActionColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) primaryActionColor else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(acc.balance, currencySymbol, language = language),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Glass Note Card
            TranslucentCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(AppStrings.t("note_optional", language)) },
                    placeholder = { Text(AppStrings.t("contribution_note_placeholder", language)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryActionColor,
                        focusedLabelColor = primaryActionColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("goal_fund_note_input")
                )
            }

            // 5. Full-Width Gradient CTA Button
            val submitAction = {
                val parsed = CurrencyFormatter.parseLocalizedDouble(amountInput)
                when {
                    parsed == null || parsed <= 0.0 -> {
                        errorMessage = AppStrings.t("invalid_amount_error", language)
                    }
                    isWithdraw && parsed > goal.savedAmount + 0.0001 -> {
                        errorMessage = AppStrings.t("withdraw_exceeds_saved_error", language)
                    }
                    else -> {
                        onConfirm(parsed, noteInput.trim(), selectedAccount)
                    }
                }
            }

            if (isWithdraw) {
                Button(
                    onClick = submitAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("goal_fund_confirm_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoalCoralRose,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.t("withdraw_funds_title", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(indigoVioletGradient)
                        .clickable(onClick = submitAction)
                        .testTag("goal_fund_confirm_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = AppStrings.t("add_savings", language),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
