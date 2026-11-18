package com.example.ui

import com.example.ui.auth.AuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSystemInsetsAndAuthTest {

    @Test
    fun testFallbackWebClientIdIsValid() {
        val clientId = AuthManager.FALLBACK_WEB_CLIENT_ID
        assertNotNull("Web Client ID must not be null", clientId)
        assertTrue("Web Client ID must not be blank", clientId.isNotBlank())
        assertTrue(
            "Web Client ID should be a valid google OAuth client ID",
            clientId.endsWith(".apps.googleusercontent.com")
        )
        assertEquals(
            "Web Client ID should match project configuration",
            "1070535347548-3q3ue02be4rj63qmsau99fgj770m0jdp.apps.googleusercontent.com",
            clientId
        )
    }

    @Test
    fun testDeveloperConfigurationErrorDetection() {
        val errorStatus10 = RuntimeException("10: Developer console is not set up correctly")
        assertTrue(AuthManager.isDeveloperConfigurationError(errorStatus10))

        val error28444 = RuntimeException("[28444] Play services developer error")
        assertTrue(AuthManager.isDeveloperConfigurationError(error28444))

        val errorStatus16 = RuntimeException("16: Cannot find matching credential")
        assertTrue(AuthManager.isDeveloperConfigurationError(errorStatus16))

        val errorStatus12500 = RuntimeException("12500: Sign in failed")
        assertTrue(AuthManager.isDeveloperConfigurationError(errorStatus12500))

        val regularError = RuntimeException("Network connection lost")
        assertFalse(AuthManager.isDeveloperConfigurationError(regularError))
    }
}
