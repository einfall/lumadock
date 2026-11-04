package com.gathin.tvhome;

import android.service.dreams.DreamService;

/**
 * Screensaver: the moving clock over the selected saver background.
 * SaverBackground owns optional photos/video only while the dream is active.
 */
public class ClockDream extends DreamService {

    private ClockScene scene;private SaverBackground background;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream_clock);
        scene = new ClockScene(getWindow().getDecorView());
        scene.useHomeWallpaper(this);background=new SaverBackground(this,getWindow().getDecorView(),scene);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        background.start();scene.start();
    }

    @Override
    public void onDreamingStopped() {
        background.stop();scene.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if(background!=null)background.close();if (scene != null) scene.stop();
        super.onDetachedFromWindow();
    }
}
