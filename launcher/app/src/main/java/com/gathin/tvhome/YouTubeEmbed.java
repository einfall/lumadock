package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.webkit.ScriptHandler;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

/** Library-backed iframe playback. Luma owns preview controls; YouTube controls stay hidden. */
final class YouTubeEmbed {
    interface Listener {
        void update(int state, double position, double duration);
        void error(String message);
    }

    final YouTubePlayerView view;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private YouTubePlayer player;
    private ScriptHandler wallpaperUi;
    private boolean closed, failed;
    private int state = -1;
    private float position, duration;
    private final Runnable timeout = () -> fail("YouTube lädt zu lange. Internet und Android System WebView prüfen.");

    YouTubeEmbed(Context context, String id, boolean preview, boolean loop, Listener listener) {
        if (!YouTubeVideo.validId(id)) throw new IllegalArgumentException("Invalid video");
        this.listener = listener;
        // Disable automatic initialization before construction to avoid a second network observer
        // racing the explicitly configured controls(0) player.
        view = (YouTubePlayerView) LayoutInflater.from(context).inflate(R.layout.youtube_player, null);
        view.setBackgroundColor(Color.BLACK);
        view.setFocusable(false);
        view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        IFramePlayerOptions options = new IFramePlayerOptions.Builder(context)
                .controls(0).mute(1).rel(0).ivLoadPolicy(3).build();
        main.postDelayed(timeout, 30000);
        try {
            if (!preview) wallpaperUi = YouTubeWallpaperUi.install(view);
            view.initialize(new AbstractYouTubePlayerListener() {
                @Override public void onReady(YouTubePlayer ready) {
                    if (closed) return;
                    player = ready;
                    ready.mute();
                    ready.loadVideo(id, 0);
                }
                @Override public void onStateChange(YouTubePlayer source, PlayerConstants.PlayerState value) {
                    if (closed) return;
                    switch (value) {
                        case PLAYING: state = 1; failed = false; break;
                        case PAUSED: state = 2; break;
                        case ENDED: state = 0; break;
                        case BUFFERING: state = 3; break;
                        case VIDEO_CUED: state = 5; break;
                        default: state = -1;
                    }
                    if (state == 1 || state == 2 || state == 0) main.removeCallbacks(timeout);
                    report();
                    if (state == 0 && loop && !closed && !failed) {
                        position = 0;
                        // Reuse the loaded video rather than triggering another video load/start UI.
                        source.seekTo(0);
                        source.play();
                        Log.d("TvHome.YouTube", "Loop: seek to start");
                    }
                }
                @Override public void onCurrentSecond(YouTubePlayer source, float second) {
                    if (!Float.isFinite(second) || second < 0 || closed) return;
                    position = second;
                    report();
                }
                @Override public void onVideoDuration(YouTubePlayer source, float seconds) {
                    if (!Float.isFinite(seconds) || seconds < 0 || closed) return;
                    duration = seconds;
                    report();
                }
                @Override public void onError(YouTubePlayer source, PlayerConstants.PlayerError error) {
                    String message;
                    switch (error) {
                        case VIDEO_NOT_FOUND: message = "Das Video ist nicht mehr verfügbar oder privat."; break;
                        case VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER:
                            message = "Dieses Video erlaubt keine eingebettete Wiedergabe."; break;
                        case REQUEST_MISSING_HTTP_REFERER:
                            message = "YouTube hat die Player-Identifikation abgelehnt. Android System WebView aktualisieren."; break;
                        default: message = "YouTube kann das Video auf diesem Gerät nicht abspielen (" + error.name() + ").";
                    }
                    fail(message);
                }
            }, options);
        } catch (RuntimeException error) {
            close();
            throw error;
        }
    }
    private void report() {
        if (!closed && !failed) listener.update(state, position, duration);
    }
    void toggle() {
        if (closed || player == null) return;
        if (state == 1) player.pause(); else player.play();
    }
    void seek(int seconds) {
        if (closed || player == null) return;
        float target = Math.max(0, position + seconds);
        if (duration > 0) target = Math.min(Math.max(0, duration - .2f), target);
        player.seekTo(target);
    }
    void sound(boolean enabled) {
        if (closed || player == null) return;
        if (enabled) player.unMute(); else player.mute();
    }
    private void fail(String message) {
        main.removeCallbacks(timeout);
        if (closed || failed) return;
        failed = true;
        listener.error(message);
    }
    void close() {
        if (closed) return;
        closed = true;
        main.removeCallbacksAndMessages(null);
        player = null;
        if (wallpaperUi != null) {
            try { wallpaperUi.remove(); } catch (RuntimeException ignored) { /* WebView may already be gone. */ }
            wallpaperUi = null;
        }
        view.release();
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
    }
}
