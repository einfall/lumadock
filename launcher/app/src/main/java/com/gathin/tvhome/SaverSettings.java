package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Edits an actual clock preview: Home's video and samplers are paused by Activity lifecycle. */
final class SaverSettings {
    private final Activity a;private final android.content.SharedPreferences prefs;private final List<SaverCatalog.Photo> photos;
    static void open(Activity a){a.startActivity(new Intent(a,ClockActivity.class).putExtra("saver_settings",true));}
    static void show(Activity a){new SaverSettings(a).home();}
    private SaverSettings(Activity a){this.a=a;prefs=SaverPrefs.get(a);photos=SaverCatalog.photos(a);}
    private String s(int id){return a.getString(id);}
    private String modeName(String id){switch(SaverPolicy.mode(id)){case "black":return s(R.string.saver_black);case "photos":return s(R.string.saver_photos);case "image":return s(R.string.saver_image);case "video":return s(R.string.saver_video);case "museum":return s(R.string.saver_museum);case "wind":return s(R.string.saver_wind);case "sky":return s(R.string.saver_sky);case "space":return s(R.string.saver_space);case "mosaic":return s(R.string.saver_mosaic);case "auto":return s(R.string.saver_auto);default:return s(R.string.saver_legacy);}}
    private String modeHint(String id){switch(id){case "museum":return s(R.string.saver_museum_hint);case "wind":return s(R.string.saver_wind_hint);case "sky":return s(R.string.saver_sky_hint);case "space":return s(R.string.saver_space_hint);case "mosaic":return s(R.string.saver_mosaic_hint);case "auto":return s(R.string.saver_auto_hint);default:return null;}}
    private void home(){
        SettingsSheet p=new SettingsSheet(a,s(R.string.saver_title));p.info(s(R.string.saver_preview_hint));
        TypographyPopup.appendFont(a,p,Typography.SAVER_CLOCK,s(R.string.typography_saver),()->{});
        String[] modes=SaverPolicy.MODES;p.options("photo",s(R.string.saver_background),java.util.Arrays.stream(modes).map(this::modeName).toArray(String[]::new),i->modes[i].equals(SaverPrefs.mode(a)),i->prefs.edit().putString(SaverPrefs.MODE,modes[i]).apply());
        p.row("move",s(R.string.saver_next_auto),s(R.string.saver_auto_hint),()->{if(!"auto".equals(SaverPrefs.mode(a))){Toast.makeText(a,R.string.saver_select_auto,Toast.LENGTH_SHORT).show();return;}if(a instanceof ClockActivity)((ClockActivity)a).nextAutoScene();});
        p.row("photo",s(R.string.saver_choose_photo),s(R.string.saver_photo_hint),()->DesignStudio.media(a,true,false));
        p.row("video",s(R.string.saver_choose_video),s(R.string.saver_video_hint),()->DesignStudio.media(a,true,true));
        p.row("photo",s(R.string.saver_image_path),s(R.string.saver_path_hint),()->input(false));
        p.row("video",s(R.string.saver_video_path),s(R.string.live_custom_hint),()->input(true));
        p.row("video",s(R.string.saver_copy_home),s(R.string.saver_copy_hint),()->{String url=LiveWallpaperPrefs.source(a);if(!LiveWallpaperSource.valid(url)){Toast.makeText(a,R.string.saver_no_video,Toast.LENGTH_LONG).show();return;}SaverPrefs.video(a,url,LiveWallpaperPrefs.title(a));});
        GridSlider dim=new GridSlider(a,s(R.string.saver_dim),0,90,LiveWallpaperSource.dim(prefs.getInt(SaverPrefs.DIM,45)),n->prefs.edit().putInt(SaverPrefs.DIM,n).apply());p.add(dim);
        int[] intervals={0,1,2,5,10,15,30,60,120};p.options("timer",s(R.string.saver_photo_change),java.util.Arrays.stream(intervals).mapToObj(n->n==0?"Aus":n+" min").toArray(String[]::new),i->prefs.getInt(SaverPrefs.MINUTES,5)==intervals[i],i->prefs.edit().putInt(SaverPrefs.MINUTES,intervals[i]).apply());
        LinearLayout shuffle=p.toggle(s(R.string.saver_shuffle),null,prefs.getBoolean(SaverPrefs.RANDOM,false),on->prefs.edit().putBoolean(SaverPrefs.RANDOM,on).apply());
        p.row("move",s(R.string.saver_next_photo),null,()->{if(photos.isEmpty())return;SaverCatalog.Photo current=SaverCatalog.find(photos,prefs.getString(SaverPrefs.PHOTO,"w00.jpg"));int at=photos.indexOf(current);SaverPrefs.photo(a,photos.get(SaverPolicy.next(photos.size(),at,prefs.getBoolean(SaverPrefs.RANDOM,false),new java.util.Random())).file);});
        p.row("move",s(R.string.saver_reload),null,()->{if(a instanceof ClockActivity)((ClockActivity)a).reloadBackground();});
        p.row("photo",s(R.string.saver_credits),null,this::credits);p.info(s(R.string.saver_protection));p.row("settings",s(R.string.saver_reset),s(R.string.saver_reset_hint),()->prefs.edit().clear().apply());
        android.content.SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{p.refreshChoices();dim.syncValue(LiveWallpaperSource.dim(prefs.getInt(SaverPrefs.DIM,45)));SettingsSheet.syncToggle(shuffle,prefs.getBoolean(SaverPrefs.RANDOM,false));};
        android.content.SharedPreferences fonts=a.getSharedPreferences(Config.SETTINGS_NAME,0);
        android.content.SharedPreferences.OnSharedPreferenceChangeListener fontUpdate=(sp,key)->p.refreshChoices();
        prefs.registerOnSharedPreferenceChangeListener(update);fonts.registerOnSharedPreferenceChangeListener(fontUpdate);p.dialog.setOnDismissListener(d->{prefs.unregisterOnSharedPreferenceChangeListener(update);fonts.unregisterOnSharedPreferenceChangeListener(fontUpdate);if(a instanceof ClockActivity&&!a.isFinishing())a.finish();});p.show();
    }
    private void modes(){SettingsSheet p=new SettingsSheet(a,s(R.string.saver_background));for(String id:SaverPolicy.MODES)p.choice("photo",modeName(id),modeHint(id),()->id.equals(SaverPrefs.mode(a)),()->{boolean same=id.equals(SaverPrefs.mode(a));prefs.edit().putString(SaverPrefs.MODE,id).apply();if(same&&a instanceof ClockActivity)((ClockActivity)a).reloadBackground();});p.info(s(R.string.saver_mode_hint));p.show();}
    private void intervals(){SettingsSheet p=new SettingsSheet(a,s(R.string.saver_photo_change));for(int n:new int[]{0,1,2,5,10,15,30,60,120})p.choice("timer",n==0?s(R.string.saver_fixed_photo):n+" min",null,()->prefs.getInt(SaverPrefs.MINUTES,5)==n,()->prefs.edit().putInt(SaverPrefs.MINUTES,n).apply());p.show();}
    private void photos(){SettingsSheet p=new SettingsSheet(a,s(R.string.saver_choose_photo));
        SaverPhotoPreview preview=new SaverPhotoPreview(a);p.preview(preview,112);
        for(SaverCatalog.Photo photo:photos){LinearLayout row=p.choice("photo",photo.title,photo.region,()->photo.file.equals(prefs.getString(SaverPrefs.PHOTO,"w00.jpg")),()->SaverPrefs.photo(a,photo.file));ImageView image=(ImageView)row.getChildAt(0);Bitmap thumb=SaverCatalog.thumb(a,photo.file);if(thumb!=null){image.setImageTintList(null);image.setImageBitmap(thumb);image.setScaleType(ImageView.ScaleType.CENTER_CROP);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(SettingsSheet.dp(a,72),SettingsSheet.dp(a,41));lp.setMarginEnd(SettingsSheet.dp(a,12));image.setLayoutParams(lp);}}
        for(int i=0;i<p.body.getChildCount()&&i<photos.size();i++){SaverCatalog.Photo photo=photos.get(i);p.body.getChildAt(i).setOnFocusChangeListener((v,on)->{if(on)preview.show(photo);});}
        android.content.SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->p.refreshChoices();prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->{prefs.unregisterOnSharedPreferenceChangeListener(update);preview.close();});p.info(s(R.string.saver_gallery_hint));p.show();
    }
    private void videoGroups(){SettingsSheet p=new SettingsSheet(a,s(R.string.saver_choose_video));
        for(String group:new String[]{"favorites","all","A","Z","C"}){List<LiveWallpaperCatalog.Item> list=LiveWallpaperSelection.pool(a,group);
            p.row("video",LiveWallpaperSelection.name(a,group)+" ("+list.size()+")",null,()->SaverVideos.show(a,LiveWallpaperSelection.name(a,group),list));}p.info(s(R.string.saver_video_hint));p.show();}
    private void credits(){SettingsSheet p=new SettingsSheet(a,s(R.string.saver_credits));p.info(s(R.string.saver_aura_credit));p.row("photo",s(R.string.saver_aura_sources),null,this::auraCredits);
        for(SaverCatalog.Photo photo:photos)p.row("photo",photo.title,photo.author+" · "+photo.license,()->{SettingsSheet detail=new SettingsSheet(a,photo.title);detail.info(photo.caption());detail.row("photo",s(R.string.saver_original_source),photo.source,()->web(photo.source));detail.row("photo",s(R.string.saver_license),photo.licenseUrl,()->web(photo.licenseUrl));detail.show();});p.show();}
    private void web(String url){try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(a,R.string.not_available,Toast.LENGTH_LONG).show();}}
    private void auraCredits(){SettingsSheet p=new SettingsSheet(a,s(R.string.saver_aura_sources));p.info(s(R.string.saver_aura_sources_info));p.row("photo","Aura TV · MIT",null,()->web("https://github.com/cookedzera/aura-tv"));p.row("photo","Open-Meteo · CC BY 4.0",null,()->web("https://open-meteo.com/"));p.row("photo","Art Institute of Chicago · Open Access",null,()->web("https://www.artic.edu/open-access/open-access-policy"));p.row("photo","Sternenkatalog · d3-celestial · BSD-3-Clause",null,()->web("https://github.com/ofrohn/d3-celestial/blob/master/LICENSE"));p.info(s(R.string.saver_aura_location));p.show();}
    private void input(boolean video){
        LinearLayout panel=SettingsSheet.panel(a);int padding=SettingsSheet.dp(a,18);panel.setPadding(padding,padding,padding,padding);panel.addView(GlassDialog.text(a,s(video?R.string.saver_video_path:R.string.saver_image_path),18));
        EditText edit=new EditText(a);edit.setSingleLine(true);edit.setTextColor(0xFFFFFFFF);edit.setTextSize(13);edit.setSelectAllOnFocus(true);edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);edit.setBackgroundResource(R.drawable.weather_field);edit.setText(prefs.getString(video?SaverPrefs.VIDEO:SaverPrefs.IMAGE,""));panel.addView(edit,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,48)));
        TextView error=GlassDialog.text(a,"",11);error.setTextColor(0xFFFFB59F);panel.addView(error);TextView use=DesignPopup.button(a,s(R.string.live_use));panel.addView(use,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,36)));Dialog dialog=GlassDialog.show(a,panel,420,185);SettingsSheet.track(a,dialog);if(dialog.getWindow()!=null)dialog.getWindow().setDimAmount(.12f);
        use.setOnClickListener(v->{String source=edit.getText().toString().trim();if(!LiveWallpaperSource.valid(source)){error.setText(R.string.live_invalid);return;}if(video)SaverPrefs.video(a,source,s(R.string.saver_own_video));else SaverPrefs.image(a,source);error.setText("");use.requestFocus();android.view.inputmethod.InputMethodManager ime=a.getSystemService(android.view.inputmethod.InputMethodManager.class);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);});
        dialog.setOnDismissListener(d->{android.view.inputmethod.InputMethodManager ime=a.getSystemService(android.view.inputmethod.InputMethodManager.class);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);});use.requestFocus();
    }
}
