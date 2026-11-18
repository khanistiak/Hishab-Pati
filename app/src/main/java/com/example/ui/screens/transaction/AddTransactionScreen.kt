package com.example.ui.screens.transaction

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ocr.ParsedReceiptData
import com.example.ocr.ReceiptOcrManager
import com.example.ui.components.ConfirmedReceiptDraft
import com.example.ui.components.ReceiptReviewDialog
import com.example.ui.components.VoiceTransactionDialog
import com.example.voice.VoiceTransactionParser
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.HishabPatiDatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.components.CalculatorEvaluator
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.CategoryIcon
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.util.CurrencyFormatter
import com.example.ui.util.DateFormatter
import com.example.data.model.AppLanguage
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.ui.screens.settings.AddEditCategoryPanel
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.zIndex
import com.example.ui.ActiveWalletSheet
import com.example.ui.SubScreen
import com.example.ui.TransactionDraftState
import com.example.ui.screens.accounts.AdjustBalanceScreen
import com.example.ui.screens.accounts.EditAccountScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val txToEdit by viewModel.transactionToEdit.collectAsState()
    val initialType by viewModel.newTransactionInitialType.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val subcategories by viewModel.subcategories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val optimisticAccounts by viewModel.optimisticAccounts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val extendedSettings by viewModel.extendedSettings.collectAsState()
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()

    val effectiveAccounts = remember(accounts, optimisticAccounts) {
        val existingIds = accounts.map { it.id }.toSet()
        accounts + optimisticAccounts.filter { it.id !in existingIds }
    }

    val activeDraft = remember(txToEdit?.id) {
        viewModel.transactionDraftState.value?.takeIf { it.editingTransactionId == txToEdit?.id }
    }

    val currencySymbol = settings?.currencySymbol ?: "৳"
    val language = settings?.language ?: AppLanguage.ENGLISH
    val isBn = language == AppLanguage.BANGLA
    val focusManager = LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    var selectedType by remember(txToEdit, initialType) {
        mutableStateOf(activeDraft?.selectedType ?: txToEdit?.type ?: initialType)
    }

    var expression by remember(txToEdit) {
        mutableStateOf(
            activeDraft?.expression ?: if (txToEdit != null) {
                if (txToEdit!!.amount % 1.0 == 0.0) txToEdit!!.amount.toLong().toString()
                else String.format(Locale.US, "%.2f", txToEdit!!.amount)
            } else ""
        )
    }

    var isCalculatorVisible by remember(txToEdit, extendedSettings.autoOpenAmountKeypad) {
        mutableStateOf(
            activeDraft?.isCalculatorVisible
                ?: (txToEdit == null || extendedSettings.autoOpenAmountKeypad)
        )
    }

    LaunchedEffect(isCalculatorVisible) {
        if (isCalculatorVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Transaction Name (formerly Merchant/Payee)
    var title by remember(txToEdit) {
        mutableStateOf(activeDraft?.title ?: txToEdit?.title ?: "")
    }

    var optimisticCategories by remember(txToEdit) {
        mutableStateOf(activeDraft?.optimisticCategories ?: emptyList())
    }
    var optimisticSubcategories by remember(txToEdit) {
        mutableStateOf(activeDraft?.optimisticSubcategories ?: emptyList())
    }
    var newlyCreatedCategoryName by remember(txToEdit) {
        mutableStateOf(activeDraft?.newlyCreatedCategoryName)
    }

    val effectiveCategories = remember(categories, optimisticCategories) {
        val existingIds = categories.map { it.id }.toSet()
        val existingKeys = categories.map { "${it.name.lowercase().trim()}_${it.isIncome}_${it.isTransfer}" }.toSet()
        categories + optimisticCategories.filter {
            it.id !in existingIds && "${it.name.lowercase().trim()}_${it.isIncome}_${it.isTransfer}" !in existingKeys
        }
    }

    val effectiveSubcategories = remember(subcategories, optimisticSubcategories) {
        val existingIds = subcategories.map { it.id }.toSet()
        subcategories + optimisticSubcategories.filter { it.id !in existingIds }
    }

    var selectedCategoryName by remember(txToEdit) {
        mutableStateOf(
            activeDraft?.selectedCategoryName
                ?: txToEdit?.category
                ?: effectiveCategories.firstOrNull {
                    if (selectedType == TransactionType.INCOME) it.isIncome else (!it.isIncome && !it.isTransfer)
                }?.name
                ?: "Daily Essentials"
        )
    }

    LaunchedEffect(effectiveCategories, selectedType) {
        if (txToEdit == null && effectiveCategories.isNotEmpty()) {
            val matchesCurrentType = effectiveCategories.any {
                it.name == selectedCategoryName &&
                    (if (selectedType == TransactionType.INCOME) it.isIncome else (!it.isIncome && !it.isTransfer))
            }
            if (!matchesCurrentType) {
                effectiveCategories.firstOrNull {
                    if (selectedType == TransactionType.INCOME) it.isIncome else (!it.isIncome && !it.isTransfer)
                }?.let { firstCat ->
                    selectedCategoryName = firstCat.name
                }
            }
        }
    }

    var selectedSubcategoryName by remember(txToEdit) {
        mutableStateOf(activeDraft?.selectedSubcategoryName ?: txToEdit?.subcategory ?: "")
    }

    val preselectedAccId = viewModel.preselectedAccountIdForNewTx.collectAsState().value
    val resolvedDefaultWallet = remember(effectiveAccounts, extendedSettings.defaultWalletId) {
        com.example.data.local.UserSettingsPreferences.resolveDefaultWallet(effectiveAccounts, extendedSettings.defaultWalletId)
    }
    var userManuallyChangedWallet by remember(txToEdit) {
        mutableStateOf(activeDraft?.userManuallyChangedWallet ?: false)
    }

    var selectedAccountId by remember(txToEdit, preselectedAccId) {
        mutableStateOf(
            activeDraft?.selectedAccountId?.takeIf { it.isNotBlank() }
                ?: txToEdit?.accountId
                ?: preselectedAccId
                ?: resolvedDefaultWallet?.id
                ?: ""
        )
    }

    var selectedToAccountId by remember(txToEdit) {
        val nonHidden = effectiveAccounts.filter { !it.isHidden }
        val initialFrom = activeDraft?.selectedAccountId?.takeIf { it.isNotBlank() }
            ?: txToEdit?.accountId
            ?: preselectedAccId
            ?: resolvedDefaultWallet?.id
            ?: ""
        mutableStateOf(
            activeDraft?.selectedToAccountId?.takeIf { it.isNotBlank() }
                ?: txToEdit?.toAccountId
                ?: nonHidden.firstOrNull { it.id != initialFrom }?.id
                ?: nonHidden.firstOrNull()?.id
                ?: ""
        )
    }

    LaunchedEffect(effectiveAccounts, resolvedDefaultWallet?.id, preselectedAccId, txToEdit) {
        if (txToEdit == null && !userManuallyChangedWallet) {
            val activeIds = effectiveAccounts.filter { !it.isHidden }.map { it.id }.toSet()
            val targetId = preselectedAccId?.takeIf { it in activeIds } ?: resolvedDefaultWallet?.id ?: ""
            if (targetId.isNotBlank() && (selectedAccountId.isBlank() || selectedAccountId !in activeIds || preselectedAccId == null)) {
                selectedAccountId = targetId
            }
            if (selectedToAccountId.isBlank() || selectedToAccountId == selectedAccountId || selectedToAccountId !in activeIds) {
                val other = effectiveAccounts.firstOrNull { !it.isHidden && it.id != selectedAccountId }
                    ?: effectiveAccounts.firstOrNull { !it.isHidden }
                if (other != null) {
                    selectedToAccountId = other.id
                }
            }
        }
    }

    var selectedDateTimestamp by remember(txToEdit) {
        mutableStateOf(activeDraft?.selectedDateTimestamp ?: txToEdit?.timestamp ?: System.currentTimeMillis())
    }

    var notes by remember(txToEdit) {
        mutableStateOf(activeDraft?.notes ?: txToEdit?.notes ?: "")
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSourceWalletSheet by remember(txToEdit) {
        mutableStateOf(activeDraft?.activeWalletSheet == ActiveWalletSheet.SOURCE)
    }
    var showDestinationWalletSheet by remember(txToEdit) {
        mutableStateOf(activeDraft?.activeWalletSheet == ActiveWalletSheet.DESTINATION)
    }
    var showAddCategoryPage by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showVoiceDialog by remember { mutableStateOf(false) }
    var isScanningReceipt by remember { mutableStateOf(false) }
    var voiceOrReceiptNotice by remember(txToEdit) {
        mutableStateOf(activeDraft?.voiceOrReceiptNotice)
    }
    var receiptScanError by remember { mutableStateOf<String?>(null) }
    var isReceiptNotice by remember(txToEdit) {
        mutableStateOf(activeDraft?.isReceiptNotice ?: false)
    }
    var isCameraPermissionDenied by remember { mutableStateOf(false) }

    fun currentDraftSnapshot(
        overrideWalletSheet: ActiveWalletSheet? = null,
        overrideCalculatorVisible: Boolean? = null
    ): TransactionDraftState {
        val currentSheet = overrideWalletSheet ?: when {
            showSourceWalletSheet -> ActiveWalletSheet.SOURCE
            showDestinationWalletSheet -> ActiveWalletSheet.DESTINATION
            else -> ActiveWalletSheet.NONE
        }
        return TransactionDraftState(
            editingTransactionId = txToEdit?.id,
            selectedType = selectedType,
            expression = expression,
            isCalculatorVisible = overrideCalculatorVisible ?: isCalculatorVisible,
            title = title,
            selectedCategoryName = selectedCategoryName,
            selectedSubcategoryName = selectedSubcategoryName,
            selectedAccountId = selectedAccountId,
            selectedToAccountId = selectedToAccountId,
            userManuallyChangedWallet = userManuallyChangedWallet,
            selectedDateTimestamp = selectedDateTimestamp,
            notes = notes,
            activeWalletSheet = currentSheet,
            optimisticCategories = optimisticCategories,
            optimisticSubcategories = optimisticSubcategories,
            newlyCreatedCategoryName = newlyCreatedCategoryName,
            voiceOrReceiptNotice = voiceOrReceiptNotice,
            isReceiptNotice = isReceiptNotice
        )
    }

    LaunchedEffect(
        txToEdit?.id,
        selectedType,
        expression,
        isCalculatorVisible,
        title,
        selectedCategoryName,
        selectedSubcategoryName,
        selectedAccountId,
        selectedToAccountId,
        userManuallyChangedWallet,
        selectedDateTimestamp,
        notes,
        showSourceWalletSheet,
        showDestinationWalletSheet,
        optimisticCategories,
        optimisticSubcategories,
        newlyCreatedCategoryName,
        voiceOrReceiptNotice,
        isReceiptNotice
    ) {
        viewModel.updateTransactionDraft(currentDraftSnapshot())
    }

    val titleInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(titleInteractionSource) {
        titleInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is FocusInteraction.Focus) {
                isCalculatorVisible = false
            }
        }
    }

    val notesInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(notesInteractionSource) {
        notesInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is FocusInteraction.Focus) {
                isCalculatorVisible = false
            }
        }
    }

    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var capturedReceiptBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingReceiptReview by remember { mutableStateOf<ParsedReceiptData?>(null) }

    fun handleOcrResult(result: Result<ParsedReceiptData>) {
        isScanningReceipt = false
        result.onSuccess { parsed ->
            if (parsed.hasText) {
                receiptScanError = null
                pendingReceiptReview = parsed
            } else {
                isReceiptNotice = false
                receiptScanError = if (isBn) {
                    "রসিদে কোনো স্পষ্ট লেখা পাওয়া যায়নি। পর্যাপ্ত আলোতে আবার ছবি তুলুন।"
                } else {
                    "No readable text was detected on the receipt. Please retake the photo in clear lighting."
                }
            }
        }.onFailure {
            isReceiptNotice = false
            receiptScanError = if (isBn) {
                "রশিদ স্ক্যান ত্রুটি। পুনরায় চেষ্টা করতে পারেন।"
            } else {
                "Receipt scan failed. Please try again or enter details manually."
            }
        }
    }

    val previewCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        capturedReceiptBitmap = bitmap
        isScanningReceipt = true
        receiptScanError = null
        voiceOrReceiptNotice = null
        isReceiptNotice = false
        coroutineScope.launch {
            val result = ReceiptOcrManager.processReceiptBitmap(bitmap, accounts, categories)
            handleOcrResult(result)
        }
    }

    val fullResCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingReceiptUri
        if (!success || uri == null) return@rememberLauncherForActivityResult
        isScanningReceipt = true
        receiptScanError = null
        voiceOrReceiptNotice = null
        isReceiptNotice = false
        coroutineScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    capturedReceiptBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
            }
            val uriResult = ReceiptOcrManager.processReceiptUri(context, uri, accounts, categories)
            if (uriResult.isSuccess) {
                handleOcrResult(uriResult)
            } else {
                val bmp = capturedReceiptBitmap
                if (bmp != null) {
                    handleOcrResult(ReceiptOcrManager.processReceiptBitmap(bmp, accounts, categories))
                } else {
                    handleOcrResult(uriResult)
                }
            }
        }
    }

    fun startCameraCapture() {
        try {
            val dir = File(context.cacheDir, "receipt_captures").apply { mkdirs() }
            val tempFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            pendingReceiptUri = uri
            fullResCameraLauncher.launch(uri)
        } catch (_: Exception) {
            previewCameraLauncher.launch(null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCameraPermissionDenied = false
            receiptScanError = null
            startCameraCapture()
        } else {
            isCameraPermissionDenied = true
            receiptScanError = if (isBn) {
                "রশিদ স্ক্যান করতে ক্যামেরা পারমিশন দেওয়া আবশ্যক।"
            } else {
                "Camera permission is required to capture and scan receipts."
            }
        }
    }

    fun launchCameraFlow() {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPerm) {
            isCameraPermissionDenied = false
            receiptScanError = null
            startCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val pendingStartReceiptScan by viewModel.pendingStartReceiptScan.collectAsState()
    LaunchedEffect(pendingStartReceiptScan) {
        if (pendingStartReceiptScan) {
            viewModel.pendingStartReceiptScan.value = false
            launchCameraFlow()
        }
    }

    val isShowingAddAccountPage = currentSubScreen == SubScreen.EDIT_ACCOUNT ||
        currentSubScreen == SubScreen.ADJUST_BALANCE

    // Intercept back button to dismiss Add Account page, Add Category page, or calculator if visible
    BackHandler {
        when {
            isShowingAddAccountPage -> {
                viewModel.closeSubScreen()
            }
            showAddCategoryPage -> {
                showAddCategoryPage = false
                showCategorySheet = true
            }
            isCalculatorVisible -> {
                isCalculatorVisible = false
            }
            else -> {
                viewModel.clearTransactionDraft()
                onBack()
            }
        }
    }

    val selectedAccount = effectiveAccounts.firstOrNull { it.id == selectedAccountId }
    val selectedToAccount = effectiveAccounts.firstOrNull { it.id == selectedToAccountId }

    val liveEvaluatedAmount = remember(expression) {
        CalculatorEvaluator.evaluate(expression)
    }

    val formattedDate = remember(selectedDateTimestamp, language) {
        val raw = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date(selectedDateTimestamp))
        DateFormatter.localizeText(raw, language)
    }

    val scrollState = viewModel.rememberScreenScrollState(
        subKey = txToEdit?.id ?: "new",
        screenOverride = SubScreen.ADD_EDIT_TRANSACTION
    )
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (currentSubScreen == SubScreen.ADJUST_BALANCE) {
        val accountForAdjust by viewModel.accountForAdjustBalance.collectAsState()
        AdjustBalanceScreen(
            viewModel = viewModel,
            account = accountForAdjust,
            onBack = { viewModel.closeSubScreen() }
        )
    } else if (currentSubScreen == SubScreen.EDIT_ACCOUNT) {
        val accountToEdit by viewModel.accountToEdit.collectAsState()
        EditAccountScreen(
            viewModel = viewModel,
            account = accountToEdit,
            onBack = { viewModel.closeSubScreen() },
            onNavigateToAdjustBalance = { acc -> viewModel.openAdjustBalance(acc) }
        )
    } else if (showAddCategoryPage) {
        AddEditCategoryPanel(
            categoryToEdit = null,
            initialTypeIndex = when (selectedType) {
                TransactionType.INCOME -> 1
                TransactionType.TRANSFER -> 2
                else -> 0
            },
            language = language,
            isDark = isDark,
            allSubcategories = effectiveSubcategories,
            onBack = {
                showAddCategoryPage = false
                showCategorySheet = true
            },
            onSave = { name, nameBn, isIncome, isTransfer, isActive, iconName, colorHex, newSubnames ->
                val (createdCat, createdSubs) = viewModel.addCategory(
                    name = name,
                    nameBn = nameBn,
                    isIncome = isIncome,
                    isTransfer = isTransfer,
                    isActive = isActive,
                    iconName = iconName,
                    colorHex = colorHex,
                    initialSubcategories = newSubnames
                )
                optimisticCategories = optimisticCategories + createdCat
                if (createdSubs.isNotEmpty()) {
                    optimisticSubcategories = optimisticSubcategories + createdSubs
                }
                if (isIncome && selectedType != TransactionType.INCOME) {
                    selectedType = TransactionType.INCOME
                } else if (!isIncome && !isTransfer && selectedType != TransactionType.EXPENSE) {
                    selectedType = TransactionType.EXPENSE
                }
                newlyCreatedCategoryName = createdCat.name
                showAddCategoryPage = false
                showCategorySheet = true
            },
            onAddSubcategory = { catId, subName ->
                viewModel.addSubcategory(catId, subName, "")
            },
            onDeleteSubcategory = { subId ->
                viewModel.deleteSubcategory(subId)
            }
        )
    } else {
        // Edge-to-Edge: inner Scaffold has zero insets because outer Scaffold in MainActivity
        // handles statusBars padding. This eliminates excessive blank space while respecting safe insets.
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Pinned Top Header Row: Close ✕, Heading, Camera 📷, Mic 🎙, Delete 🗑 (if editing)
                Surface(
                    color = if (scrollState.value > 0) {
                        MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                    } else {
                        Color.Transparent
                    },
                    shadowElevation = if (scrollState.value > 8) 2.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .zIndex(2f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                        }
                        .testTag("add_transaction_fixed_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Close ✕ button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.5.dp,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_close_transaction"),
                            onClick = {
                                viewModel.clearTransactionDraft()
                                onBack()
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isBn) "বন্ধ করুন" else "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Compact, consistent Heading
                        val headingText = if (txToEdit != null) {
                            when (selectedType) {
                                TransactionType.EXPENSE -> if (isBn) "ব্যয় সম্পাদনা" else "Edit Expense"
                                TransactionType.INCOME -> if (isBn) "আয় সম্পাদনা" else "Edit Income"
                                TransactionType.TRANSFER -> if (isBn) "স্থানান্তর সম্পাদনা" else "Edit Transfer"
                                else -> if (isBn) "লেনদেন সম্পাদনা" else "Edit Transaction"
                            }
                        } else {
                            when (selectedType) {
                                TransactionType.EXPENSE -> if (isBn) "নতুন ব্যয়" else "New Expense"
                                TransactionType.INCOME -> if (isBn) "নতুন আয়" else "New Income"
                                TransactionType.TRANSFER -> if (isBn) "নতুন স্থানান্তর" else "New Transfer"
                                else -> if (isBn) "নতুন লেনদেন" else "New Transaction"
                            }
                        }
                        Text(
                            text = headingText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        // Right action icons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Camera for receipt OCR
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .testTag("receipt_camera_button"),
                                onClick = {
                                    isCalculatorVisible = false
                                    focusManager.clearFocus()
                                    launchCameraFlow()
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = if (isBn) "রশিদ স্ক্যান করুন" else "Scan Receipt",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Mic for voice input
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .testTag("voice_input_button"),
                                onClick = {
                                    isCalculatorVisible = false
                                    focusManager.clearFocus()
                                    showVoiceDialog = true
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = if (isBn) "ভয়েস ইনপুট" else "Voice Input",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Delete button if editing
                            if (txToEdit != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = ExpenseRed.copy(alpha = 0.12f),
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        isCalculatorVisible = false
                                        focusManager.clearFocus()
                                        showDeleteConfirm = true
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Transaction",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Scrollable Content Below Pinned Header
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                        }
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 24.dp)
                        .imePadding()
                        .testTag("add_transaction_scroll_content")
                ) {
                    // Voice or Receipt Feedback Banner
                AnimatedVisibility(
                    visible = voiceOrReceiptNotice != null || isScanningReceipt || receiptScanError != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    when {
                        isScanningReceipt -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("receipt_scanning_progress")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isBn) "রশিদ বিশ্লেষণ করা হচ্ছে (অন-ডিভাইস OCR)..." else "Analyzing receipt (on-device OCR)...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        voiceOrReceiptNotice != null -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = IncomeGreen.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("populated_feedback_banner")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = voiceOrReceiptNotice!!,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isReceiptNotice) {
                                        TextButton(
                                            onClick = { launchCameraFlow() },
                                            modifier = Modifier.testTag("retake_receipt_button")
                                        ) {
                                            Text(
                                                text = if (isBn) "পুনরায় স্ক্যান" else "Retake",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { voiceOrReceiptNotice = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        receiptScanError != null -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("receipt_scan_error_banner")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = receiptScanError!!,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isCameraPermissionDenied) {
                                        TextButton(
                                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                            modifier = Modifier.testTag("grant_camera_permission_button")
                                        ) {
                                            Text(
                                                text = if (isBn) "অনুমতি দিন" else "Grant",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        TextButton(
                                            onClick = { launchCameraFlow() },
                                            modifier = Modifier.testTag("retake_receipt_button")
                                        ) {
                                            Text(
                                                text = if (isBn) "পুনরায় চেষ্টা" else "Retake",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { receiptScanError = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Segmented Tabs: Income, Expense, Transfer
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Expense Tab
                        val isExpense = selectedType == TransactionType.EXPENSE
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isExpense) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isExpense) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxHeight(),
                            onClick = {
                                selectedType = TransactionType.EXPENSE
                                val defaultCat = categories.firstOrNull { !it.isIncome }
                                if (defaultCat != null) {
                                    selectedCategoryName = defaultCat.name
                                    selectedSubcategoryName = ""
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isExpense) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "ব্যয়" else "Expense",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isExpense) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Income Tab
                        val isIncome = selectedType == TransactionType.INCOME
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isIncome) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isIncome) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxHeight(),
                            onClick = {
                                selectedType = TransactionType.INCOME
                                val defaultCat = categories.firstOrNull { it.isIncome }
                                if (defaultCat != null) {
                                    selectedCategoryName = defaultCat.name
                                    selectedSubcategoryName = ""
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "আয়" else "Income",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isIncome) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Transfer Tab
                        val isTransfer = selectedType == TransactionType.TRANSFER
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isTransfer) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (isTransfer) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(3.dp)
                                .fillMaxHeight(),
                            onClick = { selectedType = TransactionType.TRANSFER }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = null,
                                    tint = if (isTransfer) TransferBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "স্থানান্তর" else "Transfer",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isTransfer) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isTransfer) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Simplified, Compact Amount Section (with Prominent Calculator Button)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.2.dp,
                        if (isCalculatorVisible) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    shadowElevation = if (isCalculatorVisible) 3.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            isCalculatorVisible = true
                        }
                        .testTag("amount_card_container")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Amount Area: Currency Symbol + Amount Numerals
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    isCalculatorVisible = true
                                }
                                .testTag("amount_display_field"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 24.sp
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val rawDisplayStr = if (expression.isBlank()) "0" else expression
                            val displayStr = if (isBn) CurrencyFormatter.convertToBanglaDigits(rawDisplayStr) else rawDisplayStr
                            Text(
                                text = displayStr,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (displayStr.length > 12) 20.sp else 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1
                            )

                            // Show live evaluation preview when doing calculation
                            val hasCalcOp = expression.contains("+") || expression.contains("-") ||
                                    expression.contains("×") || expression.contains("÷") || expression.contains("%")
                            if (hasCalcOp && liveEvaluatedAmount > 0) {
                                Text(
                                    text = " = ${CurrencyFormatter.format(liveEvaluatedAmount, "", language = language)}",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 16.sp
                                    ),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Prominent Calculator Keypad Toggle Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCalculatorVisible) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val next = !isCalculatorVisible
                                    if (next) focusManager.clearFocus()
                                    isCalculatorVisible = next
                                }
                                .testTag("calculator_toggle_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = if (isBn) "ক্যালকুলেটর খুলুন" else "Toggle Calculator",
                                    tint = if (isCalculatorVisible) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Horizontally Scrollable Wallet / Account Selector
                if (selectedType != TransactionType.TRANSFER) {
                    WalletRowSelector(
                        label = if (isBn) "ওয়ালেট / অ্যাকাউন্ট" else "Wallet / Account",
                        selectedAccountId = selectedAccountId,
                        accounts = effectiveAccounts,
                        currencySymbol = currencySymbol,
                        onSelectAccount = {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                            userManuallyChangedWallet = true
                            selectedAccountId = it.id
                        },
                        onViewAllWallets = {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                            showSourceWalletSheet = true
                        },
                        testTagPrefix = "tx_wallet"
                    )
                } else {
                    // Transfer: Source Wallet
                    WalletRowSelector(
                        label = if (isBn) "উৎস ওয়ালেট" else "From Wallet",
                        selectedAccountId = selectedAccountId,
                        accounts = effectiveAccounts,
                        currencySymbol = currencySymbol,
                        onSelectAccount = {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                            userManuallyChangedWallet = true
                            selectedAccountId = it.id
                            if (selectedToAccountId == it.id) {
                                // Clear or pick next account for destination
                                val other = effectiveAccounts.firstOrNull { acc -> !acc.isHidden && acc.id != it.id }
                                if (other != null) selectedToAccountId = other.id
                            }
                        },
                        onViewAllWallets = {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                            showSourceWalletSheet = true
                        },
                        testTagPrefix = "transfer_from_wallet"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Transfer: Destination Wallet (source wallet is excluded)
                    WalletRowSelector(
                        label = if (isBn) "গন্তব্য ওয়ালেট" else "To Wallet",
                        selectedAccountId = selectedToAccountId,
                        accounts = effectiveAccounts,
                        currencySymbol = currencySymbol,
                        excludedAccountId = selectedAccountId,
                        onSelectAccount = {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                            userManuallyChangedWallet = true
                            selectedToAccountId = it.id
                        },
                        onViewAllWallets = {
                            isCalculatorVisible = false
                            focusManager.clearFocus()
                            showDestinationWalletSheet = true
                        },
                        testTagPrefix = "transfer_to_wallet"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Category Selection Field (for Expense & Income)
                if (selectedType != TransactionType.TRANSFER) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isBn) "ক্যাটাগরি" else "Category",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val currentCategory = effectiveCategories.firstOrNull { it.name == selectedCategoryName }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isCalculatorVisible = false
                                    focusManager.clearFocus()
                                    showCategorySheet = true
                                }
                                .testTag("category_field_selector")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIcon(
                                    iconName = currentCategory?.iconName ?: "category",
                                    colorHex = currentCategory?.colorHex ?: "#6366F1",
                                    size = 40.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentCategory?.getDisplayName(language) ?: selectedCategoryName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    val subText = if (selectedSubcategoryName.isNotBlank()) {
                                        if (isBn) "সাবক্যাটাগরি: $selectedSubcategoryName" else "Subcategory: $selectedSubcategoryName"
                                    } else {
                                        if (isBn) "সাবক্যাটাগরি নির্বাচন করুন" else "Tap to choose subcategory"
                                    }

                                    Text(
                                        text = subText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = if (selectedSubcategoryName.isNotBlank()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = if (isBn) "ক্যাটাগরি নির্বাচন করুন" else "Select Category",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 6. Transaction Name (Renamed from Merchant/Payee with editable text input)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isBn) "লেনদেনের শিরোনাম / বিবরণ" else "Transaction Name",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            isCalculatorVisible = false
                            title = it
                        },
                        interactionSource = titleInteractionSource,
                        placeholder = {
                            Text(
                                text = if (isBn) "লেনদেনের নাম লিখুন" else "Enter transaction name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    isCalculatorVisible = false
                                }
                            }
                            .testTag("merchant_payee_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Date Selector & Note Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Selector Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isCalculatorVisible = false
                                focusManager.clearFocus()
                                showDatePickerDialog = true
                            }
                            .testTag("transaction_date_picker")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = if (isBn) "তারিখ" else "Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBn) "তারিখ" else "Date",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Field (optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        isCalculatorVisible = false
                        notes = it
                    },
                    interactionSource = notesInteractionSource,
                    placeholder = {
                        Text(
                            text = if (isBn) "নোট (ঐচ্ছিক)" else "Note (optional)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                isCalculatorVisible = false
                            }
                        }
                        .testTag("transaction_notes_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 8. Save Button
                val finalAmountToSave = if (liveEvaluatedAmount > 0) liveEvaluatedAmount else CalculatorEvaluator.evaluate(expression)
                val isValidAccountSelection = if (selectedType == TransactionType.TRANSFER) {
                    selectedAccountId.isNotBlank() && selectedToAccountId.isNotBlank() && selectedAccountId != selectedToAccountId
                } else {
                    selectedAccountId.isNotBlank()
                }
                val saveButtonEnabled = finalAmountToSave > 0 && isValidAccountSelection

                Button(
                    onClick = {
                        val cleanTitle = title.ifBlank {
                            if (selectedType == TransactionType.TRANSFER) "Transfer to ${selectedToAccount?.name ?: "Account"}"
                            else "$selectedCategoryName ${if (selectedSubcategoryName.isNotBlank()) "($selectedSubcategoryName)" else ""}"
                        }
                        viewModel.saveTransaction(
                            id = txToEdit?.id,
                            type = selectedType,
                            amount = finalAmountToSave,
                            title = cleanTitle,
                            category = if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategoryName,
                            subcategory = if (selectedType == TransactionType.TRANSFER) "" else selectedSubcategoryName,
                            accountId = selectedAccountId,
                            toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                            timestamp = selectedDateTimestamp,
                            notes = notes
                        )
                    },
                    enabled = saveButtonEnabled,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (selectedType) {
                            TransactionType.EXPENSE -> Color(0xFF6366F1)
                            TransactionType.INCOME -> Color(0xFF10B981)
                            TransactionType.TRANSFER -> Color(0xFF3B82F6)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_transaction_button")
                ) {
                    val saveButtonText = if (txToEdit != null) {
                        if (isBn) "লেনদেন আপডেট করুন" else "Update transaction"
                    } else {
                        when (selectedType) {
                            TransactionType.EXPENSE -> if (isBn) "ব্যয় সংরক্ষণ করুন" else "Save expense"
                            TransactionType.INCOME -> if (isBn) "আয় সংরক্ষণ করুন" else "Save income"
                            TransactionType.TRANSFER -> if (isBn) "স্থানান্তর সম্পন্ন করুন" else "Save transfer"
                            else -> if (isBn) "সংরক্ষণ করুন" else "Save transaction"
                        }
                    }
                    Text(
                        text = saveButtonText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                }

                // 9. Docked Calculator Keypad with % Percentage Key (never overlaps scrollable fields)
                AnimatedVisibility(
                    visible = isCalculatorVisible,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CalculatorKeypad(
                        expression = expression,
                        onExpressionChange = { expression = it },
                        onDone = { evaluated ->
                            expression = if (evaluated % 1.0 == 0.0) evaluated.toLong().toString()
                            else String.format(Locale.US, "%.2f", evaluated)
                            isCalculatorVisible = false
                        },
                        onDismiss = { isCalculatorVisible = false },
                        language = language
                    )
                }
            }
        }
        }
    }

    // Category Selection Modal Bottom Sheet
    if (showCategorySheet && !showAddCategoryPage && !isShowingAddAccountPage) {
        CategorySelectionBottomSheet(
            selectedCategoryName = selectedCategoryName,
            selectedSubcategoryName = selectedSubcategoryName,
            selectedType = selectedType,
            categories = effectiveCategories,
            subcategories = effectiveSubcategories,
            language = language,
            onTypeChanged = { selectedType = it },
            onCategorySelected = { catName, subName ->
                selectedCategoryName = catName
                selectedSubcategoryName = subName
            },
            onAddNewCategory = {
                showCategorySheet = false
                showAddCategoryPage = true
            },
            onDismiss = { showCategorySheet = false },
            newlyAddedCategoryName = newlyCreatedCategoryName
        )
    }

    // View Wallet Bottom Sheet (Source Wallet)
    if (showSourceWalletSheet && !isShowingAddAccountPage) {
        WalletSelectionBottomSheet(
            title = if (selectedType == TransactionType.TRANSFER) (if (isBn) "উৎস ওয়ালেট নির্বাচন করুন" else "Select Source Wallet")
                    else (if (isBn) "ওয়ালেট নির্বাচন করুন" else "Select Wallet"),
            selectedAccountId = selectedAccountId,
            accounts = effectiveAccounts,
            currencySymbol = currencySymbol,
            onSelectAccount = {
                userManuallyChangedWallet = true
                selectedAccountId = it.id
                if (selectedType == TransactionType.TRANSFER && selectedToAccountId == it.id) {
                    val other = effectiveAccounts.firstOrNull { acc -> !acc.isHidden && acc.id != it.id }
                    if (other != null) selectedToAccountId = other.id
                }
            },
            onDismiss = { showSourceWalletSheet = false },
            onAddAccount = {
                isCalculatorVisible = false
                viewModel.openAddAccountFromTransaction(
                    currentDraftSnapshot(
                        overrideWalletSheet = ActiveWalletSheet.SOURCE,
                        overrideCalculatorVisible = false
                    )
                )
            }
        )
    }

    // View Wallet Bottom Sheet (Destination Wallet for Transfer)
    if (showDestinationWalletSheet && !isShowingAddAccountPage) {
        WalletSelectionBottomSheet(
            title = if (isBn) "গন্তব্য ওয়ালেট নির্বাচন করুন" else "Select Destination Wallet",
            selectedAccountId = selectedToAccountId,
            accounts = effectiveAccounts,
            currencySymbol = currencySymbol,
            excludedAccountId = selectedAccountId,
            onSelectAccount = {
                userManuallyChangedWallet = true
                selectedToAccountId = it.id
            },
            onDismiss = { showDestinationWalletSheet = false },
            onAddAccount = {
                isCalculatorVisible = false
                viewModel.openAddAccountFromTransaction(
                    currentDraftSnapshot(
                        overrideWalletSheet = ActiveWalletSheet.DESTINATION,
                        overrideCalculatorVisible = false
                    )
                )
            }
        )
    }

    // Date Picker Dialog
    if (showDatePickerDialog) {
        HishabPatiDatePickerDialog(
            initialSelectedDateMillis = selectedDateTimestamp,
            language = language,
            firstDayOfWeek = extendedSettings.startOfWeek,
            onDateSelected = { pickedMillis ->
                selectedDateTimestamp = pickedMillis
                showDatePickerDialog = false
            },
            onDismissRequest = { showDatePickerDialog = false }
        )
    }

    // Delete Confirmation Dialog
    val currentTx = txToEdit
    if (showDeleteConfirm && currentTx != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(if (isBn) "লেনদেন মুছবেন?" else "Delete Transaction?") },
            text = {
                Text(
                    if (isBn) "আপনি কি নিশ্চিতভাবে \"${currentTx.title}\" লেনদেনটি মুছে ফেলতে চান? অ্যাকাউন্টের ব্যালেন্স সেই অনুযায়ী সমন্বয় করা হবে।"
                    else "Are you sure you want to permanently delete \"${currentTx.title}\"? Account balances will be updated accordingly."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(currentTx.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(if (isBn) "মুছুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Receipt OCR Review Dialog (never saves automatically)
    val activeReceiptReview = pendingReceiptReview
    if (activeReceiptReview != null) {
        ReceiptReviewDialog(
            parsedReceipt = activeReceiptReview,
            receiptBitmap = capturedReceiptBitmap,
            isProcessingOcr = isScanningReceipt,
            isBangla = isBn,
            currencySymbol = currencySymbol,
            accounts = accounts,
            categories = categories,
            defaultAccountId = selectedAccountId,
            onRotateReceipt = {
                val bmp = capturedReceiptBitmap ?: return@ReceiptReviewDialog
                val rotated = ReceiptOcrManager.rotateBitmap(bmp, 90f)
                capturedReceiptBitmap = rotated
                isScanningReceipt = true
                coroutineScope.launch {
                    val res = ReceiptOcrManager.processReceiptBitmap(rotated, accounts, categories)
                    isScanningReceipt = false
                    res.onSuccess { reparsed ->
                        pendingReceiptReview = reparsed
                    }
                }
            },
            onRescanReceipt = {
                pendingReceiptReview = null
                launchCameraFlow()
            },
            onApplyToForm = { confirmedDraft ->
                pendingReceiptReview = null
                val filledFields = mutableListOf<String>()
                if (confirmedDraft.amount > 0.0) {
                    expression = if (confirmedDraft.amount % 1.0 == 0.0) {
                        confirmedDraft.amount.toLong().toString()
                    } else {
                        String.format(Locale.US, "%.2f", confirmedDraft.amount)
                    }
                    filledFields.add(if (isBn) "পরিমাণ: $currencySymbol$expression" else "Amount: $currencySymbol$expression")
                }
                if (confirmedDraft.title.isNotBlank()) {
                    title = confirmedDraft.title
                    filledFields.add(if (isBn) "বিবরণ: $title" else "Title: $title")
                }
                if (confirmedDraft.dateMillis != null) {
                    selectedDateTimestamp = confirmedDraft.dateMillis
                    filledFields.add(if (isBn) "তারিখ" else "Date")
                }
                if (confirmedDraft.categoryName.isNotBlank()) {
                    selectedCategoryName = confirmedDraft.categoryName
                    if (confirmedDraft.subcategoryName.isNotBlank()) {
                        selectedSubcategoryName = confirmedDraft.subcategoryName
                    }
                    filledFields.add(if (isBn) "ক্যাটাগরি: $selectedCategoryName" else "Category: $selectedCategoryName")
                }
                if (confirmedDraft.accountId.isNotBlank()) {
                    selectedAccountId = confirmedDraft.accountId
                }
                if (notes.isBlank() && confirmedDraft.notes.isNotBlank()) {
                    notes = confirmedDraft.notes
                }
                selectedType = TransactionType.EXPENSE
                isReceiptNotice = true
                receiptScanError = null
                voiceOrReceiptNotice = if (isBn) {
                    "✓ রসিদ থেকে তথ্য পূরণ হয়েছে (${filledFields.joinToString(", ")})। সংরক্ষণের আগে যাচাই করুন।"
                } else {
                    "✓ Populated from reviewed receipt (${filledFields.joinToString(", ")}). Tap Save when ready."
                }
            },
            onConfirmAndSave = { confirmedDraft ->
                pendingReceiptReview = null
                viewModel.saveTransaction(
                    id = txToEdit?.id,
                    type = TransactionType.EXPENSE,
                    amount = confirmedDraft.amount,
                    title = confirmedDraft.title,
                    category = confirmedDraft.categoryName,
                    subcategory = confirmedDraft.subcategoryName,
                    accountId = confirmedDraft.accountId.ifBlank { selectedAccountId },
                    toAccountId = null,
                    timestamp = confirmedDraft.dateMillis ?: selectedDateTimestamp,
                    notes = confirmedDraft.notes
                )
                onBack()
            },
            onDismiss = { pendingReceiptReview = null }
        )
    }

    // Voice Input Dialog with Review Transaction Step
    if (showVoiceDialog) {
        VoiceTransactionDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceDialog = false },
            onApplyDraftToForm = { parsed ->
                showVoiceDialog = false
                val populated = mutableListOf<String>()

                if (parsed.hasConfidentAmount && parsed.amount > 0.0) {
                    expression = if (parsed.amount % 1.0 == 0.0) parsed.amount.toLong().toString()
                    else String.format(Locale.US, "%.2f", parsed.amount)
                    populated.add(if (isBn) "পরিমাণ: $currencySymbol$expression" else "Amount: $currencySymbol$expression")
                }
                if (parsed.hasConfidentDescription && parsed.description.isNotBlank()) {
                    title = parsed.description
                    populated.add(if (isBn) "বিবরণ: $title" else "Title: $title")
                }
                if (parsed.hasConfidentCategory && parsed.categoryName.isNotBlank()) {
                    selectedCategoryName = parsed.categoryName
                    if (parsed.subcategoryName.isNotBlank()) {
                        selectedSubcategoryName = parsed.subcategoryName
                    }
                    populated.add(if (isBn) "ক্যাটাগরি: $selectedCategoryName" else "Category: $selectedCategoryName")
                }
                if (parsed.hasConfidentAccount && parsed.matchedAccountId != null) {
                    selectedAccountId = parsed.matchedAccountId
                    val accName = accounts.firstOrNull { it.id == parsed.matchedAccountId }?.name
                    if (accName != null) {
                        populated.add(if (isBn) "ওয়ালেট: $accName" else "Wallet: $accName")
                    }
                }
                if (parsed.type != selectedType) {
                    selectedType = parsed.type
                }
                if (notes.isBlank() && parsed.rawText.isNotBlank()) {
                    notes = "Voice: \"${parsed.rawText}\""
                }

                isReceiptNotice = false
                if (populated.isNotEmpty()) {
                    receiptScanError = null
                    voiceOrReceiptNotice = if (isBn) {
                        "✓ ভয়েস ইনপুট থেকে তথ্য পূরণ হয়েছে (${populated.joinToString(", ")})। অনুগ্রহ করে তথ্য যাচাই করুন।"
                    } else {
                        "✓ Populated from speech (${populated.joinToString(", ")}). Review details before saving."
                    }
                } else {
                    voiceOrReceiptNotice = null
                    receiptScanError = if (isBn) {
                        "বক্তব্য থেকে কোনো নির্দিষ্ট তথ্য শনাক্ত করা যায়নি। অনুগ্রহ করে নিজে তথ্য প্রদান করুন বা পুনরায় বলুন।"
                    } else {
                        "Could not detect transaction details from speech. Please enter manually or speak again."
                    }
                }
            },
            onSavedTransaction = {
                showVoiceDialog = false
                onBack()
            }
        )
    }
}
