package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.model.AccountType
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.MainViewModel
import com.example.ui.screens.plan.calculateDebtRemaining
import com.example.ui.screens.plan.isDebtAdditionTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebtTopUpRegressionTest {

    private suspend fun awaitUntil(timeoutMs: Long = 3000L, predicate: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            delay(25L)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `lend more via saveDebtTransaction increases total principal and remaining balance instead of marking paid`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.repository.eraseAllData()

        val wallet = AccountEntity(
            id = "acc_cash_test",
            name = "Cash Wallet",
            type = AccountType.CASH,
            balance = 20000.0
        )
        viewModel.repository.insertAccount(wallet)

        val initialDebt = DebtEntity(
            id = "debt_lend_topup",
            personName = "Nabila",
            amount = 5000.0,
            totalAmount = 5000.0,
            paidAmount = 0.0,
            type = DebtType.LENT,
            isSettled = false
        )
        viewModel.repository.insertDebt(initialDebt)

        // Action launched by DebtDetailsScreen: "Lend More" (with space!)
        viewModel.saveDebtTransaction(
            debt = initialDebt,
            type = "Lend More",
            amount = 3000.0,
            account = wallet,
            notes = "Second loan",
            customTitle = "Lent More"
        )

        awaitUntil {
            val d = viewModel.repository.getDebtById(initialDebt.id)
            val txs = viewModel.repository.getTransactionsForDebt(initialDebt.id)
            d != null && d.totalAmount >= 8000.0 && txs.isNotEmpty()
        }

        val updated = viewModel.repository.getDebtById(initialDebt.id)
        assertNotNull(updated)
        assertEquals(8000.0, updated!!.totalAmount, 0.001)
        assertEquals(0.0, updated.paidAmount, 0.001)
        assertEquals(8000.0, updated.amount, 0.001)
        assertEquals(8000.0, calculateDebtRemaining(updated), 0.001)
        assertFalse("Lend More must NOT mark debt as settled", updated.isSettled)

        // Verify linked transaction was logged as addition (+), not repayment
        val txs = viewModel.repository.getTransactionsForDebt(initialDebt.id)
        assertEquals(1, txs.size)
        val topUpTx = txs.first()
        assertEquals(TransactionType.LEND, topUpTx.type)
        assertEquals("Lend More", topUpTx.subcategory)
        assertEquals(3000.0, topUpTx.amount, 0.001)
        assertTrue(isDebtAdditionTransaction(topUpTx, isPayable = false))

        // Verify selected wallet decreased by 3,000 (20,000 -> 17,000)
        awaitUntil {
            val acc = viewModel.repository.accounts.first().firstOrNull { it.id == wallet.id }
            (acc?.balance ?: 0.0) <= 17000.001
        }
        val updatedWallet = viewModel.repository.accounts.first().firstOrNull { it.id == wallet.id }
        assertNotNull(updatedWallet)
        assertEquals(17000.0, updatedWallet!!.balance, 0.001)

        // Verify deleting the Lend More transaction reverts debt total & wallet balance
        viewModel.deleteTransaction(topUpTx.id)
        awaitUntil {
            val d = viewModel.repository.getDebtById(initialDebt.id)
            val acc = viewModel.repository.accounts.first().firstOrNull { it.id == wallet.id }
            d != null && d.totalAmount <= 5000.001 && (acc?.balance ?: 0.0) >= 19999.999
        }
        val afterDeleteDebt = viewModel.repository.getDebtById(initialDebt.id)!!
        assertEquals(5000.0, afterDeleteDebt.totalAmount, 0.001)
        assertEquals(5000.0, afterDeleteDebt.amount, 0.001)
        assertEquals(0.0, afterDeleteDebt.paidAmount, 0.001)
        val restoredWallet = viewModel.repository.accounts.first().firstOrNull { it.id == wallet.id }!!
        assertEquals(20000.0, restoredWallet.balance, 0.001)
    }

    @Test
    fun `lend more on partially paid or settled loan increases remaining and un-settles debt`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.repository.eraseAllData()

        val partiallyPaid = DebtEntity(
            id = "debt_partial_lend",
            personName = "Rafi",
            amount = 2000.0,
            totalAmount = 5000.0,
            paidAmount = 3000.0,
            type = DebtType.LENT,
            isSettled = false
        )
        viewModel.repository.insertDebt(partiallyPaid)

        viewModel.saveDebtTransaction(
            debt = partiallyPaid,
            type = "Lend More",
            amount = 4000.0,
            account = null,
            customTitle = "Lent More"
        )

        awaitUntil {
            val d = viewModel.repository.getDebtById(partiallyPaid.id)
            d != null && d.totalAmount >= 9000.0
        }

        val afterPartialTopUp = viewModel.repository.getDebtById(partiallyPaid.id)!!
        assertEquals(9000.0, afterPartialTopUp.totalAmount, 0.001)
        assertEquals(3000.0, afterPartialTopUp.paidAmount, 0.001)
        assertEquals(6000.0, afterPartialTopUp.amount, 0.001)
        assertEquals(6000.0, calculateDebtRemaining(afterPartialTopUp), 0.001)
        assertFalse(afterPartialTopUp.isSettled)

        // Now test topping up an already settled debt -> must reopen (isSettled = false)
        val settledDebt = DebtEntity(
            id = "debt_settled_lend",
            personName = "Karim",
            amount = 0.0,
            totalAmount = 4000.0,
            paidAmount = 4000.0,
            type = DebtType.LENT,
            isSettled = true
        )
        viewModel.repository.insertDebt(settledDebt)

        viewModel.saveDebtTransaction(
            debt = settledDebt,
            type = "Lend More",
            amount = 1500.0,
            account = null
        )

        awaitUntil {
            val d = viewModel.repository.getDebtById(settledDebt.id)
            d != null && d.totalAmount >= 5500.0
        }

        val reopened = viewModel.repository.getDebtById(settledDebt.id)!!
        assertEquals(5500.0, reopened.totalAmount, 0.001)
        assertEquals(4000.0, reopened.paidAmount, 0.001)
        assertEquals(1500.0, reopened.amount, 0.001)
        assertEquals(1500.0, calculateDebtRemaining(reopened), 0.001)
        assertFalse("Topping up a settled debt must reopen it", reopened.isSettled)
    }

    @Test
    fun `borrow more and add interest increase payable balance while payment reduces and settles`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        viewModel.repository.eraseAllData()

        val bank = AccountEntity(
            id = "acc_bank_test",
            name = "Bank",
            type = AccountType.BANK,
            balance = 10000.0
        )
        viewModel.repository.insertAccount(bank)

        val borrowed = DebtEntity(
            id = "debt_borrow_topup",
            personName = "Arian",
            amount = 10000.0,
            totalAmount = 10000.0,
            paidAmount = 0.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        viewModel.repository.insertDebt(borrowed)

        // 1. Borrow More 5,000 -> total 15,000, remaining 15,000, bank balance 15,000
        viewModel.saveDebtTransaction(
            debt = borrowed,
            type = "Borrow More",
            amount = 5000.0,
            account = bank
        )
        awaitUntil {
            val d = viewModel.repository.getDebtById(borrowed.id)
            d != null && d.totalAmount >= 15000.0
        }

        val afterBorrowMore = viewModel.repository.getDebtById(borrowed.id)!!
        assertEquals(15000.0, afterBorrowMore.totalAmount, 0.001)
        assertEquals(15000.0, afterBorrowMore.amount, 0.001)
        assertEquals(0.0, afterBorrowMore.paidAmount, 0.001)
        assertFalse(afterBorrowMore.isSettled)

        // 2. Add Interest 1,000 -> total 16,000, remaining 16,000
        viewModel.saveDebtTransaction(
            debt = afterBorrowMore,
            type = "Add Interest",
            amount = 1000.0,
            account = null
        )
        awaitUntil {
            val d = viewModel.repository.getDebtById(borrowed.id)
            d != null && d.totalAmount >= 16000.0
        }

        val afterInterest = viewModel.repository.getDebtById(borrowed.id)!!
        assertEquals(16000.0, afterInterest.totalAmount, 0.001)
        assertEquals(16000.0, afterInterest.amount, 0.001)
        assertEquals(0.0, afterInterest.paidAmount, 0.001)
        assertFalse(afterInterest.isSettled)

        // 3. Record full payment 16,000 -> remaining 0, isSettled = true
        viewModel.saveDebtTransaction(
            debt = afterInterest,
            type = "Record Payment",
            amount = 16000.0,
            account = bank
        )
        awaitUntil {
            val d = viewModel.repository.getDebtById(borrowed.id)
            d != null && d.isSettled
        }

        val settled = viewModel.repository.getDebtById(borrowed.id)!!
        assertEquals(16000.0, settled.totalAmount, 0.001)
        assertEquals(16000.0, settled.paidAmount, 0.001)
        assertEquals(0.0, settled.amount, 0.001)
        assertEquals(0.0, calculateDebtRemaining(settled), 0.001)
        assertTrue(settled.isSettled)

        val txs = viewModel.repository.getTransactionsForDebt(borrowed.id)
        assertEquals(3, txs.size)
        assertEquals(2, txs.count { isDebtAdditionTransaction(it, isPayable = true) })
        assertEquals(1, txs.count { !isDebtAdditionTransaction(it, isPayable = true) })
    }
}
