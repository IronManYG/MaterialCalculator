package dev.gaddal.sifr.feature.calculator.di

import dev.gaddal.sifr.feature.calculator.domain.ExpressionWriter
import dev.gaddal.sifr.feature.calculator.ui.CalculatorViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val calculatorModule = module {
    factoryOf(::ExpressionWriter)
    viewModelOf(::CalculatorViewModel)
}
