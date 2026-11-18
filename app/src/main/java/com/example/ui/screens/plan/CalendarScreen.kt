package com.example.ui.screens.plan

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.SubScreen
import com.example.ui.components.ScrollAwareFixedHeader
import com.example.ui.components.TranslucentCard
import com.example.ui.components.recordFrostedBackdrop
import com.example.ui.components.rememberScrollAwareGlassLayout
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.AppStrings
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode {
    MONTH,
    AGENDA
}

enum class HeaderPickerMode {
    NONE,
    MONTH_SELECTOR,
    YEAR_SELECTOR
}

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val extendedSettings by viewModel.extendedSettings.collectAsState()
    val language = settings?.language ?: AppLanguage.ENGLISH
    val currencySymbol = settings?.currencySymbol ?: "৳"
    val startOfWeek = extendedSettings.startOfWeek

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var pickerMode by remember { mutableStateOf(HeaderPickerMode.NONE) }

    // Calendar state defaults to current date (not hardcoded Oct 2026)
    val today = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val calSummary = remember(transactions, debts, accounts, currentYear, currentMonth, selectedDay) {
        com.example.ui.util.FinancialReportAnalytics.computeCalendar(
            transactions = transactions,
            debts = debts,
            accounts = accounts,
            year = currentYear,
            month = currentMonth,
            selectedDay = selectedDay
        )
    }

    val monthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val fullMonthNames = remember {
        listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }

    // Days in current selected month
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, currentMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Sunday is 1, Monday is 2 in standard Java Calendar
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    val leadingEmptyDays = com.example.data.local.UserSettingsPreferences.computeLeadingEmptyDays(firstDayOfWeek, startOfWeek)
    val orderedWeekDays = remember(startOfWeek) {
        com.example.data.local.UserSettingsPreferences.orderedWeekDays(startOfWeek)
    }

    val monthIncome = calSummary.monthIncome
    val monthExpense = calSummary.monthExpense
    val pendingDebtsTotal = calSummary.pendingDebtsTotal
    val selectedDayTransactions = calSummary.selectedDayTransactions
    val dayTotalIn = calSummary.selectedDayIncome
    val dayTotalOut = calSummary.selectedDayExpense

    val listState = viewModel.rememberScreenLazyListState(
        screenOverride = com.example.ui.SubScreen.CALENDAR
    )
    val scrollLayout = rememberScrollAwareGlassLayout(
        listState = listState,
        defaultHeaderHeight = 62.dp,
        extraTopGap = 4.dp,
        bottomNavClearance = 80.dp
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
                .testTag("calendar_lazy_column"),
            contentPadding = scrollLayout.contentPadding
        ) {
            // Segmented Control: Month vs Agenda
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .height(46.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        // Month tab
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (viewMode == CalendarViewMode.MONTH) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewMode = CalendarViewMode.MONTH },
                            shadowElevation = if (viewMode == CalendarViewMode.MONTH) 2.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = AppStrings.t("month", language),
                                    fontWeight = if (viewMode == CalendarViewMode.MONTH) FontWeight.Bold else FontWeight.Medium,
                                    color = if (viewMode == CalendarViewMode.MONTH) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Agenda tab
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (viewMode == CalendarViewMode.AGENDA) MaterialTheme.colorScheme.surface else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewMode = CalendarViewMode.AGENDA },
                            shadowElevation = if (viewMode == CalendarViewMode.AGENDA) 2.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = AppStrings.t("agenda", language),
                                    fontWeight = if (viewMode == CalendarViewMode.AGENDA) FontWeight.Bold else FontWeight.Medium,
                                    color = if (viewMode == CalendarViewMode.AGENDA) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Month / Year Navigation Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable Month Header with dropdown indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                pickerMode = when (pickerMode) {
                                    HeaderPickerMode.NONE -> HeaderPickerMode.MONTH_SELECTOR
                                    HeaderPickerMode.MONTH_SELECTOR -> HeaderPickerMode.YEAR_SELECTOR
                                    HeaderPickerMode.YEAR_SELECTOR -> HeaderPickerMode.NONE
                                }
                            }
                            .padding(4.dp)
                    ) {
                        val headerText = when (pickerMode) {
                            HeaderPickerMode.YEAR_SELECTOR -> "2024 – 2035"
                            HeaderPickerMode.MONTH_SELECTOR -> "$currentYear"
                            HeaderPickerMode.NONE -> "${fullMonthNames[currentMonth]} $currentYear"
                        }
                        Text(
                            text = headerText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (pickerMode == HeaderPickerMode.NONE) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                            contentDescription = "Expand picker",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Navigation buttons: Today, <, >
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    currentYear = today.get(Calendar.YEAR)
                                    currentMonth = today.get(Calendar.MONTH)
                                    selectedDay = today.get(Calendar.DAY_OF_MONTH)
                                    pickerMode = HeaderPickerMode.NONE
                                }
                        ) {
                            Text(
                                text = AppStrings.t("today", language),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear--
                                    } else {
                                        currentMonth--
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear++
                                    } else {
                                        currentMonth++
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Month Picker (Jan - Dec 3x4 grid)
            if (pickerMode == HeaderPickerMode.MONTH_SELECTOR) {
                item {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val chunkedMonths = (0..11).chunked(3)
                            chunkedMonths.forEach { rowMonths ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowMonths.forEach { mIndex ->
                                        val isSelected = mIndex == currentMonth
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    currentMonth = mIndex
                                                    pickerMode = HeaderPickerMode.NONE
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = monthNames[mIndex],
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Year Picker (2024 - 2035 3x4 grid)
            if (pickerMode == HeaderPickerMode.YEAR_SELECTOR) {
                item {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val yearsList = (2024..2035).toList()
                            val chunkedYears = yearsList.chunked(3)
                            chunkedYears.forEach { rowYears ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowYears.forEach { yr ->
                                        val isSelected = yr == currentYear
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) Color(0xFF4F46E5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    currentYear = yr
                                                    pickerMode = HeaderPickerMode.MONTH_SELECTOR
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = yr.toString(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Standard Calendar Month View (when not showing selector grid)
            if (viewMode == CalendarViewMode.MONTH && pickerMode == HeaderPickerMode.NONE) {
                item {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Weekday headers rotated to start on user's selected Start of Week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                orderedWeekDays.forEach { dayOption ->
                                    val dayLabel = if (language == AppLanguage.BANGLA) dayOption.shortBn else dayOption.shortEn
                                    Text(
                                        text = dayLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 7 columns grid for days
                            val totalSlots = leadingEmptyDays + maxDaysInMonth
                            val rowsCount = (totalSlots + 6) / 7

                            for (row in 0 until rowsCount) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    for (col in 0..6) {
                                        val slotIndex = row * 7 + col
                                        val dayNumber = slotIndex - leadingEmptyDays + 1

                                        if (dayNumber in 1..maxDaysInMonth) {
                                            val isSelected = dayNumber == selectedDay

                                            // Determine dots for this day from real data
                                            val dots = calSummary.dayDots[dayNumber] ?: com.example.ui.util.CalendarDayDots(false, false, false, false)
                                            val hasExpense = dots.hasExpense
                                            val hasIncome = dots.hasIncome
                                            val hasTransfer = dots.hasTransfer
                                            val hasCardDate = dots.hasCardDate

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .width(38.dp)
                                                    .clip(RoundedCornerShape(19.dp))
                                                    .clickable { selectedDay = dayNumber }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color(0xFF4F46E5) else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(dayNumber.toString()) else dayNumber.toString(),
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                // Dot indicators
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.height(6.dp)
                                                ) {
                                                    if (hasExpense) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                                    }
                                                    if (hasIncome) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                                    }
                                                    if (hasTransfer) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                                    }
                                                    if (hasCardDate) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFB45309)))
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(38.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Legend Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(color = Color(0xFFEF4444), label = AppStrings.t("spent_bills", language))
                                LegendDot(color = Color(0xFF10B981), label = AppStrings.t("income", language))
                                LegendDot(color = Color(0xFF3B82F6), label = AppStrings.t("transfers_emi", language))
                                LegendDot(color = Color(0xFFB45309), label = AppStrings.t("card_dates", language))
                            }
                        }
                    }
                }
            }

            // Summary Strip: Money In, Money Out, Still Due
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TranslucentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = AppStrings.t("money_in", language),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+${CurrencyFormatter.format(monthIncome, currencySymbol, language = language)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Column {
                            Text(
                                text = AppStrings.t("money_out", language),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "-${CurrencyFormatter.format(monthExpense, currencySymbol, language = language)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }

                        Column {
                            Text(
                                text = AppStrings.t("still_due", language),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.format(pendingDebtsTotal, currencySymbol, language = language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            // Day / Agenda Transactions Section
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSelectedToday = selectedDay == today.get(Calendar.DAY_OF_MONTH) &&
                            currentMonth == today.get(Calendar.MONTH) &&
                            currentYear == today.get(Calendar.YEAR)
                    val dayHeader = if (isSelectedToday) {
                        val dayOfWeekName = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(today.time)
                        "${AppStrings.t("today", language)} • $dayOfWeekName, $selectedDay ${monthNames[currentMonth]}"
                    } else {
                        "Date • $selectedDay ${monthNames[currentMonth]} $currentYear"
                    }
                    Text(
                        text = dayHeader,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )

                    val netDayTotal = dayTotalIn - dayTotalOut
                    val netDayDisplay = if (netDayTotal > 0) {
                        "+${CurrencyFormatter.format(netDayTotal, currencySymbol, language = language)} in"
                    } else if (netDayTotal < 0) {
                        "-${CurrencyFormatter.format(kotlin.math.abs(netDayTotal), currencySymbol, language = language)} out"
                    } else {
                        "+${CurrencyFormatter.format(0.0, currencySymbol, language = language)}"
                    }
                    Text(
                        text = netDayDisplay,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (netDayTotal >= 0) IncomeGreen else ExpenseRed
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Items list for the selected day or agenda
            if (selectedDayTransactions.isEmpty()) {
                item {
                    TranslucentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) "এই দিনে কোনো লেনদেন নেই" else "No transactions on this date",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(selectedDayTransactions) { tx ->
                val acc = accounts.find { it.id == tx.accountId }
                val accName = acc?.name ?: "Money Bag"

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (tx.type) {
                                TransactionType.INCOME -> Color(0xFFDCFCE7)
                                TransactionType.EXPENSE -> Color(0xFFFEE2E2)
                                TransactionType.TRANSFER -> Color(0xFFE0E7FF)
                                else -> Color(0xFFEDE9FE)
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (tx.type) {
                                        TransactionType.INCOME -> Icons.Default.TrendingUp
                                        TransactionType.EXPENSE -> Icons.Default.TrendingDown
                                        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                                        else -> Icons.AutoMirrored.Filled.ReceiptLong
                                    },
                                    contentDescription = null,
                                    tint = when (tx.type) {
                                        TransactionType.INCOME -> IncomeGreen
                                        TransactionType.EXPENSE -> ExpenseRed
                                        TransactionType.TRANSFER -> Color(0xFF4F46E5)
                                        else -> Color(0xFF7C3AED)
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.category} • $accName",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val sign = when (tx.type) {
                                TransactionType.INCOME -> "+"
                                TransactionType.EXPENSE -> "-"
                                else -> ""
                            }
                            val amtColor = when (tx.type) {
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.EXPENSE -> ExpenseRed
                                else -> Color(0xFF4F46E5)
                            }
                            Text(
                                text = "$sign${CurrencyFormatter.format(tx.amount, currencySymbol, language = language)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = amtColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = DateFormatter.formatTime(tx.timestamp),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        }

        ScrollAwareFixedHeader(
            layoutState = scrollLayout,
            testTag = "calendar_fixed_header"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.size(42.dp),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = AppStrings.t("calendar", language),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Circular "+" Action Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.size(42.dp),
                    onClick = {
                        viewModel.transactionToEdit.value = null
                        viewModel.openSubScreen(SubScreen.ADD_EDIT_TRANSACTION)
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }
}
