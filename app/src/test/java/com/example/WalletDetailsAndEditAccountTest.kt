package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import com.example.data.repository.WalletRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.accounts.CardColorPresets
import com.example.ui.screens.accounts.ComprehensiveColorPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WalletDetailsAndEditAccountTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: WalletRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(
                app,
                com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.example.test")
                    .setApiKey("fakeApiKey1234567890")
                    .setProjectId("test-project")
                    .build()
            )
        }
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WalletRepository(db)
        viewModel = MainViewModel(app)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `includeInTotal exclusion decreases total assets and net worth`() {
        val acc1 = AccountEntity(
            id = "acc_bank",
            name = "Bank PLC",
            type = AccountType.BANK,
            balance = 10000.0,
            includeInTotal = true,
            isHidden = false
        )
        val acc2 = AccountEntity(
            id = "acc_cash",
            name = "Cash Wallet",
            type = AccountType.CASH,
            balance = 5000.0,
            includeInTotal = false,
            isHidden = false
        )

        val netWorthWithExclusion = viewModel.calculateNetWorth(listOf(acc1, acc2), emptyList())
        assertEquals(10000.0, netWorthWithExclusion.totalAssets, 0.01)
        assertEquals(10000.0, netWorthWithExclusion.netWorth, 0.01)

        val acc2Included = acc2.copy(includeInTotal = true)
        val netWorthIncluded = viewModel.calculateNetWorth(listOf(acc1, acc2Included), emptyList())
        assertEquals(15000.0, netWorthIncluded.totalAssets, 0.01)
        assertEquals(15000.0, netWorthIncluded.netWorth, 0.01)
    }

    @Test
    fun `adjust balance with transaction recording ON creates linked transaction`() = runBlocking {
        val account = AccountEntity(
            id = "acc_test_1",
            name = "City Bank",
            type = AccountType.BANK,
            balance = 5000.0,
            includeInTotal = true,
            isHidden = false
        )
        db.accountDao().insertAccount(account)

        // Adjust balance upward by 2500 (5000 -> 7500) with recordAsTransaction = true
        repository.adjustAccountBalance("acc_test_1", 7500.0, recordAsTransaction = true)

        val updatedAccount = db.accountDao().getAccountById("acc_test_1")
        assertNotNull(updatedAccount)
        assertEquals(7500.0, updatedAccount!!.balance, 0.01)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals("acc_test_1", tx.accountId)
        assertEquals(2500.0, tx.amount, 0.01)
        assertEquals(TransactionType.INCOME, tx.type)
        assertTrue(tx.title.contains("Balance Adjustment"))
    }

    @Test
    fun `adjust balance with transaction recording OFF updates balance without creating transaction`() = runBlocking {
        val account = AccountEntity(
            id = "acc_test_2",
            name = "bKash Personal",
            type = AccountType.MFS,
            balance = 8000.0,
            includeInTotal = true,
            isHidden = false
        )
        db.accountDao().insertAccount(account)

        // Adjust balance downward to 6000 with recordAsTransaction = false
        repository.adjustAccountBalance("acc_test_2", 6000.0, recordAsTransaction = false)

        val updatedAccount = db.accountDao().getAccountById("acc_test_2")
        assertNotNull(updatedAccount)
        assertEquals(6000.0, updatedAccount!!.balance, 0.01)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(0, transactions.size)
    }

    @Test
    fun `archive and restore wallet flow preserves balances and history`() = runBlocking {
        val account = AccountEntity(
            id = "acc_archive_test",
            name = "Old Wallet",
            type = AccountType.CASH,
            balance = 3200.0,
            includeInTotal = true,
            isHidden = false
        )
        db.accountDao().insertAccount(account)

        // Archive wallet
        repository.setAccountArchived("acc_archive_test", archived = true)
        val archived = db.accountDao().getAccountById("acc_archive_test")
        assertNotNull(archived)
        assertTrue(archived!!.isHidden)
        assertEquals(3200.0, archived.balance, 0.01)

        // Net worth calculation excludes hidden accounts
        val netWorth = viewModel.calculateNetWorth(listOf(archived), emptyList())
        assertEquals(0.0, netWorth.totalAssets, 0.01)

        // Unarchive wallet
        repository.setAccountArchived("acc_archive_test", archived = false)
        val restored = db.accountDao().getAccountById("acc_archive_test")
        assertNotNull(restored)
        assertFalse(restored!!.isHidden)
        assertEquals(3200.0, restored.balance, 0.01)

        val netWorthRestored = viewModel.calculateNetWorth(listOf(restored), emptyList())
        assertEquals(3200.0, netWorthRestored.totalAssets, 0.01)
    }

    @Test
    fun `color palette contains full curated selection with valid hex codes`() {
        assertTrue(ComprehensiveColorPalette.size >= 28)
        ComprehensiveColorPalette.forEach { preset ->
            assertTrue(preset.name.isNotBlank())
            assertTrue(preset.hex.startsWith("#"))
            assertEquals(7, preset.hex.length)
        }
        assertTrue(CardColorPresets.size >= 10)
    }
}
