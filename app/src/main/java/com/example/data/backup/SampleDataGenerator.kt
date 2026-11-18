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

    fun generateSampleJsonTemplate(referenceTimeMillis: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
        val currentMonthKey = String.format(
            Locale.US,
            "%04d-%02d",
            nowCal.get(Calendar.YEAR),
            nowCal.get(Calendar.MONTH) + 1
        )
        val monthStartCal = (nowCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStartMillis = monthStartCal.timeInMillis
        val availableSpanMillis = (referenceTimeMillis - monthStartMillis).coerceAtLeast(60_000L)

        fun tsAtStep(step: Int, totalSteps: Int): Long {
            val clamped = step.coerceIn(0, totalSteps)
            return monthStartMillis + (availableSpanMillis * clamped) / totalSteps
        }

        fun dateStrDaysFromNow(daysOffset: Int): String {
            val cal = (nowCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, daysOffset)
            }
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }

        root.put("appName", "Hishab Pati")
        root.put("schemaVersion", BackupMigrationEngine.CURRENT_SCHEMA_VERSION)
        root.put("backupDate", isoFormat.format(Date(referenceTimeMillis)))
        root.put("currency", "BDT")
        root.put(
            "_comment",
            "SAMPLE TEMPLATE: Complete fictional data for Hishab Pati matching the backup schema. " +
                "Includes reconciled opening/closing balances across Cash, bKash/Nagad MFS, Bank Debit Card, and Credit Card accounts, " +
                "all default Expense/Income/Transfer categories & subcategories, monthly budgets, savings goals, and Lent/Borrowed debts with repayments."
        )

        // 1. Settings matching AppSettingsEntity & JsonBackupManager
        val defaultSettings = AppSettingsEntity(
            userName = "Demo User",
            country = "Bangladesh",
            currencyCode = "BDT",
            currencySymbol = "৳",
            themeMode = ThemeMode.AUTO,
            themeColor = ThemeColorOption.SKY_CYAN,
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

        // 3. Transactions covering ALL 12 default Expense categories, ALL 5 default Income categories,
        // Transfers between accounts, Credit Card Payment, and Borrow/Lend + partial & full repayments.
        val totalSteps = 29
        val transactions = listOf(
            // --- Income (all 5 default Income categories) ---
            TransactionEntity(
                id = "tx_sample_inc_salary",
                type = TransactionType.INCOME,
                amount = 75000.0,
                title = "Monthly Corporate Salary",
                category = "Salary",
                subcategory = "Monthly Salary",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(1, totalSteps),
                notes = "Sample monthly salary deposit",
                isRecurring = true,
                payeeOrPayer = "Tech Innovations Ltd."
            ),
            TransactionEntity(
                id = "tx_sample_inc_bonus",
                type = TransactionType.INCOME,
                amount = 12000.0,
                title = "Quarterly Performance Bonus",
                category = "Bonus",
                subcategory = "Performance Bonus",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(2, totalSteps),
                notes = "Sample performance bonus credit",
                payeeOrPayer = "Tech Innovations Ltd."
            ),
            TransactionEntity(
                id = "tx_sample_inc_business",
                type = TransactionType.INCOME,
                amount = 15000.0,
                title = "UI Consulting Milestone",
                category = "Business",
                subcategory = "Consulting",
                accountId = "acc_sample_bkash",
                timestamp = tsAtStep(3, totalSteps),
                notes = "Sample freelance consulting fee",
                payeeOrPayer = "Apex Digital Studio"
            ),
            TransactionEntity(
                id = "tx_sample_inc_gift",
                type = TransactionType.INCOME,
                amount = 3000.0,
                title = "Family Occasion Cash Gift",
                category = "Gifts Received",
                subcategory = "Cash Gift",
                accountId = "acc_sample_cash",
                timestamp = tsAtStep(4, totalSteps),
                notes = "Sample family gift",
                payeeOrPayer = "Family"
            ),
            TransactionEntity(
                id = "tx_sample_inc_cashback",
                type = TransactionType.INCOME,
                amount = 500.0,
                title = "Mobile Payment Cashback",
                category = "Other Income",
                subcategory = "Refunds & Cashbacks",
                accountId = "acc_sample_nagad",
                timestamp = tsAtStep(5, totalSteps),
                notes = "Sample merchant cashback reward",
                payeeOrPayer = "Nagad Rewards"
            ),

            // --- Transfers between accounts ---
            TransactionEntity(
                id = "tx_sample_trf_atm",
                type = TransactionType.TRANSFER,
                amount = 8000.0,
                title = "ATM Cash Withdrawal",
                category = "Bank Transfer",
                subcategory = "ATM Withdrawal",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_cash",
                timestamp = tsAtStep(6, totalSteps),
                notes = "Sample ATM withdrawal from bank debit card to cash wallet",
                payeeOrPayer = "City Bank ATM"
            ),
            TransactionEntity(
                id = "tx_sample_trf_bkash",
                type = TransactionType.TRANSFER,
                amount = 10000.0,
                title = "Add Money to bKash",
                category = "Wallet Transfer",
                subcategory = "Mobile Financial Service",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_bkash",
                timestamp = tsAtStep(7, totalSteps),
                notes = "Sample bank-to-bKash transfer",
                payeeOrPayer = "bKash Add Money"
            ),
            TransactionEntity(
                id = "tx_sample_trf_nagad",
                type = TransactionType.TRANSFER,
                amount = 3000.0,
                title = "bKash to Nagad Wallet Transfer",
                category = "Wallet Transfer",
                subcategory = "Mobile Financial Service",
                accountId = "acc_sample_bkash",
                toAccountId = "acc_sample_nagad",
                timestamp = tsAtStep(8, totalSteps),
                notes = "Sample wallet-to-wallet transfer",
                payeeOrPayer = "Self Transfer"
            ),

            // --- Expenses (all 12 default Expense categories + subcategories) ---
            TransactionEntity(
                id = "tx_sample_exp_food_dinner",
                type = TransactionType.EXPENSE,
                amount = 1650.0,
                title = "Family Weekend Dinner",
                category = "Food & Dining",
                subcategory = "Restaurant",
                accountId = "acc_sample_cash",
                timestamp = tsAtStep(9, totalSteps),
                notes = "Sample dinner out",
                payeeOrPayer = "Spice Garden Cafe"
            ),
            TransactionEntity(
                id = "tx_sample_exp_food_coffee",
                type = TransactionType.EXPENSE,
                amount = 480.0,
                title = "Evening Coffee & Snacks",
                category = "Food & Dining",
                subcategory = "Cafes & Coffee",
                accountId = "acc_sample_bkash",
                timestamp = tsAtStep(10, totalSteps),
                notes = "Sample cafe visit",
                payeeOrPayer = "North End Roasters"
            ),
            TransactionEntity(
                id = "tx_sample_exp_shopping_clothes",
                type = TransactionType.EXPENSE,
                amount = 6500.0,
                title = "Seasonal Apparel Purchase",
                category = "Shopping",
                subcategory = "Clothing",
                accountId = "acc_sample_card",
                timestamp = tsAtStep(11, totalSteps),
                notes = "Sample clothing purchase on credit card",
                payeeOrPayer = "Aarong"
            ),
            TransactionEntity(
                id = "tx_sample_exp_shopping_elec",
                type = TransactionType.EXPENSE,
                amount = 4200.0,
                title = "Wireless Earbuds",
                category = "Shopping",
                subcategory = "Electronics",
                accountId = "acc_sample_card",
                timestamp = tsAtStep(12, totalSteps),
                notes = "Sample electronics purchase on credit card",
                payeeOrPayer = "Gadget Galaxy"
            ),
            TransactionEntity(
                id = "tx_sample_exp_transport",
                type = TransactionType.EXPENSE,
                amount = 950.0,
                title = "City Ride Sharing",
                category = "Transportation",
                subcategory = "Ride Sharing",
                accountId = "acc_sample_bkash",
                timestamp = tsAtStep(13, totalSteps),
                notes = "Sample daily commute rides",
                payeeOrPayer = "Pathao Rides"
            ),
            TransactionEntity(
                id = "tx_sample_exp_rent",
                type = TransactionType.EXPENSE,
                amount = 18000.0,
                title = "Apartment Monthly Rent",
                category = "Rent & Utilities",
                subcategory = "House Rent",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(14, totalSteps),
                notes = "Sample monthly house rent",
                isRecurring = true,
                payeeOrPayer = "Skyline Properties"
            ),
            TransactionEntity(
                id = "tx_sample_exp_internet",
                type = TransactionType.EXPENSE,
                amount = 1200.0,
                title = "Broadband Fiber Internet Bill",
                category = "Rent & Utilities",
                subcategory = "Internet",
                accountId = "acc_sample_bkash",
                timestamp = tsAtStep(15, totalSteps),
                notes = "Sample monthly internet bill",
                isRecurring = true,
                payeeOrPayer = "AmberIT"
            ),
            TransactionEntity(
                id = "tx_sample_exp_health",
                type = TransactionType.EXPENSE,
                amount = 1450.0,
                title = "Pharmacy Medicines & Vitamins",
                category = "Health & Medical",
                subcategory = "Medicines",
                accountId = "acc_sample_nagad",
                timestamp = tsAtStep(16, totalSteps),
                notes = "Sample pharmacy bill",
                payeeOrPayer = "Lazz Pharma"
            ),
            TransactionEntity(
                id = "tx_sample_exp_entertainment",
                type = TransactionType.EXPENSE,
                amount = 850.0,
                title = "Streaming Subscription Bundle",
                category = "Entertainment",
                subcategory = "Streaming Services",
                accountId = "acc_sample_card",
                timestamp = tsAtStep(17, totalSteps),
                notes = "Sample monthly entertainment subscription",
                isRecurring = true,
                payeeOrPayer = "StreamHub"
            ),
            TransactionEntity(
                id = "tx_sample_exp_personal_care",
                type = TransactionType.EXPENSE,
                amount = 750.0,
                title = "Haircut & Grooming",
                category = "Personal Care",
                subcategory = "Salon & Grooming",
                accountId = "acc_sample_cash",
                timestamp = tsAtStep(18, totalSteps),
                notes = "Sample salon service",
                payeeOrPayer = "Urban Grooming Studio"
            ),
            TransactionEntity(
                id = "tx_sample_exp_groceries_veg",
                type = TransactionType.EXPENSE,
                amount = 3200.0,
                title = "Fresh Vegetables & Fish Market",
                category = "Groceries",
                subcategory = "Produce & Vegetables",
                accountId = "acc_sample_cash",
                timestamp = tsAtStep(19, totalSteps),
                notes = "Sample weekly fresh groceries",
                payeeOrPayer = "Green Harvest Market"
            ),
            TransactionEntity(
                id = "tx_sample_exp_groceries_pantry",
                type = TransactionType.EXPENSE,
                amount = 2100.0,
                title = "Rice, Lentils & Cooking Oil",
                category = "Groceries",
                subcategory = "Pantry Staples",
                accountId = "acc_sample_nagad",
                timestamp = tsAtStep(20, totalSteps),
                notes = "Sample supermarket pantry restock",
                payeeOrPayer = "Shwapno Superstore"
            ),
            TransactionEntity(
                id = "tx_sample_exp_travel",
                type = TransactionType.EXPENSE,
                amount = 5500.0,
                title = "Weekend Resort Booking",
                category = "Travel",
                subcategory = "Hotel & Lodging",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(21, totalSteps),
                notes = "Sample weekend getaway hotel stay",
                payeeOrPayer = "Sylhet Valley Resort"
            ),
            TransactionEntity(
                id = "tx_sample_exp_family",
                type = TransactionType.EXPENSE,
                amount = 6000.0,
                title = "Parents Monthly Allowance",
                category = "Family",
                subcategory = "Parents Support",
                accountId = "acc_sample_nagad",
                timestamp = tsAtStep(22, totalSteps),
                notes = "Sample monthly support sent home",
                isRecurring = true,
                payeeOrPayer = "Parents"
            ),
            TransactionEntity(
                id = "tx_sample_exp_other",
                type = TransactionType.EXPENSE,
                amount = 1000.0,
                title = "Community Charity Contribution",
                category = "Other Expenses",
                subcategory = "Donations & Charity",
                accountId = "acc_sample_cash",
                timestamp = tsAtStep(23, totalSteps),
                notes = "Sample charitable donation",
                payeeOrPayer = "Local Community Welfare"
            ),

            // --- Credit Card Bill Payment ---
            TransactionEntity(
                id = "tx_sample_cc_payment",
                type = TransactionType.CC_PAYMENT,
                amount = 6500.0,
                title = "Credit Card Statement Payment",
                category = "Bank Transfer",
                subcategory = "Account to Account",
                accountId = "acc_sample_bank",
                toAccountId = "acc_sample_card",
                timestamp = tsAtStep(24, totalSteps),
                notes = "Sample credit card statement payment from debit bank account",
                payeeOrPayer = "Standard Chartered Bank"
            ),

            // --- Loans & Debts: Borrowed (Payable) + Partial Repayment + Settled ---
            TransactionEntity(
                id = "tx_sample_borrow_arian",
                type = TransactionType.BORROW,
                amount = 18000.0,
                title = "Medical Emergency Loan Borrowed",
                category = "Other Income",
                subcategory = "Miscellaneous",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(25, totalSteps),
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
                timestamp = tsAtStep(26, totalSteps),
                notes = "Sample partial repayment (৳6,000 of ৳18,000) to Arian Chowdhury [Debt:debt_sample_borrow_arian]",
                payeeOrPayer = "Arian Chowdhury",
                debtId = "debt_sample_borrow_arian"
            ),
            TransactionEntity(
                id = "tx_sample_borrow_farhan",
                type = TransactionType.BORROW,
                amount = 10000.0,
                title = "Short-Term Project Loan Borrowed",
                category = "Other Income",
                subcategory = "Miscellaneous",
                accountId = "acc_sample_nagad",
                timestamp = tsAtStep(25, totalSteps),
                notes = "Sample borrowed amount from Farhan Kabir [Debt:debt_sample_borrow_farhan]",
                payeeOrPayer = "Farhan Kabir",
                debtId = "debt_sample_borrow_farhan"
            ),
            TransactionEntity(
                id = "tx_sample_settle_farhan",
                type = TransactionType.EXPENSE,
                amount = 10000.0,
                title = "Loan Full Settlement Repayment",
                category = "Debt Repayment",
                subcategory = "Loan Principal",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(28, totalSteps),
                notes = "Sample full settlement repayment (৳10,000) to Farhan Kabir [Debt:debt_sample_borrow_farhan]",
                payeeOrPayer = "Farhan Kabir",
                debtId = "debt_sample_borrow_farhan"
            ),

            // --- Loans & Debts: Lent (Receivable) + Partial Collection + Settled ---
            TransactionEntity(
                id = "tx_sample_lend_nabila",
                type = TransactionType.LEND,
                amount = 9000.0,
                title = "Education Support Loan Lent",
                category = "Other Expenses",
                subcategory = "General Miscellaneous",
                accountId = "acc_sample_bkash",
                timestamp = tsAtStep(26, totalSteps),
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
                timestamp = tsAtStep(27, totalSteps),
                notes = "Sample partial installment (৳4,000 of ৳9,000) received from Nabila Rahman [Debt:debt_sample_lend_nabila]",
                payeeOrPayer = "Nabila Rahman",
                debtId = "debt_sample_lend_nabila"
            ),
            TransactionEntity(
                id = "tx_sample_lend_rafi",
                type = TransactionType.LEND,
                amount = 10000.0,
                title = "Friendly Assistance Loan Lent",
                category = "Other Expenses",
                subcategory = "General Miscellaneous",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(26, totalSteps),
                notes = "Sample short-term assistance lent to Rafi Hasan [Debt:debt_sample_lend_rafi]",
                payeeOrPayer = "Rafi Hasan",
                debtId = "debt_sample_lend_rafi"
            ),
            TransactionEntity(
                id = "tx_sample_collect_rafi",
                type = TransactionType.INCOME,
                amount = 10000.0,
                title = "Loan Full Settlement Received Back",
                category = "Other Income",
                subcategory = "Miscellaneous",
                accountId = "acc_sample_bank",
                timestamp = tsAtStep(29, totalSteps),
                notes = "Sample full settlement (৳10,000) received from Rafi Hasan [Debt:debt_sample_lend_rafi]",
                payeeOrPayer = "Rafi Hasan",
                debtId = "debt_sample_lend_rafi"
            )
        )

        // 4. Sample Accounts across all types (Cash, bKash MFS, Nagad MFS, Bank Debit Card, Credit Card)
        // Balances are computed dynamically from openingBalance + exact transaction deltas so every account reconciles 100%.
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
                colorHex = "#2563EB",
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
                colorHex = "#8B5CF6",
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

        // 5. Monthly Budgets matching BudgetEntity & JsonBackupManager
        val budgets = listOf(
            BudgetEntity(
                id = "bgt_sample_food",
                categoryName = "Food & Dining",
                monthlyLimit = 6000.0,
                monthYear = currentMonthKey,
                isOverall = false,
                rolloverEnabled = false,
                name = "Food & Dining",
                iconName = "restaurant",
                colorHex = "#F59E0B",
                alertPercentage = 80
            ),
            BudgetEntity(
                id = "bgt_sample_groceries",
                categoryName = "Groceries",
                monthlyLimit = 10000.0,
                monthYear = currentMonthKey,
                isOverall = false,
                rolloverEnabled = false,
                name = "Groceries",
                iconName = "shopping_cart",
                colorHex = "#10B981",
                alertPercentage = 80
            ),
            BudgetEntity(
                id = "bgt_sample_rent",
                categoryName = "Rent & Utilities",
                monthlyLimit = 22000.0,
                monthYear = currentMonthKey,
                isOverall = false,
                rolloverEnabled = false,
                name = "Rent & Utilities",
                iconName = "home",
                colorHex = "#3B82F6",
                alertPercentage = 85
            ),
            BudgetEntity(
                id = "bgt_sample_shopping",
                categoryName = "Shopping",
                monthlyLimit = 12000.0,
                monthYear = currentMonthKey,
                isOverall = false,
                rolloverEnabled = false,
                name = "Shopping",
                iconName = "shopping_bag",
                colorHex = "#EC4899",
                alertPercentage = 80
            ),
            BudgetEntity(
                id = "bgt_sample_transport",
                categoryName = "Transportation",
                monthlyLimit = 3000.0,
                monthYear = currentMonthKey,
                isOverall = false,
                rolloverEnabled = false,
                name = "Transportation",
                iconName = "directions_car",
                colorHex = "#6366F1",
                alertPercentage = 80
            )
        )

        // 6. Loans & Debts covering BOTH Lent (Receivable) & Borrowed (Payable) with partial & settled histories
        val debts = listOf(
            DebtEntity(
                id = "debt_sample_borrow_arian",
                personName = "Arian Chowdhury",
                amount = 12000.0,
                totalAmount = 18000.0,
                paidAmount = 6000.0,
                type = DebtType.BORROWED,
                dueDate = dateStrDaysFromNow(45),
                notes = "Sample medical emergency support loan (1 installment paid)",
                isSettled = false,
                accountId = "acc_sample_bank",
                reason = "Medical Emergency",
                colorHex = "#EF4444",
                tentativeCloseDateStr = dateStrDaysFromNow(45),
                tags = "Medical, Installment"
            ),
            DebtEntity(
                id = "debt_sample_borrow_farhan",
                personName = "Farhan Kabir",
                amount = 0.0,
                totalAmount = 10000.0,
                paidAmount = 10000.0,
                type = DebtType.BORROWED,
                dueDate = dateStrDaysFromNow(-2),
                notes = "Sample project bridge loan - fully settled",
                isSettled = true,
                accountId = "acc_sample_nagad",
                reason = "Project Bridge",
                colorHex = "#10B981",
                tentativeCloseDateStr = dateStrDaysFromNow(-2),
                tags = "Settled"
            ),
            DebtEntity(
                id = "debt_sample_lend_nabila",
                personName = "Nabila Rahman",
                amount = 5000.0,
                totalAmount = 9000.0,
                paidAmount = 4000.0,
                type = DebtType.LENT,
                dueDate = dateStrDaysFromNow(30),
                notes = "Sample college education loan (partial repayment received)",
                isSettled = false,
                accountId = "acc_sample_bkash",
                reason = "Semester Fees",
                colorHex = "#3B82F6",
                tentativeCloseDateStr = dateStrDaysFromNow(30),
                tags = "Education, Receivable"
            ),
            DebtEntity(
                id = "debt_sample_lend_rafi",
                personName = "Rafi Hasan",
                amount = 0.0,
                totalAmount = 10000.0,
                paidAmount = 10000.0,
                type = DebtType.LENT,
                dueDate = dateStrDaysFromNow(-1),
                notes = "Sample friendly assistance - fully collected",
                isSettled = true,
                accountId = "acc_sample_bank",
                reason = "Friendly Loan",
                colorHex = "#10B981",
                tentativeCloseDateStr = dateStrDaysFromNow(-1),
                tags = "Settled"
            )
        )

        // 7. Savings Goals matching SavingsGoalEntity & JsonBackupManager
        val futureDeadlineCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 180) }
        val savingsGoals = listOf(
            SavingsGoalEntity(
                id = "goal_sample_emergency",
                name = "Emergency Reserve Fund",
                targetAmount = 100000.0,
                savedAmount = 45000.0,
                themeColorHex = "#06B6D4",
                iconName = "savings",
                targetDeadline = futureDeadlineCal.timeInMillis,
                targetDeadlineStr = dateStrDaysFromNow(180),
                createdAt = monthStartMillis,
                isCompleted = false
            ),
            SavingsGoalEntity(
                id = "goal_sample_workstation",
                name = "Home Workstation Upgrade",
                targetAmount = 35000.0,
                savedAmount = 35000.0,
                themeColorHex = "#10B981",
                iconName = "laptop",
                targetDeadline = referenceTimeMillis,
                targetDeadlineStr = dateStrDaysFromNow(0),
                createdAt = monthStartMillis,
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
