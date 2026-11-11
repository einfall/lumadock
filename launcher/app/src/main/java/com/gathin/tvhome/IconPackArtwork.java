package com.gathin.tvhome;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/** Artwork from an installed pack, with the visible aspect ratio independent of its source density. */
final class IconPackArtwork extends BitmapDrawable {
    private static final int MAX_EDGE = 384;
    private static final int MIN_ALPHA = 16;

    private IconPackArtwork(Resources resources, Bitmap artwork) {
        super(resources, artwork);
    }

    static IconPackArtwork from(Resources resources, Drawable drawable) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0 || height <= 0) width = height = 128;
            float scale = Math.min(1f, MAX_EDGE / (float) Math.max(width, height));
            bitmap = Bitmap.createBitmap(Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)), Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            drawable.draw(new Canvas(bitmap));
        }
        if (bitmap == null) return null;
        if (bitmap.hasAlpha()) {
            Rect visible = visibleBounds(bitmap);
            if (visible.isEmpty()) return null;
            if (visible.width() != bitmap.getWidth() || visible.height() != bitmap.getHeight()) {
                bitmap = Bitmap.createBitmap(bitmap, visible.left, visible.top, visible.width(), visible.height());
            }
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = IconPackPolicy.fitScale(width, height, MAX_EDGE, 216);
        if (scale < 1f) {
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)), true);
        }
        return new IconPackArtwork(resources, bitmap);
    }

    private static Rect visibleBounds(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int left = width;
        int right = -1;
        int top = height;
        int bottom = -1;
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1);
            for (int x = 0; x < width; x++) {
                if ((row[x] >>> 24) < MIN_ALPHA) continue;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = y;
            }
        }
        return new Rect(left, top, right + 1, bottom + 1);
    }

    IconPackArtwork copy(Resources resources) {
        return new IconPackArtwork(resources, getBitmap());
    }

    void fit(int slotWidth, int slotHeight, Rect output) {
        int width = getIntrinsicWidth();
        int height = getIntrinsicHeight();
        float scale = IconPackPolicy.fitScale(width, height, slotWidth, slotHeight);
        int fittedWidth = slotWidth == 0 ? 0 : Math.max(1, Math.round(width * scale));
        int fittedHeight = slotHeight == 0 ? 0 : Math.max(1, Math.round(height * scale));
        int left = (slotWidth - fittedWidth) / 2;
        int top = (slotHeight - fittedHeight) / 2;
        output.set(left, top, left + fittedWidth, top + fittedHeight);
    }
}
