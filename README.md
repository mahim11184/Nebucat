# Niobium

Niobium is a Gecko-powered browser project with an Android APK build and a separate Firefox WebExtension prototype.

## Android app (APK)

The Android app is in the `android/` directory and embeds Mozilla **GeckoView**, the Android browser engine used to bring Gecko/Firefox web rendering into an app. It is a real Android application project, not a WebView wrapper.

Current Android app features:
- GeckoView browser with URL/search navigation, including support for entering `.onion` addresses.
- Download handling for files returned by GeckoView, saved under `Downloads/Niobium`.
- A downloads list for opening saved files.
- Optional SOCKS5 proxy preferences with remote DNS, intended to connect to a separately running local Tor service such as Orbot.

### Build an APK on GitHub (no PC required)

Every push to `main` that changes `android/**` or the build workflow starts **Build Niobium Android APK**.

1. Open the repository's **Actions** tab.
2. Open the latest **Build Niobium Android APK** run.
3. When it finishes successfully, open **Artifacts**.
4. Download `niobium-android-debug-apk` and extract `app-debug.apk`.
5. Install the APK on Android. This is a debug build and Android may ask you to allow installs from your file manager/browser.

You can also start a build manually from **Actions → Build Niobium Android APK → Run workflow**.

### Build locally

Install Android SDK and Java 17, then run:

```bash
cd android
gradle assembleDebug
```

APK output:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

### Tor limitations

Niobium does **not** bundle or start the Tor daemon. To use onion services, a separate Tor service must be running and expose SOCKS5 at `127.0.0.1:9050`. The Tor toggle saves Gecko proxy preferences for the next full app launch; it does not test connectivity. Use the Tor Project connection check to verify routing. This app is not the official Tor Browser and does not promise Tor Browser's anti-fingerprinting or anonymity protections.

The Android project uses Mozilla GeckoView stable `160.0.20261116130455` from Mozilla's Maven repository. GeckoView is the embeddable Gecko engine; see the [Mozilla quick-start guide](https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/geckoview-quick-start.html).

## Firefox WebExtension prototype

The repository root also keeps the original desktop Firefox WebExtension files (`manifest.json`, `background.js`, `popup.*`). That extension is separate from the Android app and does not itself produce an APK.

## License

No project-wide license has been declared. GeckoView is distributed under the Mozilla Public License 2.0; that does not automatically license Niobium's own source code.
