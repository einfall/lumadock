package com.gathin.tvhome;
import android.content.Context;
import android.content.SharedPreferences;
/** Local paint-only overrides. Legacy is exact until the user chooses or edits a material. */
final class DockStyle {
    static final String P="dock_design_",MODE=P+"mode",VISIBLE=P+"visible",TOP=P+"top",BOTTOM=P+"bottom",TRANSPARENCY=P+"transparency",RIM=P+"rim",RIM_COLOR=P+"rim_color",RIM_OPACITY=P+"rim_opacity",GLOSS=P+"gloss",RADIUS=P+"radius";
    final String mode;final boolean visible,rim,gloss;final int top,bottom,transparency,rimColor,rimOpacity,radius;
    DockStyle(String mode,boolean visible,int top,int bottom,int transparency,boolean rim,int rimColor,int rimOpacity,boolean gloss,int radius){
        this.mode=mode(mode);this.visible=visible;this.top=0xFF000000|top;this.bottom=0xFF000000|bottom;this.transparency=bound(transparency,0,100);this.rim=rim;this.rimColor=0xFF000000|rimColor;this.rimOpacity=bound(rimOpacity,0,100);this.gloss=gloss;this.radius=bound(radius,0,60);
    }
    static String mode(String id){for(String m:new String[]{"solid","gradient","glass","quickaccess"})if(m.equals(id))return m;return "legacy";}
    /** QuickAccess has its own paint profile; switching styles never overwrites the existing material. */
    static String key(String mode,String setting){return "quickaccess".equals(mode)&&!MODE.equals(setting)&&!VISIBLE.equals(setting)?P+"quickaccess_"+setting.substring(P.length()):setting;}
    static int bound(int value,int low,int high){return Math.max(low,Math.min(high,value));}
    static int alpha(int rgb,int percent){return Math.round(bound(percent,0,100)*2.55f)<<24|(rgb&0xFFFFFF);}
    static int mix(int a,int b,float amount){float t=Math.max(0,Math.min(1,amount));int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t),g=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t),v=Math.round((a&255)*(1-t)+(b&255)*t);return 0xFF000000|r<<16|g<<8|v;}
    static String hex(int color){return String.format(java.util.Locale.ROOT,"#%06X",color&0xFFFFFF);}
    static Integer parseHex(String value){if(value==null||!value.trim().matches("#?[0-9a-fA-F]{6}"))return null;return 0xFF000000|Integer.parseInt(value.trim().replace("#",""),16);}
    static DockStyle load(Context c,Config cfg){SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);String mode=mode(p.getString(MODE,"legacy"));boolean quick="quickaccess".equals(mode);return new DockStyle(mode,p.getBoolean(VISIBLE,cfg.dockBand),p.getInt(key(mode,TOP),quick?0xFFFFFFFF:0xFF141416),p.getInt(key(mode,BOTTOM),quick?0xFFFFFFFF:0xFF343B50),p.getInt(key(mode,TRANSPARENCY),quick?86:"glass".equals(mode)?60:100-cfg.dockOpacity),p.getBoolean(key(mode,RIM),true),p.getInt(key(mode,RIM_COLOR),0xFFFFFFFF),p.getInt(key(mode,RIM_OPACITY),quick?18:25),p.getBoolean(key(mode,GLOSS),!quick),p.getInt(key(mode,RADIUS),26));}
}
