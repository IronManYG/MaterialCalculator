package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.util.UiText

@Composable
fun CalculatorDisplay(
    expression: String,
    cursor: Int,
    livePreview: String?,
    error: UiText?,
    onCursorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val mainColor = if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val previewColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
    val mainStyle = TextStyle(color = mainColor, textAlign = TextAlign.End)

    Box(
        modifier = modifier,
        contentAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        // Force math content to render LTR regardless of system locale.
        // The outer Box already picked the locale-correct anchor above; this
        // inner scope stops the Unicode bidi algorithm from flipping trailing
        // weak operators (e.g. `+` in `10+`) to the visual leading edge.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (error != null) {
                    BasicText(
                        text = error.asString(),
                        style = mainStyle.copy(fontSize = 56.sp),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    val fieldValue = remember(expression, cursor) {
                        TextFieldValue(
                            text = expression,
                            selection = TextRange(cursor.coerceIn(0, expression.length)),
                        )
                    }
                    AutoSizingExpressionField(
                        value = fieldValue,
                        onValueChange = { newValue ->
                            // readOnly=true blocks IME text edits; we only react to
                            // selection (cursor) moves driven by tap.
                            if (newValue.selection.start != cursor) {
                                onCursorChange(newValue.selection.start)
                            }
                        },
                        style = mainStyle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error == null && !livePreview.isNullOrBlank()) {
                    BasicText(
                        text = livePreview,
                        style = TextStyle(
                            fontSize = 22.sp,
                            color = previewColor,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
