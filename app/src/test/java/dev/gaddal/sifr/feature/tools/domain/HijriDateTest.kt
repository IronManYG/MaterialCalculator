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
}
