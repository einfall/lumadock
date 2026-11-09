package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class EchoConnection {
    private static final String ALIAS = "LumaDock.EchoToken";
    private static final String TOKEN = "sealed_token";
    private final SharedPreferences prefs;

    EchoConnection(Context context) {
        prefs = context.getSharedPreferences("echo_connection", Context.MODE_PRIVATE);
    }

    String address() { return prefs.getString("address", ""); }
    String entity() { return prefs.getString("entity", ""); }
    boolean configured() { return prefs.contains(TOKEN); }

    static URI base(String input) {
        try {
            URI uri = URI.create(input.trim()).normalize();
            String host = uri.getHost();
            String scheme = uri.getScheme();
            int port = uri.getPort();
            if (host == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || port == 0 || port > 65535) return null;
            if (!"https".equals(scheme) && !("http".equals(scheme) && privateIpv4(host))) return null;
            String base = uri.toASCIIString();
            while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            return URI.create(base + "/");
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static boolean privateIpv4(String host) {
        if (!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) return false;
        String[] parts = host.split("\\.");
        for (String part : parts) {
            if ((part.length() > 1 && part.startsWith("0")) || Integer.parseInt(part) > 255) return false;
        }
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first == 10 || first == 127 || (first == 192 && second == 168)
                || (first == 172 && second >= 16 && second <= 31);
    }

    static boolean validEntity(String value) {
        return value.length() <= 128 && value.matches("media_player\\.[a-z0-9_]+");
    }

    static URI cover(URI base, String path) {
        try {
            if (path.isEmpty() || path.length() > 2048) return null;
            URI uri = base.resolve(path);
            if (!base.getScheme().equals(uri.getScheme()) || uri.getHost() == null
                    || !base.getHost().equalsIgnoreCase(uri.getHost()) || port(base) != port(uri)
                    || uri.getUserInfo() != null || uri.getFragment() != null) return null;
            return uri;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equals(uri.getScheme()) ? 443 : 80;
    }

    static boolean validToken(String value) {
        return value.matches("[A-Za-z0-9._~-]{12,4096}");
    }

    boolean canReuseToken(URI base) {
        return configured() && base.toASCIIString().equals(address());
    }

    void save(URI base, String entity, String token) throws Exception {
        String address = base.toASCIIString();
        SharedPreferences.Editor edit = prefs.edit().putString("address", address).putString("entity", entity);
        if (!token.isEmpty()) {
            if (!validToken(token)) throw new IllegalArgumentException("Invalid token");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(true));
            cipher.updateAAD(address.getBytes(StandardCharsets.UTF_8));
            edit.putString(TOKEN, b64(cipher.getIV()) + "." + b64(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8))));
        } else if (!configured() || !address.equals(address())) {
            throw new IllegalArgumentException("A token is required for this address");
        }
        if (!edit.commit()) throw new java.io.IOException("Could not save Echo connection");
    }

    String token(String address) throws Exception {
        String[] parts = prefs.getString(TOKEN, "").split("\\.", -1);
        if (parts.length != 2) throw new IllegalStateException("Missing token");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(false), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        cipher.updateAAD(address.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }

    void clear() { prefs.edit().clear().apply(); }

    private static String b64(byte[] value) {
        return Base64.encodeToString(value, Base64.NO_WRAP);
    }

    private static SecretKey key(boolean create) throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        if (!create) throw new IllegalStateException("Missing encryption key");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
