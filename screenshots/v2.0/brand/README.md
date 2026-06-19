# Sifr — Brand tokens

Three machine-readable representations of this app's design tokens, generated from `output/app.config.ts`.

## Files

- `tokens.json` — Style Dictionary token tree. Run through `style-dictionary build` to emit platform outputs (CSS, iOS, Android, JS).
- `tailwind.theme.ts` — Tailwind `theme.extend` block. Import and merge into your `tailwind.config.ts`.
- `brand.css` — Plain CSS custom properties under `:root` and per-theme `[data-theme="…"]` selectors.

## Themes

### `layl-dark` (default)

- `bg`: `linear-gradient(160deg, #101A2E 0%, #070A12 100%)`
- `fg`: `#EDF1F7`
- `accent`: `#5CE8D4`
- `muted`: `#5A6478`
- `brand-dark`: `#03201B`

### `layl-light`

- `bg`: `linear-gradient(160deg, #EDF4F9 0%, #DCE7F1 100%)`
- `fg`: `#16202E`
- `accent`: `#0E9C8C`
- `muted`: `#6A7689`
- `brand-dark`: `#03201B`

### `farah`

- `bg`: `linear-gradient(160deg, #FFF4E4 0%, #FBE6CC 100%)`
- `fg`: `#4A3326`
- `accent`: `#F2683C`
- `muted`: `#B08A62`
- `brand-dark`: `#FFF6EC`
