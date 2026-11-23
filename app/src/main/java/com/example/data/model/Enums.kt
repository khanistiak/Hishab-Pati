package com.example.data.model

enum class AccountType(val displayName: String, val displayNameBn: String) {
    BANK("Debit Card", "ডেবিট কার্ড"),
    CREDIT_CARD("Credit Card", "ক্রেডিট কার্ড"),
    CASH("Cash", "নগদ টাকা"),
    MFS("MFS", "মোবাইল ওয়ালেট"),
    OTHER("Others", "অন্যান্য")
}

enum class TransactionType(val displayName: String, val displayNameBn: String) {
    EXPENSE("Expense", "খরচ"),
    INCOME("Income", "আয়"),
    TRANSFER("Transfer", "স্থানান্তর"),
    LEND("Lend Money", "ধার দেওয়া"),
    BORROW("Borrow Money", "ধার নেওয়া"),
    CC_PAYMENT("Credit Card Payment", "ক্রেডিট কার্ড পরিশোধ")
}

enum class DebtType(val displayName: String, val displayNameBn: String) {
    LENT("You Lent", "আপনি ধার দিয়েছেন"),
    BORROWED("You Borrowed", "আপনি ধার নিয়েছেন")
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা")
}

enum class ThemeMode(val displayName: String, val displayNameBn: String) {
    AUTO("Auto", "স্বয়ংক্রিয়"),
    LIGHT("Light", "লাইট"),
    DARK("Dark", "ডার্ক")
}

enum class ThemeColorOption(val displayName: String, val primaryHex: Long, val secondaryHex: Long) {
    ROYAL_INDIGO("Royal Indigo", 0xFF4F46E5, 0xFF6366F1),
    INDIGO_NIGHT("Indigo Night", 0xFF4F46E5, 0xFF6366F1),
    ROYAL_VIOLET("Royal Violet", 0xFF7C3AED, 0xFF8B5CF6),
    EMERALD_GREEN("Emerald Green", 0xFF059669, 0xFF10B981),
    OCEAN_BLUE("Ocean Blue", 0xFF2563EB, 0xFF3B82F6),
    SUNSET_ORANGE("Sunset Orange", 0xFFEA580C, 0xFFF97316),
    ROSE_PINK("Rose Pink", 0xFFE11D48, 0xFFFB7185),
    AQUA_TEAL("Aqua Teal", 0xFF0D9488, 0xFF14B8A6),
    GOLDEN_AMBER("Golden Amber", 0xFFD97706, 0xFFF59E0B),
    CRIMSON_RED("Crimson Red", 0xFFDC2626, 0xFFEF4444),
    LIME_FRESH("Lime Fresh", 0xFF65A30D, 0xFF84CC16),
    SKY_CYAN("Sky Cyan", 0xFF0284C7, 0xFF38BDF8),
    CORAL_PEACH("Coral Peach", 0xFFF43F5E, 0xFFFB7185),
    GRAPHITE("Graphite", 0xFF334155, 0xFF475569)
}

enum class BackgroundTextureOption(val displayName: String) {
    AURORA("Aurora"),
    WAVES("Waves"),
    DOTS("Dots"),
    GRID("Grid"),
    SILK_LINES("Silk lines"),
    PLAIN("Plain"),
    COLOUR_MESH("Colour mesh")
}

enum class AccountSortOption(
    val displayName: String,
    val subtitle: String,
    val displayNameBn: String,
    val subtitleBn: String
) {
    CUSTOM("Custom Order", "Manual user arrangement", "কাস্টম ক্রম", "ব্যবহারকারীর নিজস্ব সাজানো ক্রম"),
    BALANCE_DESC("Highest Balance", "Largest balance first", "সর্বোচ্চ ব্যালেন্স", "বড় ব্যালেন্স প্রথমে"),
    BALANCE_ASC("Lowest Balance", "Smallest balance first", "সর্বনিম্ন ব্যালেন্স", "ছোট ব্যালেন্স প্রথমে"),
    NAME_ASC("Name (A to Z)", "Alphabetical ascending", "নাম (A থেকে Z)", "বর্ণানুক্রমিক আরোহী"),
    NAME_DESC("Name (Z to A)", "Alphabetical descending", "নাম (Z থেকে A)", "বর্ণানুক্রমিক অবরোহী"),
    TYPE("Account Type", "Bank, Wallet, Cash, Credit Card", "অ্যাকাউন্টের ধরণ", "ব্যাংক, ওয়ালেট, ক্যাশ, ক্রেডিট কার্ড");

    fun getDisplayName(language: AppLanguage): String =
        if (language == AppLanguage.BANGLA) displayNameBn else displayName

    fun getSubtitle(language: AppLanguage): String =
        if (language == AppLanguage.BANGLA) subtitleBn else subtitle
}

fun AccountType.getDisplayName(language: AppLanguage): String =
    if (language == AppLanguage.BANGLA) displayNameBn else displayName

fun TransactionType.getDisplayName(language: AppLanguage): String =
    if (language == AppLanguage.BANGLA) displayNameBn else displayName

fun DebtType.getDisplayName(language: AppLanguage): String =
    if (language == AppLanguage.BANGLA) displayNameBn else displayName

