# Screenshots

Two parallel uses for this folder:

1. **Drop-zone for QA captures, device screenshots, bug-report attachments** — claude reads files here when you reference them in conversation (e.g. "look at `screenshots/preview-bug.png`").
2. **Play Console upload-ready assets** — see `EN/` and `AR/` below.

## Play Console upload-ready folders

`screenshots/EN/` and `screenshots/AR/` contain **copies** of the marketing previews (originals stay at the top level as `Marketing — ….png`), renamed and ordered so you can multi-select the 8 files in one folder and drag-drop them into Play Console's Phone-screenshots section in the correct order.

| Slot | EN file | AR file | Surface | Theme |
|---|---|---|---|---|
| 01 | `EN/01. Basic, Light.png` | `AR/01. Basic, Light.png` | Basic mode | Light |
| 02 | `EN/02. Scientific, Light.png` | `AR/02. Scientific, Light.png` | Scientific mode | Light |
| 03 | `EN/03. Landscape, Light.png` | `AR/03. Landscape, Light.png` | Adaptive landscape | Light |
| 04 | `EN/04. History, Light.png` | `AR/04. History, Light.png` | History | Light |
| 05 | `EN/05. Settings, Light.png` | `AR/05. Settings, Light.png` | Settings | Light |
| 06 | `EN/06. Basic, Dark.png` | `AR/06. Basic, Dark.png` | Basic mode | Dark |
| 07 | `EN/07. Scientific, Dark.png` | `AR/07. Scientific, Dark.png` | Scientific mode | Dark |
| 08 | `EN/08. Landscape, Dark.png` | `AR/08. Landscape, Dark.png` | Adaptive landscape | Dark |

Order rationale: marquee shot first (Basic Light), then the v1.3.0 hero feature (Scientific), then adaptive-layout signal (Landscape), then practical-value surfaces (History, Settings), then the Dark variants for the three most-recognizable surfaces. Drops `History, Dark` and `Settings, Dark` — near-identical inversions of their Light versions, low marginal value when Play Console limits to 8 phone screenshots per locale.

When Play Console asks for screenshots:
1. Switch to the **English (United States)** language tab → drag all 8 from `screenshots/EN/` into the phone-screenshots area. They upload in filename order (01 → 08).
2. Switch to the **Arabic** locale tab → repeat with `screenshots/AR/`.

## Play Console screenshot tags

Play Console has a "Manage tags" UI for organizing screenshots. Tags are **for your own organization only** — not shown to users, no effect on search rankings. Useful when the listing has dozens of screenshots across surfaces, themes, locales, and releases.

Suggested tag scheme for Sifr (mirror what's in the filenames):

| Tag | Applied to |
|---|---|
| `release:v1.3.0` | All 16 screenshots in this batch — lets you find them when v1.4.0+ refresh comes |
| `locale:en` | All 8 in `EN/` |
| `locale:ar` | All 8 in `AR/` |
| `theme:light` | Slots 01–05 in each locale (10 total) |
| `theme:dark` | Slots 06–08 in each locale (6 total) |
| `surface:basic` | Slots 01, 06 in each locale (4 total) |
| `surface:scientific` | Slots 02, 07 in each locale (4 total) |
| `surface:landscape` | Slots 03, 08 in each locale (4 total) |
| `surface:history` | Slot 04 in each locale (2 total) |
| `surface:settings` | Slot 05 in each locale (2 total) |

Per-screenshot tag chips (concrete examples):

| File | Tags |
|---|---|
| `EN/01. Basic, Light.png` | `release:v1.3.0`, `locale:en`, `theme:light`, `surface:basic` |
| `EN/02. Scientific, Light.png` | `release:v1.3.0`, `locale:en`, `theme:light`, `surface:scientific` |
| `EN/06. Basic, Dark.png` | `release:v1.3.0`, `locale:en`, `theme:dark`, `surface:basic` |
| `AR/04. History, Light.png` | `release:v1.3.0`, `locale:ar`, `theme:light`, `surface:history` |

…and so on. Four tags per file, all derivable from the filename.

**Skip tags entirely if it feels like overhead** — for a single-developer project with 16 screenshots in one release, the filename ordering already does the job. Tags become valuable on the third or fourth release refresh when you want to compare "what did this look like at v1.3.0" vs "v1.5.0".

## Originals

Source files at the top of `screenshots/` (`Marketing — Basic, AR, Dark.png` etc.) are the unmodified exports from `app/src/debug/.../marketing/MarketingPreviews.kt`. The numbered files in `EN/` and `AR/` are **copies**, not moves — re-running the upload-prep script regenerates `EN/` and `AR/` from the originals.

When a future release exports a new batch (v1.4.0 et al.), re-run the same numbering scheme — see `docs/releases/v1.3.0.md` Step 3.2 for the order rationale.

## Other captures

Suggested naming for ad-hoc captures: `<topic>-<date>.png` — e.g. `preview-rendering-bug-2026-05-13.png`. Drop them at the top of `screenshots/`, alongside the `Marketing — ….png` originals.
