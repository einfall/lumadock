package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.util.AtomicFile;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

final class DesignProfilesPopup {
    private static final int EXPORT = 5811;
    private static final int IMPORT = 5812;
    private static final String BACKUP_NAME = "Luma-Dock-Layout.json";
    private static final String TAG = "LumaDock.Backup";

    private final MainActivity activity;
    private final DesignProfiles profiles;
    private final SettingsSheet sheet;

    static void show(MainActivity activity) {
        DesignProfilesPopup popup = new DesignProfilesPopup(activity);
        popup.render();
        popup.sheet.show();
    }

    private DesignProfilesPopup(MainActivity activity) {
        this.activity = activity;
        profiles = new DesignProfiles(activity);
        sheet = new SettingsSheet(activity, "Profile & Sicherung");
    }

    private void render() {
        sheet.clearRows();
        sheet.group("PROFILE");
        try {
            List<DesignProfiles.Profile> items = profiles.list();
            String active = profiles.active();
            for (DesignProfiles.Profile profile : items) {
                boolean selected = profile.id.equals(active);
                sheet.row("dock", profile.name, selected ? "Aktiv" : "Wechseln oder bearbeiten",
                        () -> profileActions(profile, selected));
            }
            if (items.size() < DesignProfiles.LIMIT) {
                sheet.row("grid", "Neues Profil", "Kopie des aktuellen Layouts",
                        () -> DesignDialogs.name(activity, "Profilname", "", this::create));
                sheet.info("Vorlagen übernehmen dein aktuelles Layout. Dock, Widgets und Hintergrund kannst du danach separat gestalten.");
                for (String name : new String[]{"Filmabend", "Gaming", "Nacht"}) {
                    boolean exists = items.stream().anyMatch(profile -> name.equalsIgnoreCase(profile.name));
                    if (!exists) sheet.row("star", name + " anlegen", null, () -> create(name));
                }
            }
        } catch (IOException | JSONException failure) {
            sheet.info("Profile konnten nicht gelesen werden.");
            Log.w(TAG, "Could not list profiles", failure);
        }

        sheet.group("SICHERUNG DES AKTIVEN PROFILS");
        sheet.row("folder", "Lokal sichern", "Sicherung im Luma-Dock-Ordner auf diesem TV",
                this::saveLocal);
        sheet.row("folder", "Lokale Sicherung laden", null, this::loadLocal);
        sheet.row("move", "Datei exportieren", "Speicherort im Android-Dateidialog wählen",
                () -> chooseFile(EXPORT));
        sheet.row("move", "Datei importieren", "Layout dieses oder eines anderen TVs laden",
                () -> chooseFile(IMPORT));
        if (profiles.hasUndo()) {
            sheet.row("refresh", "Letzte Wiederherstellung rückgängig machen", null,
                    () -> DesignDialogs.confirm(activity, "Wiederherstellung rückgängig machen",
                            "Das Layout vor dem letzten Import wieder einsetzen?", "Wiederherstellen",
                            () -> perform(profiles::undo, this::refreshHome)));
        }
        sheet.info("Die Datei enthält Layout, Reihenfolge, Ordner, Schriften und Luma-Widgets. "
                + "App-PINs, Wetter-Zugangsdaten, eigene Mediendateien und Android-Widget-Verknüpfungen sind gerätegebunden.");
    }

    private void create(String name) {
        perform(() -> profiles.create(name), this::render);
    }

    private void profileActions(DesignProfiles.Profile profile, boolean active) {
        SettingsSheet actions = new SettingsSheet(activity, profile.name);
        if (!active) {
            actions.row("dock", "Aktivieren", "Änderungen am aktuellen Profil werden gespeichert",
                    () -> perform(() -> profiles.activate(profile.id), this::refreshHome));
        }
        actions.row("text", "Umbenennen", null,
                () -> DesignDialogs.name(activity, "Profilname", profile.name, name -> {
                    perform(() -> profiles.rename(profile.id, name), () -> {
                        actions.dialog.dismiss();
                        render();
                    });
                }));
        if (!active) {
            actions.row("folder", "Profil löschen", null,
                    () -> DesignDialogs.confirm(activity, profile.name, "Dieses gespeicherte Layout löschen?",
                            "Löschen", () -> perform(() -> profiles.delete(profile.id), () -> {
                                actions.dialog.dismiss();
                                render();
                            })));
        }
        actions.show();
    }

    private void chooseFile(int request) {
        Intent intent = new Intent(request == EXPORT ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(request == EXPORT ? "application/json" : "*/*");
        if (request == EXPORT) intent.putExtra(Intent.EXTRA_TITLE, BACKUP_NAME);
        try {
            activity.startActivityForResult(intent, request);
        } catch (ActivityNotFoundException unavailable) {
            DesignDialogs.message(activity, "Auf diesem TV fehlt der Dateidialog. Verwende die lokale Sicherung.");
        }
    }

    static boolean result(MainActivity activity, int request, int result, Intent data) {
        if (request != EXPORT && request != IMPORT) return false;
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) return true;
        DesignProfilesPopup popup = new DesignProfilesPopup(activity);
        Uri uri = data.getData();
        if (request == EXPORT) {
            popup.perform(() -> {
                byte[] bytes = popup.portable().toString().getBytes(StandardCharsets.UTF_8);
                if (bytes.length > DesignSnapshot.MAX_BYTES) throw new IOException("Sicherungsdatei zu groß.");
                popup.document(() -> {
                    try (OutputStream out = activity.getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new IOException("Datei konnte nicht geöffnet werden.");
                        out.write(bytes);
                    }
                    popup.showResult(() -> DesignDialogs.message(activity, "Layout exportiert"));
                });
            }, () -> {});
        } else {
            popup.document(() -> {
                JSONObject snapshot;
                try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
                    snapshot = DesignSnapshot.decode(DesignSnapshot.read(input, DesignSnapshot.MAX_BYTES));
                }
                popup.showResult(() -> popup.confirmRestore(snapshot));
            });
        }
        return true;
    }

    private void document(Operation operation) {
        new Thread(() -> {
            try {
                operation.run();
            } catch (IOException | JSONException | RuntimeException failure) {
                Log.w(TAG, "Could not access layout document", failure);
                showResult(() -> DesignDialogs.message(activity, "Sicherungsdatei konnte nicht verarbeitet werden."));
            }
        }, "luma-layout-document").start();
    }

    private void showResult(Runnable action) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) action.run();
        });
    }

    private JSONObject portable() throws JSONException {
        return DesignSnapshot.portable(activity, DesignSnapshot.capture(activity));
    }

    private File local() {
        File directory = activity.getExternalFilesDir("backups");
        if (directory == null) directory = new File(activity.getFilesDir(), "backups");
        return new File(directory, BACKUP_NAME);
    }

    private void saveLocal() {
        File file = local();
        Runnable write = () -> perform(() -> {
            File directory = file.getParentFile();
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Sicherungsordner fehlt.");
            DesignSnapshot.write(new AtomicFile(file), portable(), DesignSnapshot.MAX_BYTES);
        }, () -> DesignDialogs.message(activity, "Gesichert unter " + file.getAbsolutePath()));
        if (file.isFile()) {
            DesignDialogs.confirm(activity, "Sicherung ersetzen", "Die bisherige lokale Sicherung überschreiben?",
                    "Ersetzen", write);
        } else {
            write.run();
        }
    }

    private void loadLocal() {
        File file = local();
        if (!file.isFile()) {
            DesignDialogs.message(activity, "Es gibt noch keine lokale Sicherung.");
            return;
        }
        perform(() -> {
            try (InputStream input = new AtomicFile(file).openRead()) {
                confirmRestore(DesignSnapshot.decode(DesignSnapshot.read(input, DesignSnapshot.MAX_BYTES)));
            }
        }, () -> {});
    }

    private void confirmRestore(JSONObject snapshot) {
        DesignDialogs.confirm(activity, "Layout wiederherstellen",
                "Das aktuelle Profil erhält das Layout aus der Sicherung. Der vorherige Stand wird zum Rückgängigmachen gespeichert.",
                "Wiederherstellen", () -> perform(() -> profiles.restore(snapshot), this::refreshHome));
    }

    private void refreshHome() {
        SettingsSheet.dismissAll(activity);
        activity.recreate();
    }

    private void perform(Operation operation, Runnable success) {
        try {
            operation.run();
            success.run();
        } catch (IOException | JSONException | RuntimeException failure) {
            Log.w(TAG, "Layout operation failed", failure);
            DesignDialogs.message(activity, "Layout-Aktion fehlgeschlagen. Prüfe die Sicherung und den freien Speicher.");
        }
    }

    private interface Operation {
        void run() throws IOException, JSONException;
    }
}
