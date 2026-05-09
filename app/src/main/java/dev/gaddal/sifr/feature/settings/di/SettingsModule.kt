package dev.gaddal.sifr.feature.settings.di

import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.feature.settings.data.SettingsRepositoryImpl
import dev.gaddal.sifr.feature.settings.ui.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    viewModelOf(::SettingsViewModel)
}
