package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** A versioned allowlist of layout settings. Device permissions and credentials live outside it. */
final class DesignSnapshot {
    static final int MAX_BYTES = 384 * 1024;
    private static final String FORMAT = "lumadock-layout";
    private static final Map<String, Class<?>> HOME = new LinkedHashMap<>();
    private static final Map<String, Class<?>> LIVE = new LinkedHashMap<>();
    private static final Map<String, Class<?>> SAVER = new LinkedHashMap<>();

    static {
        add(HOME, Integer.class, "columns", "dock_columns", "dock_pivot", Config.ROWS_BELOW_DOCK_KEY,
                Config.DOCK_OPACITY_KEY, DockPlacementPopup.X, DockPlacementPopup.Y,
                DockCounterPlacement.X, DockCounterPlacement.Y,
                DockPlacementPopup.WIDTH, DockMoveController.PX, DockMoveController.PY,
                DockStyle.TOP, DockStyle.BOTTOM, DockStyle.TRANSPARENCY, DockStyle.RIM_COLOR,
                DockStyle.RIM_OPACITY, DockStyle.RADIUS, HeaderLayoutController.STEP,
                AppTitleStyle.SIZE, AppTitleStyle.X, AppTitleStyle.Y, AppTitleStyle.LABELS,
                FocusedAppName.DX, FocusedAppName.DY, IdleClockPanel.KEY,
                DockStyle.key("quickaccess", DockStyle.TOP), DockStyle.key("quickaccess", DockStyle.BOTTOM),
                DockStyle.key("quickaccess", DockStyle.TRANSPARENCY), DockStyle.key("quickaccess", DockStyle.RIM_COLOR),
                DockStyle.key("quickaccess", DockStyle.RIM_OPACITY), DockStyle.key("quickaccess", DockStyle.RADIUS),
                SystemStatsWidget.SECONDS, QuoteFeed.MINUTES, ContentPreviewData.Y);
        add(HOME, Boolean.class, "dock_only", "dock_wrap", "dock_counter", "animations", "advanced_effects",
                "pill_text_inputs", "pill_text_network", "pill_text_settings", "visual_menu_compact",
                DockPlacementPopup.ENABLED, DockPlacementPopup.DOTS, DockMoveController.FREE,
                DockStyle.VISIBLE, DockStyle.RIM, DockStyle.GLOSS, DockStyle.key("quickaccess", DockStyle.RIM),
                DockStyle.key("quickaccess", DockStyle.GLOSS),
                AppTitleStyle.DOMINANT, FocusStyle.SHINE, FocusStyle.EDGE, "motion_overlay", "motion_zoom",
                "motion_in", "motion_out", "motion_bounce", "text_greeting_migrated", SleepTimer.SHOW,
                PlacementGuides.SHOW, PlacementGuides.SNAP, ContentPreviewData.ENABLED);
        add(HOME, String.class, "dock_browse", "dock_sort", Config.WALLPAPER_CHOICE_KEY,
                DockStyle.MODE, HeaderLayoutController.MODE, HomeBackdrop.MODE, HomeBackdrop.ID,
                Typography.APPS, Typography.HOME_CLOCK, Typography.SAVER_CLOCK, IconPacks.KEY,
                AppAppearance.LABELS, AppAppearance.ICONS, AppTitleStyle.MODE, AppTitleStyle.ALIGN, AppTitleStyle.TRANSITION,
                SurfaceThemes.COMMON, SurfaceThemes.BUTTON,
                SurfaceThemes.WIDGET, FocusStyle.COLOR, "motion_frame_mode", HomeWidgetStore.ITEMS, DockFolders.KEY);
        add(HOME, JSONArray.class, Config.DESIGN_HIDDEN_KEY, ContentPreviewData.DISABLED);
        for (String header : HeaderLayoutController.KEYS) {
            String key = HeaderLayoutController.P + header;
            add(HOME, Integer.class, key + "_x", key + "_y");
            add(HOME, Boolean.class, HeaderLayoutController.P + "show_" + header);
        }
        for (String key : new String[]{"effects_focus_", "effects_idle_"}) {
            add(HOME, Boolean.class, key + "frame", key + "glass", key + "shine", key + "thin",
                    key + "contrast", key + "nuvio", key + "glow");
            add(HOME, Integer.class, key + "gap", key + "contrast_width", key + "width",
                    key + "glow_strength", key + "glow_spread");
            add(HOME, String.class, key + "color");
        }

        add(LIVE, Boolean.class, "enabled");
        add(LIVE, Integer.class, "dim", LiveWallpaperSelection.MINUTES);
        add(LIVE, String.class, "source", "title", LiveWallpaperSelection.POOL, LiveWallpaperSelection.MODE,
                LiveWallpaperSelection.CATEGORY, BackgroundSelection.MODE);
        add(LIVE, JSONArray.class, LiveWallpaperSelection.FAVORITES, LiveWallpaperSelection.TRIGGERS,
                BackgroundSelection.TYPES);
        add(SAVER, Boolean.class, SaverPrefs.RANDOM);
        add(SAVER, Integer.class, SaverPrefs.DIM, SaverPrefs.MINUTES);
        add(SAVER, String.class, SaverPrefs.MODE, SaverPrefs.PHOTO, SaverPrefs.IMAGE, SaverPrefs.VIDEO, SaverPrefs.TITLE);
    }

    private static void add(Map<String, Class<?>> fields, Class<?> type, String... keys) {
        for (String key : keys) fields.put(key, type);
    }

    static JSONObject capture(Context context) throws JSONException {
        Order order = Order.load(Order.file(context));
        return new JSONObject().put("format", FORMAT).put("version", 1)
                .put("home", collect(context, Config.SETTINGS_NAME, HOME))
                .put("live", collect(context, LiveWallpaperPrefs.NAME, LIVE))
                .put("saver", collect(context, SaverPrefs.NAME, SAVER))
                .put("order", order == null ? JSONObject.NULL : Order.encode(order))
                .put("library", LibraryApps.encode(LibraryApps.load(context)))
                .put("removed", LibraryApps.encode(LibraryApps.loadRemoved(context)));
    }

    private static JSONObject collect(Context context, String file, Map<String, Class<?>> fields) throws JSONException {
        Map<String, ?> values = context.getSharedPreferences(file, 0).getAll();
        JSONObject result = new JSONObject();
        for (String key : fields.keySet()) {
            Object value = values.get(key);
            if (value == null) continue;
            if (value instanceof Set<?>) value = new JSONArray((Set<?>) value);
            result.put(key, value);
        }
        return result;
    }

    static JSONObject decode(String text) throws JSONException {
        JSONObject snapshot = new JSONObject(text);
        if (!FORMAT.equals(snapshot.optString("format")) || !Integer.valueOf(1).equals(snapshot.get("version"))) {
            throw new JSONException("Unsupported layout format");
        }
        JSONObject home = snapshot.getJSONObject("home");
        validate(home, HOME);
        validate(snapshot.getJSONObject("live"), LIVE);
        validate(snapshot.getJSONObject("saver"), SAVER);
        String widgets = home.optString(HomeWidgetStore.ITEMS, "[]");
        JSONArray array = new JSONArray(widgets);
        if (array.length() > HomeWidgetStore.LIMIT) throw new JSONException("Too many widgets");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String id = item.getString("id");
            String source = item.getString("source");
            if (id.isEmpty() || id.length() > 100 || HomeWidgetStore.SUMMARY.equals(id) || !ids.add(id)
                    || (WidgetSource.find(source) == null && !"android".equals(source))) {
                throw new JSONException("Invalid widget");
            }
        }
        DockFolders.decode(home.optString(DockFolders.KEY, "[]"));
        new JSONObject(home.optString(AppAppearance.LABELS, "{}"));
        new JSONObject(home.optString(AppAppearance.ICONS, "{}"));
        if (!snapshot.has("order")) throw new JSONException("Missing app order");
        if (!snapshot.isNull("order")) {
            Order order = Order.decode(snapshot.getString("order"));
            if (order == null || order.keys.size() > 1500) throw new JSONException("Invalid app order");
            for (String key : order.keys) {
                if (key.length() > 512 || android.content.ComponentName.unflattenFromString(key) == null) {
                    throw new JSONException("Invalid app component");
                }
            }
        }
        validateLibrary(snapshot.getString("library"));
        validateLibrary(snapshot.getString("removed"));
        return snapshot;
    }

    private static void validateLibrary(String encoded) throws JSONException {
        if (encoded.length() > LibraryApps.MAX_ADDED * 256
                || !LibraryApps.encode(LibraryApps.decode(encoded)).equals(encoded)) {
            throw new JSONException("Invalid app visibility list");
        }
    }

    private static void validate(JSONObject object, Map<String, Class<?>> fields) throws JSONException {
        for (String key : fields.keySet()) {
            if (!object.has(key)) continue;
            Object value = object.get(key);
            if (!fields.get(key).isInstance(value)) throw new JSONException("Wrong type for " + key);
            if (value instanceof JSONArray) stringSet((JSONArray) value);
        }
        for (String key : Arrays.asList("columns", "dock_columns")) {
            if (!object.has(key)) continue;
            int value = object.getInt(key);
            if (value < 4 || value > 7) throw new JSONException("Invalid column count");
        }
    }

    private static Set<String> stringSet(JSONArray array) throws JSONException {
        if (array.length() > 1500) throw new JSONException("Too many selections");
        Set<String> result = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            Object item = array.get(i);
            if (!(item instanceof String)) throw new JSONException("Invalid selection");
            result.add((String) item);
        }
        return result;
    }

    static JSONObject portable(Context context, JSONObject snapshot) throws JSONException {
        JSONObject result = decode(snapshot.toString());
        JSONObject home = result.getJSONObject("home");
        replaceWidgets(home, widgets(home, false));
        Set<String> catalog = new HashSet<>();
        for (LiveWallpaperCatalog.Item item : LiveWallpaperCatalog.bundled(context)) catalog.add(item.url);

        JSONObject live = result.getJSONObject("live");
        if (!catalog.contains(live.optString("source"))) {
            live.remove("source");
            live.remove("title");
            live.remove("enabled");
        }
        if (live.has(LiveWallpaperSelection.FAVORITES)) {
            Set<String> favorites = stringSet(live.getJSONArray(LiveWallpaperSelection.FAVORITES));
            favorites.retainAll(catalog);
            live.put(LiveWallpaperSelection.FAVORITES, new JSONArray(favorites));
        }
        JSONObject saver = result.getJSONObject("saver");
        saver.remove(SaverPrefs.IMAGE);
        if ("image".equals(saver.optString(SaverPrefs.MODE))) saver.remove(SaverPrefs.MODE);
        if (!catalog.contains(saver.optString(SaverPrefs.VIDEO))) {
            saver.remove(SaverPrefs.VIDEO);
            saver.remove(SaverPrefs.TITLE);
            if ("video".equals(saver.optString(SaverPrefs.MODE))) saver.remove(SaverPrefs.MODE);
        }
        return result;
    }

    static JSONObject copyForProfile(JSONObject snapshot) throws JSONException {
        JSONObject copy = decode(snapshot.toString());
        JSONObject home = copy.getJSONObject("home");
        replaceWidgets(home, widgets(home, false));
        return copy;
    }

    static JSONObject forImport(Context context, JSONObject snapshot, JSONObject current) throws JSONException {
        JSONObject result = portable(context, snapshot);
        JSONObject home = result.getJSONObject("home");
        JSONArray merged = widgets(home, false);
        JSONArray existing = widgets(current.getJSONObject("home"), true);
        if (merged.length() + existing.length() > HomeWidgetStore.LIMIT) {
            throw new JSONException("Für die importierten Widgets fehlt Platz.");
        }
        for (int i = 0; i < existing.length(); i++) merged.put(existing.get(i));
        replaceWidgets(home, merged);
        if (!result.isNull("order")) {
            Order order = Order.decode(result.getString("order"));
            result.put("order", Order.encode(new Order(Order.dockKey(Config.load(context).dockApps), order.keys, order.docked)));
        }
        return decode(result.toString());
    }

    private static JSONArray widgets(JSONObject home, boolean android) throws JSONException {
        JSONArray source = new JSONArray(home.optString(HomeWidgetStore.ITEMS, "[]"));
        JSONArray result = new JSONArray();
        for (int i = 0; i < source.length(); i++) {
            JSONObject item = source.getJSONObject(i);
            if ("android".equals(item.getString("source")) == android) result.put(item);
        }
        return result;
    }

    private static void replaceWidgets(JSONObject home, JSONArray widgets) throws JSONException {
        home.put(HomeWidgetStore.ITEMS, widgets.toString());
    }

    static void apply(Context context, JSONObject snapshot) throws IOException, JSONException {
        File path = Order.file(context);
        if (snapshot.isNull("order")) {
            new AtomicFile(path).delete();
            if (path.exists()) throw new IOException("Could not clear app order");
        } else if (!Order.save(path, Order.decode(snapshot.getString("order")))) {
            throw new IOException("Could not save app order");
        }
        if (!LibraryApps.restore(context, LibraryApps.decode(snapshot.getString("library")),
                LibraryApps.decode(snapshot.getString("removed")))) {
            throw new IOException("Could not restore app visibility");
        }
        writePrefs(context, LiveWallpaperPrefs.NAME, LIVE, snapshot.getJSONObject("live"));
        writePrefs(context, SaverPrefs.NAME, SAVER, snapshot.getJSONObject("saver"));
        writePrefs(context, Config.SETTINGS_NAME, HOME, snapshot.getJSONObject("home"));
    }

    private static void writePrefs(Context context, String file, Map<String, Class<?>> fields, JSONObject values)
            throws JSONException, IOException {
        SharedPreferences prefs = context.getSharedPreferences(file, 0);
        SharedPreferences.Editor edit = prefs.edit();
        for (String key : fields.keySet()) {
            Object value = values.opt(key);
            if (value == null) edit.remove(key);
            else if (value instanceof String) edit.putString(key, (String) value);
            else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else edit.putStringSet(key, stringSet((JSONArray) value));
        }
        if (Config.SETTINGS_NAME.equals(file)) {
            edit.putLong(Config.SETTINGS_REVISION_KEY, prefs.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1);
        }
        if (!edit.commit()) throw new IOException("Could not save layout preferences");
    }

    static String read(InputStream stream, int limit) throws IOException {
        if (stream == null) throw new IOException("Could not open layout file");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (bytes.size() + count > limit) throw new IOException("Layout file too large");
            bytes.write(buffer, 0, count);
        }
        return bytes.toString(StandardCharsets.UTF_8.name());
    }

    static void write(AtomicFile file, JSONObject data, int limit) throws IOException {
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > limit) throw new IOException("Layout file too large");
        FileOutputStream out = file.startWrite();
        try {
            out.write(bytes);
            out.getFD().sync();
            file.finishWrite(out);
        } catch (IOException | RuntimeException failure) {
            file.failWrite(out);
            throw failure;
        }
    }
}
