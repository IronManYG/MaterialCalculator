package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.ui.components.SifrChip
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.UiText
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.toFraction

// Default preview-slot height keeps the Column's total height — and
// therefore the outer Box's vertical centering — stable when the preview
// content appears / disappears. 32dp comfortably holds 22sp text plus
// default line metrics. Landscape lowers this to fit a shorter display
// strip without clipping.
private val DEFAULT_PREVIEW_SLOT_HEIGHT = 32.dp

@Composable
fun CalculatorDisplay(
    expression: String,
    cursor: Int,
    selectionStart: Int,
    livePreview: String?,
    error: UiText?,
    onSelectionChange: (start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier,
    evaluatedInput: String? = null,
    justEvaluated: Boolean = false,
    angleUnit: AngleUnit? = null,
    memoryValue: Double? = null,
    onToggleAngleUnit: () -> Unit = {},
    resultActions: (@Composable () -> Unit)? = null,
    // Tape in-display receipt (keypadLayout == Tape): renders at the top of the display
    // and is HANDED the mode-chips composable so it can tuck DEG/M onto its pull-handle
    // row (reclaiming the standalone chips band). Manages its own RTL mirroring, so it
    // sits outside the forced-LTR math block below. Null for every other layout.
    tapeReceipt: (@Composable (chips: @Composable () -> Unit) -> Unit)? = null,
    maxFontSize: TextUnit = 50.sp,
    previewSlotHeight: Dp = DEFAULT_PREVIEW_SLOT_HEIGHT,
    fractionResults: Boolean = false,
    // Scientific mode squeezes the display; `compact` shaves the inter-line gaps in
    // the two-line result state so the COPY row stops clipping there.
    compact: Boolean = false,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val sifr = SifrTokens.colors
    val mainColor = if (error != null) sifr.displayError else sifr.displayExpression
    val resultColor = sifr.displayResult
    val previewColor = sifr.displayResult.copy(alpha = 0.85f)
    // Input/expression weight is palette-specific (prototype display.jsx:66 —
    // Bayan extra-bold, Raqim light, everything else medium).
    val displayWeight = when (SifrTokens.palette) {
        SifrPalette.Bayan -> FontWeight.W900
        SifrPalette.Raqim -> FontWeight.W300
        else -> FontWeight.W500
    }
    val mainStyle = TextStyle(
        color = mainColor,
        textAlign = TextAlign.End,
        fontFamily = sifr.displayFamily,
        fontWeight = displayWeight,
    )

    // While justEvaluated the 50sp line shows the INPUT the user typed (kept in
    // evaluatedInput) and the result drops to its own 30sp line below — matching
    // the prototype (display.jsx:66-89). Otherwise the line is the live editable
    // expression. Landscape passes evaluatedInput=null/justEvaluated=false, so it
    // shows the plain expression (the writer's value, = the result after '=').
    val showResult = justEvaluated && error == null && evaluatedInput != null
    val mainText = if (showResult) evaluatedInput else expression

    // Fraction shared by the result / preview / bare-number lines: only when the
    // toggle is on AND the whole expression is a bare finite number with a
    // representable fraction (toFraction returns null for integers / non-reducible).
    val fraction = remember(expression, fractionResults) {
        if (!fractionResults) null else expression.toDoubleOrNull()?.toFraction()
    }

    // Scientific (compact) tightens the inter-line gaps so the two-line result + COPY
    // row fits the squeezed display without clipping — even in the airy palettes. Tape
    // (tapeReceipt != null) tightens the same way so all 3 history rows + the input +
    // result + COPY clear the surface palettes' card/inset edge in basic mode.
    val lineGap = if (compact || tapeReceipt != null) 2.dp else 4.dp
    Box(
        modifier = modifier,
        contentAlignment = if (isRtl) Alignment.BottomEnd else Alignment.BottomStart,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(lineGap),
            horizontalAlignment = Alignment.End,
        ) {
            // Mode-chips (DEG/RAD + M) — kept in the AMBIENT layout direction (outside the
            // forced-LTR math block below) so they sit on the leading edge and mirror to
            // the right in RTL, matching the prototype (only display.jsx's math lines are
            // dir="ltr"; the chip band follows the locale, display.jsx:61).
            val chips: @Composable () -> Unit = {
                if (angleUnit != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val angleLabel = when (angleUnit) {
                            AngleUnit.Radians -> stringResource(R.string.settings_angle_rad)
                            AngleUnit.Degrees -> stringResource(R.string.settings_angle_deg)
                        }
                        SifrChip(
                            label = angleLabel,
                            active = angleUnit == AngleUnit.Radians,
                            onClick = onToggleAngleUnit,
                        )
                        if (memoryValue != null) {
                            SifrChip(label = stringResource(R.string.calc_mode_chip_memory), active = true)
                        }
                    }
                }
            }

            if (tapeReceipt != null) {
                // 0) Tape receipt — last-3 history strip + pull-handle (Tape layout only).
                // It hosts the chips on its handle row, so we don't render a separate band.
                tapeReceipt(chips)
            } else {
                // Standalone chips band above the math block (every non-Tape layout).
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = if (compact) 3.dp else 6.dp),
                ) {
                    chips()
                }
            }

            // Force math content LTR regardless of system locale (keeps trailing weak
            // operators like the '+' in "10+" — and the result — on the visual
            // trailing/right edge).
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(lineGap),
                    horizontalAlignment = Alignment.End,
                ) {
                    // 2) Expression / input line.
                    if (error != null) {
                        // Wrap up to two lines at 28sp so long localized error strings
                        // fit without clipping (QA: Honor 400 Pro overflow).
                        BasicText(
                            text = error.asString(),
                            style = mainStyle.copy(fontSize = 28.sp, textAlign = TextAlign.End, lineHeight = 32.sp),
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
                            remember(mainText, cursor, selectionStart) {
                                val maxLen = mainText.length
                                TextFieldValue(
                                    text = mainText,
                                    selection = TextRange(
                                        start = selectionStart.coerceIn(0, maxLen),
                                        end = cursor.coerceIn(0, maxLen),
                                    ),
                                )
                            }
                        }
                        val hasRangeNow = rememberUpdatedState(!showResult && cursor != selectionStart)
                        AutoSizingExpressionField(
                            value = fieldValue,
                            onValueChange = { newValue ->
                                // Frozen result line is non-editable; only react to
                                // tap/drag selection changes while editing.
                                if (!showResult) {
                                    val newStart = newValue.selection.start
                                    val newEnd = newValue.selection.end
                                    if (newStart != selectionStart || newEnd != cursor) {
                                        onSelectionChange(newStart, newEnd)
                                    }
                                }
                            },
                            style = mainStyle,
                            maxFontSize = maxFontSize,
                            // Hide the caret while showing a frozen result (prototype
                            // hides the blink on justEvaluated, display.jsx:68).
                            cursorBrush = if (showResult) SolidColor(Color.Transparent) else SolidColor(sifr.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                // Long-press toolbar: with a range selected expose only
                                // Copy (writer ignores text mutations); collapsed
                                // selection falls through to the default menu.
                                .filterTextContextMenuComponents { component ->
                                    if (hasRangeNow.value) component.key == TextContextMenuKeys.CopyKey else true
                                },
                        )
                    }

                    // 3) Result / preview line — fraction renders INLINE to its right
                    // (prototype display.jsx:77 & 87), not on its own line.
                    if (error == null) {
                        if (showResult) {
                            // Dedicated 30sp result line + inline fraction.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (compact || tapeReceipt != null) 4.dp else 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                BasicText(
                                    text = "= $expression",
                                    style = TextStyle(
                                        fontSize = if (compact) 26.sp else 30.sp,
                                        color = resultColor,
                                        fontFamily = sifr.displayFamily,
                                        textAlign = TextAlign.End,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.StartEllipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (fraction != null) {
                                    FractionLabel(
                                        fraction = fraction,
                                        color = resultColor,
                                        fontFamily = sifr.displayFamily,
                                        fontSize = 22.sp,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                }
                            }
                        } else if (!livePreview.isNullOrBlank()) {
                            // Live preview at 22sp in a fixed-height slot, prefixed with
                            // '= ' (prototype display.jsx:87) + inline fraction.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(previewSlotHeight),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    BasicText(
                                        text = "= $livePreview",
                                        style = TextStyle(
                                            fontSize = 22.sp,
                                            color = previewColor,
                                            fontFamily = sifr.displayFamily,
                                            textAlign = TextAlign.End,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.StartEllipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (fraction != null) {
                                        FractionLabel(
                                            fraction = fraction,
                                            color = previewColor,
                                            fontFamily = sifr.displayFamily,
                                            fontSize = 17.sp,
                                            modifier = Modifier.padding(start = 10.dp),
                                        )
                                    }
                                }
                            }
                        } else if (fraction != null) {
                            // Bare number with no result/preview line: fraction stands alone.
                            FractionLabel(
                                fraction = fraction,
                                color = previewColor,
                                fontFamily = sifr.displayFamily,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }

                        // 4) In-flow result-actions (COPY · SHARE [· ANS]) — portrait
                        // passes the slot; landscape leaves it null and keeps its own.
                        if (showResult && resultActions != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = if (compact) 6.dp else 12.dp),
                            ) {
                                resultActions()
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — every reachable display state
// ---------------------------------------------------------------------------

// State 1: Live typing — DEG chip + 50sp expression + 22sp live-preview line.
@Preview(name = "CalculatorDisplay — live typing", showBackground = true)
@Composable
private fun PreviewCalculatorDisplayLiveTyping() = SifrTheme(palette = SifrPalette.Layl) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(18.dp),
    ) {
        CalculatorDisplay(
            expression = "12+5",
            cursor = 4,
            selectionStart = 4,
            livePreview = "17",
            error = null,
            onSelectionChange = { _, _ -> },
            angleUnit = AngleUnit.Degrees,
        )
    }
}

// State 2: Two-line result (HEADLINE state) — evaluatedInput on top, = result below,
// COPY · SHARE · ANS in-flow. @PreviewLightDark + SifrTheme(palette) with default
// themeMode=ThemeMode.System lets the preview framework flip isSystemInDarkTheme()
// per variant, producing one light and one dark render from a single annotation.
@PreviewLightDark
@Composable
private fun PreviewCalculatorDisplayResult() = SifrTheme(palette = SifrPalette.Layl) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(18.dp),
    ) {
        CalculatorDisplay(
            expression = "512",
            cursor = 3,
            selectionStart = 3,
            livePreview = null,
            error = null,
            onSelectionChange = { _, _ -> },
            evaluatedInput = "128 x 4",
            justEvaluated = true,
            angleUnit = AngleUnit.Degrees,
            resultActions = {
                ResultActionsRow(onCopy = {}, onShare = {}, onAns = {})
            },
        )
    }
}

// State 3: Error — 28sp two-line error text; no chips needed.
@Preview(name = "CalculatorDisplay — error", showBackground = true)
@Composable
private fun PreviewCalculatorDisplayError() = SifrTheme(palette = SifrPalette.Layl) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(18.dp),
    ) {
        CalculatorDisplay(
            expression = "log(0)",
            cursor = 6,
            selectionStart = 6,
            livePreview = null,
            error = UiText.StringResource(R.string.calc_error_domain_error),
            onSelectionChange = { _, _ -> },
            angleUnit = AngleUnit.Degrees,
        )
    }
}

// State 4: Chips band — RAD active + memory chip visible.
@Preview(name = "CalculatorDisplay — RAD + memory", showBackground = true)
@Composable
private fun PreviewCalculatorDisplayRadMemory() = SifrTheme(palette = SifrPalette.Layl) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(18.dp),
    ) {
        CalculatorDisplay(
            expression = "sin(1)",
            cursor = 6,
            selectionStart = 6,
            livePreview = "1.5",
            error = null,
            onSelectionChange = { _, _ -> },
            angleUnit = AngleUnit.Radians,
            memoryValue = 42.0,
        )
    }
}
