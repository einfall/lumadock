package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class DockFolders {
    static final String KEY = "dock_folders";
    private static final String TILE_PACKAGE = "com.gathin.tvhome.folder";
    static final int LIMIT = 4;

    static final class Folder {
        final String id;
        String name;
        boolean glassTile;
        String opening = "list_ok";
        boolean pivot;
        String names = "dock";
        final Set<String> packages = new LinkedHashSet<>();

        Folder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        ComponentName component() {
            return new ComponentName(TILE_PACKAGE, TILE_PACKAGE + "." + id);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    static List<Folder> load(Context context) {
        try {
            return decode(prefs(context).getString(KEY, "[]"));
        } catch (JSONException invalid) {
            return new ArrayList<>();
        }
    }

    static Folder create(String name) {
        return new Folder(UUID.randomUUID().toString(), name);
    }

    static void save(Context context, List<Folder> folders) {
        JSONArray result = new JSONArray();
        for (Folder folder : folders) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", folder.id);
                object.put("name", folder.name);
                object.put("apps", new JSONArray(folder.packages));
                object.put("glassTile", folder.glassTile);
                object.put("popup", "popup".equals(folder.opening));
                object.put("opening", folder.opening);
                object.put("pivot", folder.pivot);
                object.put("names", folder.names);
            } catch (JSONException impossible) {
                throw new IllegalArgumentException(impossible);
            }
            result.put(object);
        }
        prefs(context).edit().putString(KEY, result.toString()).apply();
    }

    static String nameMode(String mode) {
        if ("top".equals(mode) || "focused".equals(mode) || "all".equals(mode)) return mode;
        return "dock";
    }

    static List<Folder> decode(String text) throws JSONException {
        JSONArray array = new JSONArray(text);
        if (array.length() > LIMIT) throw new JSONException("Too many folders");
        List<Folder> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> assigned = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String id = object.getString("id");
            String name = object.getString("name");
            if (!id.matches("[a-f0-9-]{36}") || !ids.add(id) || name.trim().isEmpty() || name.length() > 40) {
                throw new JSONException("Invalid folder");
            }
            Folder folder = new Folder(id, name);
            folder.glassTile = object.optBoolean("glassTile");
            String opening = object.optString("opening", object.optBoolean("popup") ? "popup" : "list_ok");
            folder.opening = "popup".equals(opening) || "list_focus".equals(opening) ? opening : "list_ok";
            folder.pivot = object.optBoolean("pivot");
            folder.names = nameMode(object.optString("names"));
            JSONArray apps = object.getJSONArray("apps");
            if (apps.length() > 300) throw new JSONException("Too many folder apps");
            for (int j = 0; j < apps.length(); j++) {
                String pkg = apps.getString(j);
                if (pkg.length() > 255 || !pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+") || !assigned.add(pkg)) {
                    throw new JSONException("Invalid folder app");
                }
                folder.packages.add(pkg);
            }
            result.add(folder);
        }
        return result;
    }
}
