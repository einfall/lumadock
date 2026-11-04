package com.gathin.tvhome;

/** Timing and coordinate rules shared by the weather settings and the header. */
final class WeatherPolicy {
    static final String INTERVAL_KEY = "refresh_minutes";
    static final int DEFAULT_MINUTES = 30;
    static final long REFRESH_MS = DEFAULT_MINUTES * 60_000L;
    static final long RETRY_MS = 5 * 60_000L;
    private WeatherPolicy() { }
    static int intervalMinutes(int value) {
        switch (value) {
            case 10: case 15: case 30: case 60: case 120: return value;
            default: return DEFAULT_MINUTES;
        }
    }
    static long intervalMs(int minutes) { return intervalMinutes(minutes) * 60_000L; }
    static boolean validCoordinates(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon)
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
    static boolean shouldFetch(long now, long fetched, long attempted) {
        return shouldFetch(now, fetched, attempted, DEFAULT_MINUTES);
    }
    static boolean shouldFetch(long now, long fetched, long attempted, int minutes) {
        return nextDelay(now, fetched, attempted, minutes) == 0;
    }
    /** Remaining time is based on the last result, not on when Home was opened. */
    static long nextDelay(long now, long fetched, long attempted, int minutes) {
        long cache = fetched > 0 && fetched <= now ? Math.max(0, intervalMs(minutes) - (now - fetched)) : 0;
        long retry = attempted > 0 && attempted <= now ? Math.max(0, RETRY_MS - (now - attempted)) : 0;
        return Math.max(cache, retry);
    }
}
