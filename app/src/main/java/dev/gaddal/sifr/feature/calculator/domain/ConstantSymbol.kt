package dev.gaddal.sifr.feature.calculator.domain

enum class ConstantSymbol(val symbol: Char, val value: Double) {
    PI('π', kotlin.math.PI),
    E('e', kotlin.math.E),
}
