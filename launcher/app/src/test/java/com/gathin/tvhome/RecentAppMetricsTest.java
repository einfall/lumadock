package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RecentAppMetricsTest {
    @Test public void cpuUsesTheSecondIntervalAndCanReportMultipleCores() {
        String dump = "   PID[%CPU\n  25  89.3\n  81 25.0\n400%cpu\n   PID %CPU\n  25  116.7\n  33 0.0\n";
        assertEquals(116.7, RecentAppMetrics.cpu(dump).get(25), .01);
        assertEquals(0, RecentAppMetrics.cpu(dump).get(33), .01);
        assertFalse(RecentAppMetrics.cpu(dump).containsKey(81));
        assertTrue(RecentAppMetrics.cpu("PID[%CPU\n  25 89.3\n").isEmpty());
    }

    @Test public void memoryReadsPssAndLeavesRssOut() {
        String dump = "Total RSS by OOM adjustment:\n  400,000K: sample (pid 55 / activities)\n"
                + "\nTotal PSS by OOM adjustment:\n    200,300K: sample (pid 55 / activities)\n"
                + "     10,000K: sample:extra (pid 56)\n\nTotal RAM: 300,000K\n";
        assertEquals(Long.valueOf(200_300L * 1024), RecentAppMetrics.pss(dump).get(55));
        assertEquals(Long.valueOf(10_000L * 1024), RecentAppMetrics.pss(dump).get(56));
        assertTrue(RecentAppMetrics.pss("Total RSS by OOM adjustment:\n  45K: sample (pid 7)\n").isEmpty());
    }
}
