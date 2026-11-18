package com.example.ui.util

import com.example.data.model.AppLanguage

object AppStrings {

    fun toBanglaDigits(input: String): String {
        return CurrencyFormatter.convertToBanglaDigits(input)
    }

    fun toBanglaDigits(number: Number, language: AppLanguage): String {
        val str = number.toString()
        return if (language == AppLanguage.BANGLA) CurrencyFormatter.convertToBanglaDigits(str) else str
    }

    fun t(key: String, language: AppLanguage): String {
        val isBn = language == AppLanguage.BANGLA
        return when (key) {
            // Navigation
            "nav_home" -> if (isBn) "হোম" else "Home"
            "nav_activity" -> if (isBn) "অ্যাক্টিভিটি" else "Activity"
            "nav_plan" -> if (isBn) "পরিকল্পনা" else "Plan"
            "nav_insights" -> if (isBn) "ইনসাইটস" else "Insights"

            // Common actions
            "add" -> if (isBn) "যোগ করুন" else "Add"
            "save" -> if (isBn) "সংরক্ষণ করুন" else "Save"
            "done" -> if (isBn) "সম্পন্ন" else "Done"
            "cancel" -> if (isBn) "বাতিল" else "Cancel"
            "back" -> if (isBn) "পেছনে" else "Back"
            "edit" -> if (isBn) "সম্পাদনা" else "Edit"
            "delete" -> if (isBn) "মুছুন" else "Delete"
            "search" -> if (isBn) "অনুসন্ধান করুন" else "Search"
            "all" -> if (isBn) "সব" else "All"
            "today" -> if (isBn) "আজ" else "Today"
            "yesterday" -> if (isBn) "গতকাল" else "Yesterday"
            "view_all" -> if (isBn) "সব দেখুন" else "View all"
            "filter" -> if (isBn) "ফিল্টার" else "Filter"
            "sort" -> if (isBn) "সাজান" else "Sort"
            "all_time" -> if (isBn) "সর্বকালের" else "All Time"

            // Financial terms
            "expenses" -> if (isBn) "ব্যয়" else "Expenses"
            "income" -> if (isBn) "আয়" else "Income"
            "transfers" -> if (isBn) "স্থানান্তর" else "Transfers"
            "balance" -> if (isBn) "ব্যালেন্স" else "Balance"
            "net_worth" -> if (isBn) "মোট সম্পদ" else "Net Worth"
            "total_balance" -> if (isBn) "সর্বমোট ব্যালেন্স" else "Total Balance"
            "accounts" -> if (isBn) "অ্যাকাউন্টস" else "Accounts"
            "all_accounts" -> if (isBn) "সকল অ্যাকাউন্ট" else "All accounts"
            "recent_transactions" -> if (isBn) "সাম্প্রতিক লেনদেন" else "Recent Transactions"
            "money_in" -> if (isBn) "মোট আয়" else "Money in"
            "money_out" -> if (isBn) "মোট ব্যয়" else "Money out"
            "net" -> if (isBn) "নেট" else "Net"
            "still_due" -> if (isBn) "বাকি আছে" else "Still due"
            "payable" -> if (isBn) "ধার নেওয়া" else "Borrowed"
            "receivable" -> if (isBn) "ধার দেওয়া" else "Lent"
            "paid" -> if (isBn) "পরিশোধিত" else "Paid"
            "remaining" -> if (isBn) "অবশিষ্ট" else "Remaining"
            "settled" -> if (isBn) "পরিশোধ সম্পন্ন" else "Settled"
            "active" -> if (isBn) "চলমান" else "Active"
            "total_debt" -> if (isBn) "সর্বমোট ঋণ" else "Total Debt"
            "amount_paid" -> if (isBn) "পরিশোধকৃত অর্থ" else "Amount Paid"
            "owed_to_you" -> if (isBn) "আপনি পাবেন" else "Owed to you"
            "you_owe" -> if (isBn) "আপনার দেনা" else "You owe"
            "summary" -> if (isBn) "সংক্ষেপ" else "Summary"
            "due_date" -> if (isBn) "পরিশোধের শেষ তারিখ" else "Due Date"
            "no_due_date" -> if (isBn) "কোন নির্দিষ্ট তারিখ নেই" else "No due date"
            "interest_rate" -> if (isBn) "সুদের হার" else "Interest Rate"
            "notes" -> if (isBn) "নোট" else "Notes"
            "no_notes" -> if (isBn) "কোন নোট নেই" else "No notes"
            "borrowed_on" -> if (isBn) "ঋণ গ্রহণের তারিখ" else "Borrowed on"
            "lent_on" -> if (isBn) "ঋণ প্রদানের তারিখ" else "Lent on"
            "pay_collect" -> if (isBn) "পরিশোধ / আদায় গ্রহণ" else "Pay / Record Collection"
            "mark_settled" -> if (isBn) "পরিশোধিত হিসেবে চিহ্নিত করুন" else "Mark as Settled"
            "no_transactions_yet" -> if (isBn) "এখনও কোন লেনদেন রেকর্ড করা হয়নি" else "No transactions recorded yet"

            // Notifications
            "notifications" -> if (isBn) "বিজ্ঞপ্তিসমূহ" else "Notifications"
            "unread" -> if (isBn) "অপঠিত" else "Unread"
            "bills" -> if (isBn) "বিলসমূহ" else "Bills"
            "insights" -> if (isBn) "ইনসাইটস" else "Insights"
            "alerts" -> if (isBn) "সতর্কতা" else "Alerts"
            "mark_all_read" -> if (isBn) "সব পঠিত চিহ্নিত করুন" else "Mark all as read"
            "no_notifications" -> if (isBn) "কোন নতুন বিজ্ঞপ্তি নেই" else "No notifications yet"

            // Plan tab
            "plan_title" -> if (isBn) "ব্যক্তিগত অর্থায়ন" else "Personal Finance"
            "savings_goals" -> if (isBn) "সঞ্চয় লক্ষ্য" else "Savings goals"
            "add_goal" -> if (isBn) "+ লক্ষ্য যোগ" else "+ Add Goal"
            "loans_debt" -> if (isBn) "ঋণ ও দেনা" else "Loans & debt"
            "explore_all" -> if (isBn) "সব দেখুন" else "Explore all"
            "monthly_budget" -> if (isBn) "মাসিক বাজেট" else "Monthly budget"
            "manage_budget" -> if (isBn) "বাজেট ব্যবস্থাপনা" else "Manage"
            "tools" -> if (isBn) "টুলস" else "Tools"
            "converter" -> if (isBn) "কনভার্টার" else "Converter"
            "networth" -> if (isBn) "মোট সম্পদ" else "Networth"
            "monthly_recap" -> if (isBn) "মাসিক রিক্যাপ" else "Monthly recap"
            "spending_comparison" -> if (isBn) "ব্যয় তুলনা" else "Spending comparison"
            "income_tracker" -> if (isBn) "আয় ট্র্যাকার" else "Income tracker"
            "calendar" -> if (isBn) "ক্যালেন্ডার" else "Calendar"

            // Debt details
            "debt_details" -> if (isBn) "ঋণের বিস্তারিত" else "Debt Details"
            "tentative_due" -> if (isBn) "সম্ভাব্য পরিশোধের তারিখ" else "Tentative due"
            "borrow_more" -> if (isBn) "আরও ঋণ গ্রহণ" else "Borrow More"
            "lend_more" -> if (isBn) "আরও ঋণ প্রদান" else "Lend More"
            "add_interest" -> if (isBn) "সুদ যুক্ত করুন" else "Add Interest"
            "record_payment" -> if (isBn) "পরিশোধ রেকর্ড করুন" else "Record Payment"

            // Activity filters & sort
            "sort_newest" -> if (isBn) "নতুনতম প্রথমে" else "Newest first"
            "sort_oldest" -> if (isBn) "পুরাতন প্রথমে" else "Oldest first"
            "sort_highest" -> if (isBn) "সর্বোচ্চ পরিমাণ" else "Highest amount"
            "sort_lowest" -> if (isBn) "সর্বনিম্ন পরিমাণ" else "Lowest amount"
            "filter_account" -> if (isBn) "অ্যাকাউন্ট অনুযায়ী ফিল্টার" else "Filter by Account"
            "filter_category" -> if (isBn) "ক্যাটাগরি অনুযায়ী ফিল্টার" else "Filter by Category"
            "filter_date_range" -> if (isBn) "তারিখ সীমা" else "Date Range"
            "this_month" -> if (isBn) "এই মাস" else "This Month"
            "last_month" -> if (isBn) "গত মাস" else "Last Month"
            "this_year" -> if (isBn) "এই বছর" else "This Year"
            "custom_range" -> if (isBn) "কাস্টম রেঞ্জ" else "Custom Range"
            "apply_filters" -> if (isBn) "ফিল্টার প্রয়োগ করুন" else "Apply Filters"
            "reset_filters" -> if (isBn) "রিসেট করুন" else "Reset Filters"

            // Shortcuts & Customize Home
            "transfer" -> if (isBn) "ট্রান্সফার" else "Transfer"
            "scan_receipt" -> if (isBn) "রসিদ স্ক্যান" else "Scan receipt"
            "expense" -> if (isBn) "ব্যয়" else "Expense"
            "voice_entry" -> if (isBn) "ভয়েস এন্ট্রি" else "Voice Entry"
            "customize_home" -> if (isBn) "হোম কাস্টমাইজ করুন" else "Customize Home"
            "card_display_style" -> if (isBn) "কার্ড প্রদর্শনের ধরণ" else "Card Display Style"
            "stacked_cards" -> if (isBn) "স্ট্যাকড কার্ড" else "Stacked Cards"
            "carousel_cards" -> if (isBn) "হরাইজন্টাল স্লাইডার" else "Horizontal Carousel"
            "toggle_sections" -> if (isBn) "সেকশন প্রদর্শন বা গোপন" else "Toggle Sections"
            "top_spending" -> if (isBn) "শীর্ষ ব্যয়" else "Top Spending"
            "spending_by_day" -> if (isBn) "দিনভিত্তিক ব্যয়" else "Spending by Day"
            "financial_health" -> if (isBn) "আর্থিক স্বাস্থ্য" else "Financial Health"
            "quick_shortcuts" -> if (isBn) "কুইক শর্টকাট" else "Quick Shortcuts"

            // Calendar
            "month" -> if (isBn) "মাস" else "Month"
            "agenda" -> if (isBn) "এজেন্ডা" else "Agenda"
            "spent_bills" -> if (isBn) "ব্যয় ও বিল" else "Spent & bills"
            "transfers_emi" -> if (isBn) "স্থানান্তর ও ইএমআই" else "Transfers & EMI"
            "card_dates" -> if (isBn) "কার্ডের তারিখ" else "Card dates"

            // Currency Converter
            "currency_converter" -> if (isBn) "কারেন্সি কনভার্টার" else "Currency converter"
            "quick_amount" -> if (isBn) "দ্রুত টাকার পরিমাণ" else "Quick amount"
            "popular_currencies" -> if (isBn) "জনপ্রিয় মুদ্রাসমূহ" else "Popular currencies"
            "from" -> if (isBn) "হতে" else "From"
            "to" -> if (isBn) "প্রতি" else "To"

            // Settings
            "settings_title" -> if (isBn) "সেটিংস" else "Settings"
            "profile" -> if (isBn) "প্রোফাইল" else "Profile"
            "preferences" -> if (isBn) "পছন্দসমূহ" else "Preferences"
            "cloud_sync" -> if (isBn) "ক্লাউড সিঙ্ক" else "Cloud Sync"
            "skip_guest" -> if (isBn) "লগইন ছাড়াই চালিয়ে যান" else "Continue as Guest"
            "sign_in_google" -> if (isBn) "গুগল দিয়ে সাইন ইন করুন" else "Sign in with Google"

            // Budget & Limits
            "total_spent" -> if (isBn) "সর্বমোট ব্যয়" else "Total Spent"
            "budget" -> if (isBn) "বাজেট" else "Budget"
            "overbudget" -> if (isBn) "বাজেট অতিক্রম" else "Overbudget"
            "underbudget" -> if (isBn) "বাজেটের ভেতরে" else "Under Budget"
            "remaining_budget" -> if (isBn) "অবশিষ্ট বাজেট" else "Remaining Budget"
            "daily_allowance" -> if (isBn) "দৈনিক বরাদ্দ" else "Daily Allowance"
            "days_left" -> if (isBn) "দিন বাকি" else "days left"
            "set_budget" -> if (isBn) "বাজেট সেট করুন" else "Set Limit"
            "details" -> if (isBn) "বিস্তারিত" else "Details"
            "of" -> if (isBn) "এর" else "of"
            "tap_to_view" -> if (isBn) "দেখতে ট্যাপ করুন" else "Tap to view"
            "no_budget_set" -> if (isBn) "কোন বাজেট নির্ধারিত নেই" else "No budget set for this month"
            "set_monthly_budget" -> if (isBn) "মাসিক বাজেট নির্ধারণ" else "Set Monthly Budget"
            "monthly_budget_desc" -> if (isBn) "আপনার মাসিক খরচের একটি সীমা ঠিক করুন যাতে অতিরিক্ত ব্যয় না হয়" else "Set a monthly spending limit to control expenses"

            // Savings & Goals
            "savings_progress" -> if (isBn) "সঞ্চয়ের অগ্রগতি" else "Savings Progress"
            "target" -> if (isBn) "লক্ষ্যমাত্রা" else "Target"
            "saved" -> if (isBn) "সঞ্চিত" else "Saved"
            "no_savings_goals" -> if (isBn) "কোন সঞ্চয় লক্ষ্য যুক্ত করা হয়নি" else "No savings goals created yet"
            "create_goal_prompt" -> if (isBn) "আপনার পরবর্তী লক্ষ্য (ভ্রমণ, জরুরি তহবিল, গ্যাজেট) যুক্ত করুন" else "Start saving for vacations, emergency funds, or gadgets"
            "no_debts" -> if (isBn) "কোন ঋণ বা দেনার রেকর্ড নেই" else "No debts or loans tracked yet"
            "track_debts_prompt" -> if (isBn) "কাউকে ধার দিয়েছেন বা কারো কাছ থেকে ঋণ নিয়েছেন? এখানে ট্র্যাক করুন" else "Lent or borrowed money? Track balances and repayments here"
            "add_debt" -> if (isBn) "+ ঋণ/দেনা যোগ" else "+ Add Debt"

            // Transaction Form
            "wallet_account" -> if (isBn) "ওয়ালেট / অ্যাকাউন্ট" else "Wallet / Account"
            "from_wallet" -> if (isBn) "উৎস ওয়ালেট" else "From Wallet"
            "to_wallet" -> if (isBn) "গন্তব্য ওয়ালেট" else "To Wallet"
            "category" -> if (isBn) "ক্যাটাগরি" else "Category"
            "subcategory" -> if (isBn) "সাবক্যাটাগরি" else "Subcategory"
            "transaction_name" -> if (isBn) "লেনদেনের শিরোনাম / বিবরণ" else "Transaction Name / Description"
            "date_time" -> if (isBn) "তারিখ ও সময়" else "Date & Time"
            "select_category" -> if (isBn) "ক্যাটাগরি নির্বাচন করুন" else "Select Category"
            "income_categories" -> if (isBn) "আয়ের ক্যাটাগরি" else "Income Categories"
            "expense_categories" -> if (isBn) "ব্যয়ের ক্যাটাগরি" else "Expense Categories"
            "type" -> if (isBn) "ধরণ" else "Type"
            "account" -> if (isBn) "অ্যাকাউন্ট" else "Account"
            "amount" -> if (isBn) "টাকার পরিমাণ" else "Amount"
            "optional" -> if (isBn) "ঐচ্ছিক" else "Optional"
            "required" -> if (isBn) "প্রয়োজনীয়" else "Required"

            // Accounts & Net Worth
            "assets" -> if (isBn) "সম্পদ" else "Assets"
            "liabilities" -> if (isBn) "দায়" else "Liabilities"
            "cash" -> if (isBn) "নগদ টাকা" else "Cash"
            "bank" -> if (isBn) "ব্যাংক" else "Bank"
            "mobile_banking" -> if (isBn) "মোবাইল ব্যাংকিং" else "Mobile Banking"
            "credit_card" -> if (isBn) "ক্রেডিট কার্ড" else "Credit Card"
            "savings" -> if (isBn) "সঞ্চয়" else "Savings"
            "investment" -> if (isBn) "বিনিয়োগ" else "Investment"
            "other" -> if (isBn) "অন্যান্য" else "Other"
            "add_account" -> if (isBn) "অ্যাকাউন্ট যোগ করুন" else "Add Account"
            "edit_account" -> if (isBn) "অ্যাকাউন্ট সম্পাদনা" else "Edit Account"
            "account_name" -> if (isBn) "অ্যাকাউন্টের নাম" else "Account Name"
            "account_type" -> if (isBn) "অ্যাকাউন্টের ধরণ" else "Account Type"
            "initial_balance" -> if (isBn) "প্রারম্ভিক ব্যালেন্স" else "Initial Balance"

            // Categories & Settings
            "all_categories" -> if (isBn) "সকল ক্যাটাগরি" else "All Categories"
            "customize_categories" -> if (isBn) "ক্যাটাগরি পরিচালনা" else "Manage Categories"
            "about_app" -> if (isBn) "হিসাব পাতি সম্পর্কে" else "About Hishab Pati"
            "app_description" -> if (isBn) "হিসাব পাতি একটি আধুনিক ও সুরক্ষিত ব্যক্তিগত অর্থ ব্যবস্থাপনা অ্যাপ। সম্পূর্ণ অফলাইনে আপনার আয়, ব্যয়, ঋণ ও বাজেট নিয়ন্ত্রণ করুন।" else "Hishab Pati is a modern, secure personal finance app. Manage your income, expenses, debts, and budgets offline with ease."
            "version" -> if (isBn) "সংস্করণ" else "Version"
            "developer" -> if (isBn) "ডেভেলপার" else "Developer"
            "theme" -> if (isBn) "থিম" else "Theme"
            "language" -> if (isBn) "ভাষা" else "Language"
            "currency" -> if (isBn) "মুদ্রা" else "Currency"
            "backup_restore" -> if (isBn) "ব্যাকআপ ও রিস্টোর" else "Backup & Restore"
            "export_backup" -> if (isBn) "ব্যাকআপ তৈরি করুন" else "Create Backup"
            "import_backup" -> if (isBn) "ব্যাকআপ রিস্টোর করুন" else "Restore Backup"
            "clear_all_data" -> if (isBn) "সকল ডেটা মুছে ফেলুন" else "Clear All Data"

            // App Branding
            "app_name" -> if (isBn) "হিসাব পাতি" else "Hishab Pati"
            "app_tagline" -> if (isBn) "আপনার অর্থ, আপনার নিয়ন্ত্রণ।" else "Your Money, Your Control."
            "app_developer" -> "Khan Istiak Shahabuddin"
            "app_footer" -> if (isBn) "বাংলাদেশে তৈরি ❤️" else "Made with ❤️ in Bangladesh"

            else -> key
        }
    }
}
