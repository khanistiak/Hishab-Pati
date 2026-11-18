package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirestoreWalletRepository
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreWalletRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAccount_authenticatedUser_successAndEmitsViaObserve() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWalletRepository(firestore)

        val accountId = "acc_" + UUID.randomUUID().toString()
        val account = AccountEntity(
            id = accountId,
            name = "City Bank Savings",
            type = AccountType.BANK,
            balance = 50000.0,
            institution = "City Bank"
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveAccount(account)
        }

        val accounts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeAccounts().first { list -> list.any { it.id == accountId } }
        }
        val found = accounts.find { it.id == accountId }
        assertNotNull(found)
        assertEquals("City Bank Savings", found?.name)
        assertEquals(50000.0, found?.balance ?: 0.0, 0.001)
    }

    @Test
    fun saveTransaction_authenticatedUser_successAndEmitsViaObserve() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWalletRepository(firestore)

        val txId = "tx_" + UUID.randomUUID().toString()
        val tx = TransactionEntity(
            id = txId,
            type = TransactionType.EXPENSE,
            amount = 1200.0,
            title = "Monthly Electricity",
            category = "Rent & Utilities",
            accountId = "acc_cash",
            timestamp = System.currentTimeMillis()
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveTransaction(tx)
        }

        val transactions = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { list -> list.any { it.id == txId } }
        }
        val found = transactions.find { it.id == txId }
        assertNotNull(found)
        assertEquals("Monthly Electricity", found?.title)
        assertEquals(1200.0, found?.amount ?: 0.0, 0.001)
    }

    @Test
    fun unauthenticatedUser_throwsIllegalStateException() = runBlocking {
        auth.signOut()
        val repository = FirestoreWalletRepository(firestore)

        val account = AccountEntity(
            id = "acc_unauth",
            name = "Test",
            type = AccountType.CASH,
            balance = 100.0
        )

        var thrown = false
        try {
            repository.saveAccount(account)
        } catch (e: IllegalStateException) {
            thrown = true
        }
        assertTrue("Expected IllegalStateException for unauthenticated caller", thrown)
    }

    @Test
    fun crossUserAccess_directFirestoreQuery_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWalletRepository(firestore)

        val accountId = "acc_" + UUID.randomUUID().toString()
        val account = AccountEntity(
            id = accountId,
            name = "Alice Secret Account",
            type = AccountType.CASH,
            balance = 9999.0
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveAccount(account)
        }

        // Now sign in as Bob and attempt to read Alice's account document directly
        signInTestUser(BOB_EMAIL)
        var permissionDenied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("accounts").document(accountId).get().await()
            }
        } catch (e: Exception) {
            val rootException = e.unwrapFirestoreException()
            if (rootException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                permissionDenied = true
            }
        }
        assertTrue("Bob reading Alice's accounts should trigger PERMISSION_DENIED", permissionDenied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_wallet@test.com"
        const val BOB_EMAIL = "bob_wallet@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
