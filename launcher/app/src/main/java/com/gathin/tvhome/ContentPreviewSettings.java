package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
/** Live per-app capability detection, with optional channel visibility and an explicit read permission. */
final class ContentPreviewSettings {
 static void show(Activity a){new ContentPreviewSettings(a);}
 private final Activity a;private final SettingsSheet page;private final SharedPreferences prefs;private final ExecutorService worker=Executors.newSingleThreadExecutor();private boolean closed,busy;
 private ContentPreviewSettings(Activity a){this.a=a;prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);page=new SettingsSheet(a,"Content-Vorschau");page.dialog.setOnDismissListener(d->{closed=true;worker.shutdownNow();});render(null);page.show();load();}
 private void load(){if(busy)return;busy=true;worker.execute(()->{ContentPreviewData.Snapshot data=ContentPreviewData.load(a);a.runOnUiThread(()->{busy=false;if(!closed)render(data);});});}
 private void render(ContentPreviewData.Snapshot data){page.clearRows();page.toggle("Content-Vorschau anzeigen","Inhalte der fokussierten App oberhalb des App-Namens",prefs.getBoolean(ContentPreviewData.ENABLED,false),on->prefs.edit().putBoolean(ContentPreviewData.ENABLED,on).apply());page.row("eye","Zugriff auf Vorschaukanäle erlauben","Liest ausschließlich vom TV freigegebene Programmlisten",()->{if(a.checkSelfPermission(ContentPreviewData.PERMISSION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)a.requestPermissions(new String[]{ContentPreviewData.PERMISSION},7091);else Toast.makeText(a,"Zugriff bereits erlaubt",Toast.LENGTH_SHORT).show();});page.row("move","Erkennung aktualisieren","Installierte Apps und geteilte Kanäle erneut prüfen",this::load);
  page.add(new GridSlider(a,"Vorschau · Position von oben",100,340,prefs.getInt(ContentPreviewData.Y,200),n->prefs.edit().putInt(ContentPreviewData.Y,n).apply()));page.info(data==null?"Kanäle werden geprüft …":data.status);if(data==null)return;
  page.group("ERKENNUNG PRO APP");Set<String> seen=new HashSet<>();AppAppearance apps=new AppAppearance(a,Config.load(a));for(AppAppearance.Item app:apps.items){if(apps.hidden.contains(app.pkg)||!seen.add(app.pkg))continue;List<ContentPreviewData.Channel> channels=data.app(app.pkg);String status=channels.isEmpty()?"Keine freigegebenen Vorschaukanäle":channels.size()+(channels.size()==1?" Kanal · ":" Kanäle · ")+data.programs(app.pkg)+" Inhalte";page.row("video",AppAppearance.label(a,app.pkg,String.valueOf(app.info.loadLabel(a.getPackageManager()))),status,()->app(app.pkg,channels));}
  page.info("Netflix, Disney+, Prime und andere Apps entscheiden selbst, ob und welche Inhalte sie teilen. Anmeldung, Region und App-Version können das ändern. Gesperrte Apps zeigen keine Vorschaubilder oder Titel. Ohne geteilte Kanäle erscheint keine erfundene Inhaltsliste.");}
 private void app(String pkg,List<ContentPreviewData.Channel> channels){SettingsSheet p=new SettingsSheet(a,"Vorschau der App");p.toggle("Vorschauen dieser App anzeigen",null,ContentPreviewData.appEnabled(a,pkg),on->set(ContentPreviewData.DISABLED,pkg,!on));if(channels.isEmpty())p.info("Diese App veröffentlicht momentan keine lesbaren Vorschaukanäle auf diesem TV.");for(ContentPreviewData.Channel c:channels)p.toggle(c.name,c.total+" geteilte Inhalte · bis zu 12 auf Home",!prefs.getStringSet(ContentPreviewData.HIDDEN,Collections.emptySet()).contains(String.valueOf(c.id)),on->set(ContentPreviewData.HIDDEN,String.valueOf(c.id),!on));p.show();}
 private void set(String key,String value,boolean add){Set<String> next=new HashSet<>(prefs.getStringSet(key,Collections.emptySet()));if(add)next.add(value);else next.remove(value);prefs.edit().putStringSet(key,next).apply();}
}
