package dev.gaddal.sifr.core.data.history

import dev.gaddal.sifr.core.domain.history.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observe(): Flow<List<HistoryEntry>>
    suspend fun add(expression: String, result: String)
    suspend fun delete(id: Long)
    suspend fun clear()
}
