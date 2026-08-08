package com.nexustech.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String CATALOG = "https://tresor562.github.io/nexus-store/catalog.json";
    private static final int BLUE = Color.rgb(11,87,208);
    private static final int INK = Color.rgb(32,33,36);
    private static final int MUTED = Color.rgb(95,99,104);
    private static final int SURFACE = Color.rgb(247,248,250);
    private LinearLayout list;
    private EditText search;
    private ProgressBar progress;
    private final List<AppItem> all = new ArrayList<>();

    static class AppItem {
        String id,name,category,description,version,androidUrl,webUrl;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildUi());
        loadCatalog();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(18), dp(18), dp(32));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("N", 20, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackgroundColor(BLUE);
        top.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView title = text("  Nexus Store", 23, INK, true);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(top);

        TextView subtitle = text("Applications, jeux et outils Nexus", 14, MUTED, false);
        subtitle.setPadding(0, dp(10), 0, dp(16));
        root.addView(subtitle);

        search = new EditText(this);
        search.setHint("Rechercher des applications et des jeux");
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setPadding(dp(18),0,dp(18),0);
        search.setBackgroundColor(Color.rgb(238,243,248));
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(54)));
        search.setOnEditorActionListener((v,action,event)->{render(search.getText().toString());return false;});

        TextView hero = text("Sélection Nexus\nNexus Ban\n\nInstallation Android via l’installateur système, sans passer par le dossier Téléchargements.", 18, INK, true);
        hero.setPadding(dp(22),dp(24),dp(22),dp(24));
        hero.setBackgroundColor(Color.rgb(232,240,254));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1,-2);
        hp.setMargins(0,dp(18),0,dp(22));
        root.addView(hero,hp);

        root.addView(text("Recommandé pour vous", 22, INK, true));
        progress = new ProgressBar(this);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(36),dp(36));
        pp.gravity=Gravity.CENTER_HORIZONTAL; pp.setMargins(0,dp(22),0,dp(10));
        root.addView(progress,pp);
        list = column();
        root.addView(list);

        TextView note = text("Nexus Store ne contourne pas Android : la première installation depuis Nexus Store peut demander d’autoriser cette source, et Android peut afficher une confirmation avant l’installation.", 12, MUTED, false);
        note.setPadding(0,dp(24),0,0);
        root.addView(note);
        return scroll;
    }

    private void loadCatalog() {
        new Thread(() -> {
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(CATALOG).openConnection();
                c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setRequestProperty("Accept","application/json");
                if(c.getResponseCode()>=400) throw new Exception("HTTP "+c.getResponseCode());
                JSONArray arr=new JSONArray(read(c.getInputStream()));
                all.clear();
                for(int i=0;i<arr.length();i++){
                    JSONObject o=arr.optJSONObject(i); if(o==null) continue;
                    AppItem a=new AppItem();
                    a.id=o.optString("id"); a.name=o.optString("name"); a.category=o.optString("category"); a.description=o.optString("description"); a.version=o.optString("version");
                    a.androidUrl=o.isNull("android")?null:o.optString("android",null);
                    a.webUrl=o.isNull("web")?null:o.optString("web",null);
                    if(a.androidUrl!=null && a.androidUrl.startsWith("./")) a.androidUrl="https://tresor562.github.io/nexus-store/"+a.androidUrl.substring(2);
                    if(a.webUrl!=null && a.webUrl.startsWith("./")) a.webUrl="https://tresor562.github.io/nexus-store/"+a.webUrl.substring(2);
                    all.add(a);
                }
                runOnUiThread(()->{progress.setVisibility(View.GONE);render("");});
            } catch(Exception e) {
                runOnUiThread(()->{progress.setVisibility(View.GONE);list.removeAllViews();list.addView(text("Catalogue momentanément indisponible.",14,MUTED,false));});
            }
        }).start();
    }

    private void render(String query) {
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        list.removeAllViews();
        for(AppItem a:all){
            String hay=(a.name+" "+a.category+" "+a.description).toLowerCase(Locale.ROOT);
            if(!q.isEmpty()&&!hay.contains(q)) continue;
            LinearLayout card=card(a);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
            cp.setMargins(0,dp(10),0,0);
            list.addView(card,cp);
        }
        if(list.getChildCount()==0) list.addView(text("Aucun résultat.",14,MUTED,false));
    }

    private LinearLayout card(AppItem a) {
        LinearLayout card=column();
        card.setPadding(dp(16),dp(16),dp(16),dp(16));
        card.setBackgroundColor(SURFACE);
        card.addView(text(a.name,18,INK,true));
        card.addView(text(a.category+" · "+a.version,12,MUTED,false));
        TextView desc=text(a.description,13,MUTED,false); desc.setPadding(0,dp(7),0,0); card.addView(desc);
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.END); actions.setPadding(0,dp(8),0,0);
        Button b=new Button(this);
        if(a.androidUrl!=null&&!a.androidUrl.isEmpty()) { b.setText("INSTALLER"); b.setOnClickListener(v->prepareInstall(a)); }
        else if(a.webUrl!=null&&!a.webUrl.isEmpty()) { b.setText("OUVRIR"); b.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(a.webUrl)))); }
        else { b.setText("BIENTÔT"); b.setEnabled(false); }
        actions.addView(b); card.addView(actions);
        return card;
    }

    private void prepareInstall(AppItem app) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                    .setTitle("Autoriser Nexus Store")
                    .setMessage("Android doit autoriser Nexus Store à demander l’installation d’applications. Active « Autoriser depuis cette source », puis reviens et touche Installer à nouveau.")
                    .setNegativeButton("Annuler",null)
                    .setPositiveButton("Ouvrir les réglages",(d,w)->startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:"+getPackageName()))))
                    .show();
            return;
        }
        Toast.makeText(this,"Préparation de "+app.name+"…",Toast.LENGTH_SHORT).show();
        new Thread(()->installFromUrl(app)).start();
    }

    private void installFromUrl(AppItem app) {
        PackageInstaller.Session session=null;
        try {
            HttpURLConnection c=(HttpURLConnection)new URL(app.androidUrl).openConnection();
            c.setConnectTimeout(20000); c.setReadTimeout(30000); c.setRequestProperty("Accept","application/vnd.android.package-archive");
            int http=c.getResponseCode(); if(http>=400) throw new Exception("HTTP "+http);
            int len=c.getContentLength();
            PackageInstaller installer=getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if(len>0) params.setSize(len);
            int sessionId=installer.createSession(params);
            session=installer.openSession(sessionId);
            try(InputStream in=c.getInputStream(); OutputStream out=session.openWrite(app.name+".apk",0,len>0?len:-1)){
                byte[] buf=new byte[64*1024]; int n;
                while((n=in.read(buf))!=-1) out.write(buf,0,n);
                session.fsync(out);
            }
            Intent result=new Intent(this,InstallResultReceiver.class);
            int flags=PendingIntent.FLAG_UPDATE_CURRENT;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S) flags|=PendingIntent.FLAG_MUTABLE;
            PendingIntent pending=PendingIntent.getBroadcast(this,sessionId,result,flags);
            session.commit(pending.getIntentSender());
        } catch(Exception e) {
            runOnUiThread(()->Toast.makeText(this,"Échec de préparation : "+e.getMessage(),Toast.LENGTH_LONG).show());
            if(session!=null) try{session.abandon();}catch(Exception ignored){}
        } finally { if(session!=null) session.close(); }
    }

    private String read(InputStream in)throws Exception { BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);return sb.toString(); }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.12f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
