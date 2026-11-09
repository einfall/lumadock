package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

final class EchoFeed {
    private static final String SETUP = "Home Assistant verbinden";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Consumer<EchoPlayback> observer;
    final EchoConnection settings;
    private final Runnable tick = this::poll;
    private EchoPlayback state = EchoPlayback.status(SETUP);
    private Future<?> pending;
    private boolean visible;
    private int generation;

    EchoFeed(Context context, Consumer<EchoPlayback> observer) {
        settings = new EchoConnection(context);
        this.observer = observer;
    }

    EchoPlayback state() { return state; }

    void visible(boolean on) {
        if (visible == on) return;
        visible = on;
        refresh();
    }

    void refresh() {
        generation++;
        main.removeCallbacks(tick);
        if (pending != null) pending.cancel(true);
        if (!visible) return;
        String status = !settings.configured() ? SETUP
                : EchoConnection.validEntity(settings.entity()) ? "Verbinde …" : "Mediaplayer auswählen";
        publish(EchoPlayback.status(status));
        poll();
    }

    private void poll() {
        String address = settings.address();
        URI base = EchoConnection.base(address);
        String entity = settings.entity();
        if (!visible || base == null || !EchoConnection.validEntity(entity) || !settings.configured()) return;
        int request = generation;
        EchoPlayback previous = state;
        pending = worker.submit(() -> {
            EchoPlayback result;
            try {
                String token = settings.token(address);
                result = EchoPlayback.read(base, entity, token, previous);
            } catch (EchoPlayback.HttpError error) {
                String status = error.code == 401 || error.code == 403 ? "Token prüfen"
                        : error.code == 404 ? "Echo-Entität prüfen" : "Home Assistant antwortet nicht";
                result = EchoPlayback.status(status);
            } catch (java.security.GeneralSecurityException error) {
                result = EchoPlayback.status("Token erneut eingeben");
            } catch (Exception error) {
                result = EchoPlayback.status("Home Assistant nicht erreichbar");
            }
            EchoPlayback updated = result;
            main.post(() -> {
                if (!visible || generation != request) return;
                publish(updated);
                main.postDelayed(tick, 15_000);
            });
        });
    }

    private void publish(EchoPlayback next) {
        state = next;
        observer.accept(next);
    }

    void close() {
        visible(false);
        worker.shutdownNow();
    }
}
