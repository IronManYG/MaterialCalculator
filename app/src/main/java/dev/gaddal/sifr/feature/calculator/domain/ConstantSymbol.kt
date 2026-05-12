package dev.gaddal.sifr.feature.calculator.domain

import kotlin.math.E
import kotlin.math.PI

enum class ConstantSymbol(val symbol: Char, val value: Double) {
    PI('π', PI),
    E('e', E),
}
