package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Home-only idle clock; leaves Android dreams and OLED protection alone. */
final class IdleClockPanel {
    static final String KEY = "home_idle_clock_minutes";
    final LinearLayout view;
    private final int original;
    private int selected;
    private Runnable updated=()->{};
    void setOnChanged(Runnable changed){updated=changed;}

    IdleClockPanel(Activity activity, int minutes, Runnable dismiss) {
        original = selected = minutes;
        view = new LinearLayout(activity);
        view.setOrientation(LinearLayout.VERTICAL);
        text(activity, R.string.idle_clock_description, 13);
        int[] values = {0, 1, 2, 5, 10};
        LinearLayout row = new LinearLayout(activity);
        TextView[] buttons = new TextView[values.length];
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            TextView button = DesignPopup.button(activity, value == 0
                    ? activity.getString(R.string.idle_clock_off) : Integer.toString(value));
            buttons[i] = button;
            button.setActivated(value == selected);
            button.setOnClickListener(v -> {
                selected = value;
                for (int n = 0; n < values.length; n++) buttons[n].setActivated(selected == values[n]);
                updated.run();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(activity, 38), 1f);
            if (i > 0) lp.setMarginStart(dp(activity, 5));
            row.addView(button, lp);
        }
        view.addView(row);
        text(activity, R.string.idle_clock_minutes, 12);
        TextView preview = DesignPopup.button(activity, activity.getString(R.string.idle_clock_preview));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(activity, 40));
        previewLp.topMargin = dp(activity, 16);
        view.addView(preview, previewLp);
        preview.setOnClickListener(v -> {
            dismiss.run();
            activity.startActivity(new Intent(activity, ClockActivity.class));
        });
        text(activity, R.string.idle_clock_protection, 12);
    }

    boolean changed() { return original != selected; }
    void save(SharedPreferences.Editor editor) { editor.putInt(KEY, selected); }

    private void text(Activity activity, int resource, int size) {
        TextView label = new TextView(activity);
        label.setText(resource);
        label.setTextSize(size);
        label.setTextColor(size == 20 ? 0xFFFFFFFF : 0xFFBDC5D4);
        label.setPadding(0, dp(activity, 10), 0, dp(activity, 10));
        view.addView(label);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
