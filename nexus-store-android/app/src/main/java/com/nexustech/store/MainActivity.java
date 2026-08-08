package com.nexustech.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
    private static final String STORE_URL = "https://nexus-store-one-lake.vercel.app/";
    private WebView webView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        webView.addJavascriptInterface(new NexusAndroidBridge(), "NexusAndroid");

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (isApk(url, mimeType)) prepareInstall(url);
            else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        webView.loadUrl(STORE_URL);
    }

    public final class NexusAndroidBridge {
        @JavascriptInterface public long getInstalledVersionCode(String packageName) {
            if (packageName == null || packageName.trim().isEmpty()) return 0;
            try {
                if (packageName.equals(getPackageName())) {
                    PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
                }
                PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
            } catch (Exception ignored) {
                return 0;
            }
        }

        @JavascriptInterface public boolean openApp(String packageName) {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (intent == null) return false;
                startActivity(intent);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        @JavascriptInterface public void installApk(String url) {
            runOnUiThread(() -> prepareInstall(url));
        }
    }

    private boolean handleUrl(String url) {
        if (url == null) return false;
        if (isApk(url, null)) {
            prepareInstall(url);
            return true;
        }
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
            c.setConnectTimeout(20000);
            c.setReadTimeout(45000);
            c.setRequestProperty("Accept", "application/vnd.android.package-archive");
            int http = c.getResponseCode();
            if (http >= 400) throw new Exception("HTTP " + http);
            int len = c.getContentLength();

            PackageInstaller installer = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if (len > 0) params.setSize(len);
            int sessionId = installer.createSession(params);
            session = installer.openSession(sessionId);

            try (InputStream in = c.getInputStream(); OutputStream out = session.openWrite("app.apk", 0, len > 0 ? len : -1)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                session.fsync(out);
            }

            Intent result = new Intent(this, InstallResultReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(this, sessionId, result, flags);
            session.commit(pending.getIntentSender());
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Installation impossible" : e.getMessage();
            runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
            if (session != null) try { session.abandon(); } catch (Exception ignored) {}
        } finally {
            if (session != null) session.close();
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
