package com.gathin.tvhome;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.*;
/** Each scope has its own sample, selected value and immediate live refresh. */
final class TypographyPopup {
    static void show(Activity a,Runnable changed){
        SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);SettingsSheet p=new SettingsSheet(a,a.getString(R.string.typography_title));
        p.group(a.getString(R.string.typography_icons));p.row("grid",a.getString(R.string.iconpacks_title),a.getString(R.string.iconpacks_hint),()->IconPackPicker.show(a,changed));
        p.group(a.getString(R.string.typography_fonts_title));
        if(a instanceof MainActivity)p.row("move",a.getString(R.string.title_style),a.getString(R.string.title_style_hint),()->AppTitlePopup.show((MainActivity)a,changed));
        String[] scopes={Typography.APPS,Typography.HOME_CLOCK,Typography.SAVER_CLOCK};int[] names={R.string.typography_apps,R.string.typography_clock,R.string.typography_saver};
        for(int i=0;i<scopes.length;i++)appendFont(a,p,scopes[i],a.getString(names[i]),changed);
        p.options("grid","Menü-Symbole",new String[]{"Bisherige","Material Outlined"},i->(i==1)==prefs.getBoolean(MenuGlyphs.KEY,false),i->prefs.edit().putBoolean(MenuGlyphs.KEY,i==1).apply());
        p.group(a.getString(R.string.visual_menu_title));p.toggle(a.getString(R.string.visual_menu_compact),a.getString(R.string.visual_menu_compact_hint),prefs.getBoolean("visual_menu_compact",false),on->{prefs.edit().putBoolean("visual_menu_compact",on).apply();SettingsSheet.resizeAll(a);});
        p.row("eye",a.getString(R.string.visual_preview),a.getString(R.string.visual_preview_hint),()->SettingsPreview.show(a));p.info(a.getString(R.string.typography_hint));
        p.show();
    }
    static void appendFont(Activity a,SettingsSheet p,String scope,String title,Runnable changed){
        SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);java.util.List<String> ids=new java.util.ArrayList<>();for(String id:FontPolicy.IDS)if(!Typography.APPS.equals(scope)||FontPolicy.forAppNames(id))ids.add(id);
        String[] labels=ids.stream().map(id->Typography.name(a,id)).toArray(String[]::new);
        int fallback=Typography.SAVER_CLOCK.equals(scope)?R.string.font_extralight:R.string.font_light;
        p.options("tune",title,labels,i->ids.get(i).equals(Typography.selected(a,scope)),i->{prefs.edit().putString(scope,ids.get(i)).putLong(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();changed.run();})
            .fontPreviews(Typography.APPS.equals(scope)?"Netflix":"12:34",i->Typography.face(a,ids.get(i),fallback));
    }
    static void choose(Activity a,String scope,String title,Runnable changed){
        SettingsSheet p=new SettingsSheet(a,title);SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);
        int fallback=Typography.SAVER_CLOCK.equals(scope)?R.string.font_extralight:R.string.font_light;
        TextView sample=GlassDialog.text(a,Typography.APPS.equals(scope)?"Netflix · YouTube":"12:34",Typography.APPS.equals(scope)?26:48);sample.setGravity(Gravity.CENTER);p.preview(sample,76);
        for(String id:FontPolicy.IDS){if(Typography.APPS.equals(scope)&&!FontPolicy.forAppNames(id))continue;LinearLayout row=p.choice("tune",Typography.name(a,id),Typography.APPS.equals(scope)?"Aa · Netflix · YouTube":"0123456789 · 12:34",()->id.equals(Typography.selected(a,scope)),()->{prefs.edit().putString(scope,id).putLong(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();changed.run();});
            ((TextView)((LinearLayout)row.getChildAt(1)).getChildAt(1)).setTypeface(Typography.face(a,id,fallback));row.setOnFocusChangeListener((v,on)->{if(on)sample.setTypeface(Typography.face(a,id,fallback));});}
        p.info(a.getString(R.string.typography_choose_hint));p.show();
    }
}
