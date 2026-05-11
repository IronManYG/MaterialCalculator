package dev.gaddal.sifr.feature.diag.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swmansion.pulsar.Pulsar
import com.swmansion.pulsar.presets.PresetsWrapper
import dev.gaddal.sifr.R
import dev.gaddal.sifr.core.ui.theme.SifrTheme

@Composable
fun HapticsTestRoot(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val presets = remember(context) { Pulsar(context).getPresets() }
    HapticsTestScreen(
        onNavigateBack = onNavigateBack,
        onPlay = { entry -> entry.invoke(presets) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticsTestScreen(
    onNavigateBack: () -> Unit,
    onPlay: (PresetEntry) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.haptics_test_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.haptics_test_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Banner() }
            item { SectionHeader(text = stringResource(R.string.haptics_test_section_felt)) }
            items(items = expectedFelt, key = { it.name }) { entry ->
                PresetCard(entry = entry, onPlay = onPlay)
            }
            item { SectionHeader(text = stringResource(R.string.haptics_test_section_silent)) }
            items(items = expectedSilent, key = { it.name }) { entry ->
                PresetCard(entry = entry, onPlay = onPlay)
            }
        }
    }
}

@Composable
private fun Banner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.haptics_test_banner),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun PresetCard(
    entry: PresetEntry,
    onPlay: (PresetEntry) -> Unit,
) {
    Card(
        onClick = { onPlay(entry) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.haptics_test_tap),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** A single Pulsar preset surfaced on the diagnostic screen.
 *
 * [invoke] calls the matching preset on a live [PresetsWrapper]. We use a
 * function reference rather than a string-keyed lookup so a typo becomes a
 * compile error. */
data class PresetEntry(
    val name: String,
    val category: String,
    val invoke: (PresetsWrapper) -> Unit,
)

/** Presets confirmed felt on Honor 400 Pro (Android 16 / MagicOS 10) plus
 * likely-felt continuous-pattern built-ins. Each has a `rawContinuousPattern`
 * channel; Pulsar's engine falls back to amplitude-scaled `createWaveform`,
 * which the device's LRA renders correctly.
 *
 * The first four (bloom / flick / burst / thud) are the in-app feedback
 * intents — wired into `FeedbackController.playHaptic`. They're listed
 * first so they're easy to A/B against the rest. */
internal val expectedFelt: List<PresetEntry> = listOf(
    PresetEntry("bloom", "In-app: Selection (confirmed felt)") { it.bloom() },
    PresetEntry("flick", "In-app: Destructive (verify here)") { it.flick() },
    PresetEntry("burst", "In-app: CalculateSuccess (verify here)") { it.burst() },
    PresetEntry("thud", "In-app: Error (verify here)") { it.thud() },
    PresetEntry("alarm", "Continuous + discrete (confirmed felt)") { it.alarm() },
    PresetEntry("anvil", "Continuous + discrete (confirmed felt)") { it.anvil() },
    PresetEntry("applause", "Continuous + discrete (confirmed felt)") { it.applause() },
    PresetEntry("heartbeat", "Rhythmic continuous (likely felt)") { it.heartbeat() },
    PresetEntry("breath", "Slow continuous (likely felt)") { it.breath() },
    PresetEntry("pulse", "Rhythmic continuous (likely felt)") { it.pulse() },
    PresetEntry("surge", "Smooth continuous (likely felt)") { it.surge() },
    PresetEntry("wave", "Continuous envelope (likely felt)") { it.wave() },
    PresetEntry("thunder", "Long continuous (likely felt)") { it.thunder() },
)

/** Presets silent on Honor 400 Pro, covering all three failure paths from the
 * Pulsar bug report at `docs/pulsar-bug-report-2026-05-11.md`. */
internal val expectedSilent: List<PresetEntry> = listOf(
    // View-based path: decorView.performHapticFeedback(...) — silent on MagicOS
    // even with HAPTIC_FEEDBACK_ENABLED=1 and all Sound & haptics toggles on.
    PresetEntry("systemKeyboardTap", "View-based (decorView.performHapticFeedback)") {
        it.systemKeyboardTap()
    },
    PresetEntry("systemSelection", "View-based (decorView.performHapticFeedback)") {
        it.systemSelection()
    },
    PresetEntry("systemImpactLight", "View-based (decorView.performHapticFeedback)") {
        it.systemImpactLight()
    },
    PresetEntry("systemImpactMedium", "View-based (decorView.performHapticFeedback)") {
        it.systemImpactMedium()
    },
    PresetEntry("systemContextClick", "View-based (decorView.performHapticFeedback)") {
        it.systemContextClick()
    },
    // Composition primitive path: Composition.addPrimitive(PRIMITIVE_*).
    // Honor 400 Pro returns false from arePrimitivesSupported for all primitives.
    PresetEntry("systemPrimitiveClick", "Composition primitive (PRIMITIVE_CLICK)") {
        it.systemPrimitiveClick()
    },
    PresetEntry("systemPrimitiveTick", "Composition primitive (PRIMITIVE_TICK)") {
        it.systemPrimitiveTick()
    },
    PresetEntry("systemPrimitiveSpin", "Composition primitive (PRIMITIVE_SPIN)") {
        it.systemPrimitiveSpin()
    },
    // Generated presets that lower to composition primitives only (no
    // continuous pattern fallback).
    PresetEntry("afterglow", "Primitive-only built-in (confirmed silent)") { it.afterglow() },
    PresetEntry("aftershock", "Primitive-only built-in (confirmed silent)") { it.aftershock() },
)

@Preview
@Composable
private fun HapticsTestScreenPreview() {
    SifrTheme {
        HapticsTestScreen(onNavigateBack = {}, onPlay = {})
    }
}
