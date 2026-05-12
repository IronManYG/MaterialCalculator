package dev.gaddal.sifr.feature.calculator.ui

import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.util.UiText
import dev.gaddal.sifr.feature.calculator.domain.CalcError

fun CalcError.toUiText(): UiText = when (this) {
    CalcError.DIVISION_BY_ZERO -> UiText.StringResource(R.string.calc_error_division_by_zero)
    CalcError.INVALID_EXPRESSION -> UiText.StringResource(R.string.calc_error_invalid_expression)
    CalcError.DOMAIN_ERROR -> UiText.StringResource(R.string.calc_error_domain_error)
    CalcError.OVERFLOW -> UiText.StringResource(R.string.calc_error_overflow)
    CalcError.SYNTAX_ERROR -> UiText.StringResource(R.string.calc_error_syntax_error)
}
