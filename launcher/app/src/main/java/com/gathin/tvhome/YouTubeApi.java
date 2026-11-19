package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.text.Html;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Public metadata only. Keys remain in device-local preferences and never enter layout exports. */
final class YouTubeApi {
    private static final String CREDENTIALS = "youtube_credentials";
    static String key(Context context) {
        return context.getSharedPreferences(CREDENTIALS, 0).getString("key", "");
    }
    static void key(Context context, String key) {
        context.getSharedPreferences(CREDENTIALS, 0).edit().putString("key", key.trim()).apply();
    }
    static final class Page {
        final List<YouTubeVideo> videos = new ArrayList<>();
        String next = "";
    }

    static Page search(Context context, String query, String page) throws Exception {
        String key = key(context);
        if (key.isEmpty()) return publicSearch(context, query);
        String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video"
                + "&videoEmbeddable=true&videoSyndicated=true&safeSearch=moderate&maxResults=24"
                + "&q=" + encode(query) + "&key=" + encode(key) + "&pageToken=" + encode(page);
        JSONObject json = request(context, url, true);
        Page result = new Page();
        result.next = json.optString("nextPageToken", "");
        JSONArray items = json.optJSONArray("items");
        if (items != null) for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String id = item.optJSONObject("id") == null ? "" : item.getJSONObject("id").optString("videoId");
            JSONObject snippet = item.optJSONObject("snippet");
            if (YouTubeVideo.validId(id) && snippet != null) result.videos.add(new YouTubeVideo(id,
                    plain(snippet.optString("title")), plain(snippet.optString("channelTitle"))));
        }
        return result;
    }

    private static Page publicSearch(Context context, String query) throws Exception {
        String html = requestText(context, "https://www.youtube.com/results?hl=de&search_query="
                + encode(query), false, 3 * 1024 * 1024);
        JSONObject data = new JSONObject(YouTubeSearchData.initialData(html));
        Page result = new Page();
        JSONObject contents = data.optJSONObject("contents");
        if (contents != null) {
            JSONObject desktop = contents.optJSONObject("twoColumnSearchResultsRenderer");
            collect(desktop == null ? contents : desktop.opt("primaryContents"), result, new java.util.HashSet<>(), 0);
        }
        return result;
    }

    private static void collect(Object value, Page result, java.util.Set<String> seen, int depth) {
        if (depth > 45 || result.videos.size() >= 60) return;
        if (value instanceof JSONArray) {
            JSONArray items = (JSONArray) value;
            for (int i = 0; i < items.length(); i++) collect(items.opt(i), result, seen, depth + 1);
        } else if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            if (object.has("adSlotRenderer") || object.has("promotedSparklesWebRenderer")) return;
            JSONObject video = object.optJSONObject("videoRenderer");
            if (video != null) {
                String id = video.optString("videoId");
                if (YouTubeVideo.validId(id) && seen.add(id)) result.videos.add(new YouTubeVideo(id,
                        runs(video.optJSONObject("title")), runs(video.optJSONObject("ownerText"))));
                return;
            }
            java.util.Iterator<String> keys = object.keys();
            while (keys.hasNext()) collect(object.opt(keys.next()), result, seen, depth + 1);
        }
    }

    private static String runs(JSONObject text) {
        if (text == null) return "";
        String simple = text.optString("simpleText");
        if (!simple.isEmpty()) return simple;
        StringBuilder result = new StringBuilder();
        JSONArray runs = text.optJSONArray("runs");
        if (runs != null) for (int i = 0; i < runs.length(); i++) {
            JSONObject run = runs.optJSONObject(i);
            if (run != null) result.append(run.optString("text"));
        }
        return result.toString();
    }

    static YouTubeVideo lookup(Context context, String id) throws Exception {
        YouTubeVideo video = new YouTubeVideo(id, "", "");
        JSONObject json = request(context, "https://www.youtube.com/oembed?format=json&url=" + encode(video.url()), false);
        return new YouTubeVideo(id, json.optString("title"), json.optString("author_name"));
    }

    private static String plain(String value) {
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString();
    }
    private static String encode(String value) throws java.io.UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }
    private static JSONObject request(Context context, String url, boolean authenticated) throws Exception {
        return new JSONObject(requestText(context, url, authenticated, 1024 * 1024));
    }
    private static String requestText(Context context, String url, boolean authenticated, int maxBytes) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", authenticated ? "LumaDock/YouTube" : "Mozilla/5.0");
        connection.setRequestProperty("Accept-Language", "de-DE,de;q=0.9");
        if (authenticated) {
            connection.setRequestProperty("X-Android-Package", context.getPackageName());
            connection.setRequestProperty("X-Android-Cert", certificate(context));
        }
        try {
            int status = connection.getResponseCode();
            if (status != 200) {
                String message = "YouTube ist gerade nicht erreichbar (" + status + ").";
                if (authenticated && (status == 400 || status == 401 || status == 403)) {
                    message = "YouTube hat die Anfrage abgelehnt. API-Schlüssel, aktivierte Data API v3 und Kontingent prüfen.";
                }
                if (!authenticated && status == 404) message = "Dieses YouTube-Video ist nicht verfügbar.";
                throw new IOException(message);
            }
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192];
                int count;
                while ((count = input.read(bytes)) != -1) {
                    if (output.size() + count > maxBytes) throw new IOException("YouTube-Antwort ist zu groß.");
                    output.write(bytes, 0, count);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            connection.disconnect();
        }
    }

    @SuppressWarnings("deprecation")
    private static String certificate(Context context) throws Exception {
        PackageManager packages = context.getPackageManager();
        Signature[] signatures = Build.VERSION.SDK_INT >= 28
                ? packages.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES)
                        .signingInfo.getApkContentsSigners()
                : packages.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(signatures[0].toByteArray());
        StringBuilder value = new StringBuilder();
        for (byte b : digest) value.append(String.format(java.util.Locale.ROOT, "%02X", b));
        return value.toString();
    }
}
