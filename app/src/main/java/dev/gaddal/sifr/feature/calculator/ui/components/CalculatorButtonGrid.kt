package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.KeypadCell
import dev.gaddal.sifr.feature.calculator.ui.KeypadRowSpec
import dev.gaddal.sifr.feature.calculator.ui.classicBasicRows
import dev.gaddal.sifr.feature.calculator.ui.memoryRow
import dev.gaddal.sifr.feature.calculator.ui.remixBasicRows
import dev.gaddal.sifr.feature.calculator.ui.scientificCells

private const val GRID_COLUMNS = 4

@Composable
fun CalculatorButtonGrid(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    layout: KeypadLayout,
    memoryKeysVisible: Boolean,
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
    // Keypad is conventionally LTR even in Arabic locale (math reads L→R).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        when (layout) {
            KeypadLayout.Arc -> ArcKeypad(
                mode = mode,
                angleUnit = angleUnit,
                memoryKeysVisible = memoryKeysVisible,
                onAction = onAction,
                modifier = modifier,
                fontSize = fontSize,
            )
            else -> {
                val rows = buildKeypadRows(mode, angleUnit, layout, memoryKeysVisible, GRID_COLUMNS)
                WeightedCellGrid(
                    rows = rows,
                    columns = GRID_COLUMNS,
                    onAction = onAction,
                    fontSize = fontSize,
                    baseRowHeight = 58.dp,
                    modifier = modifier.then(
                        if (gridLineColor != null) Modifier.background(gridLineColor).padding(sifr.keyGap) else Modifier,
                    ),
                )
            }
        }
    }
}

/**
 * Span-aware grid with two height modes. When [baseRowHeight] is non-null each
 * row is exactly `baseRowHeight * heightScale` tall (spec §4.6 — basic rows
 * 1.0×, scientific rows 0.62×, memory row 0.58×) so the keypad is content-sized
 * and the display above it flexes (portrait Classic/Remix). When [baseRowHeight]
 * is null each row fills its share of the column height via `weight(1f)` — the
 * Arc keypad uses this, sizing its own column with parent weights. Each cell
 * takes `weight(span)` so a wide cell (Remix's `0`) spans multiple columns;
 * short rows pad with weighted spacers so column widths stay aligned across rows.
 */
@Composable
fun WeightedCellGrid(
    rows: List<KeypadRowSpec>,
    columns: Int,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    baseRowHeight: Dp? = null,
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
                    .then(
                        if (baseRowHeight != null) Modifier.height(baseRowHeight * row.heightScale)
                        else Modifier.weight(1f),
                    ),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.cells.forEach { cell ->
                    CalculatorButton(
                        action = cell.action,
                        modifier = Modifier
                            .weight(cell.span.toFloat())
                            .fillMaxHeight(),
                        fontSize = fontSize,
                        onClick = { onAction(cell.action.action) },
                    )
                }
                val filled = row.cells.sumOf { it.span }
                repeat(columns - filled) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/**
 * Build the keypad as span-aware rows: scientific (when active) → memory
 * (when visible) → basic rows selected by layout. Each section chunked
 * independently so memory keys never bleed into another section.
 * Returns [KeypadRowSpec] with per-section height scales (spec §4.6):
 * scientific rows 0.62×, memory row 0.58×, basic rows 1.0×.
 */
private fun buildKeypadRows(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    layout: KeypadLayout,
    memoryKeysVisible: Boolean,
    columns: Int,
): List<KeypadRowSpec> = buildList {
    if (mode == CalculatorMode.Scientific) {
        val sci = scientificCells.filterNot { cell ->
            val text = cell.text
            (text == "deg" && angleUnit == AngleUnit.Radians) ||
                (text == "rad" && angleUnit == AngleUnit.Degrees)
        }
        addAll(
            sci.chunked(columns).map { rowActions ->
                KeypadRowSpec(rowActions.map { KeypadCell(it) }, heightScale = 0.62f)
            },
        )
    }
    if (memoryKeysVisible) add(KeypadRowSpec(memoryRow.map { KeypadCell(it) }, heightScale = 0.58f))
    addAll(
        when (layout) {
            KeypadLayout.Remix -> remixBasicRows
            else -> classicBasicRows // Classic + Tape fallback (Arc is dispatched separately)
        }.map { row -> KeypadRowSpec(row, heightScale = 1f) },
    )
}
