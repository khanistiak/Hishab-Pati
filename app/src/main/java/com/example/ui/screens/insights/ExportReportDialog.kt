package com.example.ui.screens.insights

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AppLanguage
import java.io.File
import java.io.FileOutputStream

enum class ExportFormat(
    val title: String,
    val titleBn: String,
    val subtitle: String,
    val subtitleBn: String,
    val extension: String,
    val mimeType: String
) {
    PDF(
        title = "PDF",
        titleBn = "পিডিএফ (PDF)",
        subtitle = "Formatted PDF document ready to view or print",
        subtitleBn = "দেখা বা প্রিন্ট করার উপযুক্ত পিডিএফ ফাইল",
        extension = "pdf",
        mimeType = "application/pdf"
    ),
    CSV(
        title = "CSV",
        titleBn = "সিএসভি (CSV)",
        subtitle = "Standard comma-separated values file",
        subtitleBn = "স্ট্যান্ডার্ড কমা-সেপারেটেড ডেটা ফাইল",
        extension = "csv",
        mimeType = "text/csv"
    ),
    EXCEL(
        title = "Excel (CSV)",
        titleBn = "এক্সেল (CSV)",
        subtitle = "Saved as a UTF-8 CSV file for Excel & spreadsheets",
        subtitleBn = "এক্সেল ও স্প্রেডশিটে খোলার উপযুক্ত CSV ফাইল",
        extension = "csv",
        mimeType = "text/csv"
    )
}

enum class ExportScope(
    val title: String,
    val titleBn: String,
    val subtitle: String,
    val subtitleBn: String,
    val fileSlug: String
) {
    EXPENSES_BY_CATEGORY(
        title = "Expenses by category",
        titleBn = "খাতভিত্তিক ব্যয়",
        subtitle = "Category & subcategory breakdown with spending share",
        subtitleBn = "খাত ও উপ-খাত অনুযায়ী মোট ব্যয় ও শতকরা হার",
        fileSlug = "Expenses_by_Category"
    ),
    TRANSACTIONS_IN_DATE_RANGE(
        title = "Transactions in date range",
        titleBn = "নির্দিষ্ট সময়ের লেনদেন",
        subtitle = "Detailed rows for all matching filtered transactions",
        subtitleBn = "নির্বাচিত সময়ের সকল ফিল্টারকৃত লেনদেনের তালিকা",
        fileSlug = "Transactions_in_Date_Range"
    );

    companion object {
        val CATEGORY_WISE = EXPENSES_BY_CATEGORY
        val DATE_RANGE = TRANSACTIONS_IN_DATE_RANGE
    }
}

data class ExportFilterContext(
    val periodLabel: String,
    val periodLabelBn: String = periodLabel,
    val transactionTypeLabel: String? = null,
    val transactionTypeLabelBn: String? = transactionTypeLabel,
    val categoryFilter: String? = null,
    val subcategoryFilter: String? = null,
    val walletFilter: String? = null,
    val searchQuery: String? = null,
    val isPreFiltered: Boolean = false
) {
    fun activeFilterChips(language: AppLanguage): List<String> {
        val isBn = language == AppLanguage.BANGLA
        val chips = mutableListOf<String>()
        val typeText = if (isBn) transactionTypeLabelBn else transactionTypeLabel
        if (!typeText.isNullOrBlank()) {
            chips.add("${if (isBn) "ধরণ" else "Type"}: $typeText")
        }
        if (!categoryFilter.isNullOrBlank()) {
            chips.add("${if (isBn) "খাত" else "Category"}: $categoryFilter")
        }
        if (!subcategoryFilter.isNullOrBlank()) {
            chips.add("${if (isBn) "উপ-খাত" else "Subcategory"}: $subcategoryFilter")
        }
        if (!walletFilter.isNullOrBlank()) {
            chips.add("${if (isBn) "ওয়ালেট" else "Wallet"}: $walletFilter")
        }
        if (!searchQuery.isNullOrBlank()) {
            chips.add("${if (isBn) "খোঁজ" else "Search"}: \"$searchQuery\"")
        }
        return chips
    }

    fun buildActiveFiltersSummary(language: AppLanguage): String {
        val isBn = language == AppLanguage.BANGLA
        val chips = activeFilterChips(language)
        return if (chips.isEmpty()) {
            if (isBn) "সকল লেনদেন" else "All transactions"
        } else {
            chips.joinToString(" • ")
        }
    }
}

data class GeneratedExportPayload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val format: ExportFormat,
    val scope: ExportScope
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportReportDialog(
    transactions: List<TransactionEntity>,
    dateRange: InsightsDateRange,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onExportComplete: (String) -> Unit,
    filterContext: ExportFilterContext? = null,
    accounts: List<AccountEntity> = emptyList(),
    initialScope: ExportScope = ExportScope.EXPENSES_BY_CATEGORY
) {
    val context = LocalContext.current
    val isBn = language == AppLanguage.BANGLA

    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var selectedScope by remember { mutableStateOf(initialScope) }
    var statusErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingSavePayload by remember { mutableStateOf<GeneratedExportPayload?>(null) }

    val effectivePeriodLabel = remember(filterContext, dateRange, isBn) {
        if (isBn) {
            filterContext?.periodLabelBn?.ifBlank { dateRange.formattedDisplayBn } ?: dateRange.formattedDisplayBn
        } else {
            filterContext?.periodLabel?.ifBlank { dateRange.formattedDisplay } ?: dateRange.formattedDisplay
        }
    }

    val matchingTransactions = remember(transactions, dateRange, selectedScope, filterContext) {
        InsightsCalculator.getExportableTransactionsForScope(
            transactions = transactions,
            dateRange = dateRange,
            scope = selectedScope,
            filterContext = filterContext
        )
    }

    val hasMatchingData = matchingTransactions.isNotEmpty()
    val filterChips = remember(filterContext, language) {
        filterContext?.activeFilterChips(language) ?: emptyList()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val payload = pendingSavePayload
        pendingSavePayload = null
        if (payload == null) return@rememberLauncherForActivityResult

        if (uri == null) {
            val cancelMsg = if (isBn) "ফাইল সংরক্ষণ বাতিল করা হয়েছে।" else "Export save cancelled."
            statusErrorMessage = cancelMsg
            onExportComplete(cancelMsg)
            return@rememberLauncherForActivityResult
        }

        try {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Unable to open selected destination.")
            stream.use { it.write(payload.bytes) }
            val successMsg = if (isBn) {
                "${payload.fileName} (${payload.format.titleBn}) সফলভাবে ফোনে সংরক্ষণ করা হয়েছে।"
            } else {
                "Saved ${payload.fileName} (${payload.format.title}) to storage."
            }
            onExportComplete(successMsg)
            onDismiss()
        } catch (e: Exception) {
            val errMsg = if (isBn) {
                "ফাইল সংরক্ষণ ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
            } else {
                "Failed to save report: ${e.localizedMessage ?: "Unknown error"}"
            }
            statusErrorMessage = errMsg
            onExportComplete(errMsg)
        }
    }

    fun buildPayload(): GeneratedExportPayload? {
        if (!hasMatchingData) {
            val emptyMsg = if (isBn) {
                "নির্বাচিত ফিল্টারে এক্সপোর্ট করার মতো কোনো লেনদেন নেই।"
            } else {
                "No transactions match the selected filters and report content to export."
            }
            statusErrorMessage = emptyMsg
            onExportComplete(emptyMsg)
            return null
        }

        val periodForFile = filterContext?.periodLabel?.ifBlank { dateRange.formattedDisplay } ?: dateRange.formattedDisplay
        val fileName = InsightsCalculator.buildExportFileName(
            scope = selectedScope,
            periodLabel = periodForFile,
            format = selectedFormat
        )

        val bytes: ByteArray = when (selectedFormat) {
            ExportFormat.PDF -> {
                InsightsCalculator.generatePdfReportBytes(
                    transactions = transactions,
                    dateRange = dateRange,
                    currencySymbol = currencySymbol,
                    categories = categories,
                    scope = selectedScope,
                    filterContext = filterContext,
                    accounts = accounts
                )
            }
            ExportFormat.CSV -> {
                InsightsCalculator.buildCsvReport(
                    transactions = transactions,
                    dateRange = dateRange,
                    currencySymbol = currencySymbol,
                    scope = selectedScope,
                    filterContext = filterContext,
                    accounts = accounts,
                    includeUtf8Bom = false
                ).toByteArray(Charsets.UTF_8)
            }
            ExportFormat.EXCEL -> {
                InsightsCalculator.buildCsvReport(
                    transactions = transactions,
                    dateRange = dateRange,
                    currencySymbol = currencySymbol,
                    scope = selectedScope,
                    filterContext = filterContext,
                    accounts = accounts,
                    includeUtf8Bom = true
                ).toByteArray(Charsets.UTF_8)
            }
        }

        if (bytes.isEmpty()) {
            val emptyMsg = if (isBn) {
                "এক্সপোর্ট করার জন্য কোনো ডেটা পাওয়া যায়নি।"
            } else {
                "No data available to export for the selected options."
            }
            statusErrorMessage = emptyMsg
            onExportComplete(emptyMsg)
            return null
        }

        return GeneratedExportPayload(
            fileName = fileName,
            mimeType = selectedFormat.mimeType,
            bytes = bytes,
            format = selectedFormat,
            scope = selectedScope
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_dialog_header")
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBn) "রিপোর্ট এক্সপোর্ট" else "Export Report",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("export_dialog_title")
                    )
                    Text(
                        text = if (isBn) "ফরম্যাট ও রিপোর্টের বিষয়বস্তু নির্বাচন করুন" else "Choose file format & report content",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("export_dialog_scroll_content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Period & Applied Filters Summary Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_filter_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (isBn) "সময়সীমা:" else "Period:"} $effectivePeriodLabel",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.testTag("export_period_label")
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (hasMatchingData) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = if (isBn) "${matchingTransactions.size} টি লেনদেন" else "${matchingTransactions.size} items",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasMatchingData) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("export_matching_count_badge")
                                )
                            }
                        }

                        if (filterChips.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_applied_filters_row")
                            ) {
                                filterChips.forEach { chipText ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = chipText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (isBn) "প্রয়োগকৃত ফিল্টার: সকল লেনদেন" else "Filters: All transactions in selected period",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                ),
                                modifier = Modifier.testTag("export_all_filters_label")
                            )
                        }
                    }
                }

                // 1. File Format Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isBn) "ফাইল ফরম্যাট" else "File format",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp
                        )
                    )

                    ExportFormat.entries.forEach { format ->
                        val isSelected = selectedFormat == format
                        val icon = when (format) {
                            ExportFormat.PDF -> Icons.Default.Description
                            ExportFormat.CSV -> Icons.Default.TableChart
                            ExportFormat.EXCEL -> Icons.Default.TableView
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedFormat = format
                                    statusErrorMessage = null
                                }
                                .testTag("export_format_option_${format.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFormat = format
                                        statusErrorMessage = null
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) format.titleBn else format.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                    Text(
                                        text = if (isBn) format.subtitleBn else format.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Report Content Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isBn) "রিপোর্টের বিষয়বস্তু" else "Report content",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp
                        )
                    )

                    ExportScope.entries.forEach { scope ->
                        val isSelected = selectedScope == scope
                        val scopeIcon = when (scope) {
                            ExportScope.EXPENSES_BY_CATEGORY -> Icons.Default.Category
                            ExportScope.TRANSACTIONS_IN_DATE_RANGE -> Icons.Default.ReceiptLong
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedScope = scope
                                    statusErrorMessage = null
                                }
                                .testTag("export_scope_option_${scope.name}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedScope = scope
                                        statusErrorMessage = null
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = scopeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) scope.titleBn else scope.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                    Text(
                                        text = if (isBn) scope.subtitleBn else scope.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty data or error explanation banner
                if (!hasMatchingData || statusErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_empty_data_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = statusErrorMessage ?: if (isBn) {
                                    "নির্বাচিত ফিল্টার এবং রিপোর্টের ধরণে এক্সপোর্ট করার মতো কোনো লেনদেন নেই।"
                                } else {
                                    "No transactions match the active filters for \"${selectedScope.title}\". No empty file will be created."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val payload = buildPayload() ?: return@OutlinedButton
                        pendingSavePayload = payload
                        try {
                            createDocumentLauncher.launch(payload.fileName)
                        } catch (e: Exception) {
                            // Fallback direct save to storage if SAF intent unavailable
                            val savedFile = saveReportBytesToStorage(context, payload)
                            if (savedFile != null) {
                                val msg = if (isBn) {
                                    "${payload.fileName} ফোনে সংরক্ষণ করা হয়েছে।"
                                } else {
                                    "Saved ${payload.fileName} to phone storage."
                                }
                                onExportComplete(msg)
                                onDismiss()
                            } else {
                                val errMsg = if (isBn) "সংরক্ষণ ব্যর্থ হয়েছে।" else "Failed to save file."
                                statusErrorMessage = errMsg
                                onExportComplete(errMsg)
                            }
                        }
                    },
                    enabled = hasMatchingData,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_export_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = if (isBn) "সংরক্ষণ করুন" else "Save to Device",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBn) "সেভ" else "Save",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        val payload = buildPayload() ?: return@Button
                        val savedFile = saveReportBytesToStorage(context, payload)
                        if (savedFile == null || !savedFile.exists() || savedFile.length() == 0L) {
                            val errMsg = if (isBn) {
                                "রিপোর্ট ফাইল তৈরি করতে সমস্যা হয়েছে।"
                            } else {
                                "Could not create export file. Please try again."
                            }
                            statusErrorMessage = errMsg
                            onExportComplete(errMsg)
                            return@Button
                        }

                        shareExportFile(
                            context = context,
                            file = savedFile,
                            title = "Hishab Pati - ${payload.scope.title} ($effectivePeriodLabel)",
                            mimeType = payload.mimeType
                        )

                        val successMsg = if (isBn) {
                            "${payload.fileName} (${payload.format.titleBn}) সফলভাবে এক্সপোর্ট করা হয়েছে।"
                        } else {
                            "Exported ${payload.fileName} (${payload.format.title}) successfully."
                        }
                        onExportComplete(successMsg)
                        onDismiss()
                    },
                    enabled = hasMatchingData,
                    modifier = Modifier.testTag("confirm_export_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "এক্সপোর্ট ও শেয়ার" else "Export & Share",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_export_btn")
            ) {
                Text(
                    text = if (isBn) "বাতিল" else "Cancel",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

internal fun saveReportBytesToStorage(
    context: Context,
    payload: GeneratedExportPayload
): File? {
    if (payload.bytes.isEmpty()) return null
    return try {
        // 1. Save into app's cache exports directory for FileProvider sharing
        val cacheExportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cacheFile = File(cacheExportsDir, payload.fileName)
        FileOutputStream(cacheFile).use { it.write(payload.bytes) }

        // 2. Also save into app-specific Documents directory on phone storage (no storage permission needed)
        val externalDocsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (externalDocsDir != null) {
            externalDocsDir.mkdirs()
            val externalFile = File(externalDocsDir, payload.fileName)
            FileOutputStream(externalFile).use { it.write(payload.bytes) }
        }

        // 3. On Android 10+ (API 29+), also publish to MediaStore.Downloads so user can open directly in Files/Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, payload.fileName)
                    put(MediaStore.Downloads.MIME_TYPE, payload.mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HishabPati")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (downloadUri != null) {
                    resolver.openOutputStream(downloadUri)?.use { it.write(payload.bytes) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(downloadUri, values, null, null)
                }
            } catch (_: Exception) {
                // Ignore MediaStore errors in test/restricted environments; cacheFile & externalDocsDir succeeded
            }
        }

        cacheFile
    } catch (_: Exception) {
        null
    }
}

private fun shareExportFile(
    context: Context,
    file: File,
    title: String,
    mimeType: String
) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        // Ignore if no activity is available in headless test environment
    }
}
