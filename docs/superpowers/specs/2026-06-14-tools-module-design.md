# Sifr — Tools module design (Units · Currency · Tip · Date)

> Status: **DESIGN — approved decisions, pending spec review** · 2026-06-14
> Scope: a new `Tools` destination collecting four converters. The "Open tools" top-bar
> button (GridView `⊞`) is already wired but gated to a no-op; this milestone lights it up.
> Source of truth for layout: the design handoff prototype
> `docs/design_handoff_sifr_redesign/reference_prototype/app/tools.jsx` + `screen-specs.md §Tools`.
> Per [[fidelity-match-prototype]] the structure/spacing/sizes mirror the prototype; the only
> deliberate divergence is Currency (prototype uses a static `SIFR_FX` table → we wire live FX).

## 1. Goals

- Ship the four tools the prototype defines: **Units, Currency, Tip, Date**, as a single
  segmented-tab screen reachable from the calculator top bar.
- Match the prototype's visual + interaction model exactly (segmented control, focusable
  fields fed by a 3-column numpad, big accent result, per-palette theming, RTL mirroring).
- Currency converts using **live exchange rates** (free, keyless), resilient offline via a
  local cache + a bundled seed snapshot.
- Full localization parity: every new string in all **11** locales (en, ar + the 8 Tier-1
  Latin langs), Eastern-Arabic numerals where the rest of the app uses them.

## 2. Non-goals (this milestone)

- Feeding a tool result back into the calculator (ANS-style hand-off). Tools are self-contained.
- A history/log of past conversions.
- Unit categories beyond the prototype's four (Length / Weight / Temp / Data).
- Intraday / streaming FX; per-currency flags; a currency search field (the dropdown list is enough).
- A Tools entry in Settings. The only entry point is the top-bar button.

## 3. User-facing design (mirrors `tools.jsx`)

Single screen, `SifrSegmented`-style 4-tab control on top: **Units · Currency · Tip · Date**.
Below it, the active tool's card; below that (Units/Currency/Tip only) the numpad.

- **Portrait:** segmented control → tool card → numpad stacked.
- **Landscape:** segmented control → row of `[ tool card (scrolls) | numpad (fixed end side) ]`.
  Date has no numpad; in landscape its two sub-cards sit side by side.

Shared field grammar from the prototype:
- **`ToolField`** — a tappable display-font value box. Tapping focuses it (accent border +
  blinking caret); the numpad types into the focused field. Right-aligned (mirrors in RTL).
- **`ToolSelect`** — a unit/currency dropdown (Material 3 `ExposedDropdownMenuBox`).
- **`ToolOut`** — a labelled output; `big` variant renders large + accent (the headline result).
- **`ToolNumPad`** — 3-col grid `7 8 9 / 4 5 6 / 1 2 3 / 0 . ⌫`, built from `CalculatorButton`
  (num role) so it inherits palette styling, press animation, haptics, and `Role.Button` a11y.
- **`Stepper`** — `− value +` pill pair (Tip split count).

### 3.1 Units
- Category chips: **Length · Weight · Temp · Data** (`SifrChip`, active = accent).
- Row 1: from-unit `ToolSelect` + value `ToolField`. Row 2: to-unit `ToolSelect` + big `ToolOut` result.
- Factors from the prototype `SIFR_UNITS` (SI-relative); **Temp** is special-cased (°C/°F/K).
- Changing category resets from/to to that category's first two units (prototype `useEffect`).
- Data category = digital storage (B/KB/MB/GB/TB + KiB/MiB/GiB), decimal **and** binary.

### 3.2 Currency
- Row 1: from-currency `ToolSelect` + value `ToolField`. Row 2: to-currency + big result.
- Result = `value / rate[from] * rate[to]` (rates are per-USD base, as open.er-api returns).
- Footnote: localized "rates as of `<date>` · source" (replaces prototype's static "demo rates").
- Currency list comes from the fetched/seeded rate map keys (≈160), sorted with the user's
  likely currencies (SAR/AED/EGP/USD/EUR…) — exact sort TBD in plan, default alphabetical.

### 3.3 Tip
- Bill `ToolField`; tip-% chips (10/12/15/18/20); split `Stepper` (people, min 1).
- Output row (top hairline): **Tip · Total · Each** (`each` is the big one).
- Pure arithmetic: `tip = bill * pct/100`, `total = bill + tip`, `each = total / split`.

### 3.4 Date
- Two cards (stack in portrait, side-by-side in landscape):
  1. **Difference** — two Material 3 `DatePicker`s → **Days** (big) + **Weeks · Days** breakdown.
  2. **Add** — a date + a days `ToolField` → resulting date (localized `EEE, d MMM yyyy`).
- `java.time` (`LocalDate`, `ChronoUnit.DAYS`); locale-aware month/weekday names; minSdk 24 is
  fine for `java.time` via desugaring (already enabled? — verify in plan; else use `ThreeTenABP`-free
  desugaring config). No numpad on the Date tab; the "add days" field uses the numpad **only if**
  we choose to show a compact pad there — default: a numeric `TextField` (system keyboard) for that
  one field to match the prototype's no-pad-on-Date rule. (Open item O1.)

## 4. Navigation & screen structure

- New `ToolsRoute : NavKey` in `navigation/Routes.kt`.
- `entry<ToolsRoute> { ToolsRoot(windowSizeClass, onNavigateBack = { backStack.removeLastOrNull() }) }`
  in `NavRoot.kt`.
- Wire `onTools = { backStack.add(ToolsRoute) }` in **both** `CalculatorScreen.kt` and
  `CalculatorScreenLandscape.kt` (currently `onTools = {}` gated comments).
- `ToolsRoot` is a sub-screen: back arrow + localized "Tools" title top bar (like History/Settings),
  no end icons (`screen-specs.md §Top app bar`).

## 5. Architecture & packages

Follows the existing layered, MVI, Koin-DI conventions. New package `feature/tools/`:

```
feature/tools/
  domain/
    UnitConverter.kt        // categories, factors, convert(cat, v, from, to); temp special-case
    UnitCategory.kt         // enum Length/Weight/Temp/Data + unit lists
    TipCalculator.kt        // pure fns (or inline in VM — small)
    DateCalculator.kt       // daysBetween, addDays  (java.time)
    CurrencyRepository.kt    // interface: Flow<RatesResource>; refresh()
    Rates.kt                // RatesSnapshot(base, rates: Map<String,Double>, asOf: LocalDate)
  data/
    CurrencyRepositoryImpl.kt   // Ktor fetch → cache → seed fallback
    CurrencyApi.kt              // Ktor service, open.er-api.com
    RatesCache.kt               // DataStore (Preferences or proto) read/write of last snapshot
    dto/RatesDto.kt             // open.er-api response shape
  ui/
    ToolsRoot.kt            // koinViewModel + ObserveAsEvents + windowSizeClass split
    ToolsScreen.kt          // portrait; ToolsScreenLandscape.kt for landscape
    ToolsState.kt / ToolsAction.kt / ToolsEvent.kt
    ToolsViewModel.kt       // SavedStateHandle-backed
    components/             // ToolNumPad, ToolField, ToolSelect, ToolOut, Stepper, ToolTabBar
```

Seed asset: `app/src/main/assets/currency_seed.json` — the prototype's `SIFR_FX` values, stamped
with a seed date. `BASE_URL` (per-variant `Constants.kt`) points at `https://open.er-api.com/v6/`.

## 6. Currency data layer

- **Source:** `GET {BASE_URL}latest/USD` on open.er-api.com — free, **no key**, ≈160 currencies
  **including SAR/AED/EGP/KWD/QAR/BHD/OMR** (frankfurter/ECB was rejected for lacking Gulf currencies).
- **Networking:** Ktor `HttpClient` (CIO/OkHttp engine) + `kotlinx.serialization` (load via
  `android-data-layer` skill; this is the app's first network call). `INTERNET` permission added.
- **Resource flow:** `CurrencyRepository.rates: Flow<RatesResource>` where
  `RatesResource = Loading | Success(RatesSnapshot, stale: Boolean) | SeedFallback(RatesSnapshot)`.
  Errors never block conversion — they downgrade to the cached or seed snapshot and surface a
  quiet note, never a crash.
- **Cache:** persist the last successful `RatesSnapshot` (DataStore). On screen open, serve cache
  immediately, then refresh in the background if cache age > 12h (configurable). Daily-rate source,
  so 12h is generous.
- **First run / offline / fetch failure:** bundled seed snapshot. The footnote reflects which
  source is live ("rates as of `<asOf>`" / "offline — using saved rates" / "approx (bundled)").
- **Typed errors** via the project `Result<T,E>` convention (`android-error-handling`).

## 7. Presentation (MVI)

- **`ToolsState`** (`@Stable`): `activeTab: ToolTab`, per-tool fields (`unitsCat`, `uVal`, `uFrom`,
  `uTo`, `cVal`, `cFrom`, `cTo`, `bill`, `tipPct`, `split`, `date1`, `date2`, `addDays`),
  `focusedField: ToolField?`, `rates: RatesResource`. Derived results computed in the VM, not the UI.
- **`ToolsAction`:** `SelectTab`, `FocusField`, `NumKey(char)` / `Backspace`, `SelectCategory`,
  `SelectUnit(from/to)`, `SelectCurrency(from/to)`, `SetTipPct`, `ChangeSplit(+/-)`, `SetDate(which)`,
  `RefreshRates`.
- **`ToolsEvent`:** `NavigateBack` (+ maybe `RatesRefreshFailed` for a transient snackbar — optional).
- **`ToolsViewModel`** is **SavedStateHandle-backed** (D3): tab + all field values survive process
  death; `rates` reloads from cache. Inject `CurrencyRepository` via Koin (`android-di-koin`).
- Number formatting reuses the calculator's display formatter (Eastern-Arabic numerals under `ar`,
  grouping, trailing-zero trim) — locate and reuse, do not reinvent (`core/ui/util`).

## 8. Shared UI components

All themed via `SifrTokens` and built to the prototype's metrics:
- `ToolTabBar` — reuse/extend `SifrSegmented<ToolTab>` (already generic + a11y radio group).
- `ToolField`, `ToolSelect`, `ToolOut`, `ToolNumPad` (over `CalculatorButton`), `Stepper`.
- RTL: math fields stay LTR-reading but the block mirrors with the locale, exactly like History /
  Settings ([[fidelity-match-prototype]] RTL rule). No positive `letterSpacing`/uppercase under `ar`.

## 9. Strings & localization

New keys (English values indicative; **all 11 locales required** — `lintDebug` enforces
`MissingTranslation`; reuse the Stream-1 subagent translation flow):
`tools_title`, `tools_tab_units/currency/tip/date`, `tools_cat_length/weight/temp/data`,
`tools_result`, `tools_fx_note` (+ `tools_fx_offline`, `tools_fx_seed`), `tools_bill`, `tools_tip`,
`tools_split`, `tools_people`, `tools_out_tip/total/each`, `tools_date_diff`, `tools_date_add`,
`tools_days`, `tools_weeks`, `tools_w`, `tools_d`, plus the existing `calc_open_tools` for the button.
Unit abbreviations (m, ft, kg, KB…) and currency codes (USD, SAR…) stay verbatim (not translated).

## 10. Testing

- **Domain (JVM, Truth):** `UnitConverter` (each category incl. Temp round-trips + known values),
  `DateCalculator` (leap years, negative spans, add across months), Tip math.
- **CurrencyRepository:** fake `CurrencyApi` → assert Success/stale/seed-fallback transitions and
  that a network error downgrades to cache/seed without throwing (`android-testing`, fakes-first).
- **ViewModel:** Turbine over state — tab switch resets focus; numpad edits the focused field;
  category change resets units; SavedStateHandle restore.
- **Previews:** every tab × {light/dark, portrait/landscape, ar/RTL}, loading/offline/seed currency
  states (`compose-preview-coverage`).

## 11. Decisions log

- **D1 — Currency API:** open.er-api.com (free, no key, Gulf currencies). Seed = prototype `SIFR_FX`.
- **D2 — Numpad:** reuse `CalculatorButton` (consistency + inherited a11y).
- **D3 — Persistence:** `SavedStateHandle` for tab + field values (tools hold real user input).

## 12. Risks / open items

- **O1 — Date "add days" input:** numpad is hidden on the Date tab in the prototype. Default to a
  numeric `TextField` for that one field; revisit if it feels inconsistent.
- **O2 — `java.time` on minSdk 24:** confirm core-library desugaring is enabled in `build.gradle.kts`;
  enable it if not (cheap, no API bump).
- **O3 — First network layer:** adds Ktor + serialization + `INTERNET` permission + a network
  security note for the Play listing. Keep the client tiny and tools-local.
- **O4 — Currency list ordering / count:** ≈160 entries in a dropdown is a lot; consider a short
  "frequent" group on top. Non-blocking; default alphabetical.
- **O5 — Result feedback:** none of the tools play the calculator's haptics/sound on result; decide
  whether the numpad keys reuse `FeedbackController` (likely yes, for parity).

## 13. Out of scope / future

ANS hand-off to the calculator, conversion history, more unit categories, currency favourites/search,
offline rate-staleness banner beyond the footnote. Each is a clean follow-up.
