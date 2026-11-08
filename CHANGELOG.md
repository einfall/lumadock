# Changelog

## 1.0.9 — 2026-11-08

Add separate 0–100% controls for the neon-ring halo and colored ring/track opacity. Existing widgets retain 100% values by default; surface opacity remains independent. A fully transparent halo removes the glow without hiding the measurement text.

Add a local Textwidget source with editable text (up to 500 characters), bundled/system font previews, 8–120 sp text size, opacity, optional glass background and independent width, height and D-pad placement. The visible original greeting is migrated once, retaining its text and screen position; existing widgets are preserved. The former greeting view is hidden to avoid duplicate text. No web feed is used for text widgets.

Add clock-over-date and date-over-clock header presets with a single-line date, while keeping the previous layouts and the selected clock font. Header visibility and independently placed widgets remain separate.

Add a centered glass recent-app panel with large horizontally browsable app tiles, an individual × under each app and Alle schließen. Open it from Design & Apps → Geöffnete Apps or opt into holding Back for two seconds. Short Back replays the normal system action; unrelated keys and volume repeats are not consumed. On Home the hold is handled directly; the installed Home accessibility helper provides the global shortcut inside other apps. The panel reads tasks with activities or a still-running root process for the current user through the existing authorized loopback ADB connection, and verifies tasks disappear after an explicit close. Paused tasks can already have been reclaimed by Android; this is not a list of every resident process.

Closing uses a package-name-validated local force-stop command. Home packages, active accessibility helpers, TV-input providers, protected platform/vendor packages and system UIDs are excluded. The activity is private and exposes no command or package extras. No remote host, root access, automatic sweeper or periodic app closing is introduced. Local ADB authorization and active debugging are required, as for the CEC reader; when unavailable the panel explains the limitation instead of claiming apps were closed.

Use a stronger rounded glass panel, a distinct outline and 80% background dim. The panel is a floating modal window; optional native blur is used only on Android 12+ devices reporting OS support, with dim as fallback. The Shield on Android 11 uses the stronger dim. Missing CEC volume feedback is labelled unavailable after a short wait; no percentage is invented and no direct Sonos access is introduced.

500 JVM tests pass; Android lint has zero errors. Native TV QA covers individual/all closing, short/held Back injection, ring opacity, text editing/font/size/placement and both clock layouts. Real-remote validation is recorded separately in the local test evidence.

## 1.0.8 — 2026-11-06

Keep the Dock Only band at the configured viewport width, including the final page and the last pivot positions. Empty trailing slots no longer shrink the band. Clip both app tiles and item-decoration effects horizontally to the dock viewport, so glass, outlines and shine from adjacent off-screen icons do not leak outside the dock. Vertical focus zoom remains available. Page dots keep the same band center.

Use one Systeminformationen widget source with selectable CPU, RAM, storage and network metrics and all appearance themes inside the same widget. Legacy individual metric instances are read as system-information instances with their original selected data, IDs, positions, dimensions and themes. Nothing is automatically removed or added. An empty metric selection hides the widget and requests no measurements.

Add two circular themes: leuchtender Ring with rounded progress and layered glow, and Donut with a thicker flat-ended arc. CPU is blue, RAM magenta, storage orange. Rings can be stacked vertically or laid out horizontally, with a saved 0–32 dp gap. Circles preserve their aspect ratio within the chosen size. Network rings show actual download/upload MB/s, without an invented percentage. Rendering uses native strokes, without blur or continuous animation.

Optionally color the selected app name with the icon’s dominant color. The switch under Schriften & Icons → App-Name · Größe & Position applies to both the large title and the caption below the focused app. It is off by default; font, size and placement remain independent.

490 JVM tests pass and Android lint has zero errors. TV screenshots verify dock width/edge clipping, system-widget metric selection and migration, ring themes, and the optional app-title color. Existing user widget instances and personal layout settings are preserved. No new dependency or permission.

## 1.0.7 — 2026-11-05

Remove the automatic system display and the duplicate three-cards/combined-card selector. There is now one widget system: add CPU, RAM, storage, network or Systemübersicht yourself, with independent visibility and removal for every instance. Existing independently added widgets remain unchanged. The old global switches and legacy card positions no longer create visible system cards, including after an update or restart. The shared sampler now reads only the metrics requested by enabled widget instances. Its refresh interval is available directly in the widget menu.

Internal metric widgets can shrink to 64 × 32 dp. At smaller sizes the complete content scales proportionally inside the selected width and height; text, icons and bars are not cropped or stretched. Automatic height also follows smaller widths. The four system-information layouts keep their glass/light options and independent sizing. Quote and Android widgets retain their own size controls.

478 JVM tests pass; Android lint has zero errors. Philips device checks cover absence of legacy cards/menu, adding a system widget, 160 × 96 and 64 × 32 dp sizes, persistence after reopening, hiding, removing and a tiny independent CPU widget. Test-created widgets are removed; existing independent widgets stay in place. No new permission or dependency.

## 1.0.6 — 2026-11-05

The four system-information layouts now offer a transparent glass surface, softly lit outlines and readable light text with blue, violet, green and cyan accents. Switch between glass and the previous light appearance inline, with live preview and adjustable glass opacity from 0–90%. No blur, image capture or additional background work. The original widget layout keeps its material theme.

Set widget width and height independently. Internal system, CPU, RAM, storage and network widget instances now support a persistent custom height, alongside existing quote and Android-widget size controls. System rows distribute the available height without stretching text or icons. Readable minimums follow the selected layout and metric count; the screen limits oversized values. Widget-Höhe automatisch restores natural sizing without changing width, placement or data settings. The combined system overview stores its own dimensions, material and opacity. Existing sizes remain automatic until edited. Coordinate limits follow the new widget dimensions.

477 JVM tests pass; Android lint has zero errors. Philips device screenshots verify all four glass layouts, the light alternative, opacity changes, independent width/height changes, saved dimensions after reopening and automatic height restoration. No new dependency or permission.

## 1.0.5 — 2026-11-04

Move the caption beneath the focused app with the same fullscreen D-pad placement control as widgets: Design & Apps → Schriften & Icons → App-Name · Größe & Position → App-Name frei verschieben. Arrows preview the position, OK saves a relative offset that follows the focused tile, and Back cancels. A separate reset restores the default caption position without changing the large app-name layout.

Four optional system-information layouts complement the existing appearance: Gefüllte Zeilen, Systemressourcen, Kompakte Liste and Pastellkarten. Enable them on the combined Systemübersicht or independently created system widgets. Optional network statistics display combined physical LAN and WLAN download/upload in decimal MB/s, averaged over the existing configurable sampling interval. VPN, loopback and bridge counters are excluded. A separate network widget source is also available. Sampling runs only for visible Home widgets; unavailable counters display a dash. Restricted devices use the existing authenticated local ADB connection for this fixed read-only counter query, without arbitrary shell commands or additional permissions.

472 JVM tests pass; Android lint reports zero errors. Live TV checks cover caption preview/cancel/save/focus-following, all four widget styles, and real numeric network rates. Existing settings and the original widget appearance remain the defaults.

## 1.0.4 — 2026-11-04

Move the dock with the D-pad using the same fullscreen placement control as widgets. Open Design & Apps → Layout & Dock → Dock verschieben in Dock Only view. Arrows preview the new screen position, OK saves and enables the custom position, and Back cancels without storing coordinates. The shared movement step is adjustable from 1–20 dp. The dock can move horizontally even at full width, within the available screen edges. Its focus frame, selected-app caption and page dots follow the move. Existing positions remain unchanged until a move is confirmed; the custom-position switch returns to the previous layout when disabled. Dock width remains adjustable; legacy percentage positions remain compatible.

Live device checks cover both axes, cancellation, saving and reopening, the top-left screen boundary, page dots, and restoring the previous layout. 467 JVM tests pass; Android lint has zero errors. No new permission or dependency.

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
