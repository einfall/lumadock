package com.gathin.tvhome;

import android.app.Activity;
import java.util.List;

final class DockFolderSettings {
    private final Activity activity;
    private final Runnable changed;
    private final SettingsSheet sheet;
    private boolean listOpened;

    static void show(Activity activity, Runnable changed) {
        new DockFolderSettings(activity, changed).show();
    }

    static void edit(Activity activity, String tileKey, Runnable changed) {
        DockFolderSettings ui = new DockFolderSettings(activity, changed);
        List<DockFolders.Folder> folders = DockFolders.load(activity);
        for (DockFolders.Folder folder : folders) {
            if (folder.component().flattenToShortString().equals(tileKey)) {
                ui.edit(folders, folder);
                return;
            }
        }
    }

    private DockFolderSettings(Activity activity, Runnable changed) {
        this.activity = activity;
        this.changed = changed;
        sheet = new SettingsSheet(activity, "Dock-Ordner");
    }

    private void show() {
        listOpened = true;
        populate();
        sheet.show();
    }

    private void populate() {
        sheet.clearRows();
        List<DockFolders.Folder> folders = DockFolders.load(activity);
        sheet.info("Apps in einem Ordner erscheinen gesammelt im Dock. Jeder App ist ein Ordner zugeordnet.");
        for (DockFolders.Folder folder : folders) {
            sheet.row("folder", folder.name, folder.packages.size() + " Apps", () -> edit(folders, folder));
        }
        if (folders.size() < DockFolders.LIMIT) {
            sheet.row("folder", "Neuer Ordner", "Zum Beispiel Streaming, Mediatheken oder Spiele",
                    () -> DesignDialogs.name(activity, "Ordnername", "", name -> {
                        DockFolders.Folder folder = DockFolders.create(name);
                        folders.add(folder);
                        save(folders);
                        edit(folders, folder);
                    }));
        }
    }

    private void edit(List<DockFolders.Folder> folders, DockFolders.Folder folder) {
        SettingsSheet editor = new SettingsSheet(activity, folder.name);
        editor.row("text", "Umbenennen", null,
                () -> DesignDialogs.name(activity, "Ordnername", folder.name, name -> {
                    folder.name = name;
                    save(folders);
                    editor.dialog.dismiss();
                    edit(folders, folder);
                }));
        editor.row("folder", "Ordner auflösen", "Apps kehren in die normale App-Ansicht zurück",
                () -> DesignDialogs.confirm(activity, folder.name, "Ordner auflösen?", "Auflösen", () -> {
                    folders.remove(folder);
                    save(folders);
                    editor.dialog.dismiss();
                }));
        editor.toggle("Glas-Kachel mit App-Icons", "Zeigt bis zu sechs Mini-Icons auf dem Dock", folder.glassTile, on -> {
            folder.glassTile = on;
            save(folders);
        });
        String[] openings = {"popup", "list_ok", "list_focus"};
        editor.options("folder", "Ordner öffnen",
                new String[]{"Popup", "Liste mit OK", "Liste bei Fokus"},
                i -> openings[i].equals(folder.opening), i -> {
                    folder.opening = openings[i];
                    save(folders);
                });
        editor.toggle("Listen-Pivot", "Bis zu fünf Apps. Die Auswahl bleibt beim Scrollen möglichst mittig.", folder.pivot, on -> {
            folder.pivot = on;
            save(folders);
        });
        String[] placements = {"dock", "top", "focused", "all"};
        editor.options("text", "App-Namen im Popup",
                new String[]{"Wie im Dock", "Oben mittig", "Unter dem Fokus", "Alle"},
                i -> placements[i].equals(folder.names), i -> {
                    folder.names = placements[i];
                    save(folders);
                });
        editor.group("APPS");
        AppAppearance apps = new AppAppearance(activity, Config.load(activity));
        for (AppAppearance.Item item : apps.items) {
            if (apps.hidden.contains(item.pkg)) continue;
            editor.toggle(apps.name(item), null, folder.packages.contains(item.pkg), on -> {
                if (on) {
                    for (DockFolders.Folder other : folders) other.packages.remove(item.pkg);
                    folder.packages.add(item.pkg);
                } else {
                    folder.packages.remove(item.pkg);
                }
                save(folders);
            });
        }
        editor.info("Ist eine App schon in einem anderen Ordner, wird sie hierher verschoben.");
        editor.show();
    }

    private void save(List<DockFolders.Folder> folders) {
        DockFolders.save(activity, folders);
        if (listOpened) {
            populate();
            if (sheet.dialog.isShowing() && sheet.body.getChildCount() > 1) {
                sheet.body.getChildAt(1).requestFocus();
            }
        }
        changed.run();
    }
}
