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

*Note on authorship: this report was drafted with Claude Code (Anthropic's CLI) based on my own hands-on testing on the device described below. I reviewed every claim, ran every command, and confirmed the report's content before submitting.*

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

A. **Capability probe at `Pulsar(context)` construction**, cache results:
  - `vibrator.hasAmplitudeControl()`
  - `vibrator.areAllPrimitivesSupported(PRIMITIVE_CLICK, PRIMITIVE_TICK, PRIMITIVE_LOW_TICK, PRIMITIVE_QUICK_RISE, PRIMITIVE_SLOW_RISE, PRIMITIVE_QUICK_FALL, PRIMITIVE_SPIN, PRIMITIVE_THUD)`
  - `vibrator.areAllEffectsSupported(EFFECT_CLICK, EFFECT_TICK, EFFECT_DOUBLE_CLICK, EFFECT_HEAVY_CLICK)`

B. **Fallback chain for primitive-based presets**: composition primitive (if supported) → amplitude-scaled `createWaveform` derived from the discrete pattern's timing + amplitude → unscaled `createWaveform` → `vibrate(long)` (deprecated, API 24-25). On Honor 400 Pro the chain would land on amplitude-scaled `createWaveform` (since `hasAmplitudeControl=true`), which is what `bloom()` / `alarm()` already do — those are felt.

C. **Fallback chain for system-view-based presets** (`systemKeyboardTap` and friends): try `decorView.performHapticFeedback(constant, FLAG_IGNORE_VIEW_SETTING)`; if that returns `false`, fall through to an equivalent `VibrationEffect.createPredefined(EFFECT_TICK)` (or `EFFECT_CLICK` for `systemContextClick`, `EFFECT_HEAVY_CLICK` for `systemImpactHeavy`, etc.). On Honor 400 Pro this fallback would succeed — predefined effects all report `YES`.

D. **Diagnostics**: `Log.w` on first occurrence per session when a requested effect/primitive isn't supported, and surface a `Pulsar.diagnostics()` API that returns the capability map. Silent no-ops are the worst failure mode for a haptics SDK — every consumer integrating Pulsar today has to write the capability probe I wrote above to figure out which presets will actually fire on their target devices.

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

`https://github.com/IronManYG/MaterialCalculator/tree/feature/phase2.6-feedback-wiring`

The fastest way to reproduce is the **Haptics test screen** baked into the debug build: build & install with `./gradlew :app:installDebug`, open the app, tap the **gear icon → Haptics test (Pulsar diagnostic)**. You'll see 20 named Pulsar presets in two sections — "Expected: felt" (10) and "Expected: silent" (10) — each labeled with its underlying API category (continuous-pattern built-in vs view-based `performHapticFeedback` vs `Composition.addPrimitive`). On Honor 400 Pro every row in the silent section produces nothing, every row in the felt section is clearly perceptible.

Screen source: `app/src/main/java/dev/gaddal/sifr/feature/diag/ui/HapticsTestScreen.kt`. The 20 presets were chosen to cover all three failure paths in this report at ≥2 buttons each.

If you'd rather paste-and-go without cloning, the minimal Compose Activity below reproduces the same behavior:

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

**MagicOS haptic toggles — all ON.** Verified in Settings → Sound & haptics:

- *System haptics* (described as "Haptic feedback for interaction operations and system status"): **on**
- *Back gesture haptics*: **on**
- *Dial pad haptics*: **on**
- *Haptic* (master vibration intensity): **on**

This is the stronger version of the bug — the user has every haptic-related toggle enabled, the framework reports `HAPTIC_FEEDBACK_ENABLED=1`, the device has an LRA vibrator with amplitude control and full predefined-effect support, and Pulsar's `system*` view-based presets *still* produce nothing. The failure isn't a system setting the user can fix — it's in Pulsar's call path.

**Capability probe output** (from Honor 400 Pro / MagicOS 10.0.0.151):

```
===== Vibrator capability probe =====
Device: HONOR DNP-NX9 (DNP-NX9)
Android: 16 (API 36)
Build: DNP-N39 10.0.0.151(C185E3R2P3)
hasVibrator()=true
hasAmplitudeControl()=true
effect CLICK -> YES
effect DOUBLE_CLICK -> YES
effect HEAVY_CLICK -> YES
effect TICK -> YES
primitive CLICK -> false
primitive TICK -> false
primitive LOW_TICK -> false
primitive QUICK_RISE -> false
primitive SLOW_RISE -> false
primitive QUICK_FALL -> false
primitive SPIN -> false
primitive THUD -> false
Settings.System.HAPTIC_FEEDBACK_ENABLED=1 (0=off, 1=on, -1=unset)
===== /probe =====
```

This output is the smoking gun. Three things stand out:

1. **All four predefined effects (`EFFECT_CLICK`, `EFFECT_DOUBLE_CLICK`, `EFFECT_HEAVY_CLICK`, `EFFECT_TICK`) report `YES`.** Pulsar's `systemEffectClick/Tick/HeavyClick/DoubleClick` presets — which route through `VibrationEffect.createPredefined` — should work on this device. (They likely *do*; the surprising-silent presets I observed are the view-based and primitive-based ones, see below.)

2. **All eight composition primitives report `false`** — `PRIMITIVE_CLICK`, `PRIMITIVE_TICK`, `PRIMITIVE_LOW_TICK`, `PRIMITIVE_QUICK_RISE`, `PRIMITIVE_SLOW_RISE`, `PRIMITIVE_QUICK_FALL`, `PRIMITIVE_SPIN`, `PRIMITIVE_THUD`. Every `systemPrimitive*()` preset and every generated preset that lowers to composition primitives (afterglow, aftershock, and ~all the "system" feedback styles in the playground) is silent on this device because Pulsar calls `VibrationEffect.startComposition().addPrimitive(PRIMITIVE_*).compose()` without first calling `vibrator.arePrimitivesSupported(...)`.

3. **`HAPTIC_FEEDBACK_ENABLED=1` and view-based presets are still silent.** The system haptic toggle is **on**, the vibrator is LRA-grade with amplitude control, yet `systemKeyboardTap()` produces nothing. That isolates the failure to the `decorView.performHapticFeedback(...)` path itself — most likely `View.isHapticFeedbackEnabled` is `false` on the decor view (needs `HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING`) or MagicOS's view base class intercepts.

Probe source: `app/src/main/java/dev/gaddal/sifr/core/ui/feedback/VibratorCapabilityProbe.kt`. To reproduce on another device:

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -n com.gaddal.materialcalculator/dev.gaddal.sifr.MainActivity
adb pull /sdcard/Android/data/com.gaddal.materialcalculator/files/pulsar-probe.txt
```

(Note: MagicOS encrypts `adb logcat` output by default — `(HKS)…(HKE)` blobs — so the probe writes a plaintext file to external app storage as a workaround.)

**Workaround in my own integration**: substituted `presets.systemKeyboardTap()` → `presets.bloom()`. `bloom()` works because it ships with a `rawContinuousPattern` channel as well as discrete primitives, which the engine apparently falls back to when primitives aren't honored.
