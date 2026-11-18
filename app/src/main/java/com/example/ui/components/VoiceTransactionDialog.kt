package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.voice.ParsedVoiceTransaction
import com.example.voice.VoiceInputController
import com.example.voice.VoiceLocaleOption
import com.example.voice.VoiceTransactionParser
import java.util.Locale

@Composable
fun VoiceTransactionDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSpeechRecognized: ((String) -> Unit)? = null,
    onApplyDraftToForm: ((ParsedVoiceTransaction) -> Unit)? = null,
    onSavedTransaction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val vmParsedResult by viewModel.voiceParsedResult.collectAsState()

    val isBangla = settings?.language == AppLanguage.BANGLA
    val currencySymbol = settings?.currencySymbol ?: "৳"

    val controller = remember {
        VoiceInputController(
            initialBanglaUi = isBangla,
            initialParsedResult = vmParsedResult
        )
    }
    LaunchedEffect(isBangla) {
        controller.setBanglaUi(isBangla)
    }

    val uiState by controller.state.collectAsState()

    // Review Transaction state (never saves automatically; only shown after "Complete Voice Input")
    var reviewParsed by remember { mutableStateOf<ParsedVoiceTransaction?>(vmParsedResult) }
    var editableTranscript by remember { mutableStateOf(vmParsedResult?.rawText ?: "") }
    var draftType by remember { mutableStateOf(vmParsedResult?.type ?: TransactionType.EXPENSE) }
    var draftAmountText by remember {
        mutableStateOf(
            vmParsedResult?.let {
                if (it.hasConfidentAmount && it.amount > 0.0) {
                    if (it.amount % 1.0 == 0.0) it.amount.toLong().toString()
                    else String.format(Locale.US, "%.2f", it.amount)
                } else ""
            } ?: ""
        )
    }
    var draftTitle by remember {
        mutableStateOf(
            vmParsedResult?.let { if (it.hasConfidentDescription) it.description else "" } ?: ""
        )
    }
    var draftCategory by remember {
        mutableStateOf(
            vmParsedResult?.let { if (it.hasConfidentCategory) it.categoryName else "" } ?: ""
        )
    }
    var draftSubcategory by remember {
        mutableStateOf(
            vmParsedResult?.let { if (it.hasConfidentCategory) it.subcategoryName else "" } ?: ""
        )
    }
    var draftAccountId by remember {
        mutableStateOf(
            vmParsedResult?.matchedAccountId ?: accounts.firstOrNull()?.id ?: ""
        )
    }
    var reviewValidationError by remember { mutableStateOf<String?>(null) }

    fun populateReviewFromParsed(parsed: ParsedVoiceTransaction) {
        reviewParsed = parsed
        editableTranscript = parsed.rawText
        draftType = parsed.type
        draftAmountText = if (parsed.hasConfidentAmount && parsed.amount > 0.0) {
            if (parsed.amount % 1.0 == 0.0) parsed.amount.toLong().toString()
            else String.format(Locale.US, "%.2f", parsed.amount)
        } else {
            ""
        }
        draftTitle = if (parsed.hasConfidentDescription) parsed.description else ""
        draftCategory = if (parsed.hasConfidentCategory) parsed.categoryName else ""
        draftSubcategory = if (parsed.hasConfidentCategory) parsed.subcategoryName else ""
        if (parsed.hasConfidentAccount && parsed.matchedAccountId != null) {
            draftAccountId = parsed.matchedAccountId
        } else if (draftAccountId.isBlank()) {
            draftAccountId = accounts.firstOrNull()?.id ?: ""
        }
        reviewValidationError = null
    }

    val speechRecognizerRef = remember { mutableStateOf<SpeechRecognizer?>(null) }
    var recognizerNeedsRecreate by remember { mutableStateOf(false) }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(VoiceInputController.TAG, "Listener.onReadyForSpeech")
                controller.onReadyForSpeech()
            }

            override fun onBeginningOfSpeech() {
                Log.d(VoiceInputController.TAG, "Listener.onBeginningOfSpeech")
                controller.onBeginningOfSpeech()
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(VoiceInputController.TAG, "Listener.onEndOfSpeech")
                controller.onEndOfSpeech()
            }

            override fun onError(error: Int) {
                Log.w(VoiceInputController.TAG, "Listener.onError: $error (${VoiceInputController.getErrorCodeName(error)})")
                if (
                    error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    error == SpeechRecognizer.ERROR_SERVER ||
                    error == VoiceInputController.ERROR_SERVER_DISCONNECTED ||
                    error == SpeechRecognizer.ERROR_AUDIO ||
                    error == VoiceInputController.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == VoiceInputController.ERROR_LANGUAGE_UNAVAILABLE
                ) {
                    recognizerNeedsRecreate = true
                }
                controller.onError(error)
            }

            override fun onResults(results: Bundle?) {
                Log.d(VoiceInputController.TAG, "Listener.onResults received")
                controller.onResults(results)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(VoiceInputController.TAG, "Listener.onPartialResults received")
                controller.onPartialResults(partialResults)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun getOrCreateSpeechRecognizer(): SpeechRecognizer? {
        val resolvedComponent = VoiceInputController.resolveRecognitionServiceComponent(context)
        val directAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(
            VoiceInputController.TAG,
            "getOrCreateSpeechRecognizer: resolvedComponent=$resolvedComponent, directAvailable=$directAvailable"
        )
        if (!directAvailable && resolvedComponent == null) {
            return null
        }
        val existing = speechRecognizerRef.value
        if (existing != null && !recognizerNeedsRecreate) {
            return existing
        }
        try {
            existing?.cancel()
            existing?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizerRef.value = null
        return try {
            val recognizer = when {
                resolvedComponent != null -> {
                    try {
                        SpeechRecognizer.createSpeechRecognizer(context, resolvedComponent)
                    } catch (e: Exception) {
                        Log.w(VoiceInputController.TAG, "Resolved component failed, falling back to default", e)
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                }
                directAvailable -> SpeechRecognizer.createSpeechRecognizer(context)
                else -> null
            }
            recognizer?.also { created ->
                created.setRecognitionListener(recognitionListener)
                speechRecognizerRef.value = created
                recognizerNeedsRecreate = false
            }
        } catch (e: Exception) {
            Log.e(VoiceInputController.TAG, "Failed to create SpeechRecognizer", e)
            speechRecognizerRef.value = null
            null
        }
    }

    val googleSpeechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bestMatch = VoiceInputController.extractBestCandidateFromIntent(result.data)
            Log.d(VoiceInputController.TAG, "googleSpeechIntentLauncher RESULT_OK: hasMatch=${bestMatch.isNotBlank()}, length=${bestMatch.length}")
            controller.onFinalTextCaptured(bestMatch)
        } else {
            Log.d(VoiceInputController.TAG, "googleSpeechIntentLauncher cancelled or failed: resultCode=${result.resultCode}")
            controller.onEndOfSpeech()
        }
    }

    fun launchGoogleSpeechDialog(languageTag: String = uiState.selectedLocale.languageTag) {
        if (!VoiceInputController.isSpeechServiceAvailable(context)) {
            controller.checkServiceAvailability(false)
            return
        }
        try {
            try {
                speechRecognizerRef.value?.stopListening()
            } catch (_: Exception) {
            }
            val intent = VoiceInputController.buildRecognizerIntent(
                languageTag = languageTag,
                callingPackage = context.packageName
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    if (isBangla) "আপনার খরচের কথা বলুন (যেমন: বাজারে ৫০০ টাকা খরচ)"
                    else "Speak your expense (e.g., spent 500 on groceries with Cash)"
                )
            }
            googleSpeechIntentLauncher.launch(intent)
        } catch (_: Exception) {
            controller.checkServiceAvailability(false)
        }
    }

    fun stopSpeechListening() {
        try {
            speechRecognizerRef.value?.stopListening()
        } catch (_: Exception) {
        }
        controller.onEndOfSpeech()
    }

    fun startSpeechListening(
        languageTag: String = uiState.selectedLocale.languageTag,
        isRetry: Boolean = false
    ) {
        val hasPerm = VoiceInputController.hasRecordAudioPermission(context)
        val serviceAvailable = VoiceInputController.isSpeechServiceAvailable(context)

        val canProceed = if (isRetry) {
            controller.retry(hasPerm, serviceAvailable)
        } else {
            controller.onStartListeningRequested(hasPerm, serviceAvailable)
        }
        if (!canProceed) {
            return
        }

        val recognizer = getOrCreateSpeechRecognizer()
        if (recognizer != null) {
            try {
                val intent = VoiceInputController.buildRecognizerIntent(
                    languageTag = languageTag,
                    callingPackage = context.packageName
                )
                Log.d(VoiceInputController.TAG, "SpeechRecognizer.startListening called: locale=$languageTag, pkg=${context.packageName}")
                recognizer.startListening(intent)
            } catch (e: Exception) {
                Log.w(VoiceInputController.TAG, "startListening failed, falling back to Google Speech Dialog", e)
                recognizerNeedsRecreate = true
                launchGoogleSpeechDialog(languageTag)
            }
        } else {
            Log.d(VoiceInputController.TAG, "No background SpeechRecognizer available, using Google Speech Dialog")
            launchGoogleSpeechDialog(languageTag)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        controller.onPermissionResult(isGranted)
        if (isGranted) {
            startSpeechListening(uiState.selectedLocale.languageTag)
        }
    }

    fun onMicrophoneButtonClicked() {
        if (uiState.isListening) {
            stopSpeechListening()
            return
        }
        val hasPerm = VoiceInputController.hasRecordAudioPermission(context)
        if (!hasPerm) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            val isRetryingFromError = uiState.errorMessage != null || uiState.lastErrorCode != null
            if (isRetryingFromError) {
                recognizerNeedsRecreate = true
            }
            startSpeechListening(uiState.selectedLocale.languageTag, isRetry = isRetryingFromError)
        }
    }

    fun onCompleteVoiceInputClicked() {
        try {
            speechRecognizerRef.value?.stopListening()
        } catch (_: Exception) {
        }
        val parsed = controller.completeVoiceInput(accounts, categories) ?: return
        viewModel.processVoiceInput(parsed.rawText)
        populateReviewFromParsed(parsed)
    }

    DisposableEffect(context) {
        onDispose {
            try {
                speechRecognizerRef.value?.stopListening()
                speechRecognizerRef.value?.cancel()
                speechRecognizerRef.value?.destroy()
            } catch (_: Exception) {
            }
            speechRecognizerRef.value = null
        }
    }

    // Check initial permission & service state when dialog opens
    LaunchedEffect(Unit) {
        val serviceAvailable = VoiceInputController.isSpeechServiceAvailable(context)
        controller.checkServiceAvailability(serviceAvailable)
        if (!uiState.isReviewMode && serviceAvailable) {
            val hasPerm = VoiceInputController.hasRecordAudioPermission(context)
            if (hasPerm) {
                controller.onPermissionResult(true)
            }
        }
    }

    Dialog(
        onDismissRequest = {
            try {
                speechRecognizerRef.value?.stopListening()
                speechRecognizerRef.value?.cancel()
            } catch (_: Exception) {
            }
            viewModel.voiceParsedResult.value = null
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("voice_transaction_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row matching Hishab Pati Voice Expense Entry design
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isReviewMode) {
                            if (isBangla) "লেনদেন যাচাই করুন" else "Review Transaction"
                        } else {
                            if (isBangla) "ভয়েস দিয়ে খরচ বলুন" else "Voice Expense Entry"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("voice_dialog_title")
                    )
                    IconButton(
                        onClick = {
                            try {
                                speechRecognizerRef.value?.stopListening()
                                speechRecognizerRef.value?.cancel()
                            } catch (_: Exception) {
                            }
                            viewModel.voiceParsedResult.value = null
                            onDismiss()
                        },
                        modifier = Modifier.testTag("voice_dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isBangla) "বন্ধ করুন" else "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!uiState.isReviewMode) {
                    // =========================================================================
                    // STEPS 1–3: VOICE INPUT CAPTURE, RECOGNIZED TEXT PREVIEW, & COMPLETE BUTTON
                    // =========================================================================

                    // Language Toggle Pill: বাংলা (BD) | English (matches reference screen recording)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.testTag("voice_language_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VoiceLocaleOption.entries.forEach { localeOption ->
                                val isSelected = uiState.selectedLocale == localeOption
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            val wasListening = uiState.isListening
                                            if (wasListening) {
                                                stopSpeechListening()
                                            }
                                            controller.selectLocale(localeOption)
                                            if (wasListening) {
                                                startSpeechListening(localeOption.languageTag)
                                            }
                                        }
                                        .testTag("voice_lang_${localeOption.name.lowercase()}")
                                ) {
                                    Text(
                                        text = if (isBangla) localeOption.labelBn else localeOption.labelEn,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Step 1: Microphone Button with Pulse Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "voice_mic_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (uiState.isListening) 1.25f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(750, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "voice_pulse_scale"
                    )

                    val showErrorIcon = !uiState.isListening && uiState.errorMessage != null && !uiState.hasUsableText

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.height(104.dp)
                    ) {
                        if (uiState.isListening) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(if (uiState.isListening) 76.dp else 68.dp)
                                .clip(CircleShape)
                                .clickable { onMicrophoneButtonClicked() }
                                .testTag("voice_mic_button"),
                            shape = CircleShape,
                            color = when {
                                uiState.isListening -> MaterialTheme.colorScheme.primary
                                showErrorIcon -> ExpenseRed.copy(alpha = 0.14f)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            },
                            shadowElevation = if (uiState.isListening) 6.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        uiState.isPermissionDenied -> Icons.Default.MicOff
                                        showErrorIcon -> Icons.Default.Refresh
                                        else -> Icons.Default.Mic
                                    },
                                    contentDescription = if (uiState.isListening) {
                                        if (isBangla) "শোনা থামান" else "Stop Listening"
                                    } else {
                                        if (isBangla) "মাইকে ট্যাপ করে কথা বলুন" else "Tap microphone to speak"
                                    },
                                    tint = when {
                                        uiState.isListening -> MaterialTheme.colorScheme.onPrimary
                                        showErrorIcon -> ExpenseRed
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Listening / Error / Captured Status Text (never displays "Listening... speak now" in red error state)
                    val statusText = when {
                        uiState.isListening -> {
                            if (isBangla) "শুনছি... এখন বলুন" else "Listening... speak now"
                        }
                        uiState.errorMessage != null -> {
                            uiState.errorMessage!!
                        }
                        uiState.hasUsableText -> {
                            if (isBangla) "কথা শনাক্ত হয়েছে! নিচে যাচাই করে 'ভয়েস ইনপুট সম্পন্ন করুন' চাপুন।"
                            else "Speech captured! Check below and tap Complete Voice Input."
                        }
                        else -> {
                            if (isBangla) "মাইকে ট্যাপ করে খরচের কথা বলুন..."
                            else "Tap microphone to speak your expense..."
                        }
                    }

                    val statusColor = when {
                        uiState.isListening -> MaterialTheme.colorScheme.onSurface
                        uiState.errorMessage != null -> ExpenseRed
                        uiState.hasUsableText -> IncomeGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (uiState.isListening || uiState.hasUsableText) FontWeight.SemiBold else FontWeight.Normal,
                        color = statusColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("voice_listening_status")
                    )

                    if (uiState.isPermissionDenied) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("voice_retry_permission_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBangla) "পারমিশন দিন ও আবার চেষ্টা করুন" else "Grant Permission & Retry")
                        }
                    } else if (!uiState.isListening && uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val hasPerm = VoiceInputController.hasRecordAudioPermission(context)
                                if (!hasPerm) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    recognizerNeedsRecreate = true
                                    startSpeechListening(uiState.selectedLocale.languageTag, isRetry = true)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("voice_retry_recognition_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isBangla) "আবার চেষ্টা করুন" else "Retry Voice Input")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 2: Recognized Text Display Box so the user can check what was captured
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_captured_text_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.hasUsableText) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                            }
                        ),
                        border = if (uiState.hasUsableText) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        } else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.hasUsableText) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = if (isBangla) "শনাক্তকৃত কথা" else "Recognized Speech",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (uiState.recognizedText.isNotEmpty()) {
                                    Text(
                                        text = if (isBangla) "মুছুন" else "Clear",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { controller.clearRecognizedText() }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .testTag("voice_clear_text_button")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = uiState.recognizedText,
                                onValueChange = { updated ->
                                    controller.updateRecognizedText(updated)
                                },
                                placeholder = {
                                    Text(
                                        text = if (isBangla) {
                                            "আপনার বলা কথা এখানে দেখা যাবে (যেমন: বাজারে ৫০০ টাকা খরচ)..."
                                        } else {
                                            "Captured voice text will appear here (e.g., spent 500 on groceries)..."
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_captured_text_input"),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step 3: "Complete Voice Input" Button (disabled until usable recognized text is present)
                    Button(
                        onClick = { onCompleteVoiceInputClicked() },
                        enabled = uiState.canCompleteVoiceInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("complete_voice_input_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "ভয়েস ইনপুট সম্পন্ন করুন" else "Complete Voice Input",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Use Google Speech Recognition" Outlined Pill Button (matching reference screen recording)
                    OutlinedButton(
                        onClick = { launchGoogleSpeechDialog(uiState.selectedLocale.languageTag) },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_google_speech_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "গুগল স্পিচ সার্ভিস ব্যবহার করুন" else "Use Google Speech Recognition",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Examples matching reference screen recording
                    Text(
                        text = if (isBangla) {
                            "উদাহরণ: \"বাজারে ৫০০ টাকা খরচ\", \"Grocery 500 taka bKash\""
                        } else {
                            "Example: \"spent 500 on groceries with Cash\", \"medicine 350 taka\""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Cancel Button matching reference screen recording
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                try {
                                    speechRecognizerRef.value?.stopListening()
                                    speechRecognizerRef.value?.cancel()
                                } catch (_: Exception) {
                                }
                                viewModel.voiceParsedResult.value = null
                                onDismiss()
                            },
                            modifier = Modifier.testTag("voice_cancel_button")
                        ) {
                            Text(
                                text = if (isBangla) "বাতিল" else "Cancel",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    // =========================================================================
                    // STEP 4 & 5: TRANSACTION REVIEW SCREEN (NEVER SAVES AUTOMATICALLY)
                    // =========================================================================
                    val currentParsed = reviewParsed ?: uiState.parsedTransaction
                    val amountDouble = draftAmountText.trim().replace(",", "").toDoubleOrNull() ?: 0.0
                    val isAmountUnclear = amountDouble <= 0.0

                    // Recognized Speech Transcript Box with "Speak Again" option
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_review_transcript_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBangla) "শনাক্তকৃত কথা (সম্পাদনাযোগ্য)" else "Recognized Speech (Editable)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.voiceParsedResult.value = null
                                        controller.backToVoiceInput(clearText = false)
                                    },
                                    modifier = Modifier.testTag("voice_try_again_button")
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBangla) "আবার বলুন" else "Try Again")
                                }
                            }

                            OutlinedTextField(
                                value = editableTranscript,
                                onValueChange = { newText ->
                                    editableTranscript = newText
                                    controller.updateRecognizedText(newText)
                                    val reparsed = VoiceTransactionParser.parse(newText, accounts, categories)
                                    reviewParsed = reparsed
                                    if (reparsed.hasConfidentAmount && reparsed.amount > 0.0) {
                                        draftAmountText = if (reparsed.amount % 1.0 == 0.0) {
                                            reparsed.amount.toLong().toString()
                                        } else {
                                            String.format(Locale.US, "%.2f", reparsed.amount)
                                        }
                                    }
                                    if (reparsed.hasConfidentDescription) {
                                        draftTitle = reparsed.description
                                    }
                                    if (reparsed.hasConfidentCategory) {
                                        draftCategory = reparsed.categoryName
                                        draftSubcategory = reparsed.subcategoryName
                                    }
                                    if (reparsed.hasConfidentAccount && reparsed.matchedAccountId != null) {
                                        draftAccountId = reparsed.matchedAccountId
                                    }
                                    draftType = reparsed.type
                                },
                                placeholder = {
                                    Text(
                                        if (isBangla) "যেমন: ১০ টাকা লাঞ্চ" else "e.g., 10 taka lunch"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_recognized_text_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Warning banner if speech was unclear or amount could not be identified
                    if (isAmountUnclear || currentParsed?.hasConfidentAmount != true) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_unclear_speech_banner"),
                            shape = RoundedCornerShape(12.dp),
                            color = ExpenseRed.copy(alpha = 0.12f)
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
                                    text = if (isBangla) {
                                        "কথা থেকে টাকার পরিমাণ নিশ্চিতভাবে বোঝা যায়নি। উপরে টেক্সট সম্পাদনা করুন, নিচে পরিমাণ লিখুন অথবা 'আবার বলুন' চাপুন।"
                                    } else {
                                        "Could not confidently identify the amount from speech. Edit the recognized text above, enter the amount below, or tap Try Again."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transaction Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            TransactionType.EXPENSE to (if (isBangla) "খরচ" else "Expense"),
                            TransactionType.INCOME to (if (isBangla) "আয়" else "Income"),
                            TransactionType.TRANSFER to (if (isBangla) "ট্রান্সফার" else "Transfer")
                        ).forEach { (typeOption, label) ->
                            FilterChip(
                                selected = draftType == typeOption,
                                onClick = { draftType = typeOption },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable Amount Field
                    OutlinedTextField(
                        value = draftAmountText,
                        onValueChange = {
                            draftAmountText = it
                            reviewValidationError = null
                        },
                        label = {
                            Text(if (isBangla) "পরিমাণ ($currencySymbol)" else "Amount ($currencySymbol)")
                        },
                        placeholder = {
                            Text(if (isBangla) "পরিমাণ লিখুন" else "Enter amount")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_review_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Editable Transaction Name Field
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = {
                            draftTitle = it
                            reviewValidationError = null
                        },
                        label = {
                            Text(if (isBangla) "লেনদেনের নাম / বিবরণ" else "Transaction Name")
                        },
                        placeholder = {
                            Text(if (isBangla) "যেমন: Lunch" else "e.g., Lunch")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_review_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selection (Only pre-selected if confidently identified)
                    val matchingCategories = remember(categories, draftType) {
                        categories.filter { cat ->
                            when (draftType) {
                                TransactionType.INCOME -> cat.isIncome && !cat.isTransfer
                                TransactionType.TRANSFER -> cat.isTransfer
                                else -> !cat.isIncome && !cat.isTransfer
                            }
                        }
                    }

                    if (draftType != TransactionType.TRANSFER && matchingCategories.isNotEmpty()) {
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
                                text = if (draftCategory.isNotBlank()) draftCategory
                                else (if (isBangla) "নির্বাচন করুন" else "Select category"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (draftCategory.isNotBlank()) MaterialTheme.colorScheme.primary
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
                            matchingCategories.forEach { cat ->
                                val isSelected = draftCategory.equals(cat.name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        draftCategory = cat.name
                                        draftSubcategory = "General"
                                    },
                                    label = {
                                        Text(
                                            if (isBangla && cat.nameBn.isNotBlank()) cat.nameBn else cat.name
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Wallet / Account Selection
                    if (accounts.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "ওয়ালেট / অ্যাকাউন্ট" else "Wallet / Account",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val selectedAccName = accounts.firstOrNull { it.id == draftAccountId }?.name ?: ""
                            Text(
                                text = selectedAccName,
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
                                    selected = draftAccountId == acc.id,
                                    onClick = { draftAccountId = acc.id },
                                    label = { Text(acc.name) }
                                )
                            }
                        }
                    }

                    if (reviewValidationError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = reviewValidationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    val confirmedDraft = ParsedVoiceTransaction(
                        rawText = editableTranscript.ifBlank { uiState.recognizedText },
                        amount = amountDouble,
                        type = draftType,
                        categoryName = draftCategory,
                        subcategoryName = draftSubcategory,
                        matchedAccountId = draftAccountId.ifBlank { accounts.firstOrNull()?.id },
                        matchedToAccountId = reviewParsed?.matchedToAccountId,
                        description = draftTitle.trim(),
                        hasConfidentAmount = amountDouble > 0.0,
                        hasConfidentCategory = draftCategory.isNotBlank(),
                        hasConfidentAccount = draftAccountId.isNotBlank(),
                        hasConfidentDescription = draftTitle.isNotBlank()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onApplyDraftToForm != null || onSpeechRecognized != null) {
                            OutlinedButton(
                                onClick = {
                                    if (onApplyDraftToForm != null) {
                                        onApplyDraftToForm(confirmedDraft)
                                    } else {
                                        onSpeechRecognized?.invoke(confirmedDraft.rawText)
                                    }
                                    viewModel.voiceParsedResult.value = null
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("voice_apply_to_form_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "ফর্মে বসান" else "Edit in Form",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    viewModel.voiceParsedResult.value = null
                                    controller.backToVoiceInput(clearText = false)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("voice_retry_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isBangla) "আবার বলুন" else "Try Again")
                            }
                        }

                        Button(
                            onClick = {
                                if (amountDouble <= 0.0) {
                                    reviewValidationError = if (isBangla) {
                                        "অনুগ্রহ করে সঠিক টাকার পরিমাণ দিন।"
                                    } else {
                                        "Please enter a valid amount greater than 0 before saving."
                                    }
                                    return@Button
                                }
                                val finalCat = draftCategory.ifBlank {
                                    matchingCategories.firstOrNull()?.name ?: "Other"
                                }
                                val finalSub = draftSubcategory.ifBlank { "General" }
                                val finalTitle = draftTitle.trim().ifBlank { finalCat }
                                val toSave = confirmedDraft.copy(
                                    categoryName = finalCat,
                                    subcategoryName = finalSub,
                                    description = finalTitle,
                                    hasConfidentAmount = true,
                                    hasConfidentCategory = true,
                                    hasConfidentDescription = true
                                )
                                viewModel.confirmVoiceTransaction(toSave)
                                onSavedTransaction?.invoke()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("voice_confirm_button"),
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
}
