package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.KeypadCell
import dev.gaddal.sifr.feature.calculator.ui.KeypadRowSpec
import dev.gaddal.sifr.feature.calculator.ui.arcEquals
import dev.gaddal.sifr.feature.calculator.ui.arcNumberPadRowSpecs
import dev.gaddal.sifr.feature.calculator.ui.arcOperators
import dev.gaddal.sifr.feature.calculator.ui.memoryRow
import dev.gaddal.sifr.feature.calculator.ui.scientificCells

/**
 * One-hand "Thumb arc" keypad (spec §5). Scientific grid (when active) and the
 * memory row (when visible) stack as normal grids above the Arc basic block;
 * the basic block is a 3-col number pad with four operator circles stacked up a
 * quarter-arc and an oversized `=` circle anchored bottom-end. Caller has
 * already forced LTR.
 */
@Composable
fun ArcKeypad(
    mode: CalculatorMode,
    memoryKeysVisible: Boolean,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    // Capped in scientific (see CalculatorButtonGrid.scientificGap) so the taller
    // Arc keypad doesn't crush the display above it.
    gap: Dp = SifrTokens.colors.keyGap,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (mode == CalculatorMode.Scientific) {
            val sci = remember {
                scientificCells
                    .chunked(4)
                    // 0.6 fontScale matches the portrait keypad: multi-glyph sci labels
                    // read smaller than the 27sp number keys (≈16sp), constant across modes.
                    .map { row -> KeypadRowSpec(row.map { KeypadCell(it) }, fontScale = 0.6f) }
            }
            WeightedCellGrid(
                rows = sci,
                columns = 4,
                onAction = onAction,
                fontSize = fontSize,
                gap = gap,
                modifier = Modifier.fillMaxWidth().weight(sci.size.toFloat()),
            )
        }
        if (memoryKeysVisible) {
            WeightedCellGrid(
                // 0.6 fontScale → ≈16sp keys, matching the portrait memory row (N2).
                rows = remember { listOf(KeypadRowSpec(memoryRow.map { KeypadCell(it) }, fontScale = 0.6f)) },
                columns = 4,
                onAction = onAction,
                fontSize = fontSize,
                gap = gap,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        // 5 number-pad rows → weight 5 keeps key height ~uniform with the
        // grids above (each of which weights itself by its own row count).
        ArcBasicBlock(
            onAction = onAction,
            fontSize = fontSize,
            gap = gap,
            modifier = Modifier.fillMaxWidth().weight(5f),
        )
    }
}

@Composable
private fun ArcBasicBlock(
    onAction: (CalculatorAction) -> Unit,
    fontSize: TextUnit,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    BoxWithConstraints(modifier = modifier) {
        val h = maxHeight
        val keyH = (h - gap * 4) / 5
        val opSize = keyH * 0.92f
        val eqSize = keyH * 1.38f
        // Stack operators ABOVE the equals circle so they never collide at any
        // key size (port of layouts.jsx KeypadArc arcStart/arcStep).
        val arcStart = eqSize + 18.dp
        val arcStep = (h - arcStart - opSize - 6.dp) / 3
        val opFont = (opSize.value * 0.4f).sp
        val eqFont = (eqSize.value * 0.42f).sp

        // Faint 1px arc guide, drawn first so it sits behind the keys. No
        // clickable → it doesn't intercept touches.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = h * 0.52f, y = h * 0.52f)
                .size(h * 1.35f)
                .border(1.dp, sifr.hairline, CircleShape),
        )

        // 3-col number pad on the leading 63%.
        WeightedCellGrid(
            rows = arcNumberPadRowSpecs,
            columns = 3,
            onAction = onAction,
            fontSize = fontSize,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.63f)
                .fillMaxHeight(),
        )

        // Operator circles up the arc (÷ × − +), curving inward via x-insets.
        val opOffsets = listOf(34.dp, 12.dp, 2.dp, 0.dp)
        val opBottoms = listOf(arcStart + arcStep * 3, arcStart + arcStep * 2, arcStart + arcStep, arcStart)
        arcOperators.forEachIndexed { i, op ->
            CalculatorButton(
                action = op,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -opOffsets[i], y = -opBottoms[i])
                    .size(opSize),
                fontSize = opFont,
                shape = CircleShape,
                onClick = { onAction(op.action) },
            )
        }

        // Oversized equals circle in the bottom-end corner.
        CalculatorButton(
            action = arcEquals,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -8.dp, y = -4.dp)
                .size(eqSize),
            fontSize = eqFont,
            shape = CircleShape,
            onClick = { onAction(arcEquals.action) },
        )
    }
}

@Preview(name = "Arc — Layl dark (eq glow)", showBackground = true, widthDp = 360, heightDp = 520)
@Composable
private fun PreviewArcLayl() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    ArcKeypad(
        mode = CalculatorMode.Basic,
        memoryKeysVisible = true,
        onAction = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(name = "Arc — small (compact height)", showBackground = true, widthDp = 320, heightDp = 360)
@Composable
private fun PreviewArcSmall() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
    ArcKeypad(
        mode = CalculatorMode.Basic,
        memoryKeysVisible = false,
        onAction = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(name = "Arc — tall (large keys)", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
private fun PreviewArcTall() = SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
    ArcKeypad(
        mode = CalculatorMode.Basic,
        memoryKeysVisible = true,
        onAction = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(name = "Arc — scientific", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PreviewArcScientific() = SifrTheme(palette = SifrPalette.Raqim, themeMode = ThemeMode.Light) {
    ArcKeypad(
        mode = CalculatorMode.Scientific,
        memoryKeysVisible = true,
        onAction = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
