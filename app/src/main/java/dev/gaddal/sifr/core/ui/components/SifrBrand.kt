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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
 */
@Composable
fun SifrBrand(modifier: Modifier = Modifier) {
    val sifr = SifrTokens.colors
    when (SifrTokens.palette) {
        SifrPalette.Bayan -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("✴", color = sifr.accent, fontSize = 16.sp)            // ✴ 8-point star
            Text("SIFR", color = sifr.text, fontWeight = FontWeight.W900, fontSize = 18.sp, letterSpacing = 1.1.sp, fontFamily = sifr.uiFamily)
            Text(AR_SIFR, color = sifr.dim, fontFamily = Amiri, fontSize = 18.sp)
        }
        SifrPalette.Raqim -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(AR_SIFR, color = sifr.text, fontFamily = Amiri, fontSize = 23.sp)
            Box(Modifier.size(5.dp).clip(CircleShape).background(sifr.accent))
        }
        SifrPalette.Farah -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(23.dp).clip(CircleShape).background(sifr.accent), contentAlignment = Alignment.Center) {
                Text("٠", color = sifr.accentInk, fontFamily = Amiri, fontSize = 14.sp)   // ٠
            }
            Text("Sifr", color = sifr.text, fontWeight = FontWeight.W700, fontSize = 18.sp, fontFamily = sifr.uiFamily)
        }
        SifrPalette.Mizan -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SIFR", color = sifr.text, fontFamily = sifr.uiFamily, fontSize = 15.sp, letterSpacing = 4.5.sp)
            Text(AR_SIFR, color = sifr.dim, fontFamily = Amiri, fontSize = 18.sp)
        }
        else -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            // Layl + Dynamic fallback
            Text(AR_SIFR, color = sifr.accent, fontFamily = Amiri, fontSize = 22.sp)
            Text("SIFR", color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 10.sp, letterSpacing = 2.8.sp)
        }
    }
}

@Preview(name = "SifrBrand — all palettes", showBackground = true)
@Composable
private fun PreviewSifrBrand(@PreviewParameter(PalettePreviewProvider::class) palette: SifrPalette) =
    SifrTheme(palette = palette, themeMode = ThemeMode.Dark) {
        SifrBrand(Modifier.padding(16.dp))
    }
