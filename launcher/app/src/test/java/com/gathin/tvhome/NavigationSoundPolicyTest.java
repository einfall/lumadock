package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationSoundPolicyTest {
    @Test public void mediaOutputIsTheFallbackAndSystemRemainsSelectable(){assertEquals("media",NavigationSoundPolicy.output(null));assertEquals("media",NavigationSoundPolicy.output("bad"));assertEquals("system",NavigationSoundPolicy.output("system"));}
    @Test public void existingAndUnknownPreferencesKeepSystemSounds(){assertEquals("system",NavigationSoundPolicy.mode(null));assertEquals("system",NavigationSoundPolicy.mode("bad"));assertEquals("off",NavigationSoundPolicy.mode("off"));assertEquals("luma",NavigationSoundPolicy.mode("luma"));}
    @Test public void onlyNavigationKeysPlayOnKeyDown(){for(int key:new int[]{19,20,21,22,23,66,160,4,111,82})assertTrue(NavigationSoundPolicy.key(0,key,0));for(int key:new int[]{3,24,25,26,164,7,29})assertFalse(NavigationSoundPolicy.key(0,key,0));assertFalse(NavigationSoundPolicy.key(1,23,0));}
    @Test public void heldArrowsRepeatButHoldingOkAndBackDoesNot(){assertTrue(NavigationSoundPolicy.key(0,22,3));assertFalse(NavigationSoundPolicy.key(0,23,3));assertFalse(NavigationSoundPolicy.key(0,4,3));assertFalse(NavigationSoundPolicy.due(100,70));assertTrue(NavigationSoundPolicy.due(135,70));assertTrue(NavigationSoundPolicy.due(0,-1));}
    @Test public void volumeIsBoundedIncludingSilence(){assertEquals(0,NavigationSoundPolicy.volume(-10));assertEquals(0,NavigationSoundPolicy.volume(0));assertEquals(40,NavigationSoundPolicy.volume(40));assertEquals(100,NavigationSoundPolicy.volume(500));}
}
