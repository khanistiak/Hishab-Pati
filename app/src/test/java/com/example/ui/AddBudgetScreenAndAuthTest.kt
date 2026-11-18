package com.example.ui

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.ui.auth.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddBudgetScreenAndAuthTest {

    @Test
    fun testGoogleSignIn_ResolvedWebClientIdIsValidAndNotPlaceholder() {
        val googleServicesFile = File("google-services.json")
        val content = if (googleServicesFile.exists()) googleServicesFile.readText() else File("app/google-services.json").readText()

        assertTrue("Should contain provisioned package", content.contains("com.istiak.hishabpati"))
        assertTrue("Should contain web client ID", content.contains("1070535347548-3q3ue02be4rj63qmsau99fgj770m0jdp.apps.googleusercontent.com"))
        assertFalse("Should not have placeholder client ID", content.contains("examplewebclientid"))
        assertFalse("Should not have remixed project ID", content.contains("remixed-project-id"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val resolvedClientId = AuthManager.getWebClientId(context)
        assertNotNull("Resolved default_web_client_id must not be null", resolvedClientId)
        assertEquals(
            context.getString(R.string.default_web_client_id),
            resolvedClientId
        )
        assertTrue("Resolved client ID must be a valid Google OAuth client ID", resolvedClientId!!.endsWith(".apps.googleusercontent.com"))
    }

    @Test
    fun testGoogleServicesJson_ConfiguredForHishabPatiPackage() {
        val googleServicesFile = File("google-services.json")
        if (googleServicesFile.exists()) {
            val content = googleServicesFile.readText()
            assertTrue("google-services.json should contain com.istiak.hishabpati", content.contains("com.istiak.hishabpati"))
            assertTrue("google-services.json should contain project number 1070535347548", content.contains("1070535347548"))
            assertTrue("google-services.json should contain web client ID 3q3ue02be4rj63qmsau99fgj770m0jdp", content.contains("3q3ue02be4rj63qmsau99fgj770m0jdp"))
            assertFalse("google-services.json should not contain placeholder remixed project ID", content.contains("remixed-project-id"))
        }
    }

    @Test
    fun testDeveloperConfigurationErrorDetection_DetectsReleaseSha1MismatchInCancellation() {
        val playServicesMismatchCancel = GetCredentialCancellationException(
            "[28444] Developer console is not set up correctly."
        )
        assertTrue(
            "Should detect [28444] developer console error inside GetCredentialCancellationException",
            AuthManager.isDeveloperConfigurationError(playServicesMismatchCancel)
        )

        val status10Error = GetCredentialUnknownException("10: DEVELOPER_ERROR")
        assertTrue(
            "Should detect status 10 DEVELOPER_ERROR",
            AuthManager.isDeveloperConfigurationError(status10Error)
        )

        val normalUserCancel = GetCredentialCancellationException("activity is cancelled by the user.")
        assertFalse(
            "Standard user dismissal should not be flagged as developer configuration error",
            AuthManager.isDeveloperConfigurationError(normalUserCancel)
        )
    }

    @Test
    fun testAttemptAutoSignIn_DoesNotTriggerPasskeyOrCredentialRequestWhenUnauthenticated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val credentialManager = CredentialManager.create(context)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var unauthenticatedCalled = false
        var successCalled = false

        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { successCalled = true },
            onUnauthenticated = { unauthenticatedCalled = true },
            scope = scope
        )

        assertTrue("Should immediately report unauthenticated when no Firebase session exists", unauthenticatedCalled)
        assertFalse("Should not report success when no Firebase session exists", successCalled)
        scope.cancel()
    }

    @Test
    fun testOnGoogleSignInClicked_ResetsLoadingStateWhenNoActivityContextAvailable() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val credentialManager = CredentialManager.create(appContext)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var isLoading = true
        var receivedError: String? = null

        AuthManager.onGoogleSignInClicked(
            context = appContext,
            credentialManager = credentialManager,
            onAuthSuccess = { isLoading = false },
            onAuthError = { err ->
                isLoading = false
                receivedError = err
            },
            scope = scope,
            onAuthCancelled = { isLoading = false }
        )

        assertFalse("Loading state must reset when Activity window is unavailable", isLoading)
        assertNotNull("Error message must be provided when Activity context is missing", receivedError)
        scope.cancel()
    }
}

