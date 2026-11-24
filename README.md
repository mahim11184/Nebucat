# Niobium — Firefox / Gecko edition

Niobium is being rebuilt as a Firefox WebExtension. Firefox uses the **Gecko** browser engine; this project is an add-on that runs inside Firefox, not a replacement browser engine.

## Features in 0.2.0

- Download manager popup: view recent downloads, filter them, add a URL, pause/resume/cancel active downloads, open completed files, and reveal them in the file manager.
- Optional Tor SOCKS5 proxy configuration for Firefox, defaulting to `127.0.0.1:9050`.
- Local settings are stored with Firefox extension storage.

## Install in Firefox

1. Download this repository as a ZIP and extract it.
2. Open `about:debugging#/runtime/this-firefox`.
3. Choose **Load Temporary Add-on…**.
4. Select the `manifest.json` file in the extracted folder.

Temporary add-ons are removed when Firefox restarts. For a permanent install, the add-on must be packaged and signed through Mozilla Add-ons.

## Tor setup

This add-on **does not include or launch Tor**. Install and run a separate Tor service first, then configure its SOCKS5 endpoint in Niobium. The default is:

- Host: `127.0.0.1`
- Port: `9050`
- SOCKS: `5`
- Remote DNS through SOCKS: enabled

Enable **Tor proxy** in the add-on. Use the Tor Project's connection check to verify the browser is actually reaching Tor: https://check.torproject.org/

A green/configured status only means Firefox accepted the proxy settings; it does **not** prove the Tor daemon is running, prove every browser feature is proxied, or guarantee anonymity. Browser extensions cannot turn Firefox into the Tor Browser bundle. For stronger anonymity and anti-fingerprinting protections, use the official Tor Browser: https://www.torproject.org/

Disabling Tor mode clears this add-on's proxy override and returns Firefox to its normal proxy configuration.

## Development

No build step or paid service is required. The source is plain HTML, CSS, and JavaScript using Firefox's WebExtension APIs.

## Permissions

- `downloads`: manage Firefox downloads.
- `storage`: save the Tor SOCKS5 endpoint and toggle state.
- `proxy`: configure Firefox's proxy settings when the user explicitly enables Tor mode.

## License

No license has been declared yet. Do not assume the code is licensed for redistribution until a license is added.
