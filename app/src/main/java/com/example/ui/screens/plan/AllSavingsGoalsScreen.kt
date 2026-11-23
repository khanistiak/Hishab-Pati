package com.example.ui.screens.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.model.AppLanguage
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.navigation.rememberNavigationScopeKey
import com.example.ui.theme.CardBackgroundDark
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

enum class SavingsGoalFilterTab(val label: String, val bnLabel: String) {
    ALL("All", "সব"),
    IN_PROGRESS("In Progress", "চলমান"),
    COMPLETED("Completed", "সম্পন্ন")
}

@Composable
fun AllSavingsGoalsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    BackHandler(enabled = currentSubScreen == SubScreen.ALL_SAVINGS_GOALS, onBack = onBack)

    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var selectedFilter by remember { mutableStateOf(SavingsGoalFilterTab.ALL) }

    val totalSaved = remember(savingsGoals) { savingsGoals.sumOf { it.savedAmount } }
    val totalTarget = remember(savingsGoals) { savingsGoals.sumOf { it.targetAmount } }
    val overallProgress = if (totalTarget > 0.0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
    val overallPct = (overallProgress * 100).roundToInt()
    val totalRemaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
    val activeGoalsCount = remember(savingsGoals) { savingsGoals.count { !it.isCompleted && it.savedAmount < it.targetAmount } }

    val filteredGoals = remember(savingsGoals, selectedFilter) {
        when (selectedFilter) {
            SavingsGoalFilterTab.ALL -> savingsGoals
            SavingsGoalFilterTab.IN_PROGRESS -> savingsGoals.filter { !it.isCompleted && it.savedAmount < it.targetAmount }
            SavingsGoalFilterTab.COMPLETED -> savingsGoals.filter { it.isCompleted || it.savedAmount >= it.targetAmount }
        }
    }

    val navScopeKey = rememberNavigationScopeKey(
        viewModel = viewModel,
        screenOverride = SubScreen.ALL_SAVINGS_GOALS
    )
    val listScrollKey = "$navScopeKey|LAZY:${selectedFilter.name}"
    val listState = remember(navScopeKey, selectedFilter) {
        val initial = viewModel.scrollStateStore.getLazyPosition(listScrollKey)
        LazyListState(
            firstVisibleItemIndex = initial.index,
            firstVisibleItemScrollOffset = initial.offset
        )
    }
    LaunchedEffect(listState, listScrollKey) {
        val saved = viewModel.scrollStateStore.getLazyPosition(listScrollKey)
        if (listState.firstVisibleItemIndex != saved.index || listState.firstVisibleItemScrollOffset != saved.offset) {
            listState.scrollToItem(saved.index, saved.offset)
        }
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                viewModel.scrollStateStore.saveLazyPosition(listScrollKey, index, offset)
            }
    }
    DisposableEffect(listState, listScrollKey) {
        onDispose {
            viewModel.scrollStateStore.saveLazyPosition(listScrollKey, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }

    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 68.dp,
        extraTopGap = 6.dp
    )

    val cardColor = if (isDark) CardBackgroundDark else MaterialTheme.colorScheme.surfaceContainerLowest
    val cardBorderColor = if (isDark) Color(0x22FFFFFF) else Color(0x146366F1)
    val primaryGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)))

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openNewSavingsGoal() },
                containerColor = Color.Transparent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("add_savings_goal_button")
            ) {
                Box(
                    modifier = Modifier
                        .background(primaryGradient)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isBn) "নতুন লক্ষ্য" else "Add Goal",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "নতুন লক্ষ্য" else "Add Goal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { _ ->
        Box(
            modifier = modifier
                .fillMaxSize()
            // Transparent so global MyWalletBackground shines through
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .recordFrostedBackdrop(
                        scrollLayout.backdropState,
                        listState,
                        scrollLayout.contentGraphicsLayer
                    )
                    .testTag("all_savings_goals_lazy_column"),
                contentPadding = scrollLayout.contentPadding
            ) {
                // 1. Hero Savings Bento Card
                item(key = "savings_hero_card") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "সকল লক্ষ্যে মোট সঞ্চয়" else "TOTAL SAVED ACROSS GOALS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isBn) CurrencyFormatter.convertToBanglaDigits("$overallPct%") else "$overallPct%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(totalSaved, currencySymbol, language = language),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 28.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${if (isBn) "লক্ষ্য:" else "of"} ${CurrencyFormatter.format(totalTarget, currencySymbol, language = language)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { overallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF4F46E5),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isBn) "বাকি আছে" else "Remaining",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = CurrencyFormatter.format(totalRemaining, currencySymbol, language = language),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = if (isBn) "সক্রিয় লক্ষ্য" else "Active Goals",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isBn) CurrencyFormatter.convertToBanglaDigits(activeGoalsCount.toString()) else activeGoalsCount.toString(),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Segmented Glass Filter Pills
                item(key = "savings_filter_pills") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SavingsGoalFilterTab.entries.forEach { tab ->
                            val isSelected = selectedFilter == tab
                            val count = when (tab) {
                                SavingsGoalFilterTab.ALL -> savingsGoals.size
                                SavingsGoalFilterTab.IN_PROGRESS -> savingsGoals.count { !it.isCompleted && it.savedAmount < it.targetAmount }
                                SavingsGoalFilterTab.COMPLETED -> savingsGoals.count { it.isCompleted || it.savedAmount >= it.targetAmount }
                            }
                            val tabLabel = if (isBn) tab.bnLabel else tab.label

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF4F46E5) else cardColor,
                                border = if (isSelected) null else BorderStroke(1.dp, cardBorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("filter_tab_${tab.name.lowercase()}"),
                                onClick = { selectedFilter = tab }
                            ) {
                                Text(
                                    text = "$tabLabel ($count)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3. Goal Cards List or Empty State
                if (filteredGoals.isEmpty()) {
                    item(key = "empty_savings_goals") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardColor,
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF4F46E5).copy(alpha = 0.12f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isBn) "কোনো সঞ্চয় লক্ষ্য পাওয়া যায়নি" else "No savings goals found",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBn) "নতুন লক্ষ্য যোগ করতে নিচের '+ নতুন লক্ষ্য' বাটনে চাপ দিন" else "Tap '+ Add Goal' below to start tracking your savings",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredGoals,
                        key = { it.id }
                    ) { goal ->
                        AllSavingsGoalCardItem(
                            goal = goal,
                            currencySymbol = currencySymbol,
                            language = language,
                            cardColor = cardColor,
                            borderColor = cardBorderColor,
                            onClick = { viewModel.openGoalDetails(goal.id) },
                            onSaveClick = { viewModel.openGoalDetails(goal.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(84.dp))
                }
            }

            // Fixed scroll-aware frosted header
            ScrollAwareFixedHeader(
                layoutState = scrollLayout,
                testTag = "all_savings_goals_fixed_header"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            } else {
                                Color.White.copy(alpha = 0.85f)
                            },
                            shadowElevation = if (isDark) 0.dp else 1.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("all_savings_goals_back_button"),
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

                        Column {
                            Text(
                                text = if (isBn) "সঞ্চয় লক্ষ্য" else "Savings Goals",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (isBn) "লক্ষ্য ও সম্পদ লক্ষ্যমাত্রা" else "Goals & wealth targets",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllSavingsGoalCardItem(
    goal: SavingsGoalEntity,
    currencySymbol: String,
    language: AppLanguage,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (goal.targetAmount > 0.0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (progress * 100).roundToInt()
    val isBn = language == AppLanguage.BANGLA
    val goalColor = try {
        Color(android.graphics.Color.parseColor(goal.themeColorHex))
    } catch (_: Exception) {
        Color(0xFFF59E0B)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("savings_goal_card_${goal.id}"),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = goalColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = resolveGoalIcon(goal.iconName),
                            contentDescription = goal.name,
                            tint = goalColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (goal.targetDeadlineStr.isNotBlank()) {
                        val duePrefix = if (isBn) "মেয়াদ:" else "Due:"
                        Text(
                            text = "$duePrefix ${goal.targetDeadlineStr}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = goalColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isBn) CurrencyFormatter.convertToBanglaDigits("$pct%") else "$pct%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = goalColor,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = goalColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CurrencyFormatter.format(goal.savedAmount, currencySymbol, language = language)} / ${CurrencyFormatter.format(goal.targetAmount, currencySymbol, language = language)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF4F46E5).copy(alpha = 0.12f),
                    onClick = onSaveClick
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isBn) "জমা" else "Save",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
