package dev.gaddal.sifr

import android.app.Application
import dev.gaddal.sifr.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModules)
        }
    }
}
