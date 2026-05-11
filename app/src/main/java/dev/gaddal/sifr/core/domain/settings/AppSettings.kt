package dev.gaddal.sifr.core.domain.settings

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
)
