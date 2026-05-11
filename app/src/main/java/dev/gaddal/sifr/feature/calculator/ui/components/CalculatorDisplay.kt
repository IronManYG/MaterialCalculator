package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.util.UiText

@Composable
fun CalculatorDisplay(
    expression: String,
    livePreview: String?,
    error: UiText?,
    modifier: Modifier = Modifier,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val mainText = error?.asString() ?: expression
    val mainColor = if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val previewColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)

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
                BasicText(
                    text = mainText,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 28.sp,
                        maxFontSize = 80.sp,
                        stepSize = 4.sp,
                    ),
                    style = TextStyle(color = mainColor, textAlign = TextAlign.End),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.StartEllipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
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
