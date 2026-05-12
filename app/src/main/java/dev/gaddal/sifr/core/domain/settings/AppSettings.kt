package dev.gaddal.sifr.core.domain.settings

import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val calculatorMode: CalculatorMode = CalculatorMode.Basic,
    val angleUnit: AngleUnit = AngleUnit.Degrees,
)
