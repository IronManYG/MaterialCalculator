package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * Labelled output. [big] renders the value in accent at 28sp (headline result); otherwise
 * text color at 20sp. Label is always dim, 11sp. Right-aligned to match [ToolField].
 */
@Composable
fun ToolOut(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    big: Boolean = false,
) {
    val sifr = SifrTokens.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            color = sifr.dim,
            fontFamily = sifr.uiFamily,
            fontSize = 11.sp,
        )
        Text(
            text = value.ifEmpty { "—" },
            color = if (big) sifr.accent else sifr.text,
            fontFamily = sifr.displayFamily,
            fontWeight = if (big) FontWeight.W400 else FontWeight.W300,
            fontSize = if (big) 28.sp else 20.sp,
        )
    }
}
