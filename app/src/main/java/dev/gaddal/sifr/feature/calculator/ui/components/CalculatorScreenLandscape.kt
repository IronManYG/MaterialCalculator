package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.CalculatorState
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction
import dev.gaddal.sifr.feature.calculator.ui.basicRows
import dev.gaddal.sifr.feature.calculator.ui.memoryRow
import dev.gaddal.sifr.feature.calculator.ui.scientificCells

/**
 * Restructured landscape layout. Display strip spans the top; the body is
 * split into a scientific block (leading edge) and a basic block (trailing
 * edge). Under RTL (`LayoutDirection.Rtl`) Compose's Row flips the children
 * order automatically, so the basic block ends up on the leading (right)
 * edge — matching the SPEC's RTL mirroring requirement.
 *
 * The scientific block is always visible in landscape regardless of
 * `state.mode` — rotation is opportunistic, not a forced mode change. The
 * toolbar toggle still applies; flipping back to portrait honors the
 * persisted mode.
 */
@Composable
fun CalculatorScreenLandscape(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Top: display strip, full-width
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 25.dp, bottomEnd = 25.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                CalculatorDisplay(
                    expression = state.expression,
                    cursor = state.cursor,
                    selectionStart = state.selectionStart,
                    livePreview = state.livePreview,
                    error = state.error,
                    onSelectionChange = { start, end ->
                        onAction(CalculatorAction.SelectionChanged(start, end))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    if (state.memoryValue != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.calc_mode_chip_memory),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    IconButton(
                        onClick = dropUnlessResumed { onAction(CalculatorAction.HistoryClicked) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.calc_open_history),
                        )
                    }
                    IconButton(
                        onClick = dropUnlessResumed { onAction(CalculatorAction.ToggleMode) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = stringResource(R.string.calc_toggle_mode),
                            tint = if (state.mode == CalculatorMode.Scientific)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = dropUnlessResumed { onAction(CalculatorAction.SettingsClicked) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.calc_open_settings),
                        )
                    }
                }
            }
            // Body: two-column split
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScientificBlockLandscape(
                    angleUnit = state.angleUnit,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                BasicBlockLandscape(
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

private const val SCIENTIFIC_COLUMNS = 3
private const val BASIC_COLUMNS = 4

@Composable
private fun ScientificBlockLandscape(
    angleUnit: AngleUnit,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = remember(angleUnit) {
        scientificCells.filterNot { cell ->
            val text = cell.text
            (text == "deg" && angleUnit == AngleUnit.Radians) ||
                (text == "rad" && angleUnit == AngleUnit.Degrees)
        }
    }
    WeightedButtonGrid(
        actions = actions,
        columns = SCIENTIFIC_COLUMNS,
        onAction = onAction,
        modifier = modifier,
    )
}

@Composable
private fun BasicBlockLandscape(
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Memory row at TOP of the basic block; then the standard basic grid below.
    val actions = remember { memoryRow + basicRows }
    WeightedButtonGrid(
        actions = actions,
        columns = BASIC_COLUMNS,
        onAction = onAction,
        modifier = modifier,
    )
}

// Weighted rows + weighted cells so each panel's cells share the panel
// height evenly. Replaces LazyVerticalGrid + aspectRatio(1f), which sized
// itself by content and overflowed the panel when the row count was tall.
@Composable
private fun WeightedButtonGrid(
    actions: List<CalculatorUiAction>,
    columns: Int,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(actions, columns) { actions.chunked(columns) }
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
                        onClick = { onAction(action.action) },
                    )
                }
                repeat(columns - rowActions.size) {
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

@Preview(
    name = "Landscape — Scientific + Memory",
    showBackground = true,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewLandscapeScientific() = SifrTheme {
    CalculatorScreenLandscape(
        state = CalculatorState(
            expression = "12xsin(45)",
            cursor = 10,
            mode = CalculatorMode.Scientific,
            angleUnit = AngleUnit.Degrees,
            livePreview = "8.485",
            memoryValue = 42.0,
        ),
        onAction = {},
    )
}

@Preview(
    name = "Landscape — Basic only",
    showBackground = true,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewLandscapeBasic() = SifrTheme {
    CalculatorScreenLandscape(
        state = CalculatorState(
            expression = "100+25",
            cursor = 6,
            mode = CalculatorMode.Basic,
            angleUnit = AngleUnit.Degrees,
            livePreview = "125",
        ),
        onAction = {},
    )
}
