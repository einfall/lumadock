package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/** Material and colour are independent of the volume layout and its animation. */
final class VolumeAppearance {
    static final String MATERIAL="volume_material",ACCENT="volume_accent",TRANSPARENCY="volume_transparency";
    static final String[] MATERIALS={"glass","buttons","widgets","original","ice","graphite","outline","light","aurora"};
    static final String[] ACCENTS={"auto","white","cyan","blue","green","amber","pink"};
    final boolean original,sheen;
    final int top,bottom,rim,ink,muted,accent,track;
    private VolumeAppearance(boolean original,boolean sheen,int top,int bottom,int rim,int ink,int muted,int accent,int track){this.original=original;this.sheen=sheen;this.top=top;this.bottom=bottom;this.rim=rim;this.ink=ink;this.muted=muted;this.accent=accent;this.track=track;}
    static String material(SharedPreferences p){String id=p.getString(MATERIAL,"original");for(String valid:MATERIALS)if(valid.equals(id))return id;return "original";}
    static String accent(SharedPreferences p){String id=p.getString(ACCENT,"auto");for(String valid:ACCENTS)if(valid.equals(id))return id;return "auto";}
    static VolumeAppearance load(Context c){
        SharedPreferences p=c.getSharedPreferences(Config.SETTINGS_NAME,0);String id=material(p);
        boolean buttons="buttons".equals(id);
        String resolved=buttons?SurfaceThemes.resolve(p,SurfaceThemes.BUTTON):"widgets".equals(id)?SurfaceThemes.widget(c):id;
        SurfaceStyle s=SurfaceStyle.of(resolved,buttons);boolean glass="glass".equals(id);
        int top=glass?0xC43A424E:s.top,bottom=glass?0xB3151B24:s.bottom,rim=glass?0x70FFFFFF:s.rim;
        int opacity=100-Math.max(0,Math.min(80,p.getInt(TRANSPARENCY,0)));
        top=alpha(top,opacity);bottom=alpha(bottom,opacity);
        int accent=glass?0xFFE2F1FF:s.ink;
        switch(accent(p)){case "white":accent=Color.WHITE;break;case "cyan":accent=0xFF83E7EF;break;case "blue":accent=0xFF8BBFFF;break;case "green":accent=0xFF99E6BB;break;case "amber":accent=0xFFFFCE8B;break;case "pink":accent=0xFFFFA7CF;break;}
        return new VolumeAppearance("original".equals(id),glass||s.sheen,top,bottom,rim,s.ink,s.muted,accent,(s.ink&0xFFFFFF)|0x30000000);
    }
    private static int alpha(int color,int percent){return (color&0xFFFFFF)|((Color.alpha(color)*percent/100)<<24);}
}
