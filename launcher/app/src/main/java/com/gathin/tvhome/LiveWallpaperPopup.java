package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.File;
import java.util.*;

/** Explicit selection only: browsing the manifest never streams a preview or a thumbnail. */
final class LiveWallpaperPopup {
    private final Activity a;private final Runnable changed;private final SharedPreferences prefs,runtime;
    static void show(Activity a,Runnable changed){new LiveWallpaperPopup(a,changed).home();}
    private LiveWallpaperPopup(Activity a,Runnable changed){this.a=a;this.changed=changed;prefs=LiveWallpaperPrefs.prefs(a);runtime=a.getSharedPreferences(LiveWallpaperPrefs.RUNTIME,0);}
    private String s(int id){return a.getString(id);}
    private void home(){
        LiveWallpaperCatalog.ensureFile(a);SettingsSheet p=new SettingsSheet(a,"Live-Hintergründe einstellen");
        TextView status=GlassDialog.text(a,"",11);status.setTextColor(0xFFB5C6DC);status.setPadding(dp(7),dp(8),dp(7),dp(12));p.add(status);
        final LinearLayout[] enabled={null};
        Runnable update=()->{if(enabled[0]!=null)SettingsSheet.syncToggle(enabled[0],prefs.getBoolean("enabled",false));String state=runtime.getString("state","off");int text;switch(state){case "playing":text=R.string.live_playing;break;case "loading":text=R.string.live_loading;break;case "error":text=R.string.live_error;break;case "empty":text=R.string.live_no_source;break;case "paused":text=R.string.live_paused;break;default:text=R.string.live_off;}
            status.setText(LiveWallpaperPrefs.title(a)+"\n"+s(text));};update.run();
        SharedPreferences.OnSharedPreferenceChangeListener listener=(sp,key)->update.run();prefs.registerOnSharedPreferenceChangeListener(listener);runtime.registerOnSharedPreferenceChangeListener(listener);p.dialog.setOnDismissListener(d->{prefs.unregisterOnSharedPreferenceChangeListener(listener);runtime.unregisterOnSharedPreferenceChangeListener(listener);});
        enabled[0]=p.toggle(s(R.string.live_enabled),s(R.string.live_enabled_hint),prefs.getBoolean("enabled",false),on->{BackgroundSelection.mode(a,"fixed");prefs.edit().putBoolean("enabled",on).apply();changed.run();});
        p.group(s(R.string.live_source));p.row("tune",s(R.string.live_search),s(R.string.live_search_hint),()->search(LiveWallpaperCatalog.load(a)));p.row("photo","Live-Hintergründe auswählen",s(R.string.live_presets_hint),()->DesignStudio.media(a,false,true));p.row("video",s(R.string.live_custom),s(R.string.live_custom_hint),this::custom);p.row("photo",s(R.string.live_file),s(R.string.live_file_hint),this::pickFile);
        p.info("Automatischer Wechsel von Live-Hintergründen, Bildern und Verläufen wird unter Hintergrund → Wechsel eingestellt. Eine eigene Datei / URL legt einen festen Hintergrund fest.");
        p.group(s(R.string.menu_appearance));p.add(new GridSlider(a,s(R.string.live_dim),0,90,LiveWallpaperSource.dim(prefs.getInt("dim",35)),n->{prefs.edit().putInt("dim",n).apply();changed.run();}));
        p.info(s(R.string.live_behavior));p.show();
    }
    private void presets(){List<LiveWallpaperCatalog.Item> all=LiveWallpaperCatalog.load(a);SettingsSheet p=new SettingsSheet(a,s(R.string.live_presets));
        p.row("star",s(R.string.live_favorites),s(R.string.live_browse_keys),()->browse("favorites"));
        for(String group:new String[]{"all","A","Z","C"}){List<LiveWallpaperCatalog.Item> list=LiveWallpaperSelection.pool(a,group);p.row("photo",LiveWallpaperSelection.name(a,group)+" ("+list.size()+")",null,()->browse(group));}
        p.row("tune",s(R.string.live_search),s(R.string.live_search_hint),()->search(all));p.info(s(R.string.live_network_hint));p.info(s(R.string.live_catalog_hint)+"\n"+LiveWallpaperCatalog.file(a).getAbsolutePath());p.show();
    }
    private void browse(String group){if(!"favorites".equals(group))prefs.edit().putString(LiveWallpaperSelection.CATEGORY,group).apply();LiveWallpaperBrowser.show(a,LiveWallpaperSelection.name(a,group),LiveWallpaperSelection.pool(a,group),"favorites".equals(group),changed);}
    private void rotation(){SettingsSheet p=new SettingsSheet(a,s(R.string.live_random));appendRotation(p);p.show();}
    private void appendRotation(SettingsSheet p){
        p.group(s(R.string.live_random));String[] sources={"favorites","category","all","A","Z","C"};p.options("photo",s(R.string.live_random_source),java.util.Arrays.stream(sources).map(id->LiveWallpaperSelection.name(a,id)).toArray(String[]::new),i->sources[i].equals(prefs.getString(LiveWallpaperSelection.POOL,"category")),i->prefs.edit().putString(LiveWallpaperSelection.POOL,sources[i]).apply());
        String[] triggers={"off","interval","home","tv"};p.options("timer",s(R.string.live_random_mode),java.util.Arrays.stream(triggers).map(this::modeName).toArray(String[]::new),i->i==0?LiveWallpaperSelection.triggers(a).isEmpty():LiveWallpaperSelection.triggers(a).contains(triggers[i]),i->{boolean on=i==0||!LiveWallpaperSelection.triggers(a).contains(triggers[i]);LiveWallpaperSelection.trigger(a,triggers[i],on);},true);
        p.add(new GridSlider(a,s(R.string.live_random_minutes),1,240,LiveRotationPolicy.minutes(prefs.getInt(LiveWallpaperSelection.MINUTES,15)),n->prefs.edit().putInt(LiveWallpaperSelection.MINUTES,n).apply()));p.row("move",s(R.string.live_random_now),null,()->{if(LiveWallpaperSelection.random(a))changed.run();else Toast.makeText(a,R.string.live_random_empty,Toast.LENGTH_LONG).show();});p.info(s(R.string.live_random_multiple_hint));
    }
    private String modeName(String id){switch(LiveRotationPolicy.mode(id)){case "interval":return s(R.string.live_random_interval);case "home":return s(R.string.live_random_home);case "tv":return s(R.string.live_random_tv);default:return s(R.string.menu_off);}}
    private void rotationSource(){SettingsSheet p=new SettingsSheet(a,s(R.string.live_random_source));for(String group:new String[]{"favorites","category","all","A","Z","C"}){p.choice("photo",LiveWallpaperSelection.name(a,group),null,()->group.equals(prefs.getString(LiveWallpaperSelection.POOL,"category")),()->prefs.edit().putString(LiveWallpaperSelection.POOL,group).apply());}p.show();}
    private String modeSummary(){Set<String> selected=LiveWallpaperSelection.triggers(a);List<String> names=new ArrayList<>();for(String id:LiveRotationPolicy.TRIGGERS)if(selected.contains(id))names.add(modeName(id));return names.isEmpty()?s(R.string.menu_off):android.text.TextUtils.join(" · ",names);}
    private void rotationMode(){
        SettingsSheet p=new SettingsSheet(a,s(R.string.live_random_mode));
        p.choice("timer",s(R.string.menu_off),null,()->LiveWallpaperSelection.triggers(a).isEmpty(),()->LiveWallpaperSelection.trigger(a,"off",true));
        Map<String,LinearLayout> rows=new LinkedHashMap<>();
        for(String id:LiveRotationPolicy.TRIGGERS)rows.put(id,p.toggle(modeName(id),null,LiveWallpaperSelection.triggers(a).contains(id),on->LiveWallpaperSelection.trigger(a,id,on)));
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{Set<String> selected=LiveWallpaperSelection.triggers(a);for(Map.Entry<String,LinearLayout> row:rows.entrySet())SettingsSheet.syncToggle(row.getValue(),selected.contains(row.getKey()));p.refreshChoices();};
        prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.info(s(R.string.live_random_multiple_hint));p.show();
    }
    private void search(List<LiveWallpaperCatalog.Item> all){input(s(R.string.live_search),"",s(R.string.live_search_hint),text->{List<LiveWallpaperCatalog.Item> list=new ArrayList<>();String query=text.toLowerCase(Locale.ROOT);for(LiveWallpaperCatalog.Item i:all)if(i.title.toLowerCase(Locale.ROOT).contains(query))list.add(i);LiveWallpaperBrowser.show(a,s(R.string.live_search),list,false,changed);return true;},false);}
    private void custom(){input(s(R.string.live_custom),LiveWallpaperPrefs.source(a),s(R.string.live_path_hint),text->{
        if(!LiveWallpaperSource.valid(text))return false;
        Uri uri=text.startsWith("/")?Uri.fromFile(new File(text)):Uri.parse(text);
        if("file".equalsIgnoreCase(uri.getScheme())&&!new File(uri.getPath()).canRead()){Toast.makeText(a,R.string.live_file_unreadable,Toast.LENGTH_LONG).show();return false;}
        String name=uri.getLastPathSegment();if(name==null||name.isEmpty())name=s(R.string.live_own);if(name.length()>80)name=name.substring(0,80);LiveWallpaperPrefs.select(a,text,name);changed.run();return true;
    },true);}
    interface Accept {boolean use(String text);}
    private void input(String title,String value,String hint,Accept accept,boolean uri){
        LinearLayout panel=SettingsSheet.panel(a);panel.setPadding(dp(20),dp(18),dp(20),dp(18));panel.addView(GlassDialog.text(a,title,19));
        TextView info=GlassDialog.text(a,hint,11);info.setTextColor(0xFFA9B5C6);info.setPadding(0,dp(8),0,dp(10));panel.addView(info);
        EditText edit=new EditText(a);edit.setText(value);edit.setTextColor(Color.WHITE);edit.setTextSize(13);edit.setSingleLine(true);edit.setSelectAllOnFocus(true);edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|(uri?InputType.TYPE_TEXT_VARIATION_URI:0));edit.setBackgroundResource(R.drawable.weather_field);panel.addView(edit,new LinearLayout.LayoutParams(-1,dp(44)));
        TextView error=GlassDialog.text(a,"",10);error.setTextColor(0xFFFFB59F);panel.addView(error,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout actions=new LinearLayout(a);TextView clear=DesignPopup.button(a,s(R.string.live_clear)),use=DesignPopup.button(a,s(R.string.live_use));actions.addView(clear,new LinearLayout.LayoutParams(0,dp(34),1));LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(0,dp(34),2);up.setMarginStart(dp(8));actions.addView(use,up);panel.addView(actions);
        clear.setOnClickListener(v->{edit.setText("");edit.requestFocus();InputMethodManager ime=(InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.showSoftInput(edit,InputMethodManager.SHOW_IMPLICIT);});Dialog d=GlassDialog.show(a,panel,440,270);if(uri&&d.getWindow()!=null)d.getWindow().setDimAmount(.12f);
        d.setOnDismissListener(dialog->{InputMethodManager ime=(InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);if(a instanceof MainActivity)((MainActivity)a).settleLiveWallpaperInput();});
        use.setOnClickListener(v->{String text=edit.getText().toString().trim();if(!accept.use(text)){error.setText(R.string.live_invalid);return;}InputMethodManager ime=(InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);error.setText("");if(uri)use.requestFocus();else d.dismiss();});use.requestFocus();
    }
    private void pickFile(){
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("video/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        // Several TVs expose only a placeholder that displays "no app can perform this action".
        java.util.List<android.content.pm.ResolveInfo> real=new java.util.ArrayList<>();
        for(android.content.pm.ResolveInfo r:a.getPackageManager().queryIntentActivities(pick,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY))if(r.activityInfo!=null&&!"com.google.android.tv.frameworkpackagestubs".equals(r.activityInfo.packageName))real.add(r);
        if(real.isEmpty()){Toast.makeText(a,R.string.live_no_picker,Toast.LENGTH_LONG).show();return;}
        if(real.size()==1)pick.setComponent(new ComponentName(real.get(0).activityInfo.packageName,real.get(0).activityInfo.name));
        try{a.startActivityForResult(pick,LiveWallpaperPrefs.PICK_VIDEO);}catch(ActivityNotFoundException|SecurityException ex){Toast.makeText(a,R.string.live_no_picker,Toast.LENGTH_LONG).show();}
    }
    static void picked(Activity a,Intent data){if(data==null||data.getData()==null)return;Uri uri=data.getData();if(!"content".equals(uri.getScheme()))return;try{if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0)a.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
        String name=a.getString(R.string.live_own);try(Cursor cursor=a.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst()&&!cursor.isNull(0))name=cursor.getString(0);}catch(RuntimeException ignored){}LiveWallpaperPrefs.select(a,uri.toString(),name);
    }
    private int dp(int n){return SettingsSheet.dp(a,n);}
}
