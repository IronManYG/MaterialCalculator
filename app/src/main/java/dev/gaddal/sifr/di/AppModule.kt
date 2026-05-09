package dev.gaddal.sifr.di

import dev.gaddal.sifr.feature.calculator.di.calculatorModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    calculatorModule,
)
