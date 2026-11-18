package com.example.voice

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.model.TransactionType
import java.util.Locale

data class ParsedVoiceTransaction(
    val rawText: String,
    val amount: Double,
    val type: TransactionType,
    val categoryName: String,
    val subcategoryName: String = "",
    val matchedAccountId: String?,
    val matchedToAccountId: String? = null,
    val description: String,
    val hasConfidentAmount: Boolean = false,
    val hasConfidentCategory: Boolean = false,
    val hasConfidentAccount: Boolean = false,
    val hasConfidentDescription: Boolean = false
)

/**
 * Bilingual (Bangla, English & Banglish) offline speech-to-transaction parser.
 * Never guesses or fabricates missing fields; sets confidence flags only when explicitly detected.
 */
object VoiceTransactionParser {

    private val banglaDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    // Spoken Bangla & Banglish number words -> numeric values
    private val banglaNumberWords = listOf(
        "দশ হাজার" to "10000",
        "পাঁচ হাজার" to "5000",
        "দুই হাজার" to "2000",
        "এক হাজার" to "1000",
        "হাজার" to "1000",
        "নয়শো" to "900", "নয়শো" to "900", "নয়শ" to "900",
        "আটশো" to "800", "আটশ" to "800",
        "সাতশো" to "700", "সাতশ" to "700",
        "ছয়শো" to "600", "ছয়শো" to "600", "ছয়শ" to "600",
        "পাঁচশো" to "500", "পাঁচশ" to "500",
        "চারশো" to "400", "চারশ" to "400",
        "তিনশো" to "300", "তিনশ" to "300",
        "দুইশো" to "200", "দুইশ" to "200", "দুশো" to "200",
        "দেড়শো" to "150", "দেড়শ" to "150", "দেড়শো" to "150",
        "একশো" to "100", "একশ" to "100", "শো" to "100",
        "নব্বই" to "90",
        "আশি" to "80",
        "সত্তর" to "70",
        "ষাট" to "60",
        "পঞ্চাশ" to "50",
        "চল্লিশ" to "40",
        "ত্রিশ" to "30", "তিরিশ" to "30",
        "পঁচিশ" to "25",
        "বিশ" to "20", "কুড়ি" to "20", "কুড়ি" to "20",
        "পনেরো" to "15", "পনের" to "15",
        "দশ" to "10",
        "নয়" to "9", "নয়" to "9",
        "আট" to "8",
        "সাত" to "7",
        "ছয়" to "6", "ছয়" to "6",
        "পাঁচ" to "5",
        "চার" to "4",
        "তিন" to "3",
        "দুই" to "2",
        "এক" to "1"
    )

    private val banglishNumberWords = listOf(
        "dosh hajar" to "10000", "dos hajar" to "10000",
        "pach hajar" to "5000", "panch hajar" to "5000",
        "dui hajar" to "2000",
        "ek hajar" to "1000",
        "noysho" to "900", "atsho" to "800", "satsho" to "700", "choysho" to "600",
        "pachsho" to "500", "pasho" to "500", "panchsho" to "500",
        "charsho" to "400", "tinsho" to "300", "duisho" to "200", "dusho" to "200",
        "dersho" to "150", "eksho" to "100",
        "nobboi" to "90", "ashi" to "80", "shottor" to "70", "shat" to "60",
        "ponchash" to "50", "chollish" to "40", "trish" to "30", "tirish" to "30",
        "pochish" to "25", "bish" to "20", "kuri" to "20", "ponero" to "15", "dosh" to "10", "dos" to "10"
    )

    private val englishCompoundNumbers = listOf(
        "ten thousand" to "10000",
        "five thousand" to "5000",
        "two thousand" to "2000",
        "one thousand" to "1000",
        "nine hundred" to "900",
        "eight hundred" to "800",
        "seven hundred" to "700",
        "six hundred" to "600",
        "five hundred" to "500",
        "four hundred" to "400",
        "three hundred" to "300",
        "two hundred" to "200",
        "one hundred fifty" to "150",
        "one hundred" to "100"
    )

    private val englishNumberWords = mapOf(
        "thousand" to "1000",
        "hundred" to "100",
        "ninety" to "90",
        "eighty" to "80",
        "seventy" to "70",
        "sixty" to "60",
        "fifty" to "50",
        "forty" to "40",
        "thirty" to "30",
        "twenty" to "20",
        "nineteen" to "19",
        "eighteen" to "18",
        "seventeen" to "17",
        "sixteen" to "16",
        "fifteen" to "15",
        "fourteen" to "14",
        "thirteen" to "13",
        "twelve" to "12",
        "eleven" to "11",
        "ten" to "10",
        "nine" to "9",
        "eight" to "8",
        "seven" to "7",
        "six" to "6",
        "five" to "5",
        "four" to "4",
        "three" to "3",
        "two" to "2",
        "one" to "1"
    )

    fun normalizeDigits(input: String): String {
        var text = input.map { banglaDigits[it] ?: it }.joinToString("")
        for ((word, num) in banglaNumberWords) {
            if (text.contains(word)) {
                text = text.replace(word, " $num ")
            }
        }
        for ((phrase, num) in banglishNumberWords) {
            text = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE).replace(text, num)
        }
        for ((phrase, num) in englishCompoundNumbers) {
            text = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE).replace(text, num)
        }
        for ((word, num) in englishNumberWords) {
            text = Regex("\\b$word\\b", RegexOption.IGNORE_CASE).replace(text, num)
        }
        return text.replace(Regex("\\s+"), " ").trim()
    }

    fun parse(
        spokenText: String,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): ParsedVoiceTransaction {
        val trimmedInput = spokenText.trim()
        if (trimmedInput.isBlank()) {
            return ParsedVoiceTransaction(
                rawText = spokenText,
                amount = 0.0,
                type = TransactionType.EXPENSE,
                categoryName = "",
                subcategoryName = "",
                matchedAccountId = null,
                description = "",
                hasConfidentAmount = false,
                hasConfidentCategory = false,
                hasConfidentAccount = false,
                hasConfidentDescription = false
            )
        }

        val normalized = normalizeDigits(trimmedInput)
        val lower = normalized.lowercase(Locale.getDefault())

        // 1. Extract Amount (Supports comma-formatted numbers and decimals, e.g., 1,250.50 or 500)
        val amountRegex = Regex("""(\d{1,3}(?:,\d{2,3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)""")
        val matchedAmounts = amountRegex.findAll(normalized)
            .mapNotNull { it.value.replace(",", "").toDoubleOrNull() }
            .filter { it > 0.0 }
            .toList()

        val amount = matchedAmounts.firstOrNull() ?: 0.0
        val hasConfidentAmount = amount > 0.0

        // 2. Detect Transaction Type (Transfer, Income, Expense)
        val isTransfer = lower.contains("transfer") || lower.contains("ট্রান্সফার") ||
                lower.contains("থেকে") && (lower.contains("তে") || lower.contains("এ")) && accounts.count { acc ->
                    lower.contains(acc.name.lowercase())
                } >= 2 ||
                (lower.contains("from") && lower.contains("to") && accounts.count { acc ->
                    lower.contains(acc.name.lowercase())
                } >= 2)

        val incomeKeywords = listOf(
            "income", "salary", "earned", "received", "bonus", "freelance", "dividend", "refund", "gift received",
            "আয়", "আয়", "বেতন", "পেয়েছি", "পেয়েছি", "বোনাস", "ইনকাম", "উপার্জন", "কমিশন", "ফ্রিল্যান্স",
            "beton", "peyechi", "peyesi", "pelam", "kamai", "lav", "munafa"
        )
        val isIncome = !isTransfer && incomeKeywords.any { lower.contains(it) }

        val type = when {
            isTransfer -> TransactionType.TRANSFER
            isIncome -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }

        // 3. Match Account / Wallet ONLY when explicitly mentioned in speech (no guessing)
        val accountAliases = mapOf(
            "bkash" to listOf("bkash", "বিকাশ", "বিকাশে", "বিকাশের", "bikash"),
            "nagad" to listOf("nagad", "নগদ", "নগদে", "nogod"),
            "rocket" to listOf("rocket", "রকেট", "রকেটে", "roket"),
            "upay" to listOf("upay", "উপায়", "উপায়"),
            "cash" to listOf("cash", "ক্যাশ", "নগদ টাকা", "হাতে", "pocket", "wallet", "নগদ ক্যাশ", "nagad taka"),
            "city" to listOf("city bank", "city", "সিটি ব্যাংক", "সিটি"),
            "brac" to listOf("brac bank", "brac", "ব্র্যাক ব্যাংক", "ব্র্যাক"),
            "dbbl" to listOf("dutch bangla", "dbbl", "ডাচ বাংলা"),
            "islami" to listOf("islami bank", "ইসলামী ব্যাংক"),
            "eb" to listOf("eastern bank", "ebl", "ইস্টার্ন ব্যাংক"),
            "bank" to listOf("bank", "ব্যাংক", "ব্যাংকে"),
            "card" to listOf("card", "credit card", "debit card", "কার্ড", "ক্রেডিট কার্ড")
        )

        val matchedAccounts = mutableListOf<AccountEntity>()
        for (acc in accounts) {
            val accLower = acc.name.lowercase()
            if (lower.contains(accLower)) {
                matchedAccounts.add(acc)
            } else {
                for ((key, aliases) in accountAliases) {
                    if (accLower.contains(key) && aliases.any { lower.contains(it) }) {
                        matchedAccounts.add(acc)
                        break
                    }
                }
            }
        }

        val hasConfidentAccount = matchedAccounts.isNotEmpty()
        val primaryAccount = matchedAccounts.firstOrNull()?.id
        val secondaryAccount = if (isTransfer && matchedAccounts.size >= 2) {
            matchedAccounts[1].id
        } else null

        // 4. Match Category & Subcategory ONLY when confidently identified
        var matchedCat = ""
        var matchedSub = ""
        var hasConfidentCategory = false

        val keywordToCategoryMap = listOf(
            // Food & Dining
            Triple(
                listOf(
                    "lunch", "dinner", "breakfast", "food", "meal", "restaurant", "burger", "pizza", "biriyani",
                    "biryani", "kacchi", "tehari", "khichuri", "coffee", "tea", "cha", "snacks", "snack", "bakery",
                    "cafe", "fast food", "dining", "iftaar", "sehri", "nasta", "khabar", "hotel",
                    "খাবার", "লাঞ্চ", "ডিনার", "নাস্তা", "রেস্টুরেন্ট", "বিরিয়ানি", "বিরিয়ানি", "চা", "কফি", "হোটেল", "ইফতার"
                ),
                "Food & Dining",
                "Restaurant"
            ),
            // Groceries / Bazar
            Triple(
                listOf(
                    "grocery", "groceries", "supermarket", "vegetables", "fish", "meat", "milk", "eggs", "fruits", "rice",
                    "bazar", "bazaar", "kacha bazar", "mudir", "shobji", "sobji", "mach", "mangsho", "chal", "dal", "dim", "dudh",
                    "বাজার", "মুদি", "সবজি", "মাছ", "মাংস", "চাল", "ডাল", "তেল", "দুধ", "ডিম", "ফল", "কাঁচাবাজার"
                ),
                "Groceries",
                "Market"
            ),
            // Transport
            Triple(
                listOf(
                    "uber", "pathao", "rickshaw", "riksha", "bus", "cng", "auto", "taxi", "train", "metro", "fuel", "octane",
                    "petrol", "fare", "transport", "ride", "commute", "parking", "toll", "bhara", "vara",
                    "ভাড়া", "ভাড়া", "রিকশা", "বাস", "সিএনজি", "উবার", "পাঠাও", "ট্রেন", "মেট্রো", "তেল", "অকটেন", "যাতায়াত", "যাতায়াত"
                ),
                "Transport",
                "Commute"
            ),
            // Bills & Utilities
            Triple(
                listOf(
                    "electricity", "electric", "water", "gas", "internet", "wifi", "broadband", "mobile bill", "recharge",
                    "flexiload", "utility", "utilities", "bill", "bidyut", "current bill", "pani",
                    "বিল", "বিদ্যুৎ", "কারেন্ট", "গ্যাস", "পানি", "ইন্টারনেট", "ওয়াইফাই", "ওয়াইফাই", "রিচার্জ", "মোবাইল বিল"
                ),
                "Bills & Utilities",
                "Utility"
            ),
            // Shopping
            Triple(
                listOf(
                    "shopping", "clothes", "shirt", "pant", "shoes", "dress", "saree", "panjabi", "electronics", "gadget",
                    "accessories", "cosmetics", "jama", "kapoor", "kapor", "juta",
                    "শপিং", "কেনাকাটা", "জামা", "কাপড়", "কাপড়", "জুতা", "পোশাক", "শাড়ি", "পাঞ্জাবি"
                ),
                "Shopping",
                "Personal"
            ),
            // Health & Medical
            Triple(
                listOf(
                    "medicine", "doctor", "hospital", "pharmacy", "medical", "clinic", "health", "test", "vitamin",
                    "oshudh", "osudh", "daktar",
                    "ওষুধ", "ঔষধ", "ডাক্তার", "হাসপাতাল", "ফার্মেসি", "চিকিৎসা", "টেস্ট"
                ),
                "Health",
                "Pharmacy"
            ),
            // Education
            Triple(
                listOf(
                    "book", "books", "tuition", "course", "school", "college", "university", "coaching", "stationery", "exam",
                    "boi", "khata",
                    "বই", "টিউশন", "কোর্স", "স্কুল", "কলেজ", "ভার্সিটি", "কোচিং", "খাতা", "কলম", "পরীক্ষা"
                ),
                "Education",
                "Tuition"
            ),
            // Entertainment
            Triple(
                listOf(
                    "movie", "netflix", "spotify", "game", "concert", "cinema", "ticket", "subscription", "outing",
                    "মুভি", "সিনেমা", "নেটফ্লিক্স", "গেম", "টিকিট", "বিনোদন"
                ),
                "Entertainment",
                "Subscriptions"
            ),
            // Housing / Rent
            Triple(
                listOf(
                    "rent", "house rent", "apartment", "maintenance", "service charge", "basha bhara", "basa vara", "mess bhara",
                    "বাসা ভাড়া", "বাসা ভাড়া", "বাড়ি ভাড়া", "বাড়ি ভাড়া", "সার্ভিস চার্জ"
                ),
                "Housing",
                "Rent"
            ),
            // Salary / Income
            Triple(
                listOf(
                    "salary", "wage", "paycheck", "beton", "বেতন", "মাসিক বেতন"
                ),
                "Salary",
                "Monthly"
            ),
            Triple(
                listOf(
                    "freelance", "project", "client", "upwork", "fiverr", "ফ্রিল্যান্স", "প্রজেক্ট"
                ),
                "Freelance",
                "Project"
            )
        )

        for ((keywords, defaultCat, defaultSub) in keywordToCategoryMap) {
            val matchedKeyword = keywords.firstOrNull { kw ->
                if (kw.all { it.code < 128 }) {
                    Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(lower)
                } else {
                    lower.contains(kw)
                }
            }
            if (matchedKeyword != null) {
                val userCat = categories.firstOrNull {
                    it.name.equals(defaultCat, ignoreCase = true) ||
                            it.name.contains(defaultCat.split(" ").first(), ignoreCase = true)
                }
                matchedCat = userCat?.name ?: defaultCat
                matchedSub = defaultSub
                hasConfidentCategory = true
                break
            }
        }

        if (!hasConfidentCategory) {
            for (cat in categories) {
                val catNameLower = cat.name.lowercase()
                val catBn = cat.nameBn
                if ((catNameLower.isNotBlank() && lower.contains(catNameLower)) ||
                    (catBn.isNotBlank() && lower.contains(catBn))
                ) {
                    matchedCat = cat.name
                    matchedSub = "General"
                    hasConfidentCategory = true
                    break
                }
            }
        }

        // 5. Build clean Title / Description without inventing text when empty
        val noisePatterns = listOf(
            Regex("""\d{1,3}(?:,\d{2,3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?"""),
            Regex("""(টাকা|taka|tk|bd|bdt|৳|\$)""", RegexOption.IGNORE_CASE),
            Regex("""(থেকে|দিয়ে|দিয়ে|তে|এ|জন্য| বাবদ|করেছি|করলাম|খরচ|হয়েছে|হয়েছে|দিলাম|পেলাম|পেয়েছি|পেয়েছি)"""),
            Regex("""\b(from|to|with|using|for|in|by|via|at|on|spent|paid|bought|got|received|korsi|korlam|khoroch|dilam|diyesi|pelam|peyesi|peyechi)\b""", RegexOption.IGNORE_CASE)
        )

        var cleanedDesc = normalized
        for (pattern in noisePatterns) {
            cleanedDesc = pattern.replace(cleanedDesc, " ")
        }
        // Also strip matched account names/aliases from description so "Lunch 120 from bKash" becomes "Lunch"
        for (acc in matchedAccounts) {
            cleanedDesc = cleanedDesc.replace(Regex(Regex.escape(acc.name), RegexOption.IGNORE_CASE), " ")
            for ((key, aliases) in accountAliases) {
                if (acc.name.lowercase().contains(key)) {
                    for (alias in aliases) {
                        cleanedDesc = cleanedDesc.replace(Regex(Regex.escape(alias), RegexOption.IGNORE_CASE), " ")
                    }
                }
            }
        }

        cleanedDesc = cleanedDesc
            .replace(Regex("""[^\p{L}\p{M}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        val hasConfidentDescription = cleanedDesc.length >= 2
        val finalDescription = when {
            hasConfidentDescription -> cleanedDesc.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            hasConfidentCategory -> matchedCat
            else -> ""
        }

        return ParsedVoiceTransaction(
            rawText = spokenText,
            amount = amount,
            type = type,
            categoryName = matchedCat,
            subcategoryName = matchedSub,
            matchedAccountId = primaryAccount,
            matchedToAccountId = secondaryAccount,
            description = finalDescription,
            hasConfidentAmount = hasConfidentAmount,
            hasConfidentCategory = hasConfidentCategory,
            hasConfidentAccount = hasConfidentAccount,
            hasConfidentDescription = hasConfidentDescription || hasConfidentCategory
        )
    }
}
