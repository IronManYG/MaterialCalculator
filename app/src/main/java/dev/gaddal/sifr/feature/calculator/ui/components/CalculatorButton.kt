package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction

@Composable
fun CalculatorButton(
    action: CalculatorUiAction,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    onClick: () -> Unit
) {
    val style = SifrTokens.colors.keyStyle(action.role)
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(brush = style.container)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (action.text != null) {
            Text(
                text = action.text,
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                color = style.content
            )
        } else {
            action.content()
        }
    }
}
