package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction
import dev.gaddal.sifr.feature.calculator.ui.basicRows
import dev.gaddal.sifr.feature.calculator.ui.memoryRow
import dev.gaddal.sifr.feature.calculator.ui.scientificCells

private const val GRID_COLUMNS = 4

@Composable
fun CalculatorButtonGrid(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
) {
    val rows = remember(mode, angleUnit) { buildKeypadRows(mode, angleUnit, GRID_COLUMNS) }
    val sifr = SifrTokens.colors
    val gridLineColor = when {
        sifr.mosaic -> sifr.mosaicLine
        sifr.hairlineGrid -> sifr.gridLine
        else -> null
    }
    // Calculator keypad is conventionally LTR even in Arabic locale (iOS,
    // Google Calc all do this and the SPEC says so). Force LTR so Compose
    // doesn't mirror each Row's children — otherwise "7 8 9 x" becomes
    // "x 9 8 7" and the digit column ends up on the wrong side.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier.then(
                if (gridLineColor != null) Modifier.background(gridLineColor).padding(sifr.keyGap) else Modifier,
            ),
            verticalArrangement = Arrangement.spacedBy(sifr.keyGap),
        ) {
            rows.forEach { rowActions ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(sifr.keyGap),
                ) {
                    rowActions.forEach { action ->
                        CalculatorButton(
                            action = action,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            fontSize = fontSize,
                            onClick = { onAction(action.action) },
                        )
                    }
                    // Pad short rows so cell widths stay consistent across
                    // rows (e.g. scientific has a row of 3 + 1 empty slot).
                    repeat(GRID_COLUMNS - rowActions.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Build the keypad as a list of rows, chunking each section independently
 * so memory keys never bleed into a scientific or basic row.
 *
 * Order: scientific (when active) → memory → basic. Each chunked by the
 * grid's column count. The memory row is intentionally already a single
 * 4-cell list — no chunking needed.
 */
private fun buildKeypadRows(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    columns: Int,
): List<List<CalculatorUiAction>> = buildList {
    if (mode == CalculatorMode.Scientific) {
        val sci = scientificCells.filterNot { cell ->
            val text = cell.text
            (text == "deg" && angleUnit == AngleUnit.Radians) ||
                (text == "rad" && angleUnit == AngleUnit.Degrees)
        }
        addAll(sci.chunked(columns))
    }
    add(memoryRow)
    addAll(basicRows.chunked(columns))
}
