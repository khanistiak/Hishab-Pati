package com.example.data.backup

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SampleDataGenerator {

    fun generateSampleJsonTemplate(): String {
        val root = JSONObject()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val now = 1793145600000L // Fictional baseline: Oct 27, 2026

        root.put("appName", "Hishab Pati")
        root.put("schemaVersion", BackupMigrationEngine.CURRENT_SCHEMA_VERSION)
        root.put("backupDate", isoFormat.format(Date(now)))
        root.put("currency", "BDT")
        root.put(
            "_comment",
            "DEMONSTRATION ONLY: Completely fictional sample data for Hishab Pati. " +
                "Includes fully reconciled opening/closing balances and complete examples of all supported transaction types " +
                "(Income, Expense, Transfer, Credit Card Payment, Borrowed, Lent, Partial Repayments, and Full Settlements)."
        )

        // Fictional App Settings
        val settingsObj = JSONObject().apply {
            put("userName", "Fictional Demo User")
            put("country", "Bangladesh")
            put("currencyCode", "BDT")
            put("currencySymbol", "৳")
            put("language", "ENGLISH")
            put("themeMode", "AUTO")
            put("themeColor", "SKY_CYAN")
            put("backgroundTexture", "AURORA")
            put("appLockEnabled", false)
            put("screenshotProtectionEnabled", false)
            put("notificationsEnabled", true)
        }
        root.put("settings", settingsObj)

        // 4 Reconciled Fictional Accounts
        // 1. Bank: opening 40,000 + 75000 (inc) - 15000 (trf) - 16000 (exp) + 18000 (borrow) - 6000 (repay) + 10000 (borrow) - 10000 (settle) + 12000 (rcv settle) - 8500 (cc pay) = 99,500
        // 2. Cash: opening 5,000 + 15000 (trf in) - 4350 (groceries) - 1650 (dining) = 14,000
        // 3. MFS:  opening 25,000 - 8000 (lend) + 3000 (rcv inst) - 12000 (lend) - 1200 (pharmacy) = 6,800
        // 4. Card: opening 0.0 - 8500 (shopping) + 8500 (cc pay) - 4500 (electronics) = -4,500
        val accountsArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "acc_fictional_bank")
                put("name", "City Horizon Bank")
                put("type", "BANK")
                put("institution", "City Bank PLC")
                put("balance", 99500.0)
                put("openingBalance", 40000.0)
                put("accountNumberLast4", "4821")
                put("colorHex", "#2563EB")
                put("iconName", "account_balance")
                put("isHidden", false)
                put("sortOrder", 0)
                put("notes", "Fictional salary and checking account")
            })
            put(JSONObject().apply {
                put("id", "acc_fictional_cash")
                put("name", "Cash Wallet")
                put("type", "CASH")
                put("institution", "Physical Cash")
                put("balance", 14000.0)
                put("openingBalance", 5000.0)
                put("accountNumberLast4", "")
                put("colorHex", "#10B981")
                put("iconName", "payments")
                put("isHidden", false)
                put("sortOrder", 1)
                put("notes", "Daily cash in hand")
            })
            put(JSONObject().apply {
                put("id", "acc_fictional_mfs")
                put("name", "bKash Digital Wallet")
                put("type", "MFS")
                put("institution", "bKash Limited")
                put("balance", 6800.0)
                put("openingBalance", 25000.0)
                put("accountNumberLast4", "0171")
                put("colorHex", "#E11D48")
                put("iconName", "phone_android")
                put("isHidden", false)
                put("sortOrder", 2)
                put("notes", "Mobile wallet for peer transfers")
            })
            put(JSONObject().apply {
                put("id", "acc_fictional_card")
                put("name", "Standard Visa Platinum")
                put("type", "CREDIT_CARD")
                put("institution", "Standard Chartered")
                put("balance", -4500.0)
                put("openingBalance", 0.0)
                put("accountNumberLast4", "9204")
                put("creditLimit", 150000.0)
                put("colorHex", "#8B5CF6")
                put("iconName", "credit_card")
                put("isHidden", false)
                put("sortOrder", 3)
                put("notes", "Fictional credit card for online/card shopping")
            })
        }
        root.put("accounts", accountsArr)

        // Fictional Categories
        val catArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "cat_fictional_salary")
                put("name", "Salary")
                put("nameBn", "বেতন")
                put("isIncome", true)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "payments")
                put("colorHex", "#10B981")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_essentials")
                put("name", "Daily Essentials")
                put("nameBn", "দৈনন্দিন বাজার")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "shopping_cart")
                put("colorHex", "#10B981")
                put("sortOrder", 2)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_eating")
                put("name", "Eating Out")
                put("nameBn", "বাইরে খাওয়া")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "restaurant")
                put("colorHex", "#F59E0B")
                put("sortOrder", 3)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_rent")
                put("name", "Rent & Utilities")
                put("nameBn", "ভাড়া ও ইউটিলিটি")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "home")
                put("colorHex", "#3B82F6")
                put("sortOrder", 4)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_shopping")
                put("name", "Shopping")
                put("nameBn", "কেনাকাটা")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "checkroom")
                put("colorHex", "#EC4899")
                put("sortOrder", 5)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_transfer")
                put("name", "Transfer")
                put("nameBn", "স্থানান্তর")
                put("isIncome", false)
                put("isTransfer", true)
                put("isActive", true)
                put("iconName", "swap_horiz")
                put("colorHex", "#6366F1")
                put("sortOrder", 6)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_financial")
                put("name", "Financial")
                put("nameBn", "আর্থিক")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "credit_card")
                put("colorHex", "#0EA5E9")
                put("sortOrder", 7)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_borrow")
                put("name", "Debt / Borrow")
                put("nameBn", "ধার / ঋণ")
                put("isIncome", true)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "handshake")
                put("colorHex", "#0EA5E9")
                put("sortOrder", 8)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_repay")
                put("name", "Debt Repayment")
                put("nameBn", "ঋণ পরিশোধ")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "receipt_long")
                put("colorHex", "#8B5CF6")
                put("sortOrder", 9)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_lend")
                put("name", "Lend Money")
                put("nameBn", "ঋণ প্রদান")
                put("isIncome", false)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "volunteer_activism")
                put("colorHex", "#EC4899")
                put("sortOrder", 10)
            })
            put(JSONObject().apply {
                put("id", "cat_fictional_money_rcv")
                put("name", "Money Received")
                put("nameBn", "টাকা প্রাপ্তি")
                put("isIncome", true)
                put("isTransfer", false)
                put("isActive", true)
                put("iconName", "payments")
                put("colorHex", "#10B981")
                put("sortOrder", 11)
            })
        }
        root.put("categories", catArr)

        // Fictional Subcategories with valid category references
        val subArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "sub_fictional_salary_monthly")
                put("categoryId", "cat_fictional_salary")
                put("name", "Monthly Salary")
                put("nameBn", "মাসিক বেতন")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_groceries")
                put("categoryId", "cat_fictional_essentials")
                put("name", "Groceries")
                put("nameBn", "মুদি বাজার")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_pharmacy")
                put("categoryId", "cat_fictional_essentials")
                put("name", "Medicines")
                put("nameBn", "ওষুধ")
                put("sortOrder", 2)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_restaurant")
                put("categoryId", "cat_fictional_eating")
                put("name", "Restaurant")
                put("nameBn", "রেস্তোরাঁ")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_house_rent")
                put("categoryId", "cat_fictional_rent")
                put("name", "House Rent")
                put("nameBn", "বাড়ি ভাড়া")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_clothing")
                put("categoryId", "cat_fictional_shopping")
                put("name", "Clothing")
                put("nameBn", "পোশাক")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_electronics")
                put("categoryId", "cat_fictional_shopping")
                put("name", "Electronics")
                put("nameBn", "ইলেকট্রনিক্স")
                put("sortOrder", 2)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_transfer_atm")
                put("categoryId", "cat_fictional_transfer")
                put("name", "Account Transfer")
                put("nameBn", "অ্যাকাউন্ট ট্রান্সফার")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_financial_cc")
                put("categoryId", "cat_fictional_financial")
                put("name", "Credit Card Payment")
                put("nameBn", "ক্রেডিট কার্ড পরিশোধ")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_borrow_funds")
                put("categoryId", "cat_fictional_borrow")
                put("name", "Bank Loan")
                put("nameBn", "ব্যাংক ঋণ")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_repay_inst")
                put("categoryId", "cat_fictional_repay")
                put("name", "Installment")
                put("nameBn", "কিস্তি")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_repay_settle")
                put("categoryId", "cat_fictional_repay")
                put("name", "Final Settlement")
                put("nameBn", "চূড়ান্ত পরিশোধ")
                put("sortOrder", 2)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_lend_loan")
                put("categoryId", "cat_fictional_lend")
                put("name", "Personal Loan")
                put("nameBn", "ব্যক্তিগত ঋণ")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_rcv_inst")
                put("categoryId", "cat_fictional_money_rcv")
                put("name", "From Family/Friend")
                put("nameBn", "পরিবার/বন্ধু হতে")
                put("sortOrder", 1)
            })
            put(JSONObject().apply {
                put("id", "sub_fictional_rcv_settle")
                put("categoryId", "cat_fictional_money_rcv")
                put("name", "Settlement Return")
                put("nameBn", "চূড়ান্ত ফেরত")
                put("sortOrder", 2)
            })
        }
        root.put("subcategories", subArr)

        // Complete set of 17 Fictional Transactions covering all supported types
        val txArr = JSONArray().apply {
            // 1. Income (Corporate Salary)
            put(JSONObject().apply {
                put("id", "tx_fictional_inc_1")
                put("type", "INCOME")
                put("amount", 75000.0)
                put("title", "Monthly Corporate Salary")
                put("category", "Salary")
                put("subcategory", "Monthly Salary")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1791244800000L) // Oct 05, 2026
                put("payeeOrPayer", "Tech Innovations Ltd.")
                put("notes", "Fictional corporate salary credit for October")
                put("isRecurring", true)
            })

            // 2. Wallet-to-wallet transfer (Bank -> Cash)
            put(JSONObject().apply {
                put("id", "tx_fictional_trf_1")
                put("type", "TRANSFER")
                put("amount", 15000.0)
                put("title", "ATM Cash Withdrawal to Pocket")
                put("category", "Transfer")
                put("subcategory", "Account Transfer")
                put("accountId", "acc_fictional_bank")
                put("toAccountId", "acc_fictional_cash")
                put("timestamp", 1791504000000L) // Oct 08, 2026
                put("payeeOrPayer", "Self Transfer")
                put("notes", "Fictional cash withdrawal for grocery and household expenses")
                put("isRecurring", false)
            })

            // 3. Expense (Cash Groceries)
            put(JSONObject().apply {
                put("id", "tx_fictional_exp_grocery")
                put("type", "EXPENSE")
                put("amount", 4350.0)
                put("title", "Supermarket Weekly Groceries")
                put("category", "Daily Essentials")
                put("subcategory", "Groceries")
                put("accountId", "acc_fictional_cash")
                put("timestamp", 1791849600000L) // Oct 12, 2026
                put("payeeOrPayer", "Green Harvest Mart")
                put("notes", "Fictional pantry essentials and household goods")
                put("isRecurring", false)
            })

            // 4. Expense on Credit Card (Shopping)
            put(JSONObject().apply {
                put("id", "tx_fictional_exp_shopping")
                put("type", "EXPENSE")
                put("amount", 8500.0)
                put("title", "Seasonal Work Apparel")
                put("category", "Shopping")
                put("subcategory", "Clothing")
                put("accountId", "acc_fictional_card")
                put("timestamp", 1791849600000L) // Oct 12, 2026
                put("payeeOrPayer", "Apex Tailors")
                put("notes", "Fictional clothing purchase on credit card")
                put("isRecurring", false)
            })

            // 5. Expense (Dining Out)
            put(JSONObject().apply {
                put("id", "tx_fictional_exp_dining")
                put("type", "EXPENSE")
                put("amount", 1650.0)
                put("title", "Family Weekend Dinner")
                put("category", "Eating Out")
                put("subcategory", "Restaurant")
                put("accountId", "acc_fictional_cash")
                put("timestamp", 1792108800000L) // Oct 15, 2026
                put("payeeOrPayer", "Spice Garden Cafe")
                put("notes", "Fictional weekend dinner with family")
                put("isRecurring", false)
            })

            // 6. Expense (Apartment Rent from Bank)
            put(JSONObject().apply {
                put("id", "tx_fictional_exp_rent")
                put("type", "EXPENSE")
                put("amount", 16000.0)
                put("title", "Apartment Rent & Utility Bill")
                put("category", "Rent & Utilities")
                put("subcategory", "House Rent")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1792368000000L) // Oct 18, 2026
                put("payeeOrPayer", "Skyline Property Management")
                put("notes", "Fictional monthly rent and utility billing")
                put("isRecurring", false)
            })

            // 7. Borrowed money received (Partial Debt: Arian Chowdhury)
            put(JSONObject().apply {
                put("id", "tx_fictional_borrow_arian")
                put("type", "BORROW")
                put("amount", 18000.0)
                put("title", "Emergency Medical Loan Borrowed")
                put("category", "Debt / Borrow")
                put("subcategory", "Bank Loan")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1792540800000L) // Oct 20, 2026
                put("payeeOrPayer", "Arian Chowdhury")
                put("debtId", "debt_fictional_borrow_arian")
                put("notes", "Fictional borrowed funds from Arian Chowdhury for temporary medical emergency [Debt:debt_fictional_borrow_arian]")
                put("isRecurring", false)
            })

            // 8. Borrowed money received (Settled Debt: Farhan Kabir)
            put(JSONObject().apply {
                put("id", "tx_fictional_borrow_farhan")
                put("type", "BORROW")
                put("amount", 10000.0)
                put("title", "Project Bridge Loan Borrowed")
                put("category", "Debt / Borrow")
                put("subcategory", "Bank Loan")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1792713600000L) // Oct 22, 2026
                put("payeeOrPayer", "Farhan Kabir")
                put("debtId", "debt_fictional_borrow_farhan")
                put("notes", "Fictional short-term borrowed amount from Farhan Kabir [Debt:debt_fictional_borrow_farhan]")
                put("isRecurring", false)
            })

            // 9. Debt repayment - Partial installment (to Arian Chowdhury)
            put(JSONObject().apply {
                put("id", "tx_fictional_repay_arian")
                put("type", "EXPENSE")
                put("amount", 6000.0)
                put("title", "Loan 1st Installment Repayment")
                put("category", "Debt Repayment")
                put("subcategory", "Installment")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1792713600000L) // Oct 22, 2026
                put("payeeOrPayer", "Arian Chowdhury")
                put("debtId", "debt_fictional_borrow_arian")
                put("notes", "Fictional 1st installment repayment (৳6,000 of ৳18,000) to Arian Chowdhury [Debt:debt_fictional_borrow_arian]")
                put("isRecurring", false)
            })

            // 10. Money lent (to Nabila Rahman - Partial)
            put(JSONObject().apply {
                put("id", "tx_fictional_lend_nabila")
                put("type", "LEND")
                put("amount", 8000.0)
                put("title", "Money Lent for Education")
                put("category", "Lend Money")
                put("subcategory", "Personal Loan")
                put("accountId", "acc_fictional_mfs")
                put("timestamp", 1792713600000L) // Oct 22, 2026
                put("payeeOrPayer", "Nabila Rahman")
                put("debtId", "debt_fictional_lend_nabila")
                put("notes", "Fictional personal loan lent to Nabila Rahman for college books [Debt:debt_fictional_lend_nabila]")
                put("isRecurring", false)
            })

            // 11. Money lent (to Rafi Hasan - for Full Settlement)
            put(JSONObject().apply {
                put("id", "tx_fictional_lend_rafi")
                put("type", "LEND")
                put("amount", 12000.0)
                put("title", "Short-Term Friendly Loan Lent")
                put("category", "Lend Money")
                put("subcategory", "Personal Loan")
                put("accountId", "acc_fictional_mfs")
                put("timestamp", 1792713600000L) // Oct 22, 2026
                put("payeeOrPayer", "Rafi Hasan")
                put("debtId", "debt_fictional_lend_rafi")
                put("notes", "Fictional short-term assistance lent to Rafi Hasan [Debt:debt_fictional_lend_rafi]")
                put("isRecurring", false)
            })

            // 12. Credit Card Payment (Bank -> Card)
            put(JSONObject().apply {
                put("id", "tx_fictional_cc_pay")
                put("type", "CC_PAYMENT")
                put("amount", 8500.0)
                put("title", "Credit Card Full Statement Payment")
                put("category", "Financial")
                put("subcategory", "Credit Card Payment")
                put("accountId", "acc_fictional_bank")
                put("toAccountId", "acc_fictional_card")
                put("timestamp", 1792713600000L) // Oct 22, 2026
                put("payeeOrPayer", "Standard Chartered Bank")
                put("notes", "Fictional statement payment for Visa Platinum from Bank")
                put("isRecurring", false)
            })

            // 13. Debt repayment - Full settlement (to Farhan Kabir)
            put(JSONObject().apply {
                put("id", "tx_fictional_settle_farhan")
                put("type", "EXPENSE")
                put("amount", 10000.0)
                put("title", "Loan Full Settlement")
                put("category", "Debt Repayment")
                put("subcategory", "Final Settlement")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1792886400000L) // Oct 24, 2026
                put("payeeOrPayer", "Farhan Kabir")
                put("debtId", "debt_fictional_borrow_farhan")
                put("notes", "Fictional full settlement payment (৳10,000) to Farhan Kabir [Debt:debt_fictional_borrow_farhan]")
                put("isRecurring", false)
            })

            // 14. Repayment received for money lent - Partial collection (from Nabila Rahman)
            put(JSONObject().apply {
                put("id", "tx_fictional_collect_nabila")
                put("type", "INCOME")
                put("amount", 3000.0)
                put("title", "Loan Partial Repayment Received")
                put("category", "Money Received")
                put("subcategory", "From Family/Friend")
                put("accountId", "acc_fictional_mfs")
                put("timestamp", 1792972800000L) // Oct 25, 2026
                put("payeeOrPayer", "Nabila Rahman")
                put("debtId", "debt_fictional_lend_nabila")
                put("notes", "Fictional partial installment (৳3,000 of ৳8,000) received from Nabila Rahman [Debt:debt_fictional_lend_nabila]")
                put("isRecurring", false)
            })

            // 15. Repayment received for money lent - Full settlement (from Rafi Hasan into Bank)
            put(JSONObject().apply {
                put("id", "tx_fictional_collect_rafi")
                put("type", "INCOME")
                put("amount", 12000.0)
                put("title", "Full Settlement Received for Loan")
                put("category", "Money Received")
                put("subcategory", "Settlement Return")
                put("accountId", "acc_fictional_bank")
                put("timestamp", 1793059200000L) // Oct 26, 2026
                put("payeeOrPayer", "Rafi Hasan")
                put("debtId", "debt_fictional_lend_rafi")
                put("notes", "Fictional complete settlement (৳12,000) received from Rafi Hasan [Debt:debt_fictional_lend_rafi]")
                put("isRecurring", false)
            })

            // 16. Expense on Credit Card (Electronics)
            put(JSONObject().apply {
                put("id", "tx_fictional_exp_electronics")
                put("type", "EXPENSE")
                put("amount", 4500.0)
                put("title", "Wireless Bluetooth Headphones")
                put("category", "Shopping")
                put("subcategory", "Electronics")
                put("accountId", "acc_fictional_card")
                put("timestamp", 1793059200000L) // Oct 26, 2026
                put("payeeOrPayer", "Gadget Galaxy")
                put("notes", "Fictional electronics purchase on credit card")
                put("isRecurring", false)
            })

            // 17. Expense on MFS (Health / Pharmacy)
            put(JSONObject().apply {
                put("id", "tx_fictional_exp_pharmacy")
                put("type", "EXPENSE")
                put("amount", 1200.0)
                put("title", "Family Health & First Aid Supplies")
                put("category", "Daily Essentials")
                put("subcategory", "Medicines")
                put("accountId", "acc_fictional_mfs")
                put("timestamp", 1793145600000L) // Oct 27, 2026
                put("payeeOrPayer", "MedPlus Pharmacy")
                put("notes", "Fictional first-aid and medical supplies")
                put("isRecurring", false)
            })
        }
        root.put("transactions", txArr)

        // Fictional Debts with reconciled principal, paid amounts, and outstanding balance
        val debtArr = JSONArray().apply {
            // 1. Borrowed from Arian Chowdhury (Active, Partial Repaid: 18,000 - 6,000 = 12,000)
            put(JSONObject().apply {
                put("id", "debt_fictional_borrow_arian")
                put("personName", "Arian Chowdhury")
                put("amount", 12000.0)
                put("totalAmount", 18000.0)
                put("paidAmount", 6000.0)
                put("type", "BORROWED")
                put("dueDate", "2026-12-31")
                put("notes", "Fictional medical emergency support loan")
                put("reason", "Medical Emergency")
                put("isSettled", false)
                put("accountId", "acc_fictional_bank")
                put("colorHex", "#EF4444")
            })

            // 2. Borrowed from Farhan Kabir (Settled: 10,000 - 10,000 = 0.0)
            put(JSONObject().apply {
                put("id", "debt_fictional_borrow_farhan")
                put("personName", "Farhan Kabir")
                put("amount", 0.0)
                put("totalAmount", 10000.0)
                put("paidAmount", 10000.0)
                put("type", "BORROWED")
                put("dueDate", "2026-10-31")
                put("notes", "Fictional bridge loan - fully settled")
                put("reason", "Project Bridge")
                put("isSettled", true)
                put("accountId", "acc_fictional_bank")
                put("colorHex", "#10B981")
            })

            // 3. Lent to Nabila Rahman (Active, Partial Repaid: 8,000 - 3,000 = 5,000)
            put(JSONObject().apply {
                put("id", "debt_fictional_lend_nabila")
                put("personName", "Nabila Rahman")
                put("amount", 5000.0)
                put("totalAmount", 8000.0)
                put("paidAmount", 3000.0)
                put("type", "LENT")
                put("dueDate", "2026-11-30")
                put("notes", "Fictional college education assistance")
                put("reason", "Semester Fees")
                put("isSettled", false)
                put("accountId", "acc_fictional_mfs")
                put("colorHex", "#3B82F6")
            })

            // 4. Lent to Rafi Hasan (Settled: 12,000 - 12,000 = 0.0)
            put(JSONObject().apply {
                put("id", "debt_fictional_lend_rafi")
                put("personName", "Rafi Hasan")
                put("amount", 0.0)
                put("totalAmount", 12000.0)
                put("paidAmount", 12000.0)
                put("type", "LENT")
                put("dueDate", "2026-10-31")
                put("notes", "Fictional friendly assistance - fully repaid")
                put("reason", "Friendly Loan")
                put("isSettled", true)
                put("accountId", "acc_fictional_mfs")
                put("colorHex", "#10B981")
            })
        }
        root.put("debts", debtArr)

        // Fictional Budgets with valid category names
        val bgtArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "bgt_fictional_essentials")
                put("categoryName", "Daily Essentials")
                put("monthlyLimit", 15000.0)
                put("monthYear", "2026-10")
                put("isOverall", false)
                put("rolloverEnabled", false)
            })
            put(JSONObject().apply {
                put("id", "bgt_fictional_eating")
                put("categoryName", "Eating Out")
                put("monthlyLimit", 6000.0)
                put("monthYear", "2026-10")
                put("isOverall", false)
                put("rolloverEnabled", false)
            })
            put(JSONObject().apply {
                put("id", "bgt_fictional_rent")
                put("categoryName", "Rent & Utilities")
                put("monthlyLimit", 20000.0)
                put("monthYear", "2026-10")
                put("isOverall", false)
                put("rolloverEnabled", false)
            })
        }
        root.put("budgets", bgtArr)

        // Fictional Savings Goal
        val goalArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "goal_fictional_emergency")
                put("name", "Fictional Emergency Reserve")
                put("targetAmount", 50000.0)
                put("savedAmount", 20000.0)
                put("themeColorHex", "#06B6D4")
                put("iconName", "savings")
                put("createdAt", 1791244800000L)
                put("isCompleted", false)
            })
        }
        root.put("savingsGoals", goalArr)

        return root.toString(2)
    }
}
