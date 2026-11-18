package com.example

import com.example.data.local.entity.DebtEntity
import com.example.data.model.DebtType
import com.example.ui.screens.plan.calculateDebtRemaining
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageDebtsTotalsTest {

    @Test
    fun `calculateDebtRemaining handles full, partial, and settled debts accurately`() {
        // 1. Unsettled debt with no payment made
        val freshDebt = DebtEntity(
            id = "d1",
            personName = "Snigdha",
            amount = 15000.0,
            totalAmount = 15000.0,
            paidAmount = 0.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        assertEquals(15000.0, calculateDebtRemaining(freshDebt), 0.001)

        // 2. Partially paid debt: original 20,000, paid 6,000 -> remaining 14,000
        val partialDebt = DebtEntity(
            id = "d2",
            personName = "Walid",
            amount = 14000.0,
            totalAmount = 20000.0,
            paidAmount = 6000.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        assertEquals(14000.0, calculateDebtRemaining(partialDebt), 0.001)

        // 3. Settled debt with isSettled = true -> must contribute 0.0
        val settledDebt = DebtEntity(
            id = "d3",
            personName = "Snigdha",
            amount = 2000.0, // original or remaining field
            totalAmount = 2000.0,
            paidAmount = 2000.0,
            type = DebtType.BORROWED,
            isSettled = true
        )
        assertEquals(0.0, calculateDebtRemaining(settledDebt), 0.001)

        // 4. Settled debt where amount is 0.0 and paid equals total
        val fullyPaidDebt = DebtEntity(
            id = "d4",
            personName = "Joney Vai",
            amount = 0.0,
            totalAmount = 5000.0,
            paidAmount = 5000.0,
            type = DebtType.BORROWED,
            isSettled = true
        )
        assertEquals(0.0, calculateDebtRemaining(fullyPaidDebt), 0.001)
    }

    @Test
    fun `totalBorrowed and totalLent exclude settled debts regardless of visibility toggle`() {
        val payableUnsettled1 = DebtEntity(
            id = "b1",
            personName = "Arian",
            amount = 10000.0,
            totalAmount = 10000.0,
            paidAmount = 2000.0, // partially paid: 8,000 left
            type = DebtType.BORROWED,
            isSettled = false
        )
        val payableUnsettled2 = DebtEntity(
            id = "b2",
            personName = "Karim",
            amount = 5000.0,
            totalAmount = 5000.0,
            paidAmount = 0.0, // full unpaid: 5,000 left
            type = DebtType.BORROWED,
            isSettled = false
        )
        val payableSettled = DebtEntity(
            id = "b3",
            personName = "Farhan",
            amount = 8000.0,
            totalAmount = 8000.0,
            paidAmount = 8000.0,
            type = DebtType.BORROWED,
            isSettled = true
        )

        val lentUnsettled = DebtEntity(
            id = "l1",
            personName = "Sadia",
            amount = 25000.0,
            totalAmount = 25000.0,
            paidAmount = 10000.0, // partially paid: 15,000 left
            type = DebtType.LENT,
            isSettled = false
        )
        val lentSettled = DebtEntity(
            id = "l2",
            personName = "Rafiq",
            amount = 12000.0,
            totalAmount = 12000.0,
            paidAmount = 12000.0,
            type = DebtType.LENT,
            isSettled = true
        )

        val allDebts = listOf(payableUnsettled1, payableUnsettled2, payableSettled, lentUnsettled, lentSettled)

        // When showSettledDebts = false
        var showSettled = false
        var filteredBorrowed = allDebts.filter { it.type == DebtType.BORROWED && (showSettled || !it.isSettled) }
        var filteredLent = allDebts.filter { it.type == DebtType.LENT && (showSettled || !it.isSettled) }

        assertEquals(2, filteredBorrowed.size)
        assertEquals(1, filteredLent.size)

        var totalBorrowed = allDebts.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { calculateDebtRemaining(it) }
        var totalLent = allDebts.filter { it.type == DebtType.LENT && !it.isSettled }.sumOf { calculateDebtRemaining(it) }

        // b1: 8,000 + b2: 5,000 = 13,000. Settled b3 (8,000) excluded!
        assertEquals(13000.0, totalBorrowed, 0.001)
        // l1: 15,000. Settled l2 (12,000) excluded!
        assertEquals(15000.0, totalLent, 0.001)

        // When showSettledDebts = true (settled debts become visible in the list)
        showSettled = true
        filteredBorrowed = allDebts.filter { it.type == DebtType.BORROWED && (showSettled || !it.isSettled) }
        filteredLent = allDebts.filter { it.type == DebtType.LENT && (showSettled || !it.isSettled) }

        assertEquals(3, filteredBorrowed.size)
        assertEquals(2, filteredLent.size)

        // Totals MUST remain unchanged and exclude settled debts!
        totalBorrowed = allDebts.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { calculateDebtRemaining(it) }
        totalLent = allDebts.filter { it.type == DebtType.LENT && !it.isSettled }.sumOf { calculateDebtRemaining(it) }

        assertEquals(13000.0, totalBorrowed, 0.001)
        assertEquals(15000.0, totalLent, 0.001)
    }

    @Test
    fun `totals update dynamically when a debt is partially paid, settled, or deleted`() {
        val initialDebt = DebtEntity(
            id = "d1",
            personName = "Arian",
            amount = 10000.0,
            totalAmount = 10000.0,
            paidAmount = 0.0,
            type = DebtType.BORROWED,
            isSettled = false
        )
        val debtList = mutableListOf(initialDebt)

        var total = debtList.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { calculateDebtRemaining(it) }
        assertEquals(10000.0, total, 0.001)

        // 1. Partial payment of 4,000
        val partiallyPaid = initialDebt.copy(
            paidAmount = 4000.0,
            amount = 6000.0
        )
        debtList[0] = partiallyPaid
        total = debtList.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { calculateDebtRemaining(it) }
        assertEquals(6000.0, total, 0.001)

        // 2. Fully settled
        val settled = partiallyPaid.copy(
            paidAmount = 10000.0,
            amount = 0.0,
            isSettled = true
        )
        debtList[0] = settled
        total = debtList.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { calculateDebtRemaining(it) }
        assertEquals(0.0, total, 0.001)

        // 3. Deleted
        debtList.removeAt(0)
        total = debtList.filter { it.type == DebtType.BORROWED && !it.isSettled }.sumOf { calculateDebtRemaining(it) }
        assertEquals(0.0, total, 0.001)
    }
}
