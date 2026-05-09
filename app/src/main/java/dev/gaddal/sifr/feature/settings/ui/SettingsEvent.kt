package dev.gaddal.sifr.feature.settings.ui

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent
}
