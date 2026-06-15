package dev.gaddal.sifr.feature.tools.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.components.SifrSegmented
import dev.gaddal.sifr.feature.tools.ui.ToolTab

/**
 * 4-tab segmented control. Wraps [SifrSegmented] typed to [ToolTab] with localized labels.
 */
@Composable
fun ToolTabBar(
    selected: ToolTab,
    onSelect: (ToolTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SifrSegmented(
        options = ToolTab.entries,
        selected = selected,
        label = { tab ->
            when (tab) {
                ToolTab.Units -> stringResource(R.string.tools_tab_units)
                ToolTab.Currency -> stringResource(R.string.tools_tab_currency)
                ToolTab.Tip -> stringResource(R.string.tools_tab_tip)
                ToolTab.Date -> stringResource(R.string.tools_tab_date)
            }
        },
        onSelect = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
    )
}
