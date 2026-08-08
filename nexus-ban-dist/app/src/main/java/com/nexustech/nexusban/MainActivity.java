package com.nexustech.nexusban;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int BG=Color.rgb(7,9,13), CARD=Color.rgb(17,22,29), TEXT=Color.rgb(236,243,247), MUTED=Color.rgb(157,174,184), CYAN=Color.rgb(34,211,238), GREEN=Color.rgb(74,222,128), ORANGE=Color.rgb(251,191,36), RED=Color.rgb(248,113,113);
  private EditText phone;
  private TextView result, details;

  @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(ui());}

  private View ui(){
    ScrollView s=new ScrollView(this);s.setBackgroundColor(BG);LinearLayout root=col();root.setPadding(dp(20),dp(24),dp(20),dp(36));s.addView(root);
    ImageView icon=new ImageView(this);icon.setImageResource(com.nexustech.nexusban.R.drawable.nexus_ban_icon);LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(88),dp(88));ip.gravity=Gravity.CENTER_HORIZONTAL;root.addView(icon,ip);
    TextView h=text("NEXUS BAN",30,TEXT,true);h.setGravity(Gravity.CENTER);root.addView(h);
    TextView by=text("créé par 𝐌ꝛ⥔𝕿𝖗𝖊𝖘𝖔𝖗 🌹",13,MUTED,false);by.setGravity(Gravity.CENTER);root.addView(by);
    TextView intro=text("Vérification via les données WhatsApp Business officiellement accessibles au compte Meta configuré. Une erreur réseau n’est jamais présentée comme un bannissement.",14,MUTED,false);intro.setPadding(0,dp(18),0,dp(14));root.addView(intro);
    phone=input("+229XXXXXXXX",InputType.TYPE_CLASS_PHONE);root.addView(phone);
    Button verify=button("VÉRIFIER LE STATUT");verify.setOnClickListener(v->setup());root.addView(verify);
    LinearLayout card=card();result=text("INDÉTERMINÉ",23,TEXT,true);details=text("Aucune vérification effectuée.",14,MUTED,false);card.addView(result);details.setPadding(0,dp(8),0,0);card.addView(details);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(18),0,0);root.addView(card,cp);
    TextView note=text("Nexus Ban ne prétend pas déterminer le statut de n’importe quel compte WhatsApp personnel. Meta ne fournit pas d’API publique universelle pour cela.",12,MUTED,false);note.setPadding(0,dp(18),0,0);root.addView(note);
    return s;
  }

  private void setup(){
    String p=phone.getText().toString().trim();if(!p.matches("^\\+[1-9][0-9]{7,14}$")){show("FORMAT INVALIDE","Utilise le format international E.164.",ORANGE);return;}
    LinearLayout box=col();box.setPadding(dp(18),dp(8),dp(18),0);EditText w=input("WABA ID",InputType.TYPE_CLASS_TEXT);EditText t=input("Jeton Meta",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);EditText g=input("Version Graph API (ex. v26.0)",InputType.TYPE_CLASS_TEXT);g.setText("v26.0");box.addView(w);box.addView(t);box.addView(g);
    AlertDialog d=new AlertDialog.Builder(this).setTitle("Source Meta officielle").setView(box).setNegativeButton("Annuler",null).setPositiveButton("Vérifier",null).create();d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String ws=w.getText().toString().trim(),ts=t.getText().toString().trim(),gs=g.getText().toString().trim();if(ws.isEmpty()||ts.isEmpty()||gs.isEmpty()){Toast.makeText(this,"WABA ID, jeton et version requis.",Toast.LENGTH_LONG).show();return;}d.dismiss();runCheck(p,ws,ts,gs);}));d.show();
  }

  private void runCheck(String raw,String waba,String token,String graph){show("VÉRIFICATION…","Connexion HTTPS à Meta…",CYAN);new Thread(()->{String title="INDÉTERMINÉ",body;int color=MUTED;HttpURLConnection c=null;try{String fields="id,display_phone_number,verified_name,quality_rating,status";URL u=new URL("https://graph.facebook.com/"+graph+"/"+waba+"/phone_numbers?fields="+fields+"&limit=100");c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");int http=c.getResponseCode();String payload=read(http>=400?c.getErrorStream():c.getInputStream());if(http==401||http==403){title="ACCÈS REFUSÉ";body="Meta a refusé l’autorisation (HTTP "+http+").";color=ORANGE;}else if(http>=400){title="INDÉTERMINÉ";body="Meta a retourné HTTP "+http+". Ce résultat n’est pas interprété comme un bannissement.";color=ORANGE;}else{JSONObject root=new JSONObject(payload);JSONArray data=root.optJSONArray("data");String n=raw.replaceAll("\\D","");JSONObject match=null;if(data!=null)for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o!=null&&o.optString("display_phone_number","").replaceAll("\\D","").equals(n)){match=o;break;}}if(match==null){title="INDÉTERMINÉ";body="Le numéro n’apparaît pas dans les numéros accessibles de ce WABA. Cela ne prouve pas qu’il est banni.";color=ORANGE;}else{String st=match.optString("status","UNKNOWN").toUpperCase(Locale.ROOT),q=match.optString("quality_rating","UNKNOWN").toUpperCase(Locale.ROOT);if("UNREGISTERED".equals(st)){title="NON ENREGISTRÉ";color=RED;}else if("CONNECTED".equals(st)&&!"RED".equals(q)){title="ACTIF / CONNECTÉ";color=GREEN;}else if("RED".equals(q)){title="ACTIF MAIS À RISQUE";color=ORANGE;}else{title="ENREGISTRÉ / ACCESSIBLE";color=GREEN;}body="Statut Meta : "+st+"\nQuality rating : "+q+"\nNom vérifié : "+match.optString("verified_name","—")+"\nPhone Number ID : "+match.optString("id","—");}}}catch(Exception e){title="INDÉTERMINÉ";body="Erreur réseau ou de traitement : "+e.getClass().getSimpleName()+".";color=ORANGE;}finally{if(c!=null)c.disconnect();}final String ft=title,fb=body;final int fc=color;runOnUiThread(()->show(ft,fb,fc));}).start();}

  private String read(InputStream in)throws Exception{if(in==null)return"";BufferedReader b=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String l;while((l=b.readLine())!=null)s.append(l);return s.toString();}
  private void show(String a,String b,int c){result.setText(a);result.setTextColor(c);details.setText(b);}
  private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
  private LinearLayout card(){LinearLayout l=col();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackgroundColor(CARD);return l;}
  private EditText input(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setInputType(type);e.setSingleLine(true);e.setPadding(dp(14),dp(6),dp(14),dp(6));return e;}
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.BLACK);b.setBackgroundColor(CYAN);return b;}
  private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.15f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
