package dev.gaddal.sifr.core.data.settings

import dev.gaddal.sifr.core.domain.settings.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(transform: AppSettings.() -> AppSettings)
}
