package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CecAudioStatusTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final long NOW = LocalDateTime.of(2026, 10, 28, 20, 40, 15).atZone(ZONE).toInstant().toEpochMilli();
    private static final String ACTIVE = "mSystemAudioActivated: true\n";
    private static String report(String hex) { return "[R] time=2026-10-28 20:40:15 message=<Report Audio Status> 50:7A:" + hex + "\n"; }
    private static CecAudioStatus parse(String value) { return CecAudioStatus.latest(value, NOW + 300, NOW, ZONE); }

    @Test public void actualArcReportIsFour() {
        CecAudioStatus result = parse(ACTIVE + report("00") + report("02") + report("04"));
        assertNotNull(result); assertEquals(4, result.volume); assertFalse(result.muted);
    }
    @Test public void muteBitDoesNotChangeVolume() {
        CecAudioStatus result = parse(ACTIVE + report("84"));
        assertNotNull(result); assertTrue(result.muted); assertEquals(4, result.volume);
    }
    @Test public void unknownInvalidatesEarlierKnownVolume() {
        assertNull(parse(ACTIVE + report("04") + report("7F")));
        assertNull(parse(ACTIVE + report("FF")));
        assertNull(parse(ACTIVE + report("65")));
    }
    @Test public void staleAndFutureReportsAreRejected() {
        assertNull(CecAudioStatus.latest(ACTIVE + report("04"), NOW + 6000, NOW, ZONE));
        assertNull(CecAudioStatus.latest(ACTIVE + report("04"), NOW, NOW + 3000, ZONE));
        assertNull(CecAudioStatus.latest(ACTIVE + report("04"), NOW - 2000, NOW - 2000, ZONE));
    }
    @Test public void onlyReceivedAudioSystemToTvCounts() {
        assertNull(parse(ACTIVE + report("04").replace("[R]", "[S]")));
        assertNull(parse(ACTIVE + report("04").replace("50:7A", "40:7A")));
        assertNull(parse(ACTIVE + report("04").replace("50:7A", "5F:7A")));
    }
    @Test public void inactiveSystemAudioDoesNotShowNumber() {
        assertNull(parse("mSystemAudioActivated: false\n" + report("04")));
    }
    @Test public void zeroAndHundredAreValid() {
        assertEquals(0, parse(ACTIVE + report("00")).volume);
        assertEquals(100, parse(ACTIVE + report("64")).volume);
    }
    @Test public void playbackReportsWithMillisecondsUseItsOwnAddress() {
        String shield=ACTIVE+"  HdmiCecLocalDevice #4:\n    mAddress: 4\n";
        String received=report("21").replace("20:40:15 ","20:40:15.125 ").replace("50:7A","54:7A");
        CecAudioStatus result=parse(shield+received);
        assertNotNull(result);assertEquals(33,result.volume);assertEquals(NOW+125,result.timestamp);
        assertNull(parse(shield+report("21")));
        assertNull(parse(shield+received.replace("[R]","[S]")));
    }
    @Test public void playbackUnknownAndVolumeKeyCommandsCannotInventAPercentage() {
        String shield=ACTIVE+"mAddress: 4\n";
        assertNull(parse(shield+report("7F").replace("50:7A","54:7A")));
        assertNull(parse(shield+"[S] time=2026-10-28 20:40:15.125 message=<User Control Pressed> 45:44:41\n"));
        assertNull(parse(shield+report("21").replace("50:7A","58:7A")));
    }
}
