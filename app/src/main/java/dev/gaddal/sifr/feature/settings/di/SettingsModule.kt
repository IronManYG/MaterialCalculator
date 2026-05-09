package dev.gaddal.sifr.feature.settings.di

import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.feature.settings.data.SettingsRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    // viewModelOf(::SettingsViewModel) — added in Task 6 alongside the VM file
}
