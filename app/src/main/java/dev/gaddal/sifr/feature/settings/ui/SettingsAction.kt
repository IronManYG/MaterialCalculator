package dev.gaddal.sifr.feature.settings.ui

import dev.gaddal.sifr.core.domain.settings.AppLanguage
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.domain.settings.RestoreTarget
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetPalette(val palette: SifrPalette) : SettingsAction
    data object ToggleHaptics : SettingsAction
    data object ToggleSound : SettingsAction
    data object ToggleFractionResults : SettingsAction
    data class SetAngleUnit(val unit: AngleUnit) : SettingsAction
    data object BackClicked : SettingsAction
    data class SetKeypadLayout(val layout: KeypadLayout) : SettingsAction
    data object ToggleMemoryKeys : SettingsAction
    data class SetRestoreTarget(val target: RestoreTarget) : SettingsAction
    data class SetLanguage(val language: AppLanguage) : SettingsAction
}
