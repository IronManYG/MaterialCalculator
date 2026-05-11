package dev.gaddal.sifr.core.ui.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.swmansion.pulsar.Pulsar

/**
 * Plays semantic [FeedbackIntent]s as haptic + optional sound, gated by the
 * user's [hapticsEnabled] / [soundEnabled] preferences.
 *
 * Preset choice favors short, punchy continuous-pattern built-ins:
 * `bloom`, `flick`, `burst`, `thud`. Pulsar's `system*` view-based and
 * composition-primitive presets are silent on some vendor ROMs (MagicOS /
 * EMUI / HarmonyOS-derived) — see `docs/pulsar-bug-report-2026-05-11.md`.
 * The four presets used here all carry a `rawContinuousPattern` channel,
 * which Pulsar's engine lowers to amplitude-scaled `createWaveform` —
 * the same fallback that makes `bloom` survive on Honor 400 Pro.
 */
class FeedbackController internal constructor(
    context: Context,
) {
    var hapticsEnabled: Boolean = true
        internal set
    var soundEnabled: Boolean = false
        internal set

    private val presets = Pulsar(context).getPresets()
    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_SYSTEM, TONE_VOLUME)
    }.getOrNull()

    init {
        // Retained in shipping builds as a permanent device-compatibility
        // audit hook — when a future user reports "haptics don't work on my
        // X", `adb pull` of pulsar-probe.txt gives an immediate capability
        // map. Cost: one Vibrator system-service read + a tiny file write,
        // once per FeedbackController construction. See VibratorCapabilityProbe.
        VibratorCapabilityProbe.logOnce(context)
    }

    fun play(intent: FeedbackIntent) {
        if (hapticsEnabled) playHaptic(intent)
        if (soundEnabled) playSound(intent)
    }

    /** Fires the haptic for [intent] regardless of [hapticsEnabled]. Used by
     * Settings toggle demos — the user just turned the toggle on, so demoing
     * the feature is exactly what they asked for. */
    fun playHaptic(intent: FeedbackIntent) {
        when (intent) {
            FeedbackIntent.Error -> presets.thud()
            FeedbackIntent.CalculateSuccess -> presets.burst()
            FeedbackIntent.Destructive -> presets.flick()
            FeedbackIntent.Selection -> presets.bloom()
        }
    }

    /** Fires the sound for [intent] regardless of [soundEnabled]. Same
     * rationale as [playHaptic]. */
    fun playSound(intent: FeedbackIntent) {
        // Sound is reserved for top-priority signals only. Per-action sound
        // is fatiguing in a calculator (every digit press would beep).
        if (intent == FeedbackIntent.Error) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)
        }
    }

    internal fun release() {
        toneGenerator?.release()
    }

    private companion object {
        const val TONE_VOLUME = 50
        const val TONE_DURATION_MS = 40
    }
}

@Composable
fun rememberFeedbackController(
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
): FeedbackController {
    val context = LocalContext.current
    val controller = remember(context) { FeedbackController(context) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    controller.hapticsEnabled = hapticsEnabled
    controller.soundEnabled = soundEnabled
    return controller
}
