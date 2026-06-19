# Changelog

All notable user-visible changes to **Sifr** (`com.gaddal.materialcalculator`).
This file is the source of truth for Play Console "What's new" copy — each
release block fits inside the 500-character per-locale cap.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
the project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.4.0] — 2026-06-12

`versionCode 7`. The New Look — a ground-up visual redesign of the
calculator surface on a new theme-token system. Adds five hand-crafted
themes (Layl, Bayan, Raqim, Farah, Mizan), each in light and dark, plus a
Material You dynamic option; a restyled keypad and display with per-theme
typography; and a theme picker in the Settings → Appearance section.

### Play Console — English release notes

```
What's new in 1.4.0 — The New Look:

• A fresh visual redesign of the whole calculator
• Five hand-crafted themes — Layl, Bayan, Raqim, Farah, Mizan — each in light and dark
• Material You dynamic color that follows your wallpaper (Android 12+)
• Restyled keypad and display, with a distinct typeface per theme
• Pick your theme any time in Settings → Appearance
```

### Play Console — Arabic release notes

```
ما الجديد في الإصدار 1.4.0 — حُلّة جديدة:

• إعادة تصميم بصري كامل للآلة الحاسبة
• خمس سمات مصمّمة بعناية — Layl وBayan وRaqim وFarah وMizan — بنمطين فاتح وداكن
• ألوان ديناميكية تتبع خلفية شاشتك (أندرويد 12 فأحدث)
• لوحة مفاتيح وشاشة عرض بمظهر جديد، وخط مميّز لكل سمة
• اختر سمتك في أي وقت من الإعدادات ← المظهر
```

### Added (user-visible)

- **The New Look** — a full visual redesign built on a custom theme-token
  layer (`SifrColors`).
- **Five themes** — *Layl* (dark glass, neon-teal glow), *Bayan* (bold ink
  color-blocking), *Raqim* (editorial serif on a hairline grid), *Farah*
  (warm rounded pills), and *Mizan* (machined monospace). Each ships light
  and dark variants.
- **Material You** — a *Dynamic* theme that follows your wallpaper colors on
  Android 12+.
- **Theme picker** — a new **APPEARANCE** section in Settings with a swatch
  for every theme; the choice persists across sessions.
- **Per-theme typography** — each theme pairs its own display, keypad, and
  UI typeface (downloadable Google Fonts).

### Changed

- **Calculator surface** — the keypad, display, and top bar now read every
  color from the active theme's tokens instead of the stock Material
  palette. Each key carries a role (number / operator / equals / function)
  that selects its shape, fill, and press feedback.

### Internal (not user-visible)

- New `core/ui/theme` token layer: `SifrColors` / `SifrKeyStyle` /
  `SifrKeyRole` exposed via `LocalSifrColors` + `SifrTokens`; `SifrTheme`
  bridges a Material 3 `ColorScheme` so stock components stay correct.
- `palette` preference persisted via DataStore; a 6-palette × light/dark
  selector with unit tests (`SifrPaletteTest`, `SifrPalettesTest`).
- Downloadable Google Fonts — no committed font binaries.

### Known limitations

- Some per-theme display polish (ink blocks / cards / insets, Layl's
  blurred `=` glow, Mizan's inner-top key highlight) lands in a later
  release.
- A few icon-only controls (the Delete key, the theme swatches) still need
  TalkBack labels — tracked for a follow-up.

---

## [1.3.0] — 2026-05-12

`versionCode 6`. First post-rebrand feature release. Adds a full scientific
surface, persistent settings, calculation history, haptic + sound feedback,
an adaptive landscape layout, an editable expression display, and complete
Arabic-locale coverage. Internally: rebuilt on AGP 9.2.1 / Gradle 9.5 /
Kotlin 2.3 / JDK 21, restructured into MVI + Koin + Nav3, with a typed
error wrapper and persistence via DataStore + Room.

### Play Console — English release notes

```
What's new in 1.3.0:

• Scientific mode: sin, cos, tan, log, ln, √, π, e, x^y, factorial, deg/rad
• Memory keys: MC, M+, M−, MR
• History — review past calculations, tap any row to bring it back
• Settings: light, dark, or system-follow theme, plus haptic + error-sound toggles
• Adaptive landscape layout
• Better expression display: tap to position the cursor, live preview, auto-shrinking font
• Arabic interface fully covered
```

### Play Console — Arabic release notes

```
ما الجديد في الإصدار 1.3.0:

• الوضع العلمي: sin وcos وtan وlog وln و√ وπ وe وx^y والمضروب وdeg/rad
• مفاتيح الذاكرة: MC وM+ وM− وMR
• السجل — راجع العمليات السابقة، اضغط على أي صف لاسترجاعه
• شاشة الإعدادات: مظهر فاتح، داكن، أو حسب النظام، مع تفعيل الاهتزاز وأصوات الأخطاء
• تخطيط أفقي متكيف
• شاشة عرض محسّنة للمعادلات: انقر لوضع المؤشر، معاينة فورية، حجم خط متكيف
• واجهة عربية مكتملة
```

### Added (user-visible)

- **Scientific mode** — toggle via 🧪 icon. Adds `sin`, `cos`, `tan`,
  `asin`, `acos`, `atan`, `ln`, `log`, `√`, `π`, `e`, `x^y`, `x!`,
  and a `deg/rad` angle-unit toggle. Mode + angle unit persist across
  sessions.
- **Memory keys** — `MC` / `M+` / `M−` / `MR` row with an "M" chip in
  the display when memory is non-empty.
- **History** — Room-backed log of past calculations. Tap any row to
  restore the original expression. Long-press to delete a single row;
  clear-all from the toolbar.
- **Settings screen** — light / dark / follow-system theme; haptic
  feedback toggle; error-sound toggle. All preferences persisted via
  DataStore.
- **Editable cursor** — tap anywhere in the expression to position the
  cursor; type or delete mid-expression; long-press to select a range
  and Copy.
- **Live result preview** — a grayed-out preview of the result renders
  below the expression as you type.
- **Adaptive landscape layout** — scientific and basic blocks side by
  side; status bar hidden on the calculator surface.
- **Haptic + sound feedback** — intent-driven (errors, calculate
  success, destructive actions, history selection). Both gated by the
  Settings toggles. New `VIBRATE` permission.
- **Localized error messages** — five distinct error types (division
  by zero, invalid expression, function domain, overflow, syntax) in
  English and Arabic.
- **Implicit multiplication** — typing `5` then `sin` writes `5×sin(`;
  `π` then `e` writes `π×e`.
- **Smart Delete** — pressing Delete after `sin(` (or any function
  token) peels the entire function-name + open-paren as one step.

### Changed

- **App rebrand** — `MaterialCalculator` → `Sifr` (صفر). Namespace
  changed to `dev.gaddal.sifr`. `applicationId` intentionally
  unchanged at `com.gaddal.materialcalculator` to preserve the live
  Play Store listing.
- **Display precision** — dropped the trailing IEEE-754 noise digit.
  `sin(30°)` now reads `0.5` (was `0.4999999999999999`); `sin(π)` in
  radians snaps to `0` (was `1.22E-16`).
- **Long error text** — wraps to two lines instead of clipping with
  an ellipsis.

### Internal (not user-visible)

- Build: KTS + version catalog; Gradle 9.5; AGP 9.2.1; Kotlin 2.3.21;
  JDK 21 toolchain.
- Architecture: package restructure into `core/` + `feature/`; MVI
  refactor (StateFlow + Root/Screen split); Koin DI; Navigation 3
  1.1.1.
- Typed errors: `Result<D, E : Error>` + `CalcError` + `UiText`,
  replacing the old `"Error"` magic-string sentinel.
- Native debug symbols bundled with the release AAB for better
  Play Console crash reports.
- Lint cleaned to 0 errors / 7 warnings baseline.
- 124 unit tests (was 26 in v1.2.0).

---

## [1.2.0] — 2026-05-09

`versionCode 5`. Phase 1 modernization. Shipped to clear the Play Store
inactivity warning. Build system rebuild on AGP 8.x → 9.x; SDK target
bump to 36; signing path recovered; no user-visible feature changes.
