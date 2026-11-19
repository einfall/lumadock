package com.gathin.tvhome;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** One canonical identity for pasted links, API results, favorites and playback. */
final class YouTubeVideo {
    final String id;
    final String title;
    final String channel;

    YouTubeVideo(String id, String title, String channel) {
        if (!validId(id)) throw new IllegalArgumentException("Invalid YouTube video ID");
        this.id = id;
        this.title = title == null || title.trim().isEmpty() ? "YouTube · " + id : title;
        this.channel = channel == null ? "" : channel;
    }

    String url() { return "https://www.youtube.com/watch?v=" + id; }
    String thumbnail() { return "https://i.ytimg.com/vi/" + id + "/mqdefault.jpg"; }
    static boolean validId(String id) { return id != null && id.matches("[A-Za-z0-9_-]{11}"); }

    static String id(String input) {
        if (input == null) return "";
        String value = input.trim();
        if (validId(value)) return value;
        if (value.length() > 2048) return "";
        try {
            URI uri = new URI(value);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getUserInfo() != null || uri.getPort() != -1) return "";
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.ROOT);
            String path = uri.getPath();
            String candidate = "";
            if ("youtu.be".equals(host)) candidate = path.substring(1);
            else if (host.equals("youtube.com") || host.endsWith(".youtube.com")
                    || host.equals("www.youtube-nocookie.com")) {
                String[] parts = path.split("/");
                if (parts.length == 3 && ("embed".equals(parts[1]) || "shorts".equals(parts[1])
                        || "live".equals(parts[1]))) candidate = parts[2];
                else if ("/watch".equals(path) && uri.getRawQuery() != null) {
                    for (String pair : uri.getRawQuery().split("&")) {
                        if (pair.startsWith("v=")) {
                            candidate = URLDecoder.decode(pair.substring(2), StandardCharsets.UTF_8.name());
                            break;
                        }
                    }
                }
            }
            return validId(candidate) ? candidate : "";
        } catch (Exception invalid) {
            return "";
        }
    }
}
