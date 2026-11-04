package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SleepPolicyTest {
 @Test public void onlyListedDurationsAllowed(){for(int n:SleepPolicy.MINUTES)assertTrue(SleepPolicy.allowed(n));assertFalse(SleepPolicy.allowed(-1));assertFalse(SleepPolicy.allowed(7));assertFalse(SleepPolicy.allowed(721));}
 @Test public void exactElapsedDeadlinesIncludingTwelveHours(){assertEquals(0,SleepPolicy.deadline(123,0));assertEquals(60123,SleepPolicy.deadline(123,1));assertEquals(43200123,SleepPolicy.deadline(123,720));}
 @Test public void ceilingAndExpiry(){assertEquals(1,SleepPolicy.remainingMinutes(1000,1001));assertEquals(2,SleepPolicy.remainingMinutes(1000,61001));assertEquals(0,SleepPolicy.remainingMinutes(1000,999));}
 @Test public void activeAndFinalFiveMinuteColors(){assertEquals(0,SleepPolicy.color(1000,0));assertEquals(0xFFFFB347,SleepPolicy.color(1000,301001));assertEquals(0xFFFF6B6B,SleepPolicy.color(1000,301000));}
 @Test public void countdownUsesCeilingSeconds(){assertEquals(10,SleepPolicy.countdown(1000,11000));assertEquals(10,SleepPolicy.countdown(1001,11000));assertEquals(9,SleepPolicy.countdown(2000,11000));assertEquals(1,SleepPolicy.countdown(10999,11000));assertEquals(0,SleepPolicy.countdown(11000,11000));}
 @Test public void ringFillsAndClamps(){assertEquals(0f,SleepPolicy.progress(0,11000),0f);assertEquals(0f,SleepPolicy.progress(1000,11000),0f);assertEquals(.5f,SleepPolicy.progress(6000,11000),0f);assertEquals(1f,SleepPolicy.progress(12000,11000),0f);}
 @Test public void onlyCurrentFinalTenSecondsCanWarn(){assertFalse(SleepPolicy.warningDue(999,11000,11000));assertTrue(SleepPolicy.warningDue(1000,11000,11000));assertFalse(SleepPolicy.warningDue(1000,11000,12000));assertFalse(SleepPolicy.warningDue(11000,11000,11000));assertFalse(SleepPolicy.warningDue(1000,0,0));}
}
