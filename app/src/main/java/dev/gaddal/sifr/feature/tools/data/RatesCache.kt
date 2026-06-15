package dev.gaddal.sifr.feature.tools.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** A cached snapshot + when it was fetched (epoch millis), serialized to one pref string. */
@Serializable
private data class CachedRates(
    val base: String,
    val rates: Map<String, Double>,
    val asOfEpochDay: Long,
    val fetchedAtEpochMs: Long,
)

data class CacheEntry(val snapshot: RatesSnapshot, val fetchedAtEpochMs: Long)

interface RatesCacheContract {
    suspend fun read(): CacheEntry?
    suspend fun write(snapshot: RatesSnapshot, fetchedAtEpochMs: Long)
}

class RatesCache(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : RatesCacheContract {
    override suspend fun read(): CacheEntry? {
        val raw = dataStore.data.first()[KEY_RATES_JSON] ?: return null
        return runCatching {
            val c = json.decodeFromString<CachedRates>(raw)
            CacheEntry(
                snapshot = RatesSnapshot(
                    base = c.base,
                    rates = c.rates,
                    asOf = LocalDate.ofEpochDay(c.asOfEpochDay),
                ),
                fetchedAtEpochMs = c.fetchedAtEpochMs,
            )
        }.getOrNull()
    }

    override suspend fun write(snapshot: RatesSnapshot, fetchedAtEpochMs: Long) {
        val payload = json.encodeToString(
            CachedRates(
                base = snapshot.base,
                rates = snapshot.rates,
                asOfEpochDay = snapshot.asOf.toEpochDay(),
                fetchedAtEpochMs = fetchedAtEpochMs,
            ),
        )
        dataStore.edit { it[KEY_RATES_JSON] = payload; it[KEY_RATES_FETCHED_AT] = fetchedAtEpochMs }
    }

    private companion object {
        val KEY_RATES_JSON = stringPreferencesKey("tools_rates_json")
        val KEY_RATES_FETCHED_AT = longPreferencesKey("tools_rates_fetched_at")
    }
}
