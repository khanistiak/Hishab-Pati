package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.ocr.ParsedReceiptData
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConfirmedReceiptDraft(
    val amount: Double,
    val title: String,
    val categoryName: String,
    val subcategoryName: String,
    val accountId: String,
    val dateMillis: Long?,
    val notes: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptReviewDialog(
    parsedReceipt: ParsedReceiptData,
    receiptBitmap: Bitmap?,
    isProcessingOcr: Boolean,
    isBangla: Boolean,
    currencySymbol: String,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    defaultAccountId: String,
    onRotateReceipt: () -> Unit,
    onRescanReceipt: () -> Unit,
    onApplyToForm: (ConfirmedReceiptDraft) -> Unit,
    onConfirmAndSave: (ConfirmedReceiptDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember(parsedReceipt) {
        mutableStateOf(
            parsedReceipt.totalAmount?.let { amt ->
                if (amt % 1.0 == 0.0) amt.toLong().toString()
                else String.format(Locale.US, "%.2f", amt)
            } ?: ""
        )
    }
    var titleText by remember(parsedReceipt) {
        mutableStateOf(parsedReceipt.merchantName ?: "")
    }
    var selectedCategory by remember(parsedReceipt) {
        mutableStateOf(parsedReceipt.suggestedCategory ?: "")
    }
    var selectedSubcategory by remember(parsedReceipt) {
        mutableStateOf(parsedReceipt.suggestedSubcategory ?: "")
    }
    var selectedAccountId by remember(parsedReceipt, defaultAccountId) {
        mutableStateOf(parsedReceipt.suggestedAccountId ?: defaultAccountId)
    }
    var showRawText by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val expenseCategories = remember(categories) {
        categories.filter { !it.isIncome && !it.isTransfer }
    }

    val amountDouble = amountText.trim().replace(",", "").toDoubleOrNull() ?: 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .testTag("receipt_review_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "রসিদ যাচাই করুন" else "Review Receipt",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("receipt_review_title")
                            )
                            Text(
                                text = if (isBangla) "সংরক্ষণের আগে রসিদের তথ্য যাচাই ও সম্পাদনা করুন"
                                else "Verify extracted receipt details before saving",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("receipt_review_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preview & Rotate / Rescan Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (receiptBitmap != null) {
                            Image(
                                bitmap = receiptBitmap.asImageBitmap(),
                                contentDescription = "Captured Receipt",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val dateDisplay = parsedReceipt.dateMillis?.let {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                            }
                            Text(
                                text = if (parsedReceipt.matchedTotalLabel != null) {
                                    if (isBangla) "শনাক্তকৃত লেবেল: ${parsedReceipt.matchedTotalLabel}"
                                    else "Matched label: ${parsedReceipt.matchedTotalLabel}"
                                } else {
                                    if (isBangla) "রসিদ স্ক্যান সম্পন্ন" else "Receipt scanned"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (dateDisplay != null) {
                                Text(
                                    text = if (isBangla) "তারিখ: $dateDisplay" else "Receipt Date: $dateDisplay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (receiptBitmap != null) {
                            IconButton(
                                onClick = onRotateReceipt,
                                enabled = !isProcessingOcr,
                                modifier = Modifier.testTag("receipt_rotate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = if (isBangla) "ছবি ঘোরান" else "Rotate 90°",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = onRescanReceipt,
                            enabled = !isProcessingOcr,
                            modifier = Modifier.testTag("receipt_rescan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = if (isBangla) "আবার ছবি তুলুন" else "Rescan Receipt",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isProcessingOcr) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "রসিদ পুনরায় পড়া হচ্ছে..." else "Re-reading rotated receipt...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Ambiguity / Low Confidence Warning Banner
                if (parsedReceipt.totalAmount == null || parsedReceipt.isAmountAmbiguous) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_ambiguity_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = ExpenseRed.copy(alpha = 0.11f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (parsedReceipt.totalAmount == null) {
                                    if (isBangla) {
                                        "রসিদে চূড়ান্ত মোট (Total) স্পষ্টভাবে পাওয়া যায়নি। নিচের সম্ভাব্য পরিমাণগুলো থেকে বেছে নিন বা নিজে লিখুন।"
                                    } else {
                                        "Could not find a clearly labeled final Total on this receipt. Select a candidate amount below or type the amount manually."
                                    }
                                } else {
                                    if (isBangla) {
                                        "রসিদে একাধিক সম্ভাব্য পরিমাণ দেখা গেছে। অনুগ্রহ করে সঠিক মোট পরিমাণটি নিশ্চিত করুন।"
                                    } else {
                                        "Multiple plausible amounts were found on this receipt. Please confirm or select the exact total below."
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ExpenseRed
                            )
                        }
                    }
                }

                // Candidate Amounts Selector Chips
                if (parsedReceipt.candidateAmounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBangla) "রসিদে প্রাপ্ত টাকার পরিমাণসমূহ (ট্যাপ করে নির্বাচন করুন):"
                        else "Detected Amounts on Receipt (Tap to select):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_candidate_amounts_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        parsedReceipt.candidateAmounts.forEachIndexed { index, candidate ->
                            val formattedCandidate = if (candidate.amount % 1.0 == 0.0) {
                                candidate.amount.toLong().toString()
                            } else {
                                String.format(Locale.US, "%.2f", candidate.amount)
                            }
                            val isSelected = amountDouble > 0.0 && kotlin.math.abs(amountDouble - candidate.amount) < 0.005
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    amountText = formattedCandidate
                                    validationError = null
                                },
                                label = {
                                    Text(
                                        text = "${candidate.label}: $currencySymbol$formattedCandidate",
                                        fontWeight = if (candidate.isPrimaryTotal) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("receipt_candidate_chip_$index")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Editable Total Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        validationError = null
                    },
                    label = {
                        Text(if (isBangla) "মোট পরিমাণ ($currencySymbol)" else "Total Amount ($currencySymbol)")
                    },
                    placeholder = {
                        Text(if (isBangla) "পরিমাণ লিখুন" else "Enter total amount")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_review_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Merchant / Transaction Name Field
                OutlinedTextField(
                    value = titleText,
                    onValueChange = {
                        titleText = it
                        validationError = null
                    },
                    label = {
                        Text(if (isBangla) "দোকান / লেনদেনের নাম" else "Merchant / Transaction Name")
                    },
                    placeholder = {
                        Text(if (isBangla) "যেমন: Shwapno" else "e.g., Shwapno")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_review_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                if (expenseCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "ক্যাটাগরি" else "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedCategory.ifBlank {
                                if (isBangla) "নির্বাচন করুন" else "Select category"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategory.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expenseCategories.forEach { cat ->
                            val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = cat.name
                                    selectedSubcategory = "General"
                                },
                                label = {
                                    Text(if (isBangla && cat.nameBn.isNotBlank()) cat.nameBn else cat.name)
                                }
                            )
                        }
                    }
                }

                // Wallet / Account Selector
                if (accounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBangla) "ওয়ালেট" else "Wallet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val accName = accounts.firstOrNull { it.id == selectedAccountId }?.name ?: ""
                        Text(
                            text = accName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name) }
                            )
                        }
                    }
                }

                // Collapsible Raw OCR Text
                if (parsedReceipt.rawText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showRawText = !showRawText },
                        modifier = Modifier.testTag("receipt_raw_text_toggle")
                    ) {
                        Text(
                            text = if (showRawText) {
                                if (isBangla) "রসিদের মূল টেক্সট লুকান" else "Hide Extracted Receipt Text"
                            } else {
                                if (isBangla) "রসিদের মূল টেক্সট দেখুন" else "Show Extracted Receipt Text"
                            }
                        )
                    }
                    if (showRawText) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = parsedReceipt.rawText.take(600),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                fun buildConfirmedDraft(): ConfirmedReceiptDraft {
                    val resolvedCat = selectedCategory.ifBlank {
                        expenseCategories.firstOrNull()?.name ?: "Food & Dining"
                    }
                    val resolvedSub = selectedSubcategory.ifBlank { "General" }
                    val resolvedTitle = titleText.trim().ifBlank { resolvedCat }
                    val resolvedAcc = selectedAccountId.ifBlank { accounts.firstOrNull()?.id ?: "" }
                    return ConfirmedReceiptDraft(
                        amount = amountDouble,
                        title = resolvedTitle,
                        categoryName = resolvedCat,
                        subcategoryName = resolvedSub,
                        accountId = resolvedAcc,
                        dateMillis = parsedReceipt.dateMillis,
                        notes = "Scanned Receipt:\n${parsedReceipt.rawText.take(220)}"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onApplyToForm(buildConfirmedDraft())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_apply_to_form_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "ফর্মে বসান" else "Edit in Form",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (amountDouble <= 0.0) {
                                validationError = if (isBangla) {
                                    "অনুগ্রহ করে সঠিক মোট টাকার পরিমাণ দিন।"
                                } else {
                                    "Please enter or select a valid total amount greater than 0."
                                }
                                return@Button
                            }
                            onConfirmAndSave(buildConfirmedDraft())
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("receipt_confirm_save_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "নিশ্চিত ও সেভ" else "Confirm & Save",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
