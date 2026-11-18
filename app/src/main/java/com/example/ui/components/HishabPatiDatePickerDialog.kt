package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Shared styling and date calculation helpers for Hishab Pati's custom Date Picker dialog.
 * Ensures visual consistency across Add Transaction and Filtered Reports in both Light and Dark themes.
 */
object HishabPatiDatePickerStyle {
    val DIALOG_CORNER_RADIUS = 28.dp
    val HEADER_CORNER_RADIUS = 20.dp
    val DAY_CELL_CORNER_RADIUS = 12.dp
    val BUTTON_CORNER_RADIUS = 14.dp

    val DIALOG_BORDER_WIDTH = 1.25.dp
    val TODAY_BORDER_WIDTH = 1.25.dp
    val SELECTED_DAY_BORDER_WIDTH = 1.5.dp

    fun dialogBorderColor(outline: Color, isDark: Boolean): Color {
        return outline.copy(alpha = if (isDark) 0.78f else 0.75f)
    }

    fun headerBackgroundColor(primary: Color, isDark: Boolean): Color {
        return primary.copy(alpha = if (isDark) 0.16f else 0.09f)
    }

    fun headerBorderColor(primary: Color, isDark: Boolean): Color {
        return primary.copy(alpha = if (isDark) 0.32f else 0.20f)
    }

    fun selectedDayBackgroundColor(primary: Color): Color = primary

    fun selectedDayContentColor(onPrimary: Color): Color = onPrimary

    fun todayBackgroundColor(primary: Color, isDark: Boolean): Color {
        return primary.copy(alpha = if (isDark) 0.14f else 0.08f)
    }

    fun inRangeBackgroundColor(primary: Color, isDark: Boolean): Color {
        return primary.copy(alpha = if (isDark) 0.18f else 0.11f)
    }

    fun startOfDayMillis(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDayMillis(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun isSameCalendarDay(millisA: Long, millisB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = millisA }
        val calB = Calendar.getInstance().apply { timeInMillis = millisB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    fun isDateSelectable(
        dayMillis: Long,
        minDateMillis: Long? = null,
        maxDateMillis: Long? = null
    ): Boolean {
        val dayStart = startOfDayMillis(dayMillis)
        if (minDateMillis != null && dayStart < startOfDayMillis(minDateMillis)) return false
        if (maxDateMillis != null && dayStart > startOfDayMillis(maxDateMillis)) return false
        return true
    }

    fun formatCustomRangeLabel(
        startMillis: Long,
        endMillis: Long,
        language: AppLanguage
    ): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
        val sMonth = SimpleDateFormat("MMM", Locale.US).format(startCal.time)
        val eMonth = SimpleDateFormat("MMM", Locale.US).format(endCal.time)
        val sDay = startCal.get(Calendar.DAY_OF_MONTH)
        val eDay = endCal.get(Calendar.DAY_OF_MONTH)
        val raw = if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)) {
            "$sMonth $sDay - $eMonth $eDay"
        } else {
            val sYear = startCal.get(Calendar.YEAR)
            val endYear = endCal.get(Calendar.YEAR)
            "$sMonth $sDay, $sYear - $eMonth $eDay, $endYear"
        }
        return DateFormatter.localizeText(raw, language)
    }

    /**
     * Parses manual user date input supporting standard formats:
     * yyyy-MM-dd, dd/MM/yyyy, d/M/yyyy, dd-MM-yyyy, MM/dd/yyyy, MMM d, yyyy.
     * Preserves the time-of-day from [referenceTimeMillis].
     */
    fun parseManualDateInput(input: String, referenceTimeMillis: Long = System.currentTimeMillis()): Long? {
        val normalized = normalizeDigits(input.trim())
        if (normalized.isBlank()) return null

        val patterns = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "d/M/yyyy",
            "dd-MM-yyyy",
            "d-M-yyyy",
            "MM/dd/yyyy",
            "MMM d, yyyy",
            "MMMM d, yyyy",
            "d MMM yyyy",
            "dd MMM yyyy"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }
                val parsed = sdf.parse(normalized)
                if (parsed != null) {
                    val parsedCal = Calendar.getInstance().apply { time = parsed }
                    val year = parsedCal.get(Calendar.YEAR)
                    if (year in 1900..2100) {
                        val refCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
                        refCal.set(
                            year,
                            parsedCal.get(Calendar.MONTH),
                            parsedCal.get(Calendar.DAY_OF_MONTH)
                        )
                        return refCal.timeInMillis
                    }
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    private fun normalizeDigits(text: String): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(text.length)
        for (ch in text) {
            val idx = banglaDigits.indexOf(ch)
            if (idx >= 0) sb.append(('0'.code + idx).toChar()) else sb.append(ch)
        }
        return sb.toString()
    }

    data class CalendarCell(
        val year: Int,
        val month: Int,
        val dayOfMonth: Int,
        val timestampMillis: Long,
        val isCurrentMonth: Boolean
    )

    /**
     * Generates a 42-cell (6 weeks × 7 days) calendar grid for the given [year] and [month] (0-indexed).
     */
    fun buildMonthGrid(
        year: Int,
        month: Int,
        referenceTimeMillis: Long = System.currentTimeMillis(),
        firstDayOfWeek: Int = Calendar.SUNDAY
    ): List<CalendarCell> {
        val refCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
        val hour = refCal.get(Calendar.HOUR_OF_DAY)
        val minute = refCal.get(Calendar.MINUTE)
        val second = refCal.get(Calendar.SECOND)
        val millis = refCal.get(Calendar.MILLISECOND)

        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, millis)
        }

        // Day of week: Sunday = 1 .. Saturday = 7 -> leading offset 0..6 relative to firstDayOfWeek
        val leadingDays = com.example.data.local.UserSettingsPreferences.computeLeadingEmptyDays(
            firstOfMonth.get(Calendar.DAY_OF_WEEK),
            firstDayOfWeek
        )
        val gridStart = (firstOfMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadingDays)
        }

        val cells = ArrayList<CalendarCell>(42)
        val cursor = gridStart.clone() as Calendar
        for (i in 0 until 42) {
            val cellYear = cursor.get(Calendar.YEAR)
            val cellMonth = cursor.get(Calendar.MONTH)
            val cellDay = cursor.get(Calendar.DAY_OF_MONTH)
            cells.add(
                CalendarCell(
                    year = cellYear,
                    month = cellMonth,
                    dayOfMonth = cellDay,
                    timestampMillis = cursor.timeInMillis,
                    isCurrentMonth = (cellYear == year && cellMonth == month)
                )
            )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }
}

/**
 * Redesigned, theme-consistent Date Picker Dialog for Hishab Pati.
 * Supports:
 * - Month/Year navigation (previous/next month arrows + interactive year & month grid selector)
 * - Selecting any allowed date with primary-color highlight and Today indicator
 * - Switching between Calendar view and Manual Date Input view
 * - Cancel and OK actions
 * - Full Light and Dark theme support
 */
@Composable
fun HishabPatiDatePickerDialog(
    initialSelectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
    titleText: String? = null,
    confirmButtonText: String? = null,
    dismissButtonText: String? = null,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    rangeStartMillis: Long? = null,
    firstDayOfWeek: Int = Calendar.SUNDAY
) {
    val isBn = language == AppLanguage.BANGLA
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val initialClampedMillis = remember(initialSelectedDateMillis, minDateMillis, maxDateMillis) {
        var target = initialSelectedDateMillis
        if (minDateMillis != null && HishabPatiDatePickerStyle.startOfDayMillis(target) < HishabPatiDatePickerStyle.startOfDayMillis(minDateMillis)) {
            target = minDateMillis
        }
        if (maxDateMillis != null && HishabPatiDatePickerStyle.startOfDayMillis(target) > HishabPatiDatePickerStyle.startOfDayMillis(maxDateMillis)) {
            target = maxDateMillis
        }
        target
    }

    var selectedDateMillis by remember(initialClampedMillis) {
        mutableLongStateOf(initialClampedMillis)
    }

    val initialCal = remember(initialClampedMillis) {
        Calendar.getInstance().apply { timeInMillis = initialClampedMillis }
    }
    var displayedYear by remember(initialClampedMillis) {
        mutableIntStateOf(initialCal.get(Calendar.YEAR))
    }
    var displayedMonth by remember(initialClampedMillis) {
        mutableIntStateOf(initialCal.get(Calendar.MONTH))
    }

    var isYearPickerOpen by remember { mutableStateOf(false) }
    var isManualInputMode by remember { mutableStateOf(false) }

    val inputDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    var manualInputText by remember(initialClampedMillis) {
        mutableStateOf(inputDateFormat.format(Date(initialClampedMillis)))
    }
    var manualInputError by remember { mutableStateOf<String?>(null) }

    val todayMillis = remember { System.currentTimeMillis() }

    val formattedSelectedDate = remember(selectedDateMillis, language) {
        val raw = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(Date(selectedDateMillis))
        DateFormatter.localizeText(raw, language)
    }

    val formattedDisplayedMonthYear = remember(displayedYear, displayedMonth, language) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val raw = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
        DateFormatter.localizeText(raw, language)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val borderColor = HishabPatiDatePickerStyle.dialogBorderColor(outlineColor, isDark)

    val effectiveTitle = titleText ?: if (isBn) "তারিখ নির্বাচন করুন" else "Select Date"
    val effectiveConfirmText = confirmButtonText ?: if (isBn) "ঠিক আছে" else "OK"
    val effectiveDismissText = dismissButtonText ?: if (isBn) "বাতিল" else "Cancel"

    val isCurrentSelectionValid = remember(selectedDateMillis, isManualInputMode, manualInputText, minDateMillis, maxDateMillis) {
        if (isManualInputMode) {
            val parsed = HishabPatiDatePickerStyle.parseManualDateInput(manualInputText, selectedDateMillis)
            parsed != null && HishabPatiDatePickerStyle.isDateSelectable(parsed, minDateMillis, maxDateMillis)
        } else {
            HishabPatiDatePickerStyle.isDateSelectable(selectedDateMillis, minDateMillis, maxDateMillis)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(HishabPatiDatePickerStyle.DIALOG_CORNER_RADIUS),
            color = surfaceColor,
            border = BorderStroke(HishabPatiDatePickerStyle.DIALOG_BORDER_WIDTH, borderColor),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 380.dp)
                .testTag("hishab_pati_date_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // 1. Styled Header Card (Icon badge + Title + Selected Date + Mode Toggle)
                Surface(
                    shape = RoundedCornerShape(HishabPatiDatePickerStyle.HEADER_CORNER_RADIUS),
                    color = HishabPatiDatePickerStyle.headerBackgroundColor(primaryColor, isDark),
                    border = BorderStroke(1.dp, HishabPatiDatePickerStyle.headerBorderColor(primaryColor, isDark)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryColor.copy(alpha = if (isDark) 0.24f else 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = effectiveTitle,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.testTag("date_picker_title_text")
                                )
                                Text(
                                    text = formattedSelectedDate,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = onSurfaceColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    modifier = Modifier.testTag("date_picker_selected_date_header")
                                )
                            }
                        }

                        // Switch between Calendar Grid & Manual Input
                        Surface(
                            onClick = {
                                if (!isManualInputMode) {
                                    manualInputText = inputDateFormat.format(Date(selectedDateMillis))
                                    manualInputError = null
                                    isManualInputMode = true
                                    isYearPickerOpen = false
                                } else {
                                    isManualInputMode = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = surfaceColor.copy(alpha = if (isDark) 0.7f else 0.9f),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("date_picker_mode_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isManualInputMode) Icons.Default.CalendarMonth else Icons.Default.EditCalendar,
                                    contentDescription = if (isManualInputMode) {
                                        if (isBn) "ক্যালেন্ডার দেখুন" else "Switch to calendar"
                                    } else {
                                        if (isBn) "তারিখ লিখুন" else "Switch to manual date input"
                                    },
                                    tint = primaryColor,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Main Body: Manual Date Input OR Calendar / Year-Month Picker
                AnimatedContent(
                    targetState = Triple(isManualInputMode, isYearPickerOpen, displayedYear * 12 + displayedMonth),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "DatePickerBodyTransition"
                ) { (manualMode, yearPickerMode, _) ->
                    if (manualMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isBn) "তারিখ লিখুন (YYYY-MM-DD বা DD/MM/YYYY)" else "Enter date (YYYY-MM-DD or DD/MM/YYYY)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = onSurfaceVariantColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = manualInputText,
                                onValueChange = { updated ->
                                    manualInputText = updated
                                    val parsed = HishabPatiDatePickerStyle.parseManualDateInput(updated, selectedDateMillis)
                                    if (parsed == null) {
                                        manualInputError = if (isBn) {
                                            "সঠিক ফরম্যাটে তারিখ দিন (যেমন 2026-11-02)"
                                        } else {
                                            "Enter a valid date (e.g. 2026-11-02 or 02/11/2026)"
                                        }
                                    } else if (!HishabPatiDatePickerStyle.isDateSelectable(parsed, minDateMillis, maxDateMillis)) {
                                        manualInputError = if (isBn) {
                                            "নির্বাচিত তারিখটি অনুমোদিত সীমার বাইরে"
                                        } else {
                                            "Selected date is outside the allowed range"
                                        }
                                    } else {
                                        manualInputError = null
                                        selectedDateMillis = parsed
                                        val cal = Calendar.getInstance().apply { timeInMillis = parsed }
                                        displayedYear = cal.get(Calendar.YEAR)
                                        displayedMonth = cal.get(Calendar.MONTH)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "YYYY-MM-DD",
                                        color = onSurfaceVariantColor.copy(alpha = 0.6f)
                                    )
                                },
                                isError = manualInputError != null,
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = surfaceVariantColor.copy(alpha = 0.35f),
                                    unfocusedContainerColor = surfaceVariantColor.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("date_picker_manual_input")
                            )
                            if (manualInputError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = manualInputError!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.testTag("date_picker_manual_input_error")
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Month/Year Navigation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Interactive Month/Year Dropdown Pill
                                Surface(
                                    onClick = { isYearPickerOpen = !isYearPickerOpen },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isYearPickerOpen) {
                                        primaryColor.copy(alpha = if (isDark) 0.20f else 0.12f)
                                    } else {
                                        surfaceVariantColor.copy(alpha = if (isDark) 0.55f else 0.7f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isYearPickerOpen) primaryColor else borderColor
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .testTag("date_picker_year_selector_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formattedDisplayedMonthYear,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = if (isYearPickerOpen) primaryColor else onSurfaceColor
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isYearPickerOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isBn) "বছর ও মাস পরিবর্তন" else "Select month and year",
                                            tint = if (isYearPickerOpen) primaryColor else onSurfaceVariantColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // "Today" quick-jump pill when not already on Today
                                    val isTodaySelected = HishabPatiDatePickerStyle.isSameCalendarDay(selectedDateMillis, todayMillis)
                                    val canSelectToday = HishabPatiDatePickerStyle.isDateSelectable(todayMillis, minDateMillis, maxDateMillis)
                                    if (!isTodaySelected && canSelectToday) {
                                        Surface(
                                            onClick = {
                                                val todayCal = Calendar.getInstance().apply { timeInMillis = todayMillis }
                                                selectedDateMillis = todayMillis
                                                displayedYear = todayCal.get(Calendar.YEAR)
                                                displayedMonth = todayCal.get(Calendar.MONTH)
                                                isYearPickerOpen = false
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = primaryColor.copy(alpha = if (isDark) 0.18f else 0.10f),
                                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .testTag("date_picker_today_button")
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = if (isBn) "আজ" else "Today",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = primaryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Previous Month Button
                                    Surface(
                                        onClick = {
                                            if (displayedMonth == Calendar.JANUARY) {
                                                displayedMonth = Calendar.DECEMBER
                                                displayedYear -= 1
                                            } else {
                                                displayedMonth -= 1
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = surfaceVariantColor.copy(alpha = if (isDark) 0.55f else 0.7f),
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .testTag("date_picker_prev_month")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = if (isBn) "আগের মাস" else "Previous month",
                                                tint = onSurfaceColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Next Month Button
                                    Surface(
                                        onClick = {
                                            if (displayedMonth == Calendar.DECEMBER) {
                                                displayedMonth = Calendar.JANUARY
                                                displayedYear += 1
                                            } else {
                                                displayedMonth += 1
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = surfaceVariantColor.copy(alpha = if (isDark) 0.55f else 0.7f),
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .testTag("date_picker_next_month")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = if (isBn) "পরের মাস" else "Next month",
                                                tint = onSurfaceColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (yearPickerMode) {
                                // Year & Month Selector View
                                YearAndMonthSelectorGrid(
                                    displayedYear = displayedYear,
                                    displayedMonth = displayedMonth,
                                    language = language,
                                    isDark = isDark,
                                    primaryColor = primaryColor,
                                    surfaceVariantColor = surfaceVariantColor,
                                    onSurfaceColor = onSurfaceColor,
                                    borderColor = borderColor,
                                    onMonthSelected = { newMonth ->
                                        displayedMonth = newMonth
                                    },
                                    onYearSelected = { newYear ->
                                        displayedYear = newYear
                                        isYearPickerOpen = false
                                    }
                                )
                            } else {
                                // Weekday Headers rotated to firstDayOfWeek
                                val orderedDays = remember(firstDayOfWeek) {
                                    com.example.data.local.UserSettingsPreferences.orderedWeekDays(firstDayOfWeek)
                                }
                                val weekdays = remember(orderedDays, isBn) {
                                    orderedDays.map { if (isBn) it.singleBn else it.singleEn }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    weekdays.forEach { dayLabel ->
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = onSurfaceVariantColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // 6x7 Calendar Day Cells
                                val cells = remember(displayedYear, displayedMonth, selectedDateMillis, firstDayOfWeek) {
                                    HishabPatiDatePickerStyle.buildMonthGrid(
                                        year = displayedYear,
                                        month = displayedMonth,
                                        referenceTimeMillis = selectedDateMillis,
                                        firstDayOfWeek = firstDayOfWeek
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("date_picker_calendar_grid"),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    for (week in 0 until 6) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            for (dayIdx in 0 until 7) {
                                                val cell = cells[week * 7 + dayIdx]
                                                val isSelected = HishabPatiDatePickerStyle.isSameCalendarDay(
                                                    cell.timestampMillis,
                                                    selectedDateMillis
                                                )
                                                val isToday = HishabPatiDatePickerStyle.isSameCalendarDay(
                                                    cell.timestampMillis,
                                                    todayMillis
                                                )
                                                val isRangeStart = rangeStartMillis != null &&
                                                    HishabPatiDatePickerStyle.isSameCalendarDay(
                                                        cell.timestampMillis,
                                                        rangeStartMillis
                                                    )
                                                val isInRange = if (rangeStartMillis != null) {
                                                    val dayStart = HishabPatiDatePickerStyle.startOfDayMillis(cell.timestampMillis)
                                                    val rStart = HishabPatiDatePickerStyle.startOfDayMillis(minOf(rangeStartMillis, selectedDateMillis))
                                                    val rEnd = HishabPatiDatePickerStyle.startOfDayMillis(maxOf(rangeStartMillis, selectedDateMillis))
                                                    dayStart in rStart..rEnd && !isSelected
                                                } else {
                                                    false
                                                }
                                                val isSelectable = HishabPatiDatePickerStyle.isDateSelectable(
                                                    cell.timestampMillis,
                                                    minDateMillis,
                                                    maxDateMillis
                                                )

                                                val cellBg = when {
                                                    isSelected -> HishabPatiDatePickerStyle.selectedDayBackgroundColor(primaryColor)
                                                    isRangeStart || isInRange -> HishabPatiDatePickerStyle.inRangeBackgroundColor(primaryColor, isDark)
                                                    isToday -> HishabPatiDatePickerStyle.todayBackgroundColor(primaryColor, isDark)
                                                    else -> Color.Transparent
                                                }

                                                val cellBorder = when {
                                                    isSelected -> BorderStroke(
                                                        HishabPatiDatePickerStyle.SELECTED_DAY_BORDER_WIDTH,
                                                        primaryColor
                                                    )
                                                    isRangeStart -> BorderStroke(
                                                        HishabPatiDatePickerStyle.TODAY_BORDER_WIDTH,
                                                        primaryColor
                                                    )
                                                    isToday -> BorderStroke(
                                                        HishabPatiDatePickerStyle.TODAY_BORDER_WIDTH,
                                                        primaryColor.copy(alpha = 0.75f)
                                                    )
                                                    else -> null
                                                }

                                                val textColor = when {
                                                    !isSelectable -> onSurfaceVariantColor.copy(alpha = 0.28f)
                                                    isSelected -> HishabPatiDatePickerStyle.selectedDayContentColor(MaterialTheme.colorScheme.onPrimary)
                                                    isRangeStart || isToday -> primaryColor
                                                    cell.isCurrentMonth -> onSurfaceColor
                                                    else -> onSurfaceVariantColor.copy(alpha = if (isDark) 0.38f else 0.42f)
                                                }

                                                val dayText = remember(cell.dayOfMonth, isBn) {
                                                    val raw = cell.dayOfMonth.toString()
                                                    if (isBn) CurrencyFormatter.convertToBanglaDigits(raw) else raw
                                                }

                                                val cellShape = RoundedCornerShape(HishabPatiDatePickerStyle.DAY_CELL_CORNER_RADIUS)
                                                val dayModifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(cellShape)
                                                    .background(cellBg, cellShape)
                                                    .then(
                                                        if (cellBorder != null) {
                                                            Modifier.border(cellBorder, cellShape)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .then(
                                                        if (isSelectable) {
                                                            Modifier.clickable {
                                                                selectedDateMillis = cell.timestampMillis
                                                                if (!cell.isCurrentMonth) {
                                                                    displayedYear = cell.year
                                                                    displayedMonth = cell.month
                                                                }
                                                            }
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .then(
                                                        if (cell.isCurrentMonth) {
                                                            Modifier.testTag("date_picker_day_${cell.dayOfMonth}")
                                                        } else {
                                                            Modifier
                                                        }
                                                    )

                                                Box(
                                                    modifier = dayModifier,
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = dayText,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = textColor,
                                                            fontWeight = if (isSelected || isToday || isRangeStart) {
                                                                FontWeight.Bold
                                                            } else if (cell.isCurrentMonth) {
                                                                FontWeight.Medium
                                                            } else {
                                                                FontWeight.Normal
                                                            },
                                                            fontSize = 13.sp
                                                        ),
                                                        textAlign = TextAlign.Center,
                                                        modifier = if (isSelected) {
                                                            Modifier.testTag("date_picker_selected_day_cell")
                                                        } else {
                                                            Modifier
                                                        }
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

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = borderColor.copy(alpha = 0.45f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // 3. Action Buttons Row (Cancel & OK)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(HishabPatiDatePickerStyle.BUTTON_CORNER_RADIUS),
                        color = surfaceVariantColor.copy(alpha = if (isDark) 0.45f else 0.65f),
                        border = BorderStroke(1.25.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(HishabPatiDatePickerStyle.BUTTON_CORNER_RADIUS))
                            .testTag("date_picker_cancel_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = effectiveDismissText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = onSurfaceColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val finalDate = if (isManualInputMode) {
                                HishabPatiDatePickerStyle.parseManualDateInput(manualInputText, selectedDateMillis)
                                    ?: selectedDateMillis
                            } else {
                                selectedDateMillis
                            }
                            if (HishabPatiDatePickerStyle.isDateSelectable(finalDate, minDateMillis, maxDateMillis)) {
                                onDateSelected(finalDate)
                            }
                        },
                        enabled = isCurrentSelectionValid,
                        shape = RoundedCornerShape(HishabPatiDatePickerStyle.BUTTON_CORNER_RADIUS),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = primaryColor.copy(alpha = 0.35f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("date_picker_confirm_button")
                    ) {
                        Text(
                            text = effectiveConfirmText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearAndMonthSelectorGrid(
    displayedYear: Int,
    displayedMonth: Int,
    language: AppLanguage,
    isDark: Boolean,
    primaryColor: Color,
    surfaceVariantColor: Color,
    onSurfaceColor: Color,
    borderColor: Color,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    val shortMonths = remember(language) {
        val raw = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        raw.map { DateFormatter.localizeText(it, language) }
    }
    val years = remember { (1970..2100).toList() }
    val targetYearIndex = remember(displayedYear) {
        (displayedYear - 1970 - 3).coerceIn(0, years.lastIndex)
    }
    val yearGridState = rememberLazyGridState(initialFirstVisibleItemIndex = targetYearIndex)

    LaunchedEffect(displayedYear) {
        yearGridState.scrollToItem(targetYearIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("date_picker_year_month_grid")
    ) {
        // Month Quick Selector (4 columns x 3 rows)
        Text(
            text = if (isBn) "মাস" else "Month",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        for (row in 0 until 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 4) {
                    val mIdx = row * 4 + col
                    val isSelectedMonth = mIdx == displayedMonth
                    Surface(
                        onClick = { onMonthSelected(mIdx) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelectedMonth) {
                            primaryColor
                        } else {
                            surfaceVariantColor.copy(alpha = if (isDark) 0.45f else 0.6f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelectedMonth) primaryColor else borderColor.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("date_picker_month_option_$mIdx")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = shortMonths[mIdx],
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelectedMonth) MaterialTheme.colorScheme.onPrimary else onSurfaceColor,
                                    fontWeight = if (isSelectedMonth) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isBn) "বছর" else "Year",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = yearGridState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 148.dp)
        ) {
            items(years, key = { it }) { yr ->
                val isSelectedYear = yr == displayedYear
                val yrText = if (isBn) CurrencyFormatter.convertToBanglaDigits(yr.toString()) else yr.toString()
                Surface(
                    onClick = { onYearSelected(yr) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelectedYear) {
                        primaryColor
                    } else {
                        surfaceVariantColor.copy(alpha = if (isDark) 0.45f else 0.6f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelectedYear) primaryColor else borderColor.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .testTag("date_picker_year_option_$yr")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = yrText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSelectedYear) MaterialTheme.colorScheme.onPrimary else onSurfaceColor,
                                fontWeight = if (isSelectedYear) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared 2-step Custom Date Range Picker Dialog (Start Date -> End Date)
 * built on top of [HishabPatiDatePickerDialog] so that Custom Date selection
 * in Filtered Reports shares the exact same redesigned calendar component and visual style.
 */
@Composable
fun HishabPatiCustomDateRangePickerDialog(
    initialStartDateMillis: Long,
    initialEndDateMillis: Long? = null,
    language: AppLanguage = AppLanguage.ENGLISH,
    onDateRangeSelected: (range: Pair<Long, Long>, formattedLabel: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val isBn = language == AppLanguage.BANGLA
    var step by remember { mutableIntStateOf(1) }
    var pickedStartMillis by remember(initialStartDateMillis) {
        mutableLongStateOf(HishabPatiDatePickerStyle.startOfDayMillis(initialStartDateMillis))
    }

    val defaultEndMillis = remember(pickedStartMillis, initialEndDateMillis) {
        val candidate = initialEndDateMillis ?: pickedStartMillis
        if (HishabPatiDatePickerStyle.startOfDayMillis(candidate) < pickedStartMillis) {
            pickedStartMillis
        } else {
            candidate
        }
    }

    HishabPatiDatePickerDialog(
        initialSelectedDateMillis = if (step == 1) pickedStartMillis else defaultEndMillis,
        titleText = if (step == 1) {
            if (isBn) "শুরুর তারিখ নির্বাচন করুন" else "Select Start Date"
        } else {
            if (isBn) "শেষের তারিখ নির্বাচন করুন" else "Select End Date"
        },
        language = language,
        minDateMillis = if (step == 1) null else pickedStartMillis,
        rangeStartMillis = if (step == 1) null else pickedStartMillis,
        onDateSelected = { selectedMillis ->
            if (step == 1) {
                pickedStartMillis = HishabPatiDatePickerStyle.startOfDayMillis(selectedMillis)
                step = 2
            } else {
                val normalizedStart = HishabPatiDatePickerStyle.startOfDayMillis(minOf(pickedStartMillis, selectedMillis))
                val normalizedEnd = HishabPatiDatePickerStyle.endOfDayMillis(maxOf(pickedStartMillis, selectedMillis))
                val label = HishabPatiDatePickerStyle.formatCustomRangeLabel(normalizedStart, normalizedEnd, language)
                onDateRangeSelected(normalizedStart to normalizedEnd, label)
            }
        },
        onDismissRequest = onDismissRequest
    )
}
