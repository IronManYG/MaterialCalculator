package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    if (isLandscape) {
        ToolsScreenLandscape(state = state, onAction = viewModel::onAction)
    } else {
        ToolsScreen(state = state, onAction = viewModel::onAction)
    }
}
