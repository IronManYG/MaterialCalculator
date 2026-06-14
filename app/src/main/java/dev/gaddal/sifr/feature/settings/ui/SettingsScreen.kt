package dev.gaddal.sifr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
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
    SettingsScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
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
            SectionLabel(stringResource(R.string.settings_language_section))
            SifrCard {
                AppLanguage.entries.forEachIndexed { index, lang ->
                    if (index > 0) SifrRowDivider()
                    SifrRow(
                        label = lang.displayLabel(),
                        onClick = { onAction(SettingsAction.SetLanguage(lang)) },
                        trailing = {
                            if (lang == state.settings.language) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = sifr.accent,
                                )
                            }
                        },
                    )
                }
            }

            // ── Appearance ──────────────────────────────────────────────
            SectionLabel(stringResource(R.string.settings_appearance_section))
            SifrCard {
                SifrRow(
                    label = stringResource(R.string.settings_theme_section), // "Mode"
                    trailing = {
                        SifrSegmented(
                            options = ThemeMode.entries,
                            selected = state.settings.themeMode,
                            label = { stringResource(it.shortLabelRes()) },
                            onSelect = { onAction(SettingsAction.SetThemeMode(it)) },
                        )
                    },
                )
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
                SifrRow(
                    label = stringResource(R.string.settings_angle_unit),
                    trailing = {
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
                        )
                    },
                )
                SifrRowDivider()
                SifrRow(
                    label = stringResource(R.string.settings_restore_target),
                    sub = stringResource(R.string.settings_restore_target_sub),
                    trailing = {
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
                        )
                    },
                )
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

/** A titled block inside a card for the picker grids (Theme swatches, keypad layouts). */
@Composable
private fun CardBlock(label: String, content: @Composable () -> Unit) {
    val sifr = SifrTokens.colors
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(text = label, color = sifr.text, fontFamily = sifr.uiFamily, fontSize = 14.5.sp)
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
