package com.mahim11184.niobium;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://duckduckgo.com/";
    private static final String PREFS = "niobium_settings";
    private static GeckoRuntime runtime;

    private GeckoView geckoView;
    private GeckoSession session;
    private EditText addressBar;
    private TextView engineStatus;
    private TextView torButton;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 18, 27));
        getWindow().setNavigationBarColor(Color.rgb(11, 18, 27));
        buildInterface();
        initializeGecko();
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 18, 27));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(9), dp(16), dp(9));
        header.setBackgroundColor(Color.rgb(15, 25, 37));

        TextView brand = new TextView(this);
        brand.setText("Niobium");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(20);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(34), 1));

        TextView engine = new TextView(this);
        engine.setText("GECKO ENGINE");
        engine.setTextColor(Color.rgb(81, 214, 208));
        engine.setTextSize(10);
        engine.setLetterSpacing(0.12f);
        header.addView(engine);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(54)));

        LinearLayout addressRow = new LinearLayout(this);
        addressRow.setGravity(Gravity.CENTER_VERTICAL);
        addressRow.setPadding(dp(12), dp(8), dp(12), dp(5));

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setTextColor(Color.WHITE);
        addressBar.setHintTextColor(Color.rgb(145, 162, 181));
        addressBar.setHint("Search or enter a URL / .onion address");
        addressBar.setPadding(dp(12), 0, dp(12), 0);
        addressBar.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setBackground(roundRect(Color.rgb(23, 32, 44), Color.rgb(48, 67, 86), 10));
        addressRow.addView(addressBar, new LinearLayout.LayoutParams(0, dp(44), 1));

        TextView go = makeButton("GO", true);
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(58), dp(44));
        goParams.leftMargin = dp(8);
        addressRow.addView(go, goParams);
        go.setOnClickListener(v -> navigate(addressBar.getText().toString()));

        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                navigate(addressBar.getText().toString());
                return true;
            }
            return false;
        });
        root.addView(addressRow);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(10), dp(2), dp(10), dp(8));

        TextView back = makeButton("‹", false);
        TextView forward = makeButton("›", false);
        TextView home = makeButton("⌂", false);
        TextView downloads = makeButton("Downloads", false);
        torButton = makeButton("Tor: OFF", false);

        addControl(controls, back, 0.7f);
        addControl(controls, forward, 0.7f);
        addControl(controls, home, 0.7f);
        addControl(controls, downloads, 1.35f);
        addControl(controls, torButton, 1.0f);

        back.setOnClickListener(v -> { if (session != null) session.goBack(); });
        forward.setOnClickListener(v -> { if (session != null) session.goForward(); });
        home.setOnClickListener(v -> navigate(HOME_URL));
        downloads.setOnClickListener(v -> showDownloads());
        torButton.setOnClickListener(v -> toggleTor());
        root.addView(controls);

        engineStatus = new TextView(this);
        engineStatus.setTextColor(Color.rgb(145, 162, 181));
        engineStatus.setTextSize(10);
        engineStatus.setPadding(dp(15), 0, dp(15), dp(5));
        root.addView(engineStatus);

        geckoView = new GeckoView(this);
        root.addView(geckoView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        updateTorLabel();
    }

    private void initializeGecko() {
        try {
            if (runtime == null) {
                File config = writeGeckoConfig();
                GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                        .configFilePath(config.getAbsolutePath())
                        .build();
                runtime = GeckoRuntime.create(this, settings);
            }

            session = new GeckoSession();
            session.setContentDelegate(new GeckoSession.ContentDelegate() {
                @Override
                public void onExternalResponse(GeckoSession currentSession, WebResponse response) {
                    saveResponse(response);
                }

                @Override
                public void onTitleChange(GeckoSession currentSession, String title) {
                    if (title != null && !title.trim().isEmpty()) {
                        runOnUiThread(() -> engineStatus.setText(title));
                    }
                }
            });
            session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
                @Override
                public void onPageStart(GeckoSession currentSession, String url) {
                    runOnUiThread(() -> addressBar.setText(url));
                }

                @Override
                public void onPageStop(GeckoSession currentSession, boolean success) {
                    runOnUiThread(() -> {
                        if (!success) engineStatus.setText("Page load failed — check your connection or Tor service.");
                    });
                }
            });
            session.open(runtime);
            geckoView.setSession(session);
            session.loadUri(HOME_URL);
        } catch (Exception error) {
            engineStatus.setText("GeckoView could not start: " + error.getMessage());
            Toast.makeText(this, "Browser engine startup failed", Toast.LENGTH_LONG).show();
        }
    }

    private File writeGeckoConfig() throws Exception {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean torEnabled = prefs.getBoolean("tor_enabled", false);
        String host = prefs.getString("tor_host", "127.0.0.1");
        int port = prefs.getInt("tor_port", 9050);

        String config;
        if (torEnabled) {
            config = "prefs:\n"
                    + "  network.proxy.type: 1\n"
                    + "  network.proxy.socks: \"" + host.replace("\"", "") + "\"\n"
                    + "  network.proxy.socks_port: " + port + "\n"
                    + "  network.proxy.socks_version: 5\n"
                    + "  network.proxy.socks_remote_dns: true\n"
                    + "  network.proxy.no_proxies_on: \"localhost, 127.0.0.1\"\n";
        } else {
            config = "prefs:\n  network.proxy.type: 0\n";
        }

        File file = new File(getFilesDir(), "gecko-config.yaml");
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(config.getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
        return file;
    }

    private void navigate(String raw) {
        if (session == null) return;
        String input = raw == null ? "" : raw.trim();
        if (input.isEmpty()) return;

        String target;
        if (input.startsWith("about:") || input.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            target = input;
        } else if (input.contains(" ") || (!input.contains(".") && !input.endsWith(".onion"))) {
            target = "https://duckduckgo.com/?q=" + Uri.encode(input);
        } else {
            target = "https://" + input;
        }
        addressBar.setText(target);
        session.loadUri(target);
    }

    private void saveResponse(WebResponse response) {
        final InputStream input = response.body;
        if (input == null) {
            runOnUiThread(() -> Toast.makeText(this, "This download did not provide a readable response stream.", Toast.LENGTH_LONG).show());
            return;
        }

        final String sourceUrl = response.uri;
        final Map<String, String> headers = response.headers;
        final String mime = firstHeader(headers, "content-type", "application/octet-stream");
        final String disposition = firstHeader(headers, "content-disposition", "");
        final String fileName = safeFileName(extractFileName(disposition, sourceUrl));

        runOnUiThread(() -> Toast.makeText(this, "Downloading " + fileName, Toast.LENGTH_SHORT).show());

        downloadExecutor.execute(() -> {
            Uri savedUri = null;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Niobium");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                savedUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (savedUri == null) throw new IllegalStateException("Android could not create the download file.");

                try (InputStream in = input;
                     OutputStream out = getContentResolver().openOutputStream(savedUri, "w")) {
                    if (out == null) throw new IllegalStateException("Could not open the download destination.");
                    byte[] buffer = new byte[64 * 1024];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    out.flush();
                }

                ContentValues complete = new ContentValues();
                complete.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(savedUri, complete, null, null);
                runOnUiThread(() -> Toast.makeText(this, "Saved to Downloads/Niobium", Toast.LENGTH_LONG).show());
            } catch (Exception error) {
                try { input.close(); } catch (Exception ignored) {}
                if (savedUri != null) getContentResolver().delete(savedUri, null, null);
                runOnUiThread(() -> Toast.makeText(this, "Download failed: " + error.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String firstHeader(Map<String, String> headers, String key, String fallback) {
        if (headers == null) return fallback;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
        }
        return fallback;
    }

    private String extractFileName(String disposition, String sourceUrl) {
        if (disposition != null && !disposition.isEmpty()) {
            String lower = disposition.toLowerCase(Locale.ROOT);
            int index = lower.indexOf("filename=");
            if (index >= 0) {
                String value = disposition.substring(index + 9).trim();
                if (value.startsWith("\"")) value = value.substring(1);
                int end = value.indexOf('"');
                if (end >= 0) value = value.substring(0, end);
                return value;
            }
            int encoded = lower.indexOf("filename*=");
            if (encoded >= 0) {
                String value = disposition.substring(encoded + 10).trim();
                int marker = value.indexOf("''");
                if (marker >= 0) value = value.substring(marker + 2);
                try { return URLDecoder.decode(value, "UTF-8"); } catch (Exception ignored) {}
            }
        }
        String path = Uri.parse(sourceUrl).getLastPathSegment();
        return (path == null || path.trim().isEmpty()) ? "niobium-download" : path;
    }

    private String safeFileName(String name) {
        String safe = name == null ? "niobium-download" : name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.isEmpty() || safe.equals(".") || safe.equals("..")) return "niobium-download";
        return safe;
    }

    private void showDownloads() {
        List<Uri> uris = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String[] projection = {
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE
        };
        String selection = MediaStore.Downloads.RELATIVE_PATH + "=?";
        String[] selectionArgs = {Environment.DIRECTORY_DOWNLOADS + "/Niobium/"};

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Downloads.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    Uri itemUri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, Long.toString(id));
                    uris.add(itemUri);
                    labels.add(name + "  ·  " + formatSize(size));
                }
            }
        } catch (Exception error) {
            Toast.makeText(this, "Could not read downloads: " + error.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        if (labels.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Niobium downloads")
                    .setMessage("No downloaded files yet. Files are saved in Downloads/Niobium.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] items = labels.toArray(new String[0]);
        Uri[] itemUris = uris.toArray(new Uri[0]);
        new AlertDialog.Builder(this)
                .setTitle("Niobium downloads")
                .setItems(items, (dialog, which) -> {
                    Intent open = new Intent(Intent.ACTION_VIEW);
                    open.setDataAndType(itemUris[which], "*/*");
                    open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { startActivity(open); }
                    catch (Exception error) { Toast.makeText(this, "No app can open this file.", Toast.LENGTH_SHORT).show(); }
                })
                .setPositiveButton("Close", null)
                .show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void toggleTor() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean enabled = !prefs.getBoolean("tor_enabled", false);
        prefs.edit().putBoolean("tor_enabled", enabled).apply();
        updateTorLabel();

        new AlertDialog.Builder(this)
                .setTitle(enabled ? "Tor proxy prepared" : "Tor proxy disabled")
                .setMessage(enabled
                        ? "Niobium will use SOCKS5 at 127.0.0.1:9050 after the next full app restart. Start a separate Tor service such as Orbot first. This setting does not verify a Tor connection or guarantee anonymity."
                        : "The Tor proxy preference is disabled for the next full app launch.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateTorLabel() {
        boolean enabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("tor_enabled", false);
        if (torButton != null) torButton.setText(enabled ? "Tor: ON*" : "Tor: OFF");
        if (engineStatus != null) {
            engineStatus.setText(enabled
                    ? "Tor settings saved • restart required • local Tor service needed"
                    : "Browsing with GeckoView • Downloads save to Downloads/Niobium");
        }
    }

    private TextView makeButton(String text, boolean primary) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setTextColor(Color.WHITE);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundRect(primary ? Color.rgb(35, 125, 151) : Color.rgb(27, 40, 55),
                primary ? Color.rgb(65, 157, 181) : Color.rgb(48, 67, 86), 9));
        return button;
    }

    private void addControl(LinearLayout row, TextView button, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), weight);
        params.leftMargin = dp(3);
        params.rightMargin = dp(3);
        row.addView(button, params);
    }

    private GradientDrawable roundRect(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (session != null) session.setActive(true);
    }

    @Override
    protected void onStop() {
        if (session != null) session.setActive(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (session != null) session.close();
        downloadExecutor.shutdown();
        super.onDestroy();
    }
}
