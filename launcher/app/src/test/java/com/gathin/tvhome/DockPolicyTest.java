package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class DockPolicyTest {
 @Test public void zeroRowsReallyParksDockBelowPreviewPosition(){assertTrue(DockPolicy.top(800,280,230,0,2)>DockPolicy.top(800,280,230,1,2));assertEquals(0,DockPolicy.fullRows(0));assertEquals(0,DockPolicy.fullRows(1));assertEquals(1,DockPolicy.fullRows(2));assertEquals(2,DockPolicy.fullRows(3));}
 @Test public void everyNextSettingExposesOneMoreRow(){assertEquals(230,DockPolicy.top(1200,280,230,1,2)-DockPolicy.top(1200,280,230,2,2));assertEquals(230,DockPolicy.top(1200,280,230,2,2)-DockPolicy.top(1200,280,230,3,2));}
 @Test public void wrapReachesOtherEndAndClampedModeStaysAtEnds(){assertEquals(19,DockPolicy.horizontal(0,20,false,true));assertEquals(0,DockPolicy.horizontal(19,20,true,true));assertEquals(0,DockPolicy.horizontal(0,20,false,false));assertEquals(19,DockPolicy.horizontal(19,20,true,false));assertEquals(1,DockPolicy.horizontal(0,20,true,true));assertEquals(-1,DockPolicy.horizontal(0,0,true,true));assertEquals(0,DockPolicy.horizontal(0,1,true,true));}
 @Test public void newestInstallUsesFirstInstallTimeAndNamesBreakTies(){assertTrue(DockPolicy.compare("installed","Z","z",200,0,"A","a",100,0)<0);assertTrue(DockPolicy.compare("installed","A","a",100,0,"B","b",100,0)<0);}
 @Test public void frequentMeansMostStartsNotMostRecentStart(){assertTrue(DockPolicy.compare("frequent","Z","z",0,20,"A","a",100,2)<0);assertTrue(DockPolicy.compare("frequent","A","a",0,0,"B","b",100,0)<0);}
 @Test public void alphabeticalUsesDisplayedNameAndStableKey(){assertTrue(DockPolicy.compare("name","alpha","z",0,0,"Beta","a",200,900)<0);assertTrue(DockPolicy.compare("name","same","a",0,0,"Same","b",0,0)<0);assertEquals("custom",DockPolicy.mode("invalid"));}
}
