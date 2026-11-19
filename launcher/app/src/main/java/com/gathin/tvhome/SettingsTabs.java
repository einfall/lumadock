package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.IntConsumer;

/** Three visible tabs: focus moves to the center before the strip starts scrolling. */
final class SettingsTabs extends HorizontalScrollView {
    private final Activity activity;
    private final String title;
    private final String[] labels;
    private final TextView[] chips;
    private final LinearLayout strip;
    private final IntConsumer change;
    private final boolean animated;
    private int selected, cell;

    SettingsTabs(Activity activity, String title, String[] labels, int selected, IntConsumer change) {
        super(activity);
        this.activity = activity;
        this.title = title;
        this.labels = labels;
        this.selected = selected;
        this.change = change;
        animated = !Config.load(activity).reducedMotion;
        setId(View.generateViewId());
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setSmoothScrollingEnabled(animated);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x3011161E);
        background.setCornerRadius(dp(10));
        background.setStroke(dp(.5f), 0x28FFFFFF);
        setBackground(background);
        strip = new LinearLayout(activity);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setClipToPadding(false);
        addView(strip, new LayoutParams(-2, dp(42)));
        chips = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView chip = GlassDialog.text(activity, labels[i], 11);
            chip.setGravity(Gravity.CENTER);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setPadding(dp(5), 0, dp(5), 0);
            chip.setClickable(true);
            chip.setOnClickListener(v -> { requestFocus(); select(index); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(112), dp(34));
            params.setMargins(dp(3), 0, dp(3), 0);
            strip.addView(chip, params);
            chips[i] = chip;
        }
        setOnFocusChangeListener((v, on) -> paint());
        setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    select(Math.max(0, Math.min(chips.length - 1,
                            this.selected + (key == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1))));
                }
                return true;
            }
            if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                    View next = focusSearch(FOCUS_DOWN);
                    if (next != null && next != this) next.requestFocus();
                }
                return true;
            }
            return false;
        });
        paint();
    }

    private int dp(float value) { return SettingsSheet.dp(activity, value); }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int nextCell = Math.max(dp(75), width / 3);
        if (nextCell != cell) {
            cell = nextCell;
            for (TextView chip : chips) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chip.getLayoutParams();
                params.width = cell - dp(6);
                chip.setLayoutParams(params);
            }
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        post(() -> pivot(false));
    }

    private void select(int index) {
        if (index < 0 || index >= chips.length) return;
        if (selected == index) { pivot(animated); return; }
        selected = index;
        paint();
        pivot(animated);
        change.accept(index);
    }

    private void pivot(boolean animate) {
        if (cell <= 0) return;
        // Clamp to real content at both ends instead of reserving an empty neighbor.
        int maxScroll = Math.max(0, strip.getWidth() - getWidth());
        int x = Math.max(0, Math.min(maxScroll,
                selected * cell - (getWidth() - cell) / 2));
        if (animate) smoothScrollTo(x, 0);
        else scrollTo(x, 0);
    }

    private void paint() {
        for (int i = 0; i < chips.length; i++) {
            boolean active = i == selected;
            GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    active ? new int[]{0x554F5B69, 0x40323C49} : new int[]{0x10FFFFFF, 0x05000000});
            glass.setCornerRadius(dp(8));
            glass.setStroke(dp(active && hasFocus() ? 1.2f : .6f),
                    active ? (hasFocus() ? 0xFFFFFFFF : 0xA0FFFFFF) : 0x20FFFFFF);
            TextView chip = chips[i];
            chip.setBackground(glass);
            chip.setSelected(active);
            chip.setTextColor(active ? 0xFFFFFFFF : 0xFFB9C4D1);
            chip.animate().cancel();
            float scale = active ? 1.03f : .97f;
            if (animated) chip.animate().scaleX(scale).scaleY(scale).alpha(active ? 1f : .72f).setDuration(180).start();
            else { chip.setScaleX(scale); chip.setScaleY(scale); chip.setAlpha(active ? 1f : .72f); }
        }
        setContentDescription(title + " · " + labels[selected] + " · Links/Rechts wechseln, OK öffnen");
    }
}
