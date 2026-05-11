package dev.gaddal.sifr.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import dev.gaddal.sifr.core.ui.feedback.rememberFeedbackController
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoot(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedback = rememberFeedbackController(
        hapticsEnabled = state.settings.hapticsEnabled,
        soundEnabled = state.settings.soundEnabled,
    )
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onNavigateBack()
            SettingsEvent.DemoHaptic -> feedback.playHaptic(FeedbackIntent.Selection)
            SettingsEvent.DemoSound -> feedback.playSound(FeedbackIntent.Error)
        }
    }
    SettingsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeSection(
                selected = state.settings.themeMode,
                onSelect = { onAction(SettingsAction.SetThemeMode(it)) },
            )
            HorizontalDivider()
            SwitchRow(
                label = stringResource(R.string.settings_haptics),
                checked = state.settings.hapticsEnabled,
                onCheckedChange = { onAction(SettingsAction.ToggleHaptics) },
            )
            SwitchRow(
                label = stringResource(R.string.settings_sound),
                checked = state.settings.soundEnabled,
                onCheckedChange = { onAction(SettingsAction.ToggleSound) },
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_editing_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            SwitchRow(
                label = stringResource(R.string.settings_selection_toolbar),
                description = stringResource(R.string.settings_selection_toolbar_desc),
                checked = state.settings.showSelectionToolbar,
                onCheckedChange = { onAction(SettingsAction.ToggleSelectionToolbar) },
            )
            SwitchRow(
                label = stringResource(R.string.settings_range_selection),
                description = stringResource(R.string.settings_range_selection_desc),
                checked = state.settings.rangeSelectionEnabled,
                onCheckedChange = { onAction(SettingsAction.ToggleRangeSelection) },
            )
        }
    }
}

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_theme_section),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    ThemeMode.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    role = Role.RadioButton,
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = mode == selected, onClick = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = stringResource(mode.labelRes()))
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SifrTheme {
        SettingsScreen(
            state = SettingsState(isLoading = false),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SettingsRootPreview() {
    SifrTheme {
        SettingsScreen(state = SettingsState(isLoading = false), onAction = {})
    }
}
