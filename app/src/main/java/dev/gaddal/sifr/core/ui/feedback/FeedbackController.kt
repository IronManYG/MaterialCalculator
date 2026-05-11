package dev.gaddal.sifr.core.ui.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.swmansion.pulsar.Pulsar

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

    fun click() {
        if (hapticsEnabled) presets.systemKeyboardTap()
        if (soundEnabled) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)
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
