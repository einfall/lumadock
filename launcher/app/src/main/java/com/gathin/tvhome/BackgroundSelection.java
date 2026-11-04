package com.gathin.tvhome;
import android.content.*;
import java.util.*;
/** One selection mode for live and static Home backgrounds; does not change saver preferences. */
final class BackgroundSelection {
    static final String MODE="background_selection_mode",TYPES="background_rotation_types";
    static String mode(Context c){SharedPreferences p=LiveWallpaperPrefs.prefs(c);return BackgroundRotationPolicy.mode(p.getString(MODE,null),p.getBoolean("enabled",false),!LiveWallpaperSelection.triggers(c).isEmpty());}
    static boolean automatic(Context c){return "rotation".equals(mode(c));}
    static Set<String> types(Context c){SharedPreferences p=LiveWallpaperPrefs.prefs(c);return BackgroundRotationPolicy.types(p.contains(TYPES)?p.getStringSet(TYPES,Collections.emptySet()):null);}
    static void migrate(Context c){SharedPreferences p=LiveWallpaperPrefs.prefs(c);if(!p.contains(MODE))p.edit().putString(MODE,mode(c)).apply();}
    static void mode(Context c,String mode){LiveWallpaperPrefs.prefs(c).edit().putString(MODE,"rotation".equals(mode)?"rotation":"fixed").apply();}
    static void type(Context c,String type){Set<String> selected=types(c);if(!selected.remove(type))selected.add(type);LiveWallpaperPrefs.prefs(c).edit().putStringSet(TYPES,BackgroundRotationPolicy.types(selected)).apply();}
    static String currentKind(Context c){if(LiveWallpaperPrefs.prefs(c).getBoolean("enabled",false))return "live";String mode=c.getSharedPreferences(Config.SETTINGS_NAME,0).getString(HomeBackdrop.MODE,"original");return "photo".equals(mode)?"photo":"gradient".equals(mode)?"gradient":"original";}
    static String currentLabel(Context c){String kind=currentKind(c);SharedPreferences settings=c.getSharedPreferences(Config.SETTINGS_NAME,0);String id=settings.getString(HomeBackdrop.ID,"0");if("live".equals(kind))return "Live-Hintergrund · "+LiveWallpaperPrefs.title(c);if("photo".equals(kind)){for(SaverCatalog.Photo photo:SaverCatalog.photos(c))if(photo.file.equals(id))return "Bild · "+photo.title;return "Bild";}if("gradient".equals(kind))try{return "Verlauf · "+HomeBackdrop.NAMES[Math.max(0,Math.min(HomeBackdrop.NAMES.length-1,Integer.parseInt(id)))];}catch(NumberFormatException ignored){}return "Theme-Hintergrund";}
    static boolean random(Context c){
        SharedPreferences p=LiveWallpaperPrefs.prefs(c),settings=c.getSharedPreferences(Config.SETTINGS_NAME,0);
        List<LiveWallpaperCatalog.Item> live=LiveWallpaperSelection.pool(c,p.getString(LiveWallpaperSelection.POOL,"category"));List<SaverCatalog.Photo> photos=SaverCatalog.photos(c);
        Set<String> selected=types(c);List<String> available=BackgroundRotationPolicy.available(selected,live.size(),photos.size(),HomeBackdrop.COLORS.length);
        String current=currentKind(c);int count="live".equals(current)?live.size():"photo".equals(current)?photos.size():HomeBackdrop.COLORS.length;
        Random random=new Random();String kind=BackgroundRotationPolicy.kind(available,current,count==1,random.nextInt());if(kind==null)return false;
        int at=-1;
        if("live".equals(kind)){if(kind.equals(current))for(int i=0;i<live.size();i++)if(live.get(i).url.equals(LiveWallpaperPrefs.source(c)))at=i;LiveWallpaperCatalog.Item item=live.get(LiveRotationPolicy.nextIndex(live.size(),at,random.nextInt()));LiveWallpaperPrefs.apply(c,item.url,item.title);}
        else if("photo".equals(kind)){if(kind.equals(current))for(int i=0;i<photos.size();i++)if(photos.get(i).file.equals(settings.getString(HomeBackdrop.ID,"")))at=i;SaverCatalog.Photo photo=photos.get(LiveRotationPolicy.nextIndex(photos.size(),at,random.nextInt()));HomeBackdrop.apply(c,"photo",photo.file);}
        else{if(kind.equals(current))try{at=Integer.parseInt(settings.getString(HomeBackdrop.ID,"-1"));}catch(NumberFormatException ignored){}HomeBackdrop.apply(c,"gradient",String.valueOf(LiveRotationPolicy.nextIndex(HomeBackdrop.COLORS.length,at,random.nextInt())));}
        return true;
    }
}
