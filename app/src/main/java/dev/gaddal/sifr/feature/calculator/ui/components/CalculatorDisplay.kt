package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.util.UiText

@Composable
fun CalculatorDisplay(
    expression: String,
    error: UiText?,
    modifier: Modifier = Modifier
) {
    val text = error?.asString() ?: expression
    val textColor =
        if (error != null) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = text,
            onValueChange = {},
            textStyle = TextStyle(
                fontSize = 80.sp,
                color = textColor,
                textAlign = TextAlign.End
            ),
            maxLines = 1,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
