package dev.gaddal.sifr.feature.tools.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import java.time.LocalDate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.components.SifrSubScreenTopBar
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.feature.tools.ui.components.ToolNumPad
import dev.gaddal.sifr.feature.tools.ui.components.ToolTabBar

@Composable
fun ToolsScreenLandscape(
    state: ToolsState,
    onAction: (ToolsAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = true,
) {
    val sifr = SifrTokens.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .displayCutoutPadding(),
            ) {
                SifrSubScreenTopBar(
                    title = stringResource(R.string.tools_title),
                    onBack = { onAction(ToolsAction.BackClicked) },
                    onRotate = onRotate,
                    rotateActive = rotateActive,
                    rotateCd = stringResource(R.string.calc_rotate_orientation),
                )
            }
        },
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .displayCutoutPadding(),
        ) {
            // Left = the tool column. The tab bar lives here (not in the top bar) so it
            // spans only the tool width, not the numpad column on the trailing edge.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                ToolTabBar(
                    selected = state.activeTab,
                    onSelect = { onAction(ToolsAction.SelectTab(it)) },
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (state.activeTab) {
                        ToolTab.Units -> UnitsCard(state, onAction)
                        ToolTab.Currency -> CurrencyCard(state, onAction)
                        ToolTab.Tip -> TipCard(state, onAction)
                        ToolTab.Date -> DateCardsLandscape(state, onAction)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (state.activeTab != ToolTab.Date || state.focusedField == FocusedField.AddDays) {
                ToolNumPad(
                    onNumKey = { onAction(ToolsAction.NumKey(it)) },
                    onBackspace = { onAction(ToolsAction.Backspace) },
                    compact = true,
                    modifier = Modifier
                        .width(300.dp)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun DateCardsLandscape(state: ToolsState, onAction: (ToolsAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CalendarToggle(state.calendar, onAction)
        // Two date cards side by side in landscape. The Row fills width so the weights split evenly.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DateDiffCard(state, onAction, modifier = Modifier.weight(1f))
            DateAddCard(state, onAction, modifier = Modifier.weight(1f))
        }
    }
}

// ── Previews ────────────────────────────────────────────────────────────────
@Preview(name = "ToolsScreenLandscape — Units (Layl dark)", widthDp = 800, heightDp = 360)
@Composable
private fun PreviewToolsLandscapeUnits() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    ToolsScreenLandscape(
        state = ToolsState(activeTab = ToolTab.Units, uVal = "1", uResult = "1000"),
        onAction = {},
    )
}

@Preview(name = "ToolsScreenLandscape — Date (Bayan light)", widthDp = 800, heightDp = 360)
@Composable
private fun PreviewToolsLandscapeDate() = SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
    ToolsScreenLandscape(
        state = ToolsState(
            activeTab = ToolTab.Date,
            date1 = LocalDate.of(2026, 1, 1),
            date2 = LocalDate.of(2026, 12, 31),
            diffDays = 364,
            diffWeeks = 52,
            diffRemainingDays = 0,
        ),
        onAction = {},
    )
}
