package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** The same dock material on the Home band, folder popup and folder artwork. */
final class DockSurface {
    private final float density;
    private final boolean translucent;
    private final boolean lightGlass;
    private final int legacyOpacity;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final QuickAccessDock quickAccess = new QuickAccessDock();
    private final RectF painted = new RectF();
    private final RectF inner = new RectF();
    private DockStyle style;

    DockSurface(float density, boolean translucent, int legacyOpacity, boolean lightGlass) {
        this.density = density;
        this.translucent = translucent;
        this.legacyOpacity = legacyOpacity;
        this.lightGlass = lightGlass;
        edge.setStyle(Paint.Style.STROKE);
        innerEdge.setStyle(Paint.Style.STROKE);
    }

    void setStyle(DockStyle style) {
        this.style = style;
        painted.setEmpty();
        if (style == null || !"quickaccess".equals(style.mode)) quickAccess.clear();
    }

    void draw(Canvas canvas, RectF bounds, float legacyRadius, boolean details) {
        if (bounds.isEmpty()) return;
        boolean legacy = style == null || "legacy".equals(style.mode);
        float radius = legacy ? legacyRadius
                : Math.min(style.radius * density, Math.min(bounds.width(), bounds.height()) / 2);
        if (!legacy && "quickaccess".equals(style.mode) && details) {
            quickAccess.draw(canvas, bounds, style, density);
            return;
        }
        if (!painted.equals(bounds)) {
            painted.set(bounds);
            fill.setShader(null);
            edge.setShader(null);
            if (legacy) {
                fill.setColor(translucent ? 0x8C121014 : DockStyle.alpha(0x141416, legacyOpacity));
                if (lightGlass) {
                    fill.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                            0x24FFFFFF, 0x0DFFFFFF, Shader.TileMode.CLAMP));
                }
                edge.setStrokeWidth(density);
                edge.setColor(lightGlass ? 0x38FFFFFF : translucent ? 0x26FFFFFF : 0x1FFFFFFF);
            } else {
                prepareCustom(bounds);
            }
        }
        // A banner is clipped to the card outline by AppAdapter. Its own rim would double that contour.
        if (!details) {
            canvas.drawRect(bounds, fill);
            return;
        }
        canvas.drawRoundRect(bounds, radius, radius, fill);
        if (!legacy && "glass".equals(style.mode) && style.gloss) {
            canvas.drawRoundRect(bounds, radius, radius, gloss);
            inner.set(bounds);
            inner.inset(density, density);
            float insetRadius = Math.max(0, radius - density);
            canvas.drawRoundRect(inner, insetRadius, insetRadius, innerEdge);
        }
        if (legacy || style.rim) canvas.drawRoundRect(bounds, radius, radius, edge);
    }

    private void prepareCustom(RectF bounds) {
        int opacity = 100 - style.transparency;
        boolean glass = "glass".equals(style.mode);
        fill.setColor(DockStyle.alpha(style.top, opacity));
        if (glass || "gradient".equals(style.mode)) {
            fill.setColor(0xFFFFFFFF);
            fill.setShader(gradient(bounds, glass ? new int[]{
                    DockStyle.alpha(DockStyle.mix(style.top, 0xFFFFFFFF, .24f), opacity),
                    DockStyle.alpha(style.top, Math.round(opacity * .85f)),
                    DockStyle.alpha(style.bottom, Math.round(opacity * .7f))
            } : new int[]{DockStyle.alpha(style.top, opacity), DockStyle.alpha(style.bottom, opacity)}, null));
        }
        edge.setStrokeWidth(density * .65f);
        edge.setColor(0xFFFFFFFF);
        edge.setShader(gradient(bounds, new int[]{
                DockStyle.alpha(style.rimColor, Math.min(100, style.rimOpacity * (glass ? 2 : 1))),
                DockStyle.alpha(style.rimColor, style.rimOpacity / 2),
                DockStyle.alpha(style.rimColor, style.rimOpacity)
        }, null));
        if (glass && style.gloss) {
            gloss.setShader(gradient(bounds, new int[]{
                    DockStyle.alpha(0xFFFFFF, Math.round(opacity * .2f)), 0x00FFFFFF,
                    DockStyle.alpha(0xFFFFFF, Math.round(opacity * .12f)), 0x00FFFFFF
            }, new float[]{0, .35f, .58f, 1}));
            innerEdge.setStrokeWidth(density * .5f);
            innerEdge.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                    DockStyle.alpha(0xFFFFFF, Math.round(opacity * .35f)), 0x00FFFFFF, Shader.TileMode.CLAMP));
        }
    }

    private static LinearGradient gradient(RectF bounds, int[] colors, float[] stops) {
        return new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                colors, stops, Shader.TileMode.CLAMP);
    }

    Drawable background() {
        return new Drawable() {
            private final RectF bounds = new RectF();

            @Override protected void onBoundsChange(Rect rectangle) {
                bounds.set(rectangle);
                bounds.inset(density, density);
            }

            @Override public void draw(Canvas canvas) {
                DockSurface.this.draw(canvas, bounds, 26 * density, true);
            }

            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter filter) {}
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }
}
