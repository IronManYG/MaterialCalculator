package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TipCalculatorTest {

    @Test
    fun `tip total and each for a typical bill`() {
        val r = TipCalculator.compute(bill = 86.0, tipPercent = 15, split = 2)
        assertThat(r.tip).isWithin(1e-9).of(12.9)
        assertThat(r.total).isWithin(1e-9).of(98.9)
        assertThat(r.each).isWithin(1e-9).of(49.45)
    }

    @Test
    fun `split is floored at 1`() {
        val r = TipCalculator.compute(bill = 100.0, tipPercent = 10, split = 0)
        assertThat(r.each).isWithin(1e-9).of(110.0)
    }

    @Test
    fun `zero bill yields zero everything`() {
        val r = TipCalculator.compute(bill = 0.0, tipPercent = 20, split = 3)
        assertThat(r.tip).isEqualTo(0.0)
        assertThat(r.total).isEqualTo(0.0)
        assertThat(r.each).isEqualTo(0.0)
    }
}
