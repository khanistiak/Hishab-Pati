package com.example.ui.screens.plan

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.ui.MainViewModel
import com.example.ui.components.TranslucentCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class DebtDurationPreset(val label: String, val months: Int, val years: Int = 0) {
    M1("1M", months = 1),
    M2("2M", months = 2),
    M3("3M", months = 3),
    M6("6M", months = 6),
    Y1("1Y", months = 0, years = 1),
    CUSTOM("Custom Date", months = 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtToEdit by viewModel.debtToEdit.collectAsState()
    val initialType by viewModel.debtFilterType.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currencySymbol = settings?.currencySymbol ?: "৳"

    var debtType by remember { mutableStateOf(debtToEdit?.type ?: initialType) }
    var personName by remember { mutableStateOf(debtToEdit?.personName ?: "") }
    var reason by remember { mutableStateOf(debtToEdit?.reason ?: "") }
    var initialAmountStr by remember {
        mutableStateOf(if (debtToEdit != null) debtToEdit!!.totalAmount.toString() else "")
    }
    var notes by remember { mutableStateOf(debtToEdit?.notes ?: "") }
    var bindToWallet by remember { mutableStateOf(debtToEdit?.bindToWallet ?: false) }

    // Selected wallet ID
    var selectedWalletId by remember {
        val initialWallet = debtToEdit?.accountId?.takeIf { it.isNotBlank() }
            ?: accounts.firstOrNull()?.id
            ?: ""
        mutableStateOf(initialWallet)
    }

    val colorOptions = listOf(
        Color(0xFF3B82F6) to "#3B82F6",
        Color(0xFF8B5CF6) to "#8B5CF6",
        Color(0xFFEC4899) to "#EC4899",
        Color(0xFFEF4444) to "#EF4444",
        Color(0xFFF97316) to "#F97316",
        Color(0xFFF59E0B) to "#F59E0B"
    )
    var selectedColorIndex by remember {
        val idx = colorOptions.indexOfFirst { it.second.equals(debtToEdit?.colorHex, ignoreCase = true) }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    var createdDateMillis by remember { mutableStateOf(debtToEdit?.createdAt ?: System.currentTimeMillis()) }
    var tentativeCloseMillis by remember { mutableStateOf(debtToEdit?.tentativeCloseDate) }

    fun calculateClosing(startMillis: Long, preset: DebtDurationPreset): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            if (preset.years > 0) add(Calendar.YEAR, preset.years)
            else if (preset.months > 0) add(Calendar.MONTH, preset.months)
        }
        return cal.timeInMillis
    }

    // Determine initial active preset if tentativeCloseDate matches
    var activePreset by remember {
        val initialPreset: DebtDurationPreset? = if (debtToEdit?.tentativeCloseDate != null) {
            val close = debtToEdit!!.tentativeCloseDate!!
            val start = debtToEdit!!.createdAt
            val matching = DebtDurationPreset.entries.filter { it != DebtDurationPreset.CUSTOM }.find {
                val expected = calculateClosing(start, it)
                kotlin.math.abs(expected - close) <= 86400000L * 2
            }
            matching ?: DebtDurationPreset.CUSTOM
        } else null
        mutableStateOf(initialPreset)
    }

    var showCreatedDatePicker by remember { mutableStateOf(false) }
    var showTentativeDatePicker by remember { mutableStateOf(false) }

    val createdDateState = rememberDatePickerState(initialSelectedDateMillis = createdDateMillis)
    val tentativeDateState = rememberDatePickerState(initialSelectedDateMillis = tentativeCloseMillis ?: createdDateMillis)

    val isPayable = debtType == DebtType.BORROWED
    val activeColor = colorOptions[selectedColorIndex].first
    val activeColorHex = colorOptions[selectedColorIndex].second

    // Start Date Picker
    if (showCreatedDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showCreatedDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        createdDateState.selectedDateMillis?.let { newStart ->
                            createdDateMillis = newStart
                            // If preset is active, recalculate closing date from new start date
                            activePreset?.let { p ->
                                if (p != DebtDurationPreset.CUSTOM) {
                                    tentativeCloseMillis = calculateClosing(newStart, p)
                                } else if (tentativeCloseMillis != null && tentativeCloseMillis!! < newStart) {
                                    tentativeCloseMillis = newStart
                                }
                            }
                        }
                        showCreatedDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatedDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = createdDateState)
        }
    }

    // Custom Date Picker for Closing Date
    if (showTentativeDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showTentativeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosen = tentativeDateState.selectedDateMillis ?: createdDateMillis
                        // Validation: closing date cannot be earlier than start date
                        tentativeCloseMillis = if (chosen < createdDateMillis) createdDateMillis else chosen
                        activePreset = DebtDurationPreset.CUSTOM
                        showTentativeDatePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTentativeDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = tentativeDateState)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                    text = if (debtToEdit != null) "Edit Debt" else "Add Debt",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val amt = initialAmountStr.toDoubleOrNull() ?: 0.0
                            val name = if (personName.isBlank()) "Unknown" else personName.trim()
                            val createdStr = dateFormatter.format(Date(createdDateMillis))
                            val tentativeStr = tentativeCloseMillis?.let { dateFormatter.format(Date(it)) } ?: ""
                            val resolvedAccountId = if (bindToWallet) {
                                if (accounts.any { it.id == selectedWalletId }) selectedWalletId
                                else accounts.firstOrNull()?.id ?: ""
                            } else ""

                            val newDebt = if (debtToEdit != null) {
                                val newRemaining = (amt - debtToEdit!!.paidAmount).coerceAtLeast(0.0)
                                debtToEdit!!.copy(
                                    personName = name,
                                    amount = newRemaining,
                                    totalAmount = amt,
                                    isSettled = amt > 0.0 && newRemaining <= 0.001,
                                    type = debtType,
                                    reason = reason,
                                    colorHex = activeColorHex,
                                    createdAt = createdDateMillis,
                                    createdDateStr = createdStr,
                                    tentativeCloseDate = tentativeCloseMillis,
                                    tentativeCloseDateStr = tentativeStr,
                                    notes = notes,
                                    tags = debtToEdit?.tags ?: "",
                                    includeInBalance = true,
                                    bindToWallet = bindToWallet && resolvedAccountId.isNotBlank(),
                                    accountId = resolvedAccountId
                                )
                            } else {
                                DebtEntity(
                                    id = "debt_${UUID.randomUUID().toString().take(8)}",
                                    personName = name,
                                    amount = amt,
                                    totalAmount = amt,
                                    paidAmount = 0.0,
                                    type = debtType,
                                    reason = reason,
                                    colorHex = activeColorHex,
                                    createdAt = createdDateMillis,
                                    createdDateStr = createdStr,
                                    tentativeCloseDate = tentativeCloseMillis,
                                    tentativeCloseDateStr = tentativeStr,
                                    notes = notes,
                                    tags = "",
                                    includeInBalance = true,
                                    bindToWallet = bindToWallet && resolvedAccountId.isNotBlank(),
                                    accountId = resolvedAccountId,
                                    isSettled = false
                                )
                            }

                            if (debtToEdit != null) {
                                viewModel.updateDebt(newDebt)
                            } else {
                                viewModel.insertDebt(newDebt)
                            }
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_debt_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A303C),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (debtToEdit != null) "Update Debt" else "Save Debt",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        val listState = viewModel.rememberScreenLazyListState(
            subKey = debtToEdit?.id ?: "new",
            screenOverride = com.example.ui.SubScreen.ADD_DEBT
        )
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .testTag("add_debt_lazy_column"),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Segmented switch: Payable vs Receivable
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isPayable) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isPayable) 2.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { debtType = DebtType.BORROWED }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Borrowed",
                                        tint = if (isPayable) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Borrowed",
                                        fontWeight = if (isPayable) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = if (isPayable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (!isPayable) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (!isPayable) 2.dp else 0.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { debtType = DebtType.LENT }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Lent",
                                        tint = if (!isPayable) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Lent",
                                        fontWeight = if (!isPayable) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = if (!isPayable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Live Preview Card
            item {
                TranslucentCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initialLetter = personName.trim().take(1).uppercase(Locale.US).ifEmpty { "A" }
                        Surface(
                            shape = CircleShape,
                            color = activeColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initialLetter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = activeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (personName.isNotBlank()) personName else "Person Name",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (personName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                            Text(
                                text = if (reason.isNotBlank()) reason else (if (isPayable) "Money borrowed" else "Money lent"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPayable) ExpenseRed.copy(alpha = 0.12f) else IncomeGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isPayable) "Borrowed" else "Lent",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                color = if (isPayable) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }
            }

            // Unified Form Card: Core Details
            item {
                TranslucentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Date & Closing Date Summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCreatedDatePicker = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Start Date",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    )
                                    Text(
                                        text = dateFormatter.format(Date(createdDateMillis)),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            tentativeCloseMillis?.let { closeDate ->
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Tentative Close",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                        )
                                    )
                                    Text(
                                        text = dateFormatter.format(Date(closeDate)),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }

                        // Compact Debt Duration Selector (1M, 2M, 3M, 6M, 1Y, Custom Date)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(DebtDurationPreset.entries) { preset ->
                                    val isSelected = activePreset == preset
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (preset == DebtDurationPreset.CUSTOM) {
                                                showTentativeDatePicker = true
                                            } else {
                                                activePreset = preset
                                                tentativeCloseMillis = calculateClosing(createdDateMillis, preset)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = preset.label,
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("duration_preset_${preset.name}")
                                    )
                                }
                                if (tentativeCloseMillis != null) {
                                    item {
                                        TextButton(
                                            onClick = {
                                                tentativeCloseMillis = null
                                                activePreset = null
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "Clear",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        // Person Name Field
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            label = { Text(if (isPayable) "Who do you owe?" else "Who owes you?") },
                            placeholder = { Text("Person or company name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_person_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Reason / Category Field
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("Reason / Purpose") },
                            placeholder = { Text("e.g. Loan, Shopping, Emergency") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_reason_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Initial Amount Field with currency symbol
                        OutlinedTextField(
                            value = initialAmountStr,
                            onValueChange = { initialAmountStr = it },
                            label = { Text(if (isPayable) "Initial amount borrowed" else "Initial amount lent") },
                            leadingIcon = {
                                Text(
                                    text = "$currencySymbol ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_amount_input"),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        // Color Palette Selector (Compact Row)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Color",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                colorOptions.forEachIndexed { index, (color, _) ->
                                    val isSelected = selectedColorIndex == index
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable { selectedColorIndex = index }
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
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

                        // Notes Field
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)") },
                            placeholder = { Text("Add any terms, references, or details") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("debt_notes_input"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // Accounting Settings Card: Include in Balance & Bind to Wallet
            item {
                TranslucentCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        // Bind to Wallet Toggle (Requirement 5)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bind to Wallet",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isPayable) "Money borrowed will be credited to selected wallet"
                                    else "Money lent will be debited from selected wallet",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Switch(
                                checked = bindToWallet,
                                onCheckedChange = {
                                    bindToWallet = it
                                    if (it && selectedWalletId.isEmpty() && accounts.isNotEmpty()) {
                                        selectedWalletId = accounts.first().id
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2A303C)
                                ),
                                modifier = Modifier.testTag("bind_to_wallet_toggle")
                            )
                        }

                        // Wallet Selector when Bind to Wallet is active
                        if (bindToWallet) {
                            Spacer(modifier = Modifier.height(2.dp))
                            if (accounts.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "No wallets available. Please create an account in Wallets first.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Select Linked Wallet:",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    accounts.forEach { acc ->
                                        val isSelected = selectedWalletId == acc.id
                                        val accColor = try {
                                            Color(android.graphics.Color.parseColor(acc.colorHex))
                                        } catch (_: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedWalletId = acc.id }
                                                .testTag("wallet_option_${acc.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = accColor.copy(alpha = 0.2f),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = when (acc.type) {
                                                                AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                                                                AccountType.BANK -> Icons.Default.AccountBalance
                                                                AccountType.CASH -> Icons.Default.Payments
                                                                AccountType.MFS -> Icons.Default.PhoneAndroid
                                                                else -> Icons.Default.Wallet
                                                            },
                                                            contentDescription = null,
                                                            tint = accColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = acc.name,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                    if (acc.institution.isNotBlank()) {
                                                        Text(
                                                            text = acc.institution,
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                                                fontSize = 11.sp
                                                            )
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = CurrencyFormatter.format(acc.balance, currencySymbol),
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )

                                                Spacer(modifier = Modifier.width(6.dp))

                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedWalletId = acc.id },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = MaterialTheme.colorScheme.primary
                                                    )
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

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
