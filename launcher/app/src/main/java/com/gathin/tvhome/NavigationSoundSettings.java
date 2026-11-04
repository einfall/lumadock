package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;

final class NavigationSoundSettings {
    static void append(Activity activity,SettingsSheet sheet){
        SharedPreferences prefs=activity.getSharedPreferences(Config.SETTINGS_NAME,0);
        sheet.group("Navigationstöne");String[] ids={"system","luma","off"};
        sheet.options("volume","Tastentöne in Luma Dock",new String[]{"Systemtöne","Luma-Klick","Aus"},
                i->ids[i].equals(NavigationSoundPolicy.mode(prefs.getString(NavigationSounds.MODE,"system"))),
                i->prefs.edit().putString(NavigationSounds.MODE,ids[i]).apply());
        sheet.add(new GridSlider(activity,"Luma-Klick · Lautstärke (%)",0,100,NavigationSoundPolicy.volume(prefs.getInt(NavigationSounds.VOLUME,40)),
                n->prefs.edit().putInt(NavigationSounds.VOLUME,n).apply()));
        String[] outputs={"media","system"};
        sheet.options("volume","Luma-Klick · Audioweg",new String[]{"Medien · HDMI/ARC","Systemkanal"},
                i->outputs[i].equals(NavigationSoundPolicy.output(prefs.getString(NavigationSounds.OUTPUT,"media"))),
                i->prefs.edit().putString(NavigationSounds.OUTPUT,outputs[i]).apply());
        android.widget.LinearLayout preview=sheet.row("volume","Luma-Klick probehören","Mit der eingestellten Lautstärke",()->NavigationSounds.preview(activity));preview.setSoundEffectsEnabled(false);
        sheet.info("D-Pad, OK und Zurück auf Home und in Luma-Menüs. Der Luma-Klick ersetzt normale Klicks innerhalb unserer Oberfläche. Medien · HDMI/ARC nutzt den Medien-Audioweg, auch bei ausgeschalteten Android-Klicks. Lautstärke und Stummschaltung des Ausgabegeräts gelten weiterhin. Andere Apps und TV-Einstellungen bleiben unverändert.");
        sheet.group("HDMI-CEC-Lautstärke");
    }
}
