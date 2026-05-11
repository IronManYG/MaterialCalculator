package dev.gaddal.sifr.feature.settings.ui

sealed interface SettingsEvent {
    data object NavigateBack : SettingsEvent

    /** Toggle for haptics just flipped OFF → ON. Demo the haptic so the user
     * feels what they enabled (fires regardless of the just-set value to avoid
     * a DataStore flow-propagation race). */
    data object DemoHaptic : SettingsEvent

    /** Toggle for sound just flipped OFF → ON. Demo the error tone so the user
     * hears what they enabled. */
    data object DemoSound : SettingsEvent
}
