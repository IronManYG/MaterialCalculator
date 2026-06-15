# Tools — Foundation, Domain & Currency Data Layer (Plan 1 of 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the headless half of the Tools feature — build/dependency setup, the four pure converters (units, date, tip, number-format), and the live-FX currency data layer (Ktor fetch → DataStore cache → bundled seed fallback) — all unit-tested, with **no UI**. Plan 2 (presentation) builds on the types defined here.

**Architecture:** Pure-Kotlin domain (`feature/tools/domain/`) with zero Android deps for the converters; an offline-first currency data layer (`feature/tools/data/`) behind a `CurrencyRepository` interface that exposes `Flow<RatesResource>` and never throws — network failures downgrade to cached or bundled-seed rates. Follows the project's existing layered + Koin-DI conventions; reuses the existing `DataStore<Preferences>` and `Result<D,E>`/`Error` types.

**Tech Stack:** Kotlin, Ktor client (Android engine) + `kotlinx-serialization-json`, AndroidX DataStore Preferences, Koin, `java.time` via core-library desugaring, JUnit4 + Google Truth + Turbine + coroutines-test.

**Branch:** `feature/tools-module` (already checked out; the spec commit `eafcb98` is here). Commit per task (pre-authorized). Do **not** merge.

**Conventions (hard rules):**
- Build with `sh gradlew` — never `./gradlew` (flips the exec bit). Never `git add gradlew`.
- Tests: JUnit4 (`org.junit.Test`) + Google Truth (`assertThat(x).isEqualTo(y)`), **not** JUnit assertions.
- Conventional Commits, **no** `Co-Authored-By` / AI footer.
- **Any version literal** must come from the `dependency-version-lookup` skill — do not invent versions.
- New files: `git add` immediately.

**Spec:** `docs/superpowers/specs/2026-06-14-tools-module-design.md` (§5 packages, §6 currency data layer, §10 testing). Prototype source of truth: `docs/design_handoff_sifr_redesign/reference_prototype/app/tools.jsx` (`SIFR_UNITS`, `SIFR_FX`, `sifrConvert`) and `engine.js` (`format`).

---

## File Structure

Created in this plan:

```
app/src/main/java/dev/gaddal/sifr/feature/tools/
  domain/
    UnitCategory.kt          // enum + per-category unit tables (from SIFR_UNITS)
    UnitConverter.kt         // convert(category, value, from, to); Temp special-cased
    TipCalculator.kt         // pure tip/total/each
    DateCalculator.kt        // daysBetween, addDays (java.time)
    Rates.kt                 // RatesSnapshot, RatesResource
    CurrencyError.kt         // enum : Error
    CurrencyRepository.kt     // interface: rates Flow + refresh()
    SeedRatesProvider.kt      // interface (fakeable)
  data/
    dto/RatesDto.kt          // open.er-api response shape (@Serializable)
    CurrencyApi.kt           // interface + KtorCurrencyApi
    RatesCache.kt            // DataStore read/write of last snapshot (JSON pref)
    AssetSeedRatesProvider.kt// reads assets/currency_seed.json
    CurrencyRepositoryImpl.kt// fetch → cache → seed orchestration
  di/ToolsModule.kt          // Koin: HttpClient, Json, Api, Cache, Seed, Repository
app/src/main/java/dev/gaddal/sifr/core/domain/util/SifrNumberFormat.kt  // shared number formatter
app/src/main/assets/currency_seed.json                                   // bundled seed (SIFR_FX)

app/src/test/java/dev/gaddal/sifr/feature/tools/
  domain/UnitConverterTest.kt
  domain/TipCalculatorTest.kt
  domain/DateCalculatorTest.kt
  data/CurrencyRepositoryImplTest.kt
app/src/test/java/dev/gaddal/sifr/core/domain/util/SifrNumberFormatTest.kt
```

Modified: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, the three `Constants.kt` variants, `app/src/main/java/dev/gaddal/sifr/di/AppModule.kt`.

---

## Task 1: Add dependencies & enable desugaring

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts:93-96` (compileOptions) and `:122-183` (dependencies)

- [ ] **Step 1: Look up current stable versions**

Use the `dependency-version-lookup` skill to get the latest **stable** versions of:
- `io.ktor:ktor-client-core`, `io.ktor:ktor-client-android`, `io.ktor:ktor-client-content-negotiation`, `io.ktor:ktor-serialization-kotlinx-json` (one Ktor version for all four)
- `org.jetbrains.kotlinx:kotlinx-serialization-json` (match the existing `kotlinxSerialization = "1.11.0"` unless a newer stable exists)
- `com.android.tools:desugar_jdk_libs`

Record them; use them verbatim below (shown as `<ktor>`, `<serialization-json>`, `<desugar>`).

- [ ] **Step 2: Add catalog versions**

In `gradle/libs.versions.toml`, under `[versions]` add (place near the existing `kotlinxSerialization` line):

```toml
# Networking
ktor = "<ktor>"
desugarJdkLibs = "<desugar>"
```

If `kotlinx-serialization-json` needs a different version than `kotlinxSerialization`, add `kotlinxSerializationJson = "<serialization-json>"`; otherwise reuse `kotlinxSerialization`.

- [ ] **Step 3: Add catalog libraries**

Under `[libraries]`, after the `# Serialization` block:

```toml
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

# Networking (Ktor)
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-android = { group = "io.ktor", name = "ktor-client-android", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }

# Desugaring
desugar-jdk-libs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
```

- [ ] **Step 4: Enable desugaring in `app/build.gradle.kts`**

Replace the `compileOptions` block (lines 93-96):

```kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
```

- [ ] **Step 5: Add the dependencies**

In the `dependencies { }` block, after the `// Serialization` line (`:162`):

```kotlin
    // Serialization (for @Serializable on Nav3 routes + Tools JSON)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // Networking — Ktor (Tools currency rates; the app's first network call)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // java.time on minSdk 24
    coreLibraryDesugaring(libs.desugar.jdk.libs)
```

(Leave the existing `kotlinx.serialization.core` line as-is if already present; do not duplicate it — just add the `json` + Ktor + desugaring lines.)

- [ ] **Step 6: Sync & build**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (no new code yet — this just proves the catalog + deps resolve).

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build(tools): add Ktor + serialization-json + core-library desugaring"
```

---

## Task 2: INTERNET permission

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` (after the `VIBRATE` line)

- [ ] **Step 1: Add the permission**

After `<uses-permission android:name="android.permission.VIBRATE" />`:

```xml
    <uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 2: Build**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(tools): declare INTERNET permission for currency rates"
```

---

## Task 3: Point BASE_URL at open.er-api

**Files:**
- Modify: `app/src/debug/java/dev/gaddal/sifr/Constants.kt`
- Modify: `app/src/staging/java/dev/gaddal/sifr/Constants.kt`
- Modify: `app/src/release/java/dev/gaddal/sifr/Constants.kt`

- [ ] **Step 1: Set the URL in all three variants**

Each file's body becomes (identical across variants — the rates source is the same in debug/staging/release):

```kotlin
package dev.gaddal.sifr

object Constants {
    const val BASE_URL = "https://open.er-api.com/v6/"
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/debug/java/dev/gaddal/sifr/Constants.kt app/src/staging/java/dev/gaddal/sifr/Constants.kt app/src/release/java/dev/gaddal/sifr/Constants.kt
git commit -m "config(tools): set BASE_URL to open.er-api currency endpoint"
```

---

## Task 4: Shared number formatter (`SifrNumberFormat`)

Mirrors `ExpressionWriter.formatResult` semantics (`ExpressionWriter.kt:345-396`): snap computational-zero to `"0"`, scientific notation outside a magnitude band, otherwise fixed with a significant-digit budget and trailing-zero trim, all `Locale.ROOT` (Eastern-Arabic numerals come from font shaping downstream, exactly as the calculator does). Public + reusable so Tools formats identically to the calculator.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/core/domain/util/SifrNumberFormat.kt`
- Test: `app/src/test/java/dev/gaddal/sifr/core/domain/util/SifrNumberFormatTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.gaddal.sifr.core.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SifrNumberFormatTest {

    @Test
    fun `integer value has no decimal point`() {
        assertThat(SifrNumberFormat.format(42.0)).isEqualTo("42")
    }

    @Test
    fun `trailing zeros are trimmed`() {
        assertThat(SifrNumberFormat.format(1.5000)).isEqualTo("1.5")
    }

    @Test
    fun `exact zero formats as 0`() {
        assertThat(SifrNumberFormat.format(0.0)).isEqualTo("0")
    }

    @Test
    fun `computational zero snaps to 0`() {
        assertThat(SifrNumberFormat.format(1e-16)).isEqualTo("0")
    }

    @Test
    fun `very large magnitude uses scientific notation`() {
        assertThat(SifrNumberFormat.format(1e20)).contains("E")
    }

    @Test
    fun `negative value keeps its sign`() {
        assertThat(SifrNumberFormat.format(-3.25)).isEqualTo("-3.25")
    }

    @Test
    fun `non-finite returns empty string`() {
        assertThat(SifrNumberFormat.format(Double.NaN)).isEqualTo("")
    }

    @Test
    fun `known conversion value formats cleanly`() {
        // 100 m -> ft = 100 / 0.3048
        assertThat(SifrNumberFormat.format(100.0 / 0.3048)).isEqualTo("328.0839895013")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.core.domain.util.SifrNumberFormatTest"`
Expected: FAIL (unresolved reference `SifrNumberFormat`).

- [ ] **Step 3: Implement**

```kotlin
package dev.gaddal.sifr.core.domain.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10

/**
 * Locale-independent number formatting for Tools, mirroring the calculator's
 * [dev.gaddal.sifr.feature.calculator.domain] result formatter so converter
 * outputs read identically to the keypad's results. Western digits + Locale.ROOT;
 * Eastern-Arabic numerals are produced downstream by font shaping, not here.
 */
object SifrNumberFormat {

    private const val MAX_SIG_DIGITS = 12
    private const val SNAP_TO_ZERO_THRESHOLD = 1e-12
    private const val SCI_UPPER_THRESHOLD = 1e15
    private const val SCI_LOWER_THRESHOLD = 1e-9

    fun format(value: Double): String {
        if (value.isNaN()) return ""
        if (!value.isFinite()) return "∞" // ∞
        if (value == 0.0) return "0"

        val absValue = abs(value)
        if (absValue < SNAP_TO_ZERO_THRESHOLD) return "0"

        return if (absValue >= SCI_UPPER_THRESHOLD || absValue < SCI_LOWER_THRESHOLD) {
            formatScientific(value)
        } else {
            formatFixed(value, absValue)
        }
    }

    private fun formatFixed(value: Double, absValue: Double): String {
        val fractionDigits = if (absValue >= 1.0) {
            val integerDigits = floor(log10(absValue)).toInt() + 1
            (MAX_SIG_DIGITS - integerDigits).coerceAtLeast(0)
        } else {
            val leadingZeros = -floor(log10(absValue)).toInt() - 1
            leadingZeros + MAX_SIG_DIGITS
        }
        val raw = String.format(Locale.ROOT, "%.${fractionDigits}f", value)
        return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
    }

    private fun formatScientific(value: Double): String {
        val raw = String.format(Locale.ROOT, "%.${MAX_SIG_DIGITS - 1}e", value)
        val eIndex = raw.indexOfAny(charArrayOf('e', 'E'))
        val mantissaRaw = raw.substring(0, eIndex)
        val exponent = raw.substring(eIndex + 1).toInt()
        val mantissa = if (mantissaRaw.contains('.')) mantissaRaw.trimEnd('0').trimEnd('.') else mantissaRaw
        return "${mantissa}E$exponent"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.core.domain.util.SifrNumberFormatTest"`
Expected: PASS. (If the `known conversion value` assertion needs adjusting, run once and copy the actual `SifrNumberFormat.format(100.0/0.3048)` output into the assertion — it is deterministic.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/core/domain/util/SifrNumberFormat.kt app/src/test/java/dev/gaddal/sifr/core/domain/util/SifrNumberFormatTest.kt
git commit -m "feat(tools): add shared SifrNumberFormat mirroring the calculator formatter"
```

---

## Task 5: Unit categories & converter

Factors are SI-relative, verbatim from `tools.jsx → SIFR_UNITS`. Temp is special-cased (°C/°F/K), exactly as `sifrConvert`.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/UnitCategory.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/UnitConverter.kt`
- Test: `app/src/test/java/dev/gaddal/sifr/feature/tools/domain/UnitConverterTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `length 100 m to ft`() {
        val result = UnitConverter.convert(UnitCategory.Length, 100.0, "m", "ft")
        assertThat(result).isWithin(1e-6).of(328.0839895013123)
    }

    @Test
    fun `length round trips`() {
        val ft = UnitConverter.convert(UnitCategory.Length, 1.0, "km", "ft")
        val km = UnitConverter.convert(UnitCategory.Length, ft, "ft", "km")
        assertThat(km).isWithin(1e-9).of(1.0)
    }

    @Test
    fun `weight 1 kg to lb`() {
        val result = UnitConverter.convert(UnitCategory.Weight, 1.0, "kg", "lb")
        assertThat(result).isWithin(1e-6).of(2.2046226218)
    }

    @Test
    fun `temp 100 C to F`() {
        val result = UnitConverter.convert(UnitCategory.Temp, 100.0, "°C", "°F")
        assertThat(result).isWithin(1e-9).of(212.0)
    }

    @Test
    fun `temp 32 F to C`() {
        val result = UnitConverter.convert(UnitCategory.Temp, 32.0, "°F", "°C")
        assertThat(result).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `temp 0 C to K`() {
        val result = UnitConverter.convert(UnitCategory.Temp, 0.0, "°C", "K")
        assertThat(result).isWithin(1e-9).of(273.15)
    }

    @Test
    fun `data 1 GB to MB decimal`() {
        val result = UnitConverter.convert(UnitCategory.Data, 1.0, "GB", "MB")
        assertThat(result).isWithin(1e-6).of(1000.0)
    }

    @Test
    fun `data 1 KiB to B binary`() {
        val result = UnitConverter.convert(UnitCategory.Data, 1.0, "KiB", "B")
        assertThat(result).isWithin(1e-6).of(1024.0)
    }

    @Test
    fun `units for category come from the table in declared order`() {
        assertThat(UnitCategory.Length.units).containsExactly(
            "m", "km", "cm", "mm", "mi", "ft", "in", "yd",
        ).inOrder()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.domain.UnitConverterTest"`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implement `UnitCategory.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

/**
 * Unit-conversion categories and their SI-relative factor tables, verbatim from
 * the prototype `tools.jsx → SIFR_UNITS`. [units] preserves declaration order
 * (the picker shows them in this order; category-change resets to the first two).
 * Temp carries no factors here — it is special-cased in [UnitConverter].
 */
enum class UnitCategory(val factors: Map<String, Double>) {
    Length(
        linkedMapOf(
            "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001,
            "mi" to 1609.344, "ft" to 0.3048, "in" to 0.0254, "yd" to 0.9144,
        ),
    ),
    Weight(
        linkedMapOf(
            "kg" to 1.0, "g" to 0.001, "mg" to 0.000001,
            "lb" to 0.45359237, "oz" to 0.0283495, "t" to 1000.0,
        ),
    ),
    Temp(linkedMapOf("°C" to 1.0, "°F" to 1.0, "K" to 1.0)),
    Data(
        linkedMapOf(
            "B" to 1.0, "KB" to 1e3, "MB" to 1e6, "GB" to 1e9, "TB" to 1e12,
            "KiB" to 1024.0, "MiB" to 1048576.0, "GiB" to 1073741824.0,
        ),
    ),
    ;

    val units: List<String> get() = factors.keys.toList()
}
```

- [ ] **Step 4: Implement `UnitConverter.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

/**
 * Pure unit conversion, mirroring the prototype `sifrConvert`. Linear categories
 * convert through their SI-relative factor; Temp is an affine special case.
 */
object UnitConverter {

    fun convert(category: UnitCategory, value: Double, from: String, to: String): Double {
        if (category == UnitCategory.Temp) return convertTemp(value, from, to)
        val factors = category.factors
        val fromFactor = factors[from] ?: return Double.NaN
        val toFactor = factors[to] ?: return Double.NaN
        return value * fromFactor / toFactor
    }

    private fun convertTemp(value: Double, from: String, to: String): Double {
        // Normalise to Celsius first, then out.
        val celsius = when (from) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            else -> value - 273.15 // K
        }
        return when (to) {
            "°C" -> celsius
            "°F" -> celsius * 9.0 / 5.0 + 32.0
            else -> celsius + 273.15 // K
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.domain.UnitConverterTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/domain/UnitCategory.kt app/src/main/java/dev/gaddal/sifr/feature/tools/domain/UnitConverter.kt app/src/test/java/dev/gaddal/sifr/feature/tools/domain/UnitConverterTest.kt
git commit -m "feat(tools): unit categories + converter (length/weight/temp/data)"
```

---

## Task 6: Tip calculator

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/TipCalculator.kt`
- Test: `app/src/test/java/dev/gaddal/sifr/feature/tools/domain/TipCalculatorTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TipCalculatorTest {

    @Test
    fun `tip total and each for a typical bill`() {
        val r = TipCalculator.compute(bill = 86.0, tipPercent = 15, split = 2)
        assertThat(r.tip).isWithin(1e-9).of(12.9)
        assertThat(r.total).isWithin(1e-9).of(98.9)
        assertThat(r.each).isWithin(1e-9).of(49.45)
    }

    @Test
    fun `split is floored at 1`() {
        val r = TipCalculator.compute(bill = 100.0, tipPercent = 10, split = 0)
        assertThat(r.each).isWithin(1e-9).of(110.0)
    }

    @Test
    fun `zero bill yields zero everything`() {
        val r = TipCalculator.compute(bill = 0.0, tipPercent = 20, split = 3)
        assertThat(r.tip).isEqualTo(0.0)
        assertThat(r.total).isEqualTo(0.0)
        assertThat(r.each).isEqualTo(0.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.domain.TipCalculatorTest"`
Expected: FAIL.

- [ ] **Step 3: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

/** Pure tip arithmetic (prototype: tip = bill*pct/100, total = bill+tip, each = total/split). */
object TipCalculator {

    data class TipResult(val tip: Double, val total: Double, val each: Double)

    fun compute(bill: Double, tipPercent: Int, split: Int): TipResult {
        val safeSplit = split.coerceAtLeast(1)
        val tip = bill * tipPercent / 100.0
        val total = bill + tip
        return TipResult(tip = tip, total = total, each = total / safeSplit)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.domain.TipCalculatorTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/domain/TipCalculator.kt app/src/test/java/dev/gaddal/sifr/feature/tools/domain/TipCalculatorTest.kt
git commit -m "feat(tools): tip calculator (tip/total/each)"
```

---

## Task 7: Date calculator

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/DateCalculator.kt`
- Test: `app/src/test/java/dev/gaddal/sifr/feature/tools/domain/DateCalculatorTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class DateCalculatorTest {

    @Test
    fun `days between two dates is signed difference`() {
        val d = DateCalculator.daysBetween(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 8, 1))
        assertThat(d).isEqualTo(52L)
    }

    @Test
    fun `days between is negative when end precedes start`() {
        val d = DateCalculator.daysBetween(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 6, 10))
        assertThat(d).isEqualTo(-52L)
    }

    @Test
    fun `days between spans leap day`() {
        val d = DateCalculator.daysBetween(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 1))
        assertThat(d).isEqualTo(2L) // 2024 is a leap year (Feb 29 exists)
    }

    @Test
    fun `add days crosses month and year boundaries`() {
        val r = DateCalculator.addDays(LocalDate.of(2026, 12, 20), 90)
        assertThat(r).isEqualTo(LocalDate.of(2027, 3, 20))
    }

    @Test
    fun `weeks and remainder breakdown`() {
        val (weeks, days) = DateCalculator.weeksAndDays(52)
        assertThat(weeks).isEqualTo(7)
        assertThat(days).isEqualTo(3)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.domain.DateCalculatorTest"`
Expected: FAIL.

- [ ] **Step 3: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Pure date arithmetic over java.time (desugared on minSdk 24). */
object DateCalculator {

    /** Signed day count from [start] to [end] (negative if [end] precedes [start]). */
    fun daysBetween(start: LocalDate, end: LocalDate): Long =
        ChronoUnit.DAYS.between(start, end)

    fun addDays(date: LocalDate, days: Long): LocalDate = date.plusDays(days)

    /** Splits an absolute day count into whole weeks + remainder days (both non-negative). */
    fun weeksAndDays(totalDays: Long): Pair<Int, Int> {
        val abs = abs(totalDays)
        return (abs / 7).toInt() to (abs % 7).toInt()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.domain.DateCalculatorTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/domain/DateCalculator.kt app/src/test/java/dev/gaddal/sifr/feature/tools/domain/DateCalculatorTest.kt
git commit -m "feat(tools): date calculator (daysBetween/addDays/weeksAndDays)"
```

---

## Task 8: Currency domain types

Defines the snapshot, the resource wrapper, the error, the repository contract, and the seed-provider interface. No implementation yet.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/Rates.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/CurrencyError.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/CurrencyRepository.kt`
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/domain/SeedRatesProvider.kt`

- [ ] **Step 1: Implement `Rates.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import java.time.LocalDate

/**
 * A set of exchange rates expressed per one unit of [base] (always "USD" from
 * open.er-api). [asOf] is the provider's last-update date. Convert A→B as
 * value / rates[A] * rates[B].
 */
data class RatesSnapshot(
    val base: String,
    val rates: Map<String, Double>,
    val asOf: LocalDate,
) {
    /** Currency codes available in this snapshot, sorted alphabetically. */
    val currencies: List<String> get() = rates.keys.sorted()
}

/**
 * The currency rates as seen by the UI. Conversion never blocks: failures
 * downgrade to cached ([Success] with stale=true) or bundled ([SeedFallback]).
 */
sealed interface RatesResource {
    data object Loading : RatesResource
    data class Success(val snapshot: RatesSnapshot, val stale: Boolean) : RatesResource
    data class SeedFallback(val snapshot: RatesSnapshot) : RatesResource

    /** The snapshot to convert with, if any (Loading has none). */
    val snapshotOrNull: RatesSnapshot?
        get() = when (this) {
            is Success -> snapshot
            is SeedFallback -> snapshot
            Loading -> null
        }
}
```

- [ ] **Step 2: Implement `CurrencyError.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import dev.gaddal.sifr.core.domain.util.Error

enum class CurrencyError : Error {
    NO_NETWORK,
    SERVER_ERROR,
    SERIALIZATION,
    UNKNOWN,
}
```

- [ ] **Step 3: Implement `CurrencyRepository.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

import kotlinx.coroutines.flow.Flow

/**
 * Offline-first currency rates. [rates] serves cache or seed immediately and
 * updates after a successful refresh. [refresh] is safe to call repeatedly;
 * it no-ops the network hit when the cache is fresh unless [force] is set.
 */
interface CurrencyRepository {
    val rates: Flow<RatesResource>
    suspend fun refresh(force: Boolean = false)
}
```

- [ ] **Step 4: Implement `SeedRatesProvider.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.domain

/** Supplies the bundled fallback snapshot (read from assets in production; fakeable in tests). */
fun interface SeedRatesProvider {
    fun seed(): RatesSnapshot
}
```

- [ ] **Step 5: Build (compile check)**

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/domain/Rates.kt app/src/main/java/dev/gaddal/sifr/feature/tools/domain/CurrencyError.kt app/src/main/java/dev/gaddal/sifr/feature/tools/domain/CurrencyRepository.kt app/src/main/java/dev/gaddal/sifr/feature/tools/domain/SeedRatesProvider.kt
git commit -m "feat(tools): currency domain types (snapshot, resource, repository contract)"
```

---

## Task 9: Bundled seed asset

The minimal offline fallback — the prototype's `SIFR_FX` (10 currencies, Gulf-inclusive), stamped with a seed date.

**Files:**
- Create: `app/src/main/assets/currency_seed.json`

- [ ] **Step 1: Create the assets dir + file**

```bash
mkdir -p app/src/main/assets
```

Write `app/src/main/assets/currency_seed.json`:

```json
{
  "base": "USD",
  "asOf": "2026-06-10",
  "rates": {
    "USD": 1.0,
    "EUR": 0.87,
    "GBP": 0.75,
    "SAR": 3.75,
    "AED": 3.67,
    "EGP": 49.2,
    "KWD": 0.31,
    "JPY": 152.0,
    "INR": 84.2,
    "TRY": 38.5
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/currency_seed.json
git commit -m "feat(tools): bundle currency seed snapshot (SIFR_FX fallback)"
```

---

## Task 10: Rates DTO

open.er-api `GET latest/USD` response shape (only the fields we read; `ignoreUnknownKeys` covers the rest).

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/data/dto/RatesDto.kt`

- [ ] **Step 1: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * open.er-api.com `/v6/latest/USD` response. `result` is "success" or "error";
 * `time_last_update_unix` is the rate epoch (seconds); `rates` is per-USD.
 */
@Serializable
data class RatesDto(
    val result: String,
    @SerialName("base_code") val baseCode: String? = null,
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long = 0L,
    val rates: Map<String, Double> = emptyMap(),
)
```

- [ ] **Step 2: Build**

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (verifies the serialization plugin processes `@Serializable`).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/data/dto/RatesDto.kt
git commit -m "feat(tools): rates DTO for open.er-api response"
```

---

## Task 11: Currency API (Ktor)

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/data/CurrencyApi.kt`

- [ ] **Step 1: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.data

import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.feature.tools.data.dto.RatesDto
import dev.gaddal.sifr.feature.tools.domain.CurrencyError
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import java.time.Instant
import java.time.ZoneOffset

/** Fetches the latest rates. Never throws — maps every failure to a typed [CurrencyError]. */
interface CurrencyApi {
    suspend fun fetchLatest(base: String = "USD"): Result<RatesSnapshot, CurrencyError>
}

class KtorCurrencyApi(
    private val client: HttpClient,
    private val baseUrl: String, // Constants.BASE_URL, e.g. https://open.er-api.com/v6/
) : CurrencyApi {

    override suspend fun fetchLatest(base: String): Result<RatesSnapshot, CurrencyError> {
        return try {
            val response: HttpResponse = client.get("${baseUrl}latest/$base")
            if (!response.status.isSuccess()) {
                return Result.Error(CurrencyError.SERVER_ERROR)
            }
            val dto: RatesDto = response.body()
            if (dto.result != "success" || dto.rates.isEmpty()) {
                return Result.Error(CurrencyError.SERVER_ERROR)
            }
            val asOf = Instant.ofEpochSecond(dto.timeLastUpdateUnix)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            Result.Success(
                RatesSnapshot(
                    base = dto.baseCode ?: base,
                    rates = dto.rates,
                    asOf = asOf,
                ),
            )
        } catch (e: IOException) {
            Result.Error(CurrencyError.NO_NETWORK)
        } catch (e: SerializationException) {
            Result.Error(CurrencyError.SERIALIZATION)
        } catch (e: Exception) {
            Result.Error(CurrencyError.UNKNOWN)
        }
    }
}
```

> Note: confirm the Ktor IOException import during implementation — Ktor 3.x uses `kotlinx.io.IOException`; if the resolved Ktor version throws `java.io.IOException`, import that instead. Keep the three-tier catch (network / serialization / unknown).

- [ ] **Step 2: Build**

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/data/CurrencyApi.kt
git commit -m "feat(tools): Ktor currency API with typed errors"
```

---

## Task 12: Rates cache (DataStore)

Persists the last successful snapshot as a JSON string in the existing `DataStore<Preferences>` (reused; distinct keys), plus a fetched-at timestamp for staleness.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/data/RatesCache.kt`

- [ ] **Step 1: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** A cached snapshot + when it was fetched (epoch millis), serialized to one pref string. */
@Serializable
private data class CachedRates(
    val base: String,
    val rates: Map<String, Double>,
    val asOfEpochDay: Long,
    val fetchedAtEpochMs: Long,
)

data class CacheEntry(val snapshot: RatesSnapshot, val fetchedAtEpochMs: Long)

class RatesCache(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    suspend fun read(): CacheEntry? {
        val raw = dataStore.data.first()[KEY_RATES_JSON] ?: return null
        return runCatching {
            val c = json.decodeFromString<CachedRates>(raw)
            CacheEntry(
                snapshot = RatesSnapshot(
                    base = c.base,
                    rates = c.rates,
                    asOf = LocalDate.ofEpochDay(c.asOfEpochDay),
                ),
                fetchedAtEpochMs = c.fetchedAtEpochMs,
            )
        }.getOrNull()
    }

    suspend fun write(snapshot: RatesSnapshot, fetchedAtEpochMs: Long) {
        val payload = json.encodeToString(
            CachedRates(
                base = snapshot.base,
                rates = snapshot.rates,
                asOfEpochDay = snapshot.asOf.toEpochDay(),
                fetchedAtEpochMs = fetchedAtEpochMs,
            ),
        )
        dataStore.edit { it[KEY_RATES_JSON] = payload; it[KEY_RATES_FETCHED_AT] = fetchedAtEpochMs }
    }

    private companion object {
        val KEY_RATES_JSON = stringPreferencesKey("tools_rates_json")
        val KEY_RATES_FETCHED_AT = longPreferencesKey("tools_rates_fetched_at")
    }
}
```

- [ ] **Step 2: Build**

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/data/RatesCache.kt
git commit -m "feat(tools): DataStore-backed rates cache"
```

---

## Task 13: Asset seed provider

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/data/AssetSeedRatesProvider.kt`

- [ ] **Step 1: Implement**

```kotlin
package dev.gaddal.sifr.feature.tools.data

import android.content.Context
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
private data class SeedDto(val base: String, val asOf: String, val rates: Map<String, Double>)

/** Reads the bundled `assets/currency_seed.json` once and caches the parse. */
class AssetSeedRatesProvider(
    private val context: Context,
    private val json: Json,
) : SeedRatesProvider {

    private val cached: RatesSnapshot by lazy {
        val raw = context.assets.open("currency_seed.json").bufferedReader().use { it.readText() }
        val dto = json.decodeFromString<SeedDto>(raw)
        RatesSnapshot(base = dto.base, rates = dto.rates, asOf = LocalDate.parse(dto.asOf))
    }

    override fun seed(): RatesSnapshot = cached
}
```

- [ ] **Step 2: Build**

Run: `sh gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/data/AssetSeedRatesProvider.kt
git commit -m "feat(tools): asset-backed seed rates provider"
```

---

## Task 14: Currency repository (orchestration) + tests

The offline-first brain: serve cache or seed immediately, refresh in the background, downgrade gracefully. Fully unit-tested with fakes.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/data/CurrencyRepositoryImpl.kt`
- Test: `app/src/test/java/dev/gaddal/sifr/feature/tools/data/CurrencyRepositoryImplTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.gaddal.sifr.feature.tools.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.feature.tools.domain.CurrencyError
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.RatesSnapshot
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryImplTest {

    private val seedSnapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("USD" to 1.0, "SAR" to 3.75),
        asOf = LocalDate.of(2026, 6, 10),
    )
    private val liveSnapshot = RatesSnapshot(
        base = "USD",
        rates = mapOf("USD" to 1.0, "SAR" to 3.76, "EUR" to 0.9),
        asOf = LocalDate.of(2026, 6, 14),
    )
    private val seed = SeedRatesProvider { seedSnapshot }

    private class FakeApi(var result: Result<RatesSnapshot, CurrencyError>) : CurrencyApi {
        var calls = 0
        override suspend fun fetchLatest(base: String): Result<RatesSnapshot, CurrencyError> {
            calls++
            return result
        }
    }

    @Test
    fun `first run with empty cache and successful network emits seed then live success`() = runTest {
        val api = FakeApi(Result.Success(liveSnapshot))
        val cache = FakeCache(initial = null)
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 1_000L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            assertThat(awaitItem()).isInstanceOf(RatesResource.SeedFallback::class.java)
            repo.refresh()
            val success = awaitItem() as RatesResource.Success
            assertThat(success.stale).isFalse()
            assertThat(success.snapshot).isEqualTo(liveSnapshot)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(cache.written?.snapshot).isEqualTo(liveSnapshot)
    }

    @Test
    fun `network failure with cache present downgrades to stale success`() = runTest {
        val api = FakeApi(Result.Error(CurrencyError.NO_NETWORK))
        val cache = FakeCache(initial = CacheEntry(liveSnapshot, fetchedAtEpochMs = 0L))
        // now far ahead of fetchedAt so the cache is stale
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 100_000_000L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            val first = awaitItem() as RatesResource.Success
            assertThat(first.snapshot).isEqualTo(liveSnapshot)
            repo.refresh()
            val afterFail = awaitItem() as RatesResource.Success
            assertThat(afterFail.stale).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `network failure with no cache stays on seed fallback`() = runTest {
        val api = FakeApi(Result.Error(CurrencyError.NO_NETWORK))
        val cache = FakeCache(initial = null)
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 1_000L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            assertThat(awaitItem()).isInstanceOf(RatesResource.SeedFallback::class.java)
            repo.refresh()
            // remains seed fallback; no Success emitted
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(api.calls).isEqualTo(1)
    }

    @Test
    fun `fresh cache skips the network unless forced`() = runTest {
        val api = FakeApi(Result.Success(liveSnapshot))
        val cache = FakeCache(initial = CacheEntry(liveSnapshot, fetchedAtEpochMs = 50L))
        // now only slightly ahead of fetchedAt → within freshness window
        val repo = CurrencyRepositoryImpl(api, cache, seed, now = { 100L })

        repo.rates.test {
            assertThat(awaitItem()).isEqualTo(RatesResource.Loading)
            assertThat(awaitItem()).isInstanceOf(RatesResource.Success::class.java)
            repo.refresh(force = false)
            expectNoEvents()
            assertThat(api.calls).isEqualTo(0)
            repo.refresh(force = true)
            assertThat(awaitItem()).isInstanceOf(RatesResource.Success::class.java)
            assertThat(api.calls).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/** In-memory fake of the cache (the real one hits DataStore). */
private class FakeCache(initial: CacheEntry?) : dev.gaddal.sifr.feature.tools.data.RatesCacheContract {
    private var entry: CacheEntry? = initial
    var written: CacheEntry? = null
    override suspend fun read(): CacheEntry? = entry
    override suspend fun write(snapshot: RatesSnapshot, fetchedAtEpochMs: Long) {
        val e = CacheEntry(snapshot, fetchedAtEpochMs)
        entry = e; written = e
    }
}
```

> Testability note: the test fakes the cache via a `RatesCacheContract` interface. In **Step 3** you will extract that interface and make `RatesCache` implement it, so the repository depends on the contract (fakeable), not the DataStore-bound class.

- [ ] **Step 2: Run test to verify it fails**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.data.CurrencyRepositoryImplTest"`
Expected: FAIL (unresolved `CurrencyRepositoryImpl`, `RatesCacheContract`).

- [ ] **Step 3: Extract the cache contract**

Edit `app/src/main/java/dev/gaddal/sifr/feature/tools/data/RatesCache.kt` — add the interface and make the class implement it (keep the `CacheEntry` data class as defined in Task 12):

```kotlin
interface RatesCacheContract {
    suspend fun read(): CacheEntry?
    suspend fun write(snapshot: RatesSnapshot, fetchedAtEpochMs: Long)
}
```

Change the class declaration to `class RatesCache(...) : RatesCacheContract {` and add `override` to `read()` and `write()`.

- [ ] **Step 4: Implement `CurrencyRepositoryImpl.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.data

import dev.gaddal.sifr.core.domain.util.Result
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.RatesResource
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CurrencyRepositoryImpl(
    private val api: CurrencyApi,
    private val cache: RatesCacheContract,
    private val seed: SeedRatesProvider,
    private val freshnessWindowMs: Long = 12 * 60 * 60 * 1000L, // 12h
    private val now: () -> Long = { System.currentTimeMillis() },
) : CurrencyRepository {

    private val _rates = MutableStateFlow<RatesResource>(RatesResource.Loading)
    override val rates = _rates.asStateFlow()

    private val mutex = Mutex()
    private var loadedInitial = false
    private var lastFetchedAt: Long = 0L

    override suspend fun refresh(force: Boolean) = mutex.withLock {
        // Serve cache/seed once, before any network work.
        if (!loadedInitial) {
            loadedInitial = true
            val cached = cache.read()
            if (cached != null) {
                lastFetchedAt = cached.fetchedAtEpochMs
                val stale = now() - cached.fetchedAtEpochMs > freshnessWindowMs
                _rates.value = RatesResource.Success(cached.snapshot, stale = stale)
            } else {
                _rates.value = RatesResource.SeedFallback(seed.seed())
            }
        }

        val haveFreshCache = _rates.value.let {
            it is RatesResource.Success && !it.stale
        }
        if (haveFreshCache && !force) return@withLock

        when (val result = api.fetchLatest()) {
            is Result.Success -> {
                lastFetchedAt = now()
                cache.write(result.data, lastFetchedAt)
                _rates.value = RatesResource.Success(result.data, stale = false)
            }
            is Result.Error -> {
                // Downgrade: keep showing cache (now flagged stale) or stay on seed.
                _rates.value = when (val current = _rates.value) {
                    is RatesResource.Success -> current.copy(stale = true)
                    else -> current // SeedFallback or Loading stays as-is
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `sh gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.feature.tools.data.CurrencyRepositoryImplTest"`
Expected: PASS. (The `rates` StateFlow emits `Loading` first; the seed/cache emission happens inside `refresh()` for the first-run tests, and the test calls `refresh()` to drive it — adjust the test's `awaitItem` ordering to match if the initial serve should be eager. If you prefer eager initial serve, add an `init`-time `refresh()` via an injected scope; for this plan the explicit `refresh()` drive keeps the repo scope-free and the test deterministic.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/data/RatesCache.kt app/src/main/java/dev/gaddal/sifr/feature/tools/data/CurrencyRepositoryImpl.kt app/src/test/java/dev/gaddal/sifr/feature/tools/data/CurrencyRepositoryImplTest.kt
git commit -m "feat(tools): offline-first currency repository (cache→seed fallback) + tests"
```

---

## Task 15: Koin wiring

Provides the HttpClient, Json, API, cache, seed provider, and repository, and registers the module.

**Files:**
- Create: `app/src/main/java/dev/gaddal/sifr/feature/tools/di/ToolsModule.kt`
- Modify: `app/src/main/java/dev/gaddal/sifr/di/AppModule.kt`

- [ ] **Step 1: Implement `ToolsModule.kt`**

```kotlin
package dev.gaddal.sifr.feature.tools.di

import dev.gaddal.sifr.Constants
import dev.gaddal.sifr.feature.tools.data.AssetSeedRatesProvider
import dev.gaddal.sifr.feature.tools.data.CurrencyApi
import dev.gaddal.sifr.feature.tools.data.CurrencyRepositoryImpl
import dev.gaddal.sifr.feature.tools.data.KtorCurrencyApi
import dev.gaddal.sifr.feature.tools.data.RatesCache
import dev.gaddal.sifr.feature.tools.data.RatesCacheContract
import dev.gaddal.sifr.feature.tools.domain.CurrencyRepository
import dev.gaddal.sifr.feature.tools.domain.SeedRatesProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val toolsModule = module {
    single<Json> { Json { ignoreUnknownKeys = true } }

    single<HttpClient> {
        HttpClient(Android) {
            install(ContentNegotiation) { json(get<Json>()) }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
            }
        }
    }

    single<CurrencyApi> { KtorCurrencyApi(client = get(), baseUrl = Constants.BASE_URL) }
    single<RatesCacheContract> { RatesCache(dataStore = get(), json = get()) }
    single<SeedRatesProvider> { AssetSeedRatesProvider(context = androidContext(), json = get()) }
    single<CurrencyRepository> { CurrencyRepositoryImpl(api = get(), cache = get(), seed = get()) }
}
```

> `dataStore = get()` resolves the existing `single<DataStore<Preferences>>` from `coreDataModule`.

- [ ] **Step 2: Register in `AppModule.kt`**

```kotlin
package dev.gaddal.sifr.di

import dev.gaddal.sifr.core.data.di.coreDataModule
import dev.gaddal.sifr.feature.calculator.di.calculatorModule
import dev.gaddal.sifr.feature.history.di.historyModule
import dev.gaddal.sifr.feature.settings.di.settingsModule
import dev.gaddal.sifr.feature.tools.di.toolsModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    coreDataModule,
    calculatorModule,
    settingsModule,
    historyModule,
    toolsModule,
)
```

- [ ] **Step 3: Build**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/gaddal/sifr/feature/tools/di/ToolsModule.kt app/src/main/java/dev/gaddal/sifr/di/AppModule.kt
git commit -m "feat(tools): Koin module — HttpClient, currency API, cache, seed, repository"
```

---

## Task 16: Green checkpoint

- [ ] **Step 1: Full unit-test run**

Run: `sh gradlew :app:testDebugUnitTest`
Expected: PASS (existing 153 + the new Tools domain/data tests; ~165+ total).

- [ ] **Step 2: Lint**

Run: `sh gradlew :app:lintDebug`
Expected: no new errors. (No new strings added in Plan 1, so `MissingTranslation` is unaffected.)

- [ ] **Step 3: Assemble**

Run: `sh gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Confirm clean tree**

Run: `git status --short`
Expected: only the known `.idea/*` + `.notes/` noise; no stray source files uncommitted.

---

## Self-Review (completed)

- **Spec coverage:** §5 packages → Tasks 4–15 create the `feature/tools/domain` + `data` + `di` files (UI deferred to Plan 2). §6 currency layer → Tasks 8–15 (Ktor source, resource flow, 12h cache, seed fallback, typed errors). §10 testing → domain tests (Tasks 5–7), formatter test (Task 4), repository fake-API transitions (Task 14). O2 (desugaring) → Task 1. O3 (first network layer, INTERNET) → Tasks 1–2.
- **Deferred to Plan 2 (presentation):** `ToolTab`/State/Action/Event/VM, nav wiring (`onTools`), UI components, screens, strings, previews, VM tests. The currency-list "frequent group" ordering (O4) lives in the VM (Plan 2); Plan 1's `RatesSnapshot.currencies` is plain alphabetical.
- **Type consistency:** `RatesSnapshot(base, rates, asOf)`, `RatesResource.{Loading,Success(snapshot,stale),SeedFallback(snapshot)}`, `CurrencyError`, `CurrencyApi.fetchLatest`, `RatesCacheContract.{read,write}`, `CacheEntry`, `SeedRatesProvider.seed`, `CurrencyRepository.{rates,refresh(force)}` are used identically across tasks.
- **Placeholders:** none — except deliberately externalized version literals (Task 1) which the `dependency-version-lookup` skill must supply per project policy, and one self-correcting assertion (Task 4 Step 4) that is deterministic.
```
