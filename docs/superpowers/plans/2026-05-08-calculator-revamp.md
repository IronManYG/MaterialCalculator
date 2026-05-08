# Calculator Revamp Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Modernize the published MaterialCalculator app — bump SDK to 36, dependencies to current stables, fix three known bugs, polish display + edge-to-edge — and ship it to Play Store production before the **2026-06-02** dormancy deadline.

**Architecture:** Single Android app module, Groovy `build.gradle`, Compose + Material 3, MVVM with `domain/` + `presentation/` packages. No structural refactor. Changes are localized to: `build.gradle` (root + app), `gradle-wrapper.properties`, four files in `domain/` and `presentation/`, plus four new test cases. No KTS migration, no version catalog, no rebrand.

**Tech Stack:** Kotlin 2.3.10, AGP 8.13.2, Gradle 8.13, JVM 17, Compose BOM 2026.05.00, Material3 1.3.2, JUnit 4 + Google Truth.

**Source spec:** `docs/superpowers/specs/2026-05-08-calculator-revamp-design.md`

---

## File Map

### Created
- `docs/superpowers/plans/2026-05-08-calculator-revamp.md` — this file (already created)

### Modified
- `gradle/wrapper/gradle-wrapper.properties` — bump Gradle 8.7 → 8.13
- `build.gradle` (root) — bump Kotlin 2.0.20 → 2.3.10, AGP 8.5.1 → 8.13.2, drop unused `compose_version` ext
- `app/build.gradle` — `compileSdk`/`targetSdk` 34 → 36, `versionCode` 4 → 5, `versionName` 1.1.0 → 1.2.0; migrate Compose deps to BOM 2026.05.00; bump non-Compose deps; remove duplicate declarations
- `app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt` — add `formatResult` helper, error-state handling on `Calculate`, fix `canEnterOperation` short-circuit bug, error-recovery rule at top of `processAction`
- `app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt` — add 4 new tests
- `app/src/main/java/com/example/materialcalculator/presentation/CalculatorDisplay.kt` — render `"Error"` text in `colorScheme.error`
- `app/src/main/java/com/example/materialcalculator/ui/theme/Theme.kt` — remove deprecated `statusBarColor` `SideEffect` block (incompatible with API 36 mandatory edge-to-edge); dynamic-color logic already exists, untouched
- `app/src/main/java/com/example/materialcalculator/MainActivity.kt` — `enableEdgeToEdge()` in `onCreate`
- `app/src/main/java/com/example/materialcalculator/presentation/CalculatorScreen.kt` — wrap content in `Scaffold` so system-bar insets land cleanly

### Untouched
- `domain/ExpressionParser.kt`, `domain/ExpressionEvaluator.kt`, `domain/ExpressionPart.kt`, `domain/Operation.kt`, `domain/CalculatorAction.kt`
- `presentation/CalculatorViewModel.kt`, `presentation/CalculatorButton.kt`, `presentation/CalculatorButtonGrid.kt`, `presentation/CalculatorActions.kt`, `presentation/CalculatorUiAction.kt`
- `ui/theme/Color.kt`, `ui/theme/Type.kt`
- All XML resources, `AndroidManifest.xml`, `proguard-rules.pro`, `signingConfigs` block, build types

---

## Conventions

- **Tests use Google Truth, not JUnit assertions:** `assertThat(actual).isEqualTo(expected)`.
- **Test method names use Kotlin backticks with spaces:** `` `My descriptive name`() ``. Matches existing files.
- **Operator characters:** multiply is `'x'` (lowercase), divide is `'/'` in the underlying string (`'÷'` only renders in the button label). Don't substitute.
- **Each task ends in one atomic commit.** Commit messages follow the existing repo style — short imperative phrase, no body unless needed.
- **Don't commit pre-staged files you didn't change.** Run `git status` before each commit; if `.idea/*` or other unrelated files are staged, unstage them with `git restore --staged <path>` first.

---

## Pre-flight (before Task 1)

- [ ] Confirm you're on branch `development`: `git branch --show-current` → `development`
- [ ] Confirm working tree is clean of new code-changes from prior sessions (the `.idea/*` and `app/build.gradle` modifications shown by `git status` are user-pre-existing — leave them):
  ```bash
  git status --short
  ```
- [ ] Java 17 active: `java -version` → `openjdk version "17.x.x"` or similar

---

## Task 1: Bump Gradle wrapper, Kotlin, and AGP

**Files:**
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `build.gradle` (root)

- [ ] **Step 1: Bump Gradle wrapper to 8.13**

Replace the `distributionUrl` line in `gradle/wrapper/gradle-wrapper.properties`:

```properties
#Mon Dec 19 11:13:40 AST 2022
distributionBase=GRADLE_USER_HOME
distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip
distributionPath=wrapper/dists
zipStorePath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
```

Only the `distributionUrl` value changes (`gradle-8.7-bin.zip` → `gradle-8.13-bin.zip`).

- [ ] **Step 2: Bump Kotlin and AGP versions in root `build.gradle`**

Replace the entire root `build.gradle` with:

```groovy
buildscript {
    ext {
        kotlin_version = '2.3.10'
        agp_version = '8.13.2'
    }
}// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id 'com.android.application' version "$agp_version" apply false
    id 'com.android.library' version "$agp_version" apply false
    id 'org.jetbrains.kotlin.android' version "$kotlin_version" apply false
    id 'org.jetbrains.kotlin.plugin.compose' version "$kotlin_version"
}
```

Note: `compose_version` ext is **removed**. The Compose BOM (introduced in Task 3) replaces it.

- [ ] **Step 3: Verify Gradle resolves the new toolchain**

Run:

```bash
./gradlew --version
```

Expected: `Gradle 8.13` and `Kotlin 2.3.x` lines (Kotlin appears in `Daemon JVM` info or after first plugin resolution). First run will download the new wrapper distribution — give it a minute.

- [ ] **Step 4: Verify the build still configures (NOT compiles yet — Compose deps still on old pins)**

Run:

```bash
./gradlew help
```

Expected: `BUILD SUCCESSFUL`. If it fails complaining about a Compose Compiler / Kotlin version mismatch, that's expected — Task 3 fixes it.

If it fails for any other reason (missing JDK, wrapper download error), troubleshoot before proceeding.

- [ ] **Step 5: Commit**

```bash
git status --short
# unstage any unrelated pre-staged files first if necessary:
# git restore --staged <path>

git add gradle/wrapper/gradle-wrapper.properties build.gradle
git commit -m "Bump Gradle 8.13, Kotlin 2.3.10, AGP 8.13.2"
```

---

## Task 2: Bump SDK targets and app version

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: Bump `compileSdk`, `targetSdk`, `versionCode`, `versionName`**

In `app/build.gradle`, the `android { ... defaultConfig { ... } }` block currently is:

```groovy
android {
    namespace 'com.example.materialcalculator'
    compileSdk 34

    defaultConfig {
        applicationId "com.gaddal.materialcalculator"
        minSdk 24
        targetSdk 34
        versionCode 4
        versionName "1.1.0"
        ...
    }
```

Change to:

```groovy
android {
    namespace 'com.example.materialcalculator'
    compileSdk 36

    defaultConfig {
        applicationId "com.gaddal.materialcalculator"
        minSdk 24
        targetSdk 36
        versionCode 5
        versionName "1.2.0"
        ...
    }
```

`namespace`, `applicationId`, `minSdk`, `testInstrumentationRunner`, and `vectorDrawables` are unchanged. **Don't change `applicationId`** — it's locked to the Play Store listing.

- [ ] **Step 2: Verify it parses**

```bash
./gradlew :app:tasks --quiet | head -5
```

Expected: prints task list, no error. If it fails because Compose deps don't yet support `compileSdk 36`, wait — Task 3 fixes that. Move on if the failure is dependency-related.

- [ ] **Step 3: Commit**

```bash
git status --short
git add app/build.gradle
git commit -m "Bump compileSdk/targetSdk to 36, version to 1.2.0"
```

---

## Task 3: Migrate to Compose BOM and bump non-Compose deps

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: Replace the entire `dependencies { ... }` block**

Find the existing block (currently at the bottom of `app/build.gradle`, starts with `dependencies {`). Replace it with this exact block:

```groovy
dependencies {

    implementation 'androidx.core:core-ktx:1.18.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.10.0'
    implementation 'androidx.activity:activity-compose:1.13.0'

    // Compose BOM — pins all androidx.compose.* artifacts to a coherent set
    implementation platform('androidx.compose:compose-bom:2026.05.00')
    androidTestImplementation platform('androidx.compose:compose-bom:2026.05.00')

    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    debugImplementation 'androidx.compose.ui:ui-tooling'
    debugImplementation 'androidx.compose.ui:ui-test-manifest'
    androidTestImplementation 'androidx.compose.ui:ui-test-junit4'

    // Material3 pinned outside the BOM — BOM may resolve to a 1.4.0-alpha
    implementation 'androidx.compose.material3:material3:1.3.2'
    implementation 'androidx.compose.material3:material3-window-size-class:1.3.2'

    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0'
    implementation 'com.google.android.material:material:1.13.0'

    // Unit tests
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'androidx.test:core:1.6.1'
    testImplementation 'androidx.arch.core:core-testing:2.2.0'
    testImplementation 'com.google.truth:truth:1.1.3'

    // Instrumented tests
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.6.1'
}
```

Changes vs. before:
- `core-ktx` 1.13.1 → 1.18.0
- `lifecycle-runtime-ktx` 2.8.6 → 2.10.0
- `lifecycle-viewmodel-compose` 2.8.6 → 2.10.0
- `activity-compose` 1.9.2 → 1.13.0
- All `compose-ui*` artifacts now versionless — pinned by `platform('androidx.compose:compose-bom:2026.05.00')`
- `material3` 1.3.0 → 1.3.2 (pinned explicitly)
- `material3-window-size-class` 1.3.0 → 1.3.2
- `com.google.android.material:material` 1.12.0 → 1.13.0
- Removed duplicates (`material3:1.3.0` was declared twice; `junit:junit` was declared twice)
- Removed the `$compose_version` substitutions (no longer defined)

- [ ] **Step 2: Build the debug APK to validate dependency resolution**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. First build will download new artifacts; allow 2–5 minutes.

If you hit a Material3 / Compose UI API mismatch at compile time (e.g., a `Divider` / `HorizontalDivider` rename or similar), the fallback is to drop the BOM and pin Compose UI explicitly to 1.7.8 + Material3 1.3.2 (a known-stable pair):
```groovy
implementation 'androidx.compose.ui:ui:1.7.8'
implementation 'androidx.compose.ui:ui-tooling-preview:1.7.8'
debugImplementation 'androidx.compose.ui:ui-tooling:1.7.8'
debugImplementation 'androidx.compose.ui:ui-test-manifest:1.7.8'
androidTestImplementation 'androidx.compose.ui:ui-test-junit4:1.7.8'
```
(and remove both `platform(...)` lines). Try the BOM first; only fall back if the build won't compile.

- [ ] **Step 3: Run all unit tests to confirm no regressions from the dep bump**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All 8 existing tests pass (3 evaluator + 2 parser + 3 writer).

- [ ] **Step 4: Commit**

```bash
git status --short
git add app/build.gradle
git commit -m "Migrate Compose deps to BOM 2026.05.00, bump non-Compose stables"
```

---

## Task 4: TDD — Fix `canEnterOperation` short-circuit bug

**Files:**
- Modify: `app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt`
- Modify: `app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt:74`

- [ ] **Step 1: Write the failing test**

In `ExpressionWriterTest.kt`, append a new test method *inside* the existing class (just before the closing `}` of the class):

```kotlin
    @Test
    fun `Cannot start with multiply`() {
        writer.processAction(CalculatorAction.Op(Operation.MULTIPLY))

        assertThat(writer.expression).isEqualTo("")
    }
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Cannot start with multiply"
```

Expected: **FAIL** — the test crashes with `StringIndexOutOfBoundsException` because `expression.last()` is called on an empty string. (This is the very bug we're fixing.)

- [ ] **Step 3: Apply the minimal fix**

Open `app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt`. Find this method (around line 70–75):

```kotlin
    private fun canEnterOperation(operation: Operation): Boolean {
        if (operation in listOf(Operation.ADD, Operation.SUBTRACT)) {
            return expression.isEmpty() || expression.last() in "$operationSymbols()0123456789"
        }
        return expression.isNotEmpty() || expression.last() in "0123456789)"
    }
```

Change `||` to `&&` on the last line:

```kotlin
    private fun canEnterOperation(operation: Operation): Boolean {
        if (operation in listOf(Operation.ADD, Operation.SUBTRACT)) {
            return expression.isEmpty() || expression.last() in "$operationSymbols()0123456789"
        }
        return expression.isNotEmpty() && expression.last() in "0123456789)"
    }
```

That's the only change to this file in this task.

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Cannot start with multiply"
```

Expected: **PASS**.

- [ ] **Step 5: Run the full test suite to confirm no regressions**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All 9 tests pass (8 existing + 1 new).

- [ ] **Step 6: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt \
        app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt
git commit -m "Fix canEnterOperation short-circuit on empty expression"
```

---

## Task 5: TDD — Format integer results without trailing `.0`

**Files:**
- Modify: `app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt`
- Modify: `app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt`

- [ ] **Step 1: Write the failing test**

Append to `ExpressionWriterTest.kt`, inside the class:

```kotlin
    @Test
    fun `Integer result drops decimal`() {
        writer.processAction(CalculatorAction.Number(4))
        writer.processAction(CalculatorAction.Op(Operation.ADD))
        writer.processAction(CalculatorAction.Number(1))
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("5")
    }
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Integer result drops decimal"
```

Expected: **FAIL** with `expected: "5" but was: "5.0"`.

- [ ] **Step 3: Add the `formatResult` helper and wire it into `Calculate`**

In `ExpressionWriter.kt`, replace the existing `Calculate` branch (currently lines 9–13 inside the `when` block):

```kotlin
            CalculatorAction.Calculate -> {
                val parser = ExpressionParser(prepareForCalculation())
                val evaluator = ExpressionEvaluator(parser.parse())
                expression = evaluator.evaluate().toString()
            }
```

with:

```kotlin
            CalculatorAction.Calculate -> {
                val parser = ExpressionParser(prepareForCalculation())
                val evaluator = ExpressionEvaluator(parser.parse())
                expression = formatResult(evaluator.evaluate())
            }
```

Then add a new `private fun formatResult` near the bottom of the class, *above* the closing brace, *below* `canEnterOperation`:

```kotlin
    private fun formatResult(value: Double): String {
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        return "%.10f".format(value).trimEnd('0').trimEnd('.')
    }
```

- [ ] **Step 4: Run to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Integer result drops decimal"
```

Expected: **PASS**.

- [ ] **Step 5: Run the full suite — confirm existing tests still pass**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All 10 tests pass (9 from before + 1 new). Existing evaluator tests still pass (they don't go through `ExpressionWriter`).

- [ ] **Step 6: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt \
        app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt
git commit -m "Format integer results without trailing decimal"
```

---

## Task 6: TDD — Divide-by-zero yields `"Error"`

**Files:**
- Modify: `app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt`
- Modify: `app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt`

- [ ] **Step 1: Write the failing test**

Append to `ExpressionWriterTest.kt`, inside the class:

```kotlin
    @Test
    fun `Divide by zero yields Error`() {
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Calculate)

        assertThat(writer.expression).isEqualTo("Error")
    }
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Divide by zero yields Error"
```

Expected: **FAIL** with `expected: "Error" but was: "Infinity"`.

- [ ] **Step 3: Wrap `Calculate` in try/catch + finite check**

In `ExpressionWriter.kt`, replace the `Calculate` branch (modified in Task 5):

```kotlin
            CalculatorAction.Calculate -> {
                val parser = ExpressionParser(prepareForCalculation())
                val evaluator = ExpressionEvaluator(parser.parse())
                expression = formatResult(evaluator.evaluate())
            }
```

with:

```kotlin
            CalculatorAction.Calculate -> {
                expression = try {
                    val parser = ExpressionParser(prepareForCalculation())
                    val result = ExpressionEvaluator(parser.parse()).evaluate()
                    if (result.isFinite()) formatResult(result) else "Error"
                } catch (_: Exception) {
                    "Error"
                }
            }
```

`formatResult` (added in Task 5) is unchanged.

- [ ] **Step 4: Run to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Divide by zero yields Error"
```

Expected: **PASS**.

- [ ] **Step 5: Run the full suite**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All 11 tests pass.

- [ ] **Step 6: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt \
        app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt
git commit -m "Catch divide-by-zero and malformed expressions, set Error sentinel"
```

---

## Task 7: TDD — Error state clears on next input

**Files:**
- Modify: `app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt`
- Modify: `app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt`

- [ ] **Step 1: Write the failing test**

Append to `ExpressionWriterTest.kt`, inside the class:

```kotlin
    @Test
    fun `Error clears on next input`() {
        // Trigger error
        writer.processAction(CalculatorAction.Number(5))
        writer.processAction(CalculatorAction.Op(Operation.DIVIDE))
        writer.processAction(CalculatorAction.Number(0))
        writer.processAction(CalculatorAction.Calculate)
        // Sanity: we are in error state
        assertThat(writer.expression).isEqualTo("Error")

        // Pressing a digit should start a fresh expression
        writer.processAction(CalculatorAction.Number(7))

        assertThat(writer.expression).isEqualTo("7")
    }
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Error clears on next input"
```

Expected: **FAIL** with `expected: "7" but was: "Error7"` (the digit gets concatenated to `"Error"`).

- [ ] **Step 3: Add the recovery rule at the top of `processAction`**

In `ExpressionWriter.kt`, the current `processAction` starts with:

```kotlin
    fun processAction(action: CalculatorAction) {
        when (action) {
            CalculatorAction.Calculate -> {
                ...
```

Add a single `if` line at the top, *before* the `when`:

```kotlin
    fun processAction(action: CalculatorAction) {
        if (expression == "Error" && action !is CalculatorAction.Calculate) {
            expression = ""
        }
        when (action) {
            CalculatorAction.Calculate -> {
                ...
```

This relies on `CalculatorAction.Calculate` being an `object` (it is — defined in `domain/CalculatorAction.kt`), so the `!is` check works against the singleton.

- [ ] **Step 4: Run to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionWriterTest.Error clears on next input"
```

Expected: **PASS**.

- [ ] **Step 5: Run the full suite**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: All 12 tests pass (8 existing + 4 new). This matches the spec's verification matrix line "All 12 domain unit tests pass."

- [ ] **Step 6: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/domain/ExpressionWriter.kt \
        app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt
git commit -m "Clear Error state on next user input"
```

---

## Task 8: Render `"Error"` text in the M3 error color

**Files:**
- Modify: `app/src/main/java/com/example/materialcalculator/presentation/CalculatorDisplay.kt`

This task has no unit test (Compose color is a render-time concern; instrumented tests don't easily assert color). Verify visually in Task 12.

- [ ] **Step 1: Replace the file**

Current `CalculatorDisplay.kt` has the text color hardcoded to `MaterialTheme.colorScheme.onSecondaryContainer`. Change to derive the color from the expression:

```kotlin
package com.example.materialcalculator.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorDisplay(
    expression: String,
    modifier: Modifier = Modifier
) {
    val isError = expression == "Error"
    val textColor =
        if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = expression,
            onValueChange = {},
            textStyle = TextStyle(
                fontSize = 80.sp,
                color = textColor,
                textAlign = TextAlign.End
            ),
            maxLines = 1,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

The only logical change is the addition of `isError`/`textColor` and using `textColor` inside `TextStyle`. Imports unchanged.

- [ ] **Step 2: Build to confirm no compile errors**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/presentation/CalculatorDisplay.kt
git commit -m "Render Error text in colorScheme.error"
```

---

## Task 9: Remove deprecated `statusBarColor` SideEffect from Theme.kt

**Files:**
- Modify: `app/src/main/java/com/example/materialcalculator/ui/theme/Theme.kt`

**Why:** `Window.statusBarColor` is deprecated in API 35 and a no-op once edge-to-edge is enforced (which API 36 does for apps targeting it). Keeping the `SideEffect` block fights edge-to-edge. Dynamic color logic above the SideEffect block is correct and stays untouched.

- [ ] **Step 1: Replace the file**

```kotlin
package com.example.materialcalculator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun MaterialCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

Differences vs. before:
- Removed imports: `android.app.Activity`, `androidx.compose.runtime.SideEffect`, `androidx.compose.ui.graphics.toArgb`, `androidx.compose.ui.platform.LocalView`, `androidx.core.view.ViewCompat`.
- Removed the entire `val view = LocalView.current ... SideEffect { ... }` block (8 lines).
- All other behavior — `darkColorScheme`/`lightColorScheme` definitions, dynamic-color branching, `MaterialTheme` call — unchanged.

- [ ] **Step 2: Build to confirm**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/ui/theme/Theme.kt
git commit -m "Drop deprecated statusBarColor SideEffect for edge-to-edge"
```

---

## Task 10: Enable edge-to-edge in MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/materialcalculator/MainActivity.kt`

- [ ] **Step 1: Replace the file**

```kotlin
package com.example.materialcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.materialcalculator.presentation.CalculatorScreen
import com.example.materialcalculator.ui.theme.MaterialCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialCalculatorTheme {
                CalculatorScreen()
            }
        }
    }
}
```

Differences vs. before:
- Added import `androidx.activity.enableEdgeToEdge`.
- Added `enableEdgeToEdge()` line in `onCreate` *before* `setContent { ... }`.
- Removed unused imports (`fillMaxSize`, `MaterialTheme`, `Surface`, `Text`, `Composable`, `Modifier`, `Preview`) — they were never used in the original file beyond a comment-out preview that wasn't there.

`enableEdgeToEdge()` lives in `androidx.activity:activity-compose:1.13.0` (which Task 3 pinned).

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/MainActivity.kt
git commit -m "Enable edge-to-edge in MainActivity"
```

---

## Task 11: Wrap CalculatorScreen in Scaffold for inset handling

**Files:**
- Modify: `app/src/main/java/com/example/materialcalculator/presentation/CalculatorScreen.kt`

- [ ] **Step 1: Replace the file**

Current `CalculatorScreen.kt` uses a raw `Surface { Column(SpaceBetween) { Display ; Spacer ; ButtonGrid } }`. Wrap in `Scaffold` so system-bar insets get applied to the content `padding` automatically.

```kotlin
package com.example.materialcalculator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CalculatorDisplay(
                expression = viewModel.expression,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 25.dp,
                            bottomEnd = 25.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(
                        vertical = 64.dp,
                        horizontal = 16.dp
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CalculatorButtonGrid(
                actions = calculatorActions,
                onAction = viewModel::onAction,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
```

Differences vs. before:
- Outer `Surface` → `Scaffold` (M3). `Scaffold` provides `containerColor` (matches old `Surface(color = background)`) and emits `innerPadding` representing system-bar insets.
- `Column` now `.padding(innerPadding)` to apply the insets.
- Imports updated: `Surface` → `Scaffold`, added `padding` import.
- The `CalculatorDisplay` and `CalculatorButtonGrid` calls are byte-for-byte identical to before.

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git status --short
git add app/src/main/java/com/example/materialcalculator/presentation/CalculatorScreen.kt
git commit -m "Wrap CalculatorScreen in Scaffold for inset-aware layout"
```

---

## Task 12: Final pre-flight verification (no commit)

**Files:** none (verification only)

This task runs the full release pre-flight checklist from the spec's Section 4. **Do not modify any code in this task** — if any step fails, return to the relevant earlier task and patch.

- [ ] **Step 1: Clean build**

```bash
./gradlew clean
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Lint (release variant)**

```bash
./gradlew :app:lintRelease
```

Expected: `BUILD SUCCESSFUL`. Inspect `app/build/reports/lint-results-release.html` for any new errors. Warnings are tolerable; new **errors** are not — patch and re-run.

- [ ] **Step 3: All 12 unit tests on release variant**

```bash
./gradlew :app:testReleaseUnitTest
```

Expected: `BUILD SUCCESSFUL`, `12 tests completed, 0 failed`.

- [ ] **Step 4: Smoke-build the release APK**

```bash
./gradlew :app:assembleRelease
```

Expected: `BUILD SUCCESSFUL`. Output at `app/build/outputs/apk/release/app-release.apk`. Will only sign if `local.properties` has the keystore values populated.

- [ ] **Step 5: Build the release AAB (the artifact you actually upload)**

```bash
./gradlew :app:bundleRelease
```

Expected: `BUILD SUCCESSFUL`. Output at `app/build/outputs/bundle/release/app-release.aab`.

- [ ] **Step 6: Install staging variant on a connected physical device**

(Requires a USB-connected device with USB debugging enabled, or a running emulator. The instrumented test in Step 7 also needs this.)

```bash
./gradlew :app:installStaging
```

Expected: `BUILD SUCCESSFUL` and the staging app appears in the launcher (alongside production install if any). The `.staging` applicationIdSuffix lets it coexist.

- [ ] **Step 7: Run the instrumented Compose UI test**

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: `BUILD SUCCESSFUL`, 1 test passes (the existing `CalculatorScreenTest.enterExpression_correctResultDisplayed`).

- [ ] **Step 8: Manual smoke matrix** (perform on the device)

Run each of these in the staging app and confirm the result:

| Action | Expected display |
|---|---|
| `4` `+` `1` `=` | `5` (no `.0`) |
| `5` `÷` `0` `=` | `Error` (in red/error color) |
| After Error: press `7` | `7` (Error cleared) |
| Empty → press `x` | nothing (no crash) |
| `(` `5` `+` `4` `)` `=` | `9` |
| Rotate device portrait → landscape | layout reflows, no clipping under status/nav bar |
| Change device wallpaper → relaunch | calculator color palette shifts (Android 12+) |
| Toggle system dark mode | calculator theme follows |

If any row fails, patch the relevant earlier task before proceeding.

- [ ] **Step 9: Update CLAUDE.md "Project" line**

The project header in `CLAUDE.md` line 7 currently says:
```
... Kotlin 2.0.20, AGP 8.5.1, Jetpack Compose with Material 3, JVM target 17, minSdk 24 / targetSdk 34.
```

Update to:
```
... Kotlin 2.3.10, AGP 8.13.2, Jetpack Compose (BOM 2026.05.00) with Material 3, JVM target 17, minSdk 24 / targetSdk 36.
```

```bash
git status --short
git add CLAUDE.md
git commit -m "Update CLAUDE.md project header for Phase 1 versions"
```

- [ ] **Step 10: Final git log review**

```bash
git log --oneline development ^master
```

Expected: 11 commits, in order:
```
Update CLAUDE.md project header for Phase 1 versions
Wrap CalculatorScreen in Scaffold for inset-aware layout
Enable edge-to-edge in MainActivity
Drop deprecated statusBarColor SideEffect for edge-to-edge
Render Error text in colorScheme.error
Clear Error state on next user input
Catch divide-by-zero and malformed expressions, set Error sentinel
Format integer results without trailing decimal
Fix canEnterOperation short-circuit on empty expression
Migrate Compose deps to BOM 2026.05.00, bump non-Compose stables
Bump compileSdk/targetSdk to 36, version to 1.2.0
Bump Gradle 8.13, Kotlin 2.3.10, AGP 8.13.2
```
(Plus the earlier `Added Phase 1 calculator revamp design + CLAUDE.md` from the brainstorming step, already on `development`.)

---

## What's NOT in this plan (intentional)

- **Play Console paperwork** (health declaration, upload key reset). These are non-code, gated on Google's wall-clock SLA, and live in Section 4 of the spec. Start them on Day 0, in parallel with this plan's coding work.
- **PR / merge to master / Play Console upload.** Those are the user's call once Task 12 verification is green.
- **Phase 2 features** (rebrand, themes, settings, history, haptics, sound, modes, KTS migration). Each gets its own spec + plan post-deadline.

## Rollback strategy

Each task is one atomic commit. If a task fails verification, revert with:
```bash
git reset --hard HEAD~1
```
**Only** do this for the most-recent failing task before sharing with anyone else. If the failure is discovered later, write a forward-fix commit instead.

If the entire branch needs to be abandoned post-merge: revert the merge commit on `master`, don't rewrite history.
