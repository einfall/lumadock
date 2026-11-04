package com.gathin.tvhome;
import android.content.*;
final class LiveWallpaperPrefs {
    static final String NAME="live_wallpaper",RUNTIME="live_wallpaper_runtime";
    static final int PICK_VIDEO=5810;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(NAME,0);}
    static void clock(Context c){prefs(c).edit().putLong("last_change_elapsed",android.os.SystemClock.elapsedRealtime()).putInt("last_change_boot",android.provider.Settings.Global.getInt(c.getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,0)).apply();}
    static void select(Context c,String source,String name){BackgroundSelection.mode(c,"fixed");apply(c,source,name);}
    static void apply(Context c,String source,String name){clock(c);prefs(c).edit().putString("source",LiveWallpaperSource.clean(source)).putString("title",name).putBoolean("enabled",true).apply();}
    static String title(Context c){return prefs(c).getString("title",c.getString(R.string.live_no_source));}
    static String source(Context c){return prefs(c).getString("source","");}
}
