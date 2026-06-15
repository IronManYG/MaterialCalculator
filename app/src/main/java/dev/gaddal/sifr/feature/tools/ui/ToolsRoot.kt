package dev.gaddal.sifr.feature.tools.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
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

    // Status-bar visibility in landscape is owned by NavRoot (a single owner keyed on the top
    // route + orientation), so Tools no longer races the calculator over it across navigation.
    val view = LocalView.current

    // Manual orientation toggle, mirroring the calculator's Rotate action. The activity
    // already owns the initial orientation lock (CalculatorRoot), so Tools just flips it
    // on demand and inherits whatever orientation it was opened in.
    val activity = view.context as? Activity
    val onRotate = remember(isLandscape, activity) {
        {
            activity?.requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    if (isLandscape) {
        ToolsScreenLandscape(
            state = state,
            onAction = viewModel::onAction,
            onRotate = onRotate,
            rotateActive = true,
        )
    } else {
        ToolsScreen(
            state = state,
            onAction = viewModel::onAction,
            onRotate = onRotate,
            rotateActive = false,
        )
    }
}
