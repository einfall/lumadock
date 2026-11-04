# Builds and releases

Luma Dock 1.0.0 uses versionCode 100 and retains `com.gathin.tvhome` for existing installations. Increase versionCode for every update; versionName is the visible release label. Official APKs must use the same private signing key. The key is intentionally absent from this repository.

## Build

Use JDK 17, Android SDK Platform 34 and Build Tools 34.0.0. Set `ANDROID_HOME` or create `launcher/local.properties` with your SDK path. For official signing, set:

```text
LUMA_SIGNING_STORE=<absolute path to the private signing store>
LUMA_SIGNING_STORE_PASSWORD=<store password>
LUMA_SIGNING_ALIAS=<existing key alias>
LUMA_SIGNING_KEY_PASSWORD=<key password>
```

Then run `launcher/gradlew -p launcher testReleaseUnitTest lintRelease assembleRelease`, or `launcher/gradlew.bat` on Windows. The output is `launcher/app/build/outputs/apk/release/app-release.apk`. An unsigned or differently signed development build cannot replace the official app. The release APK has debugging disabled; the signing identity is retained from the previously installed custom launcher to preserve upgrades and settings.

## Release assets

Publish a stable release tag such as `v1.0.0` with:

- `Luma-Dock-1.0.0-universal.apk`
- `update.json`
- `SHA256SUMS`

The update manifest has schema_version 1, package_name, version_name, version_code, minimum_sdk, release_notes and `artifacts.universal` containing url, sha256 and size_bytes. The URL must point to that release's APK on `github.com/einfall/lumadock/releases/download/`.

`scripts/prepare-update.py` creates the manifest and checksums from a signed APK. Upload the three files in GitHub Releases. The app reads the manifest through `releases/latest/download/update.json`; prereleases and draft releases do not become the stable update automatically.

The CI workflow builds and tests source, using an ephemeral development signing key. It does not publish that build as an official update. The separate publisher accepts an already signed, owner-prepared APK on a temporary `release-upload/**` branch, verifies its digest, package, version, SDK and signing-certificate digest, uploads the release assets and removes only that temporary branch. The private signing key is never sent to Actions.

## Installation behavior

Info checks for a stable update manually or automatically once a day on Home. Automatic downloads are optional. Every installation opens Android's standard confirmation and may require granting installation from Luma Dock once. Downloaded files live in the app's private cache and are shared only through its scoped FileProvider. Hashes, package names, SDK/version and the installed signing certificate are checked again before installation. A current version can also be downloaded again for recovery; lower versions are rejected.

Keep the signing store backed up privately. Do not commit API keys, ADB keys, PINs, device backups or runtime settings.
