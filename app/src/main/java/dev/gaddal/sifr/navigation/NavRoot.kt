package dev.gaddal.sifr.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.gaddal.sifr.feature.calculator.ui.CalculatorRoot
import dev.gaddal.sifr.feature.settings.ui.SettingsRoot

@Composable
fun NavRoot() {
    val backStack = rememberNavBackStack(CalculatorRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<CalculatorRoute> {
                CalculatorRoot(onNavigateToSettings = { backStack.add(SettingsRoute) })
            }
            entry<SettingsRoute> {
                SettingsRoot(onNavigateBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
