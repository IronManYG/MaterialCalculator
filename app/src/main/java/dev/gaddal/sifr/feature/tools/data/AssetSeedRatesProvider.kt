package dev.gaddal.sifr.feature.tools.data

import android.content.Context
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
private data class SeedDto(val base: String, val asOf: String, val rates: Map<String, Double>)

/** Reads the bundled `assets/currency_seed.json` once and caches the parse. */
class AssetSeedRatesProvider(
    private val context: Context,
    private val json: Json,
) : SeedRatesProvider {

    private val cached: RatesSnapshot by lazy {
        val raw = context.assets.open("currency_seed.json").bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<SeedDto>(raw)
        RatesSnapshot(base = dto.base, rates = dto.rates, asOf = LocalDate.parse(dto.asOf))
    }

    override fun seed(): RatesSnapshot = cached
}
