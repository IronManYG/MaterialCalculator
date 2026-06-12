package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Sifr pill chip. `chipRadius` is fully rounded (999 → RoundedCornerShape(50%)).
 * - Outline (default): hairline border, dim text.
 * - active = true: accent border + accent text (e.g. RAD, M).
 * - filled = true: solid accent background + accentInk text.
 * Click + ripple are confined to the chip (never a parent row) — see spec §4.7.
 */
@Composable
fun SifrChip(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    filled: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(50)
    val content = when {
        filled -> sifr.accentInk
        active -> sifr.accent
        else -> sifr.dim
    }
    val borderColor = if (active || filled) sifr.accent else sifr.hairline

    var m = modifier.clip(shape)
    if (filled) m = m.background(sifr.accent) else m = m.border(BorderStroke(1.dp, borderColor), shape)
    if (onClick != null) m = m.clickable(role = Role.Button) { onClick() }
    m = m.padding(horizontal = 12.dp, vertical = 5.dp)

    Text(
        text = label,
        color = content,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp,
        textAlign = TextAlign.Center,
        fontFamily = sifr.uiFamily,
        modifier = m,
    )
}

@Preview(name = "SifrChip — Layl dark", showBackground = true)
@Composable
private fun PreviewSifrChips() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(12.dp),
    ) {
        SifrChip(label = "DEG", onClick = {})
        SifrChip(label = "RAD", active = true, onClick = {})
        SifrChip(label = "M", active = true)
        SifrChip(label = "COPY", onClick = {})
    }
}
