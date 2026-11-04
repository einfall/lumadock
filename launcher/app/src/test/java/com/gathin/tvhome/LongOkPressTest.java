package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class LongOkPressTest {
 @Test public void shortPressIsNotAnAndroidDefaultLongPress(){LongOkPress p=new LongOkPress();p.press(10);assertFalse(p.fire(510));assertEquals(LongOkPress.CLICK,p.release(1510,false));assertEquals(LongOkPress.NONE,p.release(1511,false));}
 @Test public void exactTwoSecondThresholdAndSingleMenu(){LongOkPress p=new LongOkPress();p.press(100);assertFalse(p.fire(2099));assertTrue(p.fire(2100));assertFalse(p.fire(2200));assertEquals(LongOkPress.NONE,p.release(2300,false));}
 @Test public void busyUiStillOpensMenuInsteadOfLaunching(){LongOkPress p=new LongOkPress();p.press(100);assertEquals(LongOkPress.MENU,p.release(2150,false));}
 @Test public void directionChangeAndCancelledReleaseNeverLaunch(){LongOkPress p=new LongOkPress();p.press(0);p.cancel();assertFalse(p.fire(3000));assertEquals(LongOkPress.NONE,p.release(3000,false));p.press(4000);assertEquals(LongOkPress.NONE,p.release(4100,true));}
}
