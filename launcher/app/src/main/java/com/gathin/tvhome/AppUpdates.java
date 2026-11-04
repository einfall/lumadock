package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
/** No background service: check on Home at most daily; installation always uses Android's confirmation. */
final class AppUpdates {
 static final String AUTO="luma_update_auto",DOWNLOAD="luma_update_download",LAST="luma_update_last";
 static final class State {final String stage,message;final int percent;final AppUpdateClient.Release release;final File file;State(String s,String m,int p,AppUpdateClient.Release r,File f){stage=s;message=m;percent=p;release=r;file=f;}boolean busy(){return "checking".equals(stage)||"download".equals(stage)||"verifying".equals(stage);}}
 private static AppUpdates instance;static synchronized AppUpdates get(Context c){if(instance==null)instance=new AppUpdates(c.getApplicationContext());return instance;}
 private final Context c;private final SharedPreferences prefs;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Set<Consumer<State>> listeners=new LinkedHashSet<>();private WeakReference<Activity> home=new WeakReference<>(null);private State state=new State("idle","Bereit zur Update-Prüfung",0,null,null);
 private AppUpdates(Context c){this.c=c;prefs=c.getSharedPreferences(Config.SETTINGS_NAME,0);}
 State state(){return state;}void observe(Consumer<State> listener){listeners.add(listener);listener.accept(state);}void forget(Consumer<State> listener){listeners.remove(listener);}
 private void state(State next){state=next;for(Consumer<State> l:new ArrayList<>(listeners))l.accept(next);}
 static void onHome(Activity a){AppUpdates u=get(a);u.home=new WeakReference<>(a);if(u.prefs.getBoolean(AUTO,true)&&UpdatePolicy.due(System.currentTimeMillis(),u.prefs.getLong(LAST,0)))u.check(true);}
 void check(boolean automatic){if(state.busy())return;prefs.edit().putLong(LAST,System.currentTimeMillis()).apply();state(new State("checking","Release-Informationen werden geprüft …",0,null,null));worker.execute(()->{try{AppUpdateClient.Release r=AppUpdateClient.fetch(c);boolean newer=r.code>AppUpdateClient.version(AppUpdateClient.installed(c));main.post(()->{state(new State(newer?"available":"current",newer?"Luma Dock "+r.version+" ist verfügbar":"Luma Dock "+r.version+" ist aktuell",0,r,null));if(automatic&&newer){notifyHome("Luma Dock "+r.version+" verfügbar · Einstellungen → Info");if(prefs.getBoolean(DOWNLOAD,false))download();}});}catch(Exception e){main.post(()->state(new State("error",message(e),0,null,null)));}});}
 void download(){if(state.busy())return;AppUpdateClient.Release release=state.release;if(release==null){check(false);return;}state(new State("download","APK wird heruntergeladen …",0,release,null));worker.execute(()->{try{File file=AppUpdateClient.download(c,release,done->main.post(()->state(new State(done>=release.size?"verifying":"download",done>=release.size?"Prüfsumme und APK-Signatur werden geprüft …":"APK wird heruntergeladen …",(int)Math.min(100,done*100/release.size),release,null))));main.post(()->{state(new State("ready","APK geprüft · bereit zur Installation",100,release,file));notifyHome("Luma Dock: Download geprüft · Installation unter Info");});}catch(Exception e){main.post(()->state(new State("error",message(e),0,release,null)));}});}
 void install(Activity a){State ready=state;if(!"ready".equals(ready.stage)||ready.file==null){Toast.makeText(a,"Zuerst eine APK herunterladen und prüfen lassen",Toast.LENGTH_LONG).show();return;}if(!a.getPackageManager().canRequestPackageInstalls()){allow(a);return;}state(new State("verifying","APK vor Installation erneut prüfen …",100,ready.release,ready.file));worker.execute(()->{try{AppUpdateClient.verify(c,ready.release,ready.file);main.post(()->{state(new State("ready","APK geprüft · Android-Installer öffnen",100,ready.release,ready.file));if(a.isFinishing()||a.isDestroyed())return;try{Uri uri=FileProvider.getUriForFile(a,a.getPackageName()+".iconpack-files",ready.file);a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(ActivityNotFoundException|SecurityException e){state(new State("error","Android-Installer nicht verfügbar",0,ready.release,null));}});}catch(Exception e){main.post(()->state(new State("error",message(e),0,ready.release,null)));}});}
 static void allow(Activity a){try{a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));}catch(ActivityNotFoundException e){Toast.makeText(a,R.string.not_available,Toast.LENGTH_LONG).show();}}
 private void notifyHome(String message){Activity a=home.get();if(a!=null&&!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus())Toast.makeText(a,message,Toast.LENGTH_LONG).show();}
 private static String message(Exception e){return e.getMessage()==null?"Update-Prüfung fehlgeschlagen":e.getMessage();}
}
