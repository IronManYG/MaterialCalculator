package dev.gaddal.sifr.feature.tools.di

import dev.gaddal.sifr.Constants
import dev.gaddal.sifr.feature.tools.data.AssetSeedRatesProvider
import dev.gaddal.sifr.feature.tools.data.CurrencyApi
import dev.gaddal.sifr.feature.tools.data.CurrencyRepositoryImpl
import dev.gaddal.sifr.feature.tools.data.KtorCurrencyApi
import dev.gaddal.sifr.feature.tools.data.RatesCache
import dev.gaddal.sifr.feature.tools.data.RatesCacheContract
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import dev.gaddal.sifr.feature.tools.ui.ToolsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val toolsModule = module {
    single<Json> { Json { ignoreUnknownKeys = true } }

    single<HttpClient> {
        HttpClient(Android) {
            install(ContentNegotiation) { json(get<Json>()) }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
            }
        }
    }

    single<CurrencyApi> { KtorCurrencyApi(client = get(), baseUrl = Constants.BASE_URL) }
    single<RatesCacheContract> { RatesCache(dataStore = get(), json = get()) }
    single<SeedRatesProvider> { AssetSeedRatesProvider(context = androidContext(), json = get()) }
    single<CurrencyRepository> { CurrencyRepositoryImpl(api = get(), cache = get(), seed = get()) }
    viewModelOf(::ToolsViewModel)
}
