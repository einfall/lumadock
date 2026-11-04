package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * The clock screensaver shown by Home itself after home_idle_clock_minutes without input. Some TVs' own
 * screens (CVTE's live TV) do not keep the screen on, so the system screensaver would cut into programmes;
 * this one only ever starts from Home. Any key returns to Home.
 */
public class ClockActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }


    private ClockScene scene;private SaverBackground background;private boolean listening;
    private final android.content.BroadcastReceiver screen=new android.content.BroadcastReceiver(){public void onReceive(android.content.Context c,android.content.Intent i){if(background!=null)background.stop();if(scene!=null)scene.stop();finish();}};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dream_clock);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
        scene = new ClockScene(getWindow().getDecorView());
        scene.preview(getIntent().getBooleanExtra("saver_settings",false)||getIntent().getBooleanExtra("visual_studio",false));scene.useHomeWallpaper(this);background=new SaverBackground(this,getWindow().getDecorView(),scene);
        android.content.IntentFilter filter=new android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF);if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(screen,filter,android.content.Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,filter);listening=true;
        if(getIntent().getBooleanExtra("visual_studio",false))getWindow().getDecorView().post(()->{if(!isFinishing())DesignStudio.showSaver(this);});
        if(getIntent().getBooleanExtra("saver_settings",false))getWindow().getDecorView().post(()->{if(!isFinishing()){SaverSettings.show(this);if(getIntent().getBooleanExtra("saver_font",false))TypographyPopup.choose(this,Typography.SAVER_CLOCK,getString(R.string.typography_saver),()->{});}});
    }

    @Override
    protected void onResume() {
        super.onResume();
        background.start();scene.start();
    }

    @Override
    protected void onPause() {
        background.stop();scene.stop();SettingsSheet.dismissAll(this);
        super.onPause();
        finish();   // never left behind in the back stack
    }

    void reloadBackground(){if(background!=null)background.reload();}
    void nextAutoScene(){if(background!=null)background.nextAuto();}
    @Override protected void onDestroy(){SettingsSheet.dismissAll(this);if(background!=null)background.close();if(listening)unregisterReceiver(screen);super.onDestroy();}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // The key that wakes the clock should not also act on Home, so it is consumed here.
        if (event.getAction() == KeyEvent.ACTION_UP) finish();
        return true;
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        finish();
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
