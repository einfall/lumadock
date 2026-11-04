package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.ImageView;
/** Actual Activity preview, refreshed on settings changes; attachment owns all listeners and callbacks. */
final class StudioPreview extends ImageView {
 private final Activity a;private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final SharedPreferences[] prefs;private boolean attached,busy;
 private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,k)->refresh();
 private final Runnable capture=this::copy,late=this::copy,video=this::copy;
 StudioPreview(Activity a){super(a);this.a=a;setScaleType(ScaleType.FIT_CENTER);setContentDescription("Live-Vorschau der tatsächlichen Ansicht");prefs=new SharedPreferences[]{a.getSharedPreferences(Config.SETTINGS_NAME,0),LiveWallpaperPrefs.prefs(a),SaverPrefs.get(a),a.getSharedPreferences(WeatherPanel.PREFS,0)};}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;for(SharedPreferences p:prefs)p.registerOnSharedPreferenceChangeListener(listener);refresh();}
 @Override protected void onDetachedFromWindow(){attached=false;for(SharedPreferences p:prefs)p.unregisterOnSharedPreferenceChangeListener(listener);handler.removeCallbacksAndMessages(null);setImageDrawable(null);super.onDetachedFromWindow();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE&&handler!=null)refresh();}
 void refresh(){if(!attached)return;handler.removeCallbacksAndMessages(null);handler.postDelayed(capture,160);handler.postDelayed(late,1400);handler.postDelayed(video,6500);}
 private void copy(){if(!attached||busy||!isShown()||a.isFinishing())return;View source=a.getWindow().getDecorView();if(source.getWidth()<1||source.getHeight()<1)return;busy=true;Bitmap b=Bitmap.createBitmap(640,360,Bitmap.Config.ARGB_8888);try{PixelCopy.request(a.getWindow(),b,result->{busy=false;if(!attached){b.recycle();return;}if(result!=PixelCopy.SUCCESS){Canvas c=new Canvas(b);c.scale(640f/source.getWidth(),360f/source.getHeight());source.draw(c);}setImageBitmap(b);},handler);}catch(IllegalArgumentException e){busy=false;b.recycle();}}
}
