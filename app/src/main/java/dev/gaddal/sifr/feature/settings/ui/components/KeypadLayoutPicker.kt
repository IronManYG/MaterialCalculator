package dev.gaddal.sifr.feature.settings.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/** All four prototype layouts (Classic / Remix / Arc / Tape). */
private val PICKABLE_LAYOUTS = listOf(
    KeypadLayout.Classic,
    KeypadLayout.Remix,
    KeypadLayout.Arc,
    KeypadLayout.Tape,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeypadLayoutPicker(
    selected: KeypadLayout,
    onSelect: (KeypadLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SifrTokens.colors
    // All 4 layouts on one row, equal width (prototype screens.jsx:133 — flex:1).
    // weight(1f) + maxItemsInEachRow=4 keeps them aligned even when the Arabic labels
    // ("شبكة معدّلة" / "قوس الإبهام") are wider than the English ones.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4,
    ) {
        PICKABLE_LAYOUTS.forEach { layout ->
            val isSelected = layout == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) tokens.accent else tokens.hairline,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(layout) }
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LayoutMiniDiagram(
                    layout = layout,
                    cellColor = tokens.dim,
                    accent = tokens.accent,
                    modifier = Modifier.size(width = 44.dp, height = 46.dp),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(layout.labelRes()),
                    // Selected label adopts the accent (prototype LayoutDiagram:
                    // `color: selected ? t.accent : t.dim`); others stay dim.
                    color = if (isSelected) tokens.accent else tokens.dim,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

private fun KeypadLayout.labelRes(): Int = when (this) {
    KeypadLayout.Classic -> R.string.settings_keypad_classic
    KeypadLayout.Remix -> R.string.settings_keypad_remix
    KeypadLayout.Arc -> R.string.settings_keypad_arc
    KeypadLayout.Tape -> R.string.settings_keypad_tape
}

/**
 * Tiny schematic of each layout's signature shape — a faithful port of the
 * prototype `LayoutDiagram` (screens.jsx:54-92): a thin "display" strip on top,
 * then a *simplified* keypad grid (4×3, not the real 4×5). Decorative, not
 * pixel-exact.
 *  - Classic / Tape → 4×3 cell grid (Tape adds a dashed receipt line in the strip)
 *  - Remix → two full rows + a wide pill "0", a dot, and an accent "="
 *  - Arc → 3×3 number grid on the leading 58% + accent "=" disc and two operator outlines
 */
@Composable
private fun LayoutMiniDiagram(
    layout: KeypadLayout,
    cellColor: Color,
    accent: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = w * 0.06f
        // Cells are the prototype's dim dots (t.dim @ 0.55); accent cells stay full.
        val dot = cellColor.copy(alpha = 0.55f)
        fun cell(x: Float, y: Float, cw: Float, ch: Float, color: Color, radius: Float = ch * 0.3f) =
            drawRoundRect(color = color, topLeft = Offset(x, y), size = Size(cw, ch), cornerRadius = CornerRadius(radius, radius))

        // The display "strip" above the keypad (prototype reserves ~11px before the grid).
        val stripH = h * 0.18f
        val gridTop = stripH + gap
        val gridH = h - gridTop

        when (layout) {
            KeypadLayout.Classic, KeypadLayout.Tape -> {
                val cols = 4; val rows = 3
                val cw = (w - gap * (cols - 1)) / cols
                val ch = (gridH - gap * (rows - 1)) / rows
                for (r in 0 until rows) for (c in 0 until cols) {
                    cell(c * (cw + gap), gridTop + r * (ch + gap), cw, ch, dot)
                }
                if (layout == KeypadLayout.Tape) {
                    // Dashed "tape" receipt line in the display strip.
                    drawLine(
                        color = dot,
                        start = Offset(0f, stripH * 0.55f),
                        end = Offset(w, stripH * 0.55f),
                        strokeWidth = h * 0.022f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(w * 0.12f, w * 0.08f), 0f,
                        ),
                    )
                }
            }
            KeypadLayout.Remix -> {
                val cols = 4; val rows = 3
                val cw = (w - gap * (cols - 1)) / cols
                val ch = (gridH - gap * (rows - 1)) / rows
                // Two full rows of dots.
                for (r in 0 until 2) for (c in 0 until cols) {
                    cell(c * (cw + gap), gridTop + r * (ch + gap), cw, ch, dot)
                }
                // Last row: wide pill "0" (spans 2), a dot, then the accent "=".
                val lastY = gridTop + 2 * (ch + gap)
                cell(0f, lastY, cw * 2 + gap, ch, dot, radius = ch * 0.5f)
                cell(2 * (cw + gap), lastY, cw, ch, dot)
                cell(3 * (cw + gap), lastY, cw, ch, accent)
            }
            KeypadLayout.Arc -> {
                // 3×3 number grid on the leading 58%.
                val cols = 3; val rows = 3
                val padW = w * 0.58f
                val cw = (padW - gap * (cols - 1)) / cols
                val ch = (gridH - gap * (rows - 1)) / rows
                for (r in 0 until rows) for (c in 0 until cols) {
                    cell(c * (cw + gap), gridTop + r * (ch + gap), cw, ch, dot)
                }
                // Accent "=" disc in the bottom-end corner + two operator outlines up the arc.
                val eqR = w * 0.14f
                drawCircle(accent, eqR, Offset(w - eqR - gap, h - eqR - gap))
                val opR = w * 0.075f
                drawCircle(
                    color = dot, radius = opR,
                    center = Offset(w - eqR - gap, h - eqR * 2 - opR * 2),
                    style = Stroke(width = w * 0.02f),
                )
                drawCircle(
                    color = dot, radius = opR,
                    center = Offset(w - eqR - gap - opR * 1.4f, h - eqR * 2 - opR * 4.2f),
                    style = Stroke(width = w * 0.02f),
                )
            }
        }
    }
}
