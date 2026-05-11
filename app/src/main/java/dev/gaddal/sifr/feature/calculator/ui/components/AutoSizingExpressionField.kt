package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A read-only single-line text field that auto-shrinks its font to fit the
 * available width, then falls back to native horizontal scroll-to-cursor
 * once the minimum font is reached.
 *
 * Combines the system-calculator behavior of "shrink first, scroll if still
 * too big" with a placeable cursor — Foundation 1.11's `TextAutoSize.StepBased`
 * is gated to `BasicText` (display-only) and is therefore incompatible with
 * cursor placement. This composable measures with `TextMeasurer` at the app
 * layer, then renders into a normal `BasicTextField(readOnly=true,
 * singleLine=true)` which natively scrolls to keep the cursor visible.
 */
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
            onValueChange = onValueChange,
            readOnly = true,
            singleLine = true,
            textStyle = style.copy(fontSize = pickedFontSize),
            cursorBrush = cursorBrush,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
