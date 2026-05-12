package dev.gaddal.sifr.feature.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.ui.feedback.rememberFeedbackController
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorButtonGrid
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorDisplay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CalculatorRoot(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: CalculatorViewModel = koinViewModel(),
    settingsRepository: SettingsRepository = koinInject(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsRepository.observe()
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val feedback = rememberFeedbackController(
        hapticsEnabled = settings.hapticsEnabled,
        soundEnabled = settings.soundEnabled,
    )
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CalculatorEvent.NavigateToSettings -> onNavigateToSettings()
            CalculatorEvent.NavigateToHistory -> onNavigateToHistory()
            is CalculatorEvent.PlayFeedback -> feedback.play(event.intent)
        }
    }
    CalculatorScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun CalculatorScreen(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 25.dp,
                            bottomEnd = 25.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.secondaryContainer)
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
                        .fillMaxSize()
                        .padding(
                            vertical = 64.dp,
                            horizontal = 16.dp
                        )
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
            Spacer(modifier = Modifier.height(8.dp))
            CalculatorButtonGrid(
                mode = state.mode,
                angleUnit = state.angleUnit,
                onAction = onAction,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun CalculatorScreenPreview() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "12+5",
                cursor = "12+5".length,
                livePreview = "17",
            ),
            onAction = {},
        )
    }
}
