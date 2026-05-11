package dev.gaddal.sifr.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.gaddal.sifr.feature.calculator.ui.CalculatorRoot
import dev.gaddal.sifr.feature.diag.ui.HapticsTestRoot
import dev.gaddal.sifr.feature.history.ui.HistoryRoot
import dev.gaddal.sifr.feature.settings.ui.SettingsRoot

@Composable
fun NavRoot() {
    val backStack = rememberNavBackStack(CalculatorRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<CalculatorRoute> {
                CalculatorRoot(
                    onNavigateToSettings = { backStack.add(SettingsRoute) },
                    onNavigateToHistory = { backStack.add(HistoryRoute) },
                )
            }
            entry<SettingsRoute> {
                SettingsRoot(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToHapticsTest = { backStack.add(HapticsTestRoute) },
                )
            }
            entry<HistoryRoute> {
                HistoryRoot(onNavigateBack = { backStack.removeLastOrNull() })
            }
            entry<HapticsTestRoute> {
                HapticsTestRoot(onNavigateBack = { backStack.removeLastOrNull() })
            }
        },
    )
}
