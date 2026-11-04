package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Separate paint settings: edits invalidate the dock only, preserving app focus and scroll position. */
final class DockStylePopup {
    private final Activity a;private final Runnable changed;private final SharedPreferences prefs;private final Config cfg;
    static void show(Activity a,Runnable changed){new DockStylePopup(a,changed).home(null);}
    private DockStylePopup(Activity a,Runnable changed){this.a=a;this.changed=changed;prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);cfg=Config.load(a);}
    private String s(int id){return a.getString(id);}
    private DockStyle current(){return DockStyle.load(a,cfg);}
    private String settingKey(String key){return DockStyle.key(current().mode,key);}
    private void save(SharedPreferences.Editor edit,boolean material){if(material&&"legacy".equals(current().mode))edit.putString(DockStyle.MODE,"solid");edit.putLong(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();changed.run();}
    static void append(Activity a,Runnable changed,SettingsSheet sheet){new DockStylePopup(a,changed).home(sheet);}
    private void home(SettingsSheet parent){
        DockStyle d=current();SettingsSheet p=parent==null?new SettingsSheet(a,s(R.string.dock_style_title)):parent;p.group(s(R.string.dock_style_title));
        LinearLayout visible=p.toggle(s(R.string.dock_style_visible),null,d.visible,on->save(prefs.edit().putBoolean(DockStyle.VISIBLE,on),false));
        String[] modes={"legacy","solid","gradient","glass","quickaccess"};p.options("dock",s(R.string.dock_style_material),java.util.Arrays.stream(modes).map(this::modeName).toArray(String[]::new),i->modes[i].equals(current().mode),i->{String mode=modes[i];SharedPreferences.Editor edit=prefs.edit().putString(DockStyle.MODE,mode);if(!prefs.contains(DockStyle.key(mode,DockStyle.TRANSPARENCY)))edit.putInt(DockStyle.key(mode,DockStyle.TRANSPARENCY),"quickaccess".equals(mode)?86:"glass".equals(mode)?60:"gradient".equals(mode)?20:0);save(edit,false);});
        p.group(s(R.string.dock_style_colors));inlineColor(p,DockStyle.TOP,s(R.string.dock_style_top));inlineColor(p,DockStyle.BOTTOM,s(R.string.dock_style_bottom));
        GridSlider transparency=new GridSlider(a,s(R.string.dock_style_transparency),0,100,d.transparency,n->save(prefs.edit().putInt(settingKey(DockStyle.TRANSPARENCY),n),true));p.add(transparency);
        LinearLayout gloss=p.toggle(s(R.string.dock_style_gloss),s(R.string.dock_style_gloss_hint),d.gloss,on->save(prefs.edit().putBoolean(settingKey(DockStyle.GLOSS),on),true));
        p.group(s(R.string.dock_style_shape));GridSlider radius=new GridSlider(a,s(R.string.dock_style_radius),0,60,d.radius,n->save(prefs.edit().putInt(settingKey(DockStyle.RADIUS),n),true));p.add(radius);
        LinearLayout rimShown=p.toggle(s(R.string.dock_style_rim),null,d.rim,on->save(prefs.edit().putBoolean(settingKey(DockStyle.RIM),on),true));
        inlineColor(p,DockStyle.RIM_COLOR,s(R.string.dock_style_rim_color));p.row("tune",s(R.string.dock_style_custom_color),"Eigene HEX-Farbe für oben, unten oder Rand",()->{SettingsSheet custom=new SettingsSheet(a,s(R.string.dock_style_custom_color));for(String key:new String[]{DockStyle.TOP,DockStyle.BOTTOM,DockStyle.RIM_COLOR})custom.row("photo",key,null,()->hex(key,key,colorValue(key),custom));custom.show();});
        GridSlider rimStrength=new GridSlider(a,s(R.string.dock_style_rim_strength),0,100,d.rimOpacity,n->save(prefs.edit().putInt(settingKey(DockStyle.RIM_OPACITY),n),true));p.add(rimStrength);
        p.info(s(R.string.dock_style_behavior));p.row("settings",s(R.string.dock_style_reset),s(R.string.dock_style_reset_hint),()->{SharedPreferences.Editor edit=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith(DockStyle.P))edit.remove(key);save(edit,false);});
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{if(key!=null&&key.startsWith(DockStyle.P)){DockStyle next=current();transparency.syncValue(next.transparency);radius.syncValue(next.radius);rimStrength.syncValue(next.rimOpacity);SettingsSheet.syncToggle(visible,next.visible);SettingsSheet.syncToggle(gloss,next.gloss);SettingsSheet.syncToggle(rimShown,next.rim);p.refreshChoices();}};
        prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(dialog->prefs.unregisterOnSharedPreferenceChangeListener(update));if(parent==null)p.show();
    }
    private void inlineColor(SettingsSheet p,String key,String title){int[] values={0xFF141416,0xFF000000,0xFF343B50,0xFFFFFFFF,0xFF526079,0xFF244D7A,0xFF23585D,0xFF4A376A,0xFF773C62,0xFF48664F,0xFF8C5732,0xFFC8494D};String[] names=a.getResources().getStringArray(R.array.dock_style_color_names);p.options("photo",title,names,i->colorValue(key)==values[i],i->save(prefs.edit().putInt(settingKey(key),values[i]),true));}
    private static void subtitle(LinearLayout row,String value){((TextView)((LinearLayout)row.getChildAt(1)).getChildAt(1)).setText(value);}
    private String modeName(String mode){switch(DockStyle.mode(mode)){case "solid":return s(R.string.dock_style_solid);case "gradient":return s(R.string.dock_style_gradient);case "glass":return s(R.string.dock_style_glass);case "quickaccess":return s(R.string.dock_style_quickaccess);default:return s(R.string.dock_style_legacy);}}
    private void materials(){
        SettingsSheet p=new SettingsSheet(a,s(R.string.dock_style_material));
        for(String mode:new String[]{"legacy","solid","gradient","glass","quickaccess"})p.choice("dock",modeName(mode),"quickaccess".equals(mode)?s(R.string.dock_style_quickaccess_hint):null,()->current().mode.equals(mode),()->{
            SharedPreferences.Editor edit=prefs.edit().putString(DockStyle.MODE,mode);
            if(!prefs.contains(DockStyle.key(mode,DockStyle.TRANSPARENCY)))edit.putInt(DockStyle.key(mode,DockStyle.TRANSPARENCY),"quickaccess".equals(mode)?86:"glass".equals(mode)?60:"gradient".equals(mode)?20:0);
            save(edit,false);
        });
        p.info(s(R.string.dock_style_material_hint));p.show();
    }
    private int colorValue(String key){DockStyle style=current();return DockStyle.BOTTOM.equals(key)?style.bottom:DockStyle.RIM_COLOR.equals(key)?style.rimColor:style.top;}
    private void colors(String key,String title){
        SettingsSheet p=new SettingsSheet(a,title);
        int[] values={0xFF141416,0xFF000000,0xFF343B50,0xFFFFFFFF,0xFF526079,0xFF244D7A,0xFF23585D,0xFF4A376A,0xFF773C62,0xFF48664F,0xFF8C5732,0xFFC8494D};
        String[] names=a.getResources().getStringArray(R.array.dock_style_color_names);
        LinearLayout custom=p.row("tune",s(R.string.dock_style_custom_color),DockStyle.hex(colorValue(key)),()->hex(key,title,colorValue(key),p));
        for(int i=0;i<values.length;i++){
            final int color=values[i];LinearLayout row=p.choice("photo",names[i],DockStyle.hex(color),()->colorValue(key)==color,()->save(prefs.edit().putInt(settingKey(key),color),true));
            ((ImageView)row.getChildAt(0)).setImageTintList(android.content.res.ColorStateList.valueOf(color));
        }
        Runnable refresh=()->{
            int selected=colorValue(key);boolean preset=false;for(int value:values)if(value==selected)preset=true;
            subtitle(custom,DockStyle.hex(selected));custom.setSelected(!preset);
            ((TextView)custom.getChildAt(custom.getChildCount()-1)).setText(preset?"›":"✓");p.refreshChoices();
        };refresh.run();
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,setting)->{if(setting!=null&&setting.startsWith(DockStyle.P))refresh.run();};
        prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();
    }
    private void hex(String key,String title,int color,SettingsSheet parent){
        LinearLayout panel=SettingsSheet.panel(a);int pad=SettingsSheet.dp(a,18);panel.setPadding(pad,pad,pad,pad);panel.addView(GlassDialog.text(a,title,18));
        TextView hint=GlassDialog.text(a,s(R.string.dock_style_hex_hint),11);hint.setPadding(0,SettingsSheet.dp(a,8),0,SettingsSheet.dp(a,10));panel.addView(hint);
        EditText edit=new EditText(a);edit.setSingleLine(true);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(7)});edit.setTextColor(Color.WHITE);edit.setTextSize(17);edit.setSelectAllOnFocus(true);edit.setText(DockStyle.hex(color));edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);edit.setBackgroundResource(R.drawable.weather_field);panel.addView(edit,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,44)));
        TextView error=GlassDialog.text(a,"",10);error.setTextColor(0xFFFFB59F);panel.addView(error,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,25)));
        LinearLayout actions=new LinearLayout(a);TextView clear=DesignPopup.button(a,s(R.string.dock_style_clear)),use=DesignPopup.button(a,s(R.string.dock_style_use));actions.addView(clear,new LinearLayout.LayoutParams(0,SettingsSheet.dp(a,34),1));LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(0,SettingsSheet.dp(a,34),2);up.setMarginStart(SettingsSheet.dp(a,8));actions.addView(use,up);panel.addView(actions);Dialog dialog=GlassDialog.show(a,panel,360,220);if(dialog.getWindow()!=null)dialog.getWindow().setDimAmount(.12f);
        clear.setOnClickListener(v->{edit.setText("");edit.requestFocus();android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.showSoftInput(edit,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
        dialog.setOnDismissListener(ignored->{android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);});
        use.setOnClickListener(v->{Integer parsed=DockStyle.parseHex(edit.getText().toString());if(parsed==null){error.setText(R.string.dock_style_hex_error);return;}save(prefs.edit().putInt(settingKey(key),parsed),true);error.setText("");parent.refreshChoices();use.requestFocus();android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);});use.requestFocus();
    }
}
