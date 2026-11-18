package com.example.data.model

data class CurrencyOption(
    val code: String,
    val name: String,
    val nameBn: String,
    val symbol: String,
    val flag: String = ""
)

object SupportedCurrencies {
    val allCurrencies = listOf(
        CurrencyOption("BDT", "Bangladeshi Taka", "বাংলাদেশী টাকা", "৳", "🇧🇩"),
        CurrencyOption("USD", "US Dollar", "ইউএস ডলার", "$", "🇺🇸"),
        CurrencyOption("EUR", "Euro", "ইউরো", "€", "🇪🇺"),
        CurrencyOption("GBP", "British Pound", "ব্রিটিশ পাউন্ড", "£", "🇬🇧"),
        CurrencyOption("INR", "Indian Rupee", "ভারতীয় রুপি", "₹", "🇮🇳"),
        CurrencyOption("SAR", "Saudi Riyal", "সৌদি রিয়াল", "ر.স", "🇸🇦"),
        CurrencyOption("AED", "UAE Dirham", "সংযুক্ত আরব আমিরাত দিরহাম", "د.إ", "🇦🇪"),
        CurrencyOption("MYR", "Malaysian Ringgit", "মালয়েশিয়ান রিঙ্গিত", "RM", "🇲🇾"),
        CurrencyOption("SGD", "Singapore Dollar", "সিঙ্গাপুর ডলার", "S$", "🇸🇬"),
        CurrencyOption("JPY", "Japanese Yen", "জাপানি ইয়েন", "¥", "🇯🇵"),
        CurrencyOption("CNY", "Chinese Yuan", "চীনা ইউয়ান", "¥", "🇨🇳"),
        CurrencyOption("AUD", "Australian Dollar", "অস্ট্রেলিয়ান ডলার", "A$", "🇦🇺"),
        CurrencyOption("CAD", "Canadian Dollar", "কানাডিয়ান ডলার", "C$", "🇨🇦")
    )
}
