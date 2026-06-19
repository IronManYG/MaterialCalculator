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
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.KeypadCell
import dev.gaddal.sifr.feature.calculator.ui.KeypadRowSpec
import dev.gaddal.sifr.feature.calculator.ui.classicBasicRows
import dev.gaddal.sifr.feature.calculator.ui.memoryRow
import dev.gaddal.sifr.feature.calculator.ui.remixBasicRows
import dev.gaddal.sifr.feature.calculator.ui.scientificCells

private const val GRID_COLUMNS = 4

/**
 * In scientific mode the keypad gains 5 extra rows (4 sci + memory); on the airy
 * palettes (Farah/Mizan keyGap 11–12dp) the inter-row gaps alone push the 10-row
 * keypad past its budget, squeezing the display below its 240dp floor and clipping
 * the two-line result's COPY row. Cap the gap at 8dp in scientific so the keypad
 * stays in budget and the display keeps its floor; basic mode keeps the palette's
 * full gap, and the already-dense palettes (Bayan 2dp / Raqim 1dp) are unchanged.
 */
private fun scientificGap(keyGap: Dp, mode: CalculatorMode): Dp =
    if (mode == CalculatorMode.Scientific) minOf(keyGap, 8.dp) else keyGap

@Composable
fun CalculatorButtonGrid(
    mode: CalculatorMode,
    layout: KeypadLayout,
    memoryKeysVisible: Boolean,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    maxHeight: Dp? = null,
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
            KeypadLayout.Arc -> {
                // Arc is content-sized like Classic so the display flexes above it
                // (prototype H = 5·keyH + 4·gap). Without a height bound the weight-based
                // ArcKeypad fills the whole column and crushes the display to its floor
                // ("takes over the whole screen"). Memory row IS shown in Arc (user
                // decision 2026-06-13) — this deliberately diverges from the prototype
                // (main.jsx:169 hid it) to keep MC/M+/M−/MR reachable one-handed.
                val gap = scientificGap(sifr.keyGap, mode)
                val sciRowCount = if (mode == CalculatorMode.Scientific) {
                    (scientificCells.size + GRID_COLUMNS - 1) / GRID_COLUMNS
                } else 0
                // 5 basic rows + sci rows + memory row (when visible); each section
                // weights 1:1 per row inside ArcKeypad, so unitRows is the total.
                val memoryRows = if (memoryKeysVisible) 1 else 0
                val unitRows = 5 + sciRowCount + memoryRows
                val arcHeightModifier = if (maxHeight != null) {
                    val base = ((maxHeight - gap * (unitRows - 1)) / unitRows)
                        .coerceIn(46.dp, 58.dp)
                    // Arc weights every row equally (it does NOT scale sci/memory rows
                    // like Classic), so in scientific the 10-row stack at the 46dp floor
                    // overflows the keypad budget and squeezes the display below its
                    // floor — clipping the two-line result + COPY row. Cap at maxHeight
                    // (the keypad budget) so the display always keeps its reservation;
                    // the Arc keys then distribute that height and shrink a touch.
                    Modifier.height((base * unitRows + gap * (unitRows - 1)).coerceAtMost(maxHeight))
                } else Modifier
                ArcKeypad(
                    mode = mode,
                    memoryKeysVisible = memoryKeysVisible,
                    onAction = onAction,
                    modifier = modifier.then(arcHeightModifier),
                    fontSize = fontSize,
                    gap = gap,
                )
            }
            else -> {
                val rows = buildKeypadRows(mode, layout, memoryKeysVisible, GRID_COLUMNS)
                // keyGap is palette-variable (≈1dp hairline themes → 12dp pill
                // themes); fold it into the budget so the clamp stays accurate as
                // the inter-row gaps grow/shrink with the active palette. In
                // scientific the gap is capped so the 10-row keypad doesn't push the
                // display below its floor (which clipped the COPY row on the airy palettes).
                val gap = scientificGap(sifr.keyGap, mode)
                val base = if (maxHeight != null) {
                    val totalScale = rows.sumOf { it.heightScale.toDouble() }.toFloat()
                    val gaps = gap * (rows.size - 1)
                    ((maxHeight - gaps) / totalScale).coerceIn(46.dp, 58.dp)
                } else 58.dp
                WeightedCellGrid(
                    rows = rows,
                    columns = GRID_COLUMNS,
                    onAction = onAction,
                    fontSize = fontSize,
                    baseRowHeight = base,
                    gap = gap,
                    modifier = modifier.then(
                        if (gridLineColor != null) Modifier.background(gridLineColor).padding(gap) else Modifier,
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
    // Scientific mode caps the gap below the palette's full keyGap so the taller
    // keypad doesn't steal the display's vertical floor — pass that capped value.
    gap: Dp = SifrTokens.colors.keyGap,
) {
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
                        // Per-row font: memory/scientific rows shrink their multi-glyph
                        // labels relative to the big single-digit number keys (spec §4.6).
                        fontSize = fontSize * row.fontScale,
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
    layout: KeypadLayout,
    memoryKeysVisible: Boolean,
    columns: Int,
): List<KeypadRowSpec> = buildList {
    if (mode == CalculatorMode.Scientific) {
        addAll(
            // Scientific rows share the memory row's font (multi-glyph labels read
            // best smaller than the number keys — prototype fs 15 vs num 23). 0.6×27sp
            // ≈ 16sp, constant across modes so the keys don't resize when you toggle ƒ.
            scientificCells.chunked(columns).map { rowActions ->
                KeypadRowSpec(rowActions.map { KeypadCell(it) }, heightScale = 0.62f, fontScale = 0.6f)
            },
        )
    }
    if (memoryKeysVisible) {
        add(
            KeypadRowSpec(
                memoryRow.map { KeypadCell(it) },
                heightScale = 0.58f,
                // The 27sp number font is too big for "MC/M+/M−/MR"; 0.6×27 ≈ 16sp
                // (prototype memory fs=15 vs num 23). Constant in both modes so it
                // matches the scientific row and never resizes when ƒ is toggled.
                fontScale = 0.6f,
            ),
        )
    }
    addAll(
        when (layout) {
            KeypadLayout.Remix -> remixBasicRows
            else -> classicBasicRows // Classic + Tape (Arc is dispatched separately)
        }.map { row ->
            // Tape = compact Classic (prototype KeypadClassic `compact` → 0.87× key height).
            KeypadRowSpec(row, heightScale = if (layout == KeypadLayout.Tape) 0.87f else 1f)
        },
    )
}
