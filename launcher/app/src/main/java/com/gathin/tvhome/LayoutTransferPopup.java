package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;

@SuppressWarnings("deprecation") // The legacy NSD listeners also work on Android TV 8–13.
final class LayoutTransferPopup {
    private static final String TYPE = "_lumadock._tcp.";
    private static final String TAG = "LumaDock.Transfer";

    private final MainActivity activity;
    private final Context context;
    private final NsdManager nsd;
    private final Handler main = new Handler(Looper.getMainLooper());

    static void show(MainActivity activity) {
        new LayoutTransferPopup(activity).menu();
    }

    private LayoutTransferPopup(MainActivity activity) {
        this.activity = activity;
        context = activity.getApplicationContext();
        nsd = context.getSystemService(NsdManager.class);
    }

    private void menu() {
        SettingsSheet sheet = sheet("Im Heimnetz übertragen");
        sheet.row("move", "Dieses Gerät freigeben", "Auf dem TV mit der gewünschten Einrichtung",
                () -> DesignDialogs.name(activity, "Name dieses Geräts", deviceName(), this::share));
        sheet.row("search", "Geräte suchen", "Auf dem TV, der die Einrichtung übernehmen soll",
                () -> new Discovery().open());
        sheet.info("Überträgt das aktive Layout mit Dock, App-Reihenfolge, Ordnern, Luma-Widgets, Design und Hintergründen.");
        sheet.info("Installierte Apps, PINs, Zugangsdaten, Berechtigungen und Sonos-Zuordnung werden auf jedem Gerät separat eingerichtet.");
        sheet.show();
    }

    private SettingsSheet sheet(String title) {
        SettingsSheet sheet = new SettingsSheet(activity, title);
        sheet.hint("OK wählen · Zurück schließt");
        return sheet;
    }

    private String deviceName() {
        String system = Settings.Global.getString(context.getContentResolver(), "device_name");
        return Cards.cleanLabel(system == null ? Build.MODEL : system, "Luma Dock");
    }

    private void share(String name) {
        SettingsSheet sheet = sheet("Dieses Gerät freigeben");
        sheet.info("Auf dem zweiten TV „Geräte suchen“ öffnen und „" + name + "“ auswählen.");
        TextView code = GlassDialog.text(activity, "Freigabe wird vorbereitet …", 17);
        code.setTypeface(Typeface.MONOSPACE);
        code.setTextIsSelectable(false);
        sheet.add(code);
        TextView address = GlassDialog.text(activity, "", 11);
        sheet.add(address);
        LinearLayout close = sheet.row("close", "Freigabe beenden", null, sheet.dialog::dismiss);
        sheet.show();
        close.requestFocus();
        ShareSession session = new ShareSession(sheet, code, address);
        sheet.dialog.setOnDismissListener(dialog -> session.stop());
        main.postDelayed(session.expire, LayoutTransferLink.WINDOW_MS);
        new Thread(() -> session.run(name), "luma-layout-share").start();
    }

    private final class ShareSession {
        final SettingsSheet sheet;
        final TextView code;
        final TextView address;
        final Runnable expire = this::expired;
        volatile LayoutTransferLink.Share link;
        volatile boolean stopped;
        NsdManager.RegistrationListener registration;

        ShareSession(SettingsSheet sheet, TextView code, TextView address) {
            this.sheet = sheet;
            this.code = code;
            this.address = address;
        }

        void run(String name) {
            try (LayoutTransferLink.Share opened = new LayoutTransferLink.Share()) {
                link = opened;
                if (stopped) return;
                main.post(() -> advertise(name, opened));
                boolean sent = opened.serve(context);
                main.post(() -> finish(sent
                        ? "Einrichtung übertragen. Bestätige die Übernahme auf dem zweiten TV."
                        : "Freigabe beendet. Bei Bedarf erneut freigeben."));
            } catch (IOException | GeneralSecurityException | JSONException | RuntimeException failure) {
                if (!stopped) {
                    Log.w(TAG, "Sharing failed", failure);
                    main.post(() -> finish("Die Freigabe konnte nicht abgeschlossen werden."));
                }
            }
        }

        void advertise(String name, LayoutTransferLink.Share opened) {
            if (stopped || !sheet.dialog.isShowing()) return;
            code.setText(opened.displayCode());
            String endpoint = ipv4Address();
            address.setText(endpoint.isEmpty() ? "Code gilt einmalig für fünf Minuten."
                    : "Manuelle Verbindung: " + endpoint + ":" + opened.port() + "\nCode gilt einmalig für fünf Minuten.");
            if (nsd == null) return;
            NsdServiceInfo info = new NsdServiceInfo();
            info.setServiceName(name);
            info.setServiceType(TYPE);
            info.setPort(opened.port());
            registration = new NsdManager.RegistrationListener() {
                @Override public void onServiceRegistered(NsdServiceInfo registered) {
                    main.post(() -> {
                        if (stopped) {
                            unregister(this);
                        } else if (!name.equals(registered.getServiceName())) {
                            sheet.info("Name in der Geräteliste: " + registered.getServiceName());
                        }
                    });
                }

                @Override public void onRegistrationFailed(NsdServiceInfo service, int error) {
                    main.post(() -> {
                        if (!stopped) sheet.info("Automatische Suche nicht verfügbar. Nutze die manuelle Verbindung.");
                    });
                }

                @Override public void onServiceUnregistered(NsdServiceInfo service) {}
                @Override public void onUnregistrationFailed(NsdServiceInfo service, int error) {}
            };
            try {
                nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration);
            } catch (RuntimeException unavailable) {
                sheet.info("Automatische Suche nicht verfügbar. Nutze die manuelle Verbindung.");
            }
        }

        void expired() {
            finish("Der Code ist abgelaufen. Starte eine neue Freigabe.");
        }

        void finish(String status) {
            if (stopped) return;
            stop();
            if (sheet.dialog.isShowing()) {
                code.setText(status);
                address.setText("");
            }
        }

        void stop() {
            if (stopped) return;
            stopped = true;
            main.removeCallbacks(expire);
            unregister(registration);
            LayoutTransferLink.Share open = link;
            if (open != null) open.close();
        }
    }

    private void unregister(NsdManager.RegistrationListener listener) {
        if (nsd == null || listener == null) return;
        try {
            nsd.unregisterService(listener);
        } catch (RuntimeException ignored) {
            // Registration can finish or fail while the window closes.
        }
    }

    private String ipv4Address() {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        LinkProperties properties = manager == null ? null : manager.getLinkProperties(manager.getActiveNetwork());
        if (properties != null) {
            for (LinkAddress link : properties.getLinkAddresses()) {
                InetAddress address = link.getAddress();
                if (address instanceof Inet4Address && LayoutTransferLink.local(address)) return address.getHostAddress();
            }
        }
        return "";
    }

    private final class Discovery implements NsdManager.DiscoveryListener {
        final SettingsSheet sheet = sheet("Luma-Dock-Geräte");
        final Map<String, LinearLayout> rows = new LinkedHashMap<>();
        final TextView status = GlassDialog.text(activity, "Suche läuft …", 12);
        boolean active;
        boolean busy;

        void open() {
            sheet.info("Am anderen TV zuerst „Dieses Gerät freigeben“ öffnen.");
            sheet.add(status);
            sheet.row("network", "Adresse manuell eingeben", "IP-Adresse und Port vom anderen TV",
                    () -> DesignDialogs.name(activity, "Adresse und Port", "", this::manual));
            sheet.dialog.setOnDismissListener(dialog -> stop());
            sheet.show();
            if (nsd == null) {
                status.setText("Automatische Suche nicht verfügbar.");
                return;
            }
            try {
                active = true;
                nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, this);
            } catch (RuntimeException failure) {
                failed();
            }
        }

        void manual(String value) {
            try {
                int split = value.lastIndexOf(':');
                if (split <= 0) throw new IllegalArgumentException();
                String host = value.substring(0, split);
                if (!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) throw new IllegalArgumentException();
                int port = Integer.parseInt(value.substring(split + 1));
                byte[] bytes = new byte[4];
                String[] parts = host.split("\\.");
                for (int i = 0; i < bytes.length; i++) {
                    int number = Integer.parseInt(parts[i]);
                    if (number > 255) throw new IllegalArgumentException();
                    bytes[i] = (byte) number;
                }
                InetAddress address = InetAddress.getByAddress(bytes);
                if (port < 1 || port > 65535 || !LayoutTransferLink.local(address)) {
                    throw new IllegalArgumentException();
                }
                pairing(sheet, host, address, port);
            } catch (IOException | IllegalArgumentException failure) {
                DesignDialogs.message(activity, "Bitte eine Heimnetz-Adresse mit Port eingeben, z. B. IP-Adresse:Port.");
            }
        }

        void stop() {
            if (!active) return;
            active = false;
            try {
                nsd.stopServiceDiscovery(this);
            } catch (RuntimeException ignored) {
                // Discovery has already ended.
            }
        }

        void failed() {
            stop();
            status.setText("Automatische Suche nicht verfügbar. Gib die Adresse manuell ein.");
        }

        @Override public void onDiscoveryStarted(String serviceType) {}
        @Override public void onDiscoveryStopped(String serviceType) {}
        @Override public void onStartDiscoveryFailed(String serviceType, int error) {
            main.post(this::failed);
        }
        @Override public void onStopDiscoveryFailed(String serviceType, int error) {}

        @Override public void onServiceFound(NsdServiceInfo service) {
            main.post(() -> {
                String name = service.getServiceName();
                if (!active || rows.containsKey(name) || rows.size() >= 32) return;
                String label = Cards.cleanLabel(name, "Luma Dock");
                LinearLayout row = sheet.row("network", label, "Freigabe verfügbar", () -> resolve(service, label));
                boolean first = rows.isEmpty();
                rows.put(name, row);
                status.setText("Verfügbare Geräte");
                if (first && sheet.dialog.isShowing() && sheet.dialog.getWindow().getDecorView().hasWindowFocus()) row.requestFocus();
            });
        }

        @Override public void onServiceLost(NsdServiceInfo service) {
            main.post(() -> {
                LinearLayout row = rows.remove(service.getServiceName());
                if (row != null) sheet.body.removeView(row);
                if (rows.isEmpty() && active) status.setText("Suche läuft …");
            });
        }

        void resolve(NsdServiceInfo service, String name) {
            if (busy) return;
            busy = true;
            try {
                nsd.resolveService(service, new NsdManager.ResolveListener() {
                    @Override public void onServiceResolved(NsdServiceInfo resolved) {
                        main.post(() -> {
                            busy = false;
                            if (!sheet.dialog.isShowing()) return;
                            if (LayoutTransferLink.local(resolved.getHost())) {
                                pairing(sheet, name, resolved.getHost(), resolved.getPort());
                            } else resolveFailed();
                        });
                    }

                    @Override public void onResolveFailed(NsdServiceInfo unresolved, int error) {
                        main.post(Discovery.this::resolveFailed);
                    }
                });
            } catch (RuntimeException failure) {
                resolveFailed();
            }
        }

        void resolveFailed() {
            busy = false;
            if (sheet.dialog.isShowing()) {
                DesignDialogs.message(activity, "Gerät nicht erreichbar. Prüfe die Freigabe oder gib die Adresse manuell ein.");
            }
        }
    }

    private void pairing(SettingsSheet discovery, String name, InetAddress address, int port) {
        DesignDialogs.name(activity, "Freigabecode · " + name, "", entered -> {
            String code = LayoutTransferLink.code(entered);
            if (code == null) {
                DesignDialogs.message(activity, "Gib den vollständigen Code vom anderen TV ein.");
                return;
            }
            download(discovery, name, address, port, code);
        });
    }

    private void download(SettingsSheet discovery, String name, InetAddress address, int port, String code) {
        SettingsSheet progress = sheet("Einrichtung empfangen");
        progress.info("Verbinde mit „" + name + "“ und lade die Einrichtung …");
        progress.row("close", "Abbrechen", null, progress.dialog::dismiss);
        progress.show();
        new Thread(() -> {
            try {
                LayoutTransferBundle.Received received = LayoutTransferLink.receive(context, address, port, code);
                main.post(() -> {
                    if (!progress.dialog.isShowing() || activity.isDestroyed() || activity.isFinishing()) {
                        received.discard();
                        return;
                    }
                    progress.dialog.dismiss();
                    confirm(discovery, name, received);
                });
            } catch (IOException | JSONException | GeneralSecurityException | RuntimeException failure) {
                Log.w(TAG, "Receiving layout failed", failure);
                main.post(() -> {
                    if (!progress.dialog.isShowing() || activity.isDestroyed() || activity.isFinishing()) return;
                    progress.dialog.dismiss();
                    DesignDialogs.message(activity, "Übertragung fehlgeschlagen. Prüfe den Code und starte bei Bedarf eine neue Freigabe.");
                });
            }
        }, "luma-layout-receive").start();
    }

    private void confirm(SettingsSheet discovery, String name, LayoutTransferBundle.Received received) {
        if (!discovery.dialog.isShowing()) {
            received.discard();
            return;
        }
        SettingsSheet sheet = sheet("Einrichtung übernehmen?");
        sheet.info("Das aktive Profil übernimmt die Einrichtung von „" + name + "“. Den vorherigen Stand kannst du unter „Profile & Sicherung“ wiederherstellen.");
        if (received.skipped > 0) sheet.info("Nicht verfügbare Hintergrundmedien: " + received.skipped);
        boolean[] applied = {false};
        sheet.row("close", "Abbrechen", null, sheet.dialog::dismiss);
        sheet.row("move", "Einrichtung übernehmen", null, () -> {
            try {
                new DesignProfiles(activity).restoreTransfer(received.snapshot);
                applied[0] = true;
                SettingsSheet.dismissAll(activity);
                activity.recreate();
            } catch (IOException | JSONException | RuntimeException failure) {
                Log.w(TAG, "Could not apply received layout", failure);
                DesignDialogs.message(activity, "Die Einrichtung konnte nicht übernommen werden.");
            }
        });
        sheet.dialog.setOnDismissListener(dialog -> {
            if (!applied[0]) received.discard();
        });
        sheet.show();
    }
}
