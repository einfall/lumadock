package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
/** Real installed app-icon samples; reading a large mapping never blocks remote navigation. */
final class IconPackPicker {
    static void show(Activity a,Runnable changed){new IconPackPicker(a,changed);}
    private final Activity a;private final Runnable changed;private final SettingsSheet sheet;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final List<ResolveInfo> samples=new ArrayList<>();
    private final ImageView[] images=new ImageView[4];private final TextView caption;private boolean closed;private int generation;
    private Future<?> previewTask;
    private IconPackPicker(Activity a,Runnable changed){
        this.a=a;this.changed=changed;sheet=new SettingsSheet(a,a.getString(R.string.iconpacks_title));
        AppAppearance apps=new AppAppearance(a,Config.load(a));for(AppAppearance.Item item:apps.items)if(!apps.hidden.contains(item.pkg)&&samples.size()<4)samples.add(item.info);
        LinearLayout preview=new LinearLayout(a);preview.setOrientation(LinearLayout.VERTICAL);LinearLayout strip=new LinearLayout(a);strip.setGravity(Gravity.CENTER);
        for(int i=0;i<4;i++){images[i]=new ImageView(a);images[i].setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,SettingsSheet.dp(a,44),1);lp.setMargins(SettingsSheet.dp(a,6),SettingsSheet.dp(a,6),SettingsSheet.dp(a,6),SettingsSheet.dp(a,6));strip.addView(images[i],lp);}preview.addView(strip);caption=GlassDialog.text(a,"",10);caption.setGravity(Gravity.CENTER);preview.addView(caption);sheet.preview(preview,86);
        rows();sheet.dialog.setOnDismissListener(d->{closed=true;generation++;worker.shutdownNow();});sheet.show();
    }
    private void rows(){sheet.clearRows();
        LinearLayout original=sheet.choice("grid",a.getString(R.string.iconpacks_original),null,()->IconPacks.selected(a).isEmpty(),()->select(""));original.setOnFocusChangeListener((v,on)->{if(on)preview("");});
        List<IconPacks.Info> packs=IconPacks.installed(a);
        for(IconPacks.Info info:packs){LinearLayout row=sheet.choice("grid",info.label,null,()->info.pkg.equals(IconPacks.selected(a)),()->select(info.pkg));row.setOnFocusChangeListener((v,on)->{if(on)preview(info.pkg);});}
        if(packs.isEmpty())sheet.info(a.getString(R.string.iconpacks_empty));
        sheet.row("grid",a.getString(R.string.studio_install_packs),a.getString(R.string.studio_install_packs_hint),()->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("market://search?q=icon%20pack&c=apps")));}catch(ActivityNotFoundException e){try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://play.google.com/store/search?q=icon%20pack&c=apps")));}catch(ActivityNotFoundException ignored){Toast.makeText(a,R.string.not_available,Toast.LENGTH_LONG).show();}}});
        sheet.row("video","Icon-Packs herunterladen","Projectivy · Minimalist · Glitch Reflections TV",()->IconPackDownloads.show(a));
        sheet.row("move",a.getString(R.string.studio_reload_packs),null,()->{rows();if(sheet.body.getChildCount()>0)sheet.body.getChildAt(0).requestFocus();});sheet.info(a.getString(R.string.iconpacks_behavior));
    }
    private void preview(String pkg){
        if(closed)return;
        int token=++generation;caption.setText(R.string.iconpacks_loading);
        if(previewTask!=null)previewTask.cancel(true);
        previewTask=worker.submit(()->{IconPacks.Pack pack=IconPacks.load(a,pkg);Drawable[] drawn=new Drawable[samples.size()];for(int i=0;i<drawn.length;i++){ResolveInfo r=samples.get(i);drawn[i]=pack==null?null:pack.icon(new ComponentName(r.activityInfo.packageName,r.activityInfo.name));if(drawn[i]==null)drawn[i]=r.loadIcon(a.getPackageManager());}
            a.runOnUiThread(()->{if(closed||token!=generation)return;for(int i=0;i<images.length;i++)images[i].setImageDrawable(i<drawn.length?drawn[i]:null);caption.setText(pkg.isEmpty()?a.getString(R.string.iconpacks_original):pack==null?a.getString(R.string.iconpacks_error):a.getString(R.string.iconpacks_count,pack.items.size()));});});
    }
    private void select(String pkg){
        if(closed)return;
        worker.execute(()->{IconPacks.Pack pack=IconPacks.load(a,pkg);a.runOnUiThread(()->{if(closed)return;if(!pkg.isEmpty()&&pack==null){Toast.makeText(a,R.string.iconpacks_error,Toast.LENGTH_LONG).show();return;}android.content.SharedPreferences p=a.getSharedPreferences(Config.SETTINGS_NAME,0);p.edit().putString(IconPacks.KEY,pkg).putLong(Config.SETTINGS_REVISION_KEY,p.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();changed.run();sheet.refreshChoices();});});
    }
}
