package com.gathin.tvhome;

import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Anchors the counter to the dock until the user gives it a screen position. */
final class DockCounterPlacement implements PlacementTarget {
    static final String X = "dock_counter_x";
    static final String Y = "dock_counter_y";
    private static final int GAP_DP = 8;

    private final ViewGroup root;
    private final TextView pill;
    private final SharedPreferences prefs;
    private final float density;
    private int anchorRight;
    private int anchorBottom;
    private int previewX;
    private int previewY;
    private boolean shown;
    private boolean editing;
    private boolean preview;

    DockCounterPlacement(ViewGroup root, TextView pill, SharedPreferences prefs, float density) {
        this.root = root;
        this.pill = pill;
        this.prefs = prefs;
        this.density = density;
        pill.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> place());
    }

    void anchor(Rect dock, int viewportRight, int tileTop) {
        anchorRight = viewportRight - px(8);
        anchorBottom = dock.isEmpty() ? tileTop - px(20) : dock.top - px(GAP_DP);
        place();
    }

    void shift(int dx, int dy) {
        anchorRight += dx;
        anchorBottom += dy;
        place();
    }

    void show(String label) {
        pill.setText(label);
        shown = true;
        place();
    }

    void hide() {
        shown = false;
        place();
    }

    void beginEdit(String label) {
        pill.setText(label);
        editing = true;
        place();
    }

    void reset() {
        prefs.edit().remove(X).remove(Y).apply();
        place();
    }

    private void place() {
        pill.setVisibility(shown || editing ? View.VISIBLE : View.INVISIBLE);
        if (!shown && !editing) return;

        int left = preview ? px(previewX) : prefs.contains(X)
                ? px(prefs.getInt(X, 0)) : anchorRight - pill.getWidth();
        int top = preview ? px(previewY) : prefs.contains(Y)
                ? px(prefs.getInt(Y, 0)) : anchorBottom - pill.getHeight();
        pill.setX(HeaderLayoutPolicy.clamp(left, root.getWidth(), pill.getWidth()));
        pill.setY(HeaderLayoutPolicy.clamp(top, root.getHeight(), pill.getHeight()));
    }

    @Override public View view(String key) { return pill; }
    @Override public int width() { return dp(root.getWidth()); }
    @Override public int height() { return dp(root.getHeight()); }

    @Override public int[] position(String key) {
        return new int[]{dp(pill.getX()), dp(pill.getY())};
    }

    @Override public int[] size(String key) {
        return new int[]{(int) Math.ceil(pill.getWidth() / density), (int) Math.ceil(pill.getHeight() / density)};
    }

    @Override public Rect bounds(String key) {
        int left = Math.round(pill.getX());
        int top = Math.round(pill.getY());
        return new Rect(left, top, left + pill.getWidth(), top + pill.getHeight());
    }

    @Override public void preview(String key, int x, int y) {
        preview = true;
        previewX = x;
        previewY = y;
        place();
    }

    @Override public void endPreview() {
        preview = false;
        editing = false;
        place();
    }

    @Override public void set(String key, int x, int y) {
        prefs.edit().putInt(X, x).putInt(Y, y).apply();
        preview = false;
        place();
    }

    private int dp(float pixels) { return Math.round(pixels / density); }
    private int px(int dp) { return Math.round(dp * density); }
}
