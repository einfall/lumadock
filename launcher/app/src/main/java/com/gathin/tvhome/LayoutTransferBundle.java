package com.gathin.tvhome;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Media is streamed into a private directory. The sender cannot choose destination paths. */
final class LayoutTransferBundle {
    private static final int MAX_MEDIA_BYTES = 256 * 1024 * 1024;
    private static final int CHUNK = 32 * 1024;
    private static final String MEDIA_DIR = "luma-transfer-media";
    private static final Slot[] SLOTS = {
            new Slot("live", "source", "live.mp4", "video"),
            new Slot("saver", SaverPrefs.IMAGE, "saver.img", "image"),
            new Slot("saver", SaverPrefs.VIDEO, "saver.mp4", "video")
    };

    private LayoutTransferBundle() {}

    static void send(Context context, DataOutputStream output) throws IOException, JSONException {
        JSONObject snapshot = DesignSnapshot.captureTransfer(context);
        String[] local = new String[SLOTS.length];
        for (int i = 0; i < SLOTS.length; i++) {
            Slot slot = SLOTS[i];
            JSONObject settings = snapshot.getJSONObject(slot.group);
            String source = settings.optString(slot.key);
            if (!source.isEmpty() && !isWeb(source)) {
                local[i] = source;
                settings.remove(slot.key);
            }
        }
        JSONObject live = snapshot.getJSONObject("live");
        JSONArray favorites = live.optJSONArray(LiveWallpaperSelection.FAVORITES);
        int skipped = 0;
        if (favorites != null) {
            JSONArray portable = new JSONArray();
            for (int i = 0; i < favorites.length(); i++) {
                String source = favorites.getString(i);
                if (isWeb(source)) portable.put(source);
                else skipped++;
            }
            live.put(LiveWallpaperSelection.FAVORITES, portable);
        }
        byte[] json = snapshot.toString().getBytes(StandardCharsets.UTF_8);
        if (json.length > DesignSnapshot.MAX_BYTES) throw new IOException("Die Einrichtung ist zu groß.");
        output.writeInt(json.length);
        output.write(json);
        output.writeInt(skipped);
        int remaining = MAX_MEDIA_BYTES;
        byte[] buffer = new byte[CHUNK];
        for (int i = 0; i < SLOTS.length; i++) {
            output.writeBoolean(local[i] != null);
            if (local[i] == null) continue;
            try (InputStream media = openMedia(context, local[i], SLOTS[i])) {
                if (media != null) {
                    int read;
                    while ((read = readMedia(media, buffer)) > 0 && read <= remaining) {
                        output.writeInt(read);
                        output.write(buffer, 0, read);
                        remaining -= read;
                    }
                    output.writeInt(read == 0 ? 0 : -1);
                } else {
                    output.writeInt(-1);
                }
            }
        }
        output.flush();
    }

    static Received receive(Context context, DataInputStream input) throws IOException, JSONException {
        int length = input.readInt();
        if (length < 1 || length > DesignSnapshot.MAX_BYTES) throw new IOException("Ungültige Einrichtung.");
        byte[] json = new byte[length];
        input.readFully(json);
        JSONObject snapshot = DesignSnapshot.decode(new String(json, StandardCharsets.UTF_8));
        snapshot.getJSONObject("transfer");
        File directory = new File(new File(context.getFilesDir(), MEDIA_DIR), UUID.randomUUID().toString());
        Received result = new Received(snapshot, directory);
        int remaining = MAX_MEDIA_BYTES;
        byte[] buffer = new byte[CHUNK];
        try {
            result.skipped = input.readInt();
            if (result.skipped < 0 || result.skipped > DesignSnapshot.MAX_BYTES) {
                throw new IOException("Ungültige Favoritenliste.");
            }
            JSONObject live = snapshot.getJSONObject("live");
            JSONArray favorites = live.optJSONArray(LiveWallpaperSelection.FAVORITES);
            if (favorites != null) {
                for (int i = 0; i < favorites.length(); i++) {
                    if (!isWeb(favorites.getString(i))) throw new IOException("Ungültiger Hintergrundfavorit.");
                }
            }
            for (Slot slot : SLOTS) {
                JSONObject settings = snapshot.getJSONObject(slot.group);
                if (settings.has(slot.key) && !isWeb(settings.getString(slot.key))) {
                    throw new IOException("Ungültige Medienadresse.");
                }
                if (!input.readBoolean()) continue;
                File file = new File(directory, slot.name);
                int size = input.readInt();
                if (size > 0) {
                    if (!directory.isDirectory() && !directory.mkdirs()) {
                        throw new IOException("Medienordner konnte nicht erstellt werden.");
                    }
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        while (size > 0) {
                            if (size > buffer.length || size > remaining) throw new IOException("Medien sind zu groß.");
                            input.readFully(buffer, 0, size);
                            output.write(buffer, 0, size);
                            remaining -= size;
                            size = input.readInt();
                        }
                    }
                }
                if (size == 0 && file.isFile()) {
                    settings.put(slot.key, file.getAbsolutePath());
                } else {
                    file.delete();
                    settings.remove(slot.key);
                    result.skipped++;
                }
            }
            if (!live.has("source")) {
                live.remove("enabled");
                live.remove("title");
            }
            JSONObject saver = snapshot.getJSONObject("saver");
            if (("image".equals(saver.optString(SaverPrefs.MODE)) && !saver.has(SaverPrefs.IMAGE))
                    || ("video".equals(saver.optString(SaverPrefs.MODE)) && !saver.has(SaverPrefs.VIDEO))) {
                saver.remove(SaverPrefs.MODE);
            }
            if (!saver.has(SaverPrefs.VIDEO)) saver.remove(SaverPrefs.TITLE);
            return result;
        } catch (IOException | JSONException | RuntimeException failure) {
            result.discard();
            throw failure;
        }
    }

    private static boolean isWeb(String source) {
        if (!LiveWallpaperSource.network(source) || !LiveWallpaperSource.valid(source)) return false;
        Uri uri = Uri.parse(source);
        return uri.getHost() != null && uri.getUserInfo() == null;
    }

    private static int readMedia(InputStream input, byte[] bytes) {
        try {
            int count = input.read(bytes);
            return count == -1 ? 0 : count;
        } catch (IOException unavailable) {
            return -1;
        }
    }

    @SuppressWarnings("deprecation")
    private static InputStream openMedia(Context context, String source, Slot slot) {
        try {
            Uri uri = Uri.parse(source);
            if ("content".equals(uri.getScheme())) {
                String type = context.getContentResolver().getType(uri);
                return type != null && type.startsWith(slot.mime + "/")
                        ? context.getContentResolver().openInputStream(uri) : null;
            }
            String path = "file".equals(uri.getScheme()) ? uri.getPath() : source;
            if (path == null || !path.startsWith("/")) return null;
            File file = new File(path).getCanonicalFile();
            File imported = new File(context.getFilesDir(), MEDIA_DIR).getCanonicalFile();
            String external = Environment.getExternalStorageDirectory().getCanonicalPath() + File.separator;
            String absolute = file.getPath();
            boolean externalMedia = absolute.startsWith(external)
                    && !absolute.substring(external.length()).toLowerCase(Locale.ROOT).startsWith("android/")
                    && slot.extension(file.getName());
            if ((externalMedia || (absolute.startsWith(imported.getPath() + File.separator)
                    && file.getName().equals(slot.name))) && file.isFile() && file.length() <= MAX_MEDIA_BYTES) {
                return new FileInputStream(file);
            }
        } catch (IOException | RuntimeException unavailable) {
            return null;
        }
        return null;
    }

    static final class Received {
        final JSONObject snapshot;
        private final File directory;
        int skipped;

        Received(JSONObject snapshot, File directory) {
            this.snapshot = snapshot;
            this.directory = directory;
        }

        void discard() {
            for (Slot slot : SLOTS) new File(directory, slot.name).delete();
            directory.delete();
        }
    }

    private static final class Slot {
        final String group;
        final String key;
        final String name;
        final String mime;

        Slot(String group, String key, String name, String mime) {
            this.group = group;
            this.key = key;
            this.name = name;
            this.mime = mime;
        }

        boolean extension(String filename) {
            String value = filename.toLowerCase(Locale.ROOT);
            String[] extensions = "video".equals(mime)
                    ? new String[]{".mp4", ".m4v", ".webm", ".mov", ".mkv"}
                    : new String[]{".jpg", ".jpeg", ".png", ".webp", ".bmp", ".heic"};
            for (String extension : extensions) if (value.endsWith(extension)) return true;
            return false;
        }
    }
}
