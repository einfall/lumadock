package com.gathin.tvhome;

import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Active edits are already stored by their normal settings screens. Capture them when switching out. */
final class DesignProfiles {
    static final int LIMIT = 8;
    private static final int STORE_LIMIT = DesignSnapshot.MAX_BYTES * LIMIT + 64 * 1024;
    private static final String STANDARD = "standard";
    private static final String TAG = "LumaDock.Profiles";

    static final class Profile {
        final String id;
        final String name;

        Profile(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final Context context;
    private final AtomicFile state;
    private final AtomicFile journal;
    private final AtomicFile undo;

    DesignProfiles(Context context) {
        this.context = context;
        File files = context.getFilesDir();
        state = new AtomicFile(new File(files, "layout-profiles.json"));
        journal = new AtomicFile(new File(files, "layout-transaction.json"));
        undo = new AtomicFile(new File(files, "layout-restore-undo.json"));
    }

    static void recover(Context context) {
        DesignProfiles profiles = new DesignProfiles(context);
        try {
            profiles.recover();
        } catch (IOException | JSONException | RuntimeException failure) {
            Log.e(TAG, "Could not recover interrupted layout transaction", failure);
        }
    }

    private void recover() throws IOException, JSONException {
        JSONObject pending;
        try {
            pending = read(journal, DesignSnapshot.MAX_BYTES + 4096);
        } catch (FileNotFoundException missing) {
            if (journal.getBaseFile().exists()) throw missing;
            return;
        }
        DesignSnapshot.apply(context, DesignSnapshot.decode(pending.getJSONObject("layout").toString()));
        JSONObject profiles = state();
        profiles.put("active", pending.getString("active"));
        DesignSnapshot.write(state, profiles, STORE_LIMIT);
        journal.delete();
    }

    List<Profile> list() throws IOException, JSONException {
        JSONArray entries = state().getJSONArray("profiles");
        List<Profile> profiles = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            profiles.add(new Profile(entry.getString("id"), entry.getString("name")));
        }
        return profiles;
    }

    String active() throws IOException, JSONException {
        return state().getString("active");
    }

    void create(String name) throws IOException, JSONException {
        recover();
        JSONObject profiles = state();
        JSONArray entries = profiles.getJSONArray("profiles");
        if (entries.length() >= LIMIT) throw new IOException("Maximal acht Profile.");
        entries.put(new JSONObject().put("id", UUID.randomUUID().toString()).put("name", name)
                .put("layout", DesignSnapshot.copyForProfile(DesignSnapshot.capture(context))));
        DesignSnapshot.write(state, profiles, STORE_LIMIT);
    }

    void rename(String id, String name) throws IOException, JSONException {
        recover();
        JSONObject profiles = state();
        find(profiles.getJSONArray("profiles"), id).put("name", name);
        DesignSnapshot.write(state, profiles, STORE_LIMIT);
    }

    void delete(String id) throws IOException, JSONException {
        recover();
        JSONObject profiles = state();
        if (id.equals(profiles.getString("active"))) throw new IOException("Profil ist aktiv.");
        JSONArray entries = profiles.getJSONArray("profiles");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject profile = entries.getJSONObject(i);
            if (!id.equals(profile.getString("id"))) continue;
            entries.remove(i);
            DesignSnapshot.write(state, profiles, STORE_LIMIT);
            releaseWidgets(profile.optJSONObject("layout"));
            return;
        }
    }

    private void releaseWidgets(JSONObject layout) {
        if (layout == null) return;
        try {
            JSONArray entries = new JSONArray(layout.getJSONObject("home").optString(HomeWidgetStore.ITEMS, "[]"));
            AppWidgetHost host = new AppWidgetHost(context, AndroidWidgetBridge.HOST_ID);
            for (int i = 0; i < entries.length(); i++) {
                JSONObject widget = entries.getJSONObject(i);
                int id = widget.optInt("appWidgetId");
                if (id > 0 && "android".equals(widget.optString("source"))) host.deleteAppWidgetId(id);
            }
        } catch (JSONException | RuntimeException failure) {
            Log.w(TAG, "Could not release deleted profile widgets", failure);
        }
    }

    void activate(String id) throws IOException, JSONException {
        recover();
        JSONObject profiles = state();
        String oldId = profiles.getString("active");
        if (id.equals(oldId)) return;
        JSONArray entries = profiles.getJSONArray("profiles");
        JSONObject target = DesignSnapshot.decode(find(entries, id).getJSONObject("layout").toString());
        JSONObject current = DesignSnapshot.capture(context);
        find(entries, oldId).put("layout", current);
        profiles.put("active", id);
        transact(oldId, current, target, profiles);
    }

    void restore(JSONObject incoming) throws IOException, JSONException {
        recover();
        JSONObject current = DesignSnapshot.capture(context);
        String id = active();
        JSONObject target = DesignSnapshot.forImport(context, incoming, current);
        saveUndo(id, current);
        transact(id, current, target, null);
    }

    void restoreTransfer(JSONObject incoming) throws IOException, JSONException {
        recover();
        JSONObject current = DesignSnapshot.captureTransfer(context);
        String id = active();
        JSONObject target = DesignSnapshot.forTransferImport(context, incoming, current);
        saveUndo(id, current);
        transact(id, current, target, null);
    }

    boolean hasUndo() {
        try {
            return active().equals(read(undo, DesignSnapshot.MAX_BYTES + 4096).getString("active"));
        } catch (IOException | JSONException failure) {
            return false;
        }
    }

    void undo() throws IOException, JSONException {
        recover();
        String id = active();
        JSONObject previous = read(undo, DesignSnapshot.MAX_BYTES + 4096);
        if (!id.equals(previous.getString("active"))) throw new IOException("Sicherung gehört zu einem anderen Profil.");
        JSONObject target = DesignSnapshot.decode(previous.getJSONObject("layout").toString());
        JSONObject current = target.has("transfer")
                ? DesignSnapshot.captureTransfer(context) : DesignSnapshot.capture(context);
        transact(id, current, target, null);
        undo.delete();
    }

    private void saveUndo(String id, JSONObject layout) throws IOException, JSONException {
        DesignSnapshot.write(undo, pending(id, layout), DesignSnapshot.MAX_BYTES + 4096);
    }

    private void transact(String oldId, JSONObject current, JSONObject target, JSONObject newState)
            throws IOException, JSONException {
        DesignSnapshot.write(journal, pending(oldId, current), DesignSnapshot.MAX_BYTES + 4096);
        try {
            DesignSnapshot.apply(context, target);
            if (newState != null) DesignSnapshot.write(state, newState, STORE_LIMIT);
            journal.delete();
        } catch (IOException | JSONException | RuntimeException failure) {
            try {
                recover();
            } catch (IOException | JSONException | RuntimeException rollback) {
                failure.addSuppressed(rollback);
            }
            throw failure;
        }
    }

    private static JSONObject pending(String id, JSONObject layout) throws JSONException {
        return new JSONObject().put("active", id).put("layout", layout);
    }

    private JSONObject state() throws IOException, JSONException {
        try {
            return read(state, STORE_LIMIT);
        } catch (FileNotFoundException missing) {
            if (state.getBaseFile().exists()) throw missing;
            return new JSONObject().put("active", STANDARD).put("profiles", new JSONArray()
                    .put(new JSONObject().put("id", STANDARD).put("name", "Standard")));
        }
    }

    private static JSONObject read(AtomicFile file, int limit) throws IOException, JSONException {
        try (InputStream input = file.openRead()) {
            return new JSONObject(DesignSnapshot.read(input, limit));
        }
    }

    private static JSONObject find(JSONArray entries, String id) throws JSONException {
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (id.equals(entry.getString("id"))) return entry;
        }
        throw new JSONException("Unknown profile");
    }
}
