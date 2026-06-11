package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction

@Composable
fun CalculatorButton(
    action: CalculatorUiAction,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    onClick: () -> Unit,
) {
    val sifr = SifrTokens.colors
    val style = sifr.keyStyle(action.role)
    val shape = RoundedCornerShape(sifr.keyRadius)

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !sifr.raisedKeys) 0.94f else 1f,
        animationSpec = tween(90), label = "keyScale",
    )
    val pressTranslateY by animateFloatAsState(
        targetValue = if (pressed && sifr.raisedKeys) 2f else 0f,
        animationSpec = tween(90), label = "keyTranslate",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = pressTranslateY * density
                // The drawBehind drop shadow overdraws 3dp below the key bounds;
                // keep the layer unclipped so it isn't cropped (default is false —
                // pinned explicitly because the shadow depends on it).
                clip = false
            }
            // Solid offset drop shadow (Farah soft3d / Mizan hard). Drawn behind the key.
            .then(
                if (style.dropShadow != null) {
                    Modifier.drawBehind {
                        val off = 3.dp.toPx()
                        drawRoundRect(
                            color = style.dropShadow,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, off),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(sifr.keyRadius.toPx()),
                        )
                    }
                } else Modifier,
            )
            .clip(shape)
            .background(style.container, shape)
            .then(
                if (style.border != null) Modifier.border(1.dp, style.border, shape) else Modifier,
            )
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // Ghost Arabic-Indic numeral, top-end on number keys (Layl/Farah)
        val digit = action.text?.singleOrNull()
        if (sifr.ghostNumeral != null && action.role == SifrKeyRole.Num && digit != null && digit.isDigit()) {
            Text(
                text = arabicIndicDigit(digit),
                color = sifr.ghostNumeral,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
        if (action.text != null) {
            Text(
                text = action.text,
                fontSize = fontSize,
                fontFamily = sifr.keyFamily,
                fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                color = style.content,
                textAlign = TextAlign.Center,
            )
        } else {
            action.content()
        }
    }
}

private fun arabicIndicDigit(c: Char): String =
    if (c in '0'..'9') ('٠' + (c - '0')).toString() else c.toString()
