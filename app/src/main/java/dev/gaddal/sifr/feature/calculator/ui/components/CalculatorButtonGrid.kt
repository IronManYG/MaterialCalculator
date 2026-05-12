package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.buildCalculatorActions

private const val GRID_COLUMNS = 4

@Composable
fun CalculatorButtonGrid(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
) {
    val actions = remember(mode, angleUnit) { buildCalculatorActions(mode, angleUnit) }
    val rows = remember(actions) { actions.chunked(GRID_COLUMNS) }
    // Weighted rows + weighted cells: each row shares vertical space evenly,
    // each cell shares horizontal space evenly. Result: cells shrink in
    // scientific mode (more rows) so the display stays visible, instead of
    // the keypad pushing the display off-screen.
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { rowActions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                // Pad short trailing rows so cell widths stay consistent
                // across rows (scientific has 15 items in a 4-col grid).
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
