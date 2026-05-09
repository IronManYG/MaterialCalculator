package dev.gaddal.sifr.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import dev.gaddal.sifr.feature.calculator.ui.CalculatorRoot

@Composable
fun NavRoot() {
    val backStack = remember { NavBackStack<NavKey>(CalculatorRoute) }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<CalculatorRoute> { CalculatorRoot() }
        },
    )
}
