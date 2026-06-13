package dev.gaddal.sifr.feature.calculator.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.components.SifrCalcTopBar
import dev.gaddal.sifr.core.ui.util.UiText
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import dev.gaddal.sifr.core.ui.feedback.rememberFeedbackController
import dev.gaddal.sifr.core.ui.theme.PalettePreviewProvider
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorButtonGrid
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorDisplay
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorDisplaySurface
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorScreenLandscape
import dev.gaddal.sifr.feature.calculator.ui.components.ResultActionsRow
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
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            CalculatorEvent.NavigateToSettings -> onNavigateToSettings()
            CalculatorEvent.NavigateToHistory -> onNavigateToHistory()
            is CalculatorEvent.PlayFeedback -> feedback.play(event.intent)
            is CalculatorEvent.CopyToClipboard -> {
                ContextCompat.getSystemService(context, ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("Sifr", event.text))
                feedback.play(FeedbackIntent.Selection)
            }
            is CalculatorEvent.ShareText -> {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, event.text)
                }
                context.startActivity(Intent.createChooser(send, null))
            }
        }
    }
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    // Hide the status bar in landscape so the keypad gets that vertical
    // strip back. enableEdgeToEdge() in MainActivity already makes the
    // window draw under the bars; here we just toggle visibility.
    // Behavior = SHOW_TRANSIENT_BARS_BY_SWIPE lets users swipe from the
    // top to peek the status bar if they need it.
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
            // Always restore on leaving the calculator screen so Settings /
            // History inherit a normal status-bar window.
            val w = (view.context as? Activity)?.window
            if (w != null) {
                WindowInsetsControllerCompat(w, view)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
    val activity = view.context as? Activity
    // Manual orientation only (spec §4.9 / D1): lock to portrait on entry so the
    // OS never sensor-rotates; the Rotate top-bar action toggles. configChanges in
    // the manifest keeps this composition alive across the orientation change, so
    // this default does not fight a user-requested landscape.
    LaunchedEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    val onRotate = remember(isLandscape, activity) {
        {
            activity?.requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    if (isLandscape) {
        CalculatorScreenLandscape(state = state, onAction = viewModel::onAction)
    } else {
        CalculatorScreen(
            state = state,
            onAction = viewModel::onAction,
            onRotate = onRotate,
            rotateActive = isLandscape,   // always false in portrait branch; Phase C wires landscape→true
        )
    }
}

@Composable
fun CalculatorScreen(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = false,
) {
    // The keypad is content-sized (fixed-height rows, Task 3); the display takes
    // the remaining space and bottom-aligns its content (prototype flex:1 +
    // justify-end). On short screens the keypad clamps its row height toward the
    // 46dp floor (handled in CalculatorButtonGrid) before the display collapses.
    val sifr = SifrTokens.colors
    val isScientific = state.mode == CalculatorMode.Scientific
    val keypadFontSize = if (isScientific) 20.sp else 32.sp
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            SifrCalcTopBar(
                onHistory = dropUnlessResumed { onAction(CalculatorAction.HistoryClicked) },
                onTools = {},                              // gated: Tools screen ships in a later milestone
                onScientific = dropUnlessResumed { onAction(CalculatorAction.ToggleMode) },
                scientificActive = state.mode == CalculatorMode.Scientific,
                onRotate = onRotate,
                rotateActive = rotateActive,
                onSettings = dropUnlessResumed { onAction(CalculatorAction.SettingsClicked) },
                modifier = Modifier.statusBarsPadding(),
                historyCd = stringResource(R.string.calc_open_history),
                toolsCd = stringResource(R.string.calc_open_tools),
                scientificCd = stringResource(R.string.calc_toggle_mode),
                rotateCd = stringResource(R.string.calc_rotate_orientation),
                settingsCd = stringResource(R.string.calc_open_settings),
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
        ) {
            // Reserve a minimum for the display; the keypad may shrink its base
            // row height toward the 46dp floor (spec §4.6) so it never crowds the
            // display below this on short screens.
            val minDisplayHeight = 160.dp
            val keypadMaxHeight = (maxHeight - minDisplayHeight).coerceAtLeast(0.dp)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minDisplayHeight)
                        .weight(1f),
                ) {
                    CalculatorDisplaySurface(modifier = Modifier.fillMaxSize()) {
                        CalculatorDisplay(
                            expression = state.expression,
                            cursor = state.cursor,
                            selectionStart = state.selectionStart,
                            livePreview = state.livePreview,
                            error = state.error,
                            onSelectionChange = { start, end ->
                                onAction(CalculatorAction.SelectionChanged(start, end))
                            },
                            evaluatedInput = state.evaluatedInput,
                            justEvaluated = state.justEvaluated,
                            angleUnit = state.angleUnit,
                            memoryValue = state.memoryValue,
                            onToggleAngleUnit = { onAction(CalculatorAction.ToggleAngleUnit) },
                            resultActions = {
                                ResultActionsRow(
                                    onCopy = { onAction(CalculatorAction.CopyResult) },
                                    onShare = { onAction(CalculatorAction.ShareResult) },
                                    onAns = { onAction(CalculatorAction.UseAnswer) },
                                )
                            },
                            fractionResults = state.fractionResults,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 18.dp),
                        )
                    }
                }
                CalculatorButtonGrid(
                    mode = state.mode,
                    angleUnit = state.angleUnit,
                    layout = state.keypadLayout,
                    memoryKeysVisible = state.memoryKeysVisible,
                    onAction = onAction,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    fontSize = keypadFontSize,
                    maxHeight = keypadMaxHeight,
                )
            }
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
private fun PreviewMidTyping() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
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

@Preview(name = "Result actions (Layl dark)", showBackground = true)
@Composable
private fun PreviewResultActions() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    CalculatorScreen(
        state = CalculatorState(expression = "512", cursor = 3, justEvaluated = true, evaluatedInput = "128 x 4"),
        onAction = {},
    )
}

@Preview(name = "Result (Bayan light)", showBackground = true)
@Composable
private fun PreviewResultBayan() = SifrTheme(palette = SifrPalette.Bayan) {
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
private fun PreviewScientific() = SifrTheme(palette = SifrPalette.Farah) {
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

@Preview(name = "Palette sweep", showBackground = true)
@Composable
private fun PreviewAllPalettes(
    @PreviewParameter(PalettePreviewProvider::class) palette: SifrPalette,
) = SifrTheme(palette = palette) {
    CalculatorScreen(
        state = CalculatorState(expression = "12+5", cursor = 4, livePreview = "17"),
        onAction = {},
    )
}

@Preview(name = "Farah card (light)", showBackground = true)
@Composable
private fun PreviewFarahCard() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
    CalculatorScreen(state = CalculatorState(expression = "48", cursor = 2), onAction = {})
}

@Preview(name = "Mizan inset (dark)", showBackground = true)
@Composable
private fun PreviewMizanInset() = SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
    CalculatorScreen(state = CalculatorState(expression = "48", cursor = 2), onAction = {})
}

@Preview(name = "Fraction result (Raqim)", showBackground = true)
@Composable
private fun PreviewFraction() = SifrTheme(palette = SifrPalette.Raqim) {
    CalculatorScreen(
        state = CalculatorState(expression = "0.75", cursor = 4, fractionResults = true),
        onAction = {},
    )
}

@Preview(name = "Remix (Layl dark)", showBackground = true)
@Composable
private fun PreviewRemix() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    CalculatorScreen(
        state = CalculatorState(expression = "12+5", cursor = 4, livePreview = "17", keypadLayout = KeypadLayout.Remix),
        onAction = {},
    )
}

@Preview(name = "Remix — memory keys off (Bayan)", showBackground = true)
@Composable
private fun PreviewRemixNoMemory() = SifrTheme(palette = SifrPalette.Bayan) {
    CalculatorScreen(
        state = CalculatorState(expression = "7", cursor = 1, keypadLayout = KeypadLayout.Remix, memoryKeysVisible = false),
        onAction = {},
    )
}
