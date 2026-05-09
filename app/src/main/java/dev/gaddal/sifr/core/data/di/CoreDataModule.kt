package dev.gaddal.sifr.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dev.gaddal.sifr.core.data.calculator.CalculatorInputBus
import dev.gaddal.sifr.core.data.database.AppDatabase
import dev.gaddal.sifr.feature.history.data.HistoryDao
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "sifr_settings")

val coreDataModule = module {
    // Settings
    single<DataStore<Preferences>> { androidContext().appDataStore }

    // Room
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "sifr.db",
        ).build()
    }
    single<HistoryDao> { get<AppDatabase>().historyDao() }

    // Cross-feature bus (Calculator <- History)
    singleOf(::CalculatorInputBus)
}
