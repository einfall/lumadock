package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.widget.*;
/** Branded info and remote-friendly update actions; asynchronous status never rebuilds the list. */
final class AppInfoPopup {
 static void show(Activity a){SettingsSheet p=new SettingsSheet(a,"Info · Luma Dock");SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);AppUpdates updates=AppUpdates.get(a);LinearLayout header=new LinearLayout(a);header.setGravity(android.view.Gravity.CENTER_VERTICAL);ImageView logo=new ImageView(a);logo.setImageResource(R.mipmap.lumadock_icon);header.addView(logo,new LinearLayout.LayoutParams(SettingsSheet.dp(a,62),SettingsSheet.dp(a,62)));LinearLayout brand=new LinearLayout(a);brand.setOrientation(LinearLayout.VERTICAL);TextView name=GlassDialog.text(a,"Luma Dock",24);brand.addView(name);String version="";try{android.content.pm.PackageInfo i=AppUpdateClient.installed(a);version=i.versionName+" · Build "+AppUpdateClient.version(i);}catch(Exception ignored){}TextView sub=GlassDialog.text(a,version+"\nAndroid TV / Google TV",11);brand.addView(sub);header.addView(brand);p.add(header);
 TextView status=GlassDialog.text(a,updates.state().message,12);p.add(status);ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);p.add(progress);
 p.toggle("Automatisch nach Updates suchen","Einmal täglich beim Öffnen von Home",prefs.getBoolean(AppUpdates.AUTO,true),on->prefs.edit().putBoolean(AppUpdates.AUTO,on).apply());
 p.toggle("Neue Updates automatisch herunterladen","Installation anschließend in Android bestätigen",prefs.getBoolean(AppUpdates.DOWNLOAD,false),on->prefs.edit().putBoolean(AppUpdates.DOWNLOAD,on).apply());
 p.row("refresh","Nach Updates suchen","Stabiles Release von GitHub prüfen",()->updates.check(false));
 LinearLayout download=p.row("video","APK herunterladen","Auch zum erneuten Installieren der aktuellen Version",updates::download);
 p.row("grid","Installieren","Geprüfte APK mit Android-Bestätigung installieren",()->updates.install(a));
 p.row("settings","Installation erlauben","Android-Einstellung für Luma Dock",()->AppUpdates.allow(a));
 TextView notes=GlassDialog.text(a,"",11);p.add(notes);
 p.row("tv","GitHub-Projekt","github.com/einfall/lumadock",()->web(a,UpdatePolicy.REPO));
 p.row("text","Lizenzen & Herkunft","Open-Source-Basis und Asset-Lizenzen",()->{SettingsSheet credits=new SettingsSheet(a,"Lizenzen & Herkunft");credits.info("Luma Dock basiert auf CleanYourTV (MIT). Eigene Anpassungen: einfall. Native Java-Oberfläche mit AndroidX RecyclerView und DynamicAnimation. Schriften: SIL OFL; Material Icons: Apache 2.0. Aura-TV-Szenen: MIT; Bilder und Daten besitzen eigene Herkunftshinweise. Alle Lizenztexte und Quellen liegen im Repository.");credits.row("tv","Lizenzdateien auf GitHub",null,()->web(a,UpdatePolicy.REPO+"/blob/main/THIRD_PARTY_NOTICES.md"));credits.show();});
 java.util.function.Consumer<AppUpdates.State> listener=s->{if(a.isDestroyed())return;status.setText(s.message);progress.setVisibility(s.busy()?android.view.View.VISIBLE:android.view.View.GONE);progress.setIndeterminate("checking".equals(s.stage)||"verifying".equals(s.stage));progress.setProgress(s.percent);notes.setText(s.release==null?"":s.release.notes);download.setAlpha(s.busy()?.5f:1f);download.setContentDescription(("ready".equals(s.stage)?"APK erneut herunterladen":"APK herunterladen")+" · "+s.message);};updates.observe(listener);p.dialog.setOnDismissListener(d->updates.forget(listener));p.info("Updates werden über HTTPS geladen. Prüfsumme, Paket und Signatur müssen zu dieser Installation passen. Android bestätigt jede Installation; Einstellungen bleiben erhalten.");p.show();}
 private static void web(Activity a,String url){try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(a,url,Toast.LENGTH_LONG).show();}}
}
