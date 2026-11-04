# Changelog

## 1.0.3 — 2026-11-04

Optional navigation sounds under Design & Apps → Ton & HDMI-CEC: keep the existing system sounds (default), use an original soft Luma click, or mute normal click feedback within Luma Dock. The Luma click has its own 0–100% level, a preview button and selectable Media (HDMI/ARC, default) or System audio routing. Media routing supports devices that silence their system-audio path. D-pad, OK and Back feedback works on Home, in settings and in launcher popups. Normal view sounds are suppressed in the custom mode to avoid duplicate clicks, and their original flags are restored when switching back to System. Volume, Home, power and text keys do not trigger the custom click. Held arrows are rate-limited; holding OK does not keep playing clicks.

The short sample is synthesized from source by scripts/generate-navigation-click.py, preloaded with SoundPool, and needs no network, permission or audio focus change. No global TV audio setting or other app is changed. 467 JVM tests pass; Android lint has zero errors. The on-device listening test passed using the media audio path with global system clicks disabled.

## 1.0.2 — 2026-11-04

Optional dock placement in the Dock Only view: width 60–100%, horizontal alignment and vertical placement within the area below the header. Controls apply live and keep the settings panel open. Existing placement remains the default. Optional iOS-style page dots stay centered at the bottom inside the dock; the active dot follows the app page, including partial final pages. The numeric position pill follows dock placement too.

The opt-in CEC overlay can now also be triggered by passively observed volume keys on fixed-volume HDMI sources, while preserving native key handling. Audio-status parsing supports local playback-device addresses and fractional timestamps. A real percentage still requires received audio-status reports; volume key commands and the fixed Android HDMI level are never converted into a fabricated value. No new permission or dependency. 462 JVM tests pass; Android lint has zero errors.

## 1.0.1 — 2026-11-04

Optional selected-app caption below the focused tile and frame. Select it under Design & Apps → Schriften & Icons → App-Name · Größe & Position. Only the active tile is named; zoom, scrolling, pivot navigation and frame geometry are taken into account. The dock reserves caption space, the caption hides on the top bar, and existing title placement remains the default. App font and caption size remain adjustable. Existing free-position coordinates are preserved when switching modes. 457 JVM tests pass.

## 1.0.0 — 2026-11-04

First public Luma Dock release: native customizable TV home, glass dock, focus effects, widgets, wallpapers and screensavers. New adaptive app icon and TV banners; Info page; daily GitHub update checks, optional downloads, SHA-256/package/version/signature verification and Android-confirmed installation. Compatible existing installations retain their package identity and settings. Public assets exclude the two personal-use Projectivy wallpapers.
