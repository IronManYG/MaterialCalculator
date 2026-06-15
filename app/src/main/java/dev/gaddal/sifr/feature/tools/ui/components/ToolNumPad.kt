package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorButton

private val PAD_ROWS = listOf(
    listOf('7', '8', '9'),
    listOf('4', '5', '6'),
    listOf('1', '2', '3'),
    listOf('0', '.', '⌫'),
)

/**
 * 3-column numpad for Tools. Reuses [CalculatorButton] so key styling, press animation, and
 * haptics match the calculator. [compact] is true in landscape (shorter rows + tighter gaps).
 * [fillHeight] makes the rows share the column's height via weights instead of a fixed row
 * height — the landscape split passes it so the pad fills its column instead of leaving a gap
 * below it. The grid-line palettes (Raqim hairline / Bayan mosaic) carry their key separation as
 * thin lines between same-as-background keys rather than per-key borders; the calculator paints
 * that grid behind the whole keypad, so this pad replicates it or the keys vanish on those
 * palettes.
 */
@Composable
fun ToolNumPad(
    onNumKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    fillHeight: Boolean = false,
) {
    val sifr = SifrTokens.colors
    val gridLineColor = when {
        sifr.mosaic -> sifr.mosaicLine
        sifr.hairlineGrid -> sifr.gridLine
        else -> null
    }
    val rowHeight = if (compact) 40.dp else 56.dp
    // Grid-line palettes use the palette's own (1–2dp) keyGap so the seams read as hairlines and
    // line up with the calculator keypad; the others keep the roomier Tools spacing.
    val gap = when {
        gridLineColor != null -> sifr.keyGap
        compact -> 6.dp
        else -> 8.dp
    }
    // Width comes from the caller's [modifier] (fillMaxWidth in portrait, a fixed width in
    // landscape) so the landscape side-by-side split can pin the pad to a fixed width — an
    // internal fillMaxWidth() would override that. The rows fill whatever width the caller sets.
    Column(
        modifier = modifier
            .padding(horizontal = 18.dp)
            // The grid colour sits BEHIND the keys; the spacedBy/padding gaps let it show through
            // as the seams (the key containers cover the cells). Mirrors CalculatorButtonGrid.
            .then(
                if (gridLineColor != null) {
                    Modifier
                        .background(gridLineColor)
                        .padding(gap)
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        PAD_ROWS.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fillHeight) Modifier.weight(1f) else Modifier.height(rowHeight)),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { key ->
                    val isBackspace = key == '⌫'
                    CalculatorButton(
                        action = CalculatorUiAction(
                            text = if (isBackspace) "⌫" else key.toString(),
                            role = if (isBackspace) SifrKeyRole.Fn else SifrKeyRole.Num,
                            action = CalculatorAction.Delete, // no-op carrier; onClick drives behavior
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        fontSize = 20.sp,
                        onClick = { if (isBackspace) onBackspace() else onNumKey(key) },
                    )
                }
            }
        }
    }
}
