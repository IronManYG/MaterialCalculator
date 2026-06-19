package dev.gaddal.sifr.feature.calculator.ui

import androidx.compose.runtime.Composable
import dev.gaddal.sifr.core.ui.theme.SifrKeyRole
import dev.gaddal.sifr.feature.calculator.domain.CalculatorAction

data class CalculatorUiAction(
    val text: String?,
    val role: SifrKeyRole,
    val action: CalculatorAction,
    val content: @Composable () -> Unit = {}
)
