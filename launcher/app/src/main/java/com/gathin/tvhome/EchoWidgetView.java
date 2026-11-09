package com.gathin.tvhome;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class EchoWidgetView extends LinearLayout {
    private final ImageView artwork;
    private final ImageView playbackState;
    private final TextView title;
    private final TextView detail;
    private final LinearLayout header;
    private final LinearLayout timeline;
    private final ProgressLine progress;
    private final TextView elapsed;
    private final TextView duration;
    private SurfaceStyle surface;
    private EchoLayout configured = EchoLayout.COMPACT;
    private EchoLayout arranged;
    private EchoPlayback shown;
    private boolean active;
    private long shownAt;
    private final Runnable tick = this::renderTime;

    EchoWidgetView(Activity activity) {
        super(activity);
        header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);

        artwork = new ImageView(activity);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable plate = new GradientDrawable();
        plate.setColor(0x16000000);
        plate.setCornerRadius(SettingsSheet.dp(activity, 5));
        artwork.setBackground(plate);
        artwork.setClipToOutline(true);

        LinearLayout textColumn = new LinearLayout(activity);
        textColumn.setOrientation(VERTICAL);
        header.addView(textColumn, new LayoutParams(0, -2, 1));
        title = text(activity, 13);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        detail = text(activity, 10);
        textColumn.addView(title);
        textColumn.addView(detail);

        playbackState = new ImageView(activity);
        int glyphSize = SettingsSheet.dp(activity, 16);
        LayoutParams glyph = new LayoutParams(glyphSize, glyphSize);
        glyph.setMarginStart(SettingsSheet.dp(activity, 8));
        header.addView(playbackState, glyph);

        timeline = new LinearLayout(activity);
        timeline.setOrientation(VERTICAL);
        timeline.setVisibility(GONE);
        progress = new ProgressLine(activity);
        timeline.addView(progress, new LayoutParams(-1, SettingsSheet.dp(activity, 3)));
        LinearLayout times = new LinearLayout(activity);
        elapsed = text(activity, 9);
        duration = text(activity, 9);
        duration.setGravity(Gravity.END);
        times.addView(elapsed, new LayoutParams(0, -2, 1));
        times.addView(duration, new LayoutParams(0, -2, 1));
        timeline.addView(times, new LayoutParams(-1, -2));
        arrange();
    }

    void layout(EchoLayout layout, int width, int height) {
        configured = layout.resolve(width, height);
        if (arrange()) renderTime();
    }

    private boolean arrange() {
        EchoLayout next = configured == EchoLayout.SMALL && (shown == null || shown.cover == null)
                ? EchoLayout.COMPACT : configured;
        if (next == arranged) return false;
        arranged = next;

        detach(artwork);
        detach(header);
        detach(timeline);
        removeAllViews();
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_VERTICAL);

        title.setTextSize(next == EchoLayout.WIDE ? 18 : next == EchoLayout.COMPACT ? 13 : 16);
        detail.setTextSize(next == EchoLayout.COMPACT ? 10 : 12);
        artwork.setImportantForAccessibility(next == EchoLayout.SMALL
                ? IMPORTANT_FOR_ACCESSIBILITY_YES : IMPORTANT_FOR_ACCESSIBILITY_NO);

        switch (next) {
            case SMALL:
                addView(artwork, new LayoutParams(-1, -1));
                break;
            case WIDE:
                setOrientation(HORIZONTAL);
                LayoutParams wideArt = new LayoutParams(0, -1, 1);
                wideArt.setMarginEnd(dp(12));
                addView(artwork, wideArt);
                LinearLayout info = new LinearLayout(getContext());
                info.setOrientation(VERTICAL);
                info.setGravity(Gravity.CENTER_VERTICAL);
                addView(info, new LayoutParams(0, -1, 1));
                addDetails(info);
                break;
            case SQUARE:
            case TALL:
                LayoutParams stackedArt = new LayoutParams(-1, 0, 1);
                stackedArt.bottomMargin = dp(7);
                addView(artwork, stackedArt);
                addDetails(this);
                break;
            default:
                LinearLayout row = new LinearLayout(getContext());
                row.setGravity(Gravity.CENTER_VERTICAL);
                LayoutParams compactArt = new LayoutParams(dp(32), dp(32));
                compactArt.setMarginEnd(dp(8));
                row.addView(artwork, compactArt);
                row.addView(header, new LayoutParams(0, -2, 1));
                addView(row, new LayoutParams(-1, -2));
                addTimeline(this);
                break;
        }
        return true;
    }

    private void addDetails(LinearLayout parent) {
        parent.addView(header, new LayoutParams(-1, -2));
        addTimeline(parent);
    }

    private void addTimeline(LinearLayout parent) {
        LayoutParams size = new LayoutParams(-1, -2);
        size.topMargin = dp(5);
        parent.addView(timeline, size);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static void detach(View view) {
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
    }

    private static TextView text(Activity activity, int size) {
        TextView view = GlassDialog.text(activity, "", size);
        view.setSingleLine(true);
        view.setIncludeFontPadding(false);
        view.setEllipsize(TextUtils.TruncateAt.END);
        return view;
    }

    void style(SurfaceStyle style) {
        surface = style;
        title.setTextColor(style.ink);
        detail.setTextColor(style.muted);
        elapsed.setTextColor(style.muted);
        duration.setTextColor(style.muted);
        playbackState.setImageTintList(ColorStateList.valueOf(style.muted));
        progress.colors(style.ink);
        if (artwork.getDrawable() instanceof AppGlyph) artwork.setImageTintList(ColorStateList.valueOf(style.muted));
    }

    void update(EchoPlayback item) {
        if (shown == item) return;
        shown = item;
        shownAt = SystemClock.elapsedRealtime();
        title.setText(item.title.isEmpty() ? item.status : item.title);
        detail.setText(item.detail);
        detail.setVisibility(item.detail.isEmpty() ? GONE : VISIBLE);
        boolean playing = "playing".equals(item.state);
        boolean paused = "paused".equals(item.state);
        playbackState.setVisibility(playing || paused ? VISIBLE : GONE);
        playbackState.setImageDrawable(new AppGlyph(playing ? "pause" : "play"));
        playbackState.setContentDescription(item.status);
        if (item.cover != null) {
            artwork.setImageTintList(null);
            artwork.setImageBitmap(item.cover);
            artwork.setContentDescription(item.detail.isEmpty() ? title.getText() : title.getText() + " · " + item.detail);
        } else {
            artwork.setImageDrawable(new AppGlyph("music"));
            if (surface != null) artwork.setImageTintList(ColorStateList.valueOf(surface.muted));
        }
        arrange();
        timeline.setVisibility(item.timing.durationMs > 0 ? VISIBLE : GONE);
        if (item.timing.durationMs > 0) duration.setText(EchoTiming.clock(item.timing.durationMs));
        renderTime();
    }

    void active(boolean on) {
        if (active == on) return;
        active = on;
        renderTime();
    }

    private void renderTime() {
        removeCallbacks(tick);
        if (arranged == EchoLayout.SMALL || shown == null || shown.timing.durationMs <= 0) return;
        long since = SystemClock.elapsedRealtime() - shownAt;
        long position = shown.timing.position(since);
        String time = EchoTiming.clock(position);
        if (!time.contentEquals(elapsed.getText())) elapsed.setText(time);
        progress.position(position / (float) shown.timing.durationMs);
        if (active && shown.timing.advances(since)) postDelayed(tick, 1000 - position % 1000);
    }

    private static final class ProgressLine extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int ink;
        private float fraction;

        ProgressLine(Activity activity) { super(activity); }

        void colors(int ink) {
            this.ink = ink;
            invalidate();
        }

        void position(float value) {
            fraction = value;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            float h = getHeight();
            float radius = h / 2;
            paint.setColor(0x30000000 | (ink & 0x00FFFFFF));
            canvas.drawRoundRect(0, 0, getWidth(), h, radius, radius, paint);
            if (fraction <= 0) return;
            paint.setColor(ink);
            canvas.drawRoundRect(0, 0, getWidth() * fraction, h, radius, radius, paint);
        }
    }
}
