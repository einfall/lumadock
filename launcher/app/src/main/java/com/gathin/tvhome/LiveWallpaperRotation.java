package com.gathin.tvhome;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import java.util.Set;
/** One timer only while Home is visible; independent interval, Home and TV triggers. */
final class LiveWallpaperRotation {
    private final Context context;private final SharedPreferences p;private final Runnable changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LiveRotationWake wake;private Set<String> previous;
    private boolean active,choosing,closed,homePending;
    private final Runnable tick=()->{if(ready()&&has("interval"))choose("interval");schedule();};
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(prefs,key)->preferencesChanged(key);
    LiveWallpaperRotation(Context c,Runnable changed){
        context=c;BackgroundSelection.migrate(c);p=LiveWallpaperPrefs.prefs(c);this.changed=changed;previous=LiveWallpaperSelection.triggers(c);
        wake=new LiveRotationWake(!wakeDue());p.registerOnSharedPreferenceChangeListener(listener);
    }
    private boolean has(String id){return LiveWallpaperSelection.triggers(context).contains(id);}
    private int boot(){return Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,0);}
    private boolean wakeDue(){return p.getBoolean("random_tv_pending",false)||p.getInt("random_tv_boot",-1)!=boot();}
    private void consumedWake(){p.edit().putInt("random_tv_boot",boot()).putBoolean("random_tv_pending",false).apply();}
    private boolean ready(){PowerManager power=context.getSystemService(PowerManager.class);return active&&!closed&&BackgroundSelection.automatic(context)&&!BackgroundSelection.types(context).isEmpty()&&power!=null&&power.isInteractive();}
    private void preferencesChanged(String key){
        if(closed||choosing)return;
        boolean controls=BackgroundSelection.MODE.equals(key)||BackgroundSelection.TYPES.equals(key);
        boolean triggers=LiveWallpaperSelection.MODE.equals(key)||LiveWallpaperSelection.TRIGGERS.equals(key);
        if(triggers){
            Set<String> next=LiveWallpaperSelection.triggers(context);
            if(!previous.contains("tv")&&next.contains("tv")){wake.prime();consumedWake();}
            previous=next;
        }
        if(controls||triggers||LiveWallpaperSelection.MINUTES.equals(key)||LiveWallpaperSelection.POOL.equals(key))LiveWallpaperPrefs.clock(context);
        if(controls||triggers||LiveWallpaperSelection.MINUTES.equals(key)||LiveWallpaperSelection.POOL.equals(key)||LiveWallpaperSelection.CATEGORY.equals(key)||LiveWallpaperSelection.FAVORITES.equals(key)||"source".equals(key)||"enabled".equals(key)){schedule();changed.run();}
    }
    void start(){
        if(closed)return;
        if(active){schedule();return;}
        active=true;homePending=BackgroundSelection.automatic(context);
        if(ready()){
            boolean tv=has("tv")&&wakeDue()&&!wake.handled(),home=has("home");
            boolean select=wake.start(home,has("tv"),wakeDue(),SystemClock.elapsedRealtime());
            homePending=false;if(tv)consumedWake();
            if(select)choose(tv?(home?"home+tv":"tv"):"home");
        }
        schedule();
    }
    void stop(){active=false;homePending=false;main.removeCallbacks(tick);}
    void screenOff(){wake.off();main.removeCallbacks(tick);p.edit().putBoolean("random_tv_pending",true).apply();}
    void screenOn(){
        if(!wake.handled())p.edit().putBoolean("random_tv_pending",true).apply();
        if(ready()){
            boolean tv=wake.screenOn(has("tv"),true,SystemClock.elapsedRealtime()),home=homePending&&has("home");
            homePending=false;if(tv)consumedWake();if(tv||home)choose(tv?"tv":"home");
        }
        schedule();
    }
    private void choose(String cause){
        choosing=true;
        try{
            if(LiveWallpaperSelection.random(context)){android.util.Log.i("TvHome.Rotation","change "+cause);changed.run();}
            else LiveWallpaperPrefs.clock(context);
        }finally{choosing=false;}
    }
    private void schedule(){
        main.removeCallbacks(tick);if(!ready()||!has("interval"))return;
        long now=SystemClock.elapsedRealtime(),last=p.getLong("last_change_elapsed",0);
        int saved=p.getInt("last_change_boot",-1),boot=boot();
        if(saved!=boot||last<=0||last>now){LiveWallpaperPrefs.clock(context);last=now;saved=boot;}
        long delay=LiveRotationPolicy.delay(now,last,saved,boot,p.getInt(LiveWallpaperSelection.MINUTES,15));
        main.postDelayed(tick,Math.max(250,delay));
    }
    void close(){closed=true;stop();p.unregisterOnSharedPreferenceChangeListener(listener);}
}
