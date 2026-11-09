package com.gathin.tvhome;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

final class EchoTiming {
    static final EchoTiming UNKNOWN = new EchoTiming(-1, -1, false);
    final long durationMs;
    private final long positionMs;
    private final boolean advancing;

    private EchoTiming(long durationMs, long positionMs, boolean advancing) {
        this.durationMs = durationMs;
        this.positionMs = positionMs;
        this.advancing = advancing;
    }

    static EchoTiming from(double duration, double position, String updated, long now, boolean playing) {
        if (!Double.isFinite(duration) || !Double.isFinite(position)
                || duration <= 0 || position < 0 || duration > Long.MAX_VALUE / 1000d) return UNKNOWN;
        long end = Math.round(duration * 1000);
        if (end <= 0) return UNKNOWN;
        long start = Math.round(Math.min(position, duration) * 1000);
        if (playing && !updated.isEmpty()) {
            try {
                long age = Math.max(0, now - OffsetDateTime.parse(updated).toInstant().toEpochMilli());
                return new EchoTiming(end, start + Math.min(age, end - start), true);
            } catch (DateTimeParseException | ArithmeticException ignored) {
                // A missing or unusable timestamp permits displaying the reported position only.
            }
        }
        return new EchoTiming(end, start, false);
    }

    long position(long elapsed) {
        return advancing ? positionMs + Math.min(Math.max(0, elapsed), durationMs - positionMs) : positionMs;
    }

    boolean advances(long elapsed) { return advancing && position(elapsed) < durationMs; }

    static String clock(long milliseconds) {
        long seconds = milliseconds / 1000;
        long hours = seconds / 3600;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, seconds / 60 % 60, seconds % 60)
                : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
