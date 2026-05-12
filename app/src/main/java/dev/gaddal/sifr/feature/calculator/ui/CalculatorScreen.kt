package dev.gaddal.sifr.feature.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import dev.gaddal.sifr.core.ui.util.UiText
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
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
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorScreenLandscape
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CalculatorRoot(
    windowSizeClass: WindowSizeClass,
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
    if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
        CalculatorScreen(state = state, onAction = viewModel::onAction)
    } else {
        CalculatorScreenLandscape(state = state, onAction = viewModel::onAction)
    }
}

@Composable
fun CalculatorScreen(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
) {
    // Mode-aware ratios: in scientific mode the keypad has many more rows so
    // it needs the lion's share; in basic mode the keypad has only 6 rows so
    // the display can afford to be visually prominent. Weights are
    // complementary (sum to 1.0) so they read directly as screen percentages.
    val isScientific = state.mode == CalculatorMode.Scientific
    val displayWeight = if (isScientific) 0.30f else 0.45f
    val keypadWeight = 1f - displayWeight
    val keypadFontSize = if (isScientific) 20.sp else 32.sp
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .weight(displayWeight)
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
                            vertical = 24.dp,
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
                modifier = Modifier
                    .weight(keypadWeight)
                    .padding(8.dp),
                fontSize = keypadFontSize,
            )
        }
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun PreviewEmpty() = SifrTheme {
    CalculatorScreen(state = CalculatorState(), onAction = {})
}

@PreviewLightDark
@PreviewScreenSizes
@Composable
private fun PreviewMidTyping() = SifrTheme {
    CalculatorScreen(
        state = CalculatorState(expression = "12+5", cursor = 4, livePreview = "17"),
        onAction = {},
    )
}

@Preview(name = "Result", showBackground = true)
@Composable
private fun PreviewResult() = SifrTheme {
    CalculatorScreen(
        state = CalculatorState(expression = "17", cursor = 2),
        onAction = {},
    )
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun PreviewError() = SifrTheme {
    CalculatorScreen(
        state = CalculatorState(
            expression = "log(0)",
            cursor = 6,
            error = UiText.StringResource(R.string.calc_error_domain_error),
        ),
        onAction = {},
    )
}

@Preview(name = "Memory active", showBackground = true)
@Composable
private fun PreviewMemoryActive() = SifrTheme {
    CalculatorScreen(
        state = CalculatorState(expression = "5", cursor = 1, memoryValue = 42.0),
        onAction = {},
    )
}

@Preview(name = "Scientific mode", showBackground = true)
@Composable
private fun PreviewScientific() = SifrTheme {
    CalculatorScreen(
        state = CalculatorState(
            expression = "sin(30)",
            cursor = 7,
            mode = CalculatorMode.Scientific,
            angleUnit = AngleUnit.Degrees,
            livePreview = "0.5",
        ),
        onAction = {},
    )
}
