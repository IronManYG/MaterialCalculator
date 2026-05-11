# Pulsar bug report — draft

**Target repo:** https://github.com/software-mansion/pulsar
**Issue form:** https://github.com/software-mansion/pulsar/issues/new (Bug report template)
**Drafted:** 2026-05-11

Paste each section into the matching field on the issue form. Fill in the **bold TODO** markers before submitting.

---

## Title

```
Many system presets and amplitude-scaled presets silently no-op on devices whose vibrator HAL lacks predefined-effect / composition-primitive support
```

---

## Description

On my device, large portions of Pulsar's preset library produce **no haptic feedback at all** — the call returns normally but nothing is felt. This is reproducible in both my own SDK integration and the official Pulsar demo app installed from Play Store (`com.swmansion.pulsar.app`).

This is **not** a budget-device issue. Honor 400 Pro (2025 flagship, LRA vibrator) is the reproduction device. The hardware clearly supports rich haptics — `presets.bloom()` (which falls back to a continuous-amplitude waveform → `vibrate(VibrationEffect.createWaveform(...))`) is felt clearly. What is silent is everything routed through `View.performHapticFeedback`, `VibrationEffect.createPredefined`, and `VibrationEffect.Composition.addPrimitive` — i.e. the paths that depend on the vendor ROM honoring Android's system haptic-feedback service. MagicOS appears to filter all three. This pattern likely affects HarmonyOS-derived ROMs (Honor / Huawei) more broadly, and probably similar vendor stacks (EMUI, OriginOS, etc.) on devices users would expect "first-class" haptics on.

The pattern of what works vs. what is silent maps exactly to Android `Vibrator` hardware capabilities the SDK does not check before calling:

**Works (felt clearly):**
- Built-in presets that contain a continuous-amplitude pattern alongside their primitive pattern: `alarm`, `anvil`, `applause`, `bloom`
- Demo app screen: **Notification** tab

**Silent (no haptic):**
- `systemKeyboardTap`, `systemSelection`, `systemImpactLight/Medium/Heavy`, `systemNotificationSuccess/Warning/Error`, and most other `system*()` presets
- Built-in presets whose Studio waveform is *only* "vertical bars" plus amplitude scaling (no continuous-envelope channel): `afterglow`, `aftershock`, and many others
- Demo app screens: **Sliders**, **Buttons**, **Countdown timer**, **Boolean loader**, **Accelerator**

**Root cause (from reading the SDK source):**

1. `SystemViewBasedPresets.playHaptic(Int)` calls `decorView.performHapticFeedback(...)` with **no flags**. This routes through `Settings.System.HAPTIC_FEEDBACK_ENABLED`, which is off by default on many vendor ROMs (Xiaomi/Oppo/Vivo/Realme/Samsung-budget — and apparently MagicOS too). `FLAG_IGNORE_GLOBAL_SETTING` is system-only, but `HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING` is not used either.

2. `SystemEffectPresets` calls `VibrationEffect.createPredefined(EFFECT_CLICK / EFFECT_TICK / …)` guarded only by `SDK_INT >= Q`. It does **not** call `Vibrator.areAllEffectsSupported(...)`. The Android framework silently no-ops unsupported predefined effects — see [Vibrator.areAllEffectsSupported](https://developer.android.com/reference/android/os/Vibrator#areAllEffectsSupported(int...)). Vendor ROMs can return `VIBRATION_EFFECT_SUPPORT_NO` or `_UNKNOWN` for everything even when the hardware can render the effect.

3. `SystemPrimitivePresets` calls `VibrationEffect.startComposition().addPrimitive(PRIMITIVE_CLICK / TICK / …).compose()` guarded only by `SDK_INT >= R`. It does **not** call `Vibrator.areAllPrimitivesSupported(...)`. Same silent-no-op behavior when the device's vibrator HAL doesn't register that primitive.

4. Generated presets like `AfterglowPreset` (`rawDiscretePattern` = `[(0,1.0,0.3), (75,0.703,0.203), (150,0.5,0.1)]`, empty `rawContinuousPattern`) end up routed through composition primitives with amplitude scaling. With no `hasAmplitudeControl()` / `areAllPrimitivesSupported()` fallback, they go silent. Presets that *also* carry a `rawContinuousPattern` (alarm, bloom, applause, anvil) appear to fall back to a basic `createWaveform` and survive.

**Expected behavior:** every preset should produce *some* haptic feedback. When the device lacks the requested primitive/effect/amplitude support, Pulsar should degrade to a basic on/off `createWaveform` derived from the pattern's timing rather than calling an API that the framework will silently discard.

**Suggested fix sketch:**
- At `Pulsar(context)` construction, probe once:
  - `vibrator.hasAmplitudeControl()`
  - `vibrator.areAllPrimitivesSupported(PRIMITIVE_CLICK, PRIMITIVE_TICK, PRIMITIVE_SPIN, …)`
  - `vibrator.areAllEffectsSupported(EFFECT_CLICK, EFFECT_TICK, EFFECT_DOUBLE_CLICK, EFFECT_HEAVY_CLICK)`
- Each preset picks the first supported path in this chain: predefined effect → composition primitive (with amplitude scale) → `createOneShot` with amplitude → `createOneShot` without amplitude → `vibrate(long)` (deprecated, API 24-25).
- For `SystemViewBasedPresets`, either pass `HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING` (where applicable) or, when `Settings.System.HAPTIC_FEEDBACK_ENABLED == 0`, route through the `Vibrator` path with a hand-rolled equivalent waveform.
- A `Log.w` on first occurrence per session when a requested effect/primitive isn't supported would have caught all of this at integration time — silent no-ops are the worst failure mode for a haptics SDK.

---

## Steps to reproduce

1. Install the official Pulsar demo app on the affected device:
   https://play.google.com/store/apps/details?id=com.swmansion.pulsar.app
2. Open the **Presets** screen. Trigger each preset and note which ones produce a haptic.
3. **Result:** alarm, anvil, applause, bloom, and similar presets are felt. afterglow, aftershock, and all `system*` presets produce no haptic at all.
4. Open each demo screen (Sliders, Buttons, Countdown, Boolean loader, Notification, Accelerator).
5. **Result:** only Notification fires haptic feedback; the others are silent.
6. In-app repro:
   ```kotlin
   val presets = Pulsar(context).getPresets()
   presets.systemKeyboardTap() // silent
   presets.bloom()             // felt
   ```

---

## Link to a repository

*(No standalone repro repo. Minimal usage shown below — drop into any empty Compose project.)*

**`app/build.gradle.kts`**
```kotlin
dependencies {
    implementation("com.swmansion:pulsar:1.1.0")
}
```

**`AndroidManifest.xml`**
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

**`MainActivity.kt`**
```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swmansion.pulsar.Pulsar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val presets = remember { Pulsar(this@MainActivity).getPresets() }
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = { presets.systemKeyboardTap() }) {
                            Text("systemKeyboardTap (SILENT on Honor 400 Pro)")
                        }
                        Button(onClick = { presets.bloom() }) {
                            Text("bloom (felt)")
                        }
                        Button(onClick = { presets.systemImpactMedium() }) {
                            Text("systemImpactMedium (SILENT)")
                        }
                        Button(onClick = { presets.afterglow() }) {
                            Text("afterglow (SILENT)")
                        }
                        Button(onClick = { presets.alarm() }) {
                            Text("alarm (felt)")
                        }
                    }
                }
            }
        }
    }
}
```

Tapping `bloom` and `alarm` produces a clear haptic on Honor 400 Pro. Tapping `systemKeyboardTap`, `systemImpactMedium`, `afterglow` (and the other `system*` / amplitude-scaled presets listed above) produces nothing — no exception, no warning in logcat, just silence.

---

## SDK

`Android`

## Pulsar version

`1.1.0`

## Platform

`Android`

## React Native version

*(leave blank — not using RN SDK)*

## Build type

`debug`

## Device model

`Honor 400 Pro, Android 16, MagicOS 10.0.0.151(C185E3R2P3)`

## Host machine

`Windows 11`

---

## Additional information

- VIBRATE permission is declared in the manifest.
- Tested with the device **not** in silent / DND mode; vibration master toggle is on. Other apps (system keyboard, Settings → Sound → Vibration & haptics test) vibrate correctly with the same kind of effects.

**MagicOS haptic toggle** — **TODO**: open Settings → Sounds & vibration → Vibration & haptics (path may vary slightly by MagicOS version). Note the state of any "System haptics" / "Touch feedback" / "Keyboard haptics" toggles. If turning them on makes Pulsar's `system*` presets fire, that's the smoking gun — attach a screenshot. If they're already on and Pulsar is still silent, that's the stronger bug (vendor ROM isn't honoring its own setting for third-party apps).

**Capability probe output** — **TODO**: paste the file dump produced by the app at startup.

Note: MagicOS encrypts `adb logcat` output by default (lines look like `(HKS)…(HKE)` blobs), so the probe also writes a plaintext file to app-external storage. Pull it like this:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n com.gaddal.materialcalculator/dev.gaddal.sifr.MainActivity
# wait ~2s for app to start, then:
adb pull /sdcard/Android/data/com.gaddal.materialcalculator/files/pulsar-probe.txt docs\pulsar-probe.txt
```

Probe source: `app/src/main/java/dev/gaddal/sifr/core/ui/feedback/VibratorCapabilityProbe.kt`. Fields reported: `hasVibrator`, `hasAmplitudeControl`, `areEffectsSupported` for `EFFECT_{CLICK, DOUBLE_CLICK, HEAVY_CLICK, TICK}`, `arePrimitivesSupported` for `PRIMITIVE_{CLICK, TICK, LOW_TICK, QUICK_RISE, SLOW_RISE, QUICK_FALL, SPIN, THUD}`, and `Settings.System.HAPTIC_FEEDBACK_ENABLED`.

**Workaround in my own integration**: substituted `presets.systemKeyboardTap()` → `presets.bloom()`. `bloom()` works because it ships with a `rawContinuousPattern` channel as well as discrete primitives, which the engine apparently falls back to when primitives aren't honored.
