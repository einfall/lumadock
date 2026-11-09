package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import java.util.Collections;
import java.util.List;

/** Remote-first live placement. Cancel never writes an edited coordinate. */
final class HeaderMoveOverlay extends View {
    private final Activity activity;
    private final PlacementTarget target;
    private final String key;
    private final String title;
    private final Dialog dialog;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int step;
    private final float density;
    private final boolean showGuides;
    private final boolean snap;
    private final PlacementGuides guides;
    private int rawX;
    private int rawY;
    private int x;
    private int y;
    private boolean accepted;

    static void show(Activity activity, PlacementTarget target, String key, String title, Runnable changed) {
        if (target.view(key) == null || !target.view(key).isShown()) {
            Toast.makeText(activity, R.string.header_show_first, Toast.LENGTH_LONG).show();
            return;
        }
        new HeaderMoveOverlay(activity, target, key, title, changed);
    }

    private HeaderMoveOverlay(Activity activity, PlacementTarget target, String key,
                              String title, Runnable changed) {
        super(activity);
        this.activity = activity;
        this.target = target;
        this.key = key;
        this.title = title;
        density = activity.getResources().getDisplayMetrics().density;
        int[] position = target.position(key);
        rawX = x = position[0];
        rawY = y = position[1];
        SharedPreferences prefs = activity.getSharedPreferences(Config.SETTINGS_NAME, 0);
        step = Math.max(1, Math.min(20, prefs.getInt(HeaderLayoutController.STEP, 5)));
        showGuides = prefs.getBoolean(PlacementGuides.SHOW, true);
        snap = prefs.getBoolean(PlacementGuides.SNAP, false);
        List<Rect> neighbors = activity instanceof MainActivity
                ? ((MainActivity) activity).placementNeighbors(target, key) : Collections.emptyList();
        guides = new PlacementGuides(target.width(), target.height(), neighbors, density);
        setFocusableInTouchMode(true);

        List<Dialog> hidden = SettingsSheet.hideForPlacement(activity);
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(this);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.getDecorView().setSystemUiVisibility(
                    SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        dialog.setOnKeyListener((ignored, code, event) -> handleKey(code, event));
        NavigationSounds.attach(activity, window);
        dialog.setOnDismissListener(ignored -> {
            target.endPreview();
            SettingsSheet.restoreAfterPlacement(hidden);
            if (accepted) changed.run();
        });
        dialog.show();
        if (window != null) window.setLayout(-1, -1);
        requestFocus();
    }

    private boolean handleKey(int code, KeyEvent event) {
        int dx = 0;
        int dy = 0;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: dx = -step; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: dx = step; break;
            case KeyEvent.KEYCODE_DPAD_UP: dy = -step; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: dy = step; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    accepted = true;
                    target.set(key, x, y);
                    dialog.dismiss();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (event.getAction() == KeyEvent.ACTION_UP) dialog.dismiss();
                return true;
            default: return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int[] size = target.size(key);
            if (dx != 0) {
                rawX = HeaderLayoutPolicy.clamp(rawX + dx, target.width(), size[0]);
                x = snap ? PlacementGuides.snap(rawX, size[0], target.width(), guides.columns) : rawX;
            }
            if (dy != 0) {
                rawY = HeaderLayoutPolicy.clamp(rawY + dy, target.height(), size[1]);
                y = snap ? PlacementGuides.snap(rawY, size[1], target.height(), guides.rows) : rawY;
            }
            target.preview(key, x, y);
            postInvalidateOnAnimation();
        }
        return true;
    }

    @Override protected void onDraw(Canvas canvas) {
        setContentDescription(title + " · X " + x + " · Y " + y);
        Rect actual = target.bounds(key);
        if (showGuides) {
            drawGuides(canvas, guides.columns, actual.left, actual.right, true);
            drawGuides(canvas, guides.rows, actual.top, actual.bottom, false);
        }
        RectF box = new RectF(actual);
        box.inset(-5 * density, -5 * density);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * density);
        paint.setColor(0xFF98CCFF);
        canvas.drawRoundRect(box, 10 * density, 10 * density, paint);

        float pad = 18 * density;
        float height = 66 * density;
        float top = box.centerY() > getHeight() / 2 ? pad : getHeight() - height - pad;
        RectF bar = new RectF(pad, top, getWidth() - pad, top + height);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xF0101824);
        canvas.drawRoundRect(bar, 14 * density, 14 * density, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(16 * density);
        canvas.drawText(title + " · X " + x + " · Y " + y, bar.left + 16 * density, bar.top + 25 * density, paint);
        paint.setColor(0xFFB8C8DD);
        paint.setTextSize(11 * density);
        canvas.drawText(activity.getString(R.string.header_move_keys, step),
                bar.left + 16 * density, bar.top + 47 * density, paint);
    }

    private void drawGuides(Canvas canvas, int[] axes, int start, int end, boolean vertical) {
        int center = start + (end - start) / 2;
        int length = vertical ? getWidth() : getHeight();
        for (int axis : axes) {
            float pixel = axis * density;
            float gap = Math.min(Math.abs(pixel - center),
                    Math.min(Math.abs(pixel - start), Math.abs(pixel - end)));
            boolean primary = pixel < density || pixel >= length - density
                    || Math.abs(pixel - length / 2f) <= density;
            if (!primary && gap > PlacementGuides.DISTANCE * density) continue;
            paint.setColor(gap <= density ? 0xEEA9D5FF : 0x6098CCFF);
            paint.setStrokeWidth(density);
            pixel = Math.max(density, Math.min(length - density, pixel));
            if (vertical) canvas.drawLine(pixel, 0, pixel, getHeight(), paint);
            else canvas.drawLine(0, pixel, getWidth(), pixel, paint);
        }
    }
}
