package dev.gaddal.sifr.feature.calculator.domain

import dev.gaddal.sifr.core.domain.util.Error

enum class CalcError : Error {
    DIVISION_BY_ZERO,
    INVALID_EXPRESSION,
}
