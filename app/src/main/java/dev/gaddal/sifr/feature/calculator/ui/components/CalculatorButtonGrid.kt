package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.buildCalculatorActions

@Composable
fun CalculatorButtonGrid(
    mode: CalculatorMode,
    angleUnit: AngleUnit,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildCalculatorActions(mode, angleUnit)
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(actions) { action ->
            CalculatorButton(
                action = action,
                modifier = Modifier.aspectRatio(1f),
                onClick = { onAction(action.action) },
            )
        }
    }
}
