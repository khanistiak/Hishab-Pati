package com.example.data.backup

enum class BackupFormatType(val displayName: String, val displayNameBn: String) {
    MY_WALLET_CURRENT("Hishab Pati Backup", "হিসাব পাতি ব্যাকআপ"),
    CSV_EXPORT("CSV Spreadsheet Export", "সিএসভি স্প্রেডশিট এক্সপোর্ট"),
    MY_WALLET_CONVERTED_EXPEND("Hishab Pati (Expend Migrated)", "হিসাব পাতি (মাইগ্রেটেড)"),
    MY_WALLET_LEGACY("Legacy Backup", "লিগ্যাসি ব্যাকআপ"),
    EXPEND_LEGACY("Legacy Expend Backup", "লিগ্যাসি এক্সপেন্ড ব্যাকআপ"),
    UNKNOWN("Unknown Format", "অজানা ফরম্যাট")
}

enum class RestoreMode(val titleEn: String, val titleBn: String) {
    REPLACE_ALL("Replace All", "সব প্রতিস্থাপন করুন"),
    MERGE("Merge Data", "ডাটা মার্জ করুন")
}

enum class RestoreStage(
    val stepNumber: Int,
    val totalSteps: Int,
    val titleEn: String,
    val titleBn: String
) {
    READING_FILE(1, 11, "Reading backup file", "ব্যাকআপ ফাইল পড়া হচ্ছে"),
    VALIDATING_STRUCTURE(2, 11, "Validating backup structure", "ব্যাকআপ কাঠামো যাচাই করা হচ্ছে"),
    DETECTING_SCHEMA(3, 11, "Detecting schema version", "স্কিমা সংস্করণ শনাক্ত করা হচ্ছে"),
    MIGRATING_LEGACY(4, 11, "Migrating legacy backup data", "পুরানো ব্যাকআপ ডাটা মাইগ্রেট করা হচ্ছে"),
    VALIDATING_REFERENCES(5, 11, "Validating accounts and references", "অ্যাকাউন্ট ও রেফারেন্স যাচাই করা হচ্ছে"),
    PROCESSING_CATEGORIES(6, 11, "Processing categories and subcategories", "ক্যাটাগরি ও সাবক্যাটাগরি প্রসেস করা হচ্ছে"),
    PROCESSING_TRANSACTIONS(7, 11, "Processing transactions", "লেনদেন প্রসেস করা হচ্ছে"),
    PROCESSING_BUDGETS_DEBTS(8, 11, "Processing budgets and debts", "বাজেট ও ঋণ প্রসেস করা হচ্ছে"),
    SAVING_TO_DATABASE(9, 11, "Saving data to the database", "ডাটাবেসে সংরক্ষণ করা হচ্ছে"),
    REFRESHING_ANALYTICS(10, 11, "Refreshing dashboard and analytics", "ড্যাশবোর্ড ও অ্যানালিটিক্স রিফ্রেশ করা হচ্ছে"),
    VERIFYING_RESTORED_DATA(11, 11, "Verifying restored data", "পুনরুদ্ধারকৃত ডাটা যাচাই করা হচ্ছে")
}

enum class BackupErrorCode(
    val code: String,
    val titleEn: String,
    val titleBn: String,
    val messageEn: String,
    val messageBn: String
) {
    ERR_INVALID_EXTENSION(
        code = "ERR_INVALID_EXTENSION",
        titleEn = "Invalid Backup File",
        titleBn = "অবৈধ ব্যাকআপ ফাইল",
        messageEn = "The selected file is not a valid wallet backup (.json). Please select another file.",
        messageBn = "নির্বাচিত ফাইলটি বৈধ ওয়ালেট ব্যাকআপ (.json) নয়। অনুগ্রহ করে অন্য ফাইল নির্বাচন করুন।"
    ),
    ERR_EMPTY_OR_CORRUPTED(
        code = "ERR_EMPTY_OR_CORRUPTED",
        titleEn = "Corrupted Backup File",
        titleBn = "ক্ষতিগ্রস্ত ব্যাকআপ ফাইল",
        messageEn = "The selected file is not a valid wallet backup. Please select another file.",
        messageBn = "নির্বাচিত ফাইলটি বৈধ ওয়ালেট ব্যাকআপ নয়। অনুগ্রহ করে অন্য ফাইল নির্বাচন করুন।"
    ),
    ERR_INVALID_JSON(
        code = "ERR_INVALID_JSON",
        titleEn = "Invalid Backup",
        titleBn = "অবৈধ ব্যাকআপ",
        messageEn = "The selected file is not a valid wallet backup. Please select another file.",
        messageBn = "নির্বাচিত ফাইলটি বৈধ ওয়ালেট ব্যাকআপ নয়। অনুগ্রহ করে অন্য ফাইল নির্বাচন করুন।"
    ),
    ERR_UNSUPPORTED_VERSION(
        code = "ERR_UNSUPPORTED_VERSION",
        titleEn = "Unsupported Version",
        titleBn = "অসমর্থিত সংস্করণ",
        messageEn = "This backup was created using an unsupported version. Please update the app or use a compatible backup.",
        messageBn = "এই ব্যাকআপটি একটি অসমর্থিত সংস্করণ ব্যবহার করে তৈরি করা হয়েছে। অনুগ্রহ করে অ্যাপ আপডেট করুন অথবা উপযুক্ত ব্যাকআপ ব্যবহার করুন।"
    ),
    ERR_MISSING_REQUIRED_FIELDS(
        code = "ERR_MISSING_REQUIRED_FIELDS",
        titleEn = "Incomplete Backup Format",
        titleBn = "অসম্পূর্ণ ব্যাকআপ ফরম্যাট",
        messageEn = "The selected file is not a valid wallet backup or is missing required fields. Your existing wallet data has not been changed.",
        messageBn = "নির্বাচিত ফাইলটিতে প্রয়োজনীয় ওয়ালেট তথ্য নেই। আপনার বর্তমান ডাটা অপরিবর্তিত রয়েছে।"
    ),
    ERR_REFERENTIAL_INTEGRITY(
        code = "ERR_REFERENTIAL_INTEGRITY",
        titleEn = "Data Integrity Issue",
        titleBn = "ডাটা অখণ্ডতা সমস্যা",
        messageEn = "Some records contain invalid account or category references. Your existing wallet data has not been changed.",
        messageBn = "কিছু রেকর্ডে অবৈধ অ্যাকাউন্ট বা ক্যাটাগরি রেফারেন্স রয়েছে। আপনার বর্তমান ডাটা অপরিবর্তিত রয়েছে।"
    ),
    ERR_INVALID_FINANCIAL_DATA(
        code = "ERR_INVALID_FINANCIAL_DATA",
        titleEn = "Data Integrity Issue",
        titleBn = "ডাটা অখণ্ডতা সমস্যা",
        messageEn = "Some records contain invalid amounts or dates. Your existing wallet data has not been changed.",
        messageBn = "কিছু রেকর্ডে অবৈধ টাকার পরিমাণ বা তারিখ রয়েছে। আপনার বর্তমান ডাটা অপরিবর্তিত রয়েছে।"
    ),
    ERR_DATABASE_WRITE_FAILED(
        code = "ERR_DATABASE_WRITE_FAILED",
        titleEn = "Restore Failed",
        titleBn = "পুনরুদ্ধার ব্যর্থ হয়েছে",
        messageEn = "Restoration could not be completed. Your existing data has been preserved. Please try again.",
        messageBn = "পুনরুদ্ধার সম্পন্ন করা যায়নি। আপনার বর্তমান ডাটা সুরক্ষিত রাখা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
    ),
    ERR_STORAGE_READ(
        code = "ERR_STORAGE_READ",
        titleEn = "Storage Read Error",
        titleBn = "ফাইল পড়তে সমস্যা",
        messageEn = "Could not read the selected backup file. Please verify file permissions and try again.",
        messageBn = "নির্বাচিত ব্যাকআপ ফাইলটি পড়া যায়নি। অনুগ্রহ করে আবার চেষ্টা করুন।"
    ),
    ERR_INTERRUPTED(
        code = "ERR_INTERRUPTED",
        titleEn = "Restoration Interrupted",
        titleBn = "পুনরুদ্ধার বাধাগ্রস্ত হয়েছে",
        messageEn = "Restoration could not be completed because it was interrupted. Your existing data has been preserved.",
        messageBn = "পুনরুদ্ধার প্রক্রিয়াটি বাধাগ্রস্ত হয়েছে। আপনার বর্তমান ডাটা সুরক্ষিত রাখা হয়েছে।"
    ),
    ERR_UNEXPECTED(
        code = "ERR_UNEXPECTED",
        titleEn = "Restore Failed",
        titleBn = "পুনরুদ্ধার ব্যর্থ হয়েছে",
        messageEn = "Restoration could not be completed. Your existing data has been preserved. Please try again.",
        messageBn = "পুনরুদ্ধার সম্পন্ন করা যায়নি। আপনার বর্তমান ডাটা সুরক্ষিত রাখা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
    )
}

data class BackupDiagnosticError(
    val errorCode: BackupErrorCode,
    val failedStage: RestoreStage,
    val technicalDetails: String,
    val existingDataPreserved: Boolean = true,
    val customMessageEn: String? = null
) {
    val userTitleEn: String get() = errorCode.titleEn
    val userTitleBn: String get() = errorCode.titleBn
    val userMessageEn: String get() = customMessageEn ?: errorCode.messageEn
    val userMessageBn: String get() = errorCode.messageBn
}

class BackupRestoreException(
    val diagnostic: BackupDiagnosticError,
    cause: Throwable? = null
) : Exception(diagnostic.userMessageEn, cause)

data class BackupPreview(
    val appName: String,
    val schemaVersion: Int,
    val backupDate: String,
    val accountsCount: Int,
    val transactionsCount: Int,
    val categoriesCount: Int,
    val budgetsCount: Int,
    val debtsCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null,
    val subcategoriesCount: Int = 0,
    val savingsGoalsCount: Int = 0,
    val hasSettings: Boolean = false,
    val fileName: String = "wallet_backup.json",
    val fileSizeBytes: Long = 0L,
    val formatType: BackupFormatType = BackupFormatType.MY_WALLET_CURRENT,
    val detectedDuplicatesCount: Int = 0,
    val requiresMigration: Boolean = false,
    val diagnosticError: BackupDiagnosticError? = null
) {
    val totalRecordsCount: Int
        get() = accountsCount + transactionsCount + categoriesCount + subcategoriesCount + budgetsCount + debtsCount + savingsGoalsCount
}

data class RestoreProgress(
    val stage: RestoreStage,
    val percentage: Int,
    val isIndeterminate: Boolean = false,
    val processedRecords: Int = 0,
    val totalRecords: Int = 0,
    val statusDetail: String = ""
)

data class RestoreResultSummary(
    val mode: RestoreMode,
    val formatType: BackupFormatType,
    val schemaVersion: Int,
    val totalRestoredRecords: Int,
    val accountsRestored: Int,
    val transactionsRestored: Int,
    val categoriesRestored: Int,
    val subcategoriesRestored: Int,
    val budgetsRestored: Int,
    val debtsRestored: Int,
    val savingsGoalsRestored: Int = 0,
    val skippedDuplicatesCount: Int = 0,
    val migratedRecordsCount: Int = 0,
    val verifiedTotalIncome: Double = 0.0,
    val verifiedTotalExpense: Double = 0.0,
    val verifiedCurrentMonthExpense: Double = 0.0,
    val verifiedNetWorth: Double = 0.0,
    val isFinancialIntegrityVerified: Boolean = true,
    val preRestoreBackupCreated: Boolean = true
)

sealed class RestoreUiState {
    data object Idle : RestoreUiState()
    data class Step1Preview(
        val preview: BackupPreview,
        val selectedMode: RestoreMode = RestoreMode.REPLACE_ALL
    ) : RestoreUiState()

    data class Step2Confirm(
        val preview: BackupPreview,
        val mode: RestoreMode
    ) : RestoreUiState()

    data class Step3Restoring(
        val progress: RestoreProgress,
        val mode: RestoreMode
    ) : RestoreUiState()

    data class Step4Success(
        val summary: RestoreResultSummary
    ) : RestoreUiState()

    data class Step4Error(
        val error: BackupDiagnosticError,
        val fileName: String = ""
    ) : RestoreUiState()
}
