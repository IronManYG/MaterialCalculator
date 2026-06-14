package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DateCalculatorTest {

    @Test
    fun `days between two dates is signed difference`() {
        val d = DateCalculator.daysBetween(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 8, 1))
        assertThat(d).isEqualTo(52L)
    }

    @Test
    fun `days between is negative when end precedes start`() {
        val d = DateCalculator.daysBetween(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 6, 10))
        assertThat(d).isEqualTo(-52L)
    }

    @Test
    fun `days between spans leap day`() {
        val d = DateCalculator.daysBetween(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1))
        assertThat(d).isEqualTo(2L) // 2024 is a leap year (Feb 29 exists)
    }

    @Test
    fun `add days crosses month and year boundaries`() {
        val r = DateCalculator.addDays(LocalDate.of(2026, 12, 20), 90)
        assertThat(r).isEqualTo(LocalDate.of(2027, 3, 20))
    }

    @Test
    fun `weeks and remainder breakdown`() {
        val (weeks, days) = DateCalculator.weeksAndDays(52)
        assertThat(weeks).isEqualTo(7)
        assertThat(days).isEqualTo(3)
    }
}
