package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.function.Supplier;

final class WidgetPreview {
    private WidgetPreview() {}

    static View live(Activity activity, Supplier<View> source, View fallback) {
        return scaled(activity, source, fallback, true);
    }

    private static View scaled(Activity activity, Supplier<View> source, View fallback, boolean live) {
        FrameLayout frame = new FrameLayout(activity);
        frame.setBackground(SurfaceThemes.background(activity, "graphite", false));
        frame.setClipToOutline(true);
        frame.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        frame.addView(new View(activity) {
            @Override protected void onDraw(Canvas canvas) {
                View widget = source.get();
                boolean ready = widget != null && (!live || widget.isShown())
                        && widget.getWidth() > 1 && widget.getHeight() > 1;
                if (fallback.getVisibility() != (ready ? INVISIBLE : VISIBLE)) {
                    fallback.setVisibility(ready ? INVISIBLE : VISIBLE);
                }
                if (ready) {
                    float scale = Math.min((getWidth() - dp(activity, 8)) / (float) widget.getWidth(),
                            (getHeight() - dp(activity, 8)) / (float) widget.getHeight());
                    canvas.save();
                    canvas.translate((getWidth() - widget.getWidth() * scale) / 2,
                            (getHeight() - widget.getHeight() * scale) / 2);
                    canvas.scale(scale, scale);
                    widget.draw(canvas);
                    canvas.restore();
                }
                if (live && isShown()) postInvalidateDelayed(1000);
            }
            @Override protected void onWindowVisibilityChanged(int visibility) {
                super.onWindowVisibilityChanged(visibility);
                if (visibility == VISIBLE) invalidate();
            }
        }, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    static View sample(Activity activity, HomeWidgetStore.Spec spec) {
        String theme = SurfaceThemes.instance(activity, spec.theme);
        SurfaceStyle surface = SurfaceStyle.of(theme, false);
        if (spec.temporal()) {
            TimeWidgetView clock = new TimeWidgetView(activity, true);
            clock.style(spec.timeStyle, surface);
            SurfaceThemes.widget(clock, theme);
            return clock;
        }
        if (spec.system() && spec.metricsMask != 0) {
            HomeWidgetCard card = new HomeWidgetCard(activity, spec, spec.metricsMask, null, null);
            long gb = 1L << 30;
            card.update(new SystemStatsWidget.Snapshot(32, 4*gb, gb, 16*gb, 6*gb, 5.8, 1.2));
            card.measure(View.MeasureSpec.makeMeasureSpec(dp(activity, spec.width), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            card.layout(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight());
            return scaled(activity, () -> card, new View(activity), false);
        }
        LinearLayout content = new LinearLayout(activity);
        content.setGravity(Gravity.CENTER);
        content.setOrientation(LinearLayout.VERTICAL);
        SurfaceThemes.widget(content, theme);
        if (spec.echo()) {
            EchoWidgetView player = new EchoWidgetView(activity);
            player.style(surface);
            player.layout(spec.echoLayout, spec.width, spec.height);
            player.update(EchoPlayback.example());
            content.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
            content.addView(player, new LinearLayout.LayoutParams(-1, -1));
            return content;
        }
        if (spec.quote() && "image".equals(spec.quoteMode)) {
            content.addView(image(activity, new AppGlyph("photo")), new LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24)));
        }
        TextView text = GlassDialog.text(activity, spec.text() ? spec.textContent
                : spec.quote() ? "Jeder Tag ist ein neuer Anfang." : "Keine Messwerte gewählt", 13);
        text.setGravity(Gravity.CENTER);
        text.setMaxLines(2);
        text.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.setTextColor(spec.text() || spec.quote() ? Color.WHITE : surface.ink);
        if (spec.text() || spec.quote()) {
            text.setTypeface(Typography.face(activity, spec.text() ? spec.textFont : spec.quoteFont, R.string.font_regular));
        }
        content.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return content;
    }

    static ImageView image(Activity activity, Drawable drawable) {
        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageDrawable(drawable);
        return image;
    }

    static View tile(Activity activity, View preview, String title, String subtitle, Runnable onClick) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setId(View.generateViewId());
        tile.setPadding(dp(activity, 5), dp(activity, 5), dp(activity, 5), dp(activity, 7));
        tile.setBackground(SettingsSheet.rowBackground(activity));
        tile.setContentDescription(title + " · " + subtitle);
        tile.setOnClickListener(v -> onClick.run());
        FrameLayout stage = new FrameLayout(activity);
        stage.setBackground(SurfaceThemes.background(activity, "graphite", false));
        stage.setClipToOutline(true);
        stage.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        stage.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        tile.addView(stage, new LinearLayout.LayoutParams(-1, dp(activity, 79)));
        TextView name = label(activity, title, 12, Color.WHITE, 0xFF171D25);
        name.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), 0);
        tile.addView(name);
        TextView detail = label(activity, subtitle, 10, 0xFF9DABBD, 0xFF525C69);
        detail.setPadding(dp(activity, 4), dp(activity, 2), dp(activity, 4), 0);
        tile.addView(detail);
        return tile;
    }

    private static TextView label(Activity activity, String text, int size, int normal, int focused) {
        TextView view = GlassDialog.text(activity, text, size);
        view.setDuplicateParentStateEnabled(true);
        view.setTextColor(SettingsSheet.ink(normal, focused));
        view.setSingleLine();
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return view;
    }

    private static int dp(Activity activity, float value) { return SettingsSheet.dp(activity, value); }
}
