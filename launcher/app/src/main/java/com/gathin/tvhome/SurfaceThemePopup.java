package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.widget.*;
final class SurfaceThemePopup {
    static String name(Activity a,String id){String[] ids=SurfaceStyle.IDS;for(int i=0;i<ids.length;i++)if(ids[i].equals(id))return a.getResources().getStringArray(R.array.surface_theme_names)[i];return a.getString(R.string.surface_follow);}
    static void choose(Activity a,String title,String selected,boolean inherit,java.util.function.Consumer<String> use){
        SettingsSheet p=new SettingsSheet(a,title);java.util.List<String> ids=new java.util.ArrayList<>();if(inherit)ids.add("inherit");java.util.Collections.addAll(ids,SurfaceStyle.IDS);
        String[] current={selected};for(String id:ids)p.choice("photo",name(a,id),null,()->id.equals(current[0]),()->{use.accept(id);current[0]=id;});p.show();
    }
    static void show(Activity a){SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);SettingsSheet p=new SettingsSheet(a,a.getString(R.string.surface_title));p.info(a.getString(R.string.surface_hint));
        String[] common=SurfaceStyle.IDS;p.options("photo",a.getString(R.string.surface_common),java.util.Arrays.stream(common).map(id->name(a,id)).toArray(String[]::new),i->common[i].equals(prefs.getString(SurfaceThemes.COMMON,"original")),i->prefs.edit().putString(SurfaceThemes.COMMON,common[i]).putString(SurfaceThemes.BUTTON,"inherit").putString(SurfaceThemes.WIDGET,"inherit").apply());
        String[] separate=new String[common.length+1];separate[0]="inherit";System.arraycopy(common,0,separate,1,common.length);String[] labels=java.util.Arrays.stream(separate).map(id->name(a,id)).toArray(String[]::new);
        p.options("tune",a.getString(R.string.surface_buttons),labels,i->separate[i].equals(prefs.getString(SurfaceThemes.BUTTON,"inherit")),i->prefs.edit().putString(SurfaceThemes.BUTTON,separate[i]).apply());
        p.options("cpu",a.getString(R.string.surface_widgets),labels,i->separate[i].equals(prefs.getString(SurfaceThemes.WIDGET,"inherit")),i->prefs.edit().putString(SurfaceThemes.WIDGET,separate[i]).apply());
        SharedPreferences.OnSharedPreferenceChangeListener listener=(sp,key)->p.refreshChoices();prefs.registerOnSharedPreferenceChangeListener(listener);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(listener));p.info(a.getString(R.string.surface_note));p.show();
    }
}
