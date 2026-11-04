package com.gathin.tvhome;
import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.TextView;
final class DockBrowsePopup {
    static void show(Activity a,Runnable changed){SettingsSheet p=new SettingsSheet(a,a.getString(R.string.dock_browse_title));append(a,changed,p);p.show();}
    static void append(Activity a,Runnable changed,SettingsSheet sheet){
        SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);Config cfg=Config.load(a);int columns=prefs.getInt("dock_columns",cfg.columns);String[] ids={"edge","paging","pivot"};
        sheet.options("move",a.getString(R.string.dock_browse_mode),java.util.Arrays.stream(ids).map(id->name(a,id)).toArray(String[]::new),i->ids[i].equals(DockBrowsePolicy.mode(prefs.getString("dock_browse","edge"))),i->{save(prefs,prefs.edit().putString("dock_browse",ids[i]));changed.run();});
        sheet.add(new GridSlider(a,a.getString(R.string.dock_browse_pivot),1,columns,DockBrowsePolicy.pivot(prefs.getInt("dock_pivot",(columns+1)/2),columns),n->{int actual=prefs.getInt("dock_columns",Config.load(a).columns);save(prefs,prefs.edit().putInt("dock_pivot",Math.min(n,actual)));changed.run();}).maxFrom(()->prefs.getInt("dock_columns",Config.load(a).columns)));sheet.info(a.getString(R.string.dock_browse_pivot_hint));
    }
    private static void save(SharedPreferences p,SharedPreferences.Editor e){e.putLong(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();}
    private static String name(Activity a,String mode){return a.getString("pivot".equals(mode)?R.string.dock_browse_pivot_name:"paging".equals(mode)?R.string.dock_browse_paging:R.string.dock_browse_edge);}
}
