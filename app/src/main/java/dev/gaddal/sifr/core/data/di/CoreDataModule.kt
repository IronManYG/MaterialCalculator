package dev.gaddal.sifr.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "sifr_settings")

val coreDataModule = module {
    single<DataStore<Preferences>> { androidContext().appDataStore }
}
