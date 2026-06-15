package dev.gaddal.sifr.feature.tools.ui

/**
 * The four Tools tabs, in display order. [ordinal] is used for SavedStateHandle persistence.
 */
enum class ToolTab {
    Units,
    Currency,
    Tip,
    Date,
    ;

    companion object {
        fun fromOrdinal(i: Int): ToolTab = entries.getOrElse(i) { Units }
    }
}
