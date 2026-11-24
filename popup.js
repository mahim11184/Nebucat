const $ = (id) => document.getElementById(id);
const api = typeof browser !== "undefined" ? browser : null;
let allDownloads = [];

function prettySize(bytes) {
  if (bytes == null || bytes < 0) return "—";
  if (bytes < 1024) return bytes + " B";
  const units = ["KB","MB","GB","TB"];
  let value = bytes / 1024, i = 0;
  while (value >= 1024 && i < units.length - 1) { value /= 1024; i++; }
  return value.toFixed(1) + " " + units[i];
}
function safeName(item) {
  const raw = item.filename || item.url || "Download";
  return raw.split(/[\\/]/).filter(Boolean).pop() || raw;
}
function statusText(item) {
  if (item.state === "in_progress") return item.paused ? "Paused" : "Downloading";
  if (item.state === "complete") return "Complete";
  if (item.error) return "Failed: " + item.error;
  return "Canceled";
}
async function loadDownloads() {
  if (!api) return;
  try {
    allDownloads = await api.downloads.search({ limit: 100, orderBy: ["-startTime"] });
    renderDownloads();
  } catch (error) {
    $("download-list").innerHTML = '<div class="empty">Could not read downloads: ' + escapeHtml(error.message) + '</div>';
  }
}
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
}
function renderDownloads() {
  const query = $("filter").value.toLowerCase();
  const items = allDownloads.filter(d => (safeName(d) + " " + (d.url || "")).toLowerCase().includes(query));
  const active = allDownloads.filter(d => d.state === "in_progress").length;
  $("download-count").textContent = allDownloads.length + " downloads";
  $("active-count").textContent = active + " active";
  if (!items.length) {
    $("download-list").innerHTML = '<div class="empty">' + (query ? "No matching downloads." : "Your downloads will appear here.") + '</div>';
    return;
  }
  $("download-list").innerHTML = items.map(item => {
    const percent = item.totalBytes > 0 ? Math.max(0, Math.min(100, item.bytesReceived / item.totalBytes * 100)) : 0;
    const size = item.totalBytes > 0 ? prettySize(item.bytesReceived) + " / " + prettySize(item.totalBytes) : prettySize(item.bytesReceived);
    let buttons = "";
    if (item.state === "in_progress") {
      buttons += item.paused ? '<button data-action="resume" data-id="' + item.id + '">Resume</button>' : '<button data-action="pause" data-id="' + item.id + '">Pause</button>';
      buttons += '<button class="danger" data-action="cancel" data-id="' + item.id + '">Cancel</button>';
    }
    if (item.state === "complete") buttons += '<button data-action="open" data-id="' + item.id + '">Open</button><button data-action="show" data-id="' + item.id + '">Show in folder</button>';
    if (item.state !== "in_progress") buttons += '<button class="danger" data-action="remove" data-id="' + item.id + '">Remove</button>';
    return '<article class="download"><div class="file-name" title="' + escapeHtml(item.url || "") + '">' + escapeHtml(safeName(item)) + '</div><div class="file-meta"><span>' + escapeHtml(statusText(item)) + '</span><span>' + escapeHtml(size) + '</span></div><div class="progress"><span style="width:' + percent + '%"></span></div><div class="actions">' + buttons + '</div></article>';
  }).join("");
}
$("download-list").addEventListener("click", async event => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const id = Number(button.dataset.id), action = button.dataset.action;
  try {
    if (action === "pause") await api.downloads.pause(id);
    if (action === "resume") await api.downloads.resume(id);
    if (action === "cancel") await api.downloads.cancel(id);
    if (action === "open") await api.downloads.open(id);
    if (action === "show") await api.downloads.show(id);
    if (action === "remove") await api.downloads.erase({ id });
    await loadDownloads();
  } catch (error) { alert(error.message); }
});
$("download-form").addEventListener("submit", async event => {
  event.preventDefault();
  const url = $("download-url").value.trim();
  if (!url) return;
  try {
    await api.downloads.download({ url });
    $("download-url").value = "";
    await loadDownloads();
  } catch (error) { alert("Could not start download: " + error.message); }
});
$("filter").addEventListener("input", renderDownloads);
$("refresh").addEventListener("click", loadDownloads);

document.querySelectorAll(".tab").forEach(button => button.addEventListener("click", () => {
  document.querySelectorAll(".tab,.panel").forEach(el => el.classList.remove("active"));
  button.classList.add("active");
  $(button.dataset.tab).classList.add("active");
}));

async function loadTorSettings() {
  const saved = await api.storage.local.get({torHost:"127.0.0.1",torPort:9050,torEnabled:false});
  $("tor-host").value = saved.torHost;
  $("tor-port").value = saved.torPort;
  updateTorUi(saved.torEnabled);
}
function updateTorUi(enabled) {
  $("tor-dot").classList.toggle("on", enabled);
  $("tor-status").textContent = enabled ? "Tor proxy configured" : "Tor mode is off";
  $("tor-toggle").textContent = enabled ? "Disable Tor proxy" : "Enable Tor proxy";
}
$("tor-toggle").addEventListener("click", async () => {
  const enabled = $("tor-toggle").textContent.startsWith("Enable");
  try {
    await api.runtime.sendMessage({type:"SET_TOR",enabled});
    updateTorUi(enabled);
  } catch (error) {
    alert("Could not change Firefox proxy settings: " + error.message);
  }
});
$("save-endpoint").addEventListener("click", async () => {
  try {
    await api.runtime.sendMessage({type:"SAVE_TOR_ENDPOINT",host:$("tor-host").value,port:$("tor-port").value});
    alert("SOCKS5 address saved.");
  } catch (error) { alert(error.message); }
});

loadDownloads();
loadTorSettings();
