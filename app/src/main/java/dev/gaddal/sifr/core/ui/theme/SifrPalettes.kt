package dev.gaddal.sifr.core.ui.theme

import androidx.compose.material3.ColorScheme as M3ColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.core.domain.settings.SifrPalette

// ---------- LAYL (dark glass, neon-teal glow) ----------

internal fun laylDark(accent: Color = Color(0xFF5CE8D4)): SifrColors = SifrColors(
    background = Brush.radialGradient(listOf(Color(0xFF101A2E), Color(0xFF070A12))),
    backgroundFlat = Color(0xFF070A12),
    statusBarLightIcons = true,
    text = Color(0xFFEDF1F7), dim = Color(0xFF5A6478),
    accent = accent, accentInk = Color(0xFF03201B),
    hairline = Color.White.copy(alpha = 0.08f),
    surface = Color.White.copy(alpha = 0.04f),
    surfaceBorder = Color.White.copy(alpha = 0.08f),
    displayExpression = Color(0xFFEDF1F7), displayResult = accent,
    displayError = Color(0xFFFF8080),
    keyNum = SifrKeyStyle(SolidColor(Color.White.copy(0.045f)), Color(0xFFEDF1F7), border = Color.White.copy(0.07f)),
    keyOp = SifrKeyStyle(SolidColor(accent.copy(0.06f)), accent, border = accent.copy(0.28f)),
    keyEq = SifrKeyStyle(SolidColor(accent), Color(0xFF03201B)),
    keyFn = SifrKeyStyle(SolidColor(Color.White.copy(0.02f)), Color(0xFF8B94A8), border = Color.White.copy(0.05f)),
    keyRadius = 20.dp, keyGap = 10.dp,
    eqGlow = accent.copy(0.45f),
    displayFamily = SpaceGrotesk, keyFamily = SpaceGrotesk, uiFamily = SpaceGrotesk,
    arabicUiFamily = Cairo,
    cyrillicUiFamily = Manrope,
)

internal fun laylLight(accent: Color = Color(0xFF0E9C8C)): SifrColors = SifrColors(
    background = Brush.linearGradient(listOf(Color(0xFFEDF4F9), Color(0xFFDCE7F1))),
    backgroundFlat = Color(0xFFE4EDF5),
    statusBarLightIcons = false,
    text = Color(0xFF16202E), dim = Color(0xFF6A7689),
    accent = accent, accentInk = Color(0xFFF2FBF9),
    hairline = Color(0xFF16202E).copy(0.10f),
    surface = Color.White.copy(0.72f), surfaceBorder = Color(0xFF16202E).copy(0.07f),
    displayExpression = Color(0xFF16202E), displayResult = accent,
    displayError = Color(0xFFC94040),
    keyNum = SifrKeyStyle(SolidColor(Color.White.copy(0.75f)), Color(0xFF16202E), border = Color(0xFF16202E).copy(0.06f)),
    keyOp = SifrKeyStyle(SolidColor(accent.copy(0.08f)), accent, border = accent.copy(0.32f)),
    keyEq = SifrKeyStyle(SolidColor(accent), Color(0xFFF2FBF9)),
    keyFn = SifrKeyStyle(SolidColor(Color.White.copy(0.40f)), Color(0xFF6A7689), border = Color(0xFF16202E).copy(0.05f)),
    keyRadius = 20.dp, keyGap = 10.dp,
    eqGlow = accent.copy(0.40f),
    displayFamily = SpaceGrotesk, keyFamily = SpaceGrotesk, uiFamily = SpaceGrotesk,
    arabicUiFamily = Cairo,
    cyrillicUiFamily = Manrope,
)

// ---------- BAYAN (bold color-blocking, ink mosaic) ----------

internal fun bayanLight(accent: Color = Color(0xFF2C3FE3)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFFF2EDE0)), backgroundFlat = Color(0xFFF2EDE0),
    statusBarLightIcons = false,
    text = Color(0xFF16140F), dim = Color(0xFF7A745F), accent = accent, accentInk = Color(0xFFF2EDE0),
    hairline = Color(0xFF16140F), surface = Color(0xFFFBF8F0), surfaceBorder = Color(0xFF16140F),
    displayExpression = Color(0xFFF2EDE0), displayResult = Color(0xFF8E9BFF),
    displayError = Color(0xFFC94040),
    displayBlock = Color(0xFF16140F), // v1.5 renders the ink block; v1.4 stays flat
    keyNum = SifrKeyStyle(SolidColor(Color(0xFFF2EDE0)), Color(0xFF16140F)),
    keyOp = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyEq = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFF16140F)), Color(0xFFF2EDE0)),
    keyRadius = 0.dp, keyGap = 2.dp,
    mosaic = true, mosaicLine = Color(0xFF16140F),
    displayFamily = Archivo, keyFamily = Archivo, uiFamily = Archivo,
    arabicUiFamily = Tajawal,
    cyrillicUiFamily = Montserrat,
)

internal fun bayanDark(accent: Color = Color(0xFF5B6CFF)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFF14110C)), backgroundFlat = Color(0xFF14110C),
    statusBarLightIcons = true,
    text = Color(0xFFF2EDE0), dim = Color(0xFF8F876E), accent = accent, accentInk = Color(0xFFF2EDE0),
    hairline = Color(0xFFF2EDE0).copy(0.30f), surface = Color(0xFF1E1A12), surfaceBorder = Color(0xFFF2EDE0).copy(0.30f),
    displayExpression = Color(0xFF16140F), displayResult = Color(0xFF2C3FE3),
    displayError = Color(0xFFFF8080),
    displayBlock = Color(0xFFF2EDE0),
    keyNum = SifrKeyStyle(SolidColor(Color(0xFF211D14)), Color(0xFFF2EDE0)),
    keyOp = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyEq = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFFF2EDE0)), Color(0xFF16140F)),
    keyRadius = 0.dp, keyGap = 2.dp,
    mosaic = true, mosaicLine = Color(0xFFF2EDE0).copy(0.35f),
    displayFamily = Archivo, keyFamily = Archivo, uiFamily = Archivo,
    arabicUiFamily = Tajawal,
    cyrillicUiFamily = Montserrat,
)

// ---------- RAQIM (editorial serif, hairline grid) ----------

internal fun raqimLight(accent: Color = Color(0xFF6B7A4F)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFFFAF6EE)), backgroundFlat = Color(0xFFFAF6EE),
    statusBarLightIcons = false,
    text = Color(0xFF2A2520), dim = Color(0xFF9A8F7C), accent = accent, accentInk = Color(0xFFFAF6EE),
    hairline = Color(0xFFE3DACA), surface = Color(0xFFFFFDF7), surfaceBorder = Color(0xFFE3DACA),
    displayExpression = Color(0xFF2A2520), displayResult = accent, displayError = Color(0xFFC94040),
    resultItalic = true,
    keyNum = SifrKeyStyle(SolidColor(Color(0xFFFAF6EE)), Color(0xFF2A2520)),
    keyOp = SifrKeyStyle(SolidColor(Color(0xFFFAF6EE)), accent, italic = true),
    keyEq = SifrKeyStyle(SolidColor(Color(0xFF2A2520)), Color(0xFFFAF6EE)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFFFAF6EE)), Color(0xFF9A8F7C)),
    keyRadius = 0.dp, keyGap = 1.dp, hairlineGrid = true, gridLine = Color(0xFFE3DACA),
    displayFamily = Cormorant, keyFamily = Cormorant, uiFamily = SpaceGrotesk,
    arabicUiFamily = Amiri,
    cyrillicUiFamily = Manrope,
)

internal fun raqimDark(accent: Color = Color(0xFF99A877)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFF211D18)), backgroundFlat = Color(0xFF211D18),
    statusBarLightIcons = true,
    text = Color(0xFFEFE7D8), dim = Color(0xFF9C9079), accent = accent, accentInk = Color(0xFF211D18),
    hairline = Color(0xFFEFE7D8).copy(0.13f), surface = Color(0xFF28231D), surfaceBorder = Color(0xFFEFE7D8).copy(0.12f),
    displayExpression = Color(0xFFEFE7D8), displayResult = accent, displayError = Color(0xFFFF8080),
    resultItalic = true,
    keyNum = SifrKeyStyle(SolidColor(Color(0xFF211D18)), Color(0xFFEFE7D8)),
    keyOp = SifrKeyStyle(SolidColor(Color(0xFF211D18)), accent, italic = true),
    keyEq = SifrKeyStyle(SolidColor(Color(0xFFEFE7D8)), Color(0xFF211D18)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFF211D18)), Color(0xFF9C9079)),
    keyRadius = 0.dp, keyGap = 1.dp, hairlineGrid = true, gridLine = Color(0xFFEFE7D8).copy(0.13f),
    displayFamily = Cormorant, keyFamily = Cormorant, uiFamily = SpaceGrotesk,
    arabicUiFamily = Amiri,
    cyrillicUiFamily = Manrope,
)

// ---------- FARAH (playful pills, soft 3D) ----------

internal fun farahLight(accent: Color = Color(0xFFF2683C)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFFFFF4E4)), backgroundFlat = Color(0xFFFFF4E4),
    statusBarLightIcons = false,
    text = Color(0xFF4A3326), dim = Color(0xFFB08A62), accent = accent, accentInk = Color(0xFFFFF6EC),
    hairline = Color(0xFFF0DFC8), surface = Color(0xFFFFFDF8), surfaceBorder = Color(0xFFF4E3CC),
    displayExpression = Color(0xFF4A3326),
    // displayResult is on-container-tuned; coincides with accent for Farah/Mizan today but kept independent (cf. Bayan, which diverges).
    displayResult = Color(0xFFF2683C), displayError = Color(0xFFC94040),
    displayCard = Color(0xFFFFFDF8),
    keyNum = SifrKeyStyle(SolidColor(Color(0xFFF7E3C8)), Color(0xFF4A3326), dropShadow = Color(0xFFE8CFA9)),
    keyOp = SifrKeyStyle(SolidColor(accent), Color(0xFFFFF6EC), dropShadow = accent.copy(0.55f)),
    keyEq = SifrKeyStyle(SolidColor(Color(0xFF4A3326)), Color(0xFFFFE9CF), dropShadow = Color(0xFF2E1E14)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFFFFD66B)), Color(0xFF4A3326), dropShadow = Color(0xFFE3B945)),
    keyRadius = 999.dp, keyGap = 12.dp, raisedKeys = true,
    displayFamily = Baloo2, keyFamily = Baloo2, uiFamily = Baloo2,
    arabicUiFamily = BalooBhaijaan2,
    cyrillicUiFamily = Comfortaa,
)

internal fun farahDark(accent: Color = Color(0xFFFF7A4D)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFF2A1D13)), backgroundFlat = Color(0xFF2A1D13),
    statusBarLightIcons = true,
    text = Color(0xFFFFE9CF), dim = Color(0xFFC49A6C), accent = accent, accentInk = Color(0xFF2A1D13),
    hairline = Color(0xFFFFE9CF).copy(0.12f), surface = Color(0xFF352617), surfaceBorder = Color(0xFFFFE9CF).copy(0.10f),
    displayExpression = Color(0xFFFFE9CF), displayResult = Color(0xFFFF7A4D), displayError = Color(0xFFFF8080),
    displayCard = Color(0xFF352617),
    keyNum = SifrKeyStyle(SolidColor(Color(0xFF3E2D1C)), Color(0xFFFFE9CF), dropShadow = Color(0xFF271A0E)),
    keyOp = SifrKeyStyle(SolidColor(accent), Color(0xFF2A1D13), dropShadow = Color(0xFF000000).copy(0.40f)),
    keyEq = SifrKeyStyle(SolidColor(Color(0xFFFFE9CF)), Color(0xFF4A3326), dropShadow = Color(0xFFC9A87E)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFFE8B84B)), Color(0xFF3A2914), dropShadow = Color(0xFFA87F26)),
    keyRadius = 999.dp, keyGap = 12.dp, raisedKeys = true,
    displayFamily = Baloo2, keyFamily = Baloo2, uiFamily = Baloo2,
    arabicUiFamily = BalooBhaijaan2,
    cyrillicUiFamily = Comfortaa,
)

// ---------- MIZAN (machined, recessed gradient keys) ----------

internal fun mizanDark(accent: Color = Color(0xFFE2772E)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFF1B1B19)), backgroundFlat = Color(0xFF1B1B19),
    statusBarLightIcons = true,
    text = Color(0xFFF4F1E8), dim = Color(0xFF8E8A80), accent = accent, accentInk = Color(0xFF1B1108),
    hairline = Color.White.copy(0.08f), surface = Color(0xFF222220), surfaceBorder = Color.White.copy(0.07f),
    displayExpression = Color(0xFFF4F1E8), displayResult = Color(0xFFE2772E), displayError = Color(0xFFFF8080),
    displayInset = Color(0xFF111110),
    keyNum = SifrKeyStyle(Brush.verticalGradient(listOf(Color(0xFF272725), Color(0xFF1F1F1D))), Color(0xFFDBD7CE), dropShadow = Color.Black.copy(0.55f), innerTopHighlight = Color.White.copy(0.07f)),
    keyOp = SifrKeyStyle(Brush.verticalGradient(listOf(Color(0xFF272725), Color(0xFF1F1F1D))), accent, dropShadow = Color.Black.copy(0.55f), innerTopHighlight = Color.White.copy(0.07f)),
    keyEq = SifrKeyStyle(Brush.verticalGradient(listOf(accent.copy(0.9f), accent)), Color(0xFF1B1108), dropShadow = Color.Black.copy(0.6f), innerTopHighlight = Color.White.copy(0.25f)),
    keyFn = SifrKeyStyle(Brush.verticalGradient(listOf(Color(0xFF252523), Color(0xFF1E1E1C))), Color(0xFF8E8A80), dropShadow = Color.Black.copy(0.55f), innerTopHighlight = Color.White.copy(0.06f)),
    keyRadius = 16.dp, keyGap = 11.dp, raisedKeys = true,
    displayFamily = PlexMono, keyFamily = PlexMono, uiFamily = PlexMono,
    arabicUiFamily = IBMPlexArabic,
)

internal fun mizanLight(accent: Color = Color(0xFFD96A20)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFFD8D7D2)), backgroundFlat = Color(0xFFD8D7D2),
    statusBarLightIcons = false,
    text = Color(0xFF23221F), dim = Color(0xFF75736C), accent = accent, accentInk = Color(0xFFFFF8EE),
    hairline = Color(0xFF23221F).copy(0.12f), surface = Color(0xFFE6E5E1), surfaceBorder = Color(0xFF23221F).copy(0.10f),
    displayExpression = Color(0xFF23221F), displayResult = Color(0xFFD96A20), displayError = Color(0xFFC94040),
    displayInset = Color(0xFFC8C7C1),
    keyNum = SifrKeyStyle(Brush.verticalGradient(listOf(Color(0xFFFCFCFA), Color(0xFFEDECE8))), Color(0xFF2E2D2A), dropShadow = Color(0xFFB4B3AE), innerTopHighlight = Color.White.copy(0.9f)),
    keyOp = SifrKeyStyle(Brush.verticalGradient(listOf(Color(0xFFFCFCFA), Color(0xFFEDECE8))), accent, dropShadow = Color(0xFFB4B3AE), innerTopHighlight = Color.White.copy(0.9f)),
    keyEq = SifrKeyStyle(Brush.verticalGradient(listOf(accent, accent.copy(0.85f))), Color(0xFFFFF8EE), dropShadow = Color(0xFF8C4513), innerTopHighlight = Color.White.copy(0.3f)),
    keyFn = SifrKeyStyle(Brush.verticalGradient(listOf(Color(0xFFF2F1ED), Color(0xFFE2E1DC))), Color(0xFF75736C), dropShadow = Color(0xFFB0AFAA), innerTopHighlight = Color.White.copy(0.8f)),
    keyRadius = 16.dp, keyGap = 11.dp, raisedKeys = true,
    displayFamily = PlexMono, keyFamily = PlexMono, uiFamily = PlexMono,
    arabicUiFamily = IBMPlexArabic,
)

// ---------- SELECTORS ----------

/** Pure (Context-free) palette resolver. Dynamic falls back to Layl here;
 *  the SifrTheme wrapper substitutes the real dynamic scheme when available. */
fun sifrColorsFor(palette: SifrPalette, dark: Boolean): SifrColors = when (palette) {
    SifrPalette.Layl -> if (dark) laylDark() else laylLight()
    SifrPalette.Bayan -> if (dark) bayanDark() else bayanLight()
    SifrPalette.Raqim -> if (dark) raqimDark() else raqimLight()
    SifrPalette.Farah -> if (dark) farahDark() else farahLight()
    SifrPalette.Mizan -> if (dark) mizanDark() else mizanLight()
    SifrPalette.Dynamic -> if (dark) laylDark() else laylLight()
}

/** Maps an Android-12+ dynamic ColorScheme into SifrColors (flat keys, no bespoke construction). */
fun dynamicToSifrColors(scheme: M3ColorScheme, dark: Boolean): SifrColors {
    val accent = scheme.primary
    return SifrColors(
        background = SolidColor(scheme.background), backgroundFlat = scheme.background,
        statusBarLightIcons = dark,
        text = scheme.onBackground, dim = scheme.onSurfaceVariant,
        accent = accent, accentInk = scheme.onPrimary,
        // `outlineVariant` is near-invisible against the variant surfaces (especially in
        // dark Material You) — card / swatch / picker / chip borders vanished. `outline`
        // is the higher-contrast role meant for visible decorative borders.
        hairline = scheme.outline, surface = scheme.surfaceVariant, surfaceBorder = scheme.outline,
        displayExpression = scheme.onSurface, displayResult = accent, displayError = scheme.error,
        // Material You's key containers (surfaceVariant / secondaryContainer / surface) often sit
        // a hair off — or dead-equal to — the background, so on some wallpapers the keys render as
        // bare floating glyphs with no button shape (the 19371cb border fix only covered cards /
        // chips / pickers). Give every non-accent key a hairline `outline` border so it always
        // reads as a key; the '=' key keeps its solid accent fill (always visible, no border).
        keyNum = SifrKeyStyle(SolidColor(scheme.surfaceVariant), scheme.onSurfaceVariant, border = scheme.outline),
        keyOp = SifrKeyStyle(SolidColor(scheme.secondaryContainer), scheme.onSecondaryContainer, border = scheme.outline),
        keyEq = SifrKeyStyle(SolidColor(accent), scheme.onPrimary),
        keyFn = SifrKeyStyle(SolidColor(scheme.surface), scheme.onSurfaceVariant, border = scheme.outline),
        keyRadius = 24.dp, keyGap = 8.dp,
        displayFamily = SpaceGrotesk, keyFamily = SpaceGrotesk, uiFamily = SpaceGrotesk,
        arabicUiFamily = Cairo,
        cyrillicUiFamily = Manrope,
    )
}
