package com.gathin.tvhome;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Xml;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.xmlpull.v1.XmlPullParser;

/** Local Sonos discovery and RenderingControl. All requests target private IPv4 speakers on port 1400. */
final class SonosLan {
    private static final String ZONE = "urn:schemas-upnp-org:device:ZonePlayer:1";
    private static final String SERVICE = "urn:schemas-upnp-org:service:RenderingControl:1";
    private static final String DESCRIPTION = "/xml/device_description.xml";
    private static final String CONTROL = "/MediaRenderer/RenderingControl/Control";
    private static final String MASTER = "<InstanceID>0</InstanceID><Channel>Master</Channel>";
    private static final String SEARCH = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n"
            + "MAN: \"ssdp:discover\"\r\nMX: 1\r\nST: " + ZONE + "\r\n\r\n";

    static final class Player {
        final String ip, id, label;
        Player(String ip, String id, String label) {
            this.ip = ip;
            this.id = id;
            this.label = label;
        }
    }

    static final class Level {
        final int volume;
        final boolean muted;
        Level(int volume, boolean muted) {
            this.volume = volume;
            this.muted = muted;
        }
    }

    static List<Player> discover(Context context) throws InterruptedException {
        Map<String, Player> devices = new LinkedHashMap<>();
        for (String host : multicast(context)) {
            Player player = probe(host, 650);
            if (player != null) devices.put(player.id, player);
        }
        if (devices.isEmpty()) {
            List<Callable<Player>> tasks = new ArrayList<>();
            for (String host : nearbyHosts(context)) tasks.add(() -> probe(host, 220));
            if (!tasks.isEmpty()) {
                ExecutorService probes = Executors.newFixedThreadPool(16);
                try {
                    for (Future<Player> response : probes.invokeAll(tasks, 5, TimeUnit.SECONDS)) {
                        if (response.isCancelled()) continue;
                        try {
                            Player player = response.get();
                            if (player != null) devices.put(player.id, player);
                        } catch (java.util.concurrent.ExecutionException ignored) {}
                    }
                } finally {
                    probes.shutdownNow();
                }
            }
        }
        List<Player> result = new ArrayList<>(devices.values());
        result.sort(Comparator.comparing(player -> player.label, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static Set<String> multicast(Context context) {
        Set<String> hosts = new LinkedHashSet<>();
        WifiManager.MulticastLock lock = null;
        try {
            WifiManager wifi = context.getApplicationContext().getSystemService(WifiManager.class);
            if (wifi != null) {
                lock = wifi.createMulticastLock("luma-sonos");
                lock.acquire();
            }
        } catch (RuntimeException ignored) {}
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(250);
            byte[] query = SEARCH.getBytes(StandardCharsets.US_ASCII);
            socket.send(new DatagramPacket(query, query.length, InetAddress.getByName("239.255.255.250"), 1900));
            long until = SystemClock.elapsedRealtime() + 1700;
            while (hosts.size() < 16 && SystemClock.elapsedRealtime() < until && !Thread.currentThread().isInterrupted()) {
                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException ignored) {
                    continue;
                }
                String headers = new String(buffer, 0, packet.getLength(), StandardCharsets.US_ASCII);
                String ip = packet.getAddress().getHostAddress();
                if (privateIpv4(ip) && headers.toLowerCase(Locale.ROOT).contains(ZONE.toLowerCase(Locale.ROOT))) hosts.add(ip);
            }
        } catch (IOException ignored) {
            // Use the active network's local IPv4 subnet if SSDP is unavailable.
        } finally {
            if (lock != null && lock.isHeld()) lock.release();
        }
        return hosts;
    }

    private static Set<String> nearbyHosts(Context context) {
        Set<String> hosts = new LinkedHashSet<>();
        ConnectivityManager connectivity = context.getSystemService(ConnectivityManager.class);
        if (connectivity == null || connectivity.getActiveNetwork() == null) return hosts;
        LinkProperties link = connectivity.getLinkProperties(connectivity.getActiveNetwork());
        if (link == null) return hosts;
        for (LinkAddress address : link.getLinkAddresses()) {
            InetAddress own = address.getAddress();
            if (!(own instanceof Inet4Address) || !own.isSiteLocalAddress()) continue;
            byte[] bytes = own.getAddress();
            int prefix = Math.max(24, address.getPrefixLength());
            int count = 1 << (32 - prefix);
            int local = bytes[3] & 255;
            int base = local & ~(count - 1);
            for (int offset = 1; offset < count - 1; offset++) {
                if (base + offset == local) continue;
                hosts.add((bytes[0] & 255) + "." + (bytes[1] & 255) + "." + (bytes[2] & 255) + "." + (base + offset));
            }
            break;
        }
        return hosts;
    }

    static Player identify(String ip) throws Exception {
        return identify(ip, 650);
    }

    private static Player probe(String ip, int timeout) {
        try {
            return identify(ip, timeout);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Player identify(String ip, int timeout) throws Exception {
        Map<String, String> fields = elements(request(ip, DESCRIPTION, null, null, timeout),
                "deviceType", "manufacturer", "UDN", "roomName", "modelName", "friendlyName");
        String id = fields.get("UDN");
        if (!ZONE.equals(fields.get("deviceType")) || id == null || !id.matches("uuid:RINCON_[A-Za-z0-9_-]{8,64}")
                || !fields.getOrDefault("manufacturer", "").toLowerCase(Locale.ROOT).contains("sonos")) {
            throw new IOException("No Sonos player");
        }
        String model = fields.getOrDefault("modelName", "Sonos");
        String room = Cards.cleanLabel(fields.getOrDefault("roomName", fields.get("friendlyName")), model);
        return new Player(ip, id, room.equals(model) ? room : room + " · " + Cards.cleanLabel(model, "Sonos"));
    }

    static Level read(Player player) throws Exception {
        verify(player);
        return readVerified(player);
    }

    static Level adjust(Player player, int steps, boolean toggleMute) throws Exception {
        verify(player);
        Level level = readVerified(player);
        if (toggleMute) {
            soap(player.ip, "SetMute", "<DesiredMute>" + (level.muted ? 0 : 1) + "</DesiredMute>");
        } else {
            int target = Math.max(0, Math.min(100, level.volume + steps));
            if (target != level.volume) soap(player.ip, "SetVolume", "<DesiredVolume>" + target + "</DesiredVolume>");
        }
        return readVerified(player);
    }

    private static void verify(Player player) throws Exception {
        if (!player.id.equals(identify(player.ip).id)) throw new IOException("Sonos player changed");
    }

    private static Level readVerified(Player player) throws Exception {
        String volume = elements(soap(player.ip, "GetVolume", ""), "CurrentVolume").get("CurrentVolume");
        String mute = elements(soap(player.ip, "GetMute", ""), "CurrentMute").get("CurrentMute");
        int level = Integer.parseInt(volume);
        if (level < 0 || level > 100 || !("0".equals(mute) || "1".equals(mute))) throw new IOException("Invalid Sonos level");
        return new Level(level, "1".equals(mute));
    }

    private static byte[] soap(String ip, String action, String args) throws IOException {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><u:"
                + action + " xmlns:u=\"" + SERVICE + "\">" + MASTER + args + "</u:" + action + "></s:Body></s:Envelope>";
        return request(ip, CONTROL, action, body.getBytes(StandardCharsets.UTF_8), 650);
    }

    private static byte[] request(String ip, String path, String action, byte[] body, int timeout) throws IOException {
        if (!privateIpv4(ip)) throw new IOException("Not a private IPv4 address");
        HttpURLConnection connection = (HttpURLConnection) new URL("http", ip, 1400, path).openConnection(Proxy.NO_PROXY);
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(800);
            if (body != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
                connection.setRequestProperty("SOAPACTION", "\"" + SERVICE + "#" + action + "\"");
                try (java.io.OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
            }
            if (connection.getResponseCode() != 200) throw new IOException("Sonos request failed");
            try (InputStream input = connection.getInputStream()) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    if (output.size() + length > 64 * 1024) throw new IOException("Sonos reply too large");
                    output.write(buffer, 0, length);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    static Map<String, String> elements(byte[] xml, String... names) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
        parser.setInput(new java.io.ByteArrayInputStream(xml), null);
        Set<String> wanted = new LinkedHashSet<>(java.util.Arrays.asList(names));
        Map<String, String> values = new LinkedHashMap<>();
        for (int event = parser.next(); event != XmlPullParser.END_DOCUMENT && !wanted.isEmpty(); event = parser.next()) {
            if (event == XmlPullParser.START_TAG && wanted.remove(parser.getName())) values.put(parser.getName(), parser.nextText().trim());
        }
        return values;
    }

    static boolean privateIpv4(String value) {
        if (value == null || !value.matches("(?:0|[1-9][0-9]{0,2})(?:\\.(?:0|[1-9][0-9]{0,2})){3}")) return false;
        String[] parts = value.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        if (second > 255 || Integer.parseInt(parts[2]) > 255 || Integer.parseInt(parts[3]) > 255) return false;
        return first == 10 || (first == 192 && second == 168) || (first == 172 && second >= 16 && second <= 31);
    }
}
