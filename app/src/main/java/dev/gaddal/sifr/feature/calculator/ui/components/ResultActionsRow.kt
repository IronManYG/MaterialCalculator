package dev.gaddal.sifr.feature.calculator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.components.SifrChip
import dev.gaddal.sifr.core.ui.theme.SifrTheme

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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SifrChip(label = stringResource(R.string.calc_action_copy), onClick = onCopy)
        SifrChip(label = stringResource(R.string.calc_action_share), onClick = onShare)
    }
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
