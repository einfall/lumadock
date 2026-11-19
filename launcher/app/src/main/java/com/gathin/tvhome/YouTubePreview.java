package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** The preview owns its decoder, preserves the search list and releases playback on dismissal. */
final class YouTubePreview {
    private static final java.util.Map<Activity, java.lang.ref.WeakReference<Dialog>> OPEN = new java.util.WeakHashMap<>();
    static void close(Activity activity) {
        java.lang.ref.WeakReference<Dialog> ref = OPEN.remove(activity);
        Dialog dialog = ref == null ? null : ref.get();
        if (dialog != null) dialog.dismiss();
    }
    static void show(Activity activity, YouTubeVideo video, Runnable changed) {
        close(activity);
        new YouTubePreview(activity, video, changed);
    }

    private YouTubePreview(Activity activity, YouTubeVideo video, Runnable changed) {
        LinearLayout panel = GlassDialog.panel(activity);
        int screenWidth = Math.round(activity.getResources().getDisplayMetrics().widthPixels
                / activity.getResources().getDisplayMetrics().density);
        int screenHeight = Math.round(activity.getResources().getDisplayMetrics().heightPixels
                / activity.getResources().getDisplayMetrics().density);
        int width = Math.min(740, Math.min(screenWidth - 48, (screenHeight - 244) * 16 / 9 + 48));
        TextView title = GlassDialog.text(activity, video.title, 17);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));
        FrameLayout stage = new FrameLayout(activity);
        LinearLayout.LayoutParams media = new LinearLayout.LayoutParams(-1, dp(activity, (width - 48) * 9 / 16));
        media.topMargin = dp(activity, 10);
        panel.addView(stage, media);
        TextView status = GlassDialog.text(activity, "YouTube wird geladen …", 11);
        status.setPadding(0, dp(activity, 5), 0, dp(activity, 4));
        status.setMaxLines(2);
        panel.addView(status, new LinearLayout.LayoutParams(-1, dp(activity, 33)));
        ProgressBar progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        panel.addView(progress, new LinearLayout.LayoutParams(-1, dp(activity, 3)));
        LinearLayout controls = new LinearLayout(activity);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = button(activity, controls, "−10 s"), play = button(activity, controls, "Pause"),
                forward = button(activity, controls, "+10 s"), sound = button(activity, controls, "Ton aus");
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(-1, dp(activity, 36));
        controlParams.topMargin = dp(activity, 8);
        panel.addView(controls, controlParams);
        LinearLayout actions = new LinearLayout(activity);
        TextView favorite = button(activity, actions, ""), use = button(activity, actions, "Als Hintergrund"),
                close = button(activity, actions, "Zurück");
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(activity, 36));
        actionParams.topMargin = dp(activity, 7);
        panel.addView(actions, actionParams);
        Runnable favoriteLabel = () -> favorite.setText(YouTubeFavorites.contains(activity, video.id)
                ? "★ Entfernen" : "☆ Favorit");
        favoriteLabel.run();
        favorite.setOnClickListener(v -> {
            YouTubeBrowser.favorite(activity, video);
            favoriteLabel.run();
        });
        if (activity instanceof MainActivity) ((MainActivity) activity).previewYouTube(true);
        YouTubeEmbed[] player = {null};
        Dialog dialog = GlassDialog.show(activity, panel, width, -2);
        OPEN.put(activity, new java.lang.ref.WeakReference<>(dialog));
        SettingsSheet.track(activity, dialog);
        dialog.setOnDismissListener(d -> {
            OPEN.remove(activity);
            if (player[0] != null) player[0].close();
            if (activity instanceof MainActivity) ((MainActivity) activity).previewYouTube(false);
        });
        close.setOnClickListener(v -> dialog.dismiss());
        use.setOnClickListener(v -> {
            LiveWallpaperPrefs.select(activity, video.url(), video.title);
            changed.run();
            Toast.makeText(activity, "YouTube-Hintergrund festgelegt", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        try {
            player[0] = new YouTubeEmbed(activity, video.id, true, false, new YouTubeEmbed.Listener() {
                public void update(int state, double position, double duration) {
                    status.setText(time(position) + " / " + (duration > 0 ? time(duration) : "Live")
                            + " · YouTube" + (video.channel.isEmpty() ? "" : " · " + video.channel));
                    progress.setProgress(duration > 0 ? (int) (1000 * position / duration) : 0);
                    play.setText(state == 1 ? "Pause" : "Wiedergabe");
                }
                public void error(String message) { status.setText(message); }
            });
            stage.addView(player[0].view, new FrameLayout.LayoutParams(-1, -1));
        } catch (RuntimeException unavailable) {
            status.setText("YouTube benötigt Android System WebView auf diesem Gerät.");
        }
        back.setOnClickListener(v -> { if (player[0] != null) player[0].seek(-10); });
        forward.setOnClickListener(v -> { if (player[0] != null) player[0].seek(10); });
        play.setOnClickListener(v -> { if (player[0] != null) player[0].toggle(); });
        boolean[] audible = {false};
        sound.setOnClickListener(v -> {
            audible[0] = !audible[0];
            if (player[0] != null) player[0].sound(audible[0]);
            sound.setText(audible[0] ? "Ton an" : "Ton aus");
        });
        dialog.setOnKeyListener((d, key, event) -> {
            int seconds = key == KeyEvent.KEYCODE_MEDIA_REWIND ? -10
                    : key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ? 10 : 0;
            if (seconds != 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && player[0] != null) player[0].seek(seconds);
                return true;
            }
            if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (event.getAction() == KeyEvent.ACTION_UP && player[0] != null) player[0].toggle();
                return true;
            }
            return false;
        });
        play.requestFocus();
    }

    private static TextView button(Activity activity, LinearLayout row, String text) {
        TextView button = DesignPopup.button(activity, text);
        button.setTextSize(11);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(activity, 3), 0, dp(activity, 3), 0);
        row.addView(button, params);
        return button;
    }
    private static int dp(Activity activity, int n) { return SettingsSheet.dp(activity, n); }
    private static String time(double seconds) {
        long total = Math.max(0, (long) seconds);
        return total >= 3600 ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", total / 3600, total / 60 % 60, total % 60)
                : String.format(java.util.Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }
}
