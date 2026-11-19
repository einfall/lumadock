package com.gathin.tvhome;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Explicit empty pauses rotation. Legacy category and YouTube choices retain their meaning. */
final class LiveSourcePolicy {
    static final String[] IDS = {"all", "A", "Z", "C", "favorites", "youtube"};

    static Set<String> normalize(Collection<String> saved) {
        Set<String> result = new LinkedHashSet<>();
        for (String id : IDS) if (saved.contains(id)) result.add(id);
        return result;
    }

    static Set<String> legacy(String pool, String category, boolean youtubeIncluded) {
        String source = "category".equals(pool) ? category : pool;
        Set<String> result = normalize(Arrays.asList(source));
        if (result.isEmpty()) result.add("all");
        if (youtubeIncluded && !result.contains("all")) result.add("youtube");
        return result;
    }

    static Set<String> toggle(Set<String> current, String id, boolean enabled) {
        Set<String> result = normalize(current);
        if (!Arrays.asList(IDS).contains(id)) return result;
        if (enabled) {
            if ("all".equals(id)) result.clear();
            else result.remove("all");
            result.add(id);
        } else result.remove(id);
        return result;
    }

    static boolean includes(Set<String> sources, String origin, boolean favorite) {
        if (sources.contains("all") || (favorite && sources.contains("favorites"))) return true;
        if (origin.startsWith("C")) origin = "C";
        return sources.contains(origin);
    }
}
