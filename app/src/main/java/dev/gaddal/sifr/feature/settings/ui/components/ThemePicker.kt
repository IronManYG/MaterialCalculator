package dev.gaddal.sifr.feature.settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.theme.sifrColorsFor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePicker(
    selected: SifrPalette,
    dark: Boolean,
    onSelect: (SifrPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SifrTokens.colors
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SifrPalette.entries.forEach { palette ->
            val swatch = sifrColorsFor(palette, dark)
            val isSelected = palette == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(swatch.backgroundFlat)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) tokens.accent else swatch.surfaceBorder,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(palette) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("صفر", color = swatch.accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Dot(swatch.displayExpression); Dot(swatch.accent); Dot(swatch.keyEq.content)
                }
                Spacer(Modifier.size(6.dp))
                Text(stringResource(palette.labelRes()), color = swatch.dim, fontSize = 11.sp)
            }
        }
    }
}

/** Localized swatch label. Brand names stay Latin in every locale; only Dynamic translates. */
private fun SifrPalette.labelRes(): Int = when (this) {
    SifrPalette.Layl -> R.string.settings_palette_layl
    SifrPalette.Bayan -> R.string.settings_palette_bayan
    SifrPalette.Raqim -> R.string.settings_palette_raqim
    SifrPalette.Farah -> R.string.settings_palette_farah
    SifrPalette.Mizan -> R.string.settings_palette_mizan
    SifrPalette.Dynamic -> R.string.settings_palette_dynamic
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}
