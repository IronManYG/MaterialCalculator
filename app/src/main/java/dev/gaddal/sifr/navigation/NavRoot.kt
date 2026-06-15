package dev.gaddal.sifr.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.gaddal.sifr.feature.calculator.ui.CalculatorRoot
import dev.gaddal.sifr.feature.history.ui.HistoryRoot
import dev.gaddal.sifr.feature.settings.ui.SettingsRoot
import dev.gaddal.sifr.feature.tools.ui.ToolsRoot

private const val SLIDE_DURATION_MS = 300
private const val FADE_IN_DURATION_MS = 300
private const val FADE_OUT_DURATION_MS = 200

@Composable
fun NavRoot(windowSizeClass: WindowSizeClass) {
    val backStack = rememberNavBackStack(CalculatorRoute)

    // Status-bar visibility is owned HERE, in the one composition that outlives every
    // navigation — not per-screen. The Calculator and Tools go immersive in landscape (the
    // keypad reclaims the status-bar strip, enableEdgeToEdge() already draws under it);
    // Settings/History always keep the bar. Two per-screen DisposableEffects used to fight
    // over this: the screen being LEFT re-showed the bar in its onDispose, which fires AFTER
    // the entering screen hid it, so the bar leaked back (rotate→Tools, and on the way back to
    // the calculator). One owner keyed on (topRoute, orientation) has no such race.
    val view = LocalView.current
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val topRoute = backStack.lastOrNull()
    val immersive = isLandscape && (topRoute == CalculatorRoute || topRoute == ToolsRoute)
    DisposableEffect(immersive) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            if (immersive) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            // Only fires when the whole nav root leaves composition (app teardown) — restore
            // a normal window so we never strand a hidden bar.
            val w = (view.context as? Activity)?.window
            if (w != null) {
                WindowInsetsControllerCompat(w, view).show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

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
                        onNavigateToTools = { backStack.add(ToolsRoute) },
                    )
                }
                entry<SettingsRoute> {
                    SettingsRoot(onNavigateBack = { backStack.removeLastOrNull() })
                }
                entry<HistoryRoute> {
                    HistoryRoot(onNavigateBack = { backStack.removeLastOrNull() })
                }
                entry<ToolsRoute> {
                    ToolsRoot(
                        windowSizeClass = windowSizeClass,
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}
