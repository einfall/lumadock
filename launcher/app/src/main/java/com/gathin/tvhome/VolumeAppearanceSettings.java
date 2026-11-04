package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.*;
import android.view.Gravity;
import android.widget.FrameLayout;

/** A local preview only: opening or editing this page never sends soundbar commands. */
final class VolumeAppearanceSettings {
    static void append(Activity a,SettingsSheet p){
        SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);
        String[] materials={"Glas · passend zu Home","Button-Theme folgen","Widget-Theme folgen","Originale Stilfarben","Eisglas","Graphit","Nur Kontur","Hell","Aurora"};
        p.options("photo","Material & Farben",materials,i->VolumeAppearance.MATERIALS[i].equals(VolumeAppearance.material(prefs)),i->prefs.edit().putString(VolumeAppearance.MATERIAL,VolumeAppearance.MATERIALS[i]).apply());
        String[] accents={"Automatisch","Weiß","Cyan","Blau","Grün","Amber","Rosa"};
        p.options("star","Akzent · Ring und Füllstand",accents,i->VolumeAppearance.ACCENTS[i].equals(VolumeAppearance.accent(prefs)),i->prefs.edit().putString(VolumeAppearance.ACCENT,VolumeAppearance.ACCENTS[i]).apply());
        p.add(new GridSlider(a,"Zusätzliche Transparenz (%)",0,80,prefs.getInt(VolumeAppearance.TRANSPARENCY,0),n->prefs.edit().putInt(VolumeAppearance.TRANSPARENCY,n).apply()));
        p.info("Form und Animation bleiben beim gewählten Stil. Material, Schriftfarben und Kontur passen zur Oberfläche. Originale Stilfarben ignoriert Akzent und Transparenz. Widget-Theme folgt dem globalen Widget-Stil.");
        FrameLayout preview=new FrameLayout(a);p.preview(preview,112);VolumeOsdView[] sample={null};boolean[] mute={false};Handler handler=new Handler(Looper.getMainLooper());
        Runnable mount=()->{if(sample[0]!=null)sample[0].stop();preview.removeAllViews();VolumeStyle style=VolumeStyle.parse(prefs.getString(VolumeStyle.KEY,"glass"));VolumeOsdView view=new VolumeOsdView(a,style);sample[0]=view;
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(SettingsSheet.dp(a,style.width),SettingsSheet.dp(a,style.height),Gravity.CENTER);preview.addView(view,lp);
            preview.post(()->{if(sample[0]!=view)return;float scale=Math.min(1f,Math.min(preview.getWidth()/(float)Math.max(1,view.getWidth()),preview.getHeight()/(float)Math.max(1,view.getHeight())));view.setScaleX(scale);view.setScaleY(scale);view.state(35,mute[0]);});};
        p.toggle("Vorschau: Stumm",null,false,on->{mute[0]=on;if(sample[0]!=null)sample[0].state(35,on);});p.info("Vorschau mit Beispielwert 35 · ändert keine echte Lautstärke.");
        SharedPreferences.OnSharedPreferenceChangeListener listener=(sp,key)->{if(key!=null&&(key.equals(VolumeStyle.KEY)||key.startsWith("volume_")||key.startsWith(SurfaceThemes.P))){handler.removeCallbacks(mount);handler.postDelayed(mount,40);}};
        prefs.registerOnSharedPreferenceChangeListener(listener);p.dialog.setOnDismissListener(d->{prefs.unregisterOnSharedPreferenceChangeListener(listener);handler.removeCallbacksAndMessages(null);if(sample[0]!=null)sample[0].stop();});preview.post(mount);
    }
}
