package dev.gaddal.sifr.feature.history.data

import dev.gaddal.sifr.core.domain.history.HistoryEntry

fun HistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
    id = id,
    expression = expression,
    result = result,
    timestampMs = timestampMs,
)
