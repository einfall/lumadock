# Luma Dock development

Native Java Android TV / Google TV launcher. Use JDK 17, Android SDK 34 and build-tools 34.0.0. Run `launcher/gradlew -p launcher testReleaseUnitTest lintRelease assembleRelease` after launcher changes; keep lint at zero errors. Verify UI changes with screenshots from an authorized TV. Avoid force-stopping the default launcher during helper tests: it disables accessibility helpers. Preserve HDMI, input, remote, Bluetooth, Play services and stock-home recovery.

Never commit signing stores, API keys, PINs, device dumps, private addresses or runtime preferences. Official updates must preserve signing identity and pass hash/package/version/signature validation. Android confirms installation. Preserve third-party notices and exclude assets lacking a redistribution grant. APK binaries belong in Releases rather than main.
