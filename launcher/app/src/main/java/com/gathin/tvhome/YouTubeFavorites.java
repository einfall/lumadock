package com.gathin.tvhome;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Local YouTube favorites, independent of the aerial catalog and of API credentials. */
final class YouTubeFavorites {
    static final String KEY = "youtube_favorites";
    static final String INCLUDE = "youtube_in_rotation";
    static final String POOL = "youtube";
    static final int LIMIT = 200;

    static List<YouTubeVideo> load(Context context) {
        return decode(LiveWallpaperPrefs.prefs(context).getString(KEY, "[]"));
    }

    static void mergeLegacy(Context context) {
        android.content.SharedPreferences prefs = LiveWallpaperPrefs.prefs(context);
        java.util.Set<String> favorites = new java.util.HashSet<>(
                prefs.getStringSet(LiveWallpaperSelection.FAVORITES, java.util.Collections.emptySet()));
        boolean changed = false;
        for (YouTubeVideo video : load(context)) changed |= favorites.add(video.url());
        if (changed) prefs.edit().putStringSet(LiveWallpaperSelection.FAVORITES, favorites).apply();
    }

    static List<YouTubeVideo> decode(String text) {
        List<YouTubeVideo> items = new ArrayList<>();
        if (text == null || text.length() > 150000) return items;
        try {
            JSONArray array = new JSONArray(text);
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (int i = 0; i < array.length() && items.size() < LIMIT; i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id");
                if (!YouTubeVideo.validId(id) || !seen.add(id)) continue;
                items.add(new YouTubeVideo(id, bounded(item.optString("title"), 200),
                        bounded(item.optString("channel"), 120)));
            }
        } catch (JSONException ignored) { }
        return items;
    }

    static boolean contains(Context context, String id) {
        if (LiveWallpaperPrefs.prefs(context).getStringSet(LiveWallpaperSelection.FAVORITES,
                java.util.Collections.emptySet()).contains(new YouTubeVideo(id, "", "").url())) return true;
        for (YouTubeVideo item : load(context)) if (id.equals(item.id)) return true;
        return false;
    }

    static boolean toggle(Context context, YouTubeVideo video) {
        List<YouTubeVideo> items = load(context);
        java.util.Set<String> favorites = new java.util.HashSet<>(LiveWallpaperPrefs.prefs(context)
                .getStringSet(LiveWallpaperSelection.FAVORITES, java.util.Collections.emptySet()));
        for (YouTubeVideo item : items) favorites.add(item.url());
        boolean removed = favorites.remove(video.url());
        items.removeIf(item -> item.id.equals(video.id));
        if (!removed) {
            if (items.size() >= LIMIT) return false;
            items.add(video);
            favorites.add(video.url());
        }
        JSONArray array = new JSONArray();
        try {
            for (YouTubeVideo item : items) array.put(new JSONObject().put("id", item.id)
                    .put("title", bounded(item.title, 200)).put("channel", bounded(item.channel, 120)));
            LiveWallpaperPrefs.prefs(context).edit().putString(KEY, array.toString())
                    .putStringSet(LiveWallpaperSelection.FAVORITES, favorites).apply();
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
        return !removed;
    }

    private static String bounded(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
