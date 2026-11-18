package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.backup.BackupDiagnosticError
import com.example.data.backup.BackupFormatType
import com.example.data.backup.BackupPreview
import com.example.data.backup.RestoreMode
import com.example.data.backup.RestoreProgress
import com.example.data.backup.RestoreResultSummary
import com.example.data.backup.RestoreStage
import com.example.data.backup.RestoreUiState
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.CurrencyFormatter
import java.util.Locale

@Composable
fun BackupRestoreModal(
    state: RestoreUiState,
    isBangla: Boolean,
    currencySymbol: String,
    onSelectMode: (RestoreMode) -> Unit,
    onProceedToConfirm: () -> Unit,
    onBackToPreview: () -> Unit,
    onExecuteRestore: (RestoreMode) -> Unit,
    onExportFreshBackup: () -> Unit,
    onPickAnotherFile: () -> Unit,
    onGoToDashboard: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state is RestoreUiState.Idle) return

    val isRestoring = state is RestoreUiState.Step3Restoring
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF1A1E2E) else Color.White

    Dialog(
        onDismissRequest = {
            if (!isRestoring) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isRestoring,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("backup_restore_modal"),
            shape = RoundedCornerShape(28.dp),
            color = cardBg,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            when (state) {
                is RestoreUiState.Step1Preview -> {
                    Step1BackupPreviewContent(
                        preview = state.preview,
                        selectedMode = state.selectedMode,
                        isBangla = isBangla,
                        isDark = isDark,
                        onSelectMode = onSelectMode,
                        onProceedToConfirm = onProceedToConfirm,
                        onPickAnotherFile = onPickAnotherFile,
                        onDismiss = onDismiss
                    )
                }
                is RestoreUiState.Step2Confirm -> {
                    Step2ConfirmationContent(
                        preview = state.preview,
                        mode = state.mode,
                        isBangla = isBangla,
                        isDark = isDark,
                        onBack = onBackToPreview,
                        onExportFreshBackup = onExportFreshBackup,
                        onConfirmRestore = { onExecuteRestore(state.mode) },
                        onCancel = onDismiss
                    )
                }
                is RestoreUiState.Step3Restoring -> {
                    Step3RestoringProgressContent(
                        progress = state.progress,
                        mode = state.mode,
                        isBangla = isBangla,
                        isDark = isDark
                    )
                }
                is RestoreUiState.Step4Success -> {
                    Step4RestoreSuccessContent(
                        summary = state.summary,
                        isBangla = isBangla,
                        currencySymbol = currencySymbol,
                        isDark = isDark,
                        onGoToDashboard = onGoToDashboard
                    )
                }
                is RestoreUiState.Step4Error -> {
                    Step4RestoreErrorContent(
                        error = state.error,
                        fileName = state.fileName,
                        isBangla = isBangla,
                        isDark = isDark,
                        onPickAnotherFile = onPickAnotherFile,
                        onDismiss = onDismiss
                    )
                }
                RestoreUiState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun Step1BackupPreviewContent(
    preview: BackupPreview,
    selectedMode: RestoreMode,
    isBangla: Boolean,
    isDark: Boolean,
    onSelectMode: (RestoreMode) -> Unit,
    onProceedToConfirm: () -> Unit,
    onPickAnotherFile: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore Backup",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBangla) "ব্যাকআপ সারাংশ ও যাচাই" else "Backup Summary",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBangla) "ধাপ ১/৩ • ফাইল যাচাই সম্পন্ন" else "Step 1 of 3 • File verified",
                        style = MaterialTheme.typography.bodySmall,
                        color = IncomeGreen
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("restore_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File Metadata Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = preview.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = formatFileSize(preview.fileSizeBytes),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isBangla) "ফরম্যাট" else "Format",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${if (isBangla) preview.formatType.displayNameBn else preview.formatType.displayName} (v${preview.schemaVersion})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isBangla) "তারিখ" else "Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = preview.backupDate.take(19).replace("T", " "),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Migration / Deduplication Notice if applicable
        if (preview.requiresMigration || preview.detectedDuplicatesCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            if (preview.requiresMigration) {
                                append(
                                    if (isBangla) "লিগ্যাসি ফরম্যাট কনভার্ট করা হয়েছে। "
                                    else "Legacy format converted. "
                                )
                            }
                            if (preview.detectedDuplicatesCount > 0) {
                                append(
                                    if (isBangla) "${preview.detectedDuplicatesCount}টি ডুপ্লিকেট বাদ দেওয়া হয়েছে।"
                                    else "${preview.detectedDuplicatesCount} duplicates skipped."
                                )
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Records Summary Grid
        Text(
            text = if (isBangla) "শনাক্তকৃত রেকর্ডসমূহ" else "Records in Backup",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackupMetricChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBalance,
                    label = if (isBangla) "অ্যাকাউন্ট" else "Accounts",
                    count = preview.accountsCount,
                    accent = Color(0xFF2563EB),
                    isDark = isDark
                )
                BackupMetricChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ReceiptLong,
                    label = if (isBangla) "লেনদেন" else "Transactions",
                    count = preview.transactionsCount,
                    accent = Color(0xFF10B981),
                    isDark = isDark
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackupMetricChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Category,
                    label = if (isBangla) "ক্যাটাগরি" else "Categories",
                    count = preview.categoriesCount,
                    accent = Color(0xFF8B5CF6),
                    isDark = isDark
                )
                BackupMetricChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Category,
                    label = if (isBangla) "সাবক্যাটাগরি" else "Subcategories",
                    count = preview.subcategoriesCount,
                    accent = Color(0xFFEC4899),
                    isDark = isDark
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BackupMetricChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PieChart,
                    label = if (isBangla) "বাজেট" else "Budgets",
                    count = preview.budgetsCount,
                    accent = Color(0xFFF59E0B),
                    isDark = isDark
                )
                BackupMetricChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.SwapHoriz,
                    label = if (isBangla) "ঋণ ও ধার" else "Debts & Loans",
                    count = preview.debtsCount,
                    accent = Color(0xFFEF4444),
                    isDark = isDark
                )
            }
            if (preview.savingsGoalsCount > 0 || preview.hasSettings) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF252A3D) else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) "অন্যান্য রেকর্ড" else "Other Records",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        Text(
                            text = buildString {
                                if (preview.hasSettings) append(if (isBangla) "সেটিংস" else "App Settings")
                                if (preview.savingsGoalsCount > 0) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${preview.savingsGoalsCount} Goals")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Restoration Mode Selection
        Text(
            text = if (isBangla) "রিস্টোর মোড" else "Restoration Mode",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        RestoreModeOptionCard(
            selected = selectedMode == RestoreMode.REPLACE_ALL,
            title = if (isBangla) "সব প্রতিস্থাপন (Replace All)" else "Replace All",
            description = if (isBangla)
                "বর্তমান সব ডাটা মুছে ব্যাকআপ দিয়ে প্রতিস্থাপন করবে।"
            else
                "Overwrites existing data with this backup.",
            badgeText = if (isBangla) "সব মুছবে" else "Destructive",
            badgeColor = ExpenseRed,
            isDark = isDark,
            testTag = "mode_replace_all_card",
            onClick = { onSelectMode(RestoreMode.REPLACE_ALL) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        RestoreModeOptionCard(
            selected = selectedMode == RestoreMode.MERGE,
            title = if (isBangla) "ডাটা মার্জ (Merge)" else "Merge Data",
            description = if (isBangla)
                "বর্তমান ডাটা রেখে নতুন রেকর্ড যোগ করবে।"
            else
                "Keeps current data and adds new records.",
            badgeText = if (isBangla) "নিরাপদ" else "Safe",
            badgeColor = IncomeGreen,
            isDark = isDark,
            testTag = "mode_merge_card",
            onClick = { onSelectMode(RestoreMode.MERGE) }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onPickAnotherFile,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pick_another_file_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBangla) "অন্য ফাইল" else "Change File",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onProceedToConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .testTag("proceed_to_confirm_btn")
            ) {
                Text(
                    text = if (isBangla) "পরবর্তী ধাপ" else "Continue",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun Step2ConfirmationContent(
    preview: BackupPreview,
    mode: RestoreMode,
    isBangla: Boolean,
    isDark: Boolean,
    onBack: () -> Unit,
    onExportFreshBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancel: () -> Unit
) {
    val isReplaceAll = mode == RestoreMode.REPLACE_ALL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        // Caution Icon Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = (if (isReplaceAll) ExpenseRed else MaterialTheme.colorScheme.primary).copy(alpha = 0.14f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isReplaceAll) Icons.Default.WarningAmber else Icons.Default.MergeType,
                        contentDescription = "Confirmation Warning",
                        tint = if (isReplaceAll) ExpenseRed else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isReplaceAll) {
                    if (isBangla) "বর্তমান ডাটা প্রতিস্থাপন করবেন?" else "Replace Existing Data?"
                } else {
                    if (isBangla) "ডাটা মার্জ নিশ্চিত করুন" else "Confirm Data Merge"
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBangla) "ধাপ ২/৩ • চূড়ান্ত নিশ্চিতকরণ" else "Step 2 of 3 • Final Confirmation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning / Explanation Box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isReplaceAll) {
                ExpenseRed.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            },
            border = BorderStroke(
                1.dp,
                if (isReplaceAll) ExpenseRed.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isReplaceAll) {
                        if (isBangla)
                            "⚠️ বর্তমান সকল অ্যাকাউন্ট, লেনদেন ও বাজেট মুছে এই ব্যাকআপের ${preview.totalRecordsCount}টি রেকর্ড দিয়ে প্রতিস্থাপিত হবে।"
                        else
                            "⚠️ All current accounts, transactions, and budgets will be replaced with ${preview.totalRecordsCount} records from this backup."
                    } else {
                        if (isBangla)
                            "বর্তমান ডাটার সাথে ${preview.totalRecordsCount}টি নতুন রেকর্ড যোগ করা হবে। ডুপ্লিকেট বাদ দেওয়া হবে।"
                        else
                            "${preview.totalRecordsCount} records will be merged with your existing data. Duplicates are skipped."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla)
                            "ব্যর্থ হলে পূর্বের ডাটা অক্ষত থাকবে।"
                        else
                            "Safe rollback: current data is kept if restore fails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Recommendation to create a fresh backup first
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBangla) "আগে ব্যাকআপ রাখবেন?" else "Backup current wallet?",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBangla)
                            "প্রতিস্থাপনের আগে একটি কপি সংরক্ষণ করুন।"
                        else
                            "Save a quick copy before overwriting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onExportFreshBackup,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("export_fresh_backup_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "ব্যাকআপ" else "Backup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("confirm_back_btn")
            ) {
                Text(
                    text = if (isBangla) "পেছনে" else "Back",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onConfirmRestore,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReplaceAll) ExpenseRed else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("confirm_start_restore_btn")
            ) {
                Text(
                    text = if (isReplaceAll) {
                        if (isBangla) "প্রতিস্থাপন করুন" else "Replace & Restore"
                    } else {
                        if (isBangla) "মার্জ করুন" else "Merge Data"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun Step3RestoringProgressContent(
    progress: RestoreProgress,
    mode: RestoreMode,
    isBangla: Boolean,
    isDark: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "restore_progress"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(84.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(84.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                strokeWidth = 6.dp,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "${progress.percentage}%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isBangla) "আপনার ওয়ালেট পুনরুদ্ধার করা হচ্ছে" else "Restoring Your Wallet",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isBangla)
                "ধাপ ${progress.stage.stepNumber}/${progress.stage.totalSteps} • ${progress.stage.titleBn}"
            else
                "Stage ${progress.stage.stepNumber} of ${progress.stage.totalSteps} • ${progress.stage.titleEn}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (progress.isIndeterminate) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        } else {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (progress.totalRecords > 0) {
                    Text(
                        text = if (isBangla)
                            "${progress.processedRecords} / ${progress.totalRecords} রেকর্ড প্রসেস করা হয়েছে"
                        else
                            "${progress.processedRecords} of ${progress.totalRecords} records processed",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = progress.statusDetail.ifBlank {
                        if (isBangla) progress.stage.titleBn else progress.stage.titleEn
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isBangla)
                "রিস্টোর হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন..."
            else
                "Restoring data, please wait...",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Step4RestoreSuccessContent(
    summary: RestoreResultSummary,
    isBangla: Boolean,
    currencySymbol: String,
    isDark: Boolean,
    onGoToDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = IncomeGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = IncomeGreen,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isBangla) "ওয়ালেট সফলভাবে পুনরুদ্ধার হয়েছে!" else "Wallet Restored Successfully!",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isBangla)
                "মোট ${summary.totalRestoredRecords}টি রেকর্ড যাচাই ও সংরক্ষণ করা হয়েছে"
            else
                "${summary.totalRestoredRecords} total records imported and verified",
            style = MaterialTheme.typography.bodyMedium,
            color = IncomeGreen,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Breakdown Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatRow(
                    label = if (isBangla) "পুনরুদ্ধার মোড" else "Restore Mode",
                    value = if (isBangla) summary.mode.titleBn else summary.mode.titleEn
                )
                SummaryStatRow(
                    label = if (isBangla) "ইমপোর্টকৃত লেনদেন" else "Imported Transactions",
                    value = "${summary.transactionsRestored}"
                )
                SummaryStatRow(
                    label = if (isBangla) "পুনরুদ্ধারকৃত অ্যাকাউন্ট" else "Accounts Restored",
                    value = "${summary.accountsRestored}"
                )
                SummaryStatRow(
                    label = if (isBangla) "ক্যাটাগরি ও সাবক্যাটাগরি" else "Categories & Subcategories",
                    value = "${summary.categoriesRestored} / ${summary.subcategoriesRestored}"
                )
                SummaryStatRow(
                    label = if (isBangla) "বাজেট ও ঋণ" else "Budgets & Debts",
                    value = "${summary.budgetsRestored} budgets • ${summary.debtsRestored} debts"
                )
                if (summary.skippedDuplicatesCount > 0) {
                    SummaryStatRow(
                        label = if (isBangla) "বাদ দেওয়া ডুপ্লিকেট রেকর্ড" else "Skipped Duplicates",
                        value = "${summary.skippedDuplicatesCount}",
                        valueColor = Color(0xFFF59E0B)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SummaryStatRow(
                    label = if (isBangla) "যাচাইকৃত নিট সম্পদ" else "Verified Net Worth",
                    value = CurrencyFormatter.format(summary.verifiedNetWorth, currencySymbol),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Integrity Verified Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = IncomeGreen.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBangla)
                        "ড্যাশবোর্ড ব্যালেন্স, বাজেট এবং ক্যাটাগরি হিসাব যাচাই করা হয়েছে।"
                    else
                        "Dashboard balances, budgets, and category analytics have been refreshed and verified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onGoToDashboard,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("go_to_dashboard_btn")
        ) {
            Text(
                text = if (isBangla) "ড্যাশবোর্ডে যান" else "Go to Dashboard",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}

@Composable
private fun Step4RestoreErrorContent(
    error: BackupDiagnosticError,
    fileName: String,
    isBangla: Boolean,
    isDark: Boolean,
    onPickAnotherFile: () -> Unit,
    onDismiss: () -> Unit
) {
    var showTechDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = ExpenseRed.copy(alpha = 0.14f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Restore Error",
                    tint = ExpenseRed,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isBangla) error.userTitleBn else error.userTitleEn,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isBangla) error.userMessageBn else error.userMessageEn,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Safety Preservation Reassurance
        if (error.existingDataPreserved) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = IncomeGreen.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla)
                            "আপনার বর্তমান ওয়ালেট ডাটা সম্পূর্ণ অক্ষুণ্ণ ও নিরাপদ রয়েছে।"
                        else
                            "Your existing wallet data has been preserved and was not changed.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Expandable Technical Diagnostics
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTechDetails = !showTechDetails }
                .testTag("toggle_technical_details_btn")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBangla) "টেকনিক্যাল বিবরণ (ডিবাগিং)" else "Technical Details",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (showTechDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showTechDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Code: ${error.errorCode.code}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = ExpenseRed
                        )
                        Text(
                            text = "Stage: ${error.failedStage.stepNumber}/${error.failedStage.totalSteps} (${error.failedStage.titleEn})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (fileName.isNotBlank()) {
                            Text(
                                text = "File: $fileName",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Diagnostic: ${error.technicalDetails}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("error_close_btn")
            ) {
                Text(
                    text = if (isBangla) "বন্ধ করুন" else "Close",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onPickAnotherFile,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("error_try_another_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBangla) "অন্য ফাইল বাছুন" else "Select Another File",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BackupMetricChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    count: Int,
    accent: Color,
    isDark: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RestoreModeOptionCard(
    selected: Boolean,
    title: String,
    description: String,
    badgeText: String,
    badgeColor: Color,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.08f)
    } else {
        if (isDark) Color(0xFF252A3D) else Color(0xFFF8FAFC)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}
