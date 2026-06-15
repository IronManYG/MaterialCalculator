package dev.gaddal.sifr.feature.tools.domain

import java.util.Currency
import java.util.Locale

/**
 * Presentation helpers for currency codes. Names come localized from [java.util.Currency]
 * (no app strings), and flags are derived from the code's country prefix as emoji regional
 * indicators — with a null fallback for the supranational / metal codes (XAF, XAU, XDR, …)
 * that have no single country.
 */
object CurrencyDisplay {

    private val isoCountries: Set<String> = Locale.getISOCountries().toSet()

    /** Localized currency name (e.g. "US Dollar" / "دولار أمريكي"), or [code] if unknown. */
    fun name(code: String, locale: Locale): String =
        runCatching { Currency.getInstance(code).getDisplayName(locale) }.getOrNull() ?: code

    /**
     * Emoji flag for [code], or null when its first two letters don't name a real country.
     * Most ISO-4217 codes start with their ISO-3166 country (USD→US, SAR→SA, GBP→GB); EUR→EU
     * is the one supranational code with a standard flag, allow-listed here.
     */
    fun flag(code: String): String? {
        if (code.length != 3) return null
        val region = code.substring(0, 2).uppercase()
        if (region.any { it !in 'A'..'Z' }) return null
        if (region != "EU" && region !in isoCountries) return null
        return region
            .map { 0x1F1E6 + (it - 'A') }
            .joinToString("") { String(Character.toChars(it)) }
    }
}
