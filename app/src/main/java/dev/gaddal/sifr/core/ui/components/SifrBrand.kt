package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.Amiri
import dev.gaddal.sifr.core.ui.theme.PalettePreviewProvider
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

private const val AR_SIFR = "صفر"

/**
 * Per-theme brand lockup for the top-bar start slot (spec §4.2 / screen-specs "Top app bar").
 * Switches on the active SifrPalette; Dynamic falls back to the Layl-style lockup.
 * Sizes, colors, gaps, and structure are a 1:1 clone of the prototype's SifrBrand
 * (reference_prototype/app/ui-bits.jsx). The Arabic صفر always uses Amiri.
 */
@Composable
fun SifrBrand(modifier: Modifier = Modifier) {
    val sifr = SifrTokens.colors
    when (SifrTokens.palette) {
        SifrPalette.Bayan -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StarMark(color = sifr.accent)
            Text("SIFR", color = sifr.text, fontWeight = FontWeight.W900, fontSize = 15.sp, letterSpacing = 0.9.sp, fontFamily = sifr.uiFamily)
            Text(AR_SIFR, color = sifr.text.copy(alpha = 0.65f), fontFamily = Amiri, fontSize = 16.sp)
        }
        SifrPalette.Raqim -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(AR_SIFR, color = sifr.text, fontFamily = Amiri, fontSize = 23.sp)
            Box(Modifier.size(5.dp).clip(CircleShape).background(sifr.accent))
        }
        SifrPalette.Farah -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(23.dp).clip(CircleShape).background(sifr.accent), contentAlignment = Alignment.Center) {
                Text("٠", color = Color.White, fontFamily = Amiri, fontSize = 13.sp, modifier = Modifier.padding(bottom = 2.dp))   // ٠ optical nudge
            }
            Text("Sifr", color = sifr.text, fontWeight = FontWeight.W700, fontSize = 17.sp, fontFamily = sifr.uiFamily)
        }
        SifrPalette.Mizan -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SIFR", color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 12.sp, letterSpacing = 3.6.sp)
            Text(AR_SIFR, color = sifr.dim.copy(alpha = 0.8f), fontFamily = Amiri, fontSize = 14.sp)
        }
        else -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Layl + Dynamic fallback — baseline-aligned (prototype alignItems: baseline)
            Text(AR_SIFR, color = sifr.accent, fontFamily = Amiri, fontSize = 22.sp, modifier = Modifier.alignByBaseline())
            Text("SIFR", color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 10.sp, letterSpacing = 2.8.sp, modifier = Modifier.alignByBaseline())
        }
    }
}

/** Eight-point star (two overlapping squares, one rotated 45°) — Bayan brand mark.
 *  Mirrors the prototype's SifrStarMark: inner squares inset 12% of the box (size × 0.76). */
@Composable
private fun StarMark(color: Color, size: Dp = 13.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        val inner = size * 0.76f
        Box(Modifier.size(inner).background(color))
        Box(Modifier.size(inner).rotate(45f).background(color))
    }
}

@Preview(name = "SifrBrand — all palettes", showBackground = true)
@Composable
private fun PreviewSifrBrand(@PreviewParameter(PalettePreviewProvider::class) palette: SifrPalette) =
    SifrTheme(palette = palette, themeMode = ThemeMode.Dark) {
        SifrBrand(Modifier.padding(16.dp))
    }
