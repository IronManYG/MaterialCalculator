package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens

/**
 * A row of hairline-bordered chips shown bottom-end in the display surface
 * right after a successful '=' evaluation (`justEvaluated && error == null`).
 *
 * Ships COPY and SHARE only. ANS was evaluated and dropped: after '=' the
 * expression IS already the result, so RestoreExpression(result) would write
 * back the identical string — a pure no-op under the no-engine-changes
 * constraint. No UseAnswer action or calc_action_ans string is added.
 */
@Composable
fun ResultActionsRow(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(
            label = stringResource(R.string.calc_action_copy),
            content = sifr.dim,
            border = sifr.hairline,
            onClick = onCopy,
        )
        Chip(
            label = stringResource(R.string.calc_action_share),
            content = sifr.dim,
            border = sifr.hairline,
            onClick = onShare,
        )
    }
}

@Composable
private fun Chip(
    label: String,
    content: Color,
    border: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Text(
        text = label,
        color = content,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(shape)
            .border(BorderStroke(1.dp, border), shape)
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Preview(name = "ResultActionsRow — Layl dark", showBackground = true)
@Composable
private fun PreviewResultActionsRow() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    ResultActionsRow(
        onCopy = {},
        onShare = {},
        modifier = Modifier.padding(12.dp),
    )
}
