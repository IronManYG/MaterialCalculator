package dev.gaddal.sifr.feature.tools.domain

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.time.LocalDate

/**
 * A set of exchange rates expressed per one unit of [base] (always "USD" from
 * open.er-api). [asOf] is the provider's last-update date. Convert A→B as
 * value / rates[A] * rates[B].
 */
@Immutable
data class RatesSnapshot(
    val base: String,
    val rates: Map<String, Double>,
    val asOf: LocalDate,
) {
    /** Currency codes available in this snapshot, sorted alphabetically. */
    val currencies: List<String> get() = rates.keys.sorted()
}

/**
 * The currency rates as seen by the UI. Conversion never blocks: failures
 * downgrade to cached ([Success] with stale=true) or bundled ([SeedFallback]).
 */
@Stable
sealed interface RatesResource {
    data object Loading : RatesResource
    data class Success(val snapshot: RatesSnapshot, val stale: Boolean) : RatesResource
    data class SeedFallback(val snapshot: RatesSnapshot) : RatesResource

    /** The snapshot to convert with, if any (Loading has none). */
    val snapshotOrNull: RatesSnapshot?
        get() = when (this) {
            is Success -> snapshot
            is SeedFallback -> snapshot
            Loading -> null
        }
}
