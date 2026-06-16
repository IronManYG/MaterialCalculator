package dev.gaddal.sifr.feature.settings.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.domain.settings.AppLanguage
import dev.gaddal.sifr.core.domain.settings.AppSettings
import dev.gaddal.sifr.core.domain.settings.KeypadLayout
import dev.gaddal.sifr.core.domain.settings.RestoreTarget
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode
import dev.gaddal.sifr.core.ui.components.SifrCard
import dev.gaddal.sifr.core.ui.components.SifrRow
import dev.gaddal.sifr.core.ui.components.SifrRowDivider
import dev.gaddal.sifr.core.ui.components.SifrSegmented
import dev.gaddal.sifr.core.ui.components.SifrToggle
import dev.gaddal.sifr.core.ui.feedback.FeedbackIntent
import dev.gaddal.sifr.core.ui.feedback.rememberFeedbackController
import dev.gaddal.sifr.core.ui.locale.SifrLocale
import dev.gaddal.sifr.core.ui.theme.SifrTheme
import dev.gaddal.sifr.core.ui.theme.SifrTokens
import dev.gaddal.sifr.core.ui.util.ObserveAsEvents
import dev.gaddal.sifr.feature.calculator.domain.AngleUnit
import dev.gaddal.sifr.feature.settings.ui.components.KeypadLayoutPicker
import dev.gaddal.sifr.feature.settings.ui.components.ThemePicker
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoot(
    windowSizeClass: WindowSizeClass,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedback = rememberFeedbackController(
        hapticsEnabled = state.settings.hapticsEnabled,
        soundEnabled = state.settings.soundEnabled,
    )
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateBack -> onNavigateBack()
            SettingsEvent.DemoHaptic -> feedback.playHaptic(FeedbackIntent.Selection)
            SettingsEvent.DemoSound -> feedback.playSound(FeedbackIntent.Error)
        }
    }

    // Manual orientation toggle, mirroring the calculator/Tools Rotate action — the activity
    // owns the initial lock (CalculatorRoot); Settings just flips it on demand. NavRoot owns the
    // landscape status-bar hide for every route, so there is nothing else to wire here.
    val isLandscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val view = LocalView.current
    val activity = view.context as? Activity
    val onRotate = remember(isLandscape, activity) {
        {
            activity?.requestedOrientation =
                if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        onRotate = onRotate,
        rotateActive = isLandscape,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onRotate: () -> Unit = {},
    rotateActive: Boolean = false,
) {
    val sifr = SifrTokens.colors
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(sifr.background),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsAction.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRotate) {
                        Icon(
                            imageVector = Icons.Outlined.ScreenRotation,
                            contentDescription = stringResource(R.string.calc_rotate_orientation),
                            tint = if (rotateActive) sifr.accent else sifr.dim,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = sifr.text,
                    navigationIconContentColor = sifr.text,
                ),
            )
        },
    ) { padding ->
        val dark = when (state.settings.themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Scroll so the form stays usable in short landscape / split-screen heights.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
        ) {
            // ── Language ────────────────────────────────────────────────
            // The list grew to 11 languages — collapse it to one row that opens a bottom-sheet
            // picker. Sheet open/closed is ephemeral UI state, so it lives here (rememberSaveable),
            // not in the ViewModel.
            SectionLabel(stringResource(R.string.settings_language_section))
            var languageSheetOpen by rememberSaveable { mutableStateOf(false) }
            SifrCard {
                SifrRow(
                    label = stringResource(R.string.settings_language_section),
                    onClick = { languageSheetOpen = true },
                    // A universal "translate" glyph (文A) anchors this row visually, so a user
                    // stranded in a script they can't read can still find where to switch back.
                    leading = {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null, // label already conveys the row's purpose
                            tint = sifr.accent, // accent makes it the obvious "escape hatch" anchor
                        )
                    },
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Endonym of the current language — a literal, not a resource.
                            Text(
                                text = state.settings.language.displayLabel(),
                                color = sifr.dim,
                                fontFamily = sifr.uiFamily,
                                fontSize = 14.sp,
                            )
                            // Auto-mirrored so the chevron points the reading-exit way under RTL.
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = sifr.dim,
                            )
                        }
                    },
                )
            }
            if (languageSheetOpen) {
                LanguagePickerSheet(
                    selected = state.settings.language,
                    onSelect = {
                        onAction(SettingsAction.SetLanguage(it))
                        languageSheetOpen = false
                    },
                    onDismiss = { languageSheetOpen = false },
                )
            }

            // ── Appearance ──────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_appearance_section))
            SifrCard {
                CardBlock(stringResource(R.string.settings_theme_section)) { // "Mode"
                    SifrSegmented(
                        options = ThemeMode.entries,
                        selected = state.settings.themeMode,
                        label = { stringResource(it.shortLabelRes()) },
                        onSelect = { onAction(SettingsAction.SetThemeMode(it)) },
                        spanWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SifrRowDivider()
                CardBlock(stringResource(R.string.settings_palette_section)) { // "Theme"
                    ThemePicker(
                        selected = state.settings.palette,
                        dark = dark,
                        onSelect = { onAction(SettingsAction.SetPalette(it)) },
                    )
                }
            }

            // ── Keypad ──────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_keypad_section))
            SifrCard {
                CardBlock(stringResource(R.string.settings_keypad_layout)) { // "Layout"
                    KeypadLayoutPicker(
                        selected = state.settings.keypadLayout,
                        onSelect = { onAction(SettingsAction.SetKeypadLayout(it)) },
                    )
                }
                SifrRowDivider()
                SifrRow(
                    label = stringResource(R.string.settings_memory_keys),
                    sub = stringResource(R.string.settings_memory_keys_sub),
                    trailing = {
                        SifrToggle(
                            checked = state.settings.memoryKeysVisible,
                            onCheckedChange = { onAction(SettingsAction.ToggleMemoryKeys) },
                        )
                    },
                )
            }

            // ── Display ─────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_display_section))
            SifrCard {
                SifrRow(
                    label = stringResource(R.string.settings_fraction_results),
                    sub = stringResource(R.string.settings_fraction_results_sub),
                    trailing = {
                        SifrToggle(
                            checked = state.settings.fractionResults,
                            onCheckedChange = { onAction(SettingsAction.ToggleFractionResults) },
                        )
                    },
                )
                SifrRowDivider()
                CardBlock(stringResource(R.string.settings_angle_unit)) {
                    SifrSegmented(
                        options = AngleUnit.entries,
                        selected = state.settings.angleUnit,
                        label = {
                            stringResource(
                                if (it == AngleUnit.Degrees) R.string.settings_angle_deg
                                else R.string.settings_angle_rad,
                            )
                        },
                        onSelect = { onAction(SettingsAction.SetAngleUnit(it)) },
                        spanWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SifrRowDivider()
                CardBlock(
                    label = stringResource(R.string.settings_restore_target),
                    sub = stringResource(R.string.settings_restore_target_sub),
                ) {
                    SifrSegmented(
                        options = RestoreTarget.entries,
                        selected = state.settings.restoreTarget,
                        label = {
                            stringResource(
                                if (it == RestoreTarget.Result) R.string.settings_restore_target_result
                                else R.string.settings_restore_target_expression,
                            )
                        },
                        onSelect = { onAction(SettingsAction.SetRestoreTarget(it)) },
                        spanWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Feedback ────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_feedback_section))
            SifrCard {
                SifrRow(
                    label = stringResource(R.string.settings_haptics),
                    sub = stringResource(R.string.settings_haptics_sub),
                    trailing = {
                        SifrToggle(
                            checked = state.settings.hapticsEnabled,
                            onCheckedChange = { onAction(SettingsAction.ToggleHaptics) },
                        )
                    },
                )
                SifrRowDivider()
                SifrRow(
                    label = stringResource(R.string.settings_sound),
                    trailing = {
                        SifrToggle(
                            checked = state.settings.soundEnabled,
                            onCheckedChange = { onAction(SettingsAction.ToggleSound) },
                        )
                    },
                )
            }

            // ── About ───────────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_about_section))
            SifrCard {
                SifrRow(
                    label = "Sifr — صفر",
                    sub = stringResource(R.string.settings_about_sub),
                    trailing = {
                        Text(
                            text = "٠",
                            color = sifr.accent,
                            fontFamily = sifr.displayFamily,
                            fontSize = 22.sp,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Bottom-sheet language picker. One scrollable mutually-exclusive radio group over
 * [AppLanguage.entries], a Check on the selected language. String-free a11y: each row is a
 * `selectable(role = RadioButton)` inside a `selectableGroup()`, so the framework localizes
 * "selected" / "double-tap to activate" in every locale. Sheet open/closed is owned by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sifr = SifrTokens.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        // Flatten the glass surface over the opaque background — Layl's surface is
        // ~transparent (alpha 0.04), so the bare `surface` let the page show through.
        // Mirrors CurrencyPicker's sheet; opaque on every palette, identical look elsewhere.
        containerColor = sifr.surface.compositeOver(sifr.backgroundFlat),
        dragHandle = { BottomSheetDefaults.DragHandle(color = sifr.dim) },
    ) {
        LanguageSheetList(selected = selected, onSelect = onSelect)
    }
}

/** The sheet's scrollable radio-group body — extracted so it previews without the sheet host. */
@Composable
private fun LanguageSheetList(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sifr = SifrTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Scrollable so the list scales past today's 11 languages.
            .verticalScroll(rememberScrollState())
            .selectableGroup()
            .padding(bottom = 24.dp),
    ) {
        // Sheet heading reuses the existing "Language" string — no new key. The translate
        // glyph mirrors the Settings row, confirming the stranded user landed in the right place.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = null,
                tint = sifr.dim,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.settings_language_section),
                color = sifr.dim,
                fontFamily = sifr.uiFamily,
                fontSize = 12.sp,
            )
        }
        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(lang) },
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = lang.displayLabel(),
                    color = sifr.text,
                    fontFamily = sifr.uiFamily,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    // Decorative — selection is already conveyed by the RadioButton semantics.
                    Icon(Icons.Filled.Check, contentDescription = null, tint = sifr.accent)
                }
            }
        }
    }
}

/** Section heading above each card: small, dim, wide letter-spacing (prototype sectionTitle). */
@Composable
private fun SectionLabel(text: String) {
    val sifr = SifrTokens.colors
    // Positive letter-spacing breaks Arabic letter-joins — apply the wide tracking in LTR only.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Text(
        text = text,
        color = sifr.dim,
        fontFamily = sifr.uiFamily,
        fontSize = 11.sp,
        letterSpacing = if (rtl) TextUnit.Unspecified else 0.2.em,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * A titled block inside a card: a [label] with an optional [sub]title stacked on their own line(s),
 * then the [content] (a picker grid or a full-width segmented control) on the line below. Used for
 * the Theme / Layout pickers and — so a long translated label never gets squeezed into two lines by
 * a wide inline control — the Mode / Angle / Restore segmented settings.
 */
@Composable
private fun CardBlock(label: String, modifier: Modifier = Modifier, sub: String? = null, content: @Composable () -> Unit) {
    val sifr = SifrTokens.colors
    Column(modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(text = label, color = sifr.text, fontFamily = sifr.uiFamily, fontSize = 14.5.sp)
        if (sub != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = sub, color = sifr.dim, fontFamily = sifr.uiFamily, fontSize = 11.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

private fun ThemeMode.shortLabelRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system_short
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

@Composable
private fun AppLanguage.displayLabel(): String = when (this) {
    AppLanguage.System -> stringResource(R.string.settings_language_system)
    AppLanguage.English -> "English"
    AppLanguage.Arabic -> "العربية"
    // Endonyms — each language names itself, independent of the active locale.
    AppLanguage.Spanish -> "Español"
    AppLanguage.Portuguese -> "Português (Brasil)"
    AppLanguage.French -> "Français"
    AppLanguage.German -> "Deutsch"
    AppLanguage.Indonesian -> "Bahasa Indonesia"
    AppLanguage.Turkish -> "Türkçe"
    AppLanguage.Italian -> "Italiano"
    AppLanguage.Vietnamese -> "Tiếng Việt"
    AppLanguage.Russian -> "Русский"
}

@Preview(name = "Settings — Layl dark", showBackground = true)
@Composable
private fun SettingsPreviewLayl() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    SettingsScreen(
        state = SettingsState(settings = AppSettings(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark), isLoading = false),
        onAction = {},
    )
}

@Preview(name = "Settings — Bayan light", showBackground = true)
@Composable
private fun SettingsPreviewBayan() = SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
    SettingsScreen(
        state = SettingsState(settings = AppSettings(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light), isLoading = false),
        onAction = {},
    )
}

@Preview(name = "Settings — DISPLAY on / RAD", showBackground = true)
@Composable
private fun SettingsPreviewDisplayVariant() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Light) {
    SettingsScreen(
        state = SettingsState(settings = AppSettings(fractionResults = true, angleUnit = AngleUnit.Radians), isLoading = false),
        onAction = {},
    )
}

@Preview(name = "Settings — Keypad (Arc / mem off)", showBackground = true)
@Composable
private fun SettingsPreviewKeypad() = SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
    SettingsScreen(
        state = SettingsState(
            settings = AppSettings(keypadLayout = KeypadLayout.Tape, memoryKeysVisible = false),
            isLoading = false,
        ),
        onAction = {},
    )
}

@Preview(name = "Settings — Arabic (Farah)", showBackground = true)
@Composable
private fun SettingsPreviewArabic() = SifrLocale(language = AppLanguage.Arabic) {
    SifrTheme(palette = SifrPalette.Farah, themeMode = ThemeMode.Light) {
        SettingsScreen(
            state = SettingsState(
                settings = AppSettings(palette = SifrPalette.Farah, language = AppLanguage.Arabic),
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Settings — Russian (Layl dark)", showBackground = true)
@Composable
private fun SettingsPreviewRussian() = SifrLocale(language = AppLanguage.Russian) {
    SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
        SettingsScreen(
            state = SettingsState(
                settings = AppSettings(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark, language = AppLanguage.Russian),
                isLoading = false,
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Language sheet — Layl dark", showBackground = true)
@Composable
private fun LanguageSheetPreviewDark() = SifrTheme(palette = SifrPalette.Layl, themeMode = ThemeMode.Dark) {
    SifrCard { LanguageSheetList(selected = AppLanguage.Spanish, onSelect = {}) }
}

@Preview(name = "Language sheet — Bayan light", showBackground = true)
@Composable
private fun LanguageSheetPreviewLight() = SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
    SifrCard { LanguageSheetList(selected = AppLanguage.English, onSelect = {}) }
}

@Preview(name = "Language sheet — Arabic / RTL (Mizan)", showBackground = true, locale = "ar")
@Composable
private fun LanguageSheetPreviewArabic() = SifrLocale(language = AppLanguage.Arabic) {
    SifrTheme(palette = SifrPalette.Mizan, themeMode = ThemeMode.Dark) {
        SifrCard { LanguageSheetList(selected = AppLanguage.Arabic, onSelect = {}) }
    }
}

@Preview(name = "Language sheet — Russian (Bayan light)", showBackground = true, locale = "ru")
@Composable
private fun LanguageSheetPreviewRussian() = SifrLocale(language = AppLanguage.Russian) {
    SifrTheme(palette = SifrPalette.Bayan, themeMode = ThemeMode.Light) {
        SifrCard { LanguageSheetList(selected = AppLanguage.Russian, onSelect = {}) }
    }
}
