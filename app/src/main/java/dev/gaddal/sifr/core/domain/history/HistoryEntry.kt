package dev.gaddal.sifr.core.domain.history

data class HistoryEntry(
    val id: Long,
    val expression: String,
    val result: String,
    val timestampMs: Long,
)
