package dev.gaddal.sifr.feature.tools.domain

import kotlinx.coroutines.flow.Flow

/**
 * Offline-first currency rates. [rates] serves cache or seed immediately and
 * updates after a successful refresh. [refresh] is safe to call repeatedly;
 * it no-ops the network hit when the cache is fresh unless [force] is set.
 */
interface CurrencyRepository {
    val rates: Flow<RatesResource>
    suspend fun refresh(force: Boolean = false)
}
