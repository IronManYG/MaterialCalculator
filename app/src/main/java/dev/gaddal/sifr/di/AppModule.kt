package dev.gaddal.sifr.di

import dev.gaddal.sifr.core.data.di.coreDataModule
import dev.gaddal.sifr.feature.calculator.di.calculatorModule
import dev.gaddal.sifr.feature.history.di.historyModule
import dev.gaddal.sifr.feature.settings.di.settingsModule
import dev.gaddal.sifr.feature.tools.di.toolsModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    coreDataModule,
    calculatorModule,
    settingsModule,
    historyModule,
    toolsModule,
)
