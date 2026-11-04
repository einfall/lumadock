package com.gathin.tvhome;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;
/** Favorites store only catalog URLs in private preferences, independently of the selected video. */
final class LiveWallpaperSelection {
    static final String FAVORITES="favorites",POOL="random_pool",MODE="random_mode",TRIGGERS="random_triggers",MINUTES="random_minutes",CATEGORY="category";
    static Set<String> triggers(Context c){SharedPreferences p=LiveWallpaperPrefs.prefs(c);return LiveRotationPolicy.triggers(p.contains(TRIGGERS)?p.getStringSet(TRIGGERS,Collections.emptySet()):null,p.getString(MODE,"off"));}
    static void trigger(Context c,String id,boolean on){Set<String> next=LiveRotationPolicy.withTrigger(triggers(c),id,on);LiveWallpaperPrefs.prefs(c).edit().putStringSet(TRIGGERS,next).putString(MODE,LiveRotationPolicy.legacyMode(next)).apply();}
    static Set<String> favorites(Context c){return new HashSet<>(LiveWallpaperPrefs.prefs(c).getStringSet(FAVORITES,Collections.emptySet()));}
    static boolean toggle(Context c,String url){Set<String> f=favorites(c);boolean on=!f.remove(url);if(on)f.add(url);LiveWallpaperPrefs.prefs(c).edit().putStringSet(FAVORITES,f).apply();return on;}
    static List<LiveWallpaperCatalog.Item> pool(Context c,String group){
        SharedPreferences p=LiveWallpaperPrefs.prefs(c);if("category".equals(group))group=p.getString(CATEGORY,"all");
        Set<String> f=favorites(c);List<LiveWallpaperCatalog.Item> result=new ArrayList<>();
        for(LiveWallpaperCatalog.Item i:LiveWallpaperCatalog.load(c))if("favorites".equals(group)?f.contains(i.url):"all".equals(group)||("C".equals(group)?i.source.startsWith("C"):group.equals(i.source)))result.add(i);
        return result;
    }
    static String name(Context c,String group){
        if("category".equals(group))return c.getString(R.string.live_random_category)+": "+name(c,LiveWallpaperPrefs.prefs(c).getString(CATEGORY,"all"));
        if("favorites".equals(group))return c.getString(R.string.live_favorites);
        if("all".equals(group))return c.getString(R.string.live_all);return LiveWallpaperCatalog.sourceName(group);
    }
    static boolean random(Context c){return BackgroundSelection.random(c);}
}
