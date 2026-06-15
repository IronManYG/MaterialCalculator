package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class CurrencyDisplayTest {

    @Test
    fun `name returns the localized currency name`() {
        assertThat(CurrencyDisplay.name("USD", Locale.ENGLISH)).contains("Dollar")
        assertThat(CurrencyDisplay.name("SAR", Locale.ENGLISH)).contains("Riyal")
    }

    @Test
    fun `name falls back to the code for an unknown currency`() {
        assertThat(CurrencyDisplay.name("ZZZ", Locale.ENGLISH)).isEqualTo("ZZZ")
    }

    @Test
    fun `flag derives the emoji from a real country code`() {
        assertThat(CurrencyDisplay.flag("USD")).isEqualTo("🇺🇸") // 🇺🇸
        assertThat(CurrencyDisplay.flag("SAR")).isEqualTo("🇸🇦") // 🇸🇦
        assertThat(CurrencyDisplay.flag("GBP")).isEqualTo("🇬🇧") // 🇬🇧
    }

    @Test
    fun `flag handles the EU exception`() {
        assertThat(CurrencyDisplay.flag("EUR")).isEqualTo("🇪🇺") // 🇪🇺
    }

    @Test
    fun `flag is null for supranational and metal codes with no country`() {
        assertThat(CurrencyDisplay.flag("XAF")).isNull() // Central African CFA franc
        assertThat(CurrencyDisplay.flag("XAU")).isNull() // gold
        assertThat(CurrencyDisplay.flag("XDR")).isNull() // IMF special drawing rights
    }

    @Test
    fun `flag is null for malformed codes`() {
        assertThat(CurrencyDisplay.flag("US")).isNull()
        assertThat(CurrencyDisplay.flag("")).isNull()
    }
}
