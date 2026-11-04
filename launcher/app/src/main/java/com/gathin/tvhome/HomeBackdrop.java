package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
/** Optional static backgrounds. Decodes one sampled photo only while Home is visible. */
final class HomeBackdrop {
    static final String MODE="studio_bg_mode",ID="studio_bg_id";
    static final String[] NAMES={"Abendlicht","Lagune","Morgenrot","Nordlicht","Graphit","Sand"};
    static final int[][] COLORS={{0xFF16104C,0xFF9479AC,0xFFE8D4CF},{0xFF063E56,0xFF44A9B0,0xFFC9E2CC},{0xFF694C7A,0xFFF4B9BC,0xFFF1D88F},{0xFF091E43,0xFF3942A1,0xFF7ACAC2},{0xFF080B10,0xFF303740,0xFF67717B},{0xFF2B2428,0xFF947B67,0xFFD6C0A1}};
    private final Activity a;private final ImageView view;private final SharedPreferences prefs,live;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private Future<?> task;
    private boolean active,closed;private int generation;private String opened="";
    private final SharedPreferences.OnSharedPreferenceChangeListener change=(p,k)->{if(k!=null&&(k.startsWith("studio_bg_")||k.equals("enabled")))refresh();};
    HomeBackdrop(Activity a,FrameLayout root){this.a=a;prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);live=LiveWallpaperPrefs.prefs(a);view=new ImageView(a);view.setScaleType(ImageView.ScaleType.CENTER_CROP);view.setVisibility(View.GONE);view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);int at=root.indexOfChild(root.findViewById(R.id.ambient));root.addView(view,Math.max(0,at),new FrameLayout.LayoutParams(-1,-1));prefs.registerOnSharedPreferenceChangeListener(change);live.registerOnSharedPreferenceChangeListener(change);}
    static GradientDrawable gradient(int i){return new GradientDrawable(GradientDrawable.Orientation.TL_BR,COLORS[Math.max(0,Math.min(COLORS.length-1,i))]);}
    static void select(Activity a,String mode,String id){BackgroundSelection.mode(a,"fixed");apply(a,mode,id);}
    static void apply(Context a,String mode,String id){LiveWallpaperPrefs.clock(a);a.getSharedPreferences(Config.SETTINGS_NAME,0).edit().putString(MODE,mode).putString(ID,id).apply();LiveWallpaperPrefs.prefs(a).edit().putBoolean("enabled",false).apply();}
    void start(){active=true;refresh();}void stop(){active=false;refresh();}
    private void refresh(){if(closed)return;String mode=prefs.getString(MODE,"original"),id=prefs.getString(ID,"0");
        if(!active||live.getBoolean("enabled",false)||"original".equals(mode)){generation++;if(task!=null)task.cancel(true);view.setVisibility(View.GONE);view.setImageDrawable(null);opened="";return;}
        String key=mode+id;if(key.equals(opened))return;opened=key;int token=++generation;if(task!=null)task.cancel(true);
        if("gradient".equals(mode)){int index;try{index=Integer.parseInt(id);}catch(NumberFormatException e){index=0;}view.setImageDrawable(gradient(index));view.setVisibility(View.VISIBLE);}
        else if("photo".equals(mode)&&SaverPolicy.photoId(id)){task=worker.submit(()->{try{Bitmap b=SaverImage.load(a,id,true,1920,1080);a.runOnUiThread(()->{if(closed||token!=generation){if(b!=null)b.recycle();return;}view.setImageBitmap(b);view.setVisibility(View.VISIBLE);});}catch(Exception ignored){a.runOnUiThread(()->{if(token==generation)view.setVisibility(View.GONE);});}});}
        else view.setVisibility(View.GONE);
    }
    void close(){closed=true;generation++;prefs.unregisterOnSharedPreferenceChangeListener(change);live.unregisterOnSharedPreferenceChangeListener(change);worker.shutdownNow();view.setImageDrawable(null);}
}
