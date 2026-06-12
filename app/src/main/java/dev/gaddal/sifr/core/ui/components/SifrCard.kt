package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import androidx.compose.material3.Text
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Sifr surface card (spec §4.3): surface bg, 1px surfaceBorder, radius 18 (0 if mosaic), clipped.
 * Used to wrap Settings sections and the History list. Children supply their own padding/rows.
 */
@Composable
fun SifrCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sifr = SifrTokens.colors
    val radius = if (sifr.mosaic) 0.dp else 18.dp
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(sifr.surface)
            .border(BorderStroke(1.dp, sifr.surfaceBorder), shape),
        content = content,
    )
}

/** 1px hairline divider for between-row use inside a SifrCard. Omit after the last row. */
@Composable
fun SifrRowDivider(modifier: Modifier = Modifier) {
    val sifr = SifrTokens.colors
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = sifr.hairline)
}

@Preview(name = "SifrCard — Farah light", showBackground = true)
@Composable
private fun PreviewSifrCard() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
    SifrCard(Modifier.padding(16.dp)) {
        Text("Row one", color = SifrTokens.colors.text, modifier = Modifier.padding(13.dp))
        SifrRowDivider()
        Text("Row two", color = SifrTokens.colors.text, modifier = Modifier.padding(13.dp))
    }
}
