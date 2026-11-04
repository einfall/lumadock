package com.gathin.tvhome;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Decodes only received Audio System (5) -> TV (0) Report Audio Status frames. */
final class CecAudioStatus {
    private static final Pattern REPORT = Pattern.compile("\\[R] time=(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) message=<Report Audio Status> 50:7[Aa]:([0-9a-fA-F]{2})");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    final int volume;
    final boolean muted;
    final long timestamp;

    private CecAudioStatus(int volume, boolean muted, long timestamp) {
        this.volume = volume; this.muted = muted; this.timestamp = timestamp;
    }

    static CecAudioStatus latest(String dump, long now, long since, ZoneId zone) {
        if (!dump.contains("mSystemAudioActivated: true")) return null;
        Matcher matches = REPORT.matcher(dump);
        CecAudioStatus result = null;
        long latestTime = Long.MIN_VALUE;
        while (matches.find()) {
            try {
                long time = LocalDateTime.parse(matches.group(1), TIME).atZone(zone).toInstant().toEpochMilli();
                if (time < latestTime || time < since - 1000 || now - time > 5000 || time > now + 1000) continue;
                latestTime = time;
                int value = Integer.parseInt(matches.group(2), 16);
                int volume = value & 0x7F;
                // 0x7F means unknown. A later unknown report invalidates a previous number.
                result = volume <= 100 ? new CecAudioStatus(volume, (value & 0x80) != 0, time) : null;
            } catch (RuntimeException ignored) { }
        }
        return result;
    }
}
