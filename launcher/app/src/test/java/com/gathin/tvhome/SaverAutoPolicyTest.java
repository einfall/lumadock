package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SaverAutoPolicyTest {
    @Test public void clearNightIncludesRealSkyButNeverSpace(){for(int n=0;n<20;n++)assertNotEquals("space",SaverAutoPolicy.choose(n,true,false,true,true,true,true));assertEquals("sky",SaverAutoPolicy.choose(2,true,false,true,true,true,true));}
    @Test public void cloudyNightOmitsSky(){for(int n=0;n<20;n++)assertNotEquals("sky",SaverAutoPolicy.choose(n,true,true,true,true,true,true));}
    @Test public void daylightOnlyIncludesSpaceWhenReady(){assertEquals("space",SaverAutoPolicy.choose(2,false,false,true,true,true,true));for(int n=0;n<20;n++)assertNotEquals("space",SaverAutoPolicy.choose(n,false,false,true,true,true,false));}
    @Test public void offlineFallbackAlwaysHasAValidScene(){for(int n:new int[]{0,1,-1,Integer.MAX_VALUE,Integer.MIN_VALUE})assertEquals("mosaic",SaverAutoPolicy.choose(n,true,false,false,false,false,false));assertEquals(900000L,SaverAutoPolicy.INTERVAL);}
    @Test public void allModesSurviveValidation(){for(String id:SaverPolicy.MODES)assertEquals(id,SaverPolicy.mode(id));assertEquals("legacy",SaverPolicy.mode("not-a-scene"));}
}
