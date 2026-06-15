package dev.gaddal.sifr.feature.tools.domain

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * Formats a Gregorian [LocalDate] in the Islamic (Umm al-Qura) calendar — the variant
 * java.time's [HijrahDate] implements, and Saudi Arabia's official calendar.
 *
 * Month names are rendered from a hardcoded table rather than the formatter's `MMM`/`MMMM`
 * CLDR lookup: the core-library-desugared [HijrahDate] on minSdk 24 ships no month-name text
 * data, so on-device the CLDR lookup collapsed to nothing and the month name vanished (the
 * round-2 device-QA bug). The arithmetic — day/month/year and the 29/30-day month lengths —
 * still comes from [HijrahDate] (the Umm al-Qura tables), so this stays accurate; only the
 * month *label* is ours. Numerals follow font shaping (the Arabic font renders Western digits
 * as Eastern-Arabic, like the rest of the app), so the day/year are emitted as plain ints.
 */
object HijriDate {

    private val MONTHS_AR = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    // Common pan-Arab transliteration (matches the Intl / CLDR Umm al-Qura set in spirit;
    // ASCII to avoid font-glyph surprises).
    private val MONTHS_EN = arrayOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula",
        "Jumada al-Akhirah", "Rajab", "Shaban", "Ramadan", "Shawwal",
        "Dhu al-Qadah", "Dhu al-Hijjah",
    )

    /** e.g. "29 Dhu al-Hijjah 1447" (en) / "٢٩ ذو الحجة ١٤٤٧" (ar, via font shaping). */
    fun format(date: LocalDate, locale: Locale): String {
        val hijrah = HijrahDate.from(date)
        val day = hijrah.get(ChronoField.DAY_OF_MONTH)
        val month = hijrah.get(ChronoField.MONTH_OF_YEAR) // 1..12
        val year = hijrah.get(ChronoField.YEAR_OF_ERA)
        val names = if (locale.language == "ar") MONTHS_AR else MONTHS_EN
        return "$day ${names[month - 1]} $year"
    }
}
