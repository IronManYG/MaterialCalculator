package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
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
 */
@Composable
fun ToolNumPad(
    onNumKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val rowHeight = if (compact) 40.dp else 56.dp
    val gap = if (compact) 6.dp else 8.dp
    // Width comes from the caller's [modifier] (fillMaxWidth in portrait, a fixed width in
    // landscape) so the landscape side-by-side split can pin the pad to a fixed width — an
    // internal fillMaxWidth() would override that. The rows fill whatever width the caller sets.
    Column(
        modifier = modifier
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        PAD_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .height(rowHeight),
                        fontSize = 20.sp,
                        onClick = { if (isBackspace) onBackspace() else onNumKey(key) },
                    )
                }
            }
        }
    }
}
