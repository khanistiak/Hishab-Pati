package com.example.voice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class VoiceLocaleOption(val languageTag: String, val labelBn: String, val labelEn: String) {
    BANGLA("bn-BD", "বাংলা (BD)", "বাংলা (BD)"),
    ENGLISH("en-US", "English", "English")
}

data class VoiceInputUiState(
    val selectedLocale: VoiceLocaleOption = VoiceLocaleOption.ENGLISH,
    val isBanglaUi: Boolean = false,
    val isListening: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isPermissionDenied: Boolean = false,
    val isServiceAvailable: Boolean = true,
    val recognizedText: String = "",
    val errorMessage: String? = null,
    val lastErrorCode: Int? = null,
    val lastCallback: String? = null,
    val hasReceivedTranscript: Boolean = false,
    val retryCount: Int = 0,
    val isReviewMode: Boolean = false,
    val parsedTransaction: ParsedVoiceTransaction? = null
) {
    val hasUsableText: Boolean
        get() = recognizedText.trim().any { it.isLetterOrDigit() }

    val canCompleteVoiceInput: Boolean
        get() = hasUsableText
}

/**
 * Stateful controller managing voice recognition lifecycle, permission checks,
 * service availability, locale switching (English & Bangla), partial/final result
 * accumulation, error recovery, retry, manual text entry, and the
 * "Complete Voice Input" transition to the transaction review screen.
 *
 * Never saves a transaction automatically; completing voice input only parses
 * the captured or manually entered text and opens the transaction review state.
 */
class VoiceInputController(
    initialBanglaUi: Boolean = false,
    initialParsedResult: ParsedVoiceTransaction? = null
) {
    private var committedPrefix: String = initialParsedResult?.rawText?.trim().orEmpty()

    private val _state = MutableStateFlow(
        VoiceInputUiState(
            selectedLocale = if (initialBanglaUi) VoiceLocaleOption.BANGLA else VoiceLocaleOption.ENGLISH,
            isBanglaUi = initialBanglaUi,
            recognizedText = initialParsedResult?.rawText?.trim().orEmpty(),
            hasReceivedTranscript = !initialParsedResult?.rawText.isNullOrBlank(),
            isReviewMode = initialParsedResult != null,
            parsedTransaction = initialParsedResult
        )
    )
    val state: StateFlow<VoiceInputUiState> = _state.asStateFlow()

    fun setBanglaUi(isBangla: Boolean) {
        _state.update { it.copy(isBanglaUi = isBangla) }
    }

    fun selectLocale(locale: VoiceLocaleOption) {
        committedPrefix = _state.value.recognizedText.trim()
        Log.d(TAG, "Locale selected: ${locale.languageTag}")
        _state.update {
            it.copy(
                selectedLocale = locale,
                errorMessage = null,
                lastErrorCode = null
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        val isBn = _state.value.isBanglaUi
        Log.d(TAG, "onPermissionResult: granted=$granted")
        if (granted) {
            _state.update {
                it.copy(
                    isPermissionGranted = true,
                    isPermissionDenied = false,
                    lastCallback = "permissionGranted",
                    errorMessage = null,
                    lastErrorCode = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    isPermissionGranted = false,
                    isPermissionDenied = true,
                    isListening = false,
                    lastCallback = "permissionDenied",
                    lastErrorCode = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                    errorMessage = if (isBn) {
                        "মাইক্রোফোন পারমিশন দেওয়া হয়নি (Error 9: ERROR_INSUFFICIENT_PERMISSIONS)। কথা বলতে 'পারমিশন দিন ও আবার চেষ্টা করুন' চাপুন অথবা নিচে খরচের কথা লিখুন।"
                    } else {
                        "Microphone permission was denied (Error 9: ERROR_INSUFFICIENT_PERMISSIONS). Tap 'Grant Permission & Retry' to speak, or type your expense below."
                    }
                )
            }
        }
    }

    fun checkServiceAvailability(isAvailable: Boolean) {
        val isBn = _state.value.isBanglaUi
        Log.d(TAG, "checkServiceAvailability: isAvailable=$isAvailable")
        if (isAvailable) {
            _state.update {
                it.copy(
                    isServiceAvailable = true,
                    errorMessage = if (!it.isServiceAvailable) null else it.errorMessage
                )
            }
        } else {
            _state.update {
                it.copy(
                    isServiceAvailable = false,
                    isListening = false,
                    lastCallback = "serviceUnavailable",
                    errorMessage = if (isBn) {
                        "এই ডিভাইসে স্পিচ রিকগনিশন সার্ভিস উপলব্ধ নেই। গুগল স্পিচ সার্ভিস সক্রিয় করুন অথবা নিচে আপনার খরচের কথা লিখুন।"
                    } else {
                        "Speech recognition service is not available on this device. Enable Google Speech Services or type your expense below."
                    }
                )
            }
        }
    }

    /**
     * Validates permission and service availability before starting recognition.
     * Returns true if recognition can proceed, false otherwise.
     */
    fun onStartListeningRequested(
        hasPermission: Boolean,
        isServiceAvailable: Boolean
    ): Boolean {
        Log.d(TAG, "onStartListeningRequested: locale=${_state.value.selectedLocale.languageTag}, hasPerm=$hasPermission, serviceAvail=$isServiceAvailable")
        if (!hasPermission) {
            onPermissionResult(false)
            return false
        }
        if (!isServiceAvailable) {
            checkServiceAvailability(false)
            return false
        }
        committedPrefix = _state.value.recognizedText.trim()
        _state.update {
            val hadError = it.errorMessage != null || it.lastErrorCode != null
            it.copy(
                isPermissionGranted = true,
                isPermissionDenied = false,
                isServiceAvailable = true,
                isListening = true,
                lastCallback = "startListening",
                errorMessage = null,
                lastErrorCode = null,
                retryCount = if (hadError) it.retryCount + 1 else it.retryCount
            )
        }
        return true
    }

    /**
     * Explicitly retries voice recognition after an error, resetting the error state
     * and incrementing retryCount when recognition restarts.
     */
    fun retry(
        hasPermission: Boolean = _state.value.isPermissionGranted,
        isServiceAvailable: Boolean = _state.value.isServiceAvailable
    ): Boolean {
        Log.d(TAG, "retry called: hasPerm=$hasPermission, serviceAvail=$isServiceAvailable")
        _state.update {
            it.copy(
                errorMessage = null,
                lastErrorCode = null,
                retryCount = it.retryCount + 1
            )
        }
        if (!hasPermission) {
            onPermissionResult(false)
            return false
        }
        if (!isServiceAvailable) {
            checkServiceAvailability(false)
            return false
        }
        committedPrefix = _state.value.recognizedText.trim()
        _state.update {
            it.copy(
                isPermissionGranted = true,
                isPermissionDenied = false,
                isServiceAvailable = true,
                isListening = true,
                lastCallback = "retryListening",
                errorMessage = null,
                lastErrorCode = null
            )
        }
        return true
    }

    fun onReadyForSpeech() {
        Log.d(TAG, "Callback received: onReadyForSpeech")
        _state.update {
            it.copy(
                isListening = true,
                lastCallback = "onReadyForSpeech",
                errorMessage = null,
                lastErrorCode = null
            )
        }
    }

    fun onBeginningOfSpeech() {
        Log.d(TAG, "Callback received: onBeginningOfSpeech")
        _state.update {
            it.copy(
                isListening = true,
                lastCallback = "onBeginningOfSpeech",
                errorMessage = null,
                lastErrorCode = null
            )
        }
    }

    fun onEndOfSpeech() {
        Log.d(TAG, "Callback received: onEndOfSpeech")
        _state.update {
            it.copy(
                isListening = false,
                lastCallback = "onEndOfSpeech"
            )
        }
    }

    fun onPartialResults(partialBundle: Bundle?) {
        val stable = extractBestRecognitionCandidate(partialBundle, SpeechRecognizer.RESULTS_RECOGNITION, "results")
        val unstable = extractBestRecognitionCandidate(partialBundle, EXTRA_UNSTABLE_TEXT, "android.speech.extra.PARTIAL_RESULTS")
        val combinedSegment = combineSegments(stable, unstable)
        val hasText = combinedSegment.isNotBlank()
        Log.d(TAG, "Callback received: onPartialResults (hasTranscript=$hasText, charLength=${combinedSegment.length})")
        onPartialTextCaptured(combinedSegment)
    }

    fun onPartialTextCaptured(partialSegment: String) {
        val cleanSegment = partialSegment.trim()
        if (cleanSegment.isEmpty()) return
        val fullText = combineWithPrefix(committedPrefix, cleanSegment)
        _state.update {
            it.copy(
                recognizedText = fullText,
                lastCallback = "onPartialResults",
                hasReceivedTranscript = true,
                errorMessage = null,
                lastErrorCode = null
            )
        }
    }

    fun onResults(resultsBundle: Bundle?) {
        val bestMatch = extractBestRecognitionCandidate(resultsBundle, SpeechRecognizer.RESULTS_RECOGNITION, RecognizerIntent.EXTRA_RESULTS)
        val hasMatch = bestMatch.isNotBlank()
        Log.d(TAG, "Callback received: onResults (hasTranscript=$hasMatch, charLength=${bestMatch.length})")
        onFinalTextCaptured(bestMatch)
    }

    /**
     * Handles final recognized speech segment from SpeechRecognizer or system RecognizerIntent.
     * Updates recognizedText so the user can check what was captured, and waits for the user
     * to tap "Complete Voice Input" (never auto-completes or auto-saves).
     */
    fun onFinalTextCaptured(finalSegment: String) {
        val isBn = _state.value.isBanglaUi
        val cleanSegment = finalSegment.trim()
        if (cleanSegment.isNotEmpty()) {
            val fullText = combineWithPrefix(committedPrefix, cleanSegment)
            committedPrefix = fullText
            Log.d(TAG, "Final transcript delivered to state: totalLength=${fullText.length}")
            _state.update {
                it.copy(
                    isListening = false,
                    recognizedText = fullText,
                    lastCallback = "onResults",
                    hasReceivedTranscript = true,
                    errorMessage = null,
                    lastErrorCode = null
                )
            }
        } else {
            Log.d(TAG, "Final transcript empty/blank")
            val existing = _state.value.recognizedText.trim()
            if (existing.any { it.isLetterOrDigit() }) {
                committedPrefix = existing
                _state.update {
                    it.copy(
                        isListening = false,
                        lastCallback = "onResults(preserved)",
                        errorMessage = null,
                        lastErrorCode = null
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isListening = false,
                        lastCallback = "onResults(empty)",
                        lastErrorCode = SpeechRecognizer.ERROR_NO_MATCH,
                        errorMessage = if (isBn) {
                            "কোনো কথা স্পষ্টভাবে শনাক্ত করা যায়নি (Error 7: ERROR_NO_MATCH)। মাইকে ট্যাপ করে আবার বলুন অথবা নিচে খরচের কথা লিখুন।"
                        } else {
                            "Could not recognize any speech (Error 7: ERROR_NO_MATCH). Tap the microphone to retry or type your expense below."
                        }
                    )
                }
            }
        }
    }

    fun onError(errorCode: Int) {
        val isBn = _state.value.isBanglaUi
        val errorName = getErrorCodeName(errorCode)
        Log.w(TAG, "Callback received: onError: code=$errorCode ($errorName)")

        if (errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            onPermissionResult(false)
            return
        }

        val currentHasUsableText = _state.value.hasUsableText
        if (currentHasUsableText) {
            committedPrefix = _state.value.recognizedText.trim()
        }

        // If the user already captured valid speech before a trailing silence timeout or no-match,
        // keep the captured text intact and allow them to review & tap Complete Voice Input.
        if (currentHasUsableText && (
                errorCode == SpeechRecognizer.ERROR_NO_MATCH ||
                    errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    errorCode == SpeechRecognizer.ERROR_CLIENT
                )
        ) {
            _state.update {
                it.copy(
                    isListening = false,
                    lastCallback = "onError($errorCode:$errorName, ignored due to usable text)",
                    errorMessage = null,
                    lastErrorCode = null
                )
            }
            return
        }

        val message = when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH -> {
                if (isBn) {
                    "কথা স্পষ্ট বোঝা যায়নি (Error $errorCode: $errorName)। যেমন বলুন: 'বাজারে ৫০০ টাকা খরচ'। আবার চেষ্টা করুন অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "No matching speech recognized (Error $errorCode: $errorName). Try saying \"500 taka groceries\". Tap Retry to speak again or type your expense below."
                }
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (isBn) {
                    "নির্ধারিত সময়ে কোনো কথা শোনা যায়নি (Error $errorCode: $errorName)। মাইকে ট্যাপ করে আবার বলুন অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "No speech detected before timeout (Error $errorCode: $errorName). Tap Retry and speak into the microphone, or type your expense below."
                }
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                if (isBn) {
                    "নেটওয়ার্ক সমস্যার কারণে কথা শনাক্ত করা যায়নি (Error $errorCode: $errorName)। ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন অথবা নিচে লিখুন।"
                } else {
                    "Network error during speech recognition (Error $errorCode: $errorName). Check your internet connection and tap Retry, or type your expense below."
                }
            }
            SpeechRecognizer.ERROR_AUDIO -> {
                if (isBn) {
                    "মাইক্রোফোন অডিও রেকর্ডিং ত্রুটি (Error $errorCode: $errorName)। অন্য কোনো অ্যাপ মাইক ব্যবহার করছে কিনা দেখে আবার চেষ্টা করুন অথবা নিচে লিখুন।"
                } else {
                    "Microphone audio recording error (Error $errorCode: $errorName). Check that no other app is using the mic, then tap Retry or type your expense below."
                }
            }
            SpeechRecognizer.ERROR_SERVER -> {
                if (isBn) {
                    "স্পিচ রিকগনিশন সার্ভারে সংযোগ করা যায়নি (Error $errorCode: $errorName)। 'আবার চেষ্টা করুন' বা 'গুগল স্পিচ সার্ভিস ব্যবহার করুন' চাপুন, অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "Speech recognition service could not reach the server (Error $errorCode: $errorName). Tap Retry or Use Google Speech Recognition, or type your expense below."
                }
            }
            ERROR_SERVER_DISCONNECTED -> {
                if (isBn) {
                    "স্পিচ রিকগনিশন সার্ভিসের সংযোগ বিচ্ছিন্ন হয়েছে (Error $errorCode: $errorName)। আবার চেষ্টা করুন অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "Speech recognition service disconnected unexpectedly (Error $errorCode: $errorName). Tap Retry to reconnect or type your expense below."
                }
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                if (isBn) {
                    "স্পিচ রিকগনাইজার বর্তমানে ব্যস্ত আছে (Error $errorCode: $errorName)। একটু অপেক্ষা করে আবার চেষ্টা করুন অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "Speech recognizer is currently busy (Error $errorCode: $errorName). Wait a moment and tap Retry, or type your expense below."
                }
            }
            SpeechRecognizer.ERROR_CLIENT -> {
                if (isBn) {
                    "ডিভাইস স্পিচ ক্লায়েন্ট ত্রুটি (Error $errorCode: $errorName)। আবার চেষ্টা করুন, গুগল স্পিচ সার্ভিস ব্যবহার করুন, অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "Device speech client error (Error $errorCode: $errorName). Tap Retry, use Google Speech Recognition, or type your expense below."
                }
            }
            ERROR_LANGUAGE_NOT_SUPPORTED,
            ERROR_LANGUAGE_UNAVAILABLE -> {
                if (isBn) {
                    "নির্বাচিত ভাষাটি (${_state.value.selectedLocale.labelBn}) ডিভাইসের স্পিচ সার্ভিসে সমর্থিত নয় (Error $errorCode: $errorName)। ভাষা পরিবর্তন করুন, গুগল স্পিচ সার্ভিস ব্যবহার করুন, অথবা নিচে লিখুন।"
                } else {
                    "Selected language (${_state.value.selectedLocale.labelEn}) is not supported by the device speech service (Error $errorCode: $errorName). Switch language, use Google Speech Recognition, or type below."
                }
            }
            ERROR_TOO_MANY_REQUESTS -> {
                if (isBn) {
                    "অনেক বেশি অনুরোধ পাঠানো হয়েছে (Error $errorCode: $errorName)। কিছুক্ষণ অপেক্ষা করে আবার চেষ্টা করুন অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "Too many speech recognition requests (Error $errorCode: $errorName). Wait a moment and tap Retry, or type your expense below."
                }
            }
            else -> {
                if (isBn) {
                    "ভয়েস শনাক্তকরণে সমস্যা হয়েছে (Error $errorCode: $errorName)। আবার চেষ্টা করুন, গুগল স্পিচ সার্ভিস ব্যবহার করুন, অথবা নিচে খরচের কথা লিখুন।"
                } else {
                    "Voice recognition could not complete (Error $errorCode: $errorName). Tap Retry, use Google Speech Recognition, or type your expense below."
                }
            }
        }

        _state.update {
            it.copy(
                isListening = false,
                lastCallback = "onError($errorCode:$errorName)",
                lastErrorCode = errorCode,
                errorMessage = message
            )
        }
    }

    fun updateRecognizedText(text: String) {
        committedPrefix = text.trim()
        val hasUsable = text.trim().any { ch -> ch.isLetterOrDigit() }
        _state.update {
            it.copy(
                recognizedText = text,
                hasReceivedTranscript = if (hasUsable) true else it.hasReceivedTranscript,
                lastCallback = "manualTextUpdate",
                errorMessage = if (hasUsable) null else it.errorMessage,
                lastErrorCode = if (hasUsable) null else it.lastErrorCode
            )
        }
    }

    fun clearRecognizedText() {
        committedPrefix = ""
        _state.update {
            it.copy(
                recognizedText = "",
                hasReceivedTranscript = false,
                lastCallback = "clearRecognizedText",
                errorMessage = null,
                lastErrorCode = null
            )
        }
    }

    /**
     * Called when the user taps "Complete Voice Input".
     * Prevents proceeding if recognizedText is empty or has no usable alphanumeric/Bangla characters,
     * setting a clear error message. Otherwise parses the recognized text into a draft transaction
     * and transitions to the transaction review screen without saving automatically.
     */
    fun completeVoiceInput(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): ParsedVoiceTransaction? {
        val isBn = _state.value.isBanglaUi
        val currentText = _state.value.recognizedText.trim()

        if (!currentText.any { it.isLetterOrDigit() }) {
            _state.update {
                it.copy(
                    isListening = false,
                    isReviewMode = false,
                    parsedTransaction = null,
                    errorMessage = if (isBn) {
                        "এখনো কোনো কথা বা টেক্সট পাওয়া যায়নি। মাইক্রোফোনে খরচের কথা বলুন অথবা নিচে টেক্সট লিখুন।"
                    } else {
                        "No speech or text entered yet. Please speak using the microphone or type your expense below before completing."
                    }
                )
            }
            return null
        }

        val parsed = VoiceTransactionParser.parse(currentText, accounts, categories)
        _state.update {
            it.copy(
                isListening = false,
                errorMessage = null,
                lastErrorCode = null,
                isReviewMode = true,
                parsedTransaction = parsed
            )
        }
        return parsed
    }

    fun backToVoiceInput(clearText: Boolean = false) {
        if (clearText) {
            committedPrefix = ""
        }
        _state.update {
            it.copy(
                isListening = false,
                isReviewMode = false,
                parsedTransaction = null,
                recognizedText = if (clearText) "" else it.recognizedText,
                hasReceivedTranscript = if (clearText) false else it.hasReceivedTranscript,
                errorMessage = null,
                lastErrorCode = null
            )
        }
    }

    companion object {
        const val TAG = "VoiceInput"
        const val EXTRA_UNSTABLE_TEXT = "android.speech.extra.UNSTABLE_TEXT"
        const val ERROR_TOO_MANY_REQUESTS = 10
        const val ERROR_SERVER_DISCONNECTED = 11
        const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
        const val ERROR_LANGUAGE_UNAVAILABLE = 13

        fun getErrorCodeName(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                ERROR_TOO_MANY_REQUESTS -> "ERROR_TOO_MANY_REQUESTS"
                ERROR_SERVER_DISCONNECTED -> "ERROR_SERVER_DISCONNECTED"
                ERROR_LANGUAGE_NOT_SUPPORTED -> "ERROR_LANGUAGE_NOT_SUPPORTED"
                ERROR_LANGUAGE_UNAVAILABLE -> "ERROR_LANGUAGE_UNAVAILABLE"
                else -> "ERROR_UNKNOWN($errorCode)"
            }
        }

        fun normalizeLanguageTag(languageTag: String): String {
            val cleaned = languageTag.trim().replace('_', '-')
            return when {
                cleaned.isEmpty() -> VoiceLocaleOption.ENGLISH.languageTag
                cleaned.equals("bn", ignoreCase = true) || cleaned.equals("bn-BD", ignoreCase = true) -> VoiceLocaleOption.BANGLA.languageTag
                cleaned.equals("en", ignoreCase = true) || cleaned.equals("en-US", ignoreCase = true) -> VoiceLocaleOption.ENGLISH.languageTag
                else -> cleaned
            }
        }

        fun hasRecordAudioPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun resolveRecognitionServiceComponent(context: Context): ComponentName? {
            return try {
                val pm = context.packageManager
                val services = pm.queryIntentServices(
                    Intent(RecognitionService.SERVICE_INTERFACE),
                    0
                ).orEmpty()
                val preferred = services.firstOrNull { info ->
                    val pkg = info.serviceInfo?.packageName.orEmpty()
                    pkg.contains("google", ignoreCase = true)
                } ?: services.firstOrNull()
                preferred?.serviceInfo?.let { ComponentName(it.packageName, it.name) }
            } catch (_: Exception) {
                null
            }
        }

        fun isSpeechServiceAvailable(context: Context): Boolean {
            return try {
                val directAvailable = SpeechRecognizer.isRecognitionAvailable(context)
                if (directAvailable) return true
                if (resolveRecognitionServiceComponent(context) != null) return true
                val pm = context.packageManager
                val activityHandlers = pm.queryIntentActivities(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                    0
                )
                !activityHandlers.isNullOrEmpty()
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Builds a standard, broadly compatible RecognizerIntent for English (en-US) or Bangla (bn-BD).
         * Avoids non-standard silence length extras (`EXTRA_SPEECH_INPUT_*_MILLIS`) which cause
         * Google Speech Services to reject requests with `SpeechRecognizer.ERROR_SERVER` (4).
         */
        fun buildRecognizerIntent(
            languageTag: String,
            callingPackage: String? = null
        ): Intent {
            val normalizedTag = normalizeLanguageTag(languageTag)
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, normalizedTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, normalizedTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                if (!callingPackage.isNullOrBlank()) {
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, callingPackage)
                }
            }
        }

        /**
         * Robust extractor handling both String and CharSequence types, arrays, ArrayLists,
         * and custom Bundle keys used across different Android versions and recognition engines.
         * Strictly filters out blank and empty candidates.
         */
        fun extractBestRecognitionCandidate(bundle: Bundle?, vararg preferredKeys: String): String {
            if (bundle == null) return ""

            val orderedKeys = if (preferredKeys.isNotEmpty()) {
                (preferredKeys.toList() + listOf(
                    SpeechRecognizer.RESULTS_RECOGNITION,
                    RecognizerIntent.EXTRA_RESULTS,
                    EXTRA_UNSTABLE_TEXT,
                    "android.speech.extra.PARTIAL_RESULTS",
                    "android.speech.extra.RESULTS",
                    "results"
                )).distinct()
            } else {
                listOf(
                    SpeechRecognizer.RESULTS_RECOGNITION,
                    RecognizerIntent.EXTRA_RESULTS,
                    EXTRA_UNSTABLE_TEXT,
                    "android.speech.extra.PARTIAL_RESULTS",
                    "android.speech.extra.RESULTS",
                    "results"
                )
            }

            for (key in orderedKeys) {
                val candidate = extractStringFromBundle(bundle, key)
                if (candidate.isNotBlank()) {
                    return candidate
                }
            }

            // Fallback: check all other keys in the bundle for any recognized text list or string
            try {
                for (key in bundle.keySet()) {
                    if (orderedKeys.contains(key)) continue
                    val candidate = extractStringFromBundle(bundle, key)
                    if (candidate.isNotBlank()) {
                        return candidate
                    }
                }
            } catch (_: Exception) {
            }

            return ""
        }

        /**
         * Extracts candidate string from a bundle key, supporting String, CharSequence,
         * Array<String>, Array<CharSequence>, List<String>, List<CharSequence>, etc.
         * Returns the first non-blank candidate trimmed, or empty string if none.
         */
        fun extractStringFromBundle(bundle: Bundle, key: String): String {
            return try {
                val raw = bundle.get(key) ?: return ""
                when (raw) {
                    is CharSequence -> {
                        val trimmed = raw.toString().trim()
                        if (trimmed.isNotBlank()) trimmed else ""
                    }
                    is Array<*> -> {
                        raw.firstOrNull { it != null && it.toString().trim().isNotBlank() }
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    }
                    is Iterable<*> -> {
                        raw.firstOrNull { it != null && it.toString().trim().isNotBlank() }
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                    }
                    else -> ""
                }
            } catch (_: Exception) {
                ""
            }
        }

        /**
         * Extracts candidate string from an Intent's extras or string extra.
         */
        fun extractBestCandidateFromIntent(intent: Intent?): String {
            if (intent == null) return ""
            val bundle = intent.extras
            if (bundle != null) {
                val candidate = extractBestRecognitionCandidate(
                    bundle,
                    RecognizerIntent.EXTRA_RESULTS,
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                if (candidate.isNotBlank()) return candidate
            }
            val fromExtra = intent.getStringExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!fromExtra.isNullOrBlank()) return fromExtra.trim()
            val fromCharSequence = intent.getCharSequenceExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!fromCharSequence.isNullOrBlank()) return fromCharSequence.toString().trim()
            return ""
        }

        private fun combineSegments(stable: String, unstable: String): String {
            return when {
                stable.isEmpty() -> unstable
                unstable.isEmpty() -> stable
                stable.endsWith(unstable, ignoreCase = true) -> stable
                else -> "$stable $unstable".trim()
            }
        }

        private fun combineWithPrefix(prefix: String, newSegment: String): String {
            val cleanPrefix = prefix.trim()
            val cleanNew = newSegment.trim()
            return when {
                cleanPrefix.isEmpty() -> cleanNew
                cleanNew.isEmpty() -> cleanPrefix
                cleanNew.startsWith(cleanPrefix, ignoreCase = true) -> cleanNew
                cleanPrefix.endsWith(cleanNew, ignoreCase = true) -> cleanPrefix
                else -> "$cleanPrefix $cleanNew".trim()
            }
        }
    }
}
