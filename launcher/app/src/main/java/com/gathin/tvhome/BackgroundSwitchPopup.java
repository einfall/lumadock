package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rotation settings share the background tab instead of opening a second settings window. */
final class BackgroundSwitchPopup {
    static void show(Activity activity, Runnable changed) {
        DesignStudio.background(activity, "Wechsel");
    }

    static View content(Activity activity, Runnable changed) {
        return new Panel(activity, changed);
    }

    private static final class Panel extends ScrollView {
        private final Activity activity;
        private final SharedPreferences prefs;
        private final SharedPreferences config;
        private final List<SettingsOptions> options = new ArrayList<>();
        private final CheckBox[] sources = new CheckBox[LiveSourcePolicy.IDS.length];
        private final TextView status;
        private final TextView sourceTitle;
        private final GridSlider interval;
        private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> refresh();
        private boolean updating;
        private final LinearLayout body;

        Panel(Activity activity, Runnable changed) {
            super(activity);
            this.activity = activity;
            prefs = LiveWallpaperPrefs.prefs(activity);
            config = activity.getSharedPreferences(Config.SETTINGS_NAME, 0);
            setVerticalScrollBarEnabled(false);
            setFocusable(false);
            body = new LinearLayout(activity);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(4), dp(3), dp(4), dp(6));
            addView(body, new LayoutParams(-1, -2));

            choice("photo", "Wiedergabe", new String[]{"Fest", "Automatisch"},
                    i -> (i == 1) == BackgroundSelection.automatic(activity),
                    i -> BackgroundSelection.mode(activity, i == 1 ? "rotation" : "fixed"), false).compact();
            choice("grid", "Hintergründe", new String[]{"Live", "Bilder", "Verläufe"},
                    i -> BackgroundSelection.types(activity).contains(BackgroundRotationPolicy.TYPES[i]),
                    i -> BackgroundSelection.type(activity, BackgroundRotationPolicy.TYPES[i]), true).compact();
            sourceTitle = GlassDialog.text(activity, "Live-Quellen für Zufall", 12);
            sourceTitle.setPadding(dp(8), dp(8), 0, dp(3));
            body.addView(sourceTitle);
            LinearLayout row = null;
            for (int i = 0; i < sources.length; i++) {
                if (i % 3 == 0) {
                    row = new LinearLayout(activity);
                    body.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
                String id = LiveSourcePolicy.IDS[i];
                CheckBox check = new CheckBox(activity);
                check.setId(View.generateViewId());
                check.setText("all".equals(id) ? "Alle Quellen" : LiveWallpaperSelection.name(activity, id));
                check.setTextSize(12);
                check.setSingleLine(true);
                check.setTextColor(SettingsSheet.ink(0xFFE7EDF5, 0xFF15202D));
                check.setButtonTintList(ColorStateList.valueOf(0xFF85B9EC));
                check.setBackground(SettingsSheet.rowBackground(activity));
                check.setPadding(dp(7), 0, dp(7), 0);
                check.setMinHeight(dp(31));
                check.setOnCheckedChangeListener((button, enabled) -> {
                    if (!updating) LiveWallpaperSelection.source(activity, id, enabled);
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(33), 1);
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(check, params);
                sources[i] = check;
            }

            String[] triggers = {"off", "interval", "home", "tv"};
            choice("timer", "Wann wechseln?", new String[]{"Aus", "Nach Zeit", "Home-Start", "TV-Start"},
                    i -> i == 0 ? LiveWallpaperSelection.triggers(activity).isEmpty()
                            : LiveWallpaperSelection.triggers(activity).contains(triggers[i]),
                    i -> LiveWallpaperSelection.trigger(activity, triggers[i],
                            i == 0 || !LiveWallpaperSelection.triggers(activity).contains(triggers[i])), true);
            interval = new GridSlider(activity, "Intervall (Minuten)", 1, 240,
                    LiveRotationPolicy.minutes(prefs.getInt(LiveWallpaperSelection.MINUTES, 15)),
                    n -> prefs.edit().putInt(LiveWallpaperSelection.MINUTES, n).apply());
            body.addView(interval);
            LinearLayout next = SettingsSheet.row(activity, "move", "Jetzt wechseln", null, "›");
            next.setOnClickListener(v -> {
                if (BackgroundSelection.random(activity)) changed.run();
                else Toast.makeText(activity, "Keine Hintergründe für diese Auswahl vorhanden", Toast.LENGTH_LONG).show();
            });
            body.addView(next);
            status = GlassDialog.text(activity, "", 10);
            status.setMaxLines(2);
            status.setTextColor(0xFFA6B6C8);
            status.setPadding(dp(8), dp(7), dp(8), dp(5));
            body.addView(status);
            refresh();
        }

        private int dp(int value) { return SettingsSheet.dp(activity, value); }

        private SettingsOptions choice(String icon, String title, String[] names,
                            java.util.function.IntPredicate selected,
                            java.util.function.IntConsumer action, boolean multiple) {
            SettingsOptions choice = new SettingsOptions(activity, icon, title, names, selected, action, multiple);
            options.add(choice);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(3);
            body.addView(choice, params);
            return choice;
        }

        private void refresh() {
            updating = true;
            Set<String> selected = LiveWallpaperSelection.sources(activity);
            for (int i = 0; i < sources.length; i++) sources[i].setChecked(selected.contains(LiveSourcePolicy.IDS[i]));
            updating = false;
            for (SettingsOptions option : options) option.refresh();
            int count = LiveWallpaperSelection.rotationPool(activity).size();
            sourceTitle.setText("Live-Quellen · " + count + (count == 1 ? " Video" : " Videos"));
            interval.setVisibility(LiveWallpaperSelection.triggers(activity).contains("interval") ? VISIBLE : GONE);
            boolean empty = BackgroundSelection.types(activity).isEmpty()
                    || LiveWallpaperSelection.triggers(activity).isEmpty()
                    || (BackgroundSelection.types(activity).equals(java.util.Collections.singleton("live")) && selected.isEmpty());
            String label = BackgroundSelection.automatic(activity) ? "Automatisch" + (empty ? " · pausiert" : "") : "Fest";
            status.setText(label + " · " + BackgroundSelection.currentLabel(activity));
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            prefs.registerOnSharedPreferenceChangeListener(listener);
            config.registerOnSharedPreferenceChangeListener(listener);
            refresh();
        }

        @Override protected void onDetachedFromWindow() {
            prefs.unregisterOnSharedPreferenceChangeListener(listener);
            config.unregisterOnSharedPreferenceChangeListener(listener);
            super.onDetachedFromWindow();
        }
    }
}
