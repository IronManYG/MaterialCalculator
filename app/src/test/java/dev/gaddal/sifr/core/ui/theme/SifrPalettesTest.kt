package dev.gaddal.sifr.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import org.junit.Test

class SifrPalettesTest {
    @Test fun `layl dark uses neon teal accent`() {
        assertThat(sifrColorsFor(SifrPalette.Layl, dark = true).accent).isEqualTo(Color(0xFF5CE8D4))
    }

    @Test fun `bayan light uses indigo accent and mosaic construction`() {
        val c = sifrColorsFor(SifrPalette.Bayan, dark = false)
        assertThat(c.accent).isEqualTo(Color(0xFF2C3FE3))
        assertThat(c.mosaic).isTrue()
    }

    @Test fun `raqim marks operators and result italic`() {
        val c = sifrColorsFor(SifrPalette.Raqim, dark = true)
        assertThat(c.resultItalic).isTrue()
        assertThat(c.keyOp.italic).isTrue()
        assertThat(c.hairlineGrid).isTrue()
    }

    @Test fun `farah keys are pills`() {
        assertThat(sifrColorsFor(SifrPalette.Farah, dark = false).keyRadius.value).isEqualTo(999f)
    }

    @Test fun `every non-dynamic palette resolves in both modes`() {
        SifrPalette.entries.filterNot { it == SifrPalette.Dynamic }.forEach { p ->
            assertThat(sifrColorsFor(p, dark = true)).isNotNull()
            assertThat(sifrColorsFor(p, dark = false)).isNotNull()
        }
    }
}
