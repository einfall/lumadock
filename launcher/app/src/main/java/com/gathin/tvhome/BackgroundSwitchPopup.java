package com.gathin.tvhome;
import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.*;
/** Fixed selection and mixed rotation, with the existing interval/Home/TV wake triggers. */
final class BackgroundSwitchPopup {
    static void show(Activity a,Runnable changed){
        SharedPreferences p=LiveWallpaperPrefs.prefs(a),config=a.getSharedPreferences(Config.SETTINGS_NAME,0);SettingsSheet sheet=new SettingsSheet(a,"Hintergrund · Wechsel");
        TextView status=GlassDialog.text(a,"",12);status.setPadding(SettingsSheet.dp(a,7),SettingsSheet.dp(a,5),0,SettingsSheet.dp(a,9));sheet.add(status);
        sheet.options("photo","Auswahl",new String[]{"Fest","Wechsel"},i->(i==1)==BackgroundSelection.automatic(a),i->BackgroundSelection.mode(a,i==1?"rotation":"fixed"));
        String[] types={"Live-Hintergründe","Bilder","Verläufe"};sheet.options("grid","Was darf wechseln?",types,i->BackgroundSelection.types(a).contains(BackgroundRotationPolicy.TYPES[i]),i->BackgroundSelection.type(a,BackgroundRotationPolicy.TYPES[i]),true);
        String[] triggers={"off","interval","home","tv"};String[] names={"Aus","Nach Zeit","Bei Home-Start","Bei TV-Start"};
        sheet.options("timer","Wann wechseln?",names,i->i==0?LiveWallpaperSelection.triggers(a).isEmpty():LiveWallpaperSelection.triggers(a).contains(triggers[i]),i->LiveWallpaperSelection.trigger(a,triggers[i],i==0||!LiveWallpaperSelection.triggers(a).contains(triggers[i])),true);
        sheet.add(new GridSlider(a,"Intervall (Minuten)",1,240,LiveRotationPolicy.minutes(p.getInt(LiveWallpaperSelection.MINUTES,15)),n->p.edit().putInt(LiveWallpaperSelection.MINUTES,n).apply()));
        String[] sources={"favorites","category","all","A","Z","C"};sheet.options("video","Live-Hintergründe aus",java.util.Arrays.stream(sources).map(id->LiveWallpaperSelection.name(a,id)).toArray(String[]::new),i->sources[i].equals(p.getString(LiveWallpaperSelection.POOL,"category")),i->p.edit().putString(LiveWallpaperSelection.POOL,sources[i]).apply());
        sheet.info("Bilder und Verläufe stammen aus den eingebauten Sammlungen. Gewählte Arten haben die gleiche Chance; leere Sammlungen werden übersprungen. Ohne Art oder Auslöser pausiert der Wechsel. Er läuft nur auf Home.");
        sheet.row("move","Jetzt wechseln","Einmal aus den erlaubten Arten auswählen",()->{if(BackgroundSelection.random(a))changed.run();else Toast.makeText(a,"Keine Hintergründe für diese Auswahl vorhanden",Toast.LENGTH_LONG).show();});
        sheet.row("video","Live-Hintergründe einstellen","Eigene Datei / URL, Suche und Abdunklung",()->LiveWallpaperPopup.show(a,changed));
        if(a instanceof MainActivity)sheet.row("photo","Eigene Theme-Bilder",null,()->LauncherBehaviorPopup.wallpapers((MainActivity)a));
        sheet.info("Eine Kachel oder eigene Datei setzt einen festen Hintergrund und pausiert den Wechsel. Deine Wechselregeln bleiben gespeichert. Fest hält den gerade sichtbaren Hintergrund.");
        Runnable update=()->{String note=BackgroundSelection.automatic(a)?"Wechsel":"Fest";if(BackgroundSelection.automatic(a)&&(BackgroundSelection.types(a).isEmpty()||LiveWallpaperSelection.triggers(a).isEmpty()))note+=" · pausiert";status.setText(note+"\n"+BackgroundSelection.currentLabel(a));sheet.refreshChoices();};
        SharedPreferences.OnSharedPreferenceChangeListener listener=(sp,key)->update.run();p.registerOnSharedPreferenceChangeListener(listener);config.registerOnSharedPreferenceChangeListener(listener);sheet.dialog.setOnDismissListener(d->{p.unregisterOnSharedPreferenceChangeListener(listener);config.unregisterOnSharedPreferenceChangeListener(listener);});update.run();sheet.show();
    }
}
