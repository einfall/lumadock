package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.TextView;
import java.util.*;
/** Applies only paint. App adapters, D-pad focus, icon sizing and header positions are untouched. */
final class SurfaceThemeController {
    private final Activity a;private final SharedPreferences prefs;private final Runnable widgets;
    private final Map<TextView,Original> originals=new LinkedHashMap<>();
    private static final class Original{final Drawable background;final ColorStateList ink,tint;Original(TextView v){background=v.getBackground();ink=v.getTextColors();tint=v.getCompoundDrawableTintList();}}
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(key!=null&&key.startsWith(SurfaceThemes.P)){changed();}};
    SurfaceThemeController(Activity a,Runnable widgets){this.a=a;this.widgets=widgets;prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);for(int id:new int[]{R.id.btnInputs,R.id.btnWifi,R.id.btnVpn,R.id.btnReorder,R.id.btnLibraryTop,R.id.btnSleep,R.id.btnSettings,R.id.btnDesign}){TextView v=a.findViewById(id);originals.put(v,new Original(v));}prefs.registerOnSharedPreferenceChangeListener(listener);apply();}
    private void changed(){apply();widgets.run();}
    void apply(){String id=SurfaceThemes.resolve(prefs,SurfaceThemes.BUTTON);for(TextView v:originals.keySet()){
        if("original".equals(id)){Original o=originals.get(v);v.setBackground(o.background);v.setTextColor(o.ink);v.setCompoundDrawableTintList(o.tint);}
        else{SurfaceStyle s=SurfaceStyle.of(id,true);v.setBackground(SurfaceThemes.background(a,id,true));v.setTextColor(SurfaceThemes.ink(s));v.setCompoundDrawableTintList(SurfaceThemes.ink(s));}
    }applySleep();}
    void applySleep(){TextView v=a.findViewById(R.id.btnSleep);int active=SleepPolicy.color(android.os.SystemClock.elapsedRealtime(),SleepTimer.end(a));if(active!=0){SurfaceStyle s=SurfaceStyle.of(SurfaceThemes.resolve(prefs,SurfaceThemes.BUTTON),true);v.setCompoundDrawableTintList(new ColorStateList(new int[][]{{android.R.attr.state_focused},{}},new int[]{s.focusInk,active}));}else v.setCompoundDrawableTintList(v.getTextColors());}
    void close(){prefs.unregisterOnSharedPreferenceChangeListener(listener);}
}
