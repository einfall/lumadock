package com.gathin.tvhome;

import android.os.SystemClock;
import android.util.Xml;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.xmlpull.v1.XmlPullParser;

/** A local UPnP event subscription for the selected room's master volume and mute state. */
final class SonosVolumeEvents implements AutoCloseable {
    private static final int PORT = 1400;
    private static final String EVENT = "/MediaRenderer/RenderingControl/Event";
    private final SonosLan.Player player;
    private final Consumer<SonosLan.Level> changed;
    private final String callbackPath = "/sonos/" + UUID.randomUUID();
    private final Thread worker;
    private volatile ServerSocket callback;
    private volatile boolean closed;

    SonosVolumeEvents(SonosLan.Player player, Consumer<SonosLan.Level> changed) {
        this.player = player;
        this.changed = changed;
        worker = new Thread(this::listen, "luma-sonos-events");
        worker.start();
    }

    private void listen() {
        while (!closed) {
            try {
                subscribe();
            } catch (Exception failure) {
                if (!closed) android.util.Log.d("SonosVolume", "Event subscription unavailable: " + failure.getClass().getSimpleName());
            }
            if (closed) return;
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException stopped) {
                return;
            }
        }
    }

    private void subscribe() throws Exception {
        if (!SonosLan.privateIpv4(player.ip)) return;
        InetAddress speaker = InetAddress.getByName(player.ip);
        SonosLan.Level previous = SonosLan.read(player);
        InetAddress local;
        try (DatagramSocket route = new DatagramSocket()) {
            route.connect(speaker, PORT);
            local = route.getLocalAddress();
        }
        if (!SonosLan.privateIpv4(local.getHostAddress())) throw new IOException("No private IPv4 route");
        String sid = null;
        try (ServerSocket server = new ServerSocket(0, 4, local)) {
            callback = server;
            if (closed) return;
            server.setSoTimeout(1000);
            String address = "http://" + local.getHostAddress() + ":" + server.getLocalPort() + callbackPath;
            Subscription subscription = subscribe(speaker, "CALLBACK: <" + address + ">\r\nNT: upnp:event\r\n");
            sid = subscription.sid;
            long renew = renewal(subscription);
            while (!closed) {
                if (SystemClock.elapsedRealtime() >= renew) {
                    subscription = subscribe(speaker, "SID: " + sid + "\r\n");
                    sid = subscription.sid;
                    renew = renewal(subscription);
                }
                try (Socket event = server.accept()) {
                    if (!speaker.equals(event.getInetAddress())) continue;
                    SonosLan.Level level = notify(event, sid, previous);
                    if (level != null && (level.volume != previous.volume || level.muted != previous.muted)) {
                        previous = level;
                        if (!closed) changed.accept(level);
                    }
                } catch (SocketTimeoutException ignored) {
                    // Check the renewal deadline and lifecycle.
                } catch (Exception invalidEvent) {
                    if (closed) return;
                }
            }
        } finally {
            callback = null;
            if (sid != null) {
                try { request(speaker, "UNSUBSCRIBE", "SID: " + sid + "\r\n"); }
                catch (IOException ignored) {}
            }
        }
    }

    private static long renewal(Subscription subscription) {
        return SystemClock.elapsedRealtime() + subscription.seconds * 700L;
    }

    private static Subscription subscribe(InetAddress speaker, String headers) throws IOException {
        Map<String, String> response = request(speaker, "SUBSCRIBE", headers + "TIMEOUT: Second-300\r\n");
        String sid = response.get("sid");
        if (sid == null || !sid.matches("uuid:[A-Za-z0-9_-]{1,128}")) throw new IOException("Invalid subscription");
        String timeout = response.getOrDefault("timeout", "Second-300");
        int seconds = 300;
        if (timeout.matches("(?i)second-[0-9]{1,8}")) seconds = Integer.parseInt(timeout.substring(7));
        return new Subscription(sid, Math.max(2, Math.min(300, seconds)));
    }

    private static final class Subscription {
        final String sid;
        final int seconds;
        Subscription(String sid, int seconds) { this.sid = sid; this.seconds = seconds; }
    }

    private static Map<String, String> request(InetAddress host, String verb, String headers) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, PORT), 850);
            socket.setSoTimeout(1200);
            send(socket, verb + " " + EVENT + " HTTP/1.1\r\nHost: " + host.getHostAddress() + ":" + PORT
                    + "\r\n" + headers + "Content-Length: 0\r\nConnection: close\r\n\r\n");
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            if (!line(input).matches("HTTP/1\\.[01] 200(?: .*)?")) throw new IOException("Sonos event request failed");
            return headers(input);
        }
    }

    private SonosLan.Level notify(Socket socket, String sid, SonosLan.Level previous) throws Exception {
        socket.setSoTimeout(850);
        BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
        if (!line(input).startsWith("NOTIFY " + callbackPath + " HTTP/1.")) return null;
        Map<String, String> headers = headers(input);
        if (!sid.equals(headers.get("sid"))) return null;
        int length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
        if (length <= 0 || length > 32 * 1024) return null;
        byte[] body = new byte[length];
        int read = 0;
        while (read < length) {
            int count = input.read(body, read, length - read);
            if (count < 0) throw new EOFException();
            read += count;
        }
        send(socket, "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
        String last = SonosLan.elements(body, "LastChange").get("LastChange");
        return last == null ? null : state(last, previous);
    }

    static SonosLan.Level state(String change, SonosLan.Level previous) throws Exception {
        XmlPullParser xml = Xml.newPullParser();
        xml.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
        xml.setInput(new java.io.StringReader(change));
        boolean master = false;
        int volume = previous.volume;
        boolean muted = previous.muted;
        for (int event = xml.next(); event != XmlPullParser.END_DOCUMENT; event = xml.next()) {
            if (event == XmlPullParser.END_TAG && "InstanceID".equals(xml.getName())) master = false;
            if (event != XmlPullParser.START_TAG) continue;
            String value = xml.getAttributeValue(null, "val");
            if ("InstanceID".equals(xml.getName())) master = "0".equals(value);
            if (!master || !"Master".equals(xml.getAttributeValue(null, "channel"))) continue;
            if ("Volume".equals(xml.getName()) && value != null && value.matches("[0-9]{1,3}")) {
                int parsed = Integer.parseInt(value);
                if (parsed <= 100) volume = parsed;
            }
            if ("Mute".equals(xml.getName()) && ("0".equals(value) || "1".equals(value))) muted = "1".equals(value);
        }
        return new SonosLan.Level(volume, muted);
    }

    private static Map<String, String> headers(BufferedInputStream input) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            String line = line(input);
            if (line.isEmpty()) return result;
            int separator = line.indexOf(':');
            if (separator < 1) throw new IOException("Invalid event header");
            result.put(line.substring(0, separator).toLowerCase(Locale.ROOT), line.substring(separator + 1).trim());
        }
        throw new IOException("Too many event headers");
    }

    private static String line(BufferedInputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < 4096; i++) {
            int value = input.read();
            if (value < 0) throw new EOFException();
            if (value == '\n') return output.toString("US-ASCII").trim();
            if (value != '\r') output.write(value);
        }
        throw new IOException("Event line too long");
    }

    private static void send(Socket socket, String message) throws IOException {
        socket.getOutputStream().write(message.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
    }

    @Override public void close() {
        closed = true;
        worker.interrupt();
        ServerSocket server = callback;
        if (server != null) {
            try { server.close(); }
            catch (IOException ignored) {}
        }
    }
}
