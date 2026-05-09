# HG Calculator

A modernized Material 3 calculator for Android — currently published on the Google Play Store.

[![Play Store](https://img.shields.io/badge/Google_Play-Live-success?logo=googleplay)](https://play.google.com/store/apps/details?id=com.gaddal.materialcalculator)
![minSdk](https://img.shields.io/badge/minSdk-24-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-36-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin)
![Compose BOM](https://img.shields.io/badge/Compose_BOM-2026.05.00-4285F4)

## Features

- Material 3 with dynamic color (Android 12+)
- Edge-to-edge display
- Recursive-descent expression evaluator with operator precedence and parentheses
- Bilingual release notes (English + Arabic) — built with the MENA market in mind
- Robust error handling (divide-by-zero, malformed expressions)

## Tech stack

- **Language:** Kotlin 2.3.10
- **UI:** Jetpack Compose, Material 3 (BOM 2026.05.00)
- **Architecture:** MVVM with unidirectional data flow
- **Build:** Gradle 8.13, AGP 8.13.2, Java 17
- **Testing:** JUnit 4, Google Truth, Espresso

## Project structure

Single-module Gradle project with two layered packages inside `:app`:

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
./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionEvaluatorTest"

# Lint
./gradlew :app:lintDebug
```

### Build variants

Three build types: `debug`, `staging`, `release`. Staging carries `applicationIdSuffix .staging` so it installs alongside the production build.

`local.properties` (gitignored) must define `keystore.file`, `keystore.password`, `keystore.alias`, `keystore.alias_password` — even debug builds read these unconditionally. Supply dummy values on a fresh checkout if you only need a debug build.

## Branching

```
feature/* → development → staging → master
```

Work happens on `feature/<name>` branches off `development`. `staging` carries release candidates and uses the `staging` build variant. `master` is production-only and ships to Play Store.

## Roadmap

- **Phase 1 (shipped 2026-05-09):** Toolchain modernization, Material 3 dynamic color, edge-to-edge, error handling, bug fixes, Android 16 target SDK
- **Phase 2 (next):** Theme picker, settings panel, calculation history, haptics, GitHub Actions CI/CD, full 4-branch flow

## Author

Built by [Hussain Gaddal](https://github.com/IronManYG).
