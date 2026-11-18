package com.example.data.local

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.SubcategoryEntity
import com.example.data.local.entity.TransactionEntity

object InitialData {

    /**
     * Built-in default categories for new installations, organized strictly by transaction type:
     * Expense (12 standard categories)
     * Income (5 standard categories)
     * Transfer (3 standard categories)
     *
     * Rules:
     * - Preserves standard IDs for database and backup compatibility.
     * - Uses appropriate Material icons and distinctive colors.
     * - Gives distinct sortOrder bands per type (Expense: 0+, Income: 1000+, Transfer: 2000+)
     *   so order persistence across tabs and app restarts is 100% deterministic.
     */
    fun getDefaultCategories(): Pair<List<CategoryEntity>, List<SubcategoryEntity>> {
        val expenseCats = listOf(
            Triple("cat_food_dining", "Food & Dining", "খাবার ও ডাইনিং") to listOf(
                "Restaurant" to "রেস্তোরাঁ",
                "Fast Food" to "ফাস্ট ফুড",
                "Cafes & Coffee" to "কফি ও পানীয়",
                "Street Food" to "স্ট্রিট ফুড"
            ),
            Triple("cat_shopping", "Shopping", "কেনাকাটা") to listOf(
                "Clothing" to "পোশাক",
                "Electronics" to "ইলেকট্রনিক্স",
                "Home & Kitchen" to "গৃহস্থালী সামগ্রী",
                "Footwear" to "জুতা"
            ),
            Triple("cat_transport", "Transportation", "যাতায়াত") to listOf(
                "Fuel" to "জ্বালানী",
                "Public Transit" to "বাস ও ট্রেন",
                "Ride Sharing" to "রাইড শেয়ারিং",
                "Tolls & Parking" to "টোল ও পার্কিং"
            ),
            Triple("cat_rent", "Rent & Utilities", "ভাড়া ও ইউটিলিটি") to listOf(
                "House Rent" to "বাড়ি ভাড়া",
                "Electricity" to "বিদ্যুৎ",
                "Gas" to "গ্যাস",
                "Water" to "পানি",
                "Internet" to "ইন্টারনেট"
            ),
            Triple("cat_health_medical", "Health & Medical", "স্বাস্থ্য ও চিকিৎসা") to listOf(
                "Doctor & Visits" to "ডাক্তার",
                "Medicines" to "ওষুধ",
                "Diagnostic & Tests" to "টেস্ট ও পরীক্ষা",
                "Hospital" to "হাসপাতাল"
            ),
            Triple("cat_entertainment", "Entertainment", "বিনোদন") to listOf(
                "Movies & Shows" to "চলচ্চিত্র ও নাটক",
                "Games & Gaming" to "গেমিং",
                "Streaming Services" to "স্ট্রিমিং সাবস্ক্রিপশন",
                "Concerts & Events" to "কনসার্ট ও ইভেন্ট"
            ),
            Triple("cat_personal_care", "Personal Care", "ব্যক্তিগত যত্ন") to listOf(
                "Salon & Grooming" to "সেলুন ও রূপচর্চা",
                "Cosmetics & Toiletries" to "প্রসাধন সামগ্রী",
                "Spa & Wellness" to "স্পা ও স্বাস্থ্যচর্চা"
            ),
            Triple("cat_groceries", "Groceries", "মুদি বাজার") to listOf(
                "Produce & Vegetables" to "শাকসবজি ও ফল",
                "Fish & Meat" to "মাছ ও মাংস",
                "Dairy & Eggs" to "দুধ ও ডিম",
                "Pantry Staples" to "নিত্য প্রয়োজনীয় চাল-ডাল"
            ),
            Triple("cat_travel", "Travel", "ভ্রমণ") to listOf(
                "Flights & Long Distance" to "বিমান ও দূরপাল্লার টিকিট",
                "Hotel & Lodging" to "হোটেল ও থাকার ব্যবস্থা",
                "Sightseeing & Tours" to "দর্শনীয় স্থান",
                "Travel Shopping" to "ভ্রমণ কেনাকাটা"
            ),
            Triple("cat_family", "Family", "পরিবার") to listOf(
                "Parents Support" to "বাবা-মার খরচ",
                "Children & Childcare" to "সন্তানের খরচ",
                "Family Occasions" to "পারিবারিক অনুষ্ঠান",
                "Family Support" to "পারিবারিক সহায়তা"
            ),
            Triple("cat_debt_repayment", "Debt Repayment", "ঋণ পরিশোধ") to listOf(
                "Loan Principal" to "ঋণের আসল",
                "Installment (EMI)" to "কিস্তি (ইএমআই)",
                "Credit Card Payment" to "ক্রেডিট কার্ড বিল"
            ),
            Triple("cat_other_expenses", "Other Expenses", "অন্যান্য খরচ") to listOf(
                "General Miscellaneous" to "সাধারণ বিবিধ",
                "Donations & Charity" to "দান ও সদকা",
                "Bank & Transfer Fees" to "ব্যাংক ও ট্রান্সফার ফি"
            )
        )

        val incomeCats = listOf(
            Triple("cat_salary", "Salary", "বেতন") to listOf(
                "Monthly Salary" to "মাসিক বেতন",
                "Overtime" to "ওভারটাইম",
                "Allowances" to "ভাতা"
            ),
            Triple("cat_bonus", "Bonus", "বোনাস") to listOf(
                "Festival Bonus" to "উৎসব বোনাস",
                "Performance Bonus" to "পারফরম্যান্স বোনাস"
            ),
            Triple("cat_business", "Business", "ব্যবসা") to listOf(
                "Sales & Revenue" to "বিক্রয় আয়",
                "Client Projects" to "ক্লায়েন্ট প্রজেক্ট",
                "Consulting" to "পরামর্শ সেবা"
            ),
            Triple("cat_gifts_received", "Gifts Received", "প্রাপ্ত উপহার") to listOf(
                "Cash Gift" to "নগদ উপহার",
                "Occasion Gift" to "উপলক্ষ উপহার"
            ),
            Triple("cat_other_income", "Other Income", "অন্যান্য আয়") to listOf(
                "Interest & Profit" to "মুনাফা ও সুদ",
                "Rental Income" to "বাড়ি/জায়গা ভাড়া",
                "Refunds & Cashbacks" to "রিফান্ড ও ক্যাশব্যাক",
                "Miscellaneous" to "বিবিধ আয়"
            )
        )

        val transferCats = listOf(
            Triple("cat_wallet_transfer", "Wallet Transfer", "ওয়ালেট ট্রান্সফার") to listOf(
                "Mobile Financial Service" to "মোবাইল ফিন্যান্সিয়াল সার্ভিস (MFS)",
                "Cash to Wallet" to "নগদ থেকে ওয়ালেট",
                "Wallet to Cash" to "ওয়ালেট থেকে নগদ"
            ),
            Triple("cat_bank_transfer", "Bank Transfer", "ব্যাংক ট্রান্সফার") to listOf(
                "Account to Account" to "অ্যাকাউন্ট থেকে অ্যাকাউন্ট",
                "ATM Withdrawal" to "এটিএম উত্তোলন",
                "Cheque Deposit" to "চেক জমা"
            ),
            Triple("cat_other_transfer", "Other Transfer", "অন্যান্য ট্রান্সফার") to listOf(
                "Internal Account Transfer" to "অভ্যন্তরীণ স্থানান্তর",
                "Adjustment Transfer" to "সমন্বয় স্থানান্তর"
            )
        )

        val categories = mutableListOf<CategoryEntity>()
        val subcategories = mutableListOf<SubcategoryEntity>()

        // Expense categories: sortOrder 0..11
        var expenseIndex = 0
        expenseCats.forEach { (catInfo, subs) ->
            val (id, name, nameBn) = catInfo
            val (icon, color) = when (name) {
                "Food & Dining" -> "restaurant" to "#F59E0B"
                "Shopping" -> "shopping_bag" to "#EC4899"
                "Transportation" -> "directions_car" to "#6366F1"
                "Rent & Utilities" -> "home" to "#3B82F6"
                "Health & Medical" -> "medical_services" to "#EF4444"
                "Entertainment" -> "movie" to "#8B5CF6"
                "Personal Care" -> "face" to "#14B8A6"
                "Groceries" -> "shopping_cart" to "#10B981"
                "Travel" -> "flight" to "#06B6D4"
                "Family" -> "family_restroom" to "#F97316"
                "Debt Repayment" -> "receipt_long" to "#DC2626"
                else -> "category" to "#64748B"
            }

            categories.add(
                CategoryEntity(
                    id = id,
                    name = name,
                    nameBn = nameBn,
                    isIncome = false,
                    isTransfer = false,
                    isActive = true,
                    colorHex = color,
                    iconName = icon,
                    sortOrder = expenseIndex++
                )
            )

            subs.forEachIndexed { subIndex, (subName, subNameBn) ->
                subcategories.add(
                    SubcategoryEntity(
                        id = "${id}_sub_$subIndex",
                        categoryId = id,
                        name = subName,
                        nameBn = subNameBn,
                        sortOrder = subIndex
                    )
                )
            }
        }

        // Income categories: sortOrder 1000..1004
        var incomeIndex = 1000
        incomeCats.forEach { (catInfo, subs) ->
            val (id, name, nameBn) = catInfo
            val (icon, color) = when (name) {
                "Salary" -> "payments" to "#10B981"
                "Bonus" -> "savings" to "#059669"
                "Business" -> "trending_up" to "#2563EB"
                "Gifts Received" -> "card_giftcard" to "#EC4899"
                else -> "account_balance_wallet" to "#14B8A6"
            }

            categories.add(
                CategoryEntity(
                    id = id,
                    name = name,
                    nameBn = nameBn,
                    isIncome = true,
                    isTransfer = false,
                    isActive = true,
                    colorHex = color,
                    iconName = icon,
                    sortOrder = incomeIndex++
                )
            )

            subs.forEachIndexed { subIndex, (subName, subNameBn) ->
                subcategories.add(
                    SubcategoryEntity(
                        id = "${id}_sub_$subIndex",
                        categoryId = id,
                        name = subName,
                        nameBn = subNameBn,
                        sortOrder = subIndex
                    )
                )
            }
        }

        // Transfer categories: sortOrder 2000..2002
        var transferIndex = 2000
        transferCats.forEach { (catInfo, subs) ->
            val (id, name, nameBn) = catInfo
            val (icon, color) = when (name) {
                "Wallet Transfer" -> "wallet" to "#3B82F6"
                "Bank Transfer" -> "account_balance" to "#6366F1"
                else -> "swap_horiz" to "#0284C7"
            }

            categories.add(
                CategoryEntity(
                    id = id,
                    name = name,
                    nameBn = nameBn,
                    isIncome = false,
                    isTransfer = true,
                    isActive = true,
                    colorHex = color,
                    iconName = icon,
                    sortOrder = transferIndex++
                )
            )

            subs.forEachIndexed { subIndex, (subName, subNameBn) ->
                subcategories.add(
                    SubcategoryEntity(
                        id = "${id}_sub_$subIndex",
                        categoryId = id,
                        name = subName,
                        nameBn = subNameBn,
                        sortOrder = subIndex
                    )
                )
            }
        }

        return Pair(categories, subcategories)
    }

    fun getSampleAccounts(): List<AccountEntity> = emptyList()

    fun getSampleTransactions(): List<TransactionEntity> = emptyList()

    fun getSampleBudgets(): List<BudgetEntity> = emptyList()

    fun getSampleDebts(): List<DebtEntity> = emptyList()

    fun getSampleSavingsGoals(): List<SavingsGoalEntity> = emptyList()
}
