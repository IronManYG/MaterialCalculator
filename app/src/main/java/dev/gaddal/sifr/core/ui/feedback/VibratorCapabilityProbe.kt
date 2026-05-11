package dev.gaddal.sifr.core.ui.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log

/**
 * One-shot probe that dumps the device's vibrator capabilities to logcat.
 * Run once on first construction of [FeedbackController]. Output is meant to be
 * copy-pasted into the Pulsar bug report at docs/pulsar-bug-report-2026-05-11.md
 * (TODO #3: capability probe output).
 *
 * Filter logcat with `adb logcat -s PulsarRepro:*` to isolate.
 */
internal object VibratorCapabilityProbe {

    private const val TAG = "PulsarRepro"
    private var logged = false

    fun logOnce(context: Context) {
        if (logged) return
        logged = true
        runCatching { logCapabilities(context) }
            .onFailure { Log.w(TAG, "probe failed", it) }
    }

    private fun logCapabilities(context: Context) {
        Log.i(TAG, "===== Vibrator capability probe =====")
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        Log.i(TAG, "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.i(TAG, "Build: ${Build.DISPLAY}")

        val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        if (vibrator == null) {
            Log.i(TAG, "vibrator: <null>")
            return
        }
        Log.i(TAG, "hasVibrator()=${vibrator.hasVibrator()}")
        Log.i(TAG, "hasAmplitudeControl()=${vibrator.hasAmplitudeControl()}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effects = intArrayOf(
                VibrationEffect.EFFECT_CLICK,
                VibrationEffect.EFFECT_DOUBLE_CLICK,
                VibrationEffect.EFFECT_HEAVY_CLICK,
                VibrationEffect.EFFECT_TICK,
            )
            val effectNames = listOf("CLICK", "DOUBLE_CLICK", "HEAVY_CLICK", "TICK")
            val effectSupport = vibrator.areEffectsSupported(*effects)
            effectNames.zip(effectSupport.toList()).forEach { (name, v) ->
                Log.i(TAG, "effect $name -> ${effectSupportToString(v)}")
            }

            val primitives = intArrayOf(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
                VibrationEffect.Composition.PRIMITIVE_SPIN,
                VibrationEffect.Composition.PRIMITIVE_THUD,
            )
            val primitiveNames = listOf(
                "CLICK", "TICK", "LOW_TICK", "QUICK_RISE",
                "SLOW_RISE", "QUICK_FALL", "SPIN", "THUD",
            )
            val primitiveSupport = vibrator.arePrimitivesSupported(*primitives)
            primitiveNames.zip(primitiveSupport.toList()).forEach { (name, v) ->
                Log.i(TAG, "primitive $name -> $v")
            }
        } else {
            Log.i(TAG, "areEffectsSupported / arePrimitivesSupported require API 30+")
        }

        runCatching {
            val haptic = Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                -1,
            )
            Log.i(TAG, "Settings.System.HAPTIC_FEEDBACK_ENABLED=$haptic (0=off, 1=on, -1=unset)")
        }.onFailure { Log.w(TAG, "Settings.System read failed", it) }

        Log.i(TAG, "===== /probe =====")
    }

    private fun effectSupportToString(v: Int): String = when (v) {
        Vibrator.VIBRATION_EFFECT_SUPPORT_YES -> "YES"
        Vibrator.VIBRATION_EFFECT_SUPPORT_NO -> "NO"
        else -> "UNKNOWN"
    }
}
