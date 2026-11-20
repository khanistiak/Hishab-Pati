package com.example.ui.screens.plan

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val nameEn: String,
    val nameBn: String,
    val countryKeywords: String,
    val symbol: String,
    val flag: String,
    val defaultRatePerUsd: Double // 1 USD = X Currency
) {
    fun localizedName(language: String): String =
        if (language.startsWith("bn", ignoreCase = true) || language == "বাংলা") nameBn else nameEn
}

data class RateFetchResult(
    val isLiveSuccess: Boolean,
    val ratesPerUsd: Map<String, Double>,
    val lastUpdatedMillis: Long
)

val SUPPORTED_CURRENCIES: List<CurrencyInfo> = listOf(
    CurrencyInfo("BDT", "Bangladeshi Taka", "বাংলাদেশী টাকা", "Bangladesh Dhaka", "৳", "🇧🇩", 121.50),
    CurrencyInfo("USD", "US Dollar", "মার্কিন ডলার", "United States America USA", "$", "🇺🇸", 1.00),
    CurrencyInfo("EUR", "Euro", "ইউরো", "Europe Germany France Italy Spain EU", "€", "🇪🇺", 0.92),
    CurrencyInfo("GBP", "British Pound", "ব্রিটিশ পাউন্ড", "United Kingdom UK England Britain", "£", "🇬🇧", 0.79),
    CurrencyInfo("SAR", "Saudi Riyal", "সৌদি রিয়াল", "Saudi Arabia Riyadh Mecca", "﷼", "🇸🇦", 3.75),
    CurrencyInfo("AED", "UAE Dirham", "ইউএই দিরহাম", "United Arab Emirates Dubai Abu Dhabi", "د.إ", "🇦🇪", 3.6725),
    CurrencyInfo("INR", "Indian Rupee", "ভারতীয় রুপি", "India Mumbai Delhi Kolkata", "₹", "🇮🇳", 84.40),
    CurrencyInfo("MYR", "Malaysian Ringgit", "মালয়েশিয়ান রিঙ্গিত", "Malaysia Kuala Lumpur", "RM", "🇲🇾", 4.45),
    CurrencyInfo("SGD", "Singapore Dollar", "সিঙ্গাপুর ডলার", "Singapore", "S$", "🇸🇬", 1.34),
    CurrencyInfo("QAR", "Qatari Riyal", "কাতারি রিয়াল", "Qatar Doha", "QR", "🇶🇦", 3.64),
    CurrencyInfo("KWD", "Kuwaiti Dinar", "কুয়েতি দিনার", "Kuwait", "KD", "🇰🇼", 0.307),
    CurrencyInfo("OMR", "Omani Rial", "ওমানি রিয়াল", "Oman Muscat", "OMR", "🇴🇲", 0.385),
    CurrencyInfo("BHD", "Bahraini Dinar", "বাহরাইনি দিনার", "Bahrain Manama", "BD", "🇧🇭", 0.376),
    CurrencyInfo("CAD", "Canadian Dollar", "কানাডিয়ান ডলার", "Canada Toronto Vancouver", "C$", "🇨🇦", 1.39),
    CurrencyInfo("AUD", "Australian Dollar", "অস্ট্রেলিয়ান ডলার", "Australia Sydney Melbourne", "A$", "🇦🇺", 1.53),
    CurrencyInfo("JPY", "Japanese Yen", "জাপানি ইয়েন", "Japan Tokyo", "¥", "🇯🇵", 154.20),
    CurrencyInfo("CNY", "Chinese Yuan", "চীনা ইউয়ান", "China Beijing Shanghai", "¥", "🇨🇳", 7.23),
    CurrencyInfo("CHF", "Swiss Franc", "সুইস ফ্রাঁ", "Switzerland Zurich Geneva", "CHF", "🇨🇭", 0.88),
    CurrencyInfo("NZD", "New Zealand Dollar", "নিউজিল্যান্ড ডলার", "New Zealand Auckland", "NZ$", "🇳🇿", 1.69),
    CurrencyInfo("HKD", "Hong Kong Dollar", "হংকং ডলার", "Hong Kong", "HK$", "🇭🇰", 7.78),
    CurrencyInfo("THB", "Thai Baht", "থাই বাথ", "Thailand Bangkok", "฿", "🇹🇭", 34.60),
    CurrencyInfo("KRW", "South Korean Won", "দক্ষিণ কোরিয়ান ওন", "South Korea Seoul", "₩", "🇰🇷", 1395.0),
    CurrencyInfo("TRY", "Turkish Lira", "তুর্কি লিরা", "Turkey Istanbul Ankara", "₺", "🇹🇷", 34.45),
    CurrencyInfo("PKR", "Pakistani Rupee", "পাকিস্তানি রুপি", "Pakistan Karachi Lahore", "₨", "🇵🇰", 277.80),
    CurrencyInfo("LKR", "Sri Lankan Rupee", "শ্রীলঙ্কান রুপি", "Sri Lanka Colombo", "Rs", "🇱🇰", 292.50),
    CurrencyInfo("NPR", "Nepalese Rupee", "নেপালি রুপি", "Nepal Kathmandu", "रू", "🇳🇵", 135.10)
)

class CurrencyRateRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCachedRatesPerUsd(): Map<String, Double> {
        val defaultMap = SUPPORTED_CURRENCIES.associate { it.code to it.defaultRatePerUsd }.toMutableMap()
        val rawJson = prefs.getString(KEY_RATES_JSON, null) ?: return defaultMap
        return try {
            val json = JSONObject(rawJson)
            for (currency in SUPPORTED_CURRENCIES) {
                if (json.has(currency.code)) {
                    val value = json.optDouble(currency.code, -1.0)
                    if (value > 0.0) {
                        defaultMap[currency.code] = value
                    }
                }
            }
            defaultMap
        } catch (_: Exception) {
            defaultMap
        }
    }

    fun getLastUpdatedMillis(): Long = prefs.getLong(KEY_LAST_UPDATED, 0L)

    fun shouldAutoRefresh(): Boolean {
        val lastUpdated = getLastUpdatedMillis()
        if (lastUpdated <= 0L) return true
        val elapsed = System.currentTimeMillis() - lastUpdated
        return elapsed >= SIX_HOURS_MILLIS
    }

    fun loadCustomOverrides(): Map<String, Double> {
        val rawJson = prefs.getString(KEY_CUSTOM_OVERRIDES_JSON, null) ?: return emptyMap()
        return try {
            val json = JSONObject(rawJson)
            val result = mutableMapOf<String, Double>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val rate = json.optDouble(key, -1.0)
                if (rate > 0.0) {
                    result[key] = rate
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveCustomOverride(fromCode: String, toCode: String, rate: Double): Map<String, Double> {
        if (fromCode == toCode || rate <= 0.0) return loadCustomOverrides()
        val current = loadCustomOverrides().toMutableMap()
        current[pairKey(fromCode, toCode)] = rate
        current.remove(pairKey(toCode, fromCode))
        persistCustomOverrides(current)
        return current
    }

    fun clearCustomOverride(fromCode: String, toCode: String): Map<String, Double> {
        val current = loadCustomOverrides().toMutableMap()
        current.remove(pairKey(fromCode, toCode))
        current.remove(pairKey(toCode, fromCode))
        persistCustomOverrides(current)
        return current
    }

    private fun persistCustomOverrides(map: Map<String, Double>) {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_CUSTOM_OVERRIDES_JSON, json.toString()).apply()
    }

    suspend fun fetchLiveRates(): RateFetchResult = withContext(Dispatchers.IO) {
        val primaryRates = fetchFromPrimaryApi()
        val fetchedMap = primaryRates ?: fetchFromFallbackApi()
        if (fetchedMap != null && fetchedMap.isNotEmpty()) {
            val merged = loadCachedRatesPerUsd().toMutableMap()
            merged.putAll(fetchedMap)
            merged["USD"] = 1.0
            val now = System.currentTimeMillis()
            val json = JSONObject()
            merged.forEach { (code, value) -> json.put(code, value) }
            prefs.edit()
                .putString(KEY_RATES_JSON, json.toString())
                .putLong(KEY_LAST_UPDATED, now)
                .apply()
            RateFetchResult(
                isLiveSuccess = true,
                ratesPerUsd = merged,
                lastUpdatedMillis = now
            )
        } else {
            RateFetchResult(
                isLiveSuccess = false,
                ratesPerUsd = loadCachedRatesPerUsd(),
                lastUpdatedMillis = getLastUpdatedMillis()
            )
        }
    }

    private fun fetchFromPrimaryApi(): Map<String, Double>? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(PRIMARY_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val ratesObj = root.optJSONObject("rates") ?: return null
                parseRatesObject(ratesObj, uppercaseKeys = true)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchFromFallbackApi(): Map<String, Double>? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(FALLBACK_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val ratesObj = root.optJSONObject("usd") ?: return null
                parseRatesObject(ratesObj, uppercaseKeys = false)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseRatesObject(ratesObj: JSONObject, uppercaseKeys: Boolean): Map<String, Double> {
        val parsed = mutableMapOf<String, Double>()
        for (currency in SUPPORTED_CURRENCIES) {
            val lookupKey = if (uppercaseKeys) currency.code else currency.code.lowercase(Locale.US)
            if (ratesObj.has(lookupKey)) {
                val value = ratesObj.optDouble(lookupKey, -1.0)
                if (value > 0.0) {
                    parsed[currency.code] = value
                }
            }
        }
        return parsed
    }

    companion object {
        private const val PREFS_NAME = "hishab_pati_currency_rates_prefs"
        private const val KEY_RATES_JSON = "rates_per_usd_json"
        private const val KEY_LAST_UPDATED = "last_updated_millis"
        private const val KEY_CUSTOM_OVERRIDES_JSON = "custom_pair_overrides_json"
        private const val SIX_HOURS_MILLIS = 6 * 60 * 60 * 1000L

        private const val PRIMARY_API_URL = "https://open.er-api.com/v6/latest/USD"
        private const val FALLBACK_API_URL =
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"

        fun pairKey(fromCode: String, toCode: String): String = "${fromCode}_${toCode}"

        fun getLivePairRate(
            fromCode: String,
            toCode: String,
            ratesPerUsd: Map<String, Double>
        ): Double {
            if (fromCode == toCode) return 1.0
            val fromPerUsd = ratesPerUsd[fromCode] ?: 1.0
            val toPerUsd = ratesPerUsd[toCode] ?: 1.0
            if (fromPerUsd <= 0.0) return 1.0
            return toPerUsd / fromPerUsd
        }

        fun getEffectivePairRate(
            fromCode: String,
            toCode: String,
            ratesPerUsd: Map<String, Double>,
            customOverrides: Map<String, Double>
        ): Pair<Double, Boolean> {
            if (fromCode == toCode) return 1.0 to false
            val directCustom = customOverrides[pairKey(fromCode, toCode)]
            if (directCustom != null && directCustom > 0.0) {
                return directCustom to true
            }
            val inverseCustom = customOverrides[pairKey(toCode, fromCode)]
            if (inverseCustom != null && inverseCustom > 0.0) {
                return (1.0 / inverseCustom) to true
            }
            return getLivePairRate(fromCode, toCode, ratesPerUsd) to false
        }
    }
}
