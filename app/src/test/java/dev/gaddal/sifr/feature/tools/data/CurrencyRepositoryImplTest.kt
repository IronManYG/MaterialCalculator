package dev.gaddal.sifr.feature.tools.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.feature.tools.domain.CurrencyError
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryImplTest {

    private val seedSnapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("USD" to 1.0, "SAR" to 3.75),
        asOf = LocalDate.of(2026, 6, 10),
    )
    private val liveSnapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("USD" to 1.0, "SAR" to 3.76, "EUR" to 0.9),
        asOf = LocalDate.of(2026, 6, 14),
    )
    private val seed = SeedRatesProvider { seedSnapshot }

    private class FakeApi(var result: Result<RatesSnapshot, CurrencyError>) : CurrencyApi {
        var calls = 0
        override suspend fun fetchLatest(base: String): Result<RatesSnapshot, CurrencyError> {
            calls++
            return result
        }
    }

    @Test
    fun `first run with empty cache and successful network emits seed then live success`() = runTest {
        val api = FakeApi(Result.Success(liveSnapshot))
        val cache = FakeCache(initial = null)
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 1_000L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            assertThat(awaitItem()).isInstanceOf(RatesResource.SeedFallback::class.java)
            repo.refresh()
            val success = awaitItem() as RatesResource.Success
            assertThat(success.stale).isFalse()
            assertThat(success.snapshot).isEqualTo(liveSnapshot)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(cache.written?.snapshot).isEqualTo(liveSnapshot)
    }

    @Test
    fun `network failure with cache present downgrades to stale success`() = runTest {
        val api = FakeApi(Result.Error(CurrencyError.NO_NETWORK))
        val cache = FakeCache(initial = CacheEntry(liveSnapshot, fetchedAtEpochMs = 0L))
        // now far ahead of fetchedAt so the cache is stale
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 100_000_000L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            val first = awaitItem() as RatesResource.Success
            assertThat(first.snapshot).isEqualTo(liveSnapshot)
            repo.refresh()
            val afterFail = awaitItem() as RatesResource.Success
            assertThat(afterFail.stale).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `network failure with no cache stays on seed fallback`() = runTest {
        val api = FakeApi(Result.Error(CurrencyError.NO_NETWORK))
        val cache = FakeCache(initial = null)
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 1_000L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            assertThat(awaitItem()).isInstanceOf(RatesResource.SeedFallback::class.java)
            repo.refresh()
            // remains seed fallback; no Success emitted
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(api.calls).isEqualTo(1)
    }

    @Test
    fun `fresh cache skips the network unless forced`() = runTest {
        val api = FakeApi(Result.Success(liveSnapshot))
        val cache = FakeCache(initial = CacheEntry(liveSnapshot, fetchedAtEpochMs = 50L))
        // now only slightly ahead of fetchedAt → within freshness window
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 100L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            assertThat(awaitItem()).isInstanceOf(RatesResource.Success::class.java)
            repo.refresh(force = false)
            expectNoEvents()
            assertThat(api.calls).isEqualTo(0)
            repo.refresh(force = true)
            assertThat(awaitItem()).isInstanceOf(RatesResource.Success::class.java)
            assertThat(api.calls).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** In-memory fake of the cache (the real one hits DataStore). */
private class FakeCache(initial: CacheEntry?) : dev.gaddal.sifr.feature.tools.data.RatesCacheContract {
    private var entry: CacheEntry? = initial
    var written: CacheEntry? = null
    override suspend fun read(): CacheEntry? = entry
    override suspend fun write(snapshot: RatesSnapshot, fetchedAtEpochMs: Long) {
        val e = CacheEntry(snapshot, fetchedAtEpochMs)
        entry = e; written = e
    }
}
