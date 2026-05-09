# Sifr (صفر)

A modernized Material 3 calculator for Android — currently published on the Google Play Store. Named after the Arabic word for "zero" (the root of the English word "cipher" and the foundational concept of digital computing).

[![Play Store](https://img.shields.io/badge/Google_Play-Live-success?logo=googleplay)](https://play.google.com/store/apps/details?id=com.gaddal.materialcalculator)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)
![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.05.00-4285F4)

## Features

- Material 3 with dynamic color (Android 12+)
- Edge-to-edge display
- Recursive-descent expression evaluator with operator precedence and parentheses
- Bilingual app label (English "Sifr" + Arabic "صفر") and bilingual release notes — built with the MENA market in mind
- Robust error handling (divide-by-zero, malformed expressions)

## Tech stack

- **Language:** Kotlin 2.3.21
- **UI:** Jetpack Compose, Material 3 (BOM 2026.05.00)
- **Architecture:** MVVM with unidirectional data flow
- **Build:** Gradle 9.5.0, AGP 9.2.1, JDK 21 toolchain (declarative via `gradle/gradle-daemon-jvm.properties`), Java 17 target
- **Build scripts:** Kotlin DSL with version catalog (`gradle/libs.versions.toml`)
- **Testing:** JUnit 4, Google Truth, Espresso

## Project structure

Single-module Gradle project — namespace `dev.gaddal.sifr`, applicationId `com.gaddal.materialcalculator` (intentionally distinct to preserve the live Play Store listing). Layered packages inside `:app`:

- `domain/` — pure Kotlin (no Android deps): `ExpressionWriter`, `ExpressionParser`, `ExpressionEvaluator`, `CalculatorAction`, `Operation`
- `presentation/` — Compose UI + `CalculatorViewModel`

The calculation pipeline is one-shot per `=` press: UI emits a `CalculatorAction`, `ExpressionWriter` mutates the raw expression string, `ExpressionParser` tokenizes it, and `ExpressionEvaluator` evaluates with a hand-written recursive-descent grammar (`expression → term (± term)*`, `term → factor (×|÷|% factor)*`).

For deeper architecture notes and conventions see [`CLAUDE.md`](./CLAUDE.md).

## Build

The project uses the Gradle wrapper. From the repo root:

```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on connected device
./gradlew :app:installDebug

# All unit tests
./gradlew test

# Single test class
./gradlew :app:testDebugUnitTest --tests "dev.gaddal.sifr.domain.ExpressionEvaluatorTest"

# Lint
./gradlew :app:lintDebug
```

### Build variants

Three build types: `debug`, `staging`, `release`. Staging carries `applicationIdSuffix .staging` so it installs alongside the production build.

`local.properties` (gitignored) defines `keystore.file`, `keystore.password`, `keystore.alias`, `keystore.alias_password` for release signing. The build script wraps the read in `runCatching`, so missing or malformed `local.properties` (or just missing the `keystore.*` keys) only blocks release/staging signed builds — debug builds work without the keystore lines as long as `sdk.dir` (or `ANDROID_HOME` env var) is present.

## Branching

```
feature/* → development → staging → master
```

Work happens on `feature/<name>` branches off `development`. `staging` carries release candidates and uses the `staging` build variant. `master` is production-only and ships to Play Store.

## Roadmap

- **Phase 1 (shipped 2026-05-09):** Toolchain modernization, Material 3 dynamic color, edge-to-edge, error handling, bug fixes, Android 16 target SDK
- **Phase 2.0 (merged 2026-05-09):** Build modernization — Kotlin DSL, version catalog, Gradle 9.5.0, AGP 9.2.1, JDK 21 toolchain
- **Phase 2.1 (this rename):** Brand rename to Sifr, namespace `dev.gaddal.sifr`, Arabic app label
- **Phase 2.2+ (next):** Architecture migration (feature-folder, MVI, Koin, Navigation 3), settings panel, calculation history, error typing + i18n, GitHub Actions CI/CD

## Author

Built by [Hussain Gaddal](https://github.com/IronManYG).
