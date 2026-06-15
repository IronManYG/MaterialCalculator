package dev.gaddal.sifr.feature.tools.data

import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CurrencyRepositoryImpl(
    private val api: CurrencyApi,
    private val cache: RatesCacheContract,
    private val seed: SeedRatesProvider,
    private val freshnessWindowMs: Long = 12 * 60 * 60 * 1000L, // 12h
    private val now: () -> Long = { System.currentTimeMillis() },
) : CurrencyRepository {

    /**
     * Backing shared flow with replay=1 so:
     *  - new subscribers always get the latest resource immediately, and
     *  - every [emit] is delivered even when the value is structurally equal to the
     *    previous one (unlike StateFlow which deduplicates). This matters when a
     *    force-refresh returns identical rates data: the collector must still receive
     *    the updated emission.
     */
    private val _rates = MutableSharedFlow<RatesResource>(replay = 1)

    private val mutex = Mutex()
    private var loadedInitial = false

    /**
     * Each subscriber sees:
     *   1. [RatesResource.Loading] immediately (emitted eagerly before any suspension).
     *   2. Cache or seed snapshot (served by [serveInitial] before the upstream replay).
     *   3. All subsequent updates pushed by [refresh].
     *
     * A cold [flow] wrapper is used so that [serveInitial] is triggered on collection,
     * guaranteeing the Loading → seed/cache transition without requiring an external scope.
     */
    override val rates: Flow<RatesResource> = flow {
        emit(RatesResource.Loading)
        serveInitial()
        emitAll(_rates)
    }

    /** Reads the cache (or falls back to seed) exactly once, guarded by [mutex]. */
    private suspend fun serveInitial() = mutex.withLock { loadInitialLocked() }

    /**
     * Reads the cache (or seed) exactly once and emits the result. Single source of
     * truth for the initial serve, shared by [serveInitial] and [refresh].
     * **The caller must already hold [mutex]** — this does not lock.
     */
    private suspend fun loadInitialLocked() {
        if (loadedInitial) return
        loadedInitial = true
        val cached = cache.read()
        if (cached != null) {
            val stale = now() - cached.fetchedAtEpochMs > freshnessWindowMs
            _rates.emit(RatesResource.Success(cached.snapshot, stale = stale))
        } else {
            _rates.emit(RatesResource.SeedFallback(seed.seed()))
        }
    }

    override suspend fun refresh(force: Boolean) = mutex.withLock {
        // Cover the case where refresh() races ahead of any subscriber's serveInitial().
        loadInitialLocked()

        // Skip network if cache is fresh and caller doesn't force a refresh.
        val current = _rates.replayCache.firstOrNull()
        val haveFreshCache = current is RatesResource.Success && !current.stale
        if (haveFreshCache && !force) return@withLock

        when (val result = api.fetchLatest()) {
            is Result.Success -> {
                val fetchedAt = now()
                cache.write(result.data, fetchedAt)
                _rates.emit(RatesResource.Success(result.data, stale = false))
            }
            is Result.Error -> {
                // On failure, downgrade Success to stale=true. SeedFallback and Loading
                // have no meaningful downgrade — emit nothing to avoid spurious events.
                if (current is RatesResource.Success) {
                    _rates.emit(current.copy(stale = true))
                }
            }
        }
    }
}
