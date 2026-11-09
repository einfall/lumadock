package com.gathin.tvhome;

import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

public class EchoTimingTest {
    private static final String UPDATED = "2026-11-09T18:00:00+00:00";
    private static final long NOW = Instant.parse("2026-11-09T18:00:08Z").toEpochMilli();

    @Test public void playingUsesTheReportedTimestampAndStopsAtDuration() {
        EchoTiming timing = EchoTiming.from(180, 12, UPDATED, NOW, true);
        assertEquals(20_000, timing.position(0));
        assertEquals(22_000, timing.position(2_000));
        assertEquals(180_000, timing.position(200_000));
        assertFalse(timing.advances(200_000));
    }

    @Test public void pausedAndUntimestampedTracksKeepTheirReportedPosition() {
        assertEquals(12_000, EchoTiming.from(180, 12, UPDATED, NOW, false).position(2000));
        assertEquals(12_000, EchoTiming.from(180, 12, "", NOW, true).position(2000));
    }

    @Test public void liveStreamsAndIncompleteTimingHideTheTimeline() {
        assertSame(EchoTiming.UNKNOWN, EchoTiming.from(0, 12, UPDATED, NOW, true));
        assertSame(EchoTiming.UNKNOWN, EchoTiming.from(180, -1, UPDATED, NOW, true));
        assertEquals("4:27", EchoTiming.clock(267_000));
        assertEquals("1:01:05", EchoTiming.clock(3_665_000));
    }
}
