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
import dev.gaddal.sifr.feature.calculator.ui.KeypadCell
import dev.gaddal.sifr.feature.calculator.ui.classicBasicRows
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
    val sifr = SifrTokens.colors
    val gridLineColor = when {
        sifr.mosaic -> sifr.mosaicLine
        sifr.hairlineGrid -> sifr.gridLine
        else -> null
    }
    val rows = remember(mode, angleUnit) { buildKeypadRows(mode, angleUnit, GRID_COLUMNS) }
    // Keypad is conventionally LTR even in Arabic locale (math reads L→R).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        WeightedCellGrid(
            rows = rows,
            columns = GRID_COLUMNS,
            onAction = onAction,
            fontSize = fontSize,
            modifier = modifier.then(
                if (gridLineColor != null) Modifier.background(gridLineColor).padding(sifr.keyGap) else Modifier,
            ),
        )
    }
}

/**
 * Span-aware weighted grid: each row fills its share of the column height
 * (`weight(1f)`), and each cell takes `weight(span)` so a wide cell (Remix's
 * `0`) spans multiple columns. Short rows pad with weighted spacers so column
 * widths stay aligned across rows.
 */
@Composable
fun WeightedCellGrid(
    rows: List<List<KeypadCell>>,
    columns: Int,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
) {
    val gap = SifrTokens.colors.keyGap
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { cell ->
                    CalculatorButton(
                        action = cell.action,
                        modifier = Modifier
                            .weight(cell.span.toFloat())
                            .fillMaxHeight(),
                        fontSize = fontSize,
                        onClick = { onAction(cell.action.action) },
                    )
                }
                val filled = row.sumOf { it.span }
                repeat(columns - filled) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/**
 * Build the keypad as span-aware rows: scientific (when active) → memory →
 * Classic basic. Each section chunked independently so memory keys never
 * bleed into another section.
 */
private fun buildKeypadRows(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    columns: Int,
): List<List<KeypadCell>> = buildList {
    if (mode == CalculatorMode.Scientific) {
        val sci = scientificCells.filterNot { cell ->
            val text = cell.text
            (text == "deg" && angleUnit == AngleUnit.Radians) ||
                (text == "rad" && angleUnit == AngleUnit.Degrees)
        }
        addAll(sci.chunked(columns).map { rowActions -> rowActions.map { KeypadCell(it) } })
    }
    add(memoryRow.map { KeypadCell(it) })
    addAll(classicBasicRows)
}
