package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `length 100 m to ft`() {
        val result = UnitConverter.convert(UnitCategory.Length, 100.0, "m", "ft")
        assertThat(result).isWithin(1e-6).of(328.0839895013123)
    }

    @Test
    fun `length round trips`() {
        val ft = UnitConverter.convert(UnitCategory.Length, 1.0, "km", "ft")
        val km = UnitConverter.convert(UnitCategory.Length, ft, "ft", "km")
        assertThat(km).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `weight 1 kg to lb`() {
        val result = UnitConverter.convert(UnitCategory.Weight, 1.0, "kg", "lb")
        assertThat(result).isWithin(1e-6).of(2.2046226218)
    }

    @Test
    fun `temp 100 C to F`() {
        val result = UnitConverter.convert(UnitCategory.Temp, 100.0, "°C", "°F")
        assertThat(result).isWithin(1e-9).of(212.0)
    }

    @Test
    fun `temp 32 F to C`() {
        val result = UnitConverter.convert(UnitCategory.Temp, 32.0, "°F", "°C")
        assertThat(result).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `temp 0 C to K`() {
        val result = UnitConverter.convert(UnitCategory.Temp, 0.0, "°C", "K")
        assertThat(result).isWithin(1e-9).of(273.15)
    }

    @Test
    fun `data 1 GB to MB decimal`() {
        val result = UnitConverter.convert(UnitCategory.Data, 1.0, "GB", "MB")
        assertThat(result).isWithin(1e-6).of(1000.0)
    }

    @Test
    fun `data 1 KiB to B binary`() {
        val result = UnitConverter.convert(UnitCategory.Data, 1.0, "KiB", "B")
        assertThat(result).isWithin(1e-6).of(1024.0)
    }

    @Test
    fun `units for category come from the table in declared order`() {
        assertThat(UnitCategory.Length.units).containsExactly(
            "m", "km", "cm", "mm", "mi", "ft", "in", "yd",
        ).inOrder()
    }
}
