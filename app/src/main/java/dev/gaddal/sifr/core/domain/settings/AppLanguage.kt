package dev.gaddal.sifr.core.domain.settings

/**
 * In-app UI language. [tag] is a BCP-47 language tag fed to [java.util.Locale];
 * `null` means "follow the device locale". PascalCase to match [SifrPalette] / ThemeMode.
 * Adding a language = one entry here + a `values-<tag>/strings.xml` (the Settings list
 * grows automatically). See docs/superpowers/specs/2026-06-14-v1.8-language-design.md §11.
 */
enum class AppLanguage(val tag: String?) {
    System(null),
    English("en"),
    Arabic("ar"),
}
