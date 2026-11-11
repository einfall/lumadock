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
import android.content.SharedPreferences;
import android.os.PowerManager;
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

/** Read-only, event-driven volume overlay for HDMI-CEC or a selected local Sonos device. */
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
    private int session;
    private boolean sonos;
    private SonosLan.Player player;
    private SonosLan.Player watchedPlayer;
    private SonosVolumeEvents sonosEvents;
    private boolean screenOn;
    private final SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferencesChanged = (prefs, key) -> {
        if (KEY.equals(key) || SonosSettings.sourceSetting(key)) handler.post(this::updateSonosEvents);
    };
    private final Runnable read = this::readStatus;
    private final Runnable hide = this::hide;
    private final BroadcastReceiver volumeChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()) || Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                screenOn = Intent.ACTION_SCREEN_ON.equals(intent.getAction());
                if (!screenOn) removeNow();
                updateSonosEvents();
                return;
            }
            if (!preferences.getBoolean(KEY, false)) return;
            if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", 3) != 3) return;
            trigger();
        }
    };

    CecVolumeOverlay(AccessibilityService service) {
        this.service = service;
        windows = service.getSystemService(WindowManager.class);
        preferences = service.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        PowerManager power = service.getSystemService(PowerManager.class);
        screenOn = power == null || power.isInteractive();
        IntentFilter changes = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        changes.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
        changes.addAction("android.media.MASTER_MUTE_CHANGED_ACTION");
        changes.addAction(Intent.ACTION_SCREEN_ON);
        changes.addAction(Intent.ACTION_SCREEN_OFF);
        service.registerReceiver(volumeChanged, changes);
        preferences.registerOnSharedPreferenceChangeListener(preferencesChanged);
        updateSonosEvents();
    }

    private void updateSonosEvents() {
        SonosLan.Player selected = !closed && screenOn && preferences.getBoolean(KEY, false) && SonosSettings.enabled(service)
                ? SonosSettings.player(service) : null;
        if (samePlayer(watchedPlayer, selected)) return;
        if (sonosEvents != null) sonosEvents.close();
        watchedPlayer = selected;
        sonosEvents = selected == null ? null : new SonosVolumeEvents(selected, level -> handler.post(() -> {
            if (closed || watchedPlayer != selected) return;
            trigger(false);
            if (frame != null && sonos && samePlayer(player, selected)) frame.state(level.volume, level.muted);
        }));
    }

    void trigger() { trigger(true); }

    private void trigger(boolean refresh) {
        if (closed) return;
        VolumeStyle selected=VolumeStyle.parse(service.getSharedPreferences(Config.SETTINGS_NAME,0).getString(VolumeStyle.KEY,"glass"));
        boolean useSonos = SonosSettings.enabled(service);
        SonosLan.Player selectedPlayer = useSonos ? SonosSettings.player(service) : null;
        if (panel != null && (style != selected || sonos != useSonos || !samePlayer(player, selectedPlayer))) removeNow();
        sonos = useSonos;
        player = selectedPlayer;
        if(hiding&&panel!=null){appearance++;hiding=false;panel.animate().cancel();panel.setAlpha(1f);
            panel.setTranslationX(0);panel.setTranslationY(0);panel.setScaleX(1f);panel.setScaleY(1f);}
        if (panel == null) {
            style=selected;
            since = System.currentTimeMillis();
            session++;
            show();
        }
        until = SystemClock.uptimeMillis() + 1800;
        handler.removeCallbacks(hide);
        handler.postDelayed(hide, 2600);
        if (sonos && player == null && frame != null) {
            frame.message(R.string.sonos_volume_no_device);
        } else if (refresh && !reading) {
            handler.removeCallbacks(read);
            handler.postDelayed(read, 140);
        }
    }

    private static boolean samePlayer(SonosLan.Player first, SonosLan.Player second) {
        if (first == null || second == null) return first == second;
        return first.id.equals(second.id) && first.ip.equals(second.ip) && first.label.equals(second.label);
    }

    private void show() {
        panel=new android.widget.FrameLayout(service);
        frame = sonos ? new VolumeOsdView(service, style, player == null ? "Sonos" : player.label, "Sonos")
                : new VolumeOsdView(service, style);
        if (sonos && player != null) frame.message(R.string.sonos_volume_waiting);
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
        if (closed || panel == null || reading || (sonos && player == null)) return;
        if (sonos) {
            readSonos();
            return;
        }
        reading = true;
        final long requestSince = since;
        final int requestSession = session;
        worker.execute(() -> {
            CecAudioStatus status = null;
            boolean authorizationNeeded = false;
            try {
                String dump = LocalCecReader.read(() -> handler.post(() -> {
                    if (!closed && panel != null && requestSession == session) frame.message(R.string.cec_volume_authorize);
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
                if (requestSession != session) { handler.post(read); return; }
                if(result!=null)frame.state(result.volume,result.muted);
                else frame.message(needsAuthorization?R.string.cec_volume_authorize:SystemClock.uptimeMillis()>=until?R.string.cec_volume_unavailable:R.string.cec_volume_waiting);
                if (!needsAuthorization && SystemClock.uptimeMillis() < until) handler.postDelayed(read, 250);
            });
        });
    }

    private void readSonos() {
        reading = true;
        SonosLan.Player selected = player;
        int requestSession = session;
        worker.execute(() -> {
            SonosLan.Level reading;
            try { reading = SonosLan.read(selected); }
            catch (Exception unavailable) { reading = null; }
            SonosLan.Level level = reading;
            handler.post(() -> {
                this.reading = false;
                if (closed || panel == null) return;
                if (requestSession != session) { handler.post(read); return; }
                if (level != null) frame.state(level.volume, level.muted);
                else frame.message(SystemClock.uptimeMillis() < until ? R.string.sonos_volume_waiting : R.string.sonos_volume_unavailable);
                if (SystemClock.uptimeMillis() < until) handler.postDelayed(read, 450);
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
        closed = true;
        preferences.unregisterOnSharedPreferenceChangeListener(preferencesChanged);
        updateSonosEvents();
        service.unregisterReceiver(volumeChanged);
        handler.removeCallbacksAndMessages(null); removeNow(); worker.shutdownNow();
    }
    private int dp(int value) { return Math.round(value * service.getResources().getDisplayMetrics().density); }
}
