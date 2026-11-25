# Niobium

Niobium is an Android browser project built from Mozilla's open-source Firefox for Android (Fenix) code, with Niobium branding and package identity.

## Android APK

The Android browser source is checked out from Mozilla's own repository:

- Source: https://github.com/mozilla-mobile/firefox-android
- Source revision: Mozilla Firefox for Android repository commit `fe8a71cd70ad5674abe1824fe11dc78372b736c2`
- Build project: `fenix/`
- Engine: Mozilla GeckoView

**Important:** Mozilla archived the `mozilla-mobile/firefox-android` GitHub repository in June 2024 after migrating ongoing Firefox for Android development to Mozilla Central. This build therefore uses Mozilla's genuine upstream Fenix source at that archived revision, not IceRaven. It is not a build of the latest Firefox release.

### Build an APK on GitHub (no PC required)

Pushes to the build workflow or Mozilla source submodule trigger the GitHub Actions build.

1. Open the repository's **Actions** tab.
2. Open the latest **Build Niobium Android APK** run.
3. Wait for the build and APK verification to finish.
4. Download `niobium-mozilla-firefox-android-apks` under **Artifacts**.
5. Extract the ZIP and install the APK on Android.

The APK is a debug build signed with GitHub Actions' temporary debug key. It cannot update an install signed with a different key.

## Firefox WebExtension prototype

The repository root also contains the original desktop Firefox WebExtension prototype. It is separate from the Android app and does not itself produce an APK.

## License

Niobium's own licensing should be considered separately from Mozilla's upstream code. Mozilla Firefox for Android is distributed under the Mozilla Public License 2.0.
