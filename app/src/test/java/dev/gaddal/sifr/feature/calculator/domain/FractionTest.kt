package dev.gaddal.sifr.feature.calculator.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FractionTest {

    @Test fun `half maps to 1 over 2`() {
        assertThat(0.5.toFraction()).isEqualTo(Fraction(sign = 1, whole = 0, n = 1, d = 2))
    }

    @Test fun `one third maps to 1 over 3`() {
        assertThat((1.0 / 3.0).toFraction()).isEqualTo(Fraction(1, 0, 1, 3))
    }

    @Test fun `three quarters maps to 3 over 4`() {
        assertThat(0.75.toFraction()).isEqualTo(Fraction(1, 0, 3, 4))
    }

    @Test fun `mixed number keeps the whole part`() {
        assertThat(2.5.toFraction()).isEqualTo(Fraction(1, 2, 1, 2))
    }

    @Test fun `negative value carries a negative sign`() {
        assertThat((-0.25).toFraction()).isEqualTo(Fraction(-1, 0, 1, 4))
        assertThat((-2.5).toFraction()).isEqualTo(Fraction(-1, 2, 1, 2))
    }

    @Test fun `exact integer returns null`() {
        assertThat(7.0.toFraction()).isNull()
    }

    @Test fun `irrational-ish value beyond denominator cap returns null`() {
        // pi has no fraction with denominator <= 9999 within tolerance
        assertThat(Math.PI.toFraction()).isNull()
    }

    @Test fun `non-finite returns null`() {
        assertThat(Double.NaN.toFraction()).isNull()
        assertThat(Double.POSITIVE_INFINITY.toFraction()).isNull()
        assertThat(Double.NEGATIVE_INFINITY.toFraction()).isNull()
    }
}
