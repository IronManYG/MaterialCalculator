package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.components.SifrCalcTopBar
import dev.gaddal.sifr.core.ui.components.SifrChip
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.toDisplayExpression
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.domain.toFraction
import dev.gaddal.sifr.feature.calculator.ui.CalculatorState
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction
import dev.gaddal.sifr.feature.calculator.ui.basicRows
import dev.gaddal.sifr.feature.calculator.ui.memoryRow
import dev.gaddal.sifr.feature.calculator.ui.scientificCells

/**
 * Restructured landscape layout (v1.7 Phase C). A shared [SifrCalcTopBar] spans
 * the top (brand + History · Tools · Rotate · Settings — the ƒ toggle is hidden
 * since scientific cells are always present here, and Rotate is the active icon
 * so the user can always get back to portrait). Below it: a compact display that
 * reuses the portrait two-line model (angle/memory chips, expression, live
 * preview, and the `= result` morph with COPY · SHARE · ANS), then a two-column
 * body split into a scientific block (leading edge) and a basic block (trailing
 * edge). Under RTL (`LayoutDirection.Rtl`) Compose's Row flips the children order
 * automatically, so the basic block ends up on the leading (right) edge —
 * matching the SPEC's RTL mirroring requirement.
 *
 * The scientific block is always visible in landscape regardless of `state.mode`
 * — rotation is opportunistic, not a forced mode change. The persisted mode is
 * honored again on flipping back to portrait.
 */
@Composable
fun CalculatorScreenLandscape(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = true,
) {
    val sifr = SifrTokens.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            SifrCalcTopBar(
                onHistory = dropUnlessResumed { onAction(CalculatorAction.HistoryClicked) },
                onTools = {},                              // gated: Tools screen ships in a later milestone
                onScientific = {},                         // sci cells always visible in landscape
                scientificActive = false,
                onRotate = onRotate,
                rotateActive = rotateActive,
                onSettings = dropUnlessResumed { onAction(CalculatorAction.SettingsClicked) },
                // ƒ toggle hidden in landscape (sci is always on here) — matches the
                // prototype top bar (ui-bits.jsx:128, !land gate).
                showScientific = false,
                modifier = Modifier
                    .statusBarsPadding()
                    .displayCutoutPadding(),
                historyCd = stringResource(R.string.calc_open_history),
                toolsCd = stringResource(R.string.calc_open_tools),
                scientificCd = stringResource(R.string.calc_toggle_mode),
                rotateCd = stringResource(R.string.calc_rotate_orientation),
                settingsCd = stringResource(R.string.calc_open_settings),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .displayCutoutPadding()
                .padding(horizontal = 14.dp),
        ) {
            // Top: compact two-row display in the palette surface (block/card/inset/
            // flat). `LandscapeDisplay` collapses the portrait's vertical stack into
            // two rows — [chips · input] and [actions · = result] — so it fits the
            // short strip without spilling into the keypad, and frees vertical room
            // for the keypad. `compact` trims the surface's vertical insets.
            CalculatorDisplaySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.36f),
                compact = true,
            ) {
                LandscapeDisplay(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp),
                )
            }
            // Body: two-column split. Weight complements the display's 0.36 so the
            // keypad rows have proportional vertical room.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.64f)
                    .padding(top = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScientificBlockLandscape(
                    angleUnit = state.angleUnit,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                BasicBlockLandscape(
                    onAction = onAction,
                    memoryKeysVisible = state.memoryKeysVisible,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

/**
 * Compact two-row display for the short landscape strip. Unlike the portrait
 * [CalculatorDisplay] (which stacks chips → input → result → actions vertically),
 * this collapses to two rows so it fits without spilling into the keypad:
 *
 *   row 1:  [DEG · M chips]            ............  [input / error]
 *   row 2:  [COPY · SHARE · ANS]       ............  [= result / = preview]
 *
 * Each row weight-splits its width: the chips/actions take the visual LEFT, the
 * math takes a weighted slot on the visual RIGHT — so the right-aligned text can
 * never overlap the left chips, and the two rows can never overlap each other.
 *
 * The whole thing is forced LTR (like the prototype's `dir="ltr"` landscape root,
 * landscape.jsx:51): chips/actions stay on the visual left and the expression /
 * result stay on the visual right in both locales (each Text still shapes its own
 * Arabic glyphs correctly). The keypad block-order still mirrors in RTL — that's
 * handled by the outer landscape Row, not here.
 */
@Composable
private fun LandscapeDisplay(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    val showResult = state.justEvaluated && state.error == null && state.evaluatedInput != null
    // Render the divide operator as `÷` (the key's glyph) instead of the raw `/`.
    // Display-only + length-preserving, so the field's selection offsets still map.
    val mainText = (if (showResult) state.evaluatedInput.orEmpty() else state.expression).toDisplayExpression()
    val mainColor = if (state.error != null) sifr.displayError else sifr.displayExpression
    val resultColor = sifr.displayResult
    val previewColor = resultColor.copy(alpha = 0.85f)
    // Input/expression weight is palette-specific (prototype display.jsx:61 — Bayan
    // extra-bold, Raqim light, everything else medium).
    val displayWeight = when (SifrTokens.palette) {
        SifrPalette.Bayan -> FontWeight.W900
        SifrPalette.Raqim -> FontWeight.W300
        else -> FontWeight.W500
    }
    // includeFontPadding=false trims the field's vertical dead-space so the input
    // line is tight enough for the two compact rows.
    val mainStyle = TextStyle(
        color = mainColor,
        textAlign = TextAlign.End,
        fontFamily = sifr.displayFamily,
        fontWeight = displayWeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    val fraction = remember(state.expression, state.fractionResults) {
        if (!state.fractionResults) null else state.expression.toDoubleOrNull()?.toFraction()
    }
    val shownText = when {
        showResult -> "= ${state.expression}"
        !state.livePreview.isNullOrBlank() -> "= ${state.livePreview}"
        else -> null
    }
    val shownColor = if (showResult) resultColor else previewColor

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier) {
            // ── ROW 1: chips (left) · input / error (right) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LandscapeChips(state = state, onAction = onAction)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (state.error != null) {
                        BasicText(
                            text = state.error.asString(),
                            style = mainStyle.copy(fontSize = 20.sp, lineHeight = 24.sp),
                            maxLines = 2,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        val fieldValue = if (showResult) {
                            remember(mainText) {
                                TextFieldValue(text = mainText, selection = TextRange(mainText.length))
                            }
                        } else {
                            remember(mainText, state.cursor, state.selectionStart) {
                                val maxLen = mainText.length
                                TextFieldValue(
                                    text = mainText,
                                    selection = TextRange(
                                        start = state.selectionStart.coerceIn(0, maxLen),
                                        end = state.cursor.coerceIn(0, maxLen),
                                    ),
                                )
                            }
                        }
                        val hasRangeNow = rememberUpdatedState(!showResult && state.cursor != state.selectionStart)
                        AutoSizingExpressionField(
                            value = fieldValue,
                            onValueChange = { newValue ->
                                if (!showResult) {
                                    val newStart = newValue.selection.start
                                    val newEnd = newValue.selection.end
                                    if (newStart != state.selectionStart || newEnd != state.cursor) {
                                        onAction(CalculatorAction.SelectionChanged(newStart, newEnd))
                                    }
                                }
                            },
                            style = mainStyle,
                            // Lower cap + floor than portrait so the input line is short
                            // enough for the two compact rows of the landscape strip.
                            maxFontSize = 24.sp,
                            minFontSize = 18.sp,
                            cursorBrush = if (showResult) SolidColor(Color.Transparent) else SolidColor(sifr.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .filterTextContextMenuComponents { component ->
                                    if (hasRangeNow.value) component.key == TextContextMenuKeys.CopyKey else true
                                },
                        )
                    }
                }
            }
            // ── ROW 2: result actions (left) · result / preview (right) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showResult) {
                    ResultActionsRow(
                        onCopy = { onAction(CalculatorAction.CopyResult) },
                        onShare = { onAction(CalculatorAction.ShareResult) },
                        onAns = { onAction(CalculatorAction.UseAnswer) },
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (shownText != null) {
                        BasicText(
                            text = shownText,
                            style = TextStyle(
                                fontSize = if (showResult) 26.sp else 22.sp,
                                color = shownColor,
                                fontFamily = sifr.displayFamily,
                                textAlign = TextAlign.End,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.StartEllipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (fraction != null && (showResult || !state.livePreview.isNullOrBlank())) {
                        FractionLabel(
                            fraction = fraction,
                            color = shownColor,
                            fontFamily = sifr.displayFamily,
                            fontSize = if (showResult) 20.sp else 16.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeChips(
    state: CalculatorState,
    onAction: (CalculatorAction) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val angleLabel = when (state.angleUnit) {
            AngleUnit.Radians -> stringResource(R.string.settings_angle_rad)
            AngleUnit.Degrees -> stringResource(R.string.settings_angle_deg)
        }
        SifrChip(
            label = angleLabel,
            active = state.angleUnit == AngleUnit.Radians,
            onClick = { onAction(CalculatorAction.ToggleAngleUnit) },
        )
        if (state.memoryValue != null) {
            SifrChip(label = stringResource(R.string.calc_mode_chip_memory), active = true)
        }
    }
}

private const val SCIENTIFIC_COLUMNS = 3
private const val BASIC_COLUMNS = 4
// Landscape cells are visibly smaller than portrait (the panel shares
// horizontal space with another panel and total height is roughly half
// portrait). Pick a font size that fits a ~40dp cell.
private val LANDSCAPE_KEY_FONT_SIZE = 18.sp

@Composable
private fun ScientificBlockLandscape(
    angleUnit: AngleUnit,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(angleUnit) {
        scientificCells
            .filterNot { cell ->
                val text = cell.text
                (text == "deg" && angleUnit == AngleUnit.Radians) ||
                    (text == "rad" && angleUnit == AngleUnit.Degrees)
            }
            .chunked(SCIENTIFIC_COLUMNS)
    }
    WeightedButtonGrid(
        rows = rows,
        columns = SCIENTIFIC_COLUMNS,
        onAction = onAction,
        fontSize = LANDSCAPE_KEY_FONT_SIZE,
        modifier = modifier,
    )
}

@Composable
private fun BasicBlockLandscape(
    onAction: (CalculatorAction) -> Unit,
    memoryKeysVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    // Memory row at TOP of the basic block (when visible); then the standard
    // basic grid below. Chunk per section so the memory row stays its own row
    // and never bleeds into the basic rows.
    val rows = remember(memoryKeysVisible) {
        buildList {
            if (memoryKeysVisible) add(memoryRow)
            addAll(basicRows.chunked(BASIC_COLUMNS))
        }
    }
    WeightedButtonGrid(
        rows = rows,
        columns = BASIC_COLUMNS,
        onAction = onAction,
        fontSize = LANDSCAPE_KEY_FONT_SIZE,
        modifier = modifier,
    )
}

// Weighted rows + weighted cells so each panel's cells share the panel
// height evenly. LTR is forced so Compose doesn't mirror the cells inside
// a panel under Arabic locale — the *block order* still flips at the outer
// landscape Row (SPEC: basic left, scientific right in RTL), but cells
// inside each block stay LTR.
@Composable
private fun WeightedButtonGrid(
    rows: List<List<CalculatorUiAction>>,
    columns: Int,
    onAction: (CalculatorAction) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rows.forEach { rowActions ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowActions.forEach { action ->
                        CalculatorButton(
                            action = action,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            fontSize = fontSize,
                            onClick = { onAction(action.action) },
                        )
                    }
                    repeat(columns - rowActions.size) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Landscape — Scientific + Memory",
    showBackground = true,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewLandscapeScientific() = SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
    CalculatorScreenLandscape(
        state = CalculatorState(
            expression = "12xsin(45)",
            cursor = 10,
            mode = CalculatorMode.Scientific,
            angleUnit = AngleUnit.Degrees,
            livePreview = "8.485",
            memoryValue = 42.0,
        ),
        onAction = {},
    )
}

@Preview(
    name = "Landscape — Basic only",
    showBackground = true,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewLandscapeBasic() = SifrTheme(palette = SifrPalette.Raqim, themeMode = ThemeMode.Light) {
    CalculatorScreenLandscape(
        state = CalculatorState(
            expression = "100+25",
            cursor = 6,
            mode = CalculatorMode.Basic,
            angleUnit = AngleUnit.Degrees,
            livePreview = "125",
        ),
        onAction = {},
    )
}

@Preview(
    name = "Landscape — memory keys off",
    showBackground = true,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewLandscapeNoMemory() = SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
    CalculatorScreenLandscape(
        state = CalculatorState(
            expression = "7x8",
            cursor = 3,
            mode = CalculatorMode.Basic,
            angleUnit = AngleUnit.Degrees,
            livePreview = "56",
            memoryKeysVisible = false,
        ),
        onAction = {},
    )
}

// Two-line result morph (justEvaluated) + COPY · SHARE · ANS on Farah's card
// surface — the worst-case for the compact landscape strip (the card insets eat
// the most vertical room). Locks the result-state fit as the visual contract,
// the landscape analogue of CalculatorScreen's "Scientific result (Farah card)".
@Preview(
    name = "Landscape — result (Farah card)",
    showBackground = true,
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun PreviewLandscapeResult() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Dark) {
    CalculatorScreenLandscape(
        state = CalculatorState(
            expression = "0.5",
            cursor = 3,
            mode = CalculatorMode.Scientific,
            angleUnit = AngleUnit.Degrees,
            justEvaluated = true,
            evaluatedInput = "sin(30)",
            memoryValue = 42.0,
        ),
        onAction = {},
    )
}
