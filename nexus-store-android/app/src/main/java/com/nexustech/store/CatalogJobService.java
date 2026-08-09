package com.nexustech.store;

import android.Manifest;
import android.app.JobInfo;
import android.app.JobParameters;
import android.app.JobScheduler;
import android.app.JobService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class CatalogJobService extends JobService {
    private static final int JOB_ID = 56221;
    private static final String CATALOG = "https://tresor562.github.io/nexus-store/catalog.json";
    private static final String STORE = "https://tresor562.github.io/nexus-store/";
    private static final String CHANNEL = "nexus_store_news";

    public static void schedule(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        for (JobInfo job : scheduler.getAllPendingJobs()) if (job.getId() == JOB_ID) return;
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(context, CatalogJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(15 * 60 * 1000L)
                .build();
        scheduler.schedule(info);
    }

    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try { checkCatalog(); } catch (Exception ignored) {}
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }

    private void checkCatalog() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(CATALOG).openConnection();
        connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setRequestProperty("Accept", "application/json");
        if (connection.getResponseCode() >= 400) return;
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;while ((line = reader.readLine()) != null) json.append(line);
        }
        JSONArray rows = new JSONArray(json.toString());
        SharedPreferences prefs = getSharedPreferences("nexus_catalog", MODE_PRIVATE);
        Set<String> seen = new HashSet<>(prefs.getStringSet("seen", new HashSet<>()));
        Set<String> current = new HashSet<>();
        boolean firstRun = seen.isEmpty();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);if (row == null) continue;
            String id = row.optString("id", "");String name = row.optString("name", "Application Nexus");if (id.isEmpty()) continue;
            current.add(id);if (!firstRun && !seen.contains(id)) notifyNewApp(id, name);
        }
        prefs.edit().putStringSet("seen", current).apply();
    }

    private void notifyNewApp(String id, String name) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);if (nm == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Nouveautés Nexus Store", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class).setData(Uri.parse(STORE + "app.html?id=" + Uri.encode(id))).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending = PendingIntent.getActivity(this, Math.abs(id.hashCode()), intent, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_nexus_store).setContentTitle("Nouveau sur Nexus Store").setContentText(name + " est disponible.").setContentIntent(pending).setAutoCancel(true);
        nm.notify(Math.abs(id.hashCode()), builder.build());
    }
}
