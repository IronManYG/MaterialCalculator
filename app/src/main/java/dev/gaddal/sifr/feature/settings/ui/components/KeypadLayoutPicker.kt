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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/** Layouts offered in the picker (Tape is reserved for v1.7). */
private val PICKABLE_LAYOUTS = listOf(KeypadLayout.Classic, KeypadLayout.Remix, KeypadLayout.Arc)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeypadLayoutPicker(
    selected: KeypadLayout,
    onSelect: (KeypadLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SifrTokens.colors
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PICKABLE_LAYOUTS.forEach { layout ->
            val isSelected = layout == selected
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) tokens.accent else tokens.hairline,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(layout) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LayoutMiniDiagram(
                    layout = layout,
                    keyColor = tokens.keyNum.content,
                    accent = tokens.accent,
                    modifier = Modifier.size(width = 44.dp, height = 56.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(stringResource(layout.labelRes()), color = tokens.dim, fontSize = 11.sp)
            }
        }
    }
}

private fun KeypadLayout.labelRes(): Int = when (this) {
    KeypadLayout.Classic -> R.string.settings_keypad_classic
    KeypadLayout.Remix -> R.string.settings_keypad_remix
    KeypadLayout.Arc -> R.string.settings_keypad_arc
    KeypadLayout.Tape -> R.string.settings_keypad_classic // unreachable in picker
}

/** Tiny schematic of each layout's signature shape. Decorative, not pixel-exact. */
@Composable
private fun LayoutMiniDiagram(
    layout: KeypadLayout,
    keyColor: Color,
    accent: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val gap = size.width * 0.08f
        fun cell(x: Float, y: Float, w: Float, h: Float, color: Color) =
            drawRoundRect(color = color, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(h * 0.3f))
        when (layout) {
            KeypadLayout.Classic, KeypadLayout.Tape -> {
                val cols = 4; val rows = 5
                val cw = (size.width - gap * (cols - 1)) / cols
                val ch = (size.height - gap * (rows - 1)) / rows
                for (r in 0 until rows) for (c in 0 until cols) {
                    cell(c * (cw + gap), r * (ch + gap), cw, ch, keyColor)
                }
            }
            KeypadLayout.Remix -> {
                val cols = 4; val rows = 5
                val cw = (size.width - gap * (cols - 1)) / cols
                val ch = (size.height - gap * (rows - 1)) / rows
                for (r in 0 until rows - 1) for (c in 0 until cols) {
                    cell(c * (cw + gap), r * (ch + gap), cw, ch, keyColor)
                }
                val lastY = (rows - 1) * (ch + gap)
                cell(0f, lastY, cw * 2 + gap, ch, accent)          // wide 0
                cell(2 * (cw + gap), lastY, cw, ch, keyColor)       // .
                cell(3 * (cw + gap), lastY, cw, ch, keyColor)       // =
            }
            KeypadLayout.Arc -> {
                val cols = 3; val rows = 4
                val padW = size.width * 0.6f
                val cw = (padW - gap * (cols - 1)) / cols
                val ch = (size.height - gap * (rows - 1)) / rows
                for (r in 0 until rows) for (c in 0 until cols) {
                    cell(c * (cw + gap), r * (ch + gap), cw, ch, keyColor)
                }
                // arc of operator dots bottom-right + a bigger = dot in the corner
                val rad = size.width * 0.07f
                drawCircle(accent, rad, Offset(size.width * 0.78f, size.height * 0.30f))
                drawCircle(accent, rad, Offset(size.width * 0.88f, size.height * 0.46f))
                drawCircle(accent, rad, Offset(size.width * 0.93f, size.height * 0.64f))
                drawCircle(accent, size.width * 0.13f, Offset(size.width * 0.84f, size.height * 0.86f))
                drawArc(
                    color = accent,
                    startAngle = 180f, sweepAngle = 90f, useCenter = false,
                    topLeft = Offset(size.width * 0.45f, size.height * 0.1f),
                    size = Size(size.width * 0.9f, size.height * 0.9f),
                    style = Stroke(width = size.width * 0.015f),
                )
            }
        }
    }
}
