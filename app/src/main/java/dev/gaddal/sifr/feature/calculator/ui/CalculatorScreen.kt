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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dev.gaddal.sifr.core.domain.settings.RestoreTarget
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
import dev.gaddal.sifr.feature.calculator.ui.components.TapeReceipt
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CalculatorRoot(
    windowSizeClass: WindowSizeClass,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToTools: () -> Unit,
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
            CalculatorEvent.NavigateToTools -> onNavigateToTools()
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
    // Status-bar visibility (hide in landscape, show otherwise) is owned by NavRoot now — a
    // single owner keyed on (top route, orientation) — so the calculator and Tools no longer
    // race over it across navigation. This screen only drives the orientation lock below.
    val view = LocalView.current
    val activity = view.context as? Activity
    // Manual orientation only (spec §4.9 / D1): lock to portrait so the OS never
    // sensor-rotates; the Rotate top-bar action toggles. configChanges in the
    // manifest keeps this composition alive across the orientation change, so this
    // default does not fight a user-requested landscape.
    //
    // Lock portrait only on the FIRST entry, guarded by a saveable flag: opening
    // Settings/History disposes this composition, and an unguarded LaunchedEffect
    // would re-lock portrait on the way back — so rotating to landscape, opening
    // Settings, and pressing Back snapped you back to portrait. The flag survives
    // the nav round-trip (and resets on a fresh process launch → default portrait),
    // so the chosen orientation now persists across navigation.
    var didLockInitialOrientation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!didLockInitialOrientation) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            didLockInitialOrientation = true
        }
    }
    val onRotate = remember(isLandscape, activity) {
        {
            activity?.requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    if (isLandscape) {
        CalculatorScreenLandscape(
            state = state,
            onAction = viewModel::onAction,
            onRotate = onRotate,
            rotateActive = isLandscape,   // always true in the landscape branch
        )
    } else {
        CalculatorScreen(
            state = state,
            onAction = viewModel::onAction,
            onRotate = onRotate,
            rotateActive = isLandscape,   // always false in the portrait branch
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
    // Tape layout adds the in-display receipt at the top of the display; it tightens the
    // display's top padding and merges the DEG/M chips onto the handle row so the
    // input + result + COPY block still fits (worst case: scientific on a surface palette).
    val isTape = state.keypadLayout == KeypadLayout.Tape
    // Card / inset / block palettes draw their own container with 18–20dp of inner
    // padding; the display then added ANOTHER 18dp on top (≈38dp total vs the
    // prototype's ~7dp). In the two-line result state that double-pad squeezed the
    // bottom COPY·SHARE·ANS row past the surface and it dropped out of the layout
    // entirely (device-verified on Farah's tall keypad). Drop the display's redundant
    // vertical pad only while a frozen result is shown on a surface palette — the
    // surface's own inset still spaces the content, and the airy typing state is
    // untouched (the surface insets don't change, so the card doesn't jump on `=`).
    val hasSurface = sifr.displayCard != null || sifr.displayInset != null || sifr.displayBlock != null
    val resultShown = state.justEvaluated && state.error == null && state.evaluatedInput != null
    // One number-key size in both modes (the prototype's num fs is mode-independent,
    // 23px there). 27sp steps down from the old 32sp toward that — lighter keys, more
    // breathing room. Memory + scientific rows shrink further via per-row fontScale.
    val keypadFontSize = 27.sp
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            SifrCalcTopBar(
                onHistory = dropUnlessResumed { onAction(CalculatorAction.HistoryClicked) },
                onTools = dropUnlessResumed { onAction(CalculatorAction.ToolsClicked) },
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
            // Reserve a minimum for the display; the keypad shrinks its base row
            // height toward the 46dp floor (spec §4.6) before the display drops below
            // this. Scientific mode's tall keypad (sci rows + memory + 5 basic) would
            // otherwise squeeze the frozen two-line result + COPY row, dropping the
            // "= result" line (N3) — so reserve more there and let the keypad clamp a
            // touch shorter in return.
            // Tape parks an ~80–110dp history strip at the top of the display; reserve more
            // so the input + result + COPY block still clears the surface palettes' card/inset
            // clip (the keypad clamps its rows a touch shorter toward the 46dp floor in return).
            val minDisplayHeight = when {
                isScientific && isTape -> 288.dp
                isScientific -> 240.dp
                isTape -> 240.dp
                else -> 160.dp
            }
            val keypadMaxHeight = (maxHeight - minDisplayHeight).coerceAtLeast(0.dp)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = minDisplayHeight)
                        .weight(1f),
                ) {
                    // Scientific's tall keypad squeezes the display to its ~240dp floor.
                    // The two-line result + COPY row only fits there if the display sheds
                    // vertical chrome — and the card/block/inset palettes' display-surface
                    // insets eat even more room than the flat ones, which is why the COPY
                    // row was clipping/dropping per-palette. `compact` (stable for the whole
                    // scientific session, so the surface doesn't jump on `=`) trims the
                    // surface insets, the display's own vertical padding, and the input-line
                    // cap so the result state fits across all 5 palettes.
                    CalculatorDisplaySurface(
                        modifier = Modifier.fillMaxSize(),
                        compact = isScientific,
                        resultTight = hasSurface && resultShown && !isScientific,
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
                            tapeReceipt = if (isTape) {
                                { chips ->
                                    TapeReceipt(
                                        entries = state.recentHistory,
                                        onOpenHistory = { onAction(CalculatorAction.HistoryClicked) },
                                        onRestore = {
                                            // Restore the result (default) or the original expression,
                                            // per the user's Settings choice. Restoring an expression
                                            // re-shows its `= result` live preview.
                                            val value = if (state.restoreTarget == RestoreTarget.Expression) {
                                                it.expression
                                            } else {
                                                it.result
                                            }
                                            onAction(CalculatorAction.RestoreExpression(value))
                                        },
                                        chips = chips,
                                        compact = isScientific,
                                        // 3 rows in basic, 2 in scientific — uniform across every palette.
                                        // The tightened tape/line gaps keep all 3 clear of the surface
                                        // palettes' card/inset edge in basic mode.
                                        maxRows = if (isScientific) 2 else 3,
                                    )
                                }
                            } else {
                                null
                            },
                            fractionResults = state.fractionResults,
                            compact = isScientific,
                            // Tape's 3-row receipt steals ~one input-line of height; trim the
                            // basic input cap 50→44sp so input+result+COPY still clear the
                            // surface palettes' card edge. Scientific keeps its own 32sp cap.
                            maxFontSize = when {
                                isScientific -> 32.sp
                                isTape -> 44.sp
                                else -> 50.sp
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    // Tape parks the receipt at the very top, so shave the
                                    // display's top pad there; keep the bottom airy.
                                    top = when {
                                        isScientific -> 4.dp
                                        isTape -> 6.dp
                                        hasSurface && resultShown -> 2.dp
                                        else -> 18.dp
                                    },
                                    bottom = when {
                                        isScientific -> 4.dp
                                        hasSurface && resultShown -> 2.dp
                                        else -> 18.dp
                                    },
                                ),
                        )
                    }
                }
                CalculatorButtonGrid(
                    mode = state.mode,
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

// Scientific two-line result + COPY·SHARE·ANS on a card palette — the exact state
// that was clipping the COPY row before the compact-display fit fix (Farah's card
// surface insets eat the most vertical room). Locks the Task-1 regression.
@Preview(name = "Scientific result (Farah card)", showBackground = true)
@Composable
private fun PreviewScientificResult() = SifrTheme(palette = SifrPalette.Farah) {
    CalculatorScreen(
        state = CalculatorState(
            expression = "0.5",
            cursor = 3,
            mode = CalculatorMode.Scientific,
            angleUnit = AngleUnit.Degrees,
            justEvaluated = true,
            evaluatedInput = "sin(30)",
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

// Arc is content-sized (not screen-filling); the display flexes above it. The
// memory row shows in Arc when memoryKeysVisible = true (user decision 2026-06-13).
@Preview(name = "Arc (Layl dark)", showBackground = true)
@Composable
private fun PreviewArcLayout() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    CalculatorScreen(
        state = CalculatorState(
            expression = "12+5",
            cursor = 4,
            livePreview = "17",
            keypadLayout = KeypadLayout.Arc,
            memoryKeysVisible = true,
        ),
        onAction = {},
    )
}

// Tape = compact Classic (0.87× key height).
@Preview(name = "Tape — compact classic (Farah)", showBackground = true)
@Composable
private fun PreviewTapeLayout() = SifrTheme(palette = SifrPalette.Farah) {
    CalculatorScreen(
        state = CalculatorState(expression = "48", cursor = 2, keypadLayout = KeypadLayout.Tape),
        onAction = {},
    )
}

// Light-mode coverage of the two-line result state so the resultColor /
// displayResult token is verified in light theme (complement to PreviewResultActions
// which covers dark only).
@Preview(name = "Result actions (Bayan light)", showBackground = true)
@Composable
private fun PreviewResultActionsLight() = SifrTheme(palette = SifrPalette.Bayan) {
    CalculatorScreen(
        state = CalculatorState(expression = "512", cursor = 3, justEvaluated = true, evaluatedInput = "128 x 4"),
        onAction = {},
    )
}

// Farah card — the BASIC two-line result + COPY·SHARE·ANS row. This is the exact state
// whose bottom row dropped out of the card (the surface's own inner pad plus the
// display's redundant pad overhung the surface on Farah's tall keypad). Locks the
// resultTight / no-clip fit fix as the visual contract for the worst-case surface.
@Preview(name = "Result actions (Farah card)", showBackground = true)
@Composable
private fun PreviewResultActionsFarah() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Dark) {
    CalculatorScreen(
        state = CalculatorState(expression = "81", cursor = 2, justEvaluated = true, evaluatedInput = "9 x 9"),
        onAction = {},
    )
}

// Mizan inset — BASIC two-line result + COPY row, verifying the chip contrast and the
// re-centered (PlexMono) chip labels on the recessed inset surface.
@Preview(name = "Result actions (Mizan inset)", showBackground = true)
@Composable
private fun PreviewResultActionsMizan() = SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
    CalculatorScreen(
        state = CalculatorState(expression = "81", cursor = 2, justEvaluated = true, evaluatedInput = "9 x 9"),
        onAction = {},
    )
}
