package dev.gaddal.sifr.feature.settings.ui

import dev.gaddal.sifr.core.domain.settings.ThemeMode

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data object ToggleHaptics : SettingsAction
    data object ToggleSound : SettingsAction
    data object BackClicked : SettingsAction
}
