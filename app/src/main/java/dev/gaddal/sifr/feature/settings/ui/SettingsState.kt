package dev.gaddal.sifr.feature.settings.ui

import dev.gaddal.sifr.core.domain.settings.AppSettings

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
)
