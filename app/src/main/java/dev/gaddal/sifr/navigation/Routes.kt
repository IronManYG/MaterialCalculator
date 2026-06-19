package dev.gaddal.sifr.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object CalculatorRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object HistoryRoute : NavKey

@Serializable
data object ToolsRoute : NavKey
