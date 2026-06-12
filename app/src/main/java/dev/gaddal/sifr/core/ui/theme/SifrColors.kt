package dev.gaddal.sifr.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import dev.gaddal.sifr.core.domain.settings.SifrPalette

/** Every keypad key maps to exactly one of these four roles for styling. */
enum class SifrKeyRole { Num, Op, Eq, Fn }

@Immutable
data class SifrKeyStyle(
    val container: Brush,
    val content: Color,
    val border: Color? = null,
    val dropShadow: Color? = null,        // soft3d (Farah) / hard (Mizan): solid offset rect
    val innerTopHighlight: Color? = null, // Mizan: 1px top inner highlight
    val italic: Boolean = false,          // Raqim operators
)

@Immutable
data class SifrColors(
    val background: Brush,
    val backgroundFlat: Color,
    val statusBarLightIcons: Boolean,
    val text: Color,
    val dim: Color,
    val accent: Color,
    val accentInk: Color,
    val hairline: Color,
    val surface: Color,
    val surfaceBorder: Color,
    // display
    val displayExpression: Color,
    val displayResult: Color,
    val displayError: Color,
    val resultItalic: Boolean = false,
    val displayBlock: Color? = null,  // v1.5 Bayan ink block
    val displayInset: Color? = null,  // v1.5 Mizan recessed
    val displayCard: Color? = null,   // v1.5 Farah card
    // keys by role
    val keyNum: SifrKeyStyle,
    val keyOp: SifrKeyStyle,
    val keyEq: SifrKeyStyle,
    val keyFn: SifrKeyStyle,
    // construction
    val keyRadius: Dp,
    val keyGap: Dp,
    val mosaic: Boolean = false,
    val mosaicLine: Color = Color.Transparent,
    val hairlineGrid: Boolean = false,
    val gridLine: Color = Color.Transparent,
    val ghostNumeral: Color? = null,  // Arabic-Indic glyph on number keys (Layl/Farah)
    val eqGlow: Color? = null,        // Layl: blurred accent glow behind '='
    val raisedKeys: Boolean = false,  // Farah/Mizan: press translates down instead of scaling
    // fonts
    val displayFamily: FontFamily,
    val keyFamily: FontFamily,
    val uiFamily: FontFamily,
) {
    fun keyStyle(role: SifrKeyRole): SifrKeyStyle = when (role) {
        SifrKeyRole.Num -> keyNum
        SifrKeyRole.Op -> keyOp
        SifrKeyRole.Eq -> keyEq
        SifrKeyRole.Fn -> keyFn
    }
}

val LocalSifrColors = staticCompositionLocalOf<SifrColors> {
    error("SifrColors not provided — wrap content in SifrTheme { }.")
}

/** The active palette enum (Layl/Bayan/Raqim/Farah/Mizan/Dynamic), for components
 *  that must branch on identity (e.g. SifrBrand) rather than resolved colors. */
val LocalSifrPalette = staticCompositionLocalOf { SifrPalette.Layl }

/** Ergonomic, free (`@ReadOnlyComposable`) token accessor: `SifrTokens.colors.accent`. */
object SifrTokens {
    val colors: SifrColors
        @Composable @ReadOnlyComposable get() = LocalSifrColors.current

    val palette: SifrPalette
        @Composable @ReadOnlyComposable get() = LocalSifrPalette.current
}
