package dev.gaddal.sifr.feature.history.data

import dev.gaddal.sifr.core.data.history.HistoryRepository
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val dao: HistoryDao,
) : HistoryRepository {

    override fun observe(): Flow<List<HistoryEntry>> =
        dao.observe().map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(expression: String, result: String) {
        dao.insert(
            HistoryEntity(
                expression = expression,
                result = result,
                timestampMs = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.clear()
}
