package com.example

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.ocr.ReceiptOcrManager
import com.example.ocr.ReceiptParser
import com.example.voice.VoiceInputController
import com.example.voice.VoiceLocaleOption
import com.example.voice.VoiceTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceAndReceiptEntryTest {

    private val sampleAccounts = listOf(
        AccountEntity(
            id = "acc_cash",
            name = "Cash Wallet",
            type = AccountType.CASH,
            balance = 5000.0,
            openingBalance = 5000.0,
            colorHex = "#10B981",
            iconName = "wallet"
        ),
        AccountEntity(
            id = "acc_bkash",
            name = "bKash",
            type = AccountType.MFS,
            balance = 12000.0,
            openingBalance = 12000.0,
            colorHex = "#E2136E",
            iconName = "phone_android"
        ),
        AccountEntity(
            id = "acc_city",
            name = "City Bank",
            type = AccountType.BANK,
            balance = 45000.0,
            openingBalance = 45000.0,
            colorHex = "#2563EB",
            iconName = "account_balance"
        )
    )

    private val sampleCategories = listOf(
        CategoryEntity(
            id = "cat_food",
            name = "Food & Dining",
            nameBn = "খাবার ও রেস্তোরাঁ",
            iconName = "restaurant",
            colorHex = "#EF4444",
            isIncome = false
        ),
        CategoryEntity(
            id = "cat_groceries",
            name = "Groceries",
            nameBn = "বাজার ও মুদি",
            iconName = "shopping_cart",
            colorHex = "#10B981",
            isIncome = false
        ),
        CategoryEntity(
            id = "cat_transport",
            name = "Transport",
            nameBn = "যাতায়াত",
            iconName = "directions_bus",
            colorHex = "#F59E0B",
            isIncome = false
        ),
        CategoryEntity(
            id = "cat_bills",
            name = "Bills & Utilities",
            nameBn = "বিল ও ইউটিলিটি",
            iconName = "receipt",
            colorHex = "#3B82F6",
            isIncome = false
        ),
        CategoryEntity(
            id = "cat_salary",
            name = "Salary",
            nameBn = "বেতন",
            iconName = "payments",
            colorHex = "#10B981",
            isIncome = true
        )
    )

    // =========================================================================
    // VOICE INPUT CONTROLLER TESTS (7 Required Scenarios)
    // =========================================================================

    // Scenario 1: Successful recognition (English, Bangla, Partial & Multi-Segment, Intent Config)
    @Test
    fun testVoiceController_SuccessfulRecognition_DisplaysCapturedTextWithoutAutoProceeding() {
        val controller = VoiceInputController(initialBanglaUi = false)

        val started = controller.onStartListeningRequested(
            hasPermission = true,
            isServiceAvailable = true
        )
        assertTrue(started)
        assertTrue(controller.state.value.isListening)
        assertNull(controller.state.value.errorMessage)

        // Simulate partial recognition arriving while user speaks
        controller.onPartialTextCaptured("spent 500 on")
        assertEquals("spent 500 on", controller.state.value.recognizedText)
        assertTrue(controller.state.value.hasUsableText)
        assertTrue(controller.state.value.canCompleteVoiceInput)
        // Must NOT auto-open review screen while still capturing speech
        assertFalse(controller.state.value.isReviewMode)

        // Simulate final recognition result
        controller.onFinalTextCaptured("spent 500 on groceries with Cash")
        val afterFinal = controller.state.value
        assertFalse(afterFinal.isListening)
        assertEquals("spent 500 on groceries with Cash", afterFinal.recognizedText)
        assertTrue(afterFinal.hasUsableText)
        assertTrue(afterFinal.canCompleteVoiceInput)
        assertNull(afterFinal.errorMessage)
        // Must stay on Voice Input screen until user taps "Complete Voice Input"
        assertFalse(afterFinal.isReviewMode)
        assertNull(afterFinal.parsedTransaction)
    }

    @Test
    fun testVoiceController_SuccessfulBanglaRecognition_AndLocaleNormalization() {
        val controller = VoiceInputController(initialBanglaUi = true)
        assertEquals(VoiceLocaleOption.BANGLA, controller.state.value.selectedLocale)
        assertEquals("bn-BD", VoiceInputController.normalizeLanguageTag("bn_BD"))
        assertEquals("en-US", VoiceInputController.normalizeLanguageTag("en_US"))

        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onFinalTextCaptured("বাজারে ৫০০ টাকা খরচ বিকাশ থেকে")

        assertEquals("বাজারে ৫০০ টাকা খরচ বিকাশ থেকে", controller.state.value.recognizedText)
        assertTrue(controller.state.value.canCompleteVoiceInput)
        assertFalse(controller.state.value.isReviewMode)

        // Switching locale to English keeps already-captured text intact
        controller.selectLocale(VoiceLocaleOption.ENGLISH)
        assertEquals(VoiceLocaleOption.ENGLISH, controller.state.value.selectedLocale)
        assertEquals("বাজারে ৫০০ টাকা খরচ বিকাশ থেকে", controller.state.value.recognizedText)
    }

    // Scenario 2: Server & Service errors show specific actionable messages (never generic server error)
    @Test
    fun testVoiceController_ServerAndServiceErrors_ShowActionableMessagesAndPreservePartialText() {
        val controller = VoiceInputController(initialBanglaUi = false)

        // 2a. ERROR_SERVER when field is empty
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onError(SpeechRecognizer.ERROR_SERVER)
        val serverErrState = controller.state.value
        assertFalse(serverErrState.isListening)
        assertEquals(SpeechRecognizer.ERROR_SERVER, serverErrState.lastErrorCode)
        assertFalse(serverErrState.canCompleteVoiceInput)
        assertNotNull(serverErrState.errorMessage)
        assertFalse(
            "Must not show generic 'Speech recognition server error. Please try again.'",
            serverErrState.errorMessage == "Speech recognition server error. Please try again."
        )
        assertTrue(serverErrState.errorMessage!!.contains("Retry", ignoreCase = true))
        assertTrue(serverErrState.errorMessage!!.contains("type", ignoreCase = true))

        // 2b. ERROR_SERVER_DISCONNECTED shows distinct disconnection guidance
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onError(VoiceInputController.ERROR_SERVER_DISCONNECTED)
        val disconnectedState = controller.state.value
        assertFalse(disconnectedState.isListening)
        assertEquals(VoiceInputController.ERROR_SERVER_DISCONNECTED, disconnectedState.lastErrorCode)
        assertNotNull(disconnectedState.errorMessage)
        assertTrue(disconnectedState.errorMessage!!.contains("disconnected", ignoreCase = true))
        assertTrue(disconnectedState.errorMessage!!.contains("Retry", ignoreCase = true))

        // 2c. Unavailable speech service shows actionable message with manual entry guidance
        val startedWhenUnavailable = controller.onStartListeningRequested(
            hasPermission = true,
            isServiceAvailable = false
        )
        assertFalse(startedWhenUnavailable)
        assertFalse(controller.state.value.isListening)
        assertFalse(controller.state.value.isServiceAvailable)
        assertNotNull(controller.state.value.errorMessage)
        assertTrue(controller.state.value.errorMessage!!.contains("not available", ignoreCase = true))
        assertTrue(controller.state.value.errorMessage!!.contains("type", ignoreCase = true))

        // 2d. If partial speech WAS captured before ERROR_SERVER occurs, captured text is preserved
        controller.checkServiceAvailability(true)
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onPartialTextCaptured("250 taka lunch")
        controller.onError(SpeechRecognizer.ERROR_SERVER)
        assertEquals("250 taka lunch", controller.state.value.recognizedText)
        assertTrue(controller.state.value.canCompleteVoiceInput)
    }

    // Scenario 3: No speech (ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT, empty input)
    @Test
    fun testVoiceController_NoSpeech_ShowsClearMessageAndDisablesCompleteUntilUsableText() {
        val controller = VoiceInputController(initialBanglaUi = false)

        // Initially empty: Complete Voice Input button must be disabled
        assertFalse(controller.state.value.hasUsableText)
        assertFalse(controller.state.value.canCompleteVoiceInput)

        // 3a. ERROR_NO_MATCH when no text captured
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertFalse(controller.state.value.isListening)
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertNotNull(controller.state.value.errorMessage)
        assertTrue(controller.state.value.errorMessage!!.contains("Retry", ignoreCase = true))

        // 3b. ERROR_SPEECH_TIMEOUT when no text captured
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
        assertFalse(controller.state.value.isListening)
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertNotNull(controller.state.value.errorMessage)
        assertTrue(controller.state.value.errorMessage!!.contains("timeout", ignoreCase = true))

        // 3c. Empty final result or punctuation-only text keeps Complete Voice Input disabled
        controller.onFinalTextCaptured("   ")
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertNotNull(controller.state.value.errorMessage)

        controller.updateRecognizedText("   ...   ")
        assertFalse(controller.state.value.hasUsableText)
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertNull(controller.completeVoiceInput(sampleAccounts, sampleCategories))
        assertFalse(controller.state.value.isReviewMode)
    }

    // Scenario 4: Permission denial stops listening, shows actionable message, and recovers on grant
    @Test
    fun testVoiceController_PermissionDenial_StopsListeningAndShowsActionableError() {
        val controller = VoiceInputController(initialBanglaUi = false)

        // Starting without permission fails immediately and sets permissionDenied
        val started = controller.onStartListeningRequested(
            hasPermission = false,
            isServiceAvailable = true
        )
        assertFalse(started)
        assertFalse(controller.state.value.isListening)
        assertTrue(controller.state.value.isPermissionDenied)
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertNotNull(controller.state.value.errorMessage)
        assertTrue(controller.state.value.errorMessage!!.contains("permission", ignoreCase = true))
        assertTrue(controller.state.value.errorMessage!!.contains("Retry", ignoreCase = true))

        // Granting permission on retry clears permissionDenied and errorMessage
        controller.onPermissionResult(true)
        assertFalse(controller.state.value.isPermissionDenied)
        assertTrue(controller.state.value.isPermissionGranted)
        assertNull(controller.state.value.errorMessage)

        // SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS also triggers permissionDenied state
        controller.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        assertTrue(controller.state.value.isPermissionDenied)
        assertFalse(controller.state.value.isListening)
        assertNotNull(controller.state.value.errorMessage)
    }

    // Scenario 5: Retry after server/network/no-speech errors resets error state and captures speech
    @Test
    fun testVoiceController_RetryAfterServerError_ClearsErrorAndCapturesSpeech() {
        val controller = VoiceInputController(initialBanglaUi = false)

        // Initial attempt fails with ERROR_SERVER
        assertTrue(controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true))
        controller.onError(SpeechRecognizer.ERROR_SERVER)
        assertFalse(controller.state.value.isListening)
        assertEquals(SpeechRecognizer.ERROR_SERVER, controller.state.value.lastErrorCode)
        assertNotNull(controller.state.value.errorMessage)
        assertEquals(0, controller.state.value.retryCount)

        // User taps Retry -> clears error, increments retryCount, starts listening
        val retried = controller.retry(hasPermission = true, isServiceAvailable = true)
        assertTrue(retried)
        assertTrue(controller.state.value.isListening)
        assertNull(controller.state.value.errorMessage)
        assertNull(controller.state.value.lastErrorCode)
        assertEquals(1, controller.state.value.retryCount)

        // Subsequent recognition succeeds
        controller.onFinalTextCaptured("120 taka rickshaw fare")
        assertFalse(controller.state.value.isListening)
        assertNull(controller.state.value.errorMessage)
        assertEquals("120 taka rickshaw fare", controller.state.value.recognizedText)
        assertTrue(controller.state.value.canCompleteVoiceInput)
    }

    // Scenario 6: Manual text entry & editing works even after server error, service unavailable, or permission denial
    @Test
    fun testVoiceController_ManualTextEntry_EnablesCompletionAfterRecognitionFailure() {
        val controller = VoiceInputController(initialBanglaUi = false)

        // Recognition fails with ERROR_SERVER and empty recognized text
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onError(SpeechRecognizer.ERROR_SERVER)
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertNotNull(controller.state.value.errorMessage)

        // User manually enters text into the editable Recognized Speech field
        controller.updateRecognizedText("350 taka medicine")
        val afterManualEntry = controller.state.value
        assertEquals("350 taka medicine", afterManualEntry.recognizedText)
        assertTrue(afterManualEntry.hasUsableText)
        assertTrue(afterManualEntry.canCompleteVoiceInput)
        assertNull("Manual entry of usable text should clear the error banner", afterManualEntry.errorMessage)
        assertFalse("Must not auto-open review screen while typing", afterManualEntry.isReviewMode)

        // User can also edit the text before completing
        controller.updateRecognizedText("400 taka medicine from cash")
        assertEquals("400 taka medicine from cash", controller.state.value.recognizedText)
        assertTrue(controller.state.value.canCompleteVoiceInput)

        // Even if microphone permission was denied, manual text entry still allows completing input
        val deniedController = VoiceInputController(initialBanglaUi = false)
        deniedController.onPermissionResult(false)
        assertFalse(deniedController.state.value.canCompleteVoiceInput)
        deniedController.updateRecognizedText("100 taka tea")
        assertTrue(deniedController.state.value.canCompleteVoiceInput)
        val parsedFromDenied = deniedController.completeVoiceInput(sampleAccounts, sampleCategories)
        assertNotNull(parsedFromDenied)
        assertTrue(deniedController.state.value.isReviewMode)
        assertEquals(100.0, parsedFromDenied!!.amount, 0.001)
    }

    // Scenario 7: Completing input parses captured/edited text and opens Review Transaction without auto-saving
    @Test
    fun testVoiceController_CompletingInput_ParsesTransactionAndOpensReviewScreenWithoutAutoSaving() {
        val controller = VoiceInputController(initialBanglaUi = false)
        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        controller.onFinalTextCaptured("spent 500 on groceries with Cash")

        val parsed = controller.completeVoiceInput(sampleAccounts, sampleCategories)
        assertNotNull(parsed)

        val state = controller.state.value
        assertFalse(state.isListening)
        assertTrue(state.isReviewMode)
        assertNull(state.errorMessage)
        assertNotNull(state.parsedTransaction)
        assertEquals(500.0, state.parsedTransaction!!.amount, 0.001)
        assertEquals("Groceries", state.parsedTransaction!!.categoryName)
        assertEquals("acc_cash", state.parsedTransaction!!.matchedAccountId)
        assertEquals(TransactionType.EXPENSE, state.parsedTransaction!!.type)

        // User can go back from Review Transaction to Voice Input to edit or speak again
        controller.backToVoiceInput(clearText = false)
        assertFalse(controller.state.value.isReviewMode)
        assertEquals("spent 500 on groceries with Cash", controller.state.value.recognizedText)
    }

    // Scenario 8: Candidate extraction handles String, CharSequence, Arrays, Lists, and ignores blank candidates
    @Test
    fun testVoiceController_ExtractsCandidatesFromBothStringAndCharSequenceAndArrays() {
        // 8a: ArrayList<String>
        val bundleStringList = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("lunch 300 tk", "lunch 300"))
        }
        val matchStringList = VoiceInputController.extractBestRecognitionCandidate(bundleStringList)
        assertEquals("lunch 300 tk", matchStringList)

        // 8b: ArrayList<CharSequence>
        val bundleCharSeqList = Bundle().apply {
            putCharSequenceArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(StringBuilder("dinner 450 taka") as CharSequence))
        }
        val matchCharSeqList = VoiceInputController.extractBestRecognitionCandidate(bundleCharSeqList)
        assertEquals("dinner 450 taka", matchCharSeqList)

        // 8c: Array<String> (String[])
        val bundleStringArray = Bundle().apply {
            putStringArray(SpeechRecognizer.RESULTS_RECOGNITION, arrayOf("groceries 500 tk", "groceries"))
        }
        val matchStringArray = VoiceInputController.extractBestRecognitionCandidate(bundleStringArray)
        assertEquals("groceries 500 tk", matchStringArray)

        // 8d: Single CharSequence / String
        val bundleSingleCharSeq = Bundle().apply {
            putCharSequence(SpeechRecognizer.RESULTS_RECOGNITION, StringBuilder("medicine 120 taka"))
        }
        val matchSingleCharSeq = VoiceInputController.extractBestRecognitionCandidate(bundleSingleCharSeq)
        assertEquals("medicine 120 taka", matchSingleCharSeq)

        // 8e: Ignores leading empty/whitespace candidates and extracts first usable candidate
        val bundleWithBlanks = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("", "   ", "\t\n", "tea 25 taka"))
        }
        val matchWithBlanks = VoiceInputController.extractBestRecognitionCandidate(bundleWithBlanks)
        assertEquals("tea 25 taka", matchWithBlanks)

        // 8f: Fallback to alternate bundle keys (android.speech.extra.PARTIAL_RESULTS, results)
        val bundleAlternateKey = Bundle().apply {
            putString("android.speech.extra.PARTIAL_RESULTS", "bus fare 40")
        }
        val matchAlternate = VoiceInputController.extractBestRecognitionCandidate(bundleAlternateKey)
        assertEquals("bus fare 40", matchAlternate)

        // 8g: All blank returns empty string
        val bundleAllBlank = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("   ", ""))
        }
        val matchAllBlank = VoiceInputController.extractBestRecognitionCandidate(bundleAllBlank)
        assertEquals("", matchAllBlank)
    }

    // Scenario 9: Lifecycle callback tracing & error code surfacing in UI state
    @Test
    fun testVoiceController_TracesCallbacksAndSurfacesErrorCodes() {
        val controller = VoiceInputController(initialBanglaUi = false)

        controller.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        assertEquals("startListening", controller.state.value.lastCallback)
        assertTrue(controller.state.value.isListening)

        controller.onReadyForSpeech()
        assertEquals("onReadyForSpeech", controller.state.value.lastCallback)
        assertTrue(controller.state.value.isListening)

        controller.onBeginningOfSpeech()
        assertEquals("onBeginningOfSpeech", controller.state.value.lastCallback)
        assertTrue(controller.state.value.isListening)

        // Partial results delivery
        val partialBundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("spent 250"))
        }
        controller.onPartialResults(partialBundle)
        assertEquals("onPartialResults", controller.state.value.lastCallback)
        assertTrue(controller.state.value.hasReceivedTranscript)
        assertEquals("spent 250", controller.state.value.recognizedText)
        assertTrue(controller.state.value.canCompleteVoiceInput)

        // End of speech
        controller.onEndOfSpeech()
        assertEquals("onEndOfSpeech", controller.state.value.lastCallback)
        assertFalse(controller.state.value.isListening)

        // Final results delivery
        val finalBundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("spent 250 on lunch with Cash"))
        }
        controller.onResults(finalBundle)
        assertEquals("onResults", controller.state.value.lastCallback)
        assertTrue(controller.state.value.hasReceivedTranscript)
        assertEquals("spent 250 on lunch with Cash", controller.state.value.recognizedText)
        assertTrue(controller.state.value.canCompleteVoiceInput)

        // Surface error codes and error names
        val freshController = VoiceInputController(initialBanglaUi = false)
        freshController.onStartListeningRequested(hasPermission = true, isServiceAvailable = true)
        freshController.onError(SpeechRecognizer.ERROR_NO_MATCH)
        assertEquals(SpeechRecognizer.ERROR_NO_MATCH, freshController.state.value.lastErrorCode)
        assertTrue(freshController.state.value.lastCallback!!.contains("ERROR_NO_MATCH"))
        assertTrue(freshController.state.value.errorMessage!!.contains("Error 7", ignoreCase = true))

        freshController.onError(SpeechRecognizer.ERROR_CLIENT)
        assertEquals(SpeechRecognizer.ERROR_CLIENT, freshController.state.value.lastErrorCode)
        assertTrue(freshController.state.value.lastCallback!!.contains("ERROR_CLIENT"))
        assertTrue(freshController.state.value.errorMessage!!.contains("Error 5", ignoreCase = true))

        freshController.onError(SpeechRecognizer.ERROR_AUDIO)
        assertEquals(SpeechRecognizer.ERROR_AUDIO, freshController.state.value.lastErrorCode)
        assertTrue(freshController.state.value.errorMessage!!.contains("Error 3", ignoreCase = true))
    }

    // Scenario 10: Bangla (bn-BD) and English (en-US) locales handled separately
    @Test
    fun testVoiceController_BanglaAndEnglishLocalesHandledSeparately() {
        // English locale
        val enController = VoiceInputController(initialBanglaUi = false)
        assertEquals(VoiceLocaleOption.ENGLISH, enController.state.value.selectedLocale)
        assertEquals("en-US", enController.state.value.selectedLocale.languageTag)

        val enIntent = VoiceInputController.buildRecognizerIntent("en-US", "com.example")
        assertEquals("en-US", enIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals("en-US", enIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))

        // Bangla locale
        val bnController = VoiceInputController(initialBanglaUi = true)
        assertEquals(VoiceLocaleOption.BANGLA, bnController.state.value.selectedLocale)
        assertEquals("bn-BD", bnController.state.value.selectedLocale.languageTag)

        val bnIntent = VoiceInputController.buildRecognizerIntent("bn-BD", "com.example")
        assertEquals("bn-BD", bnIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
        assertEquals("bn-BD", bnIntent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE))

        // Normalization handles underscores and bare language codes
        assertEquals("en-US", VoiceInputController.normalizeLanguageTag("en_US"))
        assertEquals("en-US", VoiceInputController.normalizeLanguageTag("en"))
        assertEquals("bn-BD", VoiceInputController.normalizeLanguageTag("bn_BD"))
        assertEquals("bn-BD", VoiceInputController.normalizeLanguageTag("bn"))

        // Switching between locales clears prior errors while keeping recognized speech
        enController.updateRecognizedText("grocery 500")
        enController.selectLocale(VoiceLocaleOption.BANGLA)
        assertEquals(VoiceLocaleOption.BANGLA, enController.state.value.selectedLocale)
        assertEquals("grocery 500", enController.state.value.recognizedText)
    }

    // Scenario 11: Empty and whitespace results ignored; Complete button remains disabled until usable text
    @Test
    fun testVoiceController_EmptyAndWhitespaceIgnored_CompleteDisabledUntilUsableText() {
        val controller = VoiceInputController(initialBanglaUi = false)

        assertFalse(controller.state.value.canCompleteVoiceInput)

        // Passing empty bundle
        controller.onResults(Bundle())
        assertFalse(controller.state.value.canCompleteVoiceInput)

        // Passing bundle with whitespace only
        val whitespaceBundle = Bundle().apply {
            putString(SpeechRecognizer.RESULTS_RECOGNITION, "    ")
        }
        controller.onResults(whitespaceBundle)
        assertFalse(controller.state.value.canCompleteVoiceInput)

        // Manual text entry updates state and enables complete button
        controller.updateRecognizedText("lunch 150 tk")
        assertTrue(controller.state.value.hasUsableText)
        assertTrue(controller.state.value.canCompleteVoiceInput)
        assertEquals("lunch 150 tk", controller.state.value.recognizedText)

        // Clearing text disables complete button again
        controller.clearRecognizedText()
        assertFalse(controller.state.value.hasUsableText)
        assertFalse(controller.state.value.canCompleteVoiceInput)
        assertEquals("", controller.state.value.recognizedText)
    }

    // Scenario 12: Intent result extraction from Activity Result contract
    @Test
    fun testVoiceController_IntentResultExtraction() {
        // Intent with String ArrayList
        val intentStringList = Intent().apply {
            putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("uber fare 220 tk"))
        }
        val matchStringList = VoiceInputController.extractBestCandidateFromIntent(intentStringList)
        assertEquals("uber fare 220 tk", matchStringList)

        // Intent with single string extra
        val intentString = Intent().apply {
            putExtra(RecognizerIntent.EXTRA_RESULTS, "cinema ticket 350 tk")
        }
        val matchString = VoiceInputController.extractBestCandidateFromIntent(intentString)
        assertEquals("cinema ticket 350 tk", matchString)

        // Intent with null data returns empty string
        val matchNull = VoiceInputController.extractBestCandidateFromIntent(null)
        assertEquals("", matchNull)
    }

    // =========================================================================
    // VOICE PARSER & RECEIPT OCR TESTS
    // =========================================================================

    // 1. Voice Input: "10 taka lunch" phrase from user prompt
    @Test
    fun testVoiceInput_TenTakaLunch_ParsesAmountTitleAndCategory() {
        val parsed = VoiceTransactionParser.parse("10 taka lunch", sampleAccounts, sampleCategories)

        assertTrue(parsed.hasConfidentAmount)
        assertEquals(10.0, parsed.amount, 0.001)
        assertTrue(parsed.hasConfidentDescription)
        assertEquals("Lunch", parsed.description)
        assertTrue(parsed.hasConfidentCategory)
        assertEquals("Food & Dining", parsed.categoryName)
        assertEquals("Restaurant", parsed.subcategoryName)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertFalse(parsed.hasConfidentAccount)
        assertNull(parsed.matchedAccountId)
    }

    // 2. Voice Input: Bangla speech with Bengali digits & wallet
    @Test
    fun testVoiceInput_BanglaWithDigitsAndBkash() {
        val spoken = "৫০০ টাকা বাজার বিকাশ থেকে"
        val parsed = VoiceTransactionParser.parse(spoken, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasConfidentAmount)
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.hasConfidentAccount)
        assertEquals("acc_bkash", parsed.matchedAccountId)
        assertTrue(parsed.hasConfidentCategory)
        assertEquals("Groceries", parsed.categoryName)
        assertEquals(TransactionType.EXPENSE, parsed.type)
    }

    // 3. Voice Input: Bangla speech with Bengali word numbers (পাঁচশ টাকা)
    @Test
    fun testVoiceInput_BanglaWordNumber() {
        val spoken = "পাঁচশ টাকা খাবার খরচ"
        val parsed = VoiceTransactionParser.parse(spoken, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasConfidentAmount)
        assertEquals(500.0, parsed.amount, 0.001)
        assertTrue(parsed.hasConfidentCategory)
        assertEquals("Food & Dining", parsed.categoryName)
    }

    // 4. Voice Input: Banglish speech (Rickshaw bhara & bazar)
    @Test
    fun testVoiceInput_BanglishRickshawBhara() {
        val spoken = "rickshaw bhara 40 tk"
        val parsed = VoiceTransactionParser.parse(spoken, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasConfidentAmount)
        assertEquals(40.0, parsed.amount, 0.001)
        assertTrue(parsed.hasConfidentCategory)
        assertEquals("Transport", parsed.categoryName)
        assertTrue(parsed.hasConfidentDescription)
        assertEquals("Rickshaw bhara", parsed.description)
    }

    // 5. Voice Input: English speech with Cash wallet
    @Test
    fun testVoiceInput_EnglishWithCash() {
        val spoken = "250 taka lunch from cash"
        val parsed = VoiceTransactionParser.parse(spoken, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasConfidentAmount)
        assertEquals(250.0, parsed.amount, 0.001)
        assertTrue(parsed.hasConfidentAccount)
        assertEquals("acc_cash", parsed.matchedAccountId)
        assertTrue(parsed.hasConfidentCategory)
        assertEquals("Food & Dining", parsed.categoryName)
    }

    // 6. Voice Input: Unclear speech with no amount does not guess values
    @Test
    fun testVoiceInput_UnclearSpeech_NoGuessedValues() {
        val spoken = "হ্যালো আজকে আবহাওয়া ভালো"
        val parsed = VoiceTransactionParser.parse(spoken, sampleAccounts, sampleCategories)

        assertFalse(parsed.hasConfidentAmount)
        assertEquals(0.0, parsed.amount, 0.001)
        assertFalse(parsed.hasConfidentAccount)
        assertNull(parsed.matchedAccountId)
        assertFalse(parsed.hasConfidentCategory)
        assertEquals("", parsed.categoryName)
    }

    // 7. Voice Input: Speech with no wallet mentioned preserves null wallet
    @Test
    fun testVoiceInput_NoWalletMentioned_PreservesNullWallet() {
        val spoken = "১২০ টাকা রিকশা ভাড়া"
        val parsed = VoiceTransactionParser.parse(spoken, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasConfidentAmount)
        assertEquals(120.0, parsed.amount, 0.001)
        assertFalse(parsed.hasConfidentAccount)
        assertNull(parsed.matchedAccountId)
        assertTrue(parsed.hasConfidentCategory)
        assertEquals("Transport", parsed.categoryName)
    }

    // 8. Receipt OCR: Successful extraction with Bangla merchant, total, date and currency
    @Test
    fun testReceiptParser_SuccessfulReceiptExtraction() {
        val receiptText = """
            স্বপ্ন সুপারশপ
            তারিখ: ১৫/১০/২০২৬
            চাল ৫ কেজি ৩৫০.০০
            ডিম ১ ডজন ২০০.০০
            উপমোট: ৫৫০.০০
            ভ্যাট (৫%): ২৭.৫০
            সর্বমোট: ৫৭৭.৫০ টাকা
            ধন্যবাদ
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasText)
        assertNotNull(parsed.totalAmount)
        assertEquals(577.50, parsed.totalAmount!!, 0.01)
        assertEquals("স্বপ্ন সুপারশপ", parsed.merchantName)
        assertEquals("৳", parsed.detectedCurrency)
        assertNotNull(parsed.dateMillis)
        assertEquals("Groceries", parsed.suggestedCategory)
    }

    // 9. Receipt OCR: Restaurant receipt with English Grand Total
    @Test
    fun testReceiptParser_RestaurantReceipt() {
        val receiptText = """
            Kacchi Bhai Restaurant
            Date: 2026-10-18
            1x Basmati Kacchi 350.00
            2x Borhani 100.00
            Subtotal 450.00
            VAT 22.50
            Grand Total: 472.50 BDT
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasText)
        assertEquals("Kacchi Bhai Restaurant", parsed.merchantName)
        assertEquals(472.50, parsed.totalAmount ?: 0.0, 0.01)
        assertEquals("Food & Dining", parsed.suggestedCategory)
    }

    // 10. Receipt OCR: Prefers Total Payable over Cash Tendered & Change
    @Test
    fun testReceiptParser_PrefersTotalOverLargerCashTenderedAndInvoice() {
        val receiptText = """
            Agora Super Store
            Tel: 01711223344
            Invoice No: 984512
            Date: 02/11/2026 14:35
            Chicken Breast      280.00
            Fresh Milk           90.00
            Sub Total:          370.00
            Total Discount:      55.00
            Total Payable:      315.00
            Cash Tendered:     1000.00
            Change Due:         685.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertEquals(315.00, parsed.totalAmount ?: 0.0, 0.01)
        assertFalse(parsed.isAmountAmbiguous)
        assertTrue(parsed.candidateAmounts.any { it.amount == 315.00 && it.isPrimaryTotal })
        assertTrue(parsed.candidateAmounts.any { it.amount == 370.00 })
    }

    // 11. Receipt OCR: Prefers Net Total after discount over larger Subtotal
    @Test
    fun testReceiptParser_PrefersNetTotalAfterDiscountOverLargerSubtotal() {
        val receiptText = """
            Lazz Pharma
            Date: 01/11/2026
            Napa Extend         120.00
            Vitamin C           380.00
            Subtotal:           500.00
            Less Discount 10%:   50.00
            Net Total:          450.00
            Paid Amount:        500.00
            Change:              50.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertEquals(450.00, parsed.totalAmount ?: 0.0, 0.01)
        assertEquals("Health", parsed.suggestedCategory)
    }

    // 12. Receipt OCR: Total items should not be mistaken for total amount
    @Test
    fun testReceiptParser_IgnoresItemCountInTotal() {
        val receiptText = """
            Daily Mart
            Milk 1L 95.00
            Bread 65.00
            Total Items: 5
            Net Payable: 160.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertEquals(160.00, parsed.totalAmount ?: 0.0, 0.01)
    }

    // 13. Receipt OCR: Multi-line label and value
    @Test
    fun testReceiptParser_NextLineTotalAmount() {
        val receiptText = """
            Tasty Treat Bakery
            Pastry              140.00
            Coffee              110.00
            GRAND TOTAL
            ৳ 250.00
            Thank You
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertEquals(250.00, parsed.totalAmount ?: 0.0, 0.01)
    }

    // 14. Receipt OCR: Spatial bounding-box row reconstruction joins left label and right price
    @Test
    fun testReceiptOcrManager_SpatialRowReconstruction() {
        val positionedLines = listOf(
            ReceiptOcrManager.PositionedLine(text = "Shwapno", left = 40, top = 20, bottom = 50),
            ReceiptOcrManager.PositionedLine(text = "Sub Total", left = 40, top = 200, bottom = 230),
            ReceiptOcrManager.PositionedLine(text = "Grand Total", left = 40, top = 260, bottom = 290),
            ReceiptOcrManager.PositionedLine(text = "Cash Paid", left = 40, top = 320, bottom = 350),
            ReceiptOcrManager.PositionedLine(text = "400.00", left = 420, top = 202, bottom = 231),
            ReceiptOcrManager.PositionedLine(text = "420.00", left = 420, top = 261, bottom = 289),
            ReceiptOcrManager.PositionedLine(text = "500.00", left = 420, top = 321, bottom = 351)
        )

        val reconstructedText = ReceiptOcrManager.reconstructLinesFromBoxes(positionedLines).joinToString("\n")
        val parsed = ReceiptParser.parse(reconstructedText, sampleAccounts, sampleCategories)

        assertEquals(420.00, parsed.totalAmount ?: 0.0, 0.01)
    }

    // 15. Receipt OCR: Ambiguous receipt with conflicting totals flags ambiguity and surfaces candidates
    @Test
    fun testReceiptParser_AmbiguousTotals_SurfacesCandidateAmounts() {
        val receiptText = """
            City Cafe
            Burger              250.00
            Fries               100.00
            Grand Total:        350.00
            Amount Due:         315.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertTrue(parsed.isAmountAmbiguous)
        assertEquals(315.00, parsed.totalAmount ?: 0.0, 0.01)
        assertTrue(parsed.candidateAmounts.size >= 2)
        assertTrue(parsed.candidateAmounts.any { it.amount == 350.00 })
        assertTrue(parsed.candidateAmounts.any { it.amount == 315.00 })
    }

    // 16. Receipt OCR: Unlabeled numbers do not guess largest number as total
    @Test
    fun testReceiptParser_UnlabeledReceipt_DoesNotGuessLargestNumberAsTotal() {
        val receiptText = """
            Local Store
            Order # 88421
            Item A 120.00
            Item B 80.00
            200.00
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText, sampleAccounts, sampleCategories)

        assertNull(parsed.totalAmount)
        assertTrue(parsed.isAmountAmbiguous)
        assertTrue(parsed.candidateAmounts.any { it.amount == 200.00 })
        assertFalse(parsed.candidateAmounts.any { it.amount == 88421.0 })
    }

    // 17. Receipt OCR: Unreadable / Blank receipt does not invent data
    @Test
    fun testReceiptParser_UnreadableReceipt_LeavesFieldsNull() {
        val blurredText = "x#9! @pL &*%"
        val parsed = ReceiptParser.parse(blurredText, sampleAccounts, sampleCategories)

        assertTrue(parsed.hasText)
        assertNull(parsed.totalAmount)
        assertNull(parsed.dateMillis)
        assertNull(parsed.suggestedCategory)
    }

    // 18. Receipt OCR: Completely empty receipt returns all nulls
    @Test
    fun testReceiptParser_EmptyReceipt_AllNulls() {
        val emptyText = "   \n  "
        val parsed = ReceiptParser.parse(emptyText, sampleAccounts, sampleCategories)

        assertFalse(parsed.hasText)
        assertNull(parsed.totalAmount)
        assertNull(parsed.merchantName)
        assertNull(parsed.dateMillis)
        assertNull(parsed.suggestedCategory)
    }
}
