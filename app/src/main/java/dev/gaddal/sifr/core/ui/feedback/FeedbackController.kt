package dev.gaddal.sifr.core.ui.feedback

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class FeedbackController internal constructor(
    private val view: View,
) {
    var hapticsEnabled: Boolean = true
        internal set
    var soundEnabled: Boolean = false
        internal set

    fun click() {
        if (hapticsEnabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (soundEnabled) view.playSoundEffect(SoundEffectConstants.CLICK)
    }
}

@Composable
fun rememberFeedbackController(
    hapticsEnabled: Boolean,
    soundEnabled: Boolean,
): FeedbackController {
    val view = LocalView.current
    return remember(view) { FeedbackController(view) }.apply {
        this.hapticsEnabled = hapticsEnabled
        this.soundEnabled = soundEnabled
    }
}
