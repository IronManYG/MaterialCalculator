package dev.gaddal.sifr.marketing

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.gaddal.sifr.core.domain.history.HistoryEntry
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.calculator.domain.CalculatorMode
import dev.gaddal.sifr.feature.calculator.ui.CalculatorScreen
import dev.gaddal.sifr.feature.calculator.ui.CalculatorState
import dev.gaddal.sifr.feature.calculator.ui.components.CalculatorScreenLandscape
import dev.gaddal.sifr.feature.history.ui.HistoryScreen
import dev.gaddal.sifr.feature.history.ui.HistoryState
import dev.gaddal.sifr.feature.settings.ui.SettingsScreen
import dev.gaddal.sifr.feature.settings.ui.SettingsState

/**
 * Play Store marketing-asset previews.
 *
 * Each preview is sized for Play Console phone screenshots: portrait
 * shots at 360×800 dp render at 1080×2400 px on Studio's default
 * xxhdpi (3x) density; landscape at 800×360 dp renders at 2400×1080
 * px. Both fit comfortably inside Play's recommended phone-screenshot
 * dimensions (min 320 px, max 3840 px on the long edge, aspect 16:9
 * to 9:16).
 *
 * Each surface is rendered in:
 *   - both **light** and **dark** themes (Sifr defaults to follow-system,
 *     so both must be in the listing), and
 *   - both **English** and **Arabic** locales (separate Play locale
 *     listings can carry their own screenshot set).
 *
 * **Cursor field intentionally omitted on most previews.** A visible
 * editable cursor in a marketing shot reads as "screenshot of an
 * editor", not "calculator at rest". One Basic-mode preview keeps
 * the cursor mid-expression to showcase the editable-cursor feature;
 * everything else uses `CalculatorState`'s default cursor at 0 (where
 * BasicTextField does not paint a caret without focus).
 *
 * Export workflow (Android Studio):
 *   1. Open this file with the preview pane visible.
 *   2. Wait for all previews to render.
 *   3. Right-click a preview → "Save Screenshot…" → choose target dir.
 *   4. PNGs land at the rendered density (1080-wide for 360 dp portrait,
 *      2400-wide for 800 dp landscape) — ready for Play Console upload.
 *
 * These previews live in `app/src/debug/` — they're annotated with
 * `@Preview` only and have no runtime call path, so they're dead
 * code in any non-debug build. Debug-only also keeps the `marketing`
 * package out of the release APK/AAB regardless of R8 settings.
 */

// ---- Portrait: Basic mode ----

@Preview(
    name = "Marketing — Basic, EN, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun MarketingBasicEnLight() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "12+5",
                livePreview = "17",
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Basic, EN, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingBasicEnDark() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "12+5",
                livePreview = "17",
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Basic, AR, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
)
@Composable
private fun MarketingBasicArLight() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "12+5",
                livePreview = "17",
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Basic, AR, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingBasicArDark() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "12+5",
                livePreview = "17",
            ),
            onAction = {},
        )
    }
}

// ---- The one "feature showcase" preview that keeps the cursor mid-expression. ----
// Demonstrates the editable-cursor feature shipped in Phase 2.9: tap anywhere
// in the expression to position the cursor and type/delete at that position.
@Preview(
    name = "Marketing — Editable cursor showcase",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun MarketingCursorShowcase() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "12+5",
                cursor = 2,
                selectionStart = 2,
                livePreview = "17",
            ),
            onAction = {},
        )
    }
}

// ---- Portrait: Scientific mode with M chip lit ----

@Preview(
    name = "Marketing — Scientific, EN, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun MarketingScientificEnLight() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "sin(30)+cos(60)",
                livePreview = "1",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Scientific, EN, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingScientificEnDark() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "sin(30)+cos(60)",
                livePreview = "1",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Scientific, AR, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
)
@Composable
private fun MarketingScientificArLight() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "sin(30)+cos(60)",
                livePreview = "1",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Scientific, AR, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingScientificArDark() {
    SifrTheme {
        CalculatorScreen(
            state = CalculatorState(
                expression = "sin(30)+cos(60)",
                livePreview = "1",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

// ---- Landscape: Scientific + basic side-by-side ----

@Preview(
    name = "Marketing — Landscape, EN, Light",
    widthDp = 800,
    heightDp = 360,
    showBackground = true,
)
@Composable
private fun MarketingLandscapeEnLight() {
    SifrTheme {
        CalculatorScreenLandscape(
            state = CalculatorState(
                expression = "12xsin(45)",
                livePreview = "8.485",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Landscape, EN, Dark",
    widthDp = 800,
    heightDp = 360,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingLandscapeEnDark() {
    SifrTheme {
        CalculatorScreenLandscape(
            state = CalculatorState(
                expression = "12xsin(45)",
                livePreview = "8.485",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Landscape, AR, Light",
    widthDp = 800,
    heightDp = 360,
    showBackground = true,
    locale = "ar",
)
@Composable
private fun MarketingLandscapeArLight() {
    SifrTheme {
        CalculatorScreenLandscape(
            state = CalculatorState(
                expression = "12xsin(45)",
                livePreview = "8.485",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — Landscape, AR, Dark",
    widthDp = 800,
    heightDp = 360,
    showBackground = true,
    locale = "ar",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingLandscapeArDark() {
    SifrTheme {
        CalculatorScreenLandscape(
            state = CalculatorState(
                expression = "12xsin(45)",
                livePreview = "8.485",
                mode = CalculatorMode.Scientific,
                angleUnit = AngleUnit.Degrees,
                memoryValue = 42.0,
            ),
            onAction = {},
        )
    }
}

// ---- History ----

private val historyMarketingEntries = listOf(
    HistoryEntry(1, "sin(30)+cos(60)", "1", System.currentTimeMillis()),
    HistoryEntry(2, "100x0.15", "15", System.currentTimeMillis() - 60_000),
    HistoryEntry(3, "sqrt(144)", "12", System.currentTimeMillis() - 180_000),
    HistoryEntry(4, "2^10", "1024", System.currentTimeMillis() - 600_000),
    HistoryEntry(5, "12+5", "17", System.currentTimeMillis() - 3_600_000),
)

@Preview(
    name = "Marketing — History, EN, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun MarketingHistoryEnLight() {
    SifrTheme {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = historyMarketingEntries),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — History, EN, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingHistoryEnDark() {
    SifrTheme {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = historyMarketingEntries),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — History, AR, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
)
@Composable
private fun MarketingHistoryArLight() {
    SifrTheme {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = historyMarketingEntries),
            onAction = {},
        )
    }
}

@Preview(
    name = "Marketing — History, AR, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingHistoryArDark() {
    SifrTheme {
        HistoryScreen(
            state = HistoryState(isLoading = false, entries = historyMarketingEntries),
            onAction = {},
        )
    }
}

// ---- Settings ----

@Preview(
    name = "Marketing — Settings, EN, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun MarketingSettingsEnLight() {
    SifrTheme {
        SettingsScreen(state = SettingsState(isLoading = false), onAction = {})
    }
}

@Preview(
    name = "Marketing — Settings, EN, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingSettingsEnDark() {
    SifrTheme {
        SettingsScreen(state = SettingsState(isLoading = false), onAction = {})
    }
}

@Preview(
    name = "Marketing — Settings, AR, Light",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
)
@Composable
private fun MarketingSettingsArLight() {
    SifrTheme {
        SettingsScreen(state = SettingsState(isLoading = false), onAction = {})
    }
}

@Preview(
    name = "Marketing — Settings, AR, Dark",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
    locale = "ar",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketingSettingsArDark() {
    SifrTheme {
        SettingsScreen(state = SettingsState(isLoading = false), onAction = {})
    }
}
