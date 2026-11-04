package com.gathin.tvhome;
import android.content.*;
import android.graphics.*;
import java.util.*;
/** Optional Apache-licensed Material Icons Outlined; no Projectivy app logos are bundled. */
final class MenuGlyphs {
    static final String KEY="menu_material_outlined";private static Typeface face;private static boolean enabled;
    private static SharedPreferences prefs;private static SharedPreferences.OnSharedPreferenceChangeListener listener;
    private static final Map<AppGlyph,Boolean> LIVE=new WeakHashMap<>();
    static synchronized void watch(AppGlyph glyph){LIVE.put(glyph,true);}
    static void init(Context context){if(prefs!=null)return;Context app=context.getApplicationContext();prefs=app.getSharedPreferences(Config.SETTINGS_NAME,0);
        listener=(p,key)->{if(KEY.equals(key))update(app);};prefs.registerOnSharedPreferenceChangeListener(listener);update(app);}
    private static void update(Context c){enabled=prefs.getBoolean(KEY,false);if(enabled&&face==null)try{face=Typeface.createFromAsset(c.getAssets(),"reference_fonts/material_outlined.ttf");}catch(RuntimeException e){enabled=false;}
        java.util.List<AppGlyph> copy; synchronized(MenuGlyphs.class){copy=new ArrayList<>(LIVE.keySet());}for(AppGlyph g:copy)if(g!=null)g.invalidateSelf();}
    static boolean draw(Canvas c,Paint p,String kind){
        if(!enabled||face==null)return false;int code;switch(kind){case "settings":case "tune":code=0xe429;break;case "grid":code=0xe5c3;break;case "photo":code=0xe3f4;break;case "video":code=0xe04a;break;case "tv":code=0xe333;break;case "star":code=0xe838;break;case "eye":code=0xe8f4;break;case "eye_off":code=0xe8f5;break;case "lock":code=0xe897;break;case "unlock":code=0xe898;break;case "move":code=0xe5d2;break;case "cpu":case "memory":code=0xe322;break;case "storage":code=0xe1db;break;case "cloud":code=0xe2bd;break;case "moon":code=0xef44;break;case "timer":code=0xe425;break;case "volume":code=0xe050;break;case "music":code=0xe405;break;case "game":code=0xe30f;break;case "dock":code=0xe30a;break;default:return false;}
        String text=String.valueOf((char)code);p.setTypeface(face);p.setTextSize(23);if(!p.hasGlyph(text))return false;p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);Paint.FontMetrics f=p.getFontMetrics();c.drawText(text,12,12-(f.ascent+f.descent)/2,p);return true;
    }
}
