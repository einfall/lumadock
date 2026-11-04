package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherPolicyTest {
    @Test public void validatesCoordinatesIncludingZeroAndRejectsNonFiniteValues() {
        assertTrue(WeatherPolicy.validCoordinates(0,0));
        assertTrue(WeatherPolicy.validCoordinates(-90,180));
        assertFalse(WeatherPolicy.validCoordinates(91,0));
        assertFalse(WeatherPolicy.validCoordinates(0,-181));
        assertFalse(WeatherPolicy.validCoordinates(Double.NaN,0));
        assertFalse(WeatherPolicy.validCoordinates(0,Double.POSITIVE_INFINITY));
    }
    @Test public void cachesForThirtyMinutesAndRetriesNoSoonerThanFiveMinutes() {
        long now=10_000_000;
        assertFalse(WeatherPolicy.shouldFetch(now,now-1000,now-1000));
        assertTrue(WeatherPolicy.shouldFetch(now,now-WeatherPolicy.REFRESH_MS,now-WeatherPolicy.REFRESH_MS));
        assertFalse(WeatherPolicy.shouldFetch(now,0,now-1000));
        assertTrue(WeatherPolicy.shouldFetch(now,0,now-WeatherPolicy.RETRY_MS));
        assertTrue(WeatherPolicy.shouldFetch(now,0,0));
    }
    @Test public void aClockCorrectionDoesNotKeepAnOldCacheForever() {
        assertTrue(WeatherPolicy.shouldFetch(1000,2000,2000));
    }

    @Test public void changingIntervalUsesExistingFetchTime() {
        long now=20_000_000, minute=60_000;
        assertTrue(WeatherPolicy.shouldFetch(now,now-15*minute,now-15*minute,10));
        assertFalse(WeatherPolicy.shouldFetch(now,now-15*minute,now-15*minute,30));
        assertEquals(45*minute,WeatherPolicy.nextDelay(now,now-15*minute,now-15*minute,60));
        assertEquals(minute,WeatherPolicy.nextDelay(now,now-9*minute,now-9*minute,10));
    }
    @Test public void failureRetriesRespectCooldownAtEveryInterval() {
        long now=20_000_000, minute=60_000;
        for(int interval:new int[]{10,15,30,60,120}) {
            assertEquals(4*minute,WeatherPolicy.nextDelay(now,0,now-minute,interval));
            assertTrue(WeatherPolicy.shouldFetch(now,0,now-5*minute,interval));
        }
    }
    @Test public void invalidIntervalsFallBackAndBoundaryIsDue() {
        long now=20_000_000;
        assertEquals(30,WeatherPolicy.intervalMinutes(-1));
        assertEquals(30,WeatherPolicy.intervalMinutes(Integer.MAX_VALUE));
        for(int interval:new int[]{10,15,30,60,120}) {
            long duration=WeatherPolicy.intervalMs(interval);
            assertFalse(WeatherPolicy.shouldFetch(now,now-duration+1,0,interval));
            assertTrue(WeatherPolicy.shouldFetch(now,now-duration,0,interval));
        }
    }
}
