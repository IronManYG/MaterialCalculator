# Calculator Revamp — Phase 1 Design

| Field | Value |
|---|---|
| Date | 2026-05-08 |
| Author | Hussain Gaddal (with Claude Code) |
| Project | MaterialCalculator (`com.gaddal.materialcalculator`) |
| Phase | 1 — "Ship + light polish" before Play Store dormancy deadline |
| Hard deadline | **2026-06-02** (developer account closes if no production update lands) |
| Branch | `development` → PR into `master`, tag `v1.2.0` after merge |

## Background

The published app is dormant on Play Store. Three Play Console blockers:

1. **Dormancy** — must publish a production update by **2026-06-02** or the developer account closes.
2. **Update rejected 2024-09-23** — "Broken Functionality." Almost certainly the **Health apps declaration** form Google now requires; the app's manifest has zero health-related permissions, so the form answer is "No." Confirm by completing the declaration; if the rejection persists post-declaration, dig deeper.
3. **Target SDK requirement** — Play Store enforces API 35 since 2025-08-31. The build is currently on `targetSdk 34`. Move to **API 36** (Android 16, latest stable as of today, verified via `android sdk list "platforms/*" --all`) to future-proof through Aug 2027.

Signing situation: **Play App Signing is enabled**, so Google holds the real signing key and only the **upload key** matters. The upload key from the original 2022 release is lost; resettable via the Play Console support form (1–2 business-day Google approval).

The repo state when this design was written is well ahead of an older snapshot another agent reviewed in the web. The `development` branch already has the full calculator code, signing config wired to `local.properties`, three build types, and modern (~2024) Compose/Material3/AGP/Kotlin. So Phase 1 is a *modernization + polish* update, not a recovery from a stub.

## Out of scope (Phase 2, post-deadline)

Brand identity refresh (Prism / Quantum / Tactile naming), settings panel, theme picker (Cardboard / Minimalist / Retro / Glassmorphism), haptic feedback, sound profiles, calculation history, specialized modes (tip / unit conversion / financial), natural-language input, custom-formula save, Groovy → KTS + version-catalog migration. Each gets its own design doc later.

## Goals (Phase 1)

- Lift the dormancy block by publishing **any** production update before 2026-06-02.
- Bring the build to current `targetSdk` and dependency stables.
- Fix two known user-visible defects (`5/0=Infinity`, malformed expression crash).
- Fix one latent crash (`canEnterOperation` short-circuit bug).
- Keep behavior identical for the golden path so the old app's users see "the same calculator, just nicer-looking."

Non-goals: rebrand, new features, settings, themes.

---

## Section 1 — Build & version targets

| Field | Before | After | Source of truth |
|---|---|---|---|
| `compileSdk` | 34 | **36** | `android sdk list "platforms/*" --all` (locally installed) |
| `targetSdk` | 34 | **36** | Same |
| `versionCode` | 4 | **5** | Manifest convention |
| `versionName` | "1.1.0" | **"1.2.0"** | Manifest convention |
| `minSdk` | 24 | **24** (unchanged) | — |
| Kotlin | 2.0.20 | **2.3.10** | Maven Central `org.jetbrains.kotlin:kotlin-stdlib/maven-metadata.xml` |
| AGP | 8.5.1 | **8.13.2** | Google Maven `com.android.tools.build:gradle/maven-metadata.xml` |
| Compose BOM (new) | (none) | **2026.05.00** | Google Maven `androidx.compose:compose-bom/maven-metadata.xml` |
| Material3 (explicit pin) | 1.3.0 | **1.3.2** | Google Maven; pin explicitly because BOM may pull `1.4.0-alpha` |
| Lifecycle | 2.8.6 | **2.10.0** | Google Maven |
| Activity Compose | 1.9.2 | **1.13.0** | Google Maven |
| Core KTX | 1.13.1 | **1.18.0** | Google Maven |
| Material (Views) | 1.12.0 | **1.13.0** | Google Maven; needed by `Theme.MaterialCalculator` XML parent |

Compose BOM replaces individual `compose-ui`, `ui-tooling-preview`, `ui-tooling`, `ui-test-manifest`, `ui-test-junit4` pins. `material3` is pinned outside the BOM because the BOM tracks the Compose Foundation cadence and may resolve to a `1.4.0-alpha*`; we want stable.

`staging` build type, `signingConfigs.release` block, and per-variant `Constants.kt` source-set pattern stay exactly as they are.

`local.properties` keystore password rotation is **out of scope** for Phase 1 — it's gitignored. After the upload key reset lands, the new `upload-keystore.jks` replaces the upload-key role only; the original `_keystore.jks` referenced in `local.properties` stays in place (Google holds the actual signing key under Play App Signing, so the original keystore's role is functionally retired).

---

## Section 2 — Theme architecture

### Color scheme — dynamic on API 31+, fixed fallback below

`MaterialCalculatorTheme` (in `ui/theme/Theme.kt`) gets one new branch:

```kotlin
val context = LocalContext.current
val useDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val scheme = when {
    useDynamic && darkTheme -> dynamicDarkColorScheme(context)
    useDynamic              -> dynamicLightColorScheme(context)
    darkTheme               -> DarkColorScheme   // existing fixed
    else                    -> LightColorScheme  // existing fixed
}
```

- API 31+ (the dominant share of active devices on the Play Store): system-wallpaper-derived palette.
- API 24–30: existing fixed `LightColorScheme` / `DarkColorScheme` from `Color.kt` — no visual regression.
- `isSystemInDarkTheme()` already drives `darkTheme`.

### Button-color mapping — unchanged

The `HighlightLevel` → M3 role table in `presentation/CalculatorButton.kt` stays identical:

| HighlightLevel | Background | Foreground |
|---|---|---|
| `Neutral` | `surfaceVariant` | `onSurfaceVariant` |
| `SemiHighlighted` | `inverseSurface` | `inverseOnSurface` |
| `Highlighted` | `tertiary` | `onTertiary` |
| `StronglyHighlighted` | `primary` | `onPrimary` |

Because these are **role lookups**, dynamic-color values are picked up automatically on API 31+ with zero code change in the button file.

### Edge-to-edge

Activity Compose 1.13 + Compose 1.11 give us `enableEdgeToEdge()`. In `MainActivity.onCreate`, replace the implicit `setDecorFitsSystemWindows(true)` behavior with:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()              // new
    setContent { MaterialCalculatorTheme { CalculatorScreen() } }
}
```

`CalculatorScreen` currently uses raw `Surface` + `Column`. Wrap in `Scaffold { padding -> ... Column.padding(padding) ... }` so the system-bar insets land cleanly. Display + button-grid layout inside stays identical.

### Typography

`ui/theme/Type.kt` is unchanged. Default M3 `Typography` is fine for Phase 1; custom typography is a Phase 2 concern.

---

## Section 3 — Number formatting, error states, latent-bug fix

All three pieces live in `domain/ExpressionWriter.kt`. No Compose-layer logic changes.

### 3a — Result formatting

```kotlin
private fun formatResult(value: Double): String {
    if (value == value.toLong().toDouble()) return value.toLong().toString()  // 5.0 → "5"
    return "%.10f".format(value).trimEnd('0').trimEnd('.')                    // 1.5000... → "1.5"
}
```

- 10-decimal cap matches stock Android calculator's display budget.
- No thousands separators — they would break the parser when the result becomes the next input.
- The underlying expression string remains a parser-legal numeric literal.

### 3b — Error state

```kotlin
CalculatorAction.Calculate -> {
    expression = try {
        val result = ExpressionEvaluator(ExpressionParser(prepareForCalculation()).parse()).evaluate()
        if (result.isFinite()) formatResult(result) else "Error"
    } catch (_: Exception) {
        "Error"
    }
}
```

Recovery rule, added at the top of `processAction`:

```kotlin
if (expression == "Error" && action !is CalculatorAction.Calculate) {
    expression = ""   // any subsequent input clears the error and starts fresh
}
```

`CalculatorDisplay` is also tweaked (3 lines): render text in `MaterialTheme.colorScheme.error` when `expression == "Error"`, otherwise the default `onSurface`. Locked in — micro-cost, clear UX win.

The `"Error"` literal is a sentinel string; localization is Phase 2 (full strings.xml pass).

### 3c — `canEnterOperation` short-circuit bug fix

`ExpressionWriter.kt:74` — change `||` to `&&`:

```kotlin
// before — crashes on empty expression, allows "+x" sequences
return expression.isNotEmpty() || expression.last() in "0123456789)"
// after
return expression.isNotEmpty() && expression.last() in "0123456789)"
```

Why it's broken today:
- Empty expression + multiply/divide/percent press → `isNotEmpty()` returns false, evaluates `expression.last()` → `StringIndexOutOfBoundsException` (uncaught crash).
- Non-empty expression with bad last char (e.g., expression is `"+"`) → `isNotEmpty()` returns true, OR short-circuits to `true` → allows `"+x"`, `"+÷"`, `"(x"`.

After the fix, multiply/divide/percent require a digit or closing paren immediately before, matching the existing add/subtract logic's intent.

### Test additions

In `app/src/test/java/com/example/materialcalculator/domain/ExpressionWriterTest.kt`:

1. `divide_by_zero_yields_Error()` — `5 /  0 =` ⇒ `expression == "Error"`.
2. `error_clears_on_next_input()` — after error, press `7` ⇒ `expression == "7"`.
3. `cannot_start_with_multiply()` — empty + `Op(MULTIPLY)` ⇒ `expression == ""` (no exception).
4. `integer_result_drops_decimal()` — `4 + 1 =` ⇒ `expression == "5"` (not `"5.0"`).

Existing **8 domain unit tests** (3 evaluator + 2 parser + 3 writer) and **1 instrumented Compose test** continue to pass unchanged. Total after this phase: **12 domain unit tests** + 1 instrumented.

---

## Section 4 — Rollout, signing, Play Console parallel work

### Branch strategy

Single-developer project. Work on `development`, open one PR `development → master` once Phase 1 is verified locally and on a physical device. Tag the merge `v1.2.0`. No release branch.

### Day-0 actions (start today, parallel to coding)

These are gated on Google. Latency is wall-clock, not work-clock — start them first so they progress while code happens.

1. **Health declaration** in Play Console → App content → Health apps declaration → answer "**No**, my app does not have health features." Calculator manifest declares zero health/sensor/activity permissions (verified during CLAUDE.md generation).
2. **Upload key reset** via [support article 9859152](https://support.google.com/googleplay/android-developer/answer/9859152):
   ```bash
   keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   keytool -export -rfc -keystore upload-keystore.jks -alias upload -file upload_certificate.pem
   ```
   Attach `upload_certificate.pem` to the form. Approval = **1–2 business days**.
3. **`local.properties` update** (after key generation): point keystore.* keys at the new `upload-keystore.jks` path.

### Pre-flight checklist (before generating signed AAB)

In order; abort at the first failure:

```bash
./gradlew clean
./gradlew :app:lintRelease         # zero new errors
./gradlew :app:testReleaseUnitTest # all 12 domain unit tests pass (8 existing + 4 new)
./gradlew :app:assembleRelease     # smoke build, signs only if local.properties is correct
./gradlew :app:bundleRelease       # the AAB you upload
```

Then on a **physical device** (not emulator):
- Install staging variant (`./gradlew :app:installStaging`) so it sits alongside the production install.
- Sanity matrix: `4+1=5` (no `.0`), `5/0=Error`, `Error → 7 → 7`, `()` balanced, edge-to-edge looks correct portrait + landscape, dynamic color visibly responds to a wallpaper change on Android 12+.

### Play Console upload sequence

1. **Internal Testing track first** — never straight to production. Internal testing skips most of the review queue and confirms the exact signed bundle Google will ship installs cleanly.
2. Verify install via Play Store on a tester device.
3. Promote to **Production**.
4. Refresh data-safety, content-rating, ad/IAP declarations if Play Console flags them as outdated (they probably will — last touched ~3 years ago).
5. Submit. Production review: **1–3 days typical, allow up to 7**.

### Timeline against the 2026-06-02 deadline

Today is 2026-05-08 (24 working days available).

| Day | Action |
|---|---|
| 0 (today, May 8) | Health declaration + upload key reset request submitted **in parallel** with code work |
| 1–3 | Code: Sections 1–3 implemented in atomic commits (one per section), 4 new tests pass |
| 4 | Upload key reset *should* be approved (1–2 biz day SLA from Day 0) |
| 4 | Build signed AAB with new upload key, install on physical device, manual smoke matrix |
| 5 | Upload to Internal Testing, install via Play Store, smoke again |
| 6 | Promote to Production, submit |
| 7–14 | Production review (1–3 days typical, up to 7 worst-case) |
| 15–24 | **9-day buffer** against rejection / re-submission |

### Deadline-blocking risks

| Risk | Mitigation |
|---|---|
| Upload key reset not approved by Day 4 | Submit Day 0; 1–2 day SLA leaves headroom. Don't delay submission. |
| Production review rejection | Internal testing install is the dry run. Re-submission costs 1–3 days; buffer absorbs it. |
| Compose 1.5 → 1.11 regression | Pre-flight catches it; localized fix, re-run pre-flight. |
| Health declaration doesn't actually clear the Sept 2024 rejection | If "Update rejected" row persists post-declaration, open the Play Console row and we'll need a separate diagnosis cycle. |

### Phase 2 footnote — release tooling

Once we're past Jun 2, install **`gpc`** ([`AndroidPoet/playconsole-cli`](https://github.com/AndroidPoet/playconsole-cli)) for ongoing releases. Picked over `tamtom/play-console-cli` for: more mature (120★ vs 76★, fewer open issues), automated service-account setup, granular commands (`bundles wait`, `tracks halt`, `deobfuscation upload`), parsable output formats (`json/yaml/tsv`). Out of Phase 1 critical path because deadline-pressured tooling adoption is its own risk.

---

## Verification matrix (must hold before merging to master)

- [ ] All 12 domain unit tests pass (`./gradlew :app:testReleaseUnitTest`)
- [ ] Instrumented test still passes (`./gradlew :app:connectedDebugAndroidTest` on a connected device or emulator)
- [ ] `./gradlew :app:lintRelease` reports zero new errors vs. existing baseline
- [ ] Signed `app-release.aab` builds successfully with the new upload keystore
- [ ] Manual smoke matrix passes on a physical device (Sanity matrix above)
- [ ] Health declaration submitted in Play Console (`No`)
- [ ] Upload key reset approved by Google
- [ ] AAB installs and runs on Internal Testing track
- [ ] CLAUDE.md "Project" line updated (`compileSdk` / `targetSdk` 36, version 1.2.0)

## Open questions for the user (none currently blocking)

- Is "Error" acceptable as a sentinel for Phase 1, or should we localize via `R.string.calc_error` even now? (Recommendation: literal "Error", localize in Phase 2.)
- Should `staging` build type stay or be removed? (Recommendation: keep — alongside-install is useful for testing dynamic color.)
