package dev.gaddal.sifr.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observe(): Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toAppSettings() }

    override suspend fun update(transform: AppSettings.() -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = prefs.toAppSettings().transform()
            prefs[KEY_THEME_MODE] = updated.themeMode.name
            prefs[KEY_HAPTICS] = updated.hapticsEnabled
            prefs[KEY_SOUND] = updated.soundEnabled
        }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        themeMode = this[KEY_THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System,
        hapticsEnabled = this[KEY_HAPTICS] ?: true,
        soundEnabled = this[KEY_SOUND] ?: false,
    )

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
    }
}
