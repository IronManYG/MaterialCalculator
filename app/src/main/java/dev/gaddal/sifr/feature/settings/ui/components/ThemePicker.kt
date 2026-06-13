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
    // 3 swatches per row (prototype screens.jsx:38 — flex 1 1 30%): the 6 palettes
    // land as 3 + 3. weight(1f) keeps the columns equal width.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3,
    ) {
        SifrPalette.entries.forEach { palette ->
            val swatch = sifrColorsFor(palette, dark)
            val isSelected = palette == selected
            Column(
                modifier = Modifier
                    .weight(1f)
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
                // One name per swatch (prototype ThemeSwatch leads with the palette's
                // own name): the localized label — Arabic script in ar, romanized in en —
                // in the palette's accent. The redundant generic "صفر"/dim label is dropped.
                Text(
                    text = stringResource(palette.labelRes()),
                    color = swatch.accent,
                    fontWeight = FontWeight.Bold,
                    // 15sp keeps the longest name ("Dynamic") on a single line in the
                    // narrow 3-per-row swatch; shorter names still read clearly.
                    fontSize = 15.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Dot(swatch.displayExpression); Dot(swatch.accent); Dot(swatch.keyEq.content)
                }
            }
        }
    }
}

/** Localized swatch label. AR renders the palette names in Arabic script to match the
 *  prototype (i18n.js — ليل/بيان/رقيم/فرح/ميزان); EN keeps the romanized forms. */
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
