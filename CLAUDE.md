# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android calculator app (`com.gaddal.materialcalculator`, namespace `com.example.materialcalculator`). Single-module Gradle project (Groovy DSL), Kotlin 2.3.10, AGP 8.13.2, Jetpack Compose (BOM 2026.05.00) with Material 3, JVM target 17, minSdk 24 / targetSdk 36.

## Common commands

Use the wrapper (`./gradlew` on bash, `.\gradlew.bat` on PowerShell). All commands are run from the repo root.

- Build debug APK: `./gradlew :app:assembleDebug`
- Build all variants: `./gradlew assemble` (debug, staging, release — release requires keystore in `local.properties`)
- Install on connected device: `./gradlew :app:installDebug`
- Unit tests (JVM, all variants): `./gradlew test`
- Unit tests, single variant: `./gradlew :app:testDebugUnitTest`
- Run a single unit test class: `./gradlew :app:testDebugUnitTest --tests "com.example.materialcalculator.domain.ExpressionEvaluatorTest"`
- Run a single test method: `./gradlew :app:testDebugUnitTest --tests "*.ExpressionEvaluatorTest.Simple expression properly evaluated"`
- Instrumented tests (needs emulator/device): `./gradlew :app:connectedDebugAndroidTest`
- Lint: `./gradlew :app:lintDebug`
- Clean: `./gradlew clean`

### Android CLI

The official `android` CLI (`/c/ProgramData/AndroidCLI/android` on this machine) is installed and available — use it instead of `sdkmanager` for SDK platform / build-tools / system-image work. The local SDK lives at `E:\AndroidStudioDirectories\Android\Sdk` (pass `--sdk="E:/AndroidStudioDirectories/Android/Sdk"` if the CLI defaults to a different location).

- Update CLI itself: `android update`
- Inspect environment: `android info`
- List installed + available platforms: `android sdk list "platforms/*" --all`
- List build-tools: `android sdk list "build-tools/*" --all`
- Install a specific package: `android sdk install platforms/android-36 build-tools/36.0.0`
- Update a package to latest stable: `android sdk update build-tools/36.0.0`

Pattern syntax is glob (`*`), not regex. Add `--beta` or `--canary` to include preview channels. The CLI does **not** know about Maven artifacts (Compose, Kotlin, AGP, Material3, etc.) — for those use the `dependency-version-lookup` skill.

## Build variants

Three build types: `debug`, `staging`, `release`. Staging and release both have `minifyEnabled true` with the same ProGuard rules; staging adds applicationIdSuffix `.staging` so it can be installed alongside release.

Per-variant `Constants.kt` lives under `app/src/{debug,staging,release}/java/com/example/materialcalculator/Constants.kt` and currently holds a `BASE_URL`. When adding variant-specific config, follow this source-set pattern rather than build-config fields.

`local.properties` (gitignored) must define `keystore.file`, `keystore.password`, `keystore.alias`, `keystore.alias_password` — `app/build.gradle` reads these unconditionally at configuration time, so even debug-only builds will fail if the file is missing or malformed. If you only need a debug build on a fresh checkout, supply dummy values for those four keys.

## Architecture

Layered roughly into `domain/` (pure Kotlin, no Android deps) and `presentation/` (Compose UI + ViewModel). MVVM with unidirectional data flow.

Calculation pipeline (single direction, one-shot per `=` press):

1. UI emits `CalculatorAction` (sealed interface in `domain/CalculatorAction.kt`) via `CalculatorViewModel.onAction`.
2. `ExpressionWriter` (`domain/`) holds the raw expression `String` and mutates it per action — also enforces input validity (`canEnterDecimal`, `canEnterOperation`, `processParentheses`, plus `prepareForCalculation` which trims trailing operators/dots/`(` before evaluating).
3. On `Calculate`, `ExpressionParser` tokenizes the string into `List<ExpressionPart>` (`Number | Op | Parentheses`).
4. `ExpressionEvaluator` recursively evaluates the token list using a hand-written recursive-descent grammar documented at the top of `ExpressionEvaluator.kt`: `expression → term (± term)*`, `term → factor (×|÷|% factor)*`, `factor → number | (expression) | ±factor`. The evaluator's `%` is "multiply by n/100", not modulo.
5. The resulting `Double.toString()` becomes the new expression string, which is then displayed and is itself a valid input for further operations.

`CalculatorViewModel` exposes a single `expression: String` via Compose `mutableStateOf`. There is no separate UI state class — the raw expression string IS the state. `CalculatorScreen` reads it directly; the ViewModel does not survive process death (no SavedStateHandle).

UI is driven by a static `calculatorActions: List<CalculatorUiAction>` (in `presentation/CalculatorActions.kt`) laid out in a 4-column grid by `CalculatorButtonGrid`. Each `CalculatorUiAction` carries display text (or arbitrary `@Composable content`), a `HighlightLevel` (Neutral / SemiHighlighted / Highlighted / StronglyHighlighted) that maps to Material 3 color roles in `CalculatorButton`, and the `CalculatorAction` to dispatch.

## Mandatory Skill Loading

Before working on a layer, **always load the corresponding skill(s) first** via the `Skill` tool. Match the task to the closest row(s) — load multiple if the task spans layers.

### Presentation & UI (Compose)

| Layer / task | Skill to load |
|---|---|
| MVI presentation (State / Action / Event / ViewModel / Root–Screen split) | `android-presentation-mvi` |
| Compose screen architecture (state ownership, side effects, modifiers, slots, previews) | `android-compose-architecture` |
| Material 3 components, theming, lazy lists, animations, dynamic color | `android-compose-components` |
| Generic Compose UI patterns (stability, recomposition, accessibility, design system) | `android-compose-ui` |
| Reviewing / authoring Compose with the "what LLMs get wrong" checklist | `compose-agent` |
| Quantitative scored Compose audit → COMPOSE-AUDIT-REPORT.md | `compose-audit-tool` |
| Compose performance (recomposition, stability, baseline profile, Macrobenchmark) | `compose-performance` |
| Migrating XML View layouts → Compose | `compose-xml-migration` |
| Navigation 2.x (route objects, NavController, type-safe nav) | `android-navigation` |
| Navigation 3 (`androidx.navigation3`, NavDisplay, scenes, multi-back-stack) | `android-navigation-3` |

### Data, DI & background

| Layer / task | Skill to load |
|---|---|
| Repositories, data sources, DTOs, Room, Ktor, mappers, offline-first | `android-data-layer` |
| Koin DI modules, ViewModel injection, `koinViewModel()` | `android-di-koin` |
| `Result<T,E>`, error types, typed error handling anywhere in the app | `android-error-handling` |
| WorkManager, foreground services, AlarmManager, background limits | `android-background` |

### Kotlin

| Task | Skill to load |
|---|---|
| Coroutines (dispatchers, scopes, supervision, structured concurrency) | `kotlin-coroutines` |
| Flows (cold/hot, StateFlow, SharedFlow, operators, testing with Turbine) | `kotlin-flows` |
| Idiomatic Kotlin (sealed types, scope functions, smart casts, sequences) | `kotlin-idioms` |
| SOLID applied to Kotlin / KMP | `kotlin-solid-kmp` |

### Build, versions & quality

| Task | Skill to load |
|---|---|
| **Anytime a version literal needs to be written or bumped** | `dependency-version-lookup` |
| `gradle/libs.versions.toml`, plugin aliases, bundles, convention plugins | `android-version-catalog` |
| Module layout, build-logic, where new code should live | `android-module-structure` |
| AGP 9 + KMP migration (built-in Kotlin, kapt→KSP, source-set renames) | `android-agp-kmp-migration` |
| R8 / ProGuard keep rule audit, release-size shrinking | `android-r8-analyzer` |
| Lint + detekt + ktlint + Konsist quality bundle, CI gating | `android-code-quality` |

### Testing

| Task | Skill to load |
|---|---|
| Android tests (ViewModel JUnit5, Turbine, AssertK, Compose UI tests) | `android-testing` |
| Fakes-first testing philosophy, KMP `commonTest`, fixture builders | `android-unit-testing-fakes` |

### On-device AI

| Task | Skill to load |
|---|---|
| Gemini Nano (`com.google.ai.client.generativeai`, AICore, streaming) | `android-gemini-nano` |
| LiteRT-LM (Google AI Edge LLM inference, GPU/NPU backends) | `android-litert-lm` |
| Local RAG (embeddings, vector store, retrieve→generate loop) | `android-rag-local` |
| Cross-platform on-device AI (CoreML / ONNX / fallback strategy) | `kmp-on-device-ai` |
| KMP LiteRT-LM (shared inference layer across Android + iOS) | `kmp-litert-lm` |

### Form factors

| Task | Skill to load |
|---|---|
| Wear OS (Wear Compose, rotary, tiles, complications, DataLayer) | `android-wear-os` |
| Android XR / AI Glasses (Glimmer, ProjectedActivity, additive display) | `android-xr-glimmer` |
| watchOS via KMP | `kmp-watchos` |

### Kotlin Multiplatform (when this project moves to KMP)

| Task | Skill to load |
|---|---|
| KMP architecture (shared modules, expect/actual, source sets) | `kmp-architecture` |
| iOS essentials (Xcode integration, framework export, lifecycle) | `kmp-ios-essentials` |
| CocoaPods → SPM migration | `kmp-cocoapods-spm-migration` |
| KMP-wide quality orchestration (lint/detekt/ktlint across platforms) | `kmp-quality-orchestrator` |

## Rules

- **Single module, layered packages.** Use the same package structure as the `android-module-structure` skill (core, feature, etc.) but as packages within `:app`, not separate modules.
- **Git hygiene.** `git add` every new file immediately after creating it. Create meaningful, modular commits at logical checkpoints — don't batch everything into one giant commit.

## Conventions

- Operator symbols in `Operation` use `'x'` for multiply (not `*` or `×`) — the parser, writer's validity checks, and instrumented test all key off `'x'`. The button shows `"x"` and a `"÷"` glyph that is mapped to `Operation.DIVIDE` via the `CalculatorUiAction` (the underlying char in the expression string is `'/'`).
- `operationSymbols` (in `Operation.kt`) is the canonical set used for char membership checks throughout `ExpressionWriter`.
- Tests use Google Truth (`assertThat(...).isEqualTo(...)`) rather than JUnit assertions.
