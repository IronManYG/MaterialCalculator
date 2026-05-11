package dev.gaddal.sifr.core.domain.settings

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    // Two Phase 2.9 experiments — defaults preserve the pre-2.9 behavior so
    // existing users see no change unless they opt in.
    val showSelectionToolbar: Boolean = true,
    val rangeSelectionEnabled: Boolean = false,
)
