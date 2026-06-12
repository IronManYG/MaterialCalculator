package dev.gaddal.sifr.core.domain.settings

import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val palette: SifrPalette = SifrPalette.Layl,
    val hapticsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val calculatorMode: CalculatorMode = CalculatorMode.Basic,
    val angleUnit: AngleUnit = AngleUnit.Degrees,
    val fractionResults: Boolean = false,   // v1.5 — DISPLAY toggle, default off
    val memoryValue: Double? = null,        // v1.5 — persisted M register (survives force-stop)
)
