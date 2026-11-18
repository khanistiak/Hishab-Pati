package com.example.ocr

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ReceiptAmountCandidate(
    val amount: Double,
    val label: String,
    val confidenceScore: Int,
    val isPrimaryTotal: Boolean = false
)

data class ParsedReceiptData(
    val rawText: String,
    val totalAmount: Double?,
    val merchantName: String?,
    val dateMillis: Long?,
    val detectedCurrency: String?,
    val suggestedCategory: String?,
    val suggestedSubcategory: String?,
    val suggestedAccountId: String?,
    val hasText: Boolean,
    val candidateAmounts: List<ReceiptAmountCandidate> = emptyList(),
    val isAmountAmbiguous: Boolean = false,
    val matchedTotalLabel: String? = null
) {
    val dateTimestamp: Long? get() = dateMillis
    val categoryName: String? get() = suggestedCategory
    val subcategoryName: String? get() = suggestedSubcategory
}

/**
 * Context-aware receipt parser for Hishab Pati.
 *
 * Rules:
 * - Prefers clearly labeled final totals (Grand Total, Net Total, Total Payable, Amount Due)
 *   over subtotal, tax/VAT, discount, cash tendered, change, item prices, invoice numbers, dates, or phone numbers.
 * - Never assumes the largest number on the receipt is the total.
 * - Surfaces candidate amounts and flags ambiguity when multiple plausible totals exist or confidence is low.
 */
object ReceiptParser {

    private val banglaDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private fun normalizeDigits(text: String): String {
        return text.map { banglaDigits[it] ?: it }.joinToString("")
    }

    fun parse(
        rawOcrText: String,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>
    ): ParsedReceiptData {
        val cleanedText = rawOcrText.trim()
        if (cleanedText.isBlank()) {
            return ParsedReceiptData(
                rawText = rawOcrText,
                totalAmount = null,
                merchantName = null,
                dateMillis = null,
                detectedCurrency = null,
                suggestedCategory = null,
                suggestedSubcategory = null,
                suggestedAccountId = null,
                hasText = false,
                candidateAmounts = emptyList(),
                isAmountAmbiguous = false,
                matchedTotalLabel = null
            )
        }

        val lines = cleanedText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 1. Detect Currency Symbol / Code
        val detectedCurrency = detectCurrency(cleanedText)

        // 2. Extract Merchant / Store Name
        val merchantName = extractMerchantName(lines)

        // 3. Extract Total Amount & Candidate Amounts with context-aware rules
        val extraction = extractTotalAndCandidates(lines)

        // 4. Extract Date
        val dateMillis = extractDateMillis(lines)

        // 5. Infer Category & Subcategory ONLY if strong keyword matches exist
        val (suggestedCat, suggestedSub) = inferCategory(cleanedText, merchantName, categories)

        // 6. Infer Payment Method / Wallet ONLY if explicitly mentioned on receipt
        val suggestedAccountId = inferAccount(cleanedText, accounts)

        return ParsedReceiptData(
            rawText = rawOcrText,
            totalAmount = extraction.selectedTotal,
            merchantName = merchantName,
            dateMillis = dateMillis,
            detectedCurrency = detectedCurrency,
            suggestedCategory = suggestedCat,
            suggestedSubcategory = suggestedSub,
            suggestedAccountId = suggestedAccountId,
            hasText = true,
            candidateAmounts = extraction.candidates,
            isAmountAmbiguous = extraction.isAmbiguous,
            matchedTotalLabel = extraction.matchedLabel
        )
    }

    private fun detectCurrency(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())
        return when {
            text.contains("৳") || lower.contains("bdt") || lower.contains("taka") ||
                    Regex("""\btk\.?\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                    text.contains("টাকা") -> "৳"
            text.contains("$") || lower.contains("usd") -> "$"
            text.contains("€") || lower.contains("eur") -> "€"
            text.contains("£") || lower.contains("gbp") -> "£"
            text.contains("₹") || lower.contains("inr") -> "₹"
            else -> null
        }
    }

    private fun extractMerchantName(lines: List<String>): String? {
        val skipPatterns = listOf(
            "receipt", "invoice", "tax invoice", "cash memo", "bill", "welcome", "thank you",
            "date", "time", "tel", "phone", "mobile", "cell", "hotline", "vat", "bin", "tin",
            "mushak", "musak", "order", "table", "token", "cashier", "served by", "customer",
            "রসিদ", "ক্যাশ মেমো", "স্বাগতম", "ধন্যবাদ", "তারিখ", "ফোন", "মোবাইল", "ভ্যাট", "চালান"
        )
        for (line in lines.take(6)) {
            val lower = line.lowercase(Locale.getDefault())
            val digitCount = line.count { it.isDigit() || banglaDigits.containsKey(it) }
            val letterCount = line.count { it.isLetter() }
            if (letterCount >= 3 &&
                digitCount <= letterCount &&
                line.length in 3..55 &&
                skipPatterns.none { lower.startsWith(it) || lower == it } &&
                !lower.startsWith("www.") &&
                !lower.startsWith("http")
            ) {
                return line
            }
        }
        return null
    }

    private data class TotalExtractionResult(
        val selectedTotal: Double?,
        val matchedLabel: String?,
        val candidates: List<ReceiptAmountCandidate>,
        val isAmbiguous: Boolean
    )

    private data class ScoredLineCandidate(
        val lineIndex: Int,
        val amount: Double,
        val label: String,
        val tier: Int, // 1 = Explicit Grand/Net/Payable Total, 2 = Generic Total, 3 = Subtotal, 4 = Other/Unlabeled
        val score: Int
    )

    // Tier 1: Unambiguous final payable total labels
    private val tier1TotalPhrases = listOf(
        "grand total", "net total", "total payable", "net payable", "payable amount",
        "amount payable", "amount due", "balance due", "total due", "final total",
        "net bill", "total bill", "bill payable", "total to pay", "you pay",
        "সর্বমোট", "মোট প্রদেয়", "মোট প্রদেয়", "নেট বিল", "নেট মোট", "পরিশোধযোগ্য"
    )

    // Tier 2: Standard total labels (must not be modified by sub/discount/vat/paid/change/qty)
    private val tier2TotalPhrases = listOf(
        "total amount", "net amount", "bill amount", "invoice total", "sale total",
        "total tk", "total bdt", "total (bdt)", "total (tk)",
        "মোট টাকা", "মোট বিল", "মোট মূল্য", "মোট", "total"
    )

    // Non-total monetary line qualifiers
    private val subtotalPhrases = listOf(
        "sub total", "subtotal", "sub-total", "item total", "items total",
        "gross total", "gross amount", "food total",
        "উপমোট", "সাবটোটাল", "সাব-টোটাল"
    )

    private val taxOrChargePhrases = listOf(
        "vat", "tax", "gst", "cgst", "sgst", "igst", "s.tax", "sales tax",
        "supplementary duty", "service charge", "s.charge", "srv charge",
        "delivery charge", "delivery fee", "packaging", "container charge", "rounding",
        "ভ্যাট", "ট্যাক্স", "কর", "সার্ভিস চার্জ", "ডেলিভারি চার্জ"
    )

    private val discountPhrases = listOf(
        "discount", "disc", "less", "saving", "savings", "you saved", "total saved",
        "total discount", "promo", "coupon", "voucher", "rebate", "cashback",
        "ডিসকাউন্ট", "ছাড়", "ছাড়", "সাশ্রয়", "সাশ্রয়"
    )

    private val tenderedOrPaidPhrases = listOf(
        "cash tendered", "tendered", "amount tendered", "cash received", "received amount",
        "paid amount", "amount paid", "cash paid", "customer paid", "total paid",
        "payment received", "card paid", "bkash paid", "nagad paid", "given",
        "জমা", "প্রদত্ত", "নগদ প্রদান", "পরিশোধিত"
    )

    private val changePhrases = listOf(
        "change", "change due", "change amount", "cash back", "return amount", "refund",
        "ফেরত", "ভাংতি"
    )

    private val nonMonetaryLinePhrases = listOf(
        "total item", "total items", "item count", "items count", "total qty", "total quantity",
        "no of items", "number of items", "pcs", "points", "loyalty", "earned points",
        "invoice no", "invoice #", "inv no", "inv#", "receipt no", "receipt #", "bill no",
        "order no", "order #", "table no", "token no", "serial", "sl no", "bin", "tin",
        "mushak", "musak", "vat reg", "tel", "phone", "mobile", "cell", "hotline", "fax",
        "auth code", "approval", "terminal", "trxid", "txn id", "card no", "account no",
        "মোট আইটেম", "টি আইটেম", "ইনভয়েস", "রসিদ নং", "ফোন", "মোবাইল", "চালান নং"
    )

    private fun extractTotalAndCandidates(lines: List<String>): TotalExtractionResult {
        val normalizedLines = lines.map { normalizeDigits(it) }
        val scoredCandidates = mutableListOf<ScoredLineCandidate>()

        for (i in normalizedLines.indices) {
            val line = normalizedLines[i]
            val lower = line.lowercase(Locale.getDefault())

            // Skip lines that are clearly metadata (invoice numbers, phone, item count, etc.)
            if (isMetadataOrCountLine(lower)) continue

            val classification = classifyReceiptLine(lower)

            // Extract monetary number on this line, or if it's a pure label line (Tier 1 / Tier 2 / Subtotal),
            // check the immediately following line if it only contains a currency/number.
            var amount = extractMonetaryAmountFromLine(line)
            var consumedNextLine = false

            if (amount == null && classification != null && classification.first in 1..3 && i + 1 < normalizedLines.size) {
                val nextLine = normalizedLines[i + 1]
                val nextLower = nextLine.lowercase(Locale.getDefault())
                if (classifyReceiptLine(nextLower) == null && !isMetadataOrCountLine(nextLower)) {
                    amount = extractMonetaryAmountFromLine(nextLine, requireExplicitCurrencyOrStandalone = true)
                    if (amount != null) {
                        consumedNextLine = true
                    }
                }
            }

            if (amount != null && amount > 0.0) {
                if (classification != null) {
                    val (tier, baseScore, cleanLabel) = classification
                    // Only include Tiers 1, 2, and 3 (Subtotal) as candidate totals;
                    // Tax, Discount, Cash Tendered, and Change (Tier 99) are excluded from total selection.
                    if (tier in 1..3) {
                        val positionBonus = if (i >= normalizedLines.size / 2) 5 else 0
                        scoredCandidates.add(
                            ScoredLineCandidate(
                                lineIndex = if (consumedNextLine) i + 1 else i,
                                amount = amount,
                                label = cleanLabel,
                                tier = tier,
                                score = baseScore + positionBonus
                            )
                        )
                    }
                }
            }
        }

        // Group candidates by Tier
        val tier1 = scoredCandidates.filter { it.tier == 1 }
        val tier2 = scoredCandidates.filter { it.tier == 2 }
        val tier3Subtotals = scoredCandidates.filter { it.tier == 3 }

        // Build deduplicated user-facing candidate list for the Review Receipt UI
        val uiCandidates = scoredCandidates
            .sortedWith(compareBy<ScoredLineCandidate> { it.tier }.thenByDescending { it.lineIndex })
            .distinctBy { String.format(Locale.US, "%.2f", it.amount) }
            .mapIndexed { idx, cand ->
                ReceiptAmountCandidate(
                    amount = cand.amount,
                    label = cand.label,
                    confidenceScore = cand.score,
                    isPrimaryTotal = idx == 0 && cand.tier in 1..2
                )
            }

        // Case 1: One or more Tier 1 (Grand Total / Net Total / Total Payable / Amount Due) lines found
        if (tier1.isNotEmpty()) {
            // Prefer the bottom-most Tier 1 label on the receipt
            val chosen = tier1.maxWithOrNull(compareBy<ScoredLineCandidate> { it.score }.thenBy { it.lineIndex })!!
            val distinctTier1Amounts = tier1.map { it.amount }.distinct()
            return TotalExtractionResult(
                selectedTotal = chosen.amount,
                matchedLabel = chosen.label,
                candidates = uiCandidates,
                isAmbiguous = distinctTier1Amounts.size > 1
            )
        }

        // Case 2: Tier 2 (Standard Total) lines found
        if (tier2.isNotEmpty()) {
            val chosen = tier2.maxWithOrNull(compareBy<ScoredLineCandidate> { it.score }.thenBy { it.lineIndex })!!
            val distinctTier2Amounts = tier2.map { it.amount }.distinct()
            return TotalExtractionResult(
                selectedTotal = chosen.amount,
                matchedLabel = chosen.label,
                candidates = uiCandidates,
                isAmbiguous = distinctTier2Amounts.size > 1
            )
        }

        // Case 3: Only Subtotal found (no explicit Total label on receipt)
        // Surface Subtotal as a candidate and flag ambiguity so user verifies in Review step.
        if (tier3Subtotals.isNotEmpty()) {
            val chosenSub = tier3Subtotals.last()
            return TotalExtractionResult(
                selectedTotal = chosenSub.amount,
                matchedLabel = chosenSub.label,
                candidates = uiCandidates,
                isAmbiguous = true
            )
        }

        // Case 4: No labeled totals at all.
        // Do NOT assume the largest number is the total!
        // Collect plausible unlabeled currency/decimal amounts near the bottom of the receipt as low-confidence
        // candidates for the user to pick in Review Receipt, leaving selectedTotal = null.
        val unlabeledCandidates = mutableListOf<ReceiptAmountCandidate>()
        for (i in normalizedLines.indices.reversed()) {
            val line = normalizedLines[i]
            val lower = line.lowercase(Locale.getDefault())
            if (isMetadataOrCountLine(lower)) continue
            if (classifyReceiptLine(lower)?.first == 99) continue // Skip tax/discount/tendered/change
            val amt = extractMonetaryAmountFromLine(line, requireDecimalOrCurrency = true)
            if (amt != null && amt > 0.0 && unlabeledCandidates.none { it.amount == amt }) {
                unlabeledCandidates.add(
                    ReceiptAmountCandidate(
                        amount = amt,
                        label = line.take(28),
                        confidenceScore = 35,
                        isPrimaryTotal = false
                    )
                )
                if (unlabeledCandidates.size >= 4) break
            }
        }

        return TotalExtractionResult(
            selectedTotal = null,
            matchedLabel = null,
            candidates = unlabeledCandidates,
            isAmbiguous = unlabeledCandidates.isNotEmpty()
        )
    }

    /**
     * Returns Triple(tier, baseScore, displayLabel):
     * - Tier 1 (Score 100): Grand Total, Net Total, Total Payable, Amount Due, etc.
     * - Tier 2 (Score 85): Total, Total Amount, Bill Amount, মোট
     * - Tier 3 (Score 55): Subtotal, Sub Total
     * - Tier 99 (Score 0): Tax/VAT, Discount, Cash Tendered/Paid, Change (explicitly excluded from being selected as Total)
     * - null: Ordinary line item or text
     */
    private fun classifyReceiptLine(lower: String): Triple<Int, Int, String>? {
        // First check excluded monetary categories so "Total Discount", "Total VAT", "Total Paid", "Sub Total"
        // are NEVER misclassified as Tier 1 or Tier 2 Total!
        if (subtotalPhrases.any { lower.contains(it) }) {
            return Triple(3, 55, "Subtotal")
        }
        if (discountPhrases.any { lower.contains(it) }) {
            return Triple(99, 0, "Discount")
        }
        if (taxOrChargePhrases.any { matchesWordOrPhrase(lower, it) }) {
            // Special case: "Total (incl. VAT)" or "Grand Total (Inc VAT)" IS a total line!
            val isInclusiveTotal = (lower.contains("incl") || lower.contains("inc.") || lower.contains("including")) &&
                    (lower.contains("total") || lower.contains("payable"))
            if (!isInclusiveTotal) {
                return Triple(99, 0, "Tax / Charge")
            }
        }
        if (changePhrases.any { matchesWordOrPhrase(lower, it) }) {
            return Triple(99, 0, "Change")
        }
        if (tenderedOrPaidPhrases.any { lower.contains(it) } ||
            Regex("""\b(cash|card|bkash|nagad|rocket|tendered)\s*[:=-]""").containsMatchIn(lower)
        ) {
            return Triple(99, 0, "Paid / Tendered")
        }

        // Check Tier 1 (Grand Total / Net Total / Total Payable / Amount Due)
        for (phrase in tier1TotalPhrases) {
            if (lower.contains(phrase)) {
                val label = phrase.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                }
                return Triple(1, 100, label)
            }
        }

        // Check Tier 2 (Standard Total)
        for (phrase in tier2TotalPhrases) {
            if (matchesWordOrPhrase(lower, phrase)) {
                return Triple(2, 85, "Total")
            }
        }

        return null
    }

    private fun matchesWordOrPhrase(text: String, phrase: String): Boolean {
        return if (phrase.all { it.code < 128 }) {
            Regex("""\b${Regex.escape(phrase)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)
        } else {
            text.contains(phrase)
        }
    }

    private fun isMetadataOrCountLine(lower: String): Boolean {
        if (nonMonetaryLinePhrases.any { lower.contains(it) }) return true
        // Phone number pattern (e.g. 01712345678 or +8801...)
        if (Regex("""(?:\+?88)?01[3-9]\d{2}[-\s]?\d{6}""").containsMatchIn(lower)) return true
        return false
    }

    /**
     * Extracts the monetary value from a receipt line while ignoring dates, times, percentages,
     * item quantities, and long ID numbers.
     */
    private fun extractMonetaryAmountFromLine(
        normalizedLine: String,
        requireExplicitCurrencyOrStandalone: Boolean = false,
        requireDecimalOrCurrency: Boolean = false
    ): Double? {
        val hasCurrency = detectCurrency(normalizedLine) != null

        // Strip dates (dd/mm/yyyy, yyyy-mm-dd), times (hh:mm), and percentages (15%) before matching numbers
        val cleaned = normalizedLine
            .replace(Regex("""\b\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}\b"""), " ")
            .replace(Regex("""\b\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2}\b"""), " ")
            .replace(Regex("""\b\d{1,2}:\d{2}(?::\d{2})?(?:\s*[aApP][mM])?\b"""), " ")
            .replace(Regex("""\b\d+(?:\.\d+)?\s*%"""), " ")
            .replace(Regex("""\b\d+\s*[xX*]\s*"""), " ") // Strip "2 x " quantity multiplier prefix

        if (requireExplicitCurrencyOrStandalone) {
            val strippedLetters = cleaned
                .replace(Regex("""(bdt|tk|taka|usd|৳|\$|€|£|₹|টাকা)""", RegexOption.IGNORE_CASE), "")
                .trim()
            val nonNumericChars = strippedLetters.count { it.isLetter() }
            if (nonNumericChars > 3 && !hasCurrency) return null
        }

        val regex = Regex("""(\d{1,3}(?:,\d{2,3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)""")
        val matches = regex.findAll(cleaned).toList()
        if (matches.isEmpty()) return null

        // Take the right-most valid monetary token on the line
        for (match in matches.asReversed()) {
            val rawToken = match.value
            val hasDecimal = rawToken.contains(".")
            val hasComma = rawToken.contains(",")
            val digitsOnly = rawToken.replace(",", "").replace(".", "")

            // Skip long integer IDs / barcodes / phone numbers (7+ digits with no decimal or comma)
            if (!hasDecimal && !hasComma && digitsOnly.length >= 7) continue

            // Skip 4-digit years like 2024, 2025, 2026 if there's no decimal and no currency symbol
            val parsed = rawToken.replace(",", "").toDoubleOrNull() ?: continue
            if (!hasDecimal && !hasCurrency && parsed in 1990.0..2035.0 && matches.size > 1) continue

            if (requireDecimalOrCurrency && !hasDecimal && !hasCurrency && !hasComma) continue

            if (parsed > 0.0 && parsed < 10_000_000.0) {
                return parsed
            }
        }
        return null
    }

    private fun extractDateMillis(lines: List<String>): Long? {
        val dateRegexes = listOf(
            Regex("""\b(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4})\b""") to listOf(
                "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "dd/MM/yy", "dd-MM-yy", "MM/dd/yyyy"
            ),
            Regex("""\b(\d{4}[/\-.]\d{1,2}[/\-.]\d{1,2})\b""") to listOf(
                "yyyy-MM-dd", "yyyy/MM/dd"
            ),
            Regex("""\b(\d{1,2}\s+[A-Za-z]{3,9}\s+\d{2,4})\b""") to listOf(
                "dd MMM yyyy", "dd MMMM yyyy", "d MMM yyyy", "dd MMM yy"
            )
        )

        for (line in lines) {
            val normalized = normalizeDigits(line)
            for ((regex, formats) in dateRegexes) {
                val match = regex.find(normalized)
                if (match != null) {
                    val dateStr = match.groupValues[1]
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                                isLenient = false
                            }
                            val parsedDate = sdf.parse(dateStr)
                            if (parsedDate != null) {
                                val cal = Calendar.getInstance().apply { time = parsedDate }
                                val year = cal.get(Calendar.YEAR)
                                if (year in 2000..2100) {
                                    return parsedDate.time
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }
        return null
    }

    private fun inferCategory(
        fullText: String,
        merchantName: String?,
        categories: List<CategoryEntity>
    ): Pair<String?, String?> {
        val combined = ((merchantName ?: "") + " " + fullText).lowercase(Locale.getDefault())

        val rules = listOf(
            Triple(
                listOf("restaurant", "cafe", "coffee", "burger", "pizza", "biriyani", "kacchi", "dine", "kitchen", "bistro", "bakery", "food", "রেস্টুরেন্ট", "হোটেল", "ক্যাফে", "বেকারি"),
                "Food & Dining",
                "Restaurant"
            ),
            Triple(
                listOf("shwapno", "agora", "meena bazar", "unimart", "supermarket", "grocery", "groceries", "mart", "bazar", "স্বপ্ন", "আগোরা", "মীনা বাজার", "সুপারশপ", "মুদি"),
                "Groceries",
                "Supermarket"
            ),
            Triple(
                listOf("pharma", "pharmacy", "medicine", "hospital", "clinic", "diagnostic", "lazz pharma", "tamanna", "ফার্মেসি", "ওষুধ", "হাসপাতাল", "ডায়াগনস্টিক"),
                "Health",
                "Pharmacy"
            ),
            Triple(
                listOf("fuel", "filling station", "petrol", "octane", "cng", "uber", "pathao", "ফিলিং স্টেশন", "পেট্রোল", "অকটেন"),
                "Transport",
                "Fuel"
            ),
            Triple(
                listOf("aarong", "yellow", "ecstasy", "sailor", "apex", "bata", "lottie", "fashion", "apparel", "clothing", "আরং", "বাটা", "এপেক্স"),
                "Shopping",
                "Clothing"
            ),
            Triple(
                listOf("desco", "dpdc", "wasa", "titas", "bpdb", "palli bidyut", "electricity", "internet", "broadband", "বিদ্যুৎ", "ওয়াসা", "তিতাস"),
                "Bills & Utilities",
                "Utility"
            )
        )

        for ((keywords, defaultCat, defaultSub) in rules) {
            if (keywords.any { combined.contains(it) }) {
                val matchedCat = categories.firstOrNull {
                    it.name.equals(defaultCat, ignoreCase = true) ||
                            it.name.contains(defaultCat.split(" ").first(), ignoreCase = true)
                }
                val catName = matchedCat?.name ?: defaultCat
                return catName to defaultSub
            }
        }

        return null to null
    }

    private fun inferAccount(
        fullText: String,
        accounts: List<AccountEntity>
    ): String? {
        val lower = fullText.lowercase(Locale.getDefault())
        val aliases = mapOf(
            "bkash" to listOf("bkash", "বিকাশ"),
            "nagad" to listOf("nagad", "নগদ"),
            "rocket" to listOf("rocket", "রকেট"),
            "city" to listOf("city bank", "amex", "সিটি ব্যাংক"),
            "brac" to listOf("brac bank", "ব্র্যাক ব্যাংক"),
            "dbbl" to listOf("dbbl", "nexus", "ডাচ বাংলা")
        )

        for (acc in accounts) {
            val accLower = acc.name.lowercase(Locale.getDefault())
            if (lower.contains(accLower)) return acc.id
            for ((key, kwList) in aliases) {
                if (accLower.contains(key) && kwList.any { lower.contains(it) }) {
                    return acc.id
                }
            }
        }
        return null
    }
}
