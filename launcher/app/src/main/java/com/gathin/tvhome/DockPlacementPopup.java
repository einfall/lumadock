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
        sheet.toggle("Dock selbst positionieren", "Gilt für Nur Dock; Änderungen sind sofort sichtbar",
                prefs.getBoolean(ENABLED,false), on->save(prefs,prefs.edit().putBoolean(ENABLED,on),changed));
        sheet.add(new GridSlider(a,"Dock-Breite (%)",60,100,prefs.getInt(WIDTH,100),
                n->save(prefs,prefs.edit().putInt(WIDTH,n),changed)));
        sheet.add(new GridSlider(a,"Dock horizontal · links → rechts (%)",0,100,prefs.getInt(X,50),
                n->save(prefs,prefs.edit().putInt(X,n),changed)));
        sheet.add(new GridSlider(a,"Dock vertikal · oben → unten (%)",0,100,prefs.getInt(Y,100),
                n->save(prefs,prefs.edit().putInt(Y,n),changed)));
        sheet.info("Position im freien Bereich unter der Kopfzeile. Für seitliches Verschieben zuerst die Dock-Breite verkleinern. Ohne eigene Position bleibt der bisherige Look erhalten.");
        sheet.toggle("Seitenpunkte im Dock", "iOS-Stil · unten mittig · ein Punkt je Dock-Seite",
                prefs.getBoolean(DOTS,false), on->save(prefs,prefs.edit().putBoolean(DOTS,on),changed));
    }
    private static void save(SharedPreferences prefs,SharedPreferences.Editor editor,Runnable changed) {
        editor.putLong(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();changed.run();
    }
}
