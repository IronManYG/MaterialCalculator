package dev.gaddal.sifr.core.domain.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SifrPaletteTest {
    @Test fun `default palette is Layl`() {
        assertThat(AppSettings().palette).isEqualTo(SifrPalette.Layl)
    }

    @Test fun `every palette name round-trips through valueOf`() {
        SifrPalette.entries.forEach { p ->
            assertThat(SifrPalette.valueOf(p.name)).isEqualTo(p)
        }
    }

    @Test fun `dynamic is the sixth option`() {
        assertThat(SifrPalette.entries).hasSize(6)
        assertThat(SifrPalette.entries.last()).isEqualTo(SifrPalette.Dynamic)
    }
}
