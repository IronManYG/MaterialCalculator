package dev.gaddal.sifr.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
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
            prefs[KEY_PALETTE] = updated.palette.name
            prefs[KEY_HAPTICS] = updated.hapticsEnabled
            prefs[KEY_SOUND] = updated.soundEnabled
            prefs[KEY_CALC_MODE] = updated.calculatorMode.name
            prefs[KEY_ANGLE_UNIT] = updated.angleUnit.name
            prefs[KEY_FRACTION_RESULTS] = updated.fractionResults
            prefs[KEY_KEYPAD_LAYOUT] = updated.keypadLayout.name
            prefs[KEY_MEMORY_KEYS] = updated.memoryKeysVisible
            val memory = updated.memoryValue
            if (memory != null) prefs[KEY_MEMORY_VALUE] = memory
            else prefs.remove(KEY_MEMORY_VALUE)
        }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        themeMode = this[KEY_THEME_MODE]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.System,
        palette = this[KEY_PALETTE]
            ?.let { runCatching { SifrPalette.valueOf(it) }.getOrNull() }
            ?: SifrPalette.Layl,
        hapticsEnabled = this[KEY_HAPTICS] ?: true,
        soundEnabled = this[KEY_SOUND] ?: false,
        calculatorMode = this[KEY_CALC_MODE]
            ?.let { runCatching { CalculatorMode.valueOf(it) }.getOrNull() }
            ?: CalculatorMode.Basic,
        angleUnit = this[KEY_ANGLE_UNIT]
            ?.let { runCatching { AngleUnit.valueOf(it) }.getOrNull() }
            ?: AngleUnit.Degrees,
        fractionResults = this[KEY_FRACTION_RESULTS] ?: false,
        keypadLayout = this[KEY_KEYPAD_LAYOUT]
            ?.let { runCatching { KeypadLayout.valueOf(it) }.getOrNull() }
            ?: KeypadLayout.Classic,
        memoryKeysVisible = this[KEY_MEMORY_KEYS] ?: true,
        memoryValue = this[KEY_MEMORY_VALUE],
    )

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_PALETTE = stringPreferencesKey("palette")
        val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_CALC_MODE = stringPreferencesKey("calculator_mode")
        val KEY_ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val KEY_FRACTION_RESULTS = booleanPreferencesKey("fraction_results")
        val KEY_KEYPAD_LAYOUT = stringPreferencesKey("keypad_layout")
        val KEY_MEMORY_KEYS = booleanPreferencesKey("memory_keys_visible")
        val KEY_MEMORY_VALUE = doublePreferencesKey("memory_value")
    }
}
