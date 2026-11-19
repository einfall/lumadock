package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A shared favorite list and a deduplicated union of selected rotation sources. */
final class LiveWallpaperSelection {
    static final String FAVORITES = "favorites", POOL = "random_pool", MODE = "random_mode",
            TRIGGERS = "random_triggers", MINUTES = "random_minutes", CATEGORY = "category",
            SOURCES = "random_sources";

    static Set<String> triggers(Context context) {
        SharedPreferences prefs = LiveWallpaperPrefs.prefs(context);
        return LiveRotationPolicy.triggers(prefs.contains(TRIGGERS)
                ? prefs.getStringSet(TRIGGERS, Collections.emptySet()) : null, prefs.getString(MODE, "off"));
    }

    static void trigger(Context context, String id, boolean on) {
        Set<String> next = LiveRotationPolicy.withTrigger(triggers(context), id, on);
        LiveWallpaperPrefs.prefs(context).edit().putStringSet(TRIGGERS, next)
                .putString(MODE, LiveRotationPolicy.legacyMode(next)).apply();
    }

    static Set<String> favorites(Context context) {
        YouTubeFavorites.mergeLegacy(context);
        return new HashSet<>(LiveWallpaperPrefs.prefs(context).getStringSet(FAVORITES, Collections.emptySet()));
    }

    static boolean isFavorite(Context context, String url) { return favorites(context).contains(url); }

    static boolean toggle(Context context, String url) {
        String id = YouTubeVideo.id(url);
        if (!id.isEmpty()) {
            YouTubeVideo video = new YouTubeVideo(id, "", "");
            for (YouTubeVideo item : YouTubeFavorites.load(context)) if (item.id.equals(id)) video = item;
            return YouTubeFavorites.toggle(context, video);
        }
        Set<String> favorites = favorites(context);
        boolean on = !favorites.remove(url);
        if (on) favorites.add(url);
        LiveWallpaperPrefs.prefs(context).edit().putStringSet(FAVORITES, favorites).apply();
        return on;
    }

    static Set<String> sources(Context context) {
        SharedPreferences prefs = LiveWallpaperPrefs.prefs(context);
        if (!prefs.contains(SOURCES)) {
            Set<String> initial = LiveSourcePolicy.legacy(prefs.getString(POOL, "category"),
                    prefs.getString(CATEGORY, "all"), prefs.getBoolean(YouTubeFavorites.INCLUDE, false));
            prefs.edit().putStringSet(SOURCES, initial).apply();
        }
        return LiveSourcePolicy.normalize(prefs.getStringSet(SOURCES, Collections.emptySet()));
    }

    static void source(Context context, String id, boolean enabled) {
        LiveWallpaperPrefs.prefs(context).edit().putStringSet(SOURCES,
                LiveSourcePolicy.toggle(sources(context), id, enabled)).apply();
    }

    static List<LiveWallpaperCatalog.Item> pool(Context context, String group) {
        SharedPreferences prefs = LiveWallpaperPrefs.prefs(context);
        if ("category".equals(group)) group = prefs.getString(CATEGORY, "all");
        Set<String> favorites = favorites(context);
        List<LiveWallpaperCatalog.Item> result = new ArrayList<>();
        for (LiveWallpaperCatalog.Item item : known(context, favorites)) {
            if ("all".equals(group) || ("favorites".equals(group) && favorites.contains(item.url))
                    || ("C".equals(group) ? item.source.startsWith("C") : group.equals(item.source))) {
                result.add(item);
            }
        }
        return result;
    }

    private static List<LiveWallpaperCatalog.Item> known(Context context, Set<String> favorites) {
        Map<String, LiveWallpaperCatalog.Item> items = new LinkedHashMap<>();
        for (LiveWallpaperCatalog.Item item : LiveWallpaperCatalog.load(context)) items.put(item.url, item);
        for (YouTubeVideo video : YouTubeFavorites.load(context)) {
            items.put(video.url(), new LiveWallpaperCatalog.Item(video.title, video.url(), "youtube"));
        }
        List<String> extra = new ArrayList<>(favorites);
        Collections.sort(extra);
        for (String url : extra) {
            if (items.containsKey(url)) continue;
            String id = YouTubeVideo.id(url);
            if (!id.isEmpty()) items.put(url, new LiveWallpaperCatalog.Item("YouTube · " + id, url, "youtube"));
            else if (LiveWallpaperSource.valid(url)) {
                items.put(url, new LiveWallpaperCatalog.Item("Eigener Live-Hintergrund", url, "custom"));
            }
        }
        return new ArrayList<>(items.values());
    }

    static List<LiveWallpaperCatalog.Item> rotationPool(Context context) {
        Set<String> selected = sources(context), favorites = favorites(context);
        List<LiveWallpaperCatalog.Item> result = new ArrayList<>();
        for (LiveWallpaperCatalog.Item item : known(context, favorites)) {
            if (LiveSourcePolicy.includes(selected, item.source, favorites.contains(item.url))) result.add(item);
        }
        return result;
    }

    static String name(Context context, String group) {
        if (YouTubeFavorites.POOL.equals(group)) return "YouTube";
        if ("category".equals(group)) return name(context,
                LiveWallpaperPrefs.prefs(context).getString(CATEGORY, "all"));
        if ("favorites".equals(group)) return "Favoriten";
        if ("all".equals(group)) return "Alle";
        return LiveWallpaperCatalog.sourceName(group);
    }

    static boolean random(Context context) { return BackgroundSelection.random(context); }
}
