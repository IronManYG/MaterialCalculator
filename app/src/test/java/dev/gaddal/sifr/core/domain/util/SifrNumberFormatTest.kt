package dev.gaddal.sifr.core.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SifrNumberFormatTest {

    @Test
    fun `integer value has no decimal point`() {
        assertThat(SifrNumberFormat.format(42.0)).isEqualTo("42")
    }

    @Test
    fun `trailing zeros are trimmed`() {
        assertThat(SifrNumberFormat.format(1.5000)).isEqualTo("1.5")
    }

    @Test
    fun `exact zero formats as 0`() {
        assertThat(SifrNumberFormat.format(0.0)).isEqualTo("0")
    }

    @Test
    fun `computational zero snaps to 0`() {
        assertThat(SifrNumberFormat.format(1e-16)).isEqualTo("0")
    }

    @Test
    fun `very large magnitude uses scientific notation`() {
        assertThat(SifrNumberFormat.format(1e20)).contains("E")
    }

    @Test
    fun `negative value keeps its sign`() {
        assertThat(SifrNumberFormat.format(-3.25)).isEqualTo("-3.25")
    }

    @Test
    fun `NaN formats as empty string`() {
        assertThat(SifrNumberFormat.format(Double.NaN)).isEqualTo("")
    }

    @Test
    fun `infinity formats as the infinity glyph`() {
        assertThat(SifrNumberFormat.format(Double.POSITIVE_INFINITY)).isEqualTo("∞")
    }

    @Test
    fun `known conversion value formats cleanly`() {
        // 100 m -> ft = 100 / 0.3048
        assertThat(SifrNumberFormat.format(100.0 / 0.3048)).isEqualTo("328.083989501")
    }
}
