package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * `−  value  +` pill (Tip split count). [min] defaults to 1. The −/+ are [IconButton]s so the
 * touch target stays 48×48dp.
 */
@Composable
fun Stepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
) {
    val sifr = SifrTokens.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier.border(1.dp, sifr.hairline, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrement, enabled = value > min) {
            Text("−", color = if (value > min) sifr.text else sifr.dim, fontSize = 18.sp)
        }
        Text(
            text = value.toString(),
            color = sifr.text,
            fontFamily = sifr.displayFamily,
            fontWeight = FontWeight.W400,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 36.dp)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onIncrement) {
            Text("+", color = sifr.text, fontSize = 18.sp)
        }
    }
}
