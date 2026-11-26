# Niobium

Niobium is an Android browser built from Mozilla's official Firefox source tree, using the Gecko engine through GeckoView. It is not an Android WebView browser.

## Android source

- Upstream: https://github.com/mozilla-firefox/firefox
- Source location: Mozilla Central (Firefox for Android / Fenix, Android Components, and GeckoView)
- Engine: GeckoView
- App ID: `com.mahim11184.niobium.debug` for the debug APK

The old `mozilla-mobile/firefox-android` repository was archived by Mozilla. The Android browser source is now maintained in Mozilla Central, so the APK workflow clones the official `mozilla-firefox/firefox` repository directly and uses Mozilla's `mach` bootstrap/build commands.

## Android APK (no PC required)

A push to the build workflow or a manual workflow dispatch starts GitHub Actions.

1. Open the repository's **Actions** tab.
2. Open **Build Niobium Android APK**.
3. Wait for the build and APK verification to finish.
4. Download `niobium-mozilla-central-android-apks` under **Artifacts**.
5. Extract the ZIP and install the APK on Android.

The build uses Mozilla's Android artifact mode to download prebuilt Gecko native components rather than compiling all Gecko C++ code from scratch. The first build may take a long time and can fail if GitHub's runner runs out of disk, memory, or time. An APK is available only when the workflow completes successfully.

The debug APK is signed with GitHub Actions' temporary debug key. It cannot update an install signed with a different key.

## Add-ons / extensions

Niobium uses Firefox for Android's GeckoView WebExtensions implementation, not Chromium's extension system.

The upstream Android frontend includes Mozilla's AMO (addons.mozilla.org) add-on manager and the **Custom Add-on Collection** setting. In the debug build, enable it as follows:

1. Open **Settings → About Niobium**.
2. Tap the Niobium logo five times to enable the debug menu.
3. Return to **Settings** and open **Advanced → Custom Add-on Collection**.
4. Enter the numeric owner ID and collection name (slug) from an AMO collection.
5. Reopen Niobium, then open **Add-ons** to install extensions from that collection.

This allows installing many more AMO-listed Android-compatible extensions than a fixed recommended list. It does **not** guarantee every desktop Firefox extension will work: extensions that require unsupported APIs, desktop-only UI, or incompatible permissions may still fail on Android.

## Firefox WebExtension prototype

The repository root also contains the original desktop Firefox WebExtension prototype. It is separate from the Android app and does not itself produce an APK.

## License

Mozilla Firefox for Android source is distributed under the Mozilla Public License 2.0. Niobium's own code and branding should be considered separately.
