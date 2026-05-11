package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.awaitCancellation

/**
 * A single-line text field that auto-shrinks its font to fit the available
 * width, then falls back to native horizontal scroll-to-cursor once the
 * minimum font is reached. The cursor is visible and tap-to-position works
 * just like a regular editable field, but the IME (soft keyboard) is
 * blocked at the platform-input-session layer and any text mutations are
 * dropped — the only way to change the text is through the parent's
 * button-driven action pipeline.
 *
 * Combines the system-calculator behavior of "shrink first, scroll if still
 * too big" with a placeable cursor. Foundation 1.11's `TextAutoSize.StepBased`
 * is gated to `BasicText` (display-only) and is incompatible with cursor
 * placement, so this composable measures with `TextMeasurer` at the app
 * layer and renders into a normal `BasicTextField`.
 *
 * IME suppression: `InterceptPlatformTextInput` wraps the field and the
 * interceptor suspends forever via `awaitCancellation()`, so the platform
 * text-input session never starts. Focus and the blinking cursor still
 * work because they live above the platform-input layer. `onValueChange`
 * also drops any text mutation as a defensive second layer in case input
 * arrives through some other channel.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AutoSizingExpressionField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 28.sp,
    maxFontSize: TextUnit = 80.sp,
    style: TextStyle = TextStyle.Default,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
) {
    InterceptPlatformTextInput(
        interceptor = { _, _ -> awaitCancellation() },
    ) {
        BoxWithConstraints(modifier = modifier) {
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            val maxWidthPx = with(density) { maxWidth.toPx() }

            val pickedFontSize: TextUnit = remember(value.text, maxWidthPx, style) {
                if (value.text.isEmpty()) return@remember maxFontSize
                val annotated = AnnotatedString(value.text)
                var candidate = maxFontSize
                while (candidate.value > minFontSize.value) {
                    val result = textMeasurer.measure(
                        text = annotated,
                        style = style.copy(fontSize = candidate),
                        constraints = Constraints(maxWidth = Int.MAX_VALUE),
                        overflow = TextOverflow.Visible,
                        softWrap = false,
                    )
                    if (result.size.width <= maxWidthPx) return@remember candidate
                    val next = (candidate.value * 0.9f).coerceAtLeast(minFontSize.value)
                    if (next == candidate.value) return@remember minFontSize
                    candidate = next.sp
                }
                minFontSize
            }

            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    // Defensive: drop any text mutation. The interceptor above blocks
                    // the IME from ever starting, but text could in principle arrive
                    // through accessibility services or hardware keyboards. Selection
                    // (cursor) diffs still propagate so tap-to-position works.
                    if (newValue.text == value.text) {
                        onValueChange(newValue)
                    }
                },
                singleLine = true,
                textStyle = style.copy(fontSize = pickedFontSize),
                cursorBrush = cursorBrush,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
