# DuckPort identity concept

Selected DuckPort name and original vector logo for the app formerly displayed as Dioxamine. The identity is applied to localized application labels, the app header, splash screen, legacy and adaptive launcher icons, and the foreground-service notification icon. The existing application ID is retained for in-place updates and data continuity.

The mark combines a geometric duck silhouette with a terminal prompt (`>_`) in its wing. The mint body echoes the redesigned app's cyan accent. A marigold beak gives it a friendly point of contrast.

## Name shortlist

- **DuckPort** — recommended: short, friendly, and broad enough for ADB, mirroring, and Fastboot.
- **Quack ADB** — playful and immediately clear about the main feature.
- **ByteDuck** — a mascot-led name with a developer feel.
- **DuckPilot** — emphasizes controlling and navigating devices.
- **Puffin ADB** — an alternative bird mascot with a distinctive beak.
- **OtterLink** — a friendly connection-oriented direction beyond the duck theme.

These are creative naming suggestions; no store, domain, or trademark availability search has been performed.

## Assets

- `duckport-brand-preview.png` — naming and visual identity board.
- `duckport-logo.svg` / `.png` — transparent full-color mark.
- `duckport-app-icon.svg` / `.png` — dark app-icon concept, PNG at 1024 x 1024.
- `duckport-app-icon-light.svg` / `.png` — light-background variant.
- `duckport-mono.svg` / `.png` — single-ink rendering for a light background.
- `duckport-icon-48.png` — small-size legibility check.

Colors: charcoal `#111C23`, mint `#6EE7E0`, marigold `#FFC96B`, warm white `#F5F5ED`.

The SVG is the editable source. The board uses Segoe UI with Arial/sans-serif fallbacks. App-icon files contain only paths and shapes, with no font dependency. Native Android vector drawables, separate adaptive foreground/background layers, Android 13+ monochrome icons, and legacy density-specific PNGs have been generated in `app/src/main/res`.

`apply-android-branding.cjs` regenerates the Android artwork and localized display names. `deliverables/DuckPort-play-store-icon.png` is the 512 x 512 square store asset; platform masking supplies its rounded presentation. See `../PLAY_STORE_NAMING.md` for the recommended listing title.

To regenerate artwork, run `npm install` in this directory, followed by `npm run preview` or `npm run apply-android`. These tools are optional and are not required to build the Android app.
