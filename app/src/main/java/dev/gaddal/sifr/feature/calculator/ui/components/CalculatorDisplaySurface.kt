package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Wraps the calculator display in the active theme's container treatment:
 *  - Bayan -> solid ink block + faint ٥ watermark (no corner radius)
 *  - Farah -> floating card (radius 26, drop shadow)
 *  - Mizan -> recessed inset (radius 14, drawn inner-shadow cue)
 *  - Layl / Raqim -> flat (no container)
 * The display colors (displayExpression / displayResult) are already tuned to
 * the on-container values in SifrPalettes for the themes that render a container.
 */
@Composable
fun CalculatorDisplaySurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val sifr = SifrTokens.colors
    when {
        sifr.displayBlock != null -> Box(
            modifier = modifier
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .background(sifr.displayBlock)
                .padding(20.dp),
        ) {
            Text(
                text = "٥",
                color = sifr.displayExpression.copy(alpha = 0.06f),
                fontFamily = sifr.displayFamily,
                fontSize = 150.sp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            content()
        }

        sifr.displayCard != null -> Box(
            modifier = modifier
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp), clip = false)
                .clip(RoundedCornerShape(26.dp))
                .background(sifr.displayCard)
                .padding(20.dp),
            content = content,
        )

        sifr.displayInset != null -> Box(
            modifier = modifier
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(sifr.displayInset)
                .drawWithContent {
                    drawContent()
                    val w = size.width
                    val h = size.height
                    drawLine(
                        color = Color.Black.copy(alpha = 0.25f),
                        start = Offset(0f, 1.dp.toPx()),
                        end = Offset(w, 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(0f, h - 1.dp.toPx()),
                        end = Offset(w, h - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(18.dp),
            content = content,
        )

        else -> Box(modifier = modifier, content = content)
    }
}
