package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.widget.*;
/** Presets plus independent, persistent positions and visibility for every header element. */
final class HeaderLayoutPopup {
    private final Activity a;private final HeaderLayoutController layout;private final Runnable changed;private final SharedPreferences prefs;
    static void show(Activity a,HeaderLayoutController layout,Runnable changed){if(layout!=null)new HeaderLayoutPopup(a,layout,changed).home();}
    private HeaderLayoutPopup(Activity a,HeaderLayoutController layout,Runnable changed){this.a=a;this.layout=layout;this.changed=changed;prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);}
    private String s(int id){return a.getString(id);}
    private String name(String key){return a.getResources().getStringArray(R.array.header_item_names)[HeaderLayoutController.index(key)];}
    private String modeName(String mode){switch(HeaderLayoutPolicy.mode(mode)){case "row":return s(R.string.header_row);case "stack":return s(R.string.header_stack);default:return s(R.string.header_legacy);}}
    private String description(String key){int[] p=layout.position(key);return a.getString(layout.shown(key)?R.string.header_position_on:R.string.header_position_off,p[0],p[1]);}
    private void home(){
        SettingsSheet p=new SettingsSheet(a,s(R.string.header_title));String[] modes={"legacy","row","stack"};p.options("grid",s(R.string.header_preset),java.util.Arrays.stream(modes).map(this::modeName).toArray(String[]::new),i->modes[i].equals(HeaderLayoutPolicy.mode(prefs.getString(HeaderLayoutController.MODE,"legacy"))),i->{layout.preset(modes[i]);changed.run();});p.group(s(R.string.header_items));
        java.util.Map<String,LinearLayout> rows=new java.util.LinkedHashMap<>();String[] icons={"tune","timer","timer","cloud","cpu","memory","storage"};for(int i=0;i<HeaderLayoutController.KEYS.length;i++){String key=HeaderLayoutController.KEYS[i];if(i>=4)continue;rows.put(key,p.row(icons[i],name(key),description(key),()->item(key)));}
        p.info(s(R.string.header_live_hint));p.row("settings",s(R.string.header_reset),s(R.string.header_reset_hint),()->{layout.preset("legacy");changed.run();});
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->p.body.postDelayed(()->{p.refreshChoices();for(String item:rows.keySet())subtitle(rows.get(item),description(item));},50);prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();
    }
    private void presets(){SettingsSheet p=new SettingsSheet(a,s(R.string.header_preset));for(String mode:new String[]{"legacy","row","stack"}){p.choice("grid",modeName(mode),null,()->HeaderLayoutPolicy.mode(prefs.getString(HeaderLayoutController.MODE,"legacy")).equals(mode),()->{layout.preset(mode);changed.run();});}p.info(s(R.string.header_preset_hint));p.show();}
    private static void subtitle(LinearLayout row,String value){((TextView)((LinearLayout)row.getChildAt(1)).getChildAt(1)).setText(value);}
    private void item(String key){
        SettingsSheet p=new SettingsSheet(a,name(key));LinearLayout visible=p.toggle(s(R.string.header_show),null,layout.shown(key),on->{layout.show(key,on);changed.run();});
        p.row("move",s(R.string.header_move),s(R.string.header_move_hint),()->HeaderMoveOverlay.show(a,layout,key,name(key),changed));int[] pos=layout.position(key),size=layout.size(key);int[] value={pos[0],pos[1]};
        GridSlider horizontal=new GridSlider(a,s(R.string.header_x),0,Math.max(0,layout.width()-size[0]),value[0],n->{value[0]=n;layout.set(key,value[0],value[1]);changed.run();});p.add(horizontal);
        GridSlider vertical=new GridSlider(a,s(R.string.header_y),0,Math.max(0,layout.height()-size[1]),value[1],n->{value[1]=n;layout.set(key,value[0],value[1]);changed.run();});p.add(vertical);
        p.add(new GridSlider(a,s(R.string.header_step),1,20,prefs.getInt(HeaderLayoutController.STEP,5),n->prefs.edit().putInt(HeaderLayoutController.STEP,n).apply()));
        p.row("settings",s(R.string.header_reset_item),s(R.string.header_reset_item_hint),()->{layout.reset(key);changed.run();});p.info(s(R.string.header_units));
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,setting)->p.body.postDelayed(()->{int[] now=layout.position(key);value[0]=prefs.getInt(HeaderLayoutController.P+key+"_x",now[0]);value[1]=prefs.getInt(HeaderLayoutController.P+key+"_y",now[1]);horizontal.syncValue(value[0]);vertical.syncValue(value[1]);SettingsSheet.syncToggle(visible,layout.shown(key));},50);prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();
    }
}
