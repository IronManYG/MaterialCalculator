package dev.gaddal.sifr.feature.tools.domain

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a Gregorian [LocalDate] in the Islamic (Umm al-Qura) calendar — the variant
 * java.time's [HijrahDate] implements, and Saudi Arabia's official calendar. Month names
 * and numerals come localized from the formatter (Hijri month names in ar, e.g. ذو الحجة;
 * Eastern-Arabic numerals follow the locale). Desugared on minSdk 24.
 */
object HijriDate {

    /** e.g. "29 Dhuʼl-Hijjah 1447" (en) / "٢٩ ذو الحجة ١٤٤٧" (ar). */
    fun format(date: LocalDate, locale: Locale): String {
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
        return HijrahDate.from(date).format(formatter)
    }
}
