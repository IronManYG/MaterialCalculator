package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Tappable display-font value box. Right-aligned (mirrors naturally in RTL). When [focused]:
 * accent border + a blinking caret bar after the value.
 */
@Composable
fun ToolField(
    value: String,
    focused: Boolean,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (focused) sifr.accent else sifr.hairline

    // rememberInfiniteTransition is called unconditionally (Compose forbids conditional
    // remember*); the result is gated to 0 alpha when unfocused.
    val transition = rememberInfiniteTransition(label = "caret")
    val rawAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    val caretAlpha = if (focused) rawAlpha else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(sifr.background)
            .clickable(role = Role.Button) { onFocus() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value.ifEmpty { placeholder },
            color = if (value.isEmpty()) sifr.dim else sifr.text,
            fontFamily = sifr.displayFamily,
            fontWeight = FontWeight.W300,
            fontSize = 24.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .width(2.dp)
                .height(24.dp)
                .graphicsLayer { alpha = caretAlpha }
                .background(sifr.accent),
        )
    }
}
