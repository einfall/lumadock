package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;

final class DockPlacementPopup {
    static final String ENABLED="dock_position_enabled", WIDTH="dock_position_width",
            X="dock_position_x", Y="dock_position_y", DOTS="dock_page_dots";
    static int pageRoom(Activity a, boolean singleRow) {
        return singleRow && a.getSharedPreferences(Config.SETTINGS_NAME,0).getBoolean(DOTS,false)
                ? Math.round(20*a.getResources().getDisplayMetrics().density) : 0;
    }
    static void append(Activity a, SettingsSheet sheet, Runnable changed) {
        SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);
        sheet.group("Nur Dock · Position & Seiten");
        android.widget.LinearLayout ownPosition=sheet.toggle("Dock selbst positionieren", "Gespeicherte Position im Modus Nur Dock verwenden",
                prefs.getBoolean(ENABLED,false), on->save(prefs,prefs.edit().putBoolean(ENABLED,on),changed));
        GridSlider width=new GridSlider(a,"Dock-Breite (%)",60,100,prefs.getInt(WIDTH,100),
                n->save(prefs,prefs.edit().putInt(WIDTH,n),changed));sheet.add(width);
        sheet.row("move","Dock verschieben","Pfeiltasten bewegen · OK speichert · Zurück verwirft",()->{
            if(a instanceof MainActivity)((MainActivity)a).moveDock(()->{SettingsSheet.syncToggle(ownPosition,prefs.getBoolean(ENABLED,false));width.syncValue(prefs.getInt(WIDTH,100));changed.run();});
        });
        sheet.add(new GridSlider(a,"Verschieben · Schrittweite (dp)",1,20,prefs.getInt(HeaderLayoutController.STEP,5),
                n->prefs.edit().putInt(HeaderLayoutController.STEP,n).apply()));
        sheet.info("Verschieben wie bei den Widgets, mit Live-Vorschau auf dem Bildschirm. Die Schrittweite gilt auch für Widgets und Kopfzeile. Ohne eigene Position bleibt der bisherige Look erhalten.");
        sheet.toggle("Seitenpunkte im Dock", "iOS-Stil · unten mittig · ein Punkt je Dock-Seite",
                prefs.getBoolean(DOTS,false), on->save(prefs,prefs.edit().putBoolean(DOTS,on),changed));
    }
    private static void save(SharedPreferences prefs,SharedPreferences.Editor editor,Runnable changed) {
        editor.putLong(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();changed.run();
    }
}
