package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Read-only, event-driven CEC overlay. System audio broadcasts trigger the read; no key filtering or CEC sending. */
final class CecVolumeOverlay {
    static final String KEY = "cec_volume_overlay";
    private final AccessibilityService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final WindowManager windows;
    private android.widget.FrameLayout panel;
    private VolumeOsdView frame;
    private VolumeStyle style;
    private int appearance;
    private boolean hiding;
    private boolean reading, closed;
    private long until, since;
    private final Runnable read = this::readStatus;
    private final Runnable hide = this::hide;
    private final BroadcastReceiver volumeChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!service.getSharedPreferences(Config.SETTINGS_NAME, 0).getBoolean(KEY, false)) return;
            if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", 3) != 3) return;
            trigger();
        }
    };

    CecVolumeOverlay(AccessibilityService service) {
        this.service = service;
        windows = service.getSystemService(WindowManager.class);
        IntentFilter changes = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        changes.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
        changes.addAction("android.media.MASTER_MUTE_CHANGED_ACTION");
        service.registerReceiver(volumeChanged, changes);
    }

    void trigger() {
        if (closed) return;
        VolumeStyle selected=VolumeStyle.parse(service.getSharedPreferences(Config.SETTINGS_NAME,0).getString(VolumeStyle.KEY,"glass"));
        if(panel!=null&&style!=selected)removeNow();
        if(hiding&&panel!=null){appearance++;hiding=false;panel.animate().cancel();panel.setAlpha(1f);
            panel.setTranslationX(0);panel.setTranslationY(0);panel.setScaleX(1f);panel.setScaleY(1f);}
        if (panel == null) {
            style=selected;
            since = System.currentTimeMillis();
            show();
        }
        until = SystemClock.uptimeMillis() + 1800;
        handler.removeCallbacks(hide);
        handler.postDelayed(hide, 2600);
        if (!reading) {
            handler.removeCallbacks(read);
            handler.postDelayed(read, 140);
        }
    }

    private void show() {
        panel=new android.widget.FrameLayout(service);
        frame=new VolumeOsdView(service,style);
        panel.addView(frame,new android.widget.FrameLayout.LayoutParams(-1,-1));
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(dp(style.width),dp(style.height),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                android.graphics.PixelFormat.TRANSLUCENT);
        lp.gravity=VolumeStylePicker.gravity(style);
        lp.x=style==VolumeStyle.LG||style==VolumeStyle.SAMSUNG?dp(28):style==VolumeStyle.PHILIPS?dp(40):0;
        lp.y=style==VolumeStyle.LG||style==VolumeStyle.SAMSUNG?0:dp(38);
        try { windows.addView(panel,lp); }
        catch(RuntimeException e){android.util.Log.w("CecVolume","Overlay could not attach",e);panel=null;return;}
        appearance++;hiding=false;panel.setAlpha(0f);
        if(style==VolumeStyle.SAMSUNG)panel.setTranslationX(-dp(40));
        else if(style==VolumeStyle.PHILIPS)panel.setTranslationX(-dp(28));
        else if(style==VolumeStyle.LG){panel.setScaleX(.72f);panel.setScaleY(.72f);}
        else panel.setTranslationY(dp(style==VolumeStyle.SONY?12:18));
        panel.animate().alpha(1f).translationX(0).translationY(0).scaleX(1f).scaleY(1f)
                .setDuration(style==VolumeStyle.LG?340:220)
                .setInterpolator(style==VolumeStyle.LG?new android.view.animation.OvershootInterpolator(1.1f)
                        :new android.view.animation.DecelerateInterpolator()).start();
    }

    private void readStatus() {
        if (closed || panel == null || reading) return;
        reading = true;
        final long requestSince = since;
        worker.execute(() -> {
            CecAudioStatus status = null;
            boolean authorizationNeeded = false;
            try {
                String dump = LocalCecReader.read(() -> handler.post(() -> {
                    if (!closed && panel != null) frame.message(R.string.cec_volume_authorize);
                }));
                status = CecAudioStatus.latest(dump, System.currentTimeMillis(), requestSince, ZoneId.systemDefault());
            } catch (LocalCecReader.AuthorizationNeeded e) { authorizationNeeded = true; }
              catch (Exception e) { android.util.Log.d("CecVolume", "Local CEC read failed: " + e.getClass().getSimpleName()); }
            final boolean needsAuthorization = authorizationNeeded;
            final CecAudioStatus result = status;
            handler.post(() -> {
                reading = false;
                if (closed || panel == null) return;
                // A new display session cannot reuse a response from the previous one.
                if (requestSince != since) { handler.post(read); return; }
                if(result!=null)frame.state(result.volume,result.muted);
                else frame.message(needsAuthorization?R.string.cec_volume_authorize:R.string.cec_volume_waiting);
                if (!needsAuthorization && SystemClock.uptimeMillis() < until) handler.postDelayed(read, 250);
            });
        });
    }

    private void hide() {
        handler.removeCallbacks(read);
        if(panel==null)return;
        final android.widget.FrameLayout leaving=panel;
        final int token=++appearance;hiding=true;
        leaving.animate().cancel();
        android.view.ViewPropertyAnimator exit=leaving.animate().alpha(0f).setDuration(170)
                .setInterpolator(new android.view.animation.DecelerateInterpolator());
        if(style==VolumeStyle.SAMSUNG)exit.translationX(-dp(28));
        else if(style==VolumeStyle.PHILIPS)exit.translationX(-dp(20));
        else if(style==VolumeStyle.LG)exit.scaleX(.86f).scaleY(.86f);
        else exit.translationY(dp(10));
        exit.withEndAction(()->{if(panel==leaving&&appearance==token&&hiding)removeNow();}).start();
    }

    private void removeNow(){
        appearance++;hiding=false;
        if(panel!=null){panel.animate().cancel();if(frame!=null)frame.stop();
            try{windows.removeView(panel);}catch(RuntimeException ignored){}panel=null;frame=null;}
    }

    void close() {
        closed = true; service.unregisterReceiver(volumeChanged);
        handler.removeCallbacksAndMessages(null); removeNow(); worker.shutdownNow();
    }
    private int dp(int value) { return Math.round(value * service.getResources().getDisplayMetrics().density); }
}
