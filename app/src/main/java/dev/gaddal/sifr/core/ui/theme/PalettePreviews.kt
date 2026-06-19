package dev.gaddal.sifr.core.ui.theme

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.gaddal.sifr.core.domain.settings.SifrPalette

class PalettePreviewProvider : PreviewParameterProvider<SifrPalette> {
    override val values = SifrPalette.entries.asSequence()
}
