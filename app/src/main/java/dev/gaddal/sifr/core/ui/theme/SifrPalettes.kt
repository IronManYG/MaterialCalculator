package dev.gaddal.sifr.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

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
    ghostNumeral = accent.copy(0.35f),
    eqGlow = accent.copy(0.45f),
    displayFamily = SpaceGrotesk, keyFamily = SpaceGrotesk, uiFamily = SpaceGrotesk,
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
    ghostNumeral = accent.copy(0.5f),
    eqGlow = accent.copy(0.40f),
    displayFamily = SpaceGrotesk, keyFamily = SpaceGrotesk, uiFamily = SpaceGrotesk,
)

// ---------- BAYAN (bold color-blocking, ink mosaic) ----------

internal fun bayanLight(accent: Color = Color(0xFF2C3FE3)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFFF2EDE0)), backgroundFlat = Color(0xFFF2EDE0),
    statusBarLightIcons = false,
    text = Color(0xFF16140F), dim = Color(0xFF7A745F), accent = accent, accentInk = Color(0xFFF2EDE0),
    hairline = Color(0xFF16140F), surface = Color(0xFFFBF8F0), surfaceBorder = Color(0xFF16140F),
    displayExpression = Color(0xFF16140F), displayResult = Color(0xFF2C3FE3),
    displayError = Color(0xFFC94040),
    displayBlock = Color(0xFF16140F), // v1.5 renders the ink block; v1.4 stays flat
    keyNum = SifrKeyStyle(SolidColor(Color(0xFFF2EDE0)), Color(0xFF16140F)),
    keyOp = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyEq = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFF16140F)), Color(0xFFF2EDE0)),
    keyRadius = 0.dp, keyGap = 2.dp,
    mosaic = true, mosaicLine = Color(0xFF16140F),
    displayFamily = Archivo, keyFamily = Archivo, uiFamily = Archivo,
)

internal fun bayanDark(accent: Color = Color(0xFF5B6CFF)): SifrColors = SifrColors(
    background = SolidColor(Color(0xFF14110C)), backgroundFlat = Color(0xFF14110C),
    statusBarLightIcons = true,
    text = Color(0xFFF2EDE0), dim = Color(0xFF8F876E), accent = accent, accentInk = Color(0xFFF2EDE0),
    hairline = Color(0xFFF2EDE0).copy(0.30f), surface = Color(0xFF1E1A12), surfaceBorder = Color(0xFFF2EDE0).copy(0.30f),
    // v1.4 renders the display flat on the dark surface, so the expression must be light
    // (= text). The dark #16140F is the v1.5 on-cream-ink-block value; restore it when
    // displayBlock renders (v1.5). displayResult contrast on flat dark is tuned at C5/preview.
    displayExpression = Color(0xFFF2EDE0), displayResult = Color(0xFF2C3FE3),
    displayError = Color(0xFFFF8080),
    displayBlock = Color(0xFFF2EDE0),
    keyNum = SifrKeyStyle(SolidColor(Color(0xFF211D14)), Color(0xFFF2EDE0)),
    keyOp = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyEq = SifrKeyStyle(SolidColor(accent), Color(0xFFF2EDE0)),
    keyFn = SifrKeyStyle(SolidColor(Color(0xFFF2EDE0)), Color(0xFF16140F)),
    keyRadius = 0.dp, keyGap = 2.dp,
    mosaic = true, mosaicLine = Color(0xFFF2EDE0).copy(0.35f),
    displayFamily = Archivo, keyFamily = Archivo, uiFamily = Archivo,
)
