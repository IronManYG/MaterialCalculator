package dev.gaddal.sifr.core.domain.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

/**
 * Locale-independent number formatting for Tools, mirroring the calculator's
 * [dev.gaddal.sifr.feature.calculator.domain] result formatter so converter
 * outputs read identically to the keypad's results. Western digits + Locale.ROOT;
 * Eastern-Arabic numerals are produced downstream by font shaping, not here.
 */
object SifrNumberFormat {

    private const val MAX_SIG_DIGITS = 12
    private const val SNAP_TO_ZERO_THRESHOLD = 1e-12
    private const val SCI_UPPER_THRESHOLD = 1e15
    private const val SCI_LOWER_THRESHOLD = 1e-9

    fun format(value: Double): String {
        if (value.isNaN()) return ""
        if (!value.isFinite()) return "∞" // ∞
        if (value == 0.0) return "0"

        val absValue = abs(value)
        if (absValue < SNAP_TO_ZERO_THRESHOLD) return "0"

        return if (absValue >= SCI_UPPER_THRESHOLD || absValue < SCI_LOWER_THRESHOLD) {
            formatScientific(value)
        } else {
            formatFixed(value, absValue)
        }
    }

    private fun formatFixed(value: Double, absValue: Double): String {
        val fractionDigits = if (absValue >= 1.0) {
            val integerDigits = floor(log10(absValue)).toInt() + 1
            (MAX_SIG_DIGITS - integerDigits).coerceAtLeast(0)
        } else {
            val leadingZeros = -floor(log10(absValue)).toInt() - 1
            leadingZeros + MAX_SIG_DIGITS
        }
        val raw = String.format(Locale.ROOT, "%.${fractionDigits}f", value)
        return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
    }

    private fun formatScientific(value: Double): String {
        val raw = String.format(Locale.ROOT, "%.${MAX_SIG_DIGITS - 1}e", value)
        val eIndex = raw.indexOfAny(charArrayOf('e', 'E'))
        val mantissaRaw = raw.substring(0, eIndex)
        val exponent = raw.substring(eIndex + 1).toInt()
        val mantissa = if (mantissaRaw.contains('.')) mantissaRaw.trimEnd('0').trimEnd('.') else mantissaRaw
        return "${mantissa}E$exponent"
    }
}
