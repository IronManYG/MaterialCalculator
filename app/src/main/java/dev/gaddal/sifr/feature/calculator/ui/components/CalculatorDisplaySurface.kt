package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Wraps the calculator display in the active theme's container treatment:
 *  - Bayan -> solid ink block + faint ٥ watermark (no corner radius)
 *  - Farah -> floating card (radius 26, drop shadow)
 *  - Mizan -> recessed inset (radius 14, drawn inner-shadow cue)
 *  - Layl / Raqim -> flat (no container)
 * The display colors (displayExpression / displayResult) are already tuned to
 * the on-container values in SifrPalettes for the themes that render a container.
 */
@Composable
fun CalculatorDisplaySurface(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    resultTight: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val sifr = SifrTokens.colors
    // Scientific mode (compact) trims only the VERTICAL insets — the horizontal
    // margins/padding stay put so the surface keeps its width and identity, but the
    // result state stops clipping the COPY row on the card/block/inset palettes.
    val outerV = if (compact) 4.dp else 14.dp
    // `resultTight` trims only the INNER content padding (never `outerV`) while a frozen
    // two-line result is shown on a surface palette: on a tall keypad (e.g. Farah's airy
    // pills, worst on Arc) the result + COPY row overhangs the surface and the bottom row
    // drops / its text gets squeezed out. Trimming the inner pad reclaims the room without
    // resizing the card, so it never jumps on `=`.
    val innerV = when { compact -> 6.dp; resultTight -> 6.dp; else -> 20.dp }
    val insetInnerV = when { compact -> 6.dp; resultTight -> 6.dp; else -> 18.dp }
    // The screen already insets the whole content area 18dp horizontally; the surface
    // adds only a hair more so the card/block/inset reads nearly as wide as the flat
    // display (prototype puts the card ~18dp from the screen edge — display.jsx:34-36).
    val surfaceH = 4.dp
    when {
        sifr.displayBlock != null -> Box(
            modifier = modifier
                .padding(horizontal = surfaceH, vertical = outerV)
                .background(sifr.displayBlock)
                .padding(horizontal = 20.dp, vertical = innerV),
        ) {
            Text(
                text = "٥",
                color = sifr.displayExpression.copy(alpha = 0.06f),
                fontFamily = sifr.displayFamily,
                fontSize = 150.sp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            content()
        }

        sifr.displayCard != null -> Box(
            modifier = modifier
                .padding(horizontal = surfaceH, vertical = outerV)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp), clip = false)
                // Paint the rounded card via `background(shape)` rather than `clip()` so a
                // few-dp overhang of the two-line result spills like the prototype's
                // `overflow: visible` instead of being hard-clipped. The real fit is
                // handled by `resultTight` (below) + the screen's reduced display padding;
                // this just keeps the corners without a clip as a spill net.
                .background(sifr.displayCard, RoundedCornerShape(26.dp))
                .padding(horizontal = 20.dp, vertical = innerV),
            content = content,
        )

        sifr.displayInset != null -> Box(
            modifier = modifier
                .padding(horizontal = surfaceH, vertical = outerV)
                // Same no-clip rule as the card (see above): `background(shape)` keeps the
                // rounded corners but lets a marginal result overhang spill instead of
                // being hard-clipped. Fit itself is handled by `resultTight` + the screen's
                // display padding; this is the prototype-faithful `overflow: visible` net.
                .background(sifr.displayInset, RoundedCornerShape(14.dp))
                .drawWithContent {
                    drawContent()
                    val w = size.width
                    val h = size.height
                    // Inset the inner-shadow cue lines by the corner radius so they stay
                    // inside the (now un-clipped) rounded shape instead of poking out at
                    // the corners.
                    val r = 14.dp.toPx()
                    drawLine(
                        color = Color.Black.copy(alpha = 0.25f),
                        start = Offset(r, 1.dp.toPx()),
                        end = Offset(w - r, 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(r, h - 1.dp.toPx()),
                        end = Offset(w - r, h - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(horizontal = 18.dp, vertical = insetInnerV),
            content = content,
        )

        else -> Box(modifier = modifier, content = content)
    }
}
