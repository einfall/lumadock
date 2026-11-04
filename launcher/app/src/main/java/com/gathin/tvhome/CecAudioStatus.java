package com.gathin.tvhome;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only received Audio System (5) reports addressed to this TV/playback device count. */
final class CecAudioStatus {
    private static final Pattern REPORT = Pattern.compile("\\[R] time=(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?) message=<Report Audio Status> 5([0-9a-eA-E]):7[Aa]:([0-9a-fA-F]{2})(?![0-9a-fA-F:])");
    private static final Pattern ADDRESS = Pattern.compile("(?m)^\\s*mAddress:\\s*(\\d+)\\s*$");
    private static final DateTimeFormatter TIME = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND,0,9,true).optionalEnd().toFormatter();
    final int volume;
    final boolean muted;
    final long timestamp;

    private CecAudioStatus(int volume, boolean muted, long timestamp) {
        this.volume = volume; this.muted = muted; this.timestamp = timestamp;
    }

    static CecAudioStatus latest(String dump, long now, long since, ZoneId zone) {
        if (!dump.contains("mSystemAudioActivated: true")) return null;
        int local=0;Matcher addresses=ADDRESS.matcher(dump);boolean declared=false;
        while(addresses.find()){
            declared=true;int address=Integer.parseInt(addresses.group(1));
            if(address==0||address==4||address==8||address==11)local|=1<<address;
        }
        if(!declared)local=1; // Older TV dumps omit the local-device details.
        Matcher matches = REPORT.matcher(dump);
        CecAudioStatus result = null;
        long latestTime = Long.MIN_VALUE;
        while (matches.find()) {
            try {
                if((local&(1<<Integer.parseInt(matches.group(2),16)))==0)continue;
                long time = LocalDateTime.parse(matches.group(1), TIME).atZone(zone).toInstant().toEpochMilli();
                if (time < latestTime || time < since - 1000 || now - time > 5000 || time > now + 1000) continue;
                latestTime = time;
                int value = Integer.parseInt(matches.group(3), 16);
                int volume = value & 0x7F;
                // 0x7F means unknown. A later unknown report invalidates a previous number.
                result = volume <= 100 ? new CecAudioStatus(volume, (value & 0x80) != 0, time) : null;
            } catch (RuntimeException ignored) { }
        }
        return result;
    }
}
