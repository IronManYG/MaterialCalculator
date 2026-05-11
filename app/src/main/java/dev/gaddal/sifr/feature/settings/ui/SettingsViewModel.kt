package dev.gaddal.sifr.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gaddal.sifr.core.data.settings.SettingsRepository
import dev.gaddal.sifr.core.domain.settings.AppSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { settings ->
                _state.update { it.copy(settings = settings, isLoading = false) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> updateSettings { copy(themeMode = action.mode) }
            SettingsAction.ToggleHaptics -> {
                val wasEnabled = _state.value.settings.hapticsEnabled
                updateSettings { copy(hapticsEnabled = !hapticsEnabled) }
                if (!wasEnabled) emit(SettingsEvent.DemoHaptic)
            }
            SettingsAction.ToggleSound -> {
                val wasEnabled = _state.value.settings.soundEnabled
                updateSettings { copy(soundEnabled = !soundEnabled) }
                if (!wasEnabled) emit(SettingsEvent.DemoSound)
            }
            SettingsAction.BackClicked -> emit(SettingsEvent.NavigateBack)
            SettingsAction.HapticsTestClicked -> emit(SettingsEvent.NavigateToHapticsTest)
        }
    }

    private fun updateSettings(transform: AppSettings.() -> AppSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    private fun emit(event: SettingsEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}
