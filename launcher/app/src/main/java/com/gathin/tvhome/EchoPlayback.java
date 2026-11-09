package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

final class EchoPlayback {
    final String title;
    final String detail;
    final String status;
    final String state;
    final Bitmap cover;
    final EchoTiming timing;
    private final String artworkKey;

    private EchoPlayback(String title, String detail, String status, String state,
                         Bitmap cover, String artworkKey, EchoTiming timing) {
        this.title = title;
        this.detail = detail;
        this.status = status;
        this.state = state;
        this.cover = cover;
        this.artworkKey = artworkKey;
        this.timing = timing;
    }

    static EchoPlayback example() {
        return new EchoPlayback("Beispieltitel", "Interpret", "Wird abgespielt", "playing", null, "",
                EchoTiming.from(240, 74, "", 0, false));
    }

    static EchoPlayback status(String message) {
        return new EchoPlayback("", "", message, "", null, "", EchoTiming.UNKNOWN);
    }

    static EchoPlayback read(URI base, String entity, String token, EchoPlayback previous) throws Exception {
        byte[] bytes = download(base.resolve("api/states/" + entity), token, 128 * 1024, false);
        JSONObject response = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!entity.equals(response.optString("entity_id"))) throw new IOException("Wrong entity");
        String state = response.optString("state");
        if ("unavailable".equals(state) || "unknown".equals(state)) return status("Echo nicht erreichbar");
        if (!"playing".equals(state) && !"paused".equals(state) && !"buffering".equals(state)) {
            return status("Keine Wiedergabe");
        }

        JSONObject attributes = response.optJSONObject("attributes");
        if (attributes == null) return status("Keine Titelinformationen");
        String title = label(attributes, "media_title");
        String detail = label(attributes, "media_artist");
        if (detail.isEmpty()) detail = label(attributes, "media_album_name");
        String device = label(attributes, "friendly_name");
        String status = "paused".equals(state) ? "Pausiert" : "buffering".equals(state) ? "Lädt …" : "Wird abgespielt";
        if (!device.isEmpty()) status += " · " + device;
        if (title.isEmpty()) title = "Keine Titelinformationen";

        String picture = attributes.optString("entity_picture", "");
        URI artwork = EchoConnection.cover(base, picture);
        if (artwork == null && !picture.isEmpty()) {
            // Home Assistant can serve remote cover art through its own authenticated endpoint.
            artwork = base.resolve("api/media_player_proxy/" + entity);
        }
        String artworkKey = entity + "\n" + picture;
        Bitmap cover = null;
        if (artwork != null) {
            if (artworkKey.equals(previous.artworkKey) && previous.cover != null) cover = previous.cover;
            else {
                try {
                    cover = decodeCover(download(artwork, token, 1024 * 1024, true));
                } catch (IOException ignored) {
                    // The song remains usable when its cover is missing.
                }
            }
        }
        EchoTiming timing = EchoTiming.from(attributes.optDouble("media_duration", -1),
                attributes.optDouble("media_position", -1), attributes.optString("media_position_updated_at", ""),
                System.currentTimeMillis(), "playing".equals(state));
        return new EchoPlayback(title, detail, status, state, cover, artworkKey, timing);
    }

    static final class Player {
        final String id;
        final String name;

        Player(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static List<Player> players(URI base, String token) throws Exception {
        byte[] bytes = download(base.resolve("api/states"), token, 4 * 1024 * 1024, false);
        JSONArray states = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < states.length(); i++) {
            JSONObject state = states.optJSONObject(i);
            if (state == null) continue;
            String id = state.optString("entity_id");
            if (!EchoConnection.validEntity(id)) continue;
            JSONObject attributes = state.optJSONObject("attributes");
            String name = attributes == null ? "" : label(attributes, "friendly_name");
            players.add(new Player(id, name.isEmpty() ? id : name));
        }
        players.sort(Comparator.comparing((Player player) -> player.name.toLowerCase(Locale.ROOT))
                .thenComparing(player -> player.id));
        return players;
    }

    private static String label(JSONObject object, String key) {
        return object.isNull(key) ? "" : Cards.cleanLabel(object.optString(key), "");
    }

    private static Bitmap decodeCover(byte[] bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (options.outWidth <= 0 || options.outHeight <= 0
                || options.outWidth > 4096 || options.outHeight > 4096) return null;
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 320) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    private static byte[] download(URI uri, String token, int limit, boolean image) throws IOException {
        if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        try {
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(4500);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", image ? "image/*" : "application/json");
            int response = connection.getResponseCode();
            if (response != 200) throw new HttpError(response);
            String type = connection.getContentType();
            if (image && (type == null || !type.startsWith("image/"))) throw new IOException("Invalid cover");
            if (connection.getContentLengthLong() > limit) throw new IOException("Response too large");
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > limit) throw new IOException("Response too large");
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    static final class HttpError extends IOException {
        final int code;
        HttpError(int code) { this.code = code; }
    }
}
