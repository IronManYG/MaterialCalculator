package dev.gaddal.sifr.core.ui.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import java.io.File

/**
 * One-shot probe that dumps the device's vibrator capabilities to logcat AND
 * to a file in the app's external-files directory (no permission required).
 * Some vendor ROMs (MagicOS / EMUI / HarmonyOS) encrypt logcat output by
 * default, which makes `adb logcat` unreadable — the file fallback sidesteps
 * that. Pull the file with:
 *
 *     adb pull /sdcard/Android/data/com.gaddal.materialcalculator/files/pulsar-probe.txt
 *
 * Output is meant to be copy-pasted into docs/pulsar-bug-report-2026-05-11.md.
 *
 * **Retained in shipping builds.** Originally written to assemble evidence for
 * the Pulsar bug report; retained because the underlying problem (vendor ROMs
 * silently no-op on unsupported primitive/effect/view-haptic paths) is broad
 * enough that future "haptics don't work on my X" reports will recur as the
 * app reaches more devices. The probe is one-shot per process, cheap (a
 * single Vibrator system-service read + a tiny text-file write), and gives
 * any future bug report an immediate capability matrix without a code change.
 * Strip only if the app's haptic surface shrinks enough that the diagnostic
 * is no longer worth its ~1 KB of bytecode.
 */
internal object VibratorCapabilityProbe {

    private const val TAG = "PulsarRepro"
    private const val FILE_NAME = "pulsar-probe.txt"
    private var logged = false

    fun logOnce(context: Context) {
        if (logged) return
        logged = true
        runCatching {
            val text = buildReport(context)
            Log.i(TAG, text)
            writeToFile(context, text)
        }.onFailure { Log.w(TAG, "probe failed", it) }
    }

    private fun buildReport(context: Context): String = buildString {
        appendLine("===== Vibrator capability probe =====")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("Build: ${Build.DISPLAY}")

        val vibrator: Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        if (vibrator == null) {
            appendLine("vibrator: <null>")
            return@buildString
        }
        appendLine("hasVibrator()=${vibrator.hasVibrator()}")
        val hasAmplitudeControl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
        appendLine("hasAmplitudeControl()=$hasAmplitudeControl")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val effectSupport = vibrator.areEffectsSupported(
                VibrationEffect.EFFECT_CLICK,
                VibrationEffect.EFFECT_DOUBLE_CLICK,
                VibrationEffect.EFFECT_HEAVY_CLICK,
                VibrationEffect.EFFECT_TICK,
            )
            val effectNames = listOf("CLICK", "DOUBLE_CLICK", "HEAVY_CLICK", "TICK")
            effectNames.zip(effectSupport.toList()).forEach { (name, v) ->
                appendLine("effect $name -> ${effectSupportToString(v)}")
            }

            val primitiveSupportApi30 = vibrator.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            )
            val primitiveApi30Names = listOf("CLICK", "TICK", "QUICK_RISE", "SLOW_RISE", "QUICK_FALL")
            primitiveApi30Names.zip(primitiveSupportApi30.toList()).forEach { (name, v) ->
                appendLine("primitive $name -> $v")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val primitiveSupportApi31 = vibrator.arePrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                    VibrationEffect.Composition.PRIMITIVE_SPIN,
                    VibrationEffect.Composition.PRIMITIVE_THUD,
                )
                val primitiveApi31Names = listOf("LOW_TICK", "SPIN", "THUD")
                primitiveApi31Names.zip(primitiveSupportApi31.toList()).forEach { (name, v) ->
                    appendLine("primitive $name -> $v")
                }
            } else {
                appendLine("primitive LOW_TICK / SPIN / THUD -> requires API 31")
            }
        } else {
            appendLine("areEffectsSupported / arePrimitivesSupported require API 30+")
        }

        runCatching {
            val haptic = Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                -1,
            )
            appendLine("Settings.System.HAPTIC_FEEDBACK_ENABLED=$haptic (0=off, 1=on, -1=unset)")
        }.onFailure { appendLine("Settings.System read failed: ${it.message}") }

        appendLine("===== /probe =====")
    }

    private fun writeToFile(context: Context, text: String) {
        val dir: File = context.getExternalFilesDir(null) ?: context.filesDir
        val target = File(dir, FILE_NAME)
        target.writeText(text)
        Log.i(TAG, "wrote ${target.absolutePath}")
    }

    private fun effectSupportToString(v: Int): String = when (v) {
        Vibrator.VIBRATION_EFFECT_SUPPORT_YES -> "YES"
        Vibrator.VIBRATION_EFFECT_SUPPORT_NO -> "NO"
        else -> "UNKNOWN"
    }
}
