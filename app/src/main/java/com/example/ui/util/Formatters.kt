package com.example.ui.util

import com.example.data.model.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    var currentLanguage: AppLanguage = AppLanguage.ENGLISH

    fun format(
        amount: Double,
        symbol: String = "৳",
        showSign: Boolean = false,
        language: AppLanguage = currentLanguage
    ): String {
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)

        val formattedNumber = formatSouthAsian(absAmount)
        val localizedNumber = if (language == AppLanguage.BANGLA) {
            convertToBanglaDigits(formattedNumber)
        } else {
            formattedNumber
        }

        return when {
            isNegative -> "-$symbol$localizedNumber"
            showSign && amount > 0 -> "+$symbol$localizedNumber"
            else -> "$symbol$localizedNumber"
        }
    }

    private fun formatSouthAsian(amount: Double): String {
        val whole = amount.toLong()
        val frac = Math.round((amount - whole) * 100.0).toInt()

        val wholeStr = whole.toString()
        val len = wholeStr.length

        val sb = StringBuilder()
        if (len <= 3) {
            sb.append(wholeStr)
        } else {
            val lastThree = wholeStr.substring(len - 3)
            val rest = wholeStr.substring(0, len - 3)

            val restWithCommas = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                restWithCommas.append(rest[i])
                count++
                if (count == 2 && i > 0) {
                    restWithCommas.append(',')
                    count = 0
                }
            }
            sb.append(restWithCommas.reverse())
            sb.append(',')
            sb.append(lastThree)
        }

        if (frac > 0) {
            sb.append('.').append(if (frac < 10) "0$frac" else frac.toString())
        }

        return sb.toString()
    }

    fun convertToBanglaDigits(input: String): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (c in input) {
            if (c in '0'..'9') {
                sb.append(banglaDigits[c - '0'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun localizeDigits(input: String, language: AppLanguage = currentLanguage): String {
        return if (language == AppLanguage.BANGLA) convertToBanglaDigits(input) else input
    }

    fun convertToEnglishDigits(input: String): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                '০' -> sb.append('0')
                '১' -> sb.append('1')
                '২' -> sb.append('2')
                '৩' -> sb.append('3')
                '৪' -> sb.append('4')
                '৫' -> sb.append('5')
                '৬' -> sb.append('6')
                '৭' -> sb.append('7')
                '৮' -> sb.append('8')
                '৯' -> sb.append('9')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    fun parseLocalizedDouble(input: String): Double? {
        val english = convertToEnglishDigits(input.trim())
            .replace(",", "")
            .replace("৳", "")
            .replace("$", "")
            .trim()
        return english.toDoubleOrNull()
    }

    fun formatPercent(
        percent: Number,
        language: AppLanguage = currentLanguage
    ): String {
        val rounded = Math.round(percent.toDouble()).toInt()
        val text = "$rounded%"
        return if (language == AppLanguage.BANGLA) convertToBanglaDigits(text) else text
    }

    fun formatNumber(
        number: Number,
        language: AppLanguage = currentLanguage
    ): String {
        val str = number.toString()
        return if (language == AppLanguage.BANGLA) convertToBanglaDigits(str) else str
    }

    fun formatCompact(
        amount: Double,
        symbol: String = "৳",
        language: AppLanguage = currentLanguage
    ): String {
        val isNeg = amount < 0
        val abs = kotlin.math.abs(amount)
        val text = when {
            abs >= 10000000 -> String.format(Locale.US, "%.1fCr", abs / 10000000.0)
            abs >= 100000 -> String.format(Locale.US, "%.1fL", abs / 100000.0)
            abs >= 1000 -> String.format(Locale.US, "%.1fK", abs / 1000.0)
            else -> abs.toInt().toString()
        }
        val localized = if (language == AppLanguage.BANGLA) convertToBanglaDigits(text) else text
        return if (isNeg) "-$symbol$localized" else "$symbol$localized"
    }
}

object DateFormatter {
    private val dateOnlyFormat = SimpleDateFormat("EEE, dd MMM", Locale.US)
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val fullDateFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val shortMonthYearFormat = SimpleDateFormat("MMM yyyy", Locale.US)

    fun localizeText(formatted: String, language: AppLanguage): String {
        if (language != AppLanguage.BANGLA) return formatted
        var result = formatted
        // Weekdays
        result = result.replace("Mon", "সোম")
            .replace("Tue", "মঙ্গল")
            .replace("Wed", "বুধ")
            .replace("Thu", "বৃহঃ")
            .replace("Fri", "শুক্র")
            .replace("Sat", "শনি")
            .replace("Sun", "রবি")
        // Full Months
        result = result.replace("January", "জানুয়ারি")
            .replace("February", "ফেব্রুয়ারি")
            .replace("March", "মার্চ")
            .replace("April", "এপ্রিল")
            .replace("May", "মে")
            .replace("June", "জুন")
            .replace("July", "জুলাই")
            .replace("August", "আগস্ট")
            .replace("September", "সেপ্টেম্বর")
            .replace("October", "অক্টোবর")
            .replace("November", "নভেম্বর")
            .replace("December", "ডিসেম্বর")
        // Short Months
        result = result.replace("Jan", "জানু")
            .replace("Feb", "ফেব্রু")
            .replace("Mar", "মার্চ")
            .replace("Apr", "এপ্রিল")
            .replace("Jun", "জুন")
            .replace("Jul", "জুলাই")
            .replace("Aug", "আগস্ট")
            .replace("Sep", "সেপ্টে")
            .replace("Oct", "অক্টো")
            .replace("Nov", "নভে")
            .replace("Dec", "ডিসে")
        // AM / PM
        result = result.replace("AM", "এএম")
            .replace("PM", "পিএম")
            .replace("am", "এএম")
            .replace("pm", "পিএম")

        return CurrencyFormatter.convertToBanglaDigits(result)
    }

    fun formatDateGroup(timestamp: Long, language: AppLanguage = CurrencyFormatter.currentLanguage): String {
        val formatted = dateOnlyFormat.format(Date(timestamp))
        return if (language == AppLanguage.BANGLA) {
            localizeText(formatted, language)
        } else {
            formatted.uppercase(Locale.US)
        }
    }

    fun formatTime(timestamp: Long, language: AppLanguage = CurrencyFormatter.currentLanguage): String {
        val formatted = timeFormat.format(Date(timestamp))
        return if (language == AppLanguage.BANGLA) {
            localizeText(formatted, language)
        } else {
            formatted
        }
    }

    fun formatFull(timestamp: Long, language: AppLanguage = CurrencyFormatter.currentLanguage): String {
        val formatted = fullDateFormat.format(Date(timestamp))
        return if (language == AppLanguage.BANGLA) {
            localizeText(formatted, language)
        } else {
            formatted
        }
    }

    fun formatMonthYear(timestamp: Long, language: AppLanguage = CurrencyFormatter.currentLanguage): String {
        val formatted = monthYearFormat.format(Date(timestamp))
        return if (language == AppLanguage.BANGLA) {
            localizeText(formatted, language)
        } else {
            formatted
        }
    }

    fun formatShortMonthYear(timestamp: Long, language: AppLanguage = CurrencyFormatter.currentLanguage): String {
        val formatted = shortMonthYearFormat.format(Date(timestamp))
        return if (language == AppLanguage.BANGLA) {
            localizeText(formatted, language)
        } else {
            formatted
        }
    }
}
