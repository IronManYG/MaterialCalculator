package dev.gaddal.sifr.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.gaddal.sifr.feature.calculator.ui.CalculatorRoot
import dev.gaddal.sifr.feature.history.ui.HistoryRoot
import dev.gaddal.sifr.feature.settings.ui.SettingsRoot

private const val SLIDE_DURATION_MS = 300
private const val FADE_IN_DURATION_MS = 300
private const val FADE_OUT_DURATION_MS = 200

@Composable
fun NavRoot(windowSizeClass: WindowSizeClass) {
    val backStack = rememberNavBackStack(CalculatorRoute)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = {
                slideIntoContainer(
                    towards = SlideDirection.Start,
                    animationSpec = tween(SLIDE_DURATION_MS),
                ) + fadeIn(tween(FADE_IN_DURATION_MS)) togetherWith
                    slideOutOfContainer(
                        towards = SlideDirection.Start,
                        animationSpec = tween(SLIDE_DURATION_MS),
                    ) + fadeOut(tween(FADE_OUT_DURATION_MS))
            },
            popTransitionSpec = {
                slideIntoContainer(
                    towards = SlideDirection.End,
                    animationSpec = tween(SLIDE_DURATION_MS),
                ) + fadeIn(tween(FADE_IN_DURATION_MS)) togetherWith
                    slideOutOfContainer(
                        towards = SlideDirection.End,
                        animationSpec = tween(SLIDE_DURATION_MS),
                    ) + fadeOut(tween(FADE_OUT_DURATION_MS))
            },
            predictivePopTransitionSpec = { _ ->
                slideIntoContainer(
                    towards = SlideDirection.End,
                    animationSpec = tween(SLIDE_DURATION_MS),
                ) + fadeIn(tween(FADE_IN_DURATION_MS)) togetherWith
                    slideOutOfContainer(
                        towards = SlideDirection.End,
                        animationSpec = tween(SLIDE_DURATION_MS),
                    ) + fadeOut(tween(FADE_OUT_DURATION_MS))
            },
            entryProvider = entryProvider {
                entry<CalculatorRoute> {
                    CalculatorRoot(
                        windowSizeClass = windowSizeClass,
                        onNavigateToSettings = { backStack.add(SettingsRoute) },
                        onNavigateToHistory = { backStack.add(HistoryRoute) },
                    )
                }
                entry<SettingsRoute> {
                    SettingsRoot(onNavigateBack = { backStack.removeLastOrNull() })
                }
                entry<HistoryRoute> {
                    HistoryRoot(onNavigateBack = { backStack.removeLastOrNull() })
                }
            },
        )
    }
}
