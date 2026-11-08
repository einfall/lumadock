package com.gathin.tvhome;
import org.junit.Test;import java.util.*;import static org.junit.Assert.*;
public class RecentAppsPolicyTest {
 private String task(String pkg,int user,boolean empty){return "  * Recent #0: Task{abc}\n userId="+user+" effectiveUid=u0a123\n mActivityComponent="+pkg+"/.Main\n Activities=["+(empty?"":"ActivityRecord{abc}")+"]\n";}
 @Test public void onlyNonemptyTasksFromTheCurrentUserAreShown(){assertEquals(Arrays.asList("com.example.one"),RecentAppsPolicy.packages(task("com.example.one",0,false)+task("com.example.empty",0,true)+task("com.example.other",10,false),0));}
 @Test public void repeatedTasksDeduplicateInRecentOrder(){assertEquals(Arrays.asList("com.example.two","com.example.one"),RecentAppsPolicy.packages(task("com.example.two",0,false)+task("com.example.one",0,false)+task("com.example.two",0,false),0));}
 @Test public void systemAndLauncherTasksAreExcluded(){for(String p:new String[]{"com.gathin.tvhome","com.android.systemui","org.droidtv.playtv","com.mediatek.tv.service","com.google.android.gms","com.google.android.tungsten.setupwraith"}){assertFalse(RecentAppsPolicy.safe(p));assertTrue(RecentAppsPolicy.packages(task(p,0,false),0).isEmpty());}}
 @Test public void packageOperandsCannotInjectCommands(){for(String p:new String[]{"com.app;reboot","com.app && id","com.app\nreboot","$(reboot)","com.app/Activity","-x"})assertFalse(RecentAppsPolicy.safe(p));assertEquals("am force-stop --user current com.example.app",RecentAppsPolicy.stopCommand("com.example.app"));}
 @Test(expected=IllegalArgumentException.class) public void stoppingTheLauncherIsAlwaysRejected(){RecentAppsPolicy.stopCommand("com.gathin.tvhome");}
 @Test public void shieldBackgroundRootProcessesRemainClosableWithoutAnActivity(){assertEquals(Arrays.asList("com.example.player"),RecentAppsPolicy.packages(task("com.example.player",0,true)+" mRootProcess=ProcessRecord{abc 123:com.example.player/u0a123}\n",0));}
}
