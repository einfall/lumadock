package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SonosSettings {
    private static final String SOURCE = "volume_source_sonos";
    private static final String HOST = "sonos_device_ip";
    private static final String ID = "sonos_device_id";
    private static final String LABEL = "sonos_device_label";
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();

    static boolean sourceSetting(String key) {
        return SOURCE.equals(key) || HOST.equals(key) || ID.equals(key) || LABEL.equals(key);
    }

    static boolean enabled(Context context) { return prefs(context).getBoolean(SOURCE, false); }

    static SonosLan.Player player(Context context) {
        SharedPreferences preferences = prefs(context);
        String host = preferences.getString(HOST, "");
        String id = preferences.getString(ID, "");
        String label = preferences.getString(LABEL, "");
        return SonosLan.privateIpv4(host) && !id.isEmpty() && !label.isEmpty() ? new SonosLan.Player(host, id, label) : null;
    }

    static void append(Activity activity, SettingsSheet audio) {
        audio.options("volume", "Quelle der Lautstärke", new String[]{"HDMI-CEC", "Sonos im Heimnetz"},
                index -> (index == 1) == enabled(activity), index -> {
                    boolean useSonos = index == 1;
                    prefs(activity).edit().putBoolean(SOURCE, useSonos).apply();
                    if (!useSonos && prefs(activity).getBoolean(CecVolumeOverlay.KEY, false)) HomeKeyService.connectLocalCec();
                });
        audio.row("volume", "Sonos-Gerät", "Suchen, auswählen und Lautstärke regeln",
                () -> show(activity, audio::refreshChoices));
    }

    private static void show(Activity activity, Runnable sourceChanged) {
        SettingsSheet sheet = new SettingsSheet(activity, "Sonos im Heimnetz");
        Runnable render = () -> {
            sheet.clearRows();
            SonosLan.Player current = player(activity);
            if (current == null) sheet.info("Wähle die Soundbar, deren Lautstärke dieser Fernseher anzeigen soll.");
            else sheet.row("volume", current.label, "Ausgewählt · Lautstärke und Stumm regeln",
                    () -> controls(activity, current));
            sheet.row("network", "Sonos-Geräte suchen", null, () -> discover(activity, selection -> {
                save(activity, selection);
                sourceChanged.run();
                sheet.dialog.dismiss();
                show(activity, sourceChanged);
            }));
            sheet.row("settings", "Sonos-IP eingeben", "Wenn die Suche das Gerät nicht findet", () -> manual(activity, selection -> {
                save(activity, selection);
                sourceChanged.run();
                sheet.dialog.dismiss();
                show(activity, sourceChanged);
            }));
        };
        render.run();
        sheet.show();
    }

    private static void save(Activity activity, SonosLan.Player device) {
        prefs(activity).edit().putString(HOST, device.ip).putString(ID, device.id)
                .putString(LABEL, device.label).putBoolean(SOURCE, true).apply();
    }

    private static void discover(Activity activity, java.util.function.Consumer<SonosLan.Player> picked) {
        SettingsSheet results = new SettingsSheet(activity, "Sonos-Geräte auswählen");
        results.info("Sonos-Geräte werden gesucht …");
        results.show();
        NETWORK.execute(() -> {
            List<SonosLan.Player> devices;
            try {
                devices = SonosLan.discover(activity.getApplicationContext());
            } catch (Exception failure) {
                devices = java.util.Collections.emptyList();
            }
            List<SonosLan.Player> found = devices;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || !results.dialog.isShowing()) return;
                results.clearRows();
                if (found.isEmpty()) {
                    results.info("Keine Sonos-Geräte gefunden.");
                    results.row("settings", "Sonos-IP eingeben", null, () -> manual(activity, device -> {
                        results.dialog.dismiss();
                        picked.accept(device);
                    })).requestFocus();
                    return;
                }
                SonosLan.Player selected = player(activity);
                for (SonosLan.Player device : found) {
                    android.view.View row = results.row("volume", device.label,
                            selected != null && selected.id.equals(device.id) ? "Ausgewählt" : null, () -> {
                                results.dialog.dismiss();
                                picked.accept(device);
                            });
                    if (device == found.get(0)) row.requestFocus();
                }
            });
        });
    }

    private static void manual(Activity activity, java.util.function.Consumer<SonosLan.Player> picked) {
        EditText field = new EditText(activity);
        field.setSingleLine();
        field.setHint("192.168.x.x");
        field.setInputType(InputType.TYPE_CLASS_PHONE);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Sonos-IP eingeben").setView(field)
                .setNegativeButton(android.R.string.cancel, null).setPositiveButton("Verbinden", null).show();
        SettingsSheet.track(activity, dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String address = field.getText().toString().trim();
            if (!SonosLan.privateIpv4(address)) {
                field.setError("Private IPv4-Adresse eingeben");
                return;
            }
            view.setEnabled(false);
            NETWORK.execute(() -> {
                SonosLan.Player found;
                try { found = SonosLan.identify(address); }
                catch (Exception failure) { found = null; }
                SonosLan.Player device = found;
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || !dialog.isShowing()) return;
                    if (device == null) {
                        view.setEnabled(true);
                        field.setError("Sonos-Gerät nicht erreichbar");
                    } else {
                        dialog.dismiss();
                        picked.accept(device);
                    }
                });
            });
        });
        field.requestFocus();
    }

    private static void controls(Activity activity, SonosLan.Player device) {
        SettingsSheet sheet = new SettingsSheet(activity, device.label);
        TextView status = GlassDialog.text(activity, "Lautstärke wird gelesen …", 14);
        sheet.add(status);
        boolean[] busy = {false};
        sheet.row("volume", "Leiser", null, () -> level(activity, sheet, status, busy, device, -1, false));
        sheet.row("volume", "Lauter", null, () -> level(activity, sheet, status, busy, device, 1, false));
        sheet.row("volume", "Stumm umschalten", null, () -> level(activity, sheet, status, busy, device, 0, true));
        sheet.row("refresh", "Aktualisieren", null, () -> level(activity, sheet, status, busy, device, 0, false));
        sheet.show();
        level(activity, sheet, status, busy, device, 0, false);
    }

    private static void level(Activity activity, SettingsSheet sheet, TextView status, boolean[] busy,
                              SonosLan.Player device, int steps, boolean mute) {
        if (busy[0]) return;
        busy[0] = true;
        NETWORK.execute(() -> {
            String text;
            try {
                SonosLan.Level level = steps == 0 && !mute ? SonosLan.read(device) : SonosLan.adjust(device, steps, mute);
                text = level.muted ? "Stumm · " + level.volume + " %" : "Lautstärke " + level.volume + " %";
            } catch (Exception failure) {
                text = "Sonos-Gerät nicht erreichbar. Wähle es erneut über die Suche.";
            }
            String result = text;
            activity.runOnUiThread(() -> {
                busy[0] = false;
                if (!activity.isFinishing() && sheet.dialog.isShowing()) status.setText(result);
            });
        });
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }
}
