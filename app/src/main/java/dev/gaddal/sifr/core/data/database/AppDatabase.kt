package dev.gaddal.sifr.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.gaddal.sifr.feature.history.data.HistoryDao
import dev.gaddal.sifr.feature.history.data.HistoryEntity

@Database(
    entities = [HistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
