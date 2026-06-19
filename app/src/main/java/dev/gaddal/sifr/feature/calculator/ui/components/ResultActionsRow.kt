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
 * Post-`=` result actions: COPY · SHARE · ANS→ (spec §5.1 / D2). ANS→ commits the
 * just-evaluated result as the editable working expression (clears justEvaluated),
 * letting the user continue from the answer.
 */
@Composable
fun ResultActionsRow(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onAns: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SifrChip(label = stringResource(R.string.calc_action_copy), onClick = onCopy)
        SifrChip(label = stringResource(R.string.calc_action_share), onClick = onShare)
        SifrChip(label = stringResource(R.string.calc_action_ans), onClick = onAns)
    }
}

@Preview(name = "ResultActionsRow — Layl dark", showBackground = true)
@Composable
private fun PreviewResultActionsRow() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    ResultActionsRow(
        onCopy = {},
        onShare = {},
        onAns = {},
        modifier = Modifier.padding(12.dp),
    )
}
