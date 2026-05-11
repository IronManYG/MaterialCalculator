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
 * Preset choice is constrained to those confirmed to fire on Honor 400 Pro
 * (Android 16 / MagicOS 10) — the device exposed in the Pulsar bug report at
 * `docs/pulsar-bug-report-2026-05-11.md`. Pulsar's `system*` view-based and
 * composition-primitive presets are silent on that device, so we use the
 * continuous-pattern built-ins: `alarm`, `applause`, `anvil`, `bloom`.
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
        VibratorCapabilityProbe.logOnce(context)
    }

    fun play(intent: FeedbackIntent) {
        if (hapticsEnabled) playHaptic(intent)
        if (soundEnabled) playSound(intent)
    }

    private fun playHaptic(intent: FeedbackIntent) {
        when (intent) {
            FeedbackIntent.Error -> presets.alarm()
            FeedbackIntent.CalculateSuccess -> presets.applause()
            FeedbackIntent.Destructive -> presets.anvil()
            FeedbackIntent.Selection -> presets.bloom()
        }
    }

    private fun playSound(intent: FeedbackIntent) {
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
