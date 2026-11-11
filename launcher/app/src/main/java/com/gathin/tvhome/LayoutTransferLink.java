package com.gathin.tvhome;

import android.content.Context;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import javax.net.ssl.X509TrustManager;
import org.json.JSONException;

/** A short-lived, explicitly paired local connection. The code includes a TLS certificate pin. */
final class LayoutTransferLink {
    static final int WINDOW_MS = 5 * 60_000;
    private static final int MAGIC = 0x4c444b31; // LDK1
    private static final int PIN_LENGTH = 12;
    private static final int CODE_LENGTH = PIN_LENGTH + 8;
    private static final int TIMEOUT = 15_000;
    private static final String[] PROTOCOLS = {"TLSv1.2"};

    private LayoutTransferLink() {}

    static String code(String input) {
        String cleaned = input.replace("-", "").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return cleaned.matches("[0-9A-F]{" + CODE_LENGTH + "}") ? cleaned : null;
    }

    static boolean local(InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()) return false;
        byte[] bytes = address.getAddress();
        return address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || (bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc);
    }

    static LayoutTransferBundle.Received receive(Context context, InetAddress address, int port, String code)
            throws IOException, GeneralSecurityException, JSONException {
        if (!local(address)) throw new IOException("Das Gerät muss im Heimnetz erreichbar sein.");
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, new TrustManager[]{new PairingTrust(code.substring(0, PIN_LENGTH))}, null);
        try (SSLSocket socket = (SSLSocket) tls.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(address, port), TIMEOUT);
            socket.setSoTimeout(TIMEOUT);
            socket.setEnabledProtocols(PROTOCOLS);
            socket.startHandshake();
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            output.writeInt(MAGIC);
            output.write(code.substring(PIN_LENGTH).getBytes(StandardCharsets.US_ASCII));
            output.flush();
            if (!input.readBoolean()) throw new IOException("Der Freigabecode stimmt nicht.");
            LayoutTransferBundle.Received result = LayoutTransferBundle.receive(context, input);
            try {
                output.writeBoolean(true);
                output.flush();
                return result;
            } catch (IOException failure) {
                result.discard();
                throw failure;
            }
        }
    }

    static final class Share implements Closeable {
        private final SSLServerSocket listener;
        private final String pairing;
        private Socket active;

        Share() throws IOException, GeneralSecurityException {
            // Keep the throwaway TLS key in memory. Some TVs cannot use AndroidKeyStore keys
            // with Conscrypt's native server-side signature operations.
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair key = generator.generateKeyPair();
            X509Certificate certificate = LayoutTransferCertificate.create(key);
            byte[] secret = new byte[4];
            new SecureRandom().nextBytes(secret);
            pairing = pin(certificate) + hex(secret);

            char[] password = new char[0];
            KeyStore keys = KeyStore.getInstance("PKCS12");
            keys.load(null, password);
            keys.setKeyEntry("pairing", key.getPrivate(), password, new X509Certificate[]{certificate});
            KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            managers.init(keys, password);
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(managers.getKeyManagers(), null, null);
            listener = (SSLServerSocket) tls.getServerSocketFactory().createServerSocket(0);
            listener.setEnabledProtocols(PROTOCOLS);
        }

        String displayCode() {
            return pairing.replaceAll("(.{4})(?!$)", "$1-");
        }

        int port() {
            return listener.getLocalPort();
        }

        boolean serve(Context context) throws IOException, JSONException {
            byte[] expected = pairing.substring(PIN_LENGTH).getBytes(StandardCharsets.US_ASCII);
            int failures = 0;
            while (failures < 5 && !listener.isClosed()) {
                try (SSLSocket socket = (SSLSocket) listener.accept()) {
                    synchronized (this) {
                        if (listener.isClosed()) return false;
                        active = socket;
                    }
                    if (!local(socket.getInetAddress())) continue;
                    socket.setSoTimeout(TIMEOUT);
                    boolean paired = false;
                    try {
                        socket.startHandshake();
                        DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                        if (input.readInt() != MAGIC) continue;
                        byte[] supplied = new byte[expected.length];
                        input.readFully(supplied);
                        paired = MessageDigest.isEqual(expected, supplied);
                        output.writeBoolean(paired);
                        output.flush();
                        if (paired) {
                            LayoutTransferBundle.send(context, output);
                            return input.readBoolean();
                        }
                        failures++;
                    } catch (IOException failure) {
                        if (paired) throw failure;
                        if (!listener.isClosed()) Log.d("LumaDock.Transfer", "Pairing connection ended", failure);
                    }
                } finally {
                    synchronized (this) {
                        active = null;
                    }
                }
            }
            return false;
        }

        @Override
        public synchronized void close() {
            closeQuietly(listener);
            closeQuietly(active);
        }
    }

    private static void closeQuietly(Closeable socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing is also used to interrupt a blocked read.
        }
    }

    private static String pin(X509Certificate cert) throws GeneralSecurityException {
        return hex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded())).substring(0, PIN_LENGTH);
    }

    private static String hex(byte[] bytes) {
        StringBuilder output = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) output.append(String.format(Locale.ROOT, "%02X", value & 0xff));
        return output.toString();
    }

    private static final class PairingTrust implements X509TrustManager {
        private final String expected;

        PairingTrust(String expected) {
            this.expected = expected;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String type) throws CertificateException {
            if (chain == null || chain.length != 1) throw new CertificateException("Invalid pairing certificate");
            chain[0].checkValidity();
            try {
                if (!expected.equals(pin(chain[0]))) throw new CertificateException("Pairing certificate mismatch");
            } catch (GeneralSecurityException failure) {
                throw new CertificateException(failure);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String type) throws CertificateException {
            throw new CertificateException("Client certificates are not used for pairing");
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
