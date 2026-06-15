package dev.gaddal.sifr.feature.tools.ui

import android.app.Activity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun ToolsRoot(
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
    viewModel: ToolsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ToolsEvent.NavigateBack -> onNavigateBack()
            ToolsEvent.RatesRefreshFailed -> Unit // silent; FX footnote already shows offline state
        }
    }
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // Mirror CalculatorRoot: hide the status bar in landscape so Tools matches the
    // calculator's immersive landscape. The window already draws edge-to-edge
    // (enableEdgeToEdge() in MainActivity); CalculatorRoot.onDispose re-shows the status
    // bar on navigating here, so Tools must hide it again — and restore it on leaving.
    val view = LocalView.current
    DisposableEffect(isLandscape) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            if (isLandscape) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            val w = (view.context as? Activity)?.window
            if (w != null) {
                WindowInsetsControllerCompat(w, view).show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    if (isLandscape) {
        ToolsScreenLandscape(state = state, onAction = viewModel::onAction)
    } else {
        ToolsScreen(state = state, onAction = viewModel::onAction)
    }
}
