package com.nexustech.store;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String STORE_URL = "https://tresor562.github.io/nexus-store/";
    private static final String CHANNEL_ID = "nexus_store_news";
    private WebView webView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.parseColor("#070B14"));
        getWindow().setNavigationBarColor(Color.parseColor("#070B14"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        CatalogJobService.schedule(this);

        webView = new WebView(this);
        if (Build.VERSION.SDK_INT >= 35) {
            webView.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.addJavascriptInterface(new NexusAndroidBridge(), "NexusAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return handleUrl(request.getUrl().toString()); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return handleUrl(url); }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (isApk(url, mimeType)) prepareInstall(url);
            else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        String startUrl = getIntent() != null && getIntent().getDataString() != null ? getIntent().getDataString() : STORE_URL;
        webView.loadUrl(startUrl.startsWith(STORE_URL) ? startUrl : STORE_URL);
    }

    public final class NexusAndroidBridge {
        @JavascriptInterface public long getInstalledVersionCode(String packageName) {
            if (packageName == null || packageName.trim().isEmpty()) return 0;
            try {
                PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
            } catch (Exception ignored) { return 0; }
        }

        @JavascriptInterface public boolean openApp(String packageName) {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent == null) return false;
                startActivity(intent);
                return true;
            } catch (Exception ignored) { return false; }
        }

        @JavascriptInterface public void installApk(String url) { runOnUiThread(() -> prepareInstall(url)); }

        @JavascriptInterface public boolean notificationsEnabled() {
            if (Build.VERSION.SDK_INT >= 33) return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            return true;
        }

        @JavascriptInterface public void requestNotifications() { runOnUiThread(() -> requestNotificationPermissionIfNeeded()); }

        @JavascriptInterface public void notifyNewApp(String appId, String appName) {
            runOnUiThread(() -> showNewAppNotification(appId, appName));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Nouveautés Nexus Store", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Nouvelles applications et mises à jour");
        nm.createNotificationChannel(channel);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
        }
    }

    private void showNewAppNotification(String appId, String appName) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Uri target = Uri.parse(STORE_URL + "app.html?id=" + Uri.encode(appId == null ? "" : appId));
        Intent intent = new Intent(this, MainActivity.class).setData(target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending = PendingIntent.getActivity(this, Math.abs((appId == null ? appName : appId).hashCode()), intent, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_nexus_store)
                .setContentTitle("Nouveau sur Nexus Store")
                .setContentText((appName == null ? "Une nouvelle application" : appName) + " est disponible.")
                .setAutoCancel(true)
                .setContentIntent(pending);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(Math.abs((appId == null ? String.valueOf(System.currentTimeMillis()) : appId).hashCode()), builder.build());
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        if (isApk(url, null)) { prepareInstall(url); return true; }
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        if (host != null && (host.endsWith("vercel.app") || host.equals("tresor562.github.io"))) return false;
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
        return true;
    }

    private boolean isApk(String url, String mimeType) {
        return (url != null && url.toLowerCase().contains(".apk")) || "application/vnd.android.package-archive".equals(mimeType);
    }

    private void prepareInstall(String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                    .setTitle("Autoriser Nexus Store")
                    .setMessage("Autorise les installations depuis Nexus Store.")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Réglages", (d,w) -> startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()))))
                    .show();
            return;
        }
        Toast.makeText(this, "Préparation…", Toast.LENGTH_SHORT).show();
        new Thread(() -> installFromUrl(url)).start();
    }

    private void installFromUrl(String url) {
        PackageInstaller.Session session = null;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(20000);c.setReadTimeout(45000);c.setRequestProperty("Accept", "application/vnd.android.package-archive");
            int http = c.getResponseCode();if (http >= 400) throw new Exception("HTTP " + http);int len = c.getContentLength();
            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if (len > 0) params.setSize(len);
            int sessionId = installer.createSession(params);session = installer.openSession(sessionId);
            try (InputStream in = c.getInputStream(); OutputStream out = session.openWrite("app.apk", 0, len > 0 ? len : -1)) {
                byte[] buf = new byte[64 * 1024];int n;while ((n = in.read(buf)) != -1) out.write(buf, 0, n);session.fsync(out);
            }
            Intent result = new Intent(this, InstallResultReceiver.class);int flags = PendingIntent.FLAG_UPDATE_CURRENT;if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(this, sessionId, result, flags);session.commit(pending.getIntentSender());
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Installation impossible" : e.getMessage();runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());if (session != null) try { session.abandon(); } catch (Exception ignored) {}
        } finally { if (session != null) session.close(); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.evaluateJavascript("window.NexusStoreRefresh&&window.NexusStoreRefresh()", null);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);
        if (webView != null && intent != null && intent.getDataString() != null && intent.getDataString().startsWith(STORE_URL)) webView.loadUrl(intent.getDataString());
    }

    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
