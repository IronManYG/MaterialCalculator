package dev.gaddal.sifr.core.domain.settings

/**
 * Selectable keypad arrangement. PascalCase to match SifrPalette / CalculatorMode.
 * Tape is reserved for v1.7 — it stays in the enum for forward-compat but is not
 * offered by the Settings picker yet, and renders as Classic if ever set.
 */
enum class KeypadLayout { Classic, Remix, Arc, Tape }
