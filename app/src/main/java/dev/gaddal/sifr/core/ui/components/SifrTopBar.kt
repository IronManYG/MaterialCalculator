package dev.gaddal.sifr.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Calculator-root top bar (spec §4.1): brand at start; icon row at end —
 * History · Tools · Scientific(ƒ) · Rotate · Settings.
 * Icon tint = dim; active = accent (scientific when on; rotate when landscape).
 * `showScientific` is false in landscape (sci is always on there).
 * Caller applies status-bar inset padding.
 */
@Composable
fun SifrCalcTopBar(
    onHistory: () -> Unit,
    onTools: () -> Unit,
    onScientific: () -> Unit,
    scientificActive: Boolean,
    onRotate: () -> Unit,
    rotateActive: Boolean,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showScientific: Boolean = true,
    historyCd: String = "History",
    toolsCd: String = "Tools",
    scientificCd: String = "Scientific",
    rotateCd: String = "Rotate",
    settingsCd: String = "Settings",
) {
    val sifr = SifrTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 22.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SifrBrand()
        Spacer(Modifier.weight(1f))
        BarIcon(Icons.Outlined.History, historyCd, sifr.dim, onHistory)
        BarIcon(Icons.Outlined.GridView, toolsCd, sifr.dim, onTools)
        if (showScientific) {
            // ƒ glyph (italic) — accent when scientific mode is on
            IconButton(
                onClick = onScientific,
                modifier = Modifier.semantics { contentDescription = scientificCd },
            ) {
                Text(
                    text = "ƒ",   // ƒ
                    color = if (scientificActive) sifr.accent else sifr.dim,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.W600,
                    fontSize = 20.sp,
                )
            }
        }
        BarIcon(Icons.Outlined.ScreenRotation, rotateCd, if (rotateActive) sifr.accent else sifr.dim, onRotate)
        BarIcon(Icons.Outlined.Settings, settingsCd, sifr.dim, onSettings)
    }
}

/**
 * Sub-screen top bar (spec §4.1): back arrow (auto-mirrored for RTL) + title, no end icons.
 */
@Composable
fun SifrSubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = 8.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = sifr.text)
        }
        Text(
            text = title,
            color = sifr.text,
            fontFamily = sifr.uiFamily,
            fontWeight = FontWeight.W600,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun BarIcon(icon: ImageVector, cd: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = cd, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Preview(name = "SifrCalcTopBar — Layl dark", showBackground = true)
@Composable
private fun PreviewCalcTopBar() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    SifrCalcTopBar(
        onHistory = {}, onTools = {}, onScientific = {}, scientificActive = true,
        onRotate = {}, rotateActive = false, onSettings = {},
    )
}

@Preview(name = "SifrCalcTopBar — landscape (no ƒ, rotate active)", showBackground = true)
@Composable
private fun PreviewCalcTopBarLandscape() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
    SifrCalcTopBar(
        onHistory = {}, onTools = {}, onScientific = {}, scientificActive = false,
        onRotate = {}, rotateActive = true, onSettings = {}, showScientific = false,
    )
}

@Preview(name = "SifrSubScreenTopBar — Mizan dark", showBackground = true)
@Composable
private fun PreviewSubScreenTopBar() = SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
    SifrSubScreenTopBar(title = "History", onBack = {})
}
