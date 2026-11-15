package com.diary.construction.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money is always stored as a Long in paisa (1 rupee = 100 paisa).
 * These helpers turn user text into paisa and paisa back into text.
 */
object Money {
    private const val PAISA = 100L

    /** "1,250.50" -> 125050. Returns null when the text is not a valid amount. */
    fun parse(text: String): Long? {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty()) return null
        return runCatching {
            BigDecimal(clean).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        }.getOrNull()?.takeIf { it >= 0 }
    }

    /** Value for an edit field: 125050 -> "1250.5", 0 -> "". */
    fun toInput(paisa: Long): String {
        if (paisa == 0L) return ""
        return BigDecimal.valueOf(paisa, 2).stripTrailingZeros().toPlainString()
    }

    /** Display value: 12345600 -> "Rs 123,456". Decimals are shown only when needed. */
    fun format(paisa: Long, symbol: String): String {
        val negative = paisa < 0
        val abs = kotlin.math.abs(paisa)
        val fmt = if (abs % PAISA == 0L) whole() else decimal()
        val body = fmt.format(BigDecimal.valueOf(abs, 2))
        val sign = if (negative) "-" else ""
        return if (symbol.isBlank()) "$sign$body" else "$sign$symbol $body"
    }

    /** Plain number for exports: 125050 -> "1250.50". */
    fun plain(paisa: Long): String = BigDecimal.valueOf(paisa, 2).toPlainString()

    /** a * b / scale, rounded, for rate x quantity. */
    fun multiply(ratePaisa: Long, quantityScaled: Long, scale: Long = Quantity.SCALE): Long =
        BigDecimal.valueOf(ratePaisa).multiply(BigDecimal.valueOf(quantityScaled))
            .divide(BigDecimal.valueOf(scale), 0, RoundingMode.HALF_UP).toLong()

    private fun whole() = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private fun decimal() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
}

/**
 * Quantities (bags, kg, cft, sq ft, marla ...) are stored as a Long with a fixed scale of 100,
 * so 12.5 bags is stored as 1250.
 */
object Quantity {
    const val SCALE = 100L

    fun parse(text: String): Long? {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty()) return null
        return runCatching {
            BigDecimal(clean).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        }.getOrNull()?.takeIf { it >= 0 }
    }

    fun toInput(scaled: Long): String {
        if (scaled == 0L) return ""
        return BigDecimal.valueOf(scaled, 2).stripTrailingZeros().toPlainString()
    }

    fun format(scaled: Long): String {
        val fmt = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
        return fmt.format(BigDecimal.valueOf(scaled, 2))
    }

    fun fromDouble(value: Double): Long = Math.round(value * SCALE)

    fun toDouble(scaled: Long): Double = scaled.toDouble() / SCALE
}
