package com.gathin.tvhome;
import org.junit.Test;import static org.junit.Assert.*;
public class RingAlphaPolicyTest {
 @Test public void transparentGlowKeepsRgbAndDoesNotAffectOtherChannels(){assertEquals(0x00123456,RingAlphaPolicy.color(0xFF123456,42,0));assertEquals(0x2A123456,RingAlphaPolicy.color(0xFF123456,42,100));assertEquals(0x15123456,RingAlphaPolicy.color(0xFF123456,42,50));}
 @Test public void valuesClampWithoutOverflow(){assertEquals(0,RingAlphaPolicy.percent(-9));assertEquals(100,RingAlphaPolicy.percent(150));assertEquals(0xFF123456,RingAlphaPolicy.color(0x12123456,255,500));}
}
