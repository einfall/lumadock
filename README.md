# Luma Dock

![Luma Dock](branding/banner.png)

Ein anpassbarer, nativer Launcher für **Android TV und Google TV**: Glas-Dock, App-Sortierung, Live-Hintergründe, Widgets, Bildschirmschoner und Updates direkt über GitHub. Java, Android 8.0 / API 26 oder neuer.

## Einblicke

Echte TV-Screenshots mit unterschiedlichen Beispielkonfigurationen. Dock, Farben, Schriften, Widgets und Hintergründe lassen sich individuell einstellen.

**Startseite mit Glas-Dock, Wetter und Widgets**

![Luma Dock: Startseite mit Glas-Dock, Wetter und Widgets](docs/screenshots/home.png)

| Design und Schriftvorschau | Live-Hintergründe und Favoriten |
| --- | --- |
| ![Schriftarten mit direkter Vorschau in den Design-Einstellungen](docs/screenshots/design-fonts.png) | ![Live-Hintergründe mit Bildvorschauen und Favoriten](docs/screenshots/live-wallpapers.png) |
| **Widgets und helles Theme** | **Bildschirmschoner** |
| ![Helles Theme mit kombinierter CPU-, RAM- und Speicherübersicht](docs/screenshots/widgets-light.png) | ![Bildschirmschoner mit Uhr und NASA-ISS-Aufnahme](docs/screenshots/screensaver-iss.png) |

Die gezeigten App-Logos und externen Hintergrundinhalte gehören ihren jeweiligen Rechteinhabern; sie illustrieren die Darstellung im Launcher. Quellenhinweise stehen unter [Lizenzen und Herkunft](THIRD_PARTY_NOTICES.md).

## Funktionen

- Dock und Raster mit wählbaren Spalten, Paging/Pivot, Sortierung, App-Namen und Icon-Packs.
- Dock-Ordner mit Glasvorschau, Popup oder senkrechter Liste in Dock-Größe. Bei automatischer Vorschau wählt ↑ die erste App über dem Ordner.
- Fokusrahmen, Abstand, Stärke, Glanz, Farbverläufe, Glow und Zoom getrennt anpassbar.
- Glas- und Farb-Themes für Dock, Buttons und Widgets; direkte Vorschau in der Einstellungen-Liste.
- Fotos, Verläufe und stumme Live-Hintergründe, Favoriten per langem OK und kombinierbare Wechselregeln.
- Frei platzierbare Uhr, Datum und Wetter. Eigene Widgets mit Vorschaukacheln, vier Digitaluhr-Stilen, Monats- und Wochenkalender sowie Android-Widgets.
- Designprofile mit Sicherung und verschlüsselter Übertragung des aktiven Layouts im Heimnetz.
- Echo-Wiedergabe über Home Assistant mit Geräteauswahl, Titel, Cover und Fortschritt.
- Eigener Begrüßungstext als frei gestaltbares Textwidget; Uhrzeit/Datum wahlweise zweizeilig.
- Systeminformationen mit wählbaren Messwerten, Ring-Themes und getrennten Deckkraft-Reglern für Linie und Leuchten.
- Geöffnete Apps per langem Zurück, große App-Kacheln und einzelnes/gesammeltes Schließen über autorisierte lokale ADB.
- Spruch-Widget als Text/Bild, Schriftvorschau und Pixel-Ein-/Ausflug mit Ebenen vor/hinter dem Dock.
- PIN-Schutz einzelner App-Starts im Launcher, Ausblenden und Verschieben per langem OK.
- Bildschirmschoner, Sleeptimer mit Countdown und optionale Lautstärkeanzeige über HDMI-CEC oder ein ausgewähltes Sonos-Gerät im Heimnetz.
- **Info** mit Version, täglicher Update-Prüfung, optionalem Download und geprüftem Android-Installer.

## Download

[Version 1.2.3 herunterladen](https://github.com/einfall/lumadock/releases/tag/v1.2.3). Die signierte APK kann ab Android 8.0 installiert werden. Bestehende Installationen mit derselben Signatur können über **Info** aktualisiert werden.

## Einrichtung auf einen zweiten TV übertragen

1. Öffne am Quellgerät **Design & Apps → Profile & Sicherung → Im Heimnetz übertragen → Dieses Gerät freigeben**.
2. Öffne am Zielgerät **Geräte suchen**, wähle die Quelle und gib den einmaligen Freigabecode ein.
3. Bestätige **Einrichtung übernehmen**. Unter **Profile & Sicherung** kannst du die letzte Übernahme rückgängig machen.

Übertragen werden die Einstellungen des aktiven Luma-Dock-Profils, einschließlich Dock, App-Reihenfolge, Ordnern, Luma-Widgets, Schriften und Hintergrundauswahl. Apps und Icon-Packs werden auf jedem TV installiert. PINs, Zugangsdaten, Berechtigungen, Android-Widget-Verknüpfungen und die Sonos-Zuordnung bleiben gerätegebunden.
