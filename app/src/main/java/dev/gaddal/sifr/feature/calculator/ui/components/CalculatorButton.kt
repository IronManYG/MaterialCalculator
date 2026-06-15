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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.theme.buildGlowBitmap
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction
import dev.gaddal.sifr.feature.calculator.ui.CalculatorUiAction

// Layl spec: accent halo radius
private val EqGlowBlurRadius = 22.dp

@Composable
fun CalculatorButton(
    action: CalculatorUiAction,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    shape: Shape? = null,
    onClick: () -> Unit,
) {
    val sifr = SifrTokens.colors
    val style = sifr.keyStyle(action.role)
    val keyShape = shape ?: RoundedCornerShape(sifr.keyRadius)
    val isCircle = keyShape == CircleShape

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScaleState = animateFloatAsState(
        targetValue = if (pressed && !sifr.raisedKeys) 0.94f else 1f,
        animationSpec = tween(90), label = "keyScale",
    )
    val pressTranslateYState = animateFloatAsState(
        targetValue = if (pressed && sifr.raisedKeys) 2f else 0f,
        animationSpec = tween(90), label = "keyTranslate",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScaleState.value
                scaleY = pressScaleState.value
                translationY = pressTranslateYState.value * density
                // The drawBehind drop shadow overdraws 3dp below the key bounds;
                // keep the layer unclipped so it isn't cropped (default is false —
                // pinned explicitly because the shadow depends on it).
                clip = false
            }
            // Layl: blurred accent glow behind the = key. Rendered into a software
            // bitmap so BlurMaskFilter works on minSdk 24 (hardware-accelerated
            // DrawScope silently drops BlurMaskFilter below API 28). clip=false on
            // the graphicsLayer above means the overflowing glow is not cropped.
            .then(
                if (action.role == SifrKeyRole.Eq && sifr.eqGlow != null) {
                    val glowColor = sifr.eqGlow
                    val radiusDp = sifr.keyRadius
                    Modifier.drawWithCache {
                        val blurPx = EqGlowBlurRadius.toPx()
                        val pad = blurPx * 2f
                        val glow = buildGlowBitmap(
                            widthPx = size.width.toInt(),
                            heightPx = size.height.toInt(),
                            cornerRadiusPx = if (isCircle) size.minDimension / 2f else radiusDp.toPx(),
                            blurRadiusPx = blurPx,
                            colorArgb = glowColor.toArgb(),
                        ).asImageBitmap()
                        onDrawBehind {
                            drawImage(image = glow, topLeft = Offset(-pad, -pad))
                        }
                    }
                } else Modifier,
            )
            // Solid offset drop shadow (Farah soft3d / Mizan hard). Drawn behind the key.
            .then(
                if (style.dropShadow != null) {
                    Modifier.drawBehind {
                        val off = 3.dp.toPx()
                        drawRoundRect(
                            color = style.dropShadow,
                            topLeft = Offset(0f, off),
                            size = size,
                            cornerRadius = CornerRadius(
                                if (isCircle) size.minDimension / 2f else sifr.keyRadius.toPx(),
                            ),
                        )
                    }
                } else Modifier,
            )
            .clip(keyShape)
            .background(style.container, keyShape)
            // Mizan: 1px inner-top highlight line. Drawn over the key background so
            // it appears inside the top edge of the rounded rect.
            .then(
                if (style.innerTopHighlight != null) {
                    val hi = style.innerTopHighlight
                    Modifier.drawWithContent {
                        drawContent()
                        val inset = 1.dp.toPx()
                        drawLine(
                            color = hi,
                            start = Offset(inset, inset),
                            end = Offset(size.width - inset, inset),
                            strokeWidth = inset,
                        )
                    }
                } else Modifier,
            )
            .then(
                if (style.border != null) Modifier.border(1.dp, style.border, keyShape) else Modifier,
            )
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
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

@Preview(name = "Eq glow (Layl dark)", showBackground = true, backgroundColor = 0xFF070A12)
@Composable
private fun PreviewEqGlow() = SifrTheme(
    palette = SifrPalette.Layl,
    themeMode = ThemeMode.Dark,
) {
    Box(modifier = Modifier.padding(40.dp)) {
        CalculatorButton(
            action = CalculatorUiAction("=", SifrKeyRole.Eq, CalculatorAction.Calculate),
            modifier = Modifier.size(72.dp),
            onClick = {},
        )
    }
}

@Preview(name = "Mizan key highlight (dark)", showBackground = true, backgroundColor = 0xFF111110)
@Composable
private fun PreviewMizanKey() = SifrTheme(
    palette = SifrPalette.Mizan,
    themeMode = ThemeMode.Dark,
) {
    Box(modifier = Modifier.padding(40.dp)) {
        CalculatorButton(
            action = CalculatorUiAction("7", SifrKeyRole.Num, CalculatorAction.Number(7)),
            modifier = Modifier.size(72.dp),
            onClick = {},
        )
    }
}
