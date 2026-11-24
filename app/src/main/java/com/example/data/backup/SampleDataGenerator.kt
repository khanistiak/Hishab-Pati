package com.example.data.backup

import com.example.data.local.InitialData
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.BackgroundTextureOption
import com.example.data.model.DebtType
import com.example.data.model.ThemeColorOption
import com.example.data.model.ThemeMode
import com.example.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SampleDataGenerator {

    fun generateSampleJsonTemplate(referenceTimeMillis: Long = 1795391999000L): String {
        val root = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val refCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 22, 23, 59, 59)
            set(Calendar.MILLISECOND, 0)
        }
        val effectiveRefMillis = if (referenceTimeMillis > 0) referenceTimeMillis else refCal.timeInMillis

        fun tsForDate(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
            return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        root.put("appName", "Hishab Pati")
        root.put("schemaVersion", BackupMigrationEngine.CURRENT_SCHEMA_VERSION)
        root.put("backupDate", isoFormat.format(Date(effectiveRefMillis)))
        root.put("currency", "BDT")
        root.put(
            "_comment",
            "SAMPLE TEMPLATE: Realistic 3-month fictional data for September 2026, October 2026, and November 2026 (up to Nov 22) " +
                "matching the backup schema. Includes reconciled opening/closing balances across Cash, bKash/Nagad MFS, Bank Debit Card, and Credit Card accounts, " +
                "income, varied expenses with categories/subcategories & recurring flags, account-to-account transfers, monthly budgets, savings goals, and debts."
        )

        // 1. Settings matching AppSettingsEntity & JsonBackupManager (Royal Indigo theme intact, no legacy/non-indigo accent fields)
        val defaultSettings = AppSettingsEntity(
            userName = "Demo User",
            country = "Bangladesh",
            currencyCode = "BDT",
            currencySymbol = "৳",
            themeMode = ThemeMode.AUTO,
            themeColor = ThemeColorOption.ROYAL_INDIGO,
            backgroundTexture = BackgroundTextureOption.AURORA
        )
        val settingsObj = JSONObject().apply {
            put("userName", defaultSettings.userName)
            put("country", defaultSettings.country)
            put("currencyCode", defaultSettings.currencyCode)
            put("currencySymbol", defaultSettings.currencySymbol)
            put("language", defaultSettings.language.name)
            put("themeMode", defaultSettings.themeMode.name)
            put("themeColor", defaultSettings.themeColor.name)
            put("backgroundTexture", defaultSettings.backgroundTexture.name)
            put("appLockEnabled", defaultSettings.appLockEnabled)
            put("screenshotProtectionEnabled", defaultSettings.screenshotProtectionEnabled)
            put("notificationsEnabled", defaultSettings.notificationsEnabled)
        }
        root.put("settings", settingsObj)

        // 2. Default Categories & Subcategories directly from InitialData
        val (categories, subcategories) = InitialData.getDefaultCategories()

        // 3. Transactions spanning September 2026, October 2026, and November 2026 (up to Nov 22)
        val transactions = listOf(
            // ==================== SEPTEMBER 2026 ====================
            TransactionEntity(
                id = "tx_sep_salary",
                type = TransactionType.INCOME,
                amount = 75000.0,
                title = "Monthly Corporate Salary (Sep)",
                category = "Salary",
                subcategory = "Monthly Salary",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 1, 9, 0),
                notes = "September 2026 salary deposit",
                isRecurring = true,
                payeeOrPayer = "Tech Innovations Ltd."
            ),
            TransactionEntity(
                id = "tx_sep_rent",
                type = TransactionType.EXPENSE,
                amount = 18000.0,
                title = "Apartment Rent (Sep)",
                category = "Rent & Utilities",
                subcategory = "House Rent",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 2, 10, 0),
                notes = "September 2026 house rent payment",
                isRecurring = true,
                payeeOrPayer = "Skyline Properties"
            ),
            TransactionEntity(
                id = "tx_sep_internet",
                type = TransactionType.EXPENSE,
                amount = 1200.0,
                title = "Broadband Internet Bill (Sep)",
                category = "Rent & Utilities",
                subcategory = "Internet",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 5, 11, 30),
                notes = "September 2026 internet bill",
                isRecurring = true,
                payeeOrPayer = "AmberIT"
            ),
            TransactionEntity(
                id = "tx_sep_atm_withdraw",
                type = TransactionType.TRANSFER,
                amount = 10000.0,
                title = "ATM Cash Withdrawal",
                category = "Bank Transfer",
                subcategory = "ATM Withdrawal",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_cash",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 3, 14, 0),
                notes = "Cash withdrawal for daily expenses",
                payeeOrPayer = "City Bank ATM"
            ),
            TransactionEntity(
                id = "tx_sep_groceries_1",
                type = TransactionType.EXPENSE,
                amount = 4500.0,
                title = "Superstore Groceries",
                category = "Groceries",
                subcategory = "Pantry Staples",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 6, 16, 0),
                notes = "Monthly pantry restock",
                payeeOrPayer = "Shwapno Superstore"
            ),
            TransactionEntity(
                id = "tx_sep_dining_1",
                type = TransactionType.EXPENSE,
                amount = 1850.0,
                title = "Weekend Family Dinner",
                category = "Food & Dining",
                subcategory = "Restaurant",
                accountId = "acc_sample_cash",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 12, 20, 0),
                notes = "Dinner at Spice Garden",
                payeeOrPayer = "Spice Garden Cafe"
            ),
            TransactionEntity(
                id = "tx_sep_shopping_clothes",
                type = TransactionType.EXPENSE,
                amount = 5500.0,
                title = "Apparel Shopping",
                category = "Shopping",
                subcategory = "Clothing",
                accountId = "acc_sample_card",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 15, 17, 30),
                notes = "Seasonal clothing purchase",
                payeeOrPayer = "Aarong"
            ),
            TransactionEntity(
                id = "tx_sep_transport",
                type = TransactionType.EXPENSE,
                amount = 2200.0,
                title = "Commute & Ride Sharing",
                category = "Transportation",
                subcategory = "Ride Sharing",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 20, 9, 30),
                notes = "Monthly ride sharing expenses",
                payeeOrPayer = "Pathao Rides"
            ),
            TransactionEntity(
                id = "tx_sep_family",
                type = TransactionType.EXPENSE,
                amount = 6000.0,
                title = "Parents Monthly Support",
                category = "Family",
                subcategory = "Parents Support",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 25, 12, 0),
                notes = "Monthly support sent to parents",
                isRecurring = true,
                payeeOrPayer = "Parents"
            ),
            TransactionEntity(
                id = "tx_sep_cc_payment",
                type = TransactionType.CC_PAYMENT,
                amount = 5500.0,
                title = "Credit Card Bill Payment",
                category = "Bank Transfer",
                subcategory = "Account to Account",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_card",
                timestamp = tsForDate(2026, Calendar.SEPTEMBER, 28, 15, 0),
                notes = "Cleared September credit card statement",
                payeeOrPayer = "Standard Chartered Bank"
            ),

            // ==================== OCTOBER 2026 ====================
            TransactionEntity(
                id = "tx_oct_salary",
                type = TransactionType.INCOME,
                amount = 75000.0,
                title = "Monthly Corporate Salary (Oct)",
                category = "Salary",
                subcategory = "Monthly Salary",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 1, 9, 0),
                notes = "October 2026 salary deposit",
                isRecurring = true,
                payeeOrPayer = "Tech Innovations Ltd."
            ),
            TransactionEntity(
                id = "tx_oct_bonus",
                type = TransactionType.INCOME,
                amount = 15000.0,
                title = "Quarterly Performance Bonus",
                category = "Bonus",
                subcategory = "Performance Bonus",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 5, 10, 0),
                notes = "Q3 performance bonus",
                payeeOrPayer = "Tech Innovations Ltd."
            ),
            TransactionEntity(
                id = "tx_oct_rent",
                type = TransactionType.EXPENSE,
                amount = 18000.0,
                title = "Apartment Rent (Oct)",
                category = "Rent & Utilities",
                subcategory = "House Rent",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 2, 10, 0),
                notes = "October 2026 house rent payment",
                isRecurring = true,
                payeeOrPayer = "Skyline Properties"
            ),
            TransactionEntity(
                id = "tx_oct_internet",
                type = TransactionType.EXPENSE,
                amount = 1200.0,
                title = "Broadband Internet Bill (Oct)",
                category = "Rent & Utilities",
                subcategory = "Internet",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 5, 11, 30),
                notes = "October 2026 internet bill",
                isRecurring = true,
                payeeOrPayer = "AmberIT"
            ),
            TransactionEntity(
                id = "tx_oct_atm_withdraw",
                type = TransactionType.TRANSFER,
                amount = 12000.0,
                title = "ATM Cash Withdrawal",
                category = "Bank Transfer",
                subcategory = "ATM Withdrawal",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_cash",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 4, 14, 0),
                notes = "Cash withdrawal for October",
                payeeOrPayer = "City Bank ATM"
            ),
            TransactionEntity(
                id = "tx_oct_groceries_1",
                type = TransactionType.EXPENSE,
                amount = 5200.0,
                title = "Superstore Groceries & Supplies",
                category = "Groceries",
                subcategory = "Pantry Staples",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 8, 16, 0),
                notes = "Monthly groceries restock",
                payeeOrPayer = "Shwapno Superstore"
            ),
            TransactionEntity(
                id = "tx_oct_shopping_elec",
                type = TransactionType.EXPENSE,
                amount = 8500.0,
                title = "Home Office Accessories",
                category = "Shopping",
                subcategory = "Electronics",
                accountId = "acc_sample_card",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 14, 15, 0),
                notes = "Keyboard and monitor stand",
                payeeOrPayer = "Gadget Galaxy"
            ),
            TransactionEntity(
                id = "tx_oct_dining",
                type = TransactionType.EXPENSE,
                amount = 2400.0,
                title = "Cafe & Coffee Outing",
                category = "Food & Dining",
                subcategory = "Cafes & Coffee",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 18, 17, 0),
                notes = "Coffee meetup with team",
                payeeOrPayer = "North End Roasters"
            ),
            TransactionEntity(
                id = "tx_oct_health",
                type = TransactionType.EXPENSE,
                amount = 2100.0,
                title = "Pharmacy Medicines",
                category = "Health & Medical",
                subcategory = "Medicines",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 22, 11, 0),
                notes = "Monthly vitamins and medicines",
                payeeOrPayer = "Lazz Pharma"
            ),
            TransactionEntity(
                id = "tx_oct_family",
                type = TransactionType.EXPENSE,
                amount = 6000.0,
                title = "Parents Monthly Support",
                category = "Family",
                subcategory = "Parents Support",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 25, 12, 0),
                notes = "Monthly support sent to parents",
                isRecurring = true,
                payeeOrPayer = "Parents"
            ),
            TransactionEntity(
                id = "tx_oct_cc_payment",
                type = TransactionType.CC_PAYMENT,
                amount = 8500.0,
                title = "Credit Card Bill Payment",
                category = "Bank Transfer",
                subcategory = "Account to Account",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_card",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 29, 15, 0),
                notes = "Cleared October credit card statement",
                payeeOrPayer = "Standard Chartered Bank"
            ),

            // ==================== NOVEMBER 2026 (up to Nov 22) ====================
            TransactionEntity(
                id = "tx_nov_salary",
                type = TransactionType.INCOME,
                amount = 75000.0,
                title = "Monthly Corporate Salary (Nov)",
                category = "Salary",
                subcategory = "Monthly Salary",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 1, 9, 0),
                notes = "November 2026 salary deposit",
                isRecurring = true,
                payeeOrPayer = "Tech Innovations Ltd."
            ),
            TransactionEntity(
                id = "tx_nov_rent",
                type = TransactionType.EXPENSE,
                amount = 18000.0,
                title = "Apartment Rent (Nov)",
                category = "Rent & Utilities",
                subcategory = "House Rent",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 2, 10, 0),
                notes = "November 2026 house rent payment",
                isRecurring = true,
                payeeOrPayer = "Skyline Properties"
            ),
            TransactionEntity(
                id = "tx_nov_internet",
                type = TransactionType.EXPENSE,
                amount = 1200.0,
                title = "Broadband Internet Bill (Nov)",
                category = "Rent & Utilities",
                subcategory = "Internet",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 5, 11, 30),
                notes = "November 2026 internet bill",
                isRecurring = true,
                payeeOrPayer = "AmberIT"
            ),
            TransactionEntity(
                id = "tx_nov_atm_withdraw",
                type = TransactionType.TRANSFER,
                amount = 9000.0,
                title = "ATM Cash Withdrawal",
                category = "Bank Transfer",
                subcategory = "ATM Withdrawal",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_cash",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 4, 14, 0),
                notes = "Cash withdrawal for November",
                payeeOrPayer = "City Bank ATM"
            ),
            TransactionEntity(
                id = "tx_nov_groceries_1",
                type = TransactionType.EXPENSE,
                amount = 4800.0,
                title = "Superstore Groceries",
                category = "Groceries",
                subcategory = "Pantry Staples",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 7, 16, 0),
                notes = "Monthly groceries restock",
                payeeOrPayer = "Shwapno Superstore"
            ),
            TransactionEntity(
                id = "tx_nov_dining",
                type = TransactionType.EXPENSE,
                amount = 2100.0,
                title = "Weekend Dinner Out",
                category = "Food & Dining",
                subcategory = "Restaurant",
                accountId = "acc_sample_cash",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 14, 20, 0),
                notes = "Dinner with friends",
                payeeOrPayer = "Chef's Table"
            ),
            TransactionEntity(
                id = "tx_nov_shopping",
                type = TransactionType.EXPENSE,
                amount = 4200.0,
                title = "Winter Wear Shopping",
                category = "Shopping",
                subcategory = "Clothing",
                accountId = "acc_sample_card",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 18, 17, 0),
                notes = "Winter jacket purchase",
                payeeOrPayer = "Yellow"
            ),
            TransactionEntity(
                id = "tx_nov_family",
                type = TransactionType.EXPENSE,
                amount = 6000.0,
                title = "Parents Monthly Support",
                category = "Family",
                subcategory = "Parents Support",
                accountId = "acc_sample_nagad",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 20, 12, 0),
                notes = "Monthly support sent to parents",
                isRecurring = true,
                payeeOrPayer = "Parents"
            ),

            // ==================== DEBTS & LOANS ====================
            TransactionEntity(
                id = "tx_sample_borrow_arian",
                type = TransactionType.BORROW,
                amount = 18000.0,
                title = "Medical Emergency Loan Borrowed",
                category = "Other Income",
                subcategory = "Miscellaneous",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 10, 10, 0),
                notes = "Sample borrowed amount from Arian Chowdhury [Debt:debt_sample_borrow_arian]",
                payeeOrPayer = "Arian Chowdhury",
                debtId = "debt_sample_borrow_arian"
            ),
            TransactionEntity(
                id = "tx_sample_repay_arian_1",
                type = TransactionType.EXPENSE,
                amount = 6000.0,
                title = "Loan 1st Installment Repayment",
                category = "Debt Repayment",
                subcategory = "Installment (EMI)",
                accountId = "acc_sample_bank",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 10, 11, 0),
                notes = "Sample partial repayment (৳6,000 of ৳18,000) to Arian Chowdhury [Debt:debt_sample_borrow_arian]",
                payeeOrPayer = "Arian Chowdhury",
                debtId = "debt_sample_borrow_arian"
            ),
            TransactionEntity(
                id = "tx_sample_lend_nabila",
                type = TransactionType.LEND,
                amount = 9000.0,
                title = "Education Support Loan Lent",
                category = "Other Expenses",
                subcategory = "General Miscellaneous",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.OCTOBER, 15, 12, 0),
                notes = "Sample loan lent to Nabila Rahman for semester fees [Debt:debt_sample_lend_nabila]",
                payeeOrPayer = "Nabila Rahman",
                debtId = "debt_sample_lend_nabila"
            ),
            TransactionEntity(
                id = "tx_sample_collect_nabila_1",
                type = TransactionType.INCOME,
                amount = 4000.0,
                title = "Loan Partial Repayment Received Back",
                category = "Other Income",
                subcategory = "Miscellaneous",
                accountId = "acc_sample_bkash",
                timestamp = tsForDate(2026, Calendar.NOVEMBER, 15, 12, 0),
                notes = "Sample partial installment (৳4,000 of ৳9,000) received from Nabila Rahman [Debt:debt_sample_lend_nabila]",
                payeeOrPayer = "Nabila Rahman",
                debtId = "debt_sample_lend_nabila"
            )
        )

        // 4. Sample Accounts across all types (Cash, bKash MFS, Nagad MFS, Bank Debit Card, Credit Card)
        val baseAccounts = listOf(
            AccountEntity(
                id = "acc_sample_cash",
                name = "Cash Wallet",
                type = AccountType.CASH,
                institution = "Physical Cash",
                openingBalance = 8000.0,
                colorHex = "#10B981",
                iconName = "payments",
                sortOrder = 0,
                notes = "Daily pocket & household cash",
                includeInTotal = true
            ),
            AccountEntity(
                id = "acc_sample_bkash",
                name = "bKash Personal",
                type = AccountType.MFS,
                institution = "bKash Limited",
                openingBalance = 18000.0,
                accountNumberLast4 = "0171",
                colorHex = "#E2136E",
                iconName = "phone_android",
                sortOrder = 1,
                notes = "Mobile wallet for daily merchant & peer payments",
                includeInTotal = true
            ),
            AccountEntity(
                id = "acc_sample_nagad",
                name = "Nagad Personal",
                type = AccountType.MFS,
                institution = "Nagad Limited",
                openingBalance = 12000.0,
                accountNumberLast4 = "0182",
                colorHex = "#F26522",
                iconName = "phone_android",
                sortOrder = 2,
                notes = "Secondary mobile wallet for utilities & cashback",
                includeInTotal = true
            ),
            AccountEntity(
                id = "acc_sample_bank",
                name = "City Bank Debit Card",
                type = AccountType.BANK,
                institution = "City Bank PLC",
                openingBalance = 45000.0,
                accountNumberLast4 = "4821",
                colorHex = "#4F46E5",
                iconName = "account_balance",
                sortOrder = 3,
                notes = "Subtype:Salary Account\nNetwork:Visa",
                includeInTotal = true
            ),
            AccountEntity(
                id = "acc_sample_card",
                name = "Standard Visa Platinum",
                type = AccountType.CREDIT_CARD,
                institution = "Standard Chartered",
                openingBalance = 0.0,
                accountNumberLast4 = "9204",
                creditLimit = 150000.0,
                billingDate = "15",
                paymentDueDate = "05",
                minPayment = 2500.0,
                colorHex = "#6366F1",
                iconName = "credit_card",
                sortOrder = 4,
                notes = "Network:Visa",
                includeInTotal = true
            )
        )

        val accounts = baseAccounts.map { acc ->
            val delta = transactions.sumOf { tx ->
                FinancialIntegrityValidator.computeSingleTxBalanceDelta(tx, acc.id)
            }
            acc.copy(balance = acc.openingBalance + delta)
        }

        // 5. Monthly Budgets for September 2026, October 2026, and November 2026
        val monthKeys = listOf("2026-09", "2026-10", "2026-11")
        val budgets = mutableListOf<BudgetEntity>()
        monthKeys.forEach { mKey ->
            budgets.add(
                BudgetEntity(
                    id = "bgt_sample_food_$mKey",
                    categoryName = "Food & Dining",
                    monthlyLimit = 6000.0,
                    monthYear = mKey,
                    isOverall = false,
                    rolloverEnabled = false,
                    name = "Food & Dining",
                    iconName = "restaurant",
                    colorHex = "#F59E0B",
                    alertPercentage = 80
                )
            )
            budgets.add(
                BudgetEntity(
                    id = "bgt_sample_groceries_$mKey",
                    categoryName = "Groceries",
                    monthlyLimit = 10000.0,
                    monthYear = mKey,
                    isOverall = false,
                    rolloverEnabled = false,
                    name = "Groceries",
                    iconName = "shopping_cart",
                    colorHex = "#10B981",
                    alertPercentage = 80
                )
            )
            budgets.add(
                BudgetEntity(
                    id = "bgt_sample_rent_$mKey",
                    categoryName = "Rent & Utilities",
                    monthlyLimit = 22000.0,
                    monthYear = mKey,
                    isOverall = false,
                    rolloverEnabled = false,
                    name = "Rent & Utilities",
                    iconName = "home",
                    colorHex = "#4F46E5",
                    alertPercentage = 85
                )
            )
            budgets.add(
                BudgetEntity(
                    id = "bgt_sample_shopping_$mKey",
                    categoryName = "Shopping",
                    monthlyLimit = 12000.0,
                    monthYear = mKey,
                    isOverall = false,
                    rolloverEnabled = false,
                    name = "Shopping",
                    iconName = "shopping_bag",
                    colorHex = "#EC4899",
                    alertPercentage = 80
                )
            )
            budgets.add(
                BudgetEntity(
                    id = "bgt_sample_transport_$mKey",
                    categoryName = "Transportation",
                    monthlyLimit = 3000.0,
                    monthYear = mKey,
                    isOverall = false,
                    rolloverEnabled = false,
                    name = "Transportation",
                    iconName = "directions_car",
                    colorHex = "#6366F1",
                    alertPercentage = 80
                )
            )
        }

        // 6. Loans & Debts
        val debts = listOf(
            DebtEntity(
                id = "debt_sample_borrow_arian",
                personName = "Arian Chowdhury",
                amount = 12000.0,
                totalAmount = 18000.0,
                paidAmount = 6000.0,
                type = DebtType.BORROWED,
                dueDate = "2026-12-15",
                notes = "Sample medical emergency support loan (1 installment paid)",
                isSettled = false,
                accountId = "acc_sample_bank",
                reason = "Medical Emergency",
                colorHex = "#EF4444",
                tentativeCloseDateStr = "2026-12-15",
                tags = "Medical, Installment"
            ),
            DebtEntity(
                id = "debt_sample_lend_nabila",
                personName = "Nabila Rahman",
                amount = 5000.0,
                totalAmount = 9000.0,
                paidAmount = 4000.0,
                type = DebtType.LENT,
                dueDate = "2026-12-01",
                notes = "Sample college education loan (partial repayment received)",
                isSettled = false,
                accountId = "acc_sample_bkash",
                reason = "Semester Fees",
                colorHex = "#4F46E5",
                tentativeCloseDateStr = "2026-12-01",
                tags = "Education, Receivable"
            )
        )

        // 7. Savings Goals
        val savingsGoals = listOf(
            SavingsGoalEntity(
                id = "goal_sample_emergency",
                name = "Emergency Reserve Fund",
                targetAmount = 100000.0,
                savedAmount = 45000.0,
                themeColorHex = "#4F46E5",
                iconName = "savings",
                targetDeadline = tsForDate(2027, Calendar.MAY, 22),
                targetDeadlineStr = "2027-05-22",
                createdAt = tsForDate(2026, Calendar.SEPTEMBER, 1),
                isCompleted = false
            ),
            SavingsGoalEntity(
                id = "goal_sample_workstation",
                name = "Home Workstation Upgrade",
                targetAmount = 35000.0,
                savedAmount = 35000.0,
                themeColorHex = "#10B981",
                iconName = "laptop",
                targetDeadline = tsForDate(2026, Calendar.OCTOBER, 31),
                targetDeadlineStr = "2026-10-31",
                createdAt = tsForDate(2026, Calendar.SEPTEMBER, 1),
                isCompleted = true
            )
        )

        // Serialize to JSON matching JsonBackupManager.createBackupJson() exactly
        root.put("accounts", JSONArray().apply {
            accounts.forEach { acc ->
                put(JSONObject().apply {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("type", acc.type.name)
                    put("institution", acc.institution)
                    put("balance", acc.balance)
                    put("openingBalance", acc.openingBalance)
                    put("accountNumberLast4", acc.accountNumberLast4)
                    put("colorHex", acc.colorHex)
                    put("iconName", acc.iconName)
                    put("isHidden", acc.isHidden)
                    put("sortOrder", acc.sortOrder)
                    put("creditLimit", acc.creditLimit)
                    put("billingDate", acc.billingDate)
                    put("paymentDueDate", acc.paymentDueDate)
                    put("minPayment", acc.minPayment)
                    put("notes", acc.notes)
                    put("includeInTotal", acc.includeInTotal)
                })
            }
        })

        root.put("transactions", JSONArray().apply {
            transactions.forEach { tx ->
                put(JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type.name)
                    put("amount", tx.amount)
                    put("title", tx.title)
                    put("category", tx.category)
                    put("subcategory", tx.subcategory)
                    put("accountId", tx.accountId)
                    if (!tx.toAccountId.isNullOrBlank()) put("toAccountId", tx.toAccountId)
                    put("timestamp", tx.timestamp)
                    put("notes", tx.notes)
                    put("isRecurring", tx.isRecurring)
                    put("payeeOrPayer", tx.payeeOrPayer)
                    if (!tx.debtId.isNullOrBlank()) put("debtId", tx.debtId)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            categories.forEach { cat ->
                put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("nameBn", cat.nameBn)
                    put("isIncome", cat.isIncome)
                    put("isTransfer", cat.isTransfer)
                    put("isActive", cat.isActive)
                    put("iconName", cat.iconName)
                    put("colorHex", cat.colorHex)
                    put("sortOrder", cat.sortOrder)
                })
            }
        })

        root.put("subcategories", JSONArray().apply {
            subcategories.forEach { sub ->
                put(JSONObject().apply {
                    put("id", sub.id)
                    put("categoryId", sub.categoryId)
                    put("name", sub.name)
                    put("nameBn", sub.nameBn)
                    put("sortOrder", sub.sortOrder)
                })
            }
        })

        root.put("budgets", JSONArray().apply {
            budgets.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id)
                    put("categoryName", b.categoryName)
                    put("monthlyLimit", b.monthlyLimit)
                    put("monthYear", b.monthYear)
                    put("isOverall", b.isOverall)
                    put("rolloverEnabled", b.rolloverEnabled)
                    put("name", b.name)
                    put("subcategoryName", b.subcategoryName)
                    put("iconName", b.iconName)
                    put("colorHex", b.colorHex)
                    put("alertPercentage", b.alertPercentage)
                })
            }
        })

        root.put("debts", JSONArray().apply {
            debts.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("personName", d.personName)
                    put("amount", d.amount)
                    put("totalAmount", d.totalAmount)
                    put("paidAmount", d.paidAmount)
                    put("type", d.type.name)
                    put("dueDate", d.dueDate)
                    put("notes", d.notes)
                    put("isSettled", d.isSettled)
                    put("accountId", d.accountId)
                    put("reason", d.reason)
                    put("colorHex", d.colorHex)
                    put("tentativeCloseDateStr", d.tentativeCloseDateStr)
                    put("tags", d.tags)
                })
            }
        })

        root.put("savingsGoals", JSONArray().apply {
            savingsGoals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    put("targetAmount", g.targetAmount)
                    put("savedAmount", g.savedAmount)
                    put("themeColorHex", g.themeColorHex)
                    put("iconName", g.iconName)
                    if (g.targetDeadline != null) put("targetDeadline", g.targetDeadline)
                    put("targetDeadlineStr", g.targetDeadlineStr)
                    put("createdAt", g.createdAt)
                    put("isCompleted", g.isCompleted)
                })
            }
        })

        return root.toString(2)
    }
}
