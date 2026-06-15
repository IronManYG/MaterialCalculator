package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Locale

class HijriDateTest {

    @Test
    fun `format renders the Hijri calendar, not Gregorian`() {
        val gregorian = LocalDate.of(2026, 6, 15)
        val hijriYear = HijrahDate.from(gregorian).get(ChronoField.YEAR_OF_ERA)

        val out = HijriDate.format(gregorian, Locale.ENGLISH)

        assertThat(out).contains(hijriYear.toString()) // e.g. 1447
        assertThat(out).doesNotContain("2026")          // not the Gregorian year
    }

    @Test
    fun `format is non-blank for any date`() {
        assertThat(HijriDate.format(LocalDate.of(2000, 1, 1), Locale.ENGLISH)).isNotEmpty()
    }

    @Test
    fun `format advances the Hijri year across the Hijri new year`() {
        // Two Gregorian dates ~13 months apart must land in different Hijri years.
        val earlier = HijrahDate.from(LocalDate.of(2026, 1, 1)).get(ChronoField.YEAR_OF_ERA)
        val later = HijrahDate.from(LocalDate.of(2027, 2, 1)).get(ChronoField.YEAR_OF_ERA)
        assertThat(later).isGreaterThan(earlier)

        assertThat(HijriDate.format(LocalDate.of(2027, 2, 1), Locale.ENGLISH)).contains(later.toString())
    }

    // 2026-06-15 Gregorian → 29 Dhuʼl-Hijjah 1447 AH. The month NAME must render — the bug
    // was that the desugared HijrahChronology has no month-name text data on-device, so the
    // CLDR `MMM` lookup collapsed to nothing. We now render names from a hardcoded table, so
    // these JVM assertions also hold on the device (no chronology-text dependency left).
    @Test
    fun `format renders the full English Hijri month name`() {
        val out = HijriDate.format(LocalDate.of(2026, 6, 15), Locale.ENGLISH)
        assertThat(out).contains("Dhu al-Hijjah")
    }

    @Test
    fun `format renders the Arabic Hijri month name`() {
        val out = HijriDate.format(LocalDate.of(2026, 6, 15), Locale("ar"))
        assertThat(out).contains("ذو الحجة")
    }

    @Test
    fun `format renders a distinct name for every Hijri month`() {
        val names = listOf(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula",
            "Jumada al-Akhirah", "Rajab", "Shaban", "Ramadan", "Shawwal",
            "Dhu al-Qadah", "Dhu al-Hijjah",
        )
        for (month in 1..12) {
            // Build the 1st of each Hijri month directly, then convert to Gregorian, so the
            // expectation can't drift on a month-boundary guess.
            val gregorian = LocalDate.from(HijrahDate.of(1447, month, 1))
            assertThat(HijriDate.format(gregorian, Locale.ENGLISH)).contains(names[month - 1])
        }
    }
}
