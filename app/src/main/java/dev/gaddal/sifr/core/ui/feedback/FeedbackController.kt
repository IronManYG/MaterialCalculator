package dev.gaddal.sifr.core.ui.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class FeedbackController internal constructor(
    context: Context,
) {
    var hapticsEnabled: Boolean = true
        internal set
    var soundEnabled: Boolean = false
        internal set

    private val vibrator: Vibrator? = resolveVibrator(context)
    private val toneGenerator: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_SYSTEM, TONE_VOLUME)
    }.getOrNull()

    fun click() {
        if (hapticsEnabled) vibrate()
        if (soundEnabled) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MS)
    }

    internal fun release() {
        toneGenerator?.release()
    }

    private fun vibrate() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(VIBRATE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(VIBRATE_MS)
        }
    }

    private companion object {
        const val VIBRATE_MS = 35L
        const val TONE_VOLUME = 50
        const val TONE_DURATION_MS = 40
    }
}

private fun resolveVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
