package dev.gaddal.sifr.feature.tools.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * open.er-api.com `/v6/latest/USD` response. `result` is "success" or "error";
 * `time_last_update_unix` is the rate epoch (seconds); `rates` is per-USD.
 */
@Serializable
data class RatesDto(
    val result: String,
    @SerialName("base_code") val baseCode: String? = null,
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long = 0L,
    val rates: Map<String, Double> = emptyMap(),
)
