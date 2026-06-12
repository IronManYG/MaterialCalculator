# Sifr Visual Redesign — Roadmap & Architecture Spec

> **Status:** Approved 2026-06-11. Source of truth for sequencing the v2.0-concept redesign.
> **Design handoff:** `docs/design_handoff_sifr_redesign/` (final fidelity — `design-tokens.json`, `compose-implementation.md`, `screen-specs.md`, runnable `reference_prototype/`).
> **Codebase:** `dev.gaddal.sifr` · Compose · Material 3 · MVI + Koin + Nav3 · Room + DataStore.
> **Constraint:** `applicationId` stays `com.gaddal.materialcalculator` (preserves the live Play listing). Namespace stays `dev.gaddal.sifr`.

## 1. What this is

A complete visual + feature redesign of Sifr, recreating the high-fidelity handoff inside the **existing** Compose codebase — reusing its established patterns (MVI, DataStore, Room, the existing `ExpressionWriter → ExpressionParser → ExpressionEvaluator` engine). This is **UI + theming + new feature screens, not an engine rewrite.** The prototype's `app/engine.js` only mirrors the Kotlin engine so the prototype runs standalone; it is not a port target.

It introduces:
- A **custom theme layer** — 5 palettes (`Layl`, `Bayan`, `Raqim`, `Farah`, `Mizan`) × light/dark, plus the existing Material You dynamic option as a 6th choice.
- **4 keypad layouts** — `Classic` (exists), `Remix`, `Arc`, `Tape`.
- A **new `feature/tools/` module** — Units / Currency / Tip / Date.
- **Display upgrades** — result actions (copy / share / use-as-answer), fraction results, per-theme display containers.
- **In-app language switch** (English / العربية) with live RTL.

## 2. Release strategy — incremental minors

Each coherent slice ships as its own minor release. Shippable and testable at every step; fresh Play screenshots sooner; risk de-risked by getting the foundation on real devices before features are layered on top. The marketing **"v2.0"** label is optional — applied to whichever release reads as the culmination, or skipped.

**Only the next release in line gets a detailed implementation plan at a time.** This spec is the standing roadmap; per-release plans are written just-in-time.

## 3. Architecture approach — custom token layer

Material 3's `ColorScheme` has no slots for operator-key color, equals-key glow, mosaic grid line, key drop-shadow, or per-theme construction flags. So:

- Introduce a **`SifrColors`** `@Immutable` data class exposed via a `staticCompositionLocalOf` (`LocalSifrColors`), accessed through a `SifrTheme.colors` object.
- Still build a **real `ColorScheme`** (`toMaterialColorScheme`) for ripples / system surfaces — `primary = accent`, `onSurface = text`, `outline = hairline`, etc.
- Both are produced from one `SifrPalette` + mode by a `sifrColorsFor(palette, dark, accent?)` selector.
- `SifrTheme {}` wraps `MaterialTheme`, provides `LocalSifrColors`, and drives the edge-to-edge status-bar icon appearance from `statusBarLightIcons`.

Theme files live in `core/ui/theme/`: `SifrPalette.kt`, `SifrColors.kt`, `Color.kt` (extend), `Theme.kt` (extend), `Type.kt` (extend), `Shape.kt`. Construction flags (`mosaic`, `hairlineGrid`, `soft3d`/`dropShadow`, glow, recessed `displayInset`) are fields on `SifrColors`, rendered in `CalculatorButton` / `CalculatorDisplay`.

Settings domain gains enums: `SifrPalette { LAYL, BAYAN, RAQIM, FARAH, MIZAN, DYNAMIC }`, `KeypadLayout { CLASSIC, REMIX, ARC, TAPE }`, `AppLanguage { SYSTEM, EN, AR }`. `AppSettings` is extended with `palette`, `keypadLayout`, `language`, `memoryRow`, `fractionResults` (+ optional `accentOverride`), each persisted in `SettingsRepositoryImpl` by enum `.name`.

## 4. Baked-in decisions

1. **Default palette = `LAYL`**, `themeMode` stays `SYSTEM`. A light-mode user opens the redesign to `laylLight` — the new look, in their existing mode. Honors the rebrand without a jarring dark-mode surprise.
2. **All 5 themes ship in the foundation release.** A 2-theme picker undersells the headline; the bespoke construction risk (glass glow, mosaic, 3D pill, recessed gradient, hairline grid) is front-loaded into the foundation phase.
3. **Currency = static rates first**, live FX deferred to a follow-up. Keeps the Tools module free of a network/data layer + API-key setup on its first ship. The tab shows a visible "demo rates" note.
4. **Refresh the Play listing at the foundation release** (new screenshots + the deferred 512×512 app icon + 1024×500 feature graphic). That is when the look actually changes; the deferred Phase-2.11 graphics item lands here.
5. **Bundle the 1–2 critical display fonts as `assets`**, downloadable Google Fonts for the rest — kills the first-load fallback flash and the hard Play-Services dependency on the most-visible glyphs. Final call made during foundation-release planning.
6. **Arc + Tape may split** out of the layouts release if heavy (Arc's programmatic operator-arc; Tape's in-display history receipt), shifting the v1.6 → v1.7 boundary rather than bloating one release.

## 5. Phase roadmap

### v1.4.0 — "The New Look" (foundation)
- `SifrColors` + `LocalSifrColors` + `SifrTheme` wrapper + `toMaterialColorScheme`.
- All 5 palettes × light/dark (10 `SifrColors`) + `DYNAMIC` (map existing dynamic scheme) = the full picker.
- Per-theme construction: Layl glass + eq-glow, Bayan mosaic grid, Raqim hairline grid + italic, Farah 3D pill shadow, Mizan recessed gradient.
- 6 Google Fonts wired in `Type.kt` (with decision #5 on bundling).
- **Classic** keypad / display / top-bar restyled to tokens (`CalculatorButton`, `CalculatorButtonGrid`, `CalculatorDisplay`, `AutoSizingExpressionField`, top bar).
- Settings **APPEARANCE** section: Mode segmented + 5 theme swatches + Dynamic as 6th tile.
- DataStore: `palette` key (+ `accentOverride` if included).
- Preview coverage for restyled components across themes/modes.
- Refresh Play listing (decision #4).

### v1.5.0 — "Display & actions" (polish)
- Result-actions row (`COPY` / `SHARE` / `ANS →`) via existing `FeedbackController` + Android share intent.
- Fraction results: `formatFraction(value)` presentation helper (continued-fraction → n⁄d; mirror `toFraction` in `engine.js`).
- Per-theme display containers finalized (Bayan ink block, Farah floating card, Mizan recessed inset).
- Settings **DISPLAY** (fraction toggle, angle unit) + **FEEDBACK** (haptic, error sound) + **ABOUT**.
- Folds in deferred `memoryValue → DataStore` (M chip survives force-stop).

### v1.6.0 — "Keypad layouts" (feature)
- `KeypadLayout` enum + Settings **KEYPAD** section (mini-diagram picker + memory-row toggle).
- **Remix** grid (4×5, 2-col `0`), then **Arc** (3-col pad + quarter-arc operators + large `=`), then **Tape** (Classic + inline-history receipt inside display top).
- Per decision #6, Arc/Tape may slip to v1.7.

### v1.7.0 — "Tools" (new module)
- `feature/tools/` mirroring `feature/settings/` (`domain/` + `ui/` + `di/`): segmented control Units · Currency · Tip · Date.
- Units (category chips + factors), Tip (bill/%/split), Date (difference + add-days) fully functional.
- Currency with **static rates + demo-rates note** (decision #3); compact 3-col numpad feeding the active field; landscape two-pane (Date = two cards, no numpad).
- Nav entry from the top-bar Tools icon.
- **Live FX** is a separate follow-up (e.g. v1.7.1).

### v1.8.0 — "Language & landscape/RTL" (polish)
- In-app `AppLanguage` (English / العربية) with live RTL flip via `LocalLayoutDirection`; reconcile `design-tokens.json → i18n` with `res/values-ar/strings.xml`.
- **Chrome flips** (top bar, settings, tools tabs, history); **keypad + numeric display stay `LayoutDirection.Ltr`** (math reads L→R) — wrapped explicitly.
- Numbers stay Western; dates format with `ar` locale month names.
- Landscape two-pane refinement across all themes + layouts.
- Folds in `Icons.Default.ArrowBack → Icons.AutoMirrored.Filled.ArrowBack`.

## 6. Cross-cutting concerns

- **Engine parity:** reuse the existing Kotlin engine unchanged. Only add two presentation helpers: `formatFraction(value)` and the result-actions (copy / share / use-as-answer). Do **not** port `engine.js` back.
- **RTL discipline:** keypad and numeric display always LTR; everything else flips with `AppLanguage`.
- **applicationId untouched** — do not change `com.gaddal.materialcalculator`.
- **Preview-first:** agents can't see the running app. Every restyled/new screen ships `@Preview` coverage for each reachable state, sampled across themes/modes (`compose-preview-coverage`).
- **Deferred Phase-2.11 items fold in:** `memoryValue → DataStore` (v1.5), `ArrowBack → AutoMirrored` (v1.8), listing graphics (v1.4).

## 7. Deferred / out of scope (YAGNI)

- **Live currency FX** — static rates ship first; live source is a post-Tools follow-up.
- **Arabic-Indic numerals** — numerals stay Western (matches shipped app, avoids glyph fallback); revisit only if paired with a font that includes them.
- **CI/CD auto-publish** — out of scope for the redesign; tracked separately (Phase 2.11+). Keystore + `local.properties` currently live only on the Windows machine, so this is deferred until those are recovered locally.

## 8. References

- Handoff: `docs/design_handoff_sifr_redesign/{README,screen-specs,compose-implementation}.md` + `design-tokens.json` + `reference_prototype/`.
- Existing theme: `core/ui/theme/{Color,Theme,Type}.kt`.
- Existing settings: `core/domain/settings/{AppSettings,ThemeMode}.kt`, `feature/settings/`.
- Existing calculator UI: `feature/calculator/ui/`.
