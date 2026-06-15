# Sifr — Settings picker polish (language bottom sheet · equal-width segments)

> Status: **DESIGN APPROVED — QUEUED** · 2026-06-14
> Home branch: **`feature/v1.8.x-latin-languages`** (the branch where the long language list lives).
> Timing: do **after** the Tools milestone lands, bundled into that branch's pre-merge **device-QA** pass.
> Not yet committed — this file is held on disk until we're on the Latin branch; commit it there with
> `git add -f docs/superpowers/specs/2026-06-14-settings-picker-polish-design.md`.

## Why

The v1.8 in-app language picker renders every `AppLanguage` as an inline row inside one Settings card
(`feature/settings/ui/SettingsScreen.kt:125-142`). At 3 languages that's fine; the Latin-languages branch
takes it to **11**, turning it into a tall wall that pushes the rest of Settings down and is awkward to scan.
Separately, `SifrSegmented` sizes each segment to its own label (`SifrSegmented.kt:48-58`, `padding(horizontal = 14.dp)`,
no width sharing) — so **Restore (Result / Expression)** looks lopsided and **Angle (DEG / RAD)** is slightly uneven.

## Decisions (user-approved 2026-06-14)

- **D1 — Language → row + bottom sheet.** Replace the inline `AppLanguage.entries.forEach { SifrRow }` list with a
  **single `SifrRow`** ("Language" label + the current language **endonym** as a trailing value + a `›` chevron,
  `Icons.AutoMirrored` so it flips in RTL). Tapping opens a Material 3 **`ModalBottomSheet`** listing all
  `AppLanguage.entries`, each row = the language's `displayLabel()` with a `Check` (`sifr.accent`) on the selected one.
  The sheet body is **scrollable** (`LazyColumn` or a `verticalScroll` Column) so it scales past 11. Selecting a row
  dispatches `SettingsAction.SetLanguage(lang)` and dismisses the sheet.
- **D2 — Equal-width segments (default).** Change `SifrSegmented` so **all** segments share width equally — no new
  param, applied everywhere (Mode / Angle / Restore alike). Mechanism: wrap the segment `Row` in
  `Modifier.width(IntrinsicSize.Max)` and give each segment `Box` `Modifier.weight(1f)` (+ `fillMaxHeight()`), so every
  segment is as wide as the widest label. Keep the existing per-segment confined `selectable(role = Role.RadioButton)`
  a11y untouched.
- **D3 — Home + timing.** Land both on `feature/v1.8.x-latin-languages`, after the Tools milestone, as part of that
  branch's device-QA pass (the branch is build-green but not yet device-verified — this polish gives the QA pass a
  concrete agenda).

## Affected files

- **Modify** `core/ui/components/SifrSegmented.kt` — equal-width segments (D2). Re-check its `@Preview` and the
  existing Settings previews, since Mode/Angle/Restore visually widen.
- **Modify** `feature/settings/ui/SettingsScreen.kt` — swap the inline language list (`:125-142`) for the collapsed
  row + sheet (D1). Sheet open/closed is **ephemeral UI state** → hold it as `rememberSaveable { mutableStateOf(false) }`
  **local to `SettingsScreen`**, not in the ViewModel (consistent with the project keeping VM state minimal).
- **New (optional)** `feature/settings/ui/components/LanguagePickerSheet.kt` — extract the sheet for a clean preview;
  or inline it in `SettingsScreen`. Decide in the plan (extract if it earns its own preview).

## Strings & localization

- Aim for **zero new string keys** (keeps it off the ×11 `MissingTranslation` treadmill):
  - Row label reuses the existing `settings_language_section` ("Language"); sheet title reuses the same.
  - The trailing current-language **value** and every sheet row are **endonyms** — language-invariant literals already
    produced by `AppLanguage.displayLabel()` (e.g. `"English"`, `"العربية"`, `"Español"`), not resources.
  - a11y stays **string-free** (per the a11y-semantics approach): the sheet rows use
    `selectable(selected, role = Role.RadioButton)` + `selectableGroup()`; the framework localizes "selected" /
    "double-tap to activate" in all 11 locales for free. The chevron row is a plain `clickable(role = Role.Button)`.
  - If a sheet title or content-description genuinely needs a *new* string, that's the ×11 translation cost — flag it
    first; default is to avoid it.
- **RTL / font-scale:** chevron auto-mirrors; sheet content right-aligns under `ar` like the rest of Settings; no
  positive `letterSpacing` / uppercase under `ar`. Equal-width segments must not clip the longest labels (e.g. German
  "Standard", "Bahasa Indonesia" endonyms) at large font scale — verify in the preview + on device.

## Testing

- Primarily **previews + device-QA** (no domain logic changes):
  - Updated Settings preview showing the **collapsed** language row.
  - A preview of the **open sheet** (if extracted) across light/dark + an Arabic/RTL variant.
  - Equal-width segments are covered by the existing Settings previews — re-verify Restore/Angle/Mode look even.
- Optional, low priority (project leans JVM-VM tests, not UI): a Compose UI test that tapping the language row opens the
  sheet and selecting a row dispatches `SetLanguage`. Skip unless cheap.

## Out of scope (YAGNI)

- Search/filter inside the language list (revisit at ~20+ languages).
- Touching the other pickers — Theme swatches (`ThemePicker`) and Keypad layouts (`KeypadLayoutPicker`) are grids, not
  segmented controls, and are unaffected.
- Any change to the language-switch mechanism itself (`SifrLocale`, `AppLanguage`, persistence) — this is pure
  presentation polish.
