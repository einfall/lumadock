# Luma Dock

![Luma Dock](branding/banner.png)

Ein anpassbarer, nativer Launcher für **Android TV und Google TV**: Glas-Dock, App-Sortierung, Live-Hintergründe, Widgets, Bildschirmschoner und Updates direkt über GitHub. Java, Android 8.0 / API 26 oder neuer.

**[APK herunterladen](https://github.com/einfall/lumadock/releases/latest)** · [Änderungen](CHANGELOG.md) · [Release-Builds](docs/RELEASES.md) · [Lizenzen](THIRD_PARTY_NOTICES.md)

## Funktionen

- Dock und Raster mit wählbaren Spalten, Paging/Pivot, Sortierung, App-Namen und Icon-Packs.
- Fokusrahmen, Abstand, Stärke, Glanz, Farbverläufe, Glow und Zoom getrennt anpassbar.
- Glas- und Farb-Themes für Dock, Buttons und Widgets; direkte Vorschau in der Einstellungen-Liste.
- Fotos, Verläufe und stumme Live-Hintergründe, Favoriten per langem OK und kombinierbare Wechselregeln.
- Frei platzierbare Uhr, Datum, Wetter und Systemanzeigen; eigene Widget-Quellen und Android-Widgets.
- Spruch-Widget als Text/Bild, Schriftvorschau und Pixel-Ein-/Ausflug mit Ebenen vor/hinter dem Dock.
- PIN-Schutz einzelner App-Starts im Launcher, Ausblenden und Verschieben per langem OK.
- Bildschirmschoner, Sleeptimer mit Countdown und optionale HDMI-CEC-Lautstärkeanzeige.
- **Info** mit Version, täglicher Update-Prüfung, optionalem Download und geprüftem Android-Installer.

## Installation

1. APK aus den [Releases](https://github.com/einfall/lumadock/releases) auf dem TV installieren.
2. Luma Dock öffnen und in Android als Standard-Startbildschirm wählen.
3. Die bisherige Home-App erst nach einem erfolgreichen Test ändern. Die APK selbst entfernt oder deaktiviert keine andere App.

Das App-Paket heißt aus Kompatibilitätsgründen weiterhin `com.gathin.tvhome`: bestehende Installationen dieses angepassten Launchers lassen sich mit identischer Signatur aktualisieren und behalten ihre Einstellungen. Andere APKs mit derselben Paketkennung, aber anderem Schlüssel, lassen sich nicht darüber installieren.

Zusätzliche Herstellerfunktionen wie Start auf Home, Remote-Helfer und HDMI-CEC-Mitlesen sind geräteabhängig. Die optionale lokale ADB-Funktion erfordert eine ausdrückliche Autorisierung am Gerät; sie wird nicht allein durch die APK-Installation eingerichtet. Der App-PIN schützt Starts über Luma Dock und ersetzt keine systemweite Kindersicherung.

## Updates

Unter **Design & Apps → Info**. Standardmäßig wird beim Öffnen von Home höchstens einmal täglich nach einem stabilen Release gesucht. Ein Download erfolgt auf Wunsch automatisch. Die Installation verlangt die normale Android-Bestätigung. APK-Größe, SHA-256, Paket, Version, Android-Kompatibilität und Signatur werden geprüft. Downgrades und andere Signierschlüssel werden abgewiesen.

Manifest: `https://github.com/einfall/lumadock/releases/latest/download/update.json`.

## Selbst bauen

JDK 17, Android SDK Platform 34 und Build Tools 34.0.0 installieren. `launcher/local.properties` mit dem eigenen SDK-Pfad anlegen oder `ANDROID_HOME` setzen.

```sh
cd launcher
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Windows: `gradlew.bat`. Ohne privaten Release-Schlüssel wird mit dem lokalen Entwicklungs-Schlüssel signiert. Dieser Build kann eine anders signierte offizielle Installation nicht aktualisieren. Für offizielle Builds siehe [RELEASES.md](docs/RELEASES.md). Private Schlüssel, Gerätesicherungen und API-Schlüssel gehören nicht ins Repository.

## Herkunft und Lizenzen

Luma Dock basiert auf [CleanYourTV](https://github.com/Gathin23/cleanyourtv). Die MIT-Hinweise bleiben erhalten. Zusätzliche Komponenten und Assets tragen eigene Lizenzen; insbesondere der getrennte Live-Hintergrund-Katalog aus Couchy Launcher steht unter GPLv3. Video-Dateien werden nicht mitgeliefert. Details und Bild-/Schriftquellen: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Die zwei früher privat aus Projectivy extrahierten Hintergrundbilder sind mangels belegter Weiterverteilungsfreigabe nicht Bestandteil dieser Veröffentlichung. Offen lizenzierte Schriften und Material-Outlined-Symbole bleiben enthalten.
