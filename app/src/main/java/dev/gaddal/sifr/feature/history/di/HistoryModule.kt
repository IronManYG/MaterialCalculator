package dev.gaddal.sifr.feature.history.di

import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.feature.history.data.HistoryRepositoryImpl
import dev.gaddal.sifr.feature.history.ui.HistoryViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val historyModule = module {
    singleOf(::HistoryRepositoryImpl) { bind<HistoryRepository>() }
    viewModelOf(::HistoryViewModel)
}
