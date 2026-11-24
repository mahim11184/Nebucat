/* Niobium background controller. Firefox WebExtensions use Gecko's browser APIs. */
const DEFAULTS = {
  torEnabled: false,
  torHost: "127.0.0.1",
  torPort: 9050
};

browser.runtime.onInstalled.addListener(async () => {
  const saved = await browser.storage.local.get(DEFAULTS);
  await browser.storage.local.set(saved);
});

browser.proxy.onProxyError.addListener((error) => {
  console.error("Niobium proxy error:", error.message, error);
});

browser.runtime.onMessage.addListener(async (message) => {
  if (message?.type === "GET_PROXY_STATE") {
    const settings = await browser.proxy.settings.get({});
    const saved = await browser.storage.local.get(DEFAULTS);
    return {
      torEnabled: Boolean(saved.torEnabled),
      proxyLevel: settings.levelOfControl,
      proxyValue: settings.value
    };
  }
  if (message?.type === "SET_TOR") {
    const enabled = Boolean(message.enabled);
    const saved = await browser.storage.local.get(DEFAULTS);
    const host = String(saved.torHost || DEFAULTS.torHost);
    const port = Number(saved.torPort || DEFAULTS.torPort);

    if (enabled) {
      await browser.proxy.settings.set({
        value: {
          proxyType: "manual",
          socks: host,
          socksPort: port,
          socksVersion: 5,
          proxyDNS: true,
          passthrough: "localhost, 127.0.0.1"
        },
        scope: "regular"
      });
    } else {
      await browser.proxy.settings.clear({ scope: "regular" });
    }

    await browser.storage.local.set({ torEnabled: enabled });
    return { ok: true, enabled };
  }
  if (message?.type === "SAVE_TOR_ENDPOINT") {
    const host = String(message.host || "").trim();
    const port = Number(message.port);
    if (!host || !Number.isInteger(port) || port < 1 || port > 65535) {
      throw new Error("Enter a valid proxy host and port.");
    }
    await browser.storage.local.set({ torHost: host, torPort: port });
    return { ok: true };
  }
});
