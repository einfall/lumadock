package com.gathin.tvhome;
import android.content.Context;
import android.content.SharedPreferences;
final class FocusStyle {
    static final String SHINE="focus_shine",EDGE="focus_thin_edge",COLOR="focus_color";
    static final String[] COLORS={"original","white","blue","cyan","purple","pink","orange","red","green","dominant"};
    private static String legacyColor(Context c){String s=c.getSharedPreferences(Config.SETTINGS_NAME,0).getString(COLOR,"original");for(String v:COLORS)if(v.equals(s))return s;return "original";}
    static String color(Context c){return profile(c,true).color;}
    static TileEffects profile(Context c,boolean focused){
        SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);String key=focused?"effects_focus_":"effects_idle_";
        boolean oldEdge=p.getBoolean(EDGE,false);
        return new TileEffects(p.getBoolean(key+"frame",focused||oldEdge),p.getBoolean(key+"glass",focused&&!oldEdge),
            p.getBoolean(key+"shine",focused&&p.getBoolean(SHINE,false)),p.getBoolean(key+"thin",oldEdge),
            p.getString(key+"color",legacyColor(c)),p.getInt(key+"gap",0),p.getBoolean(key+"contrast",false),p.getInt(key+"contrast_width",1),p.getInt(key+"width",0),p.getBoolean(key+"nuvio",false),p.getBoolean(key+"glow",false),p.getInt(key+"glow_strength",18),p.getInt(key+"glow_spread",6));
    }
    static void save(Context c,boolean focused,TileEffects e){
        SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);String k=focused?"effects_focus_":"effects_idle_";
        p.edit().putBoolean(k+"frame",e.frame).putBoolean(k+"glass",e.glass).putBoolean(k+"shine",e.shine).putBoolean(k+"thin",e.thin)
            .putString(k+"color",e.color).putInt(k+"gap",e.gap).putBoolean(k+"contrast",e.contrast).putInt(k+"contrast_width",e.contrastWidth).putInt(k+"width",e.width).putBoolean(k+"nuvio",e.nuvio).putBoolean(k+"glow",e.glow).putInt(k+"glow_strength",e.glowStrength).putInt(k+"glow_spread",e.glowSpread).putLong(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();
    }
    static FocusMotion motion(Context c,Config cfg){
        SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);
        return new FocusMotion(!"none".equals(frameMode(c)),p.getBoolean("motion_zoom",true),
            p.getBoolean("motion_in",true),p.getBoolean("motion_out",true),p.getBoolean("motion_bounce",cfg.focusOvershoot));
    }
    static void saveMotion(Context c,FocusMotion m){
        SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);
        p.edit().putBoolean("motion_overlay",m.overlay).putBoolean("motion_zoom",m.zoom).putBoolean("motion_in",m.animateIn)
            .putBoolean("motion_out",m.animateOut).putBoolean("motion_bounce",m.bounce)
            .putLong(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();
    }
    static String frameMode(Context c){SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);return FocusFrameMode.parse(p.getString("motion_frame_mode",null),p.getBoolean("motion_overlay",true));}
    static void saveFrameMode(Context c,String mode){
        SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);mode=FocusFrameMode.parse(mode,true);
        SharedPreferences.Editor e=p.edit().putString("motion_frame_mode",mode).putBoolean("motion_overlay",!"none".equals(mode));
        if(!"none".equals(mode))e.putBoolean("effects_focus_frame",true);
        e.putLong(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();
    }
    static int fixed(String s){switch(s){case "white":return 0xFFFFFFFF;case "blue":return 0xFF519EFF;case "cyan":return 0xFF45E3EE;case "purple":return 0xFFAD83FF;case "pink":return 0xFFFF70C8;case "orange":return 0xFFFFAE52;case "red":return 0xFFFF545F;case "green":return 0xFF59E99A;default:return 0;}}
    static void apply(GlassView glass,Context c){glass.setEffects(profile(c,true));}
}
