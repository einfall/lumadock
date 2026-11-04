# Changelog

## 1.0.2 — 2026-11-04

Optional dock placement in the Dock Only view: width 60–100%, horizontal alignment and vertical placement within the area below the header. Controls apply live and keep the settings panel open. Existing placement remains the default. Optional iOS-style page dots stay centered at the bottom inside the dock; the active dot follows the app page, including partial final pages. The numeric position pill follows dock placement too.

The opt-in CEC overlay can now also be triggered by passively observed volume keys on fixed-volume HDMI sources, while preserving native key handling. Audio-status parsing supports local playback-device addresses and fractional timestamps. A real percentage still requires received audio-status reports; volume key commands and the fixed Android HDMI level are never converted into a fabricated value. No new permission or dependency. 462 JVM tests pass; Android lint has zero errors.

## 1.0.1 — 2026-11-04

Optional selected-app caption below the focused tile and frame. Select it under Design & Apps → Schriften & Icons → App-Name · Größe & Position. Only the active tile is named; zoom, scrolling, pivot navigation and frame geometry are taken into account. The dock reserves caption space, the caption hides on the top bar, and existing title placement remains the default. App font and caption size remain adjustable. Existing free-position coordinates are preserved when switching modes. 457 JVM tests pass.

## 1.0.0 — 2026-11-04

First public Luma Dock release: native customizable TV home, glass dock, focus effects, widgets, wallpapers and screensavers. New adaptive app icon and TV banners; Info page; daily GitHub update checks, optional downloads, SHA-256/package/version/signature verification and Android-confirmed installation. Compatible existing installations retain their package identity and settings. Public assets exclude the two personal-use Projectivy wallpapers.
