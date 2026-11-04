package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppTitlePolicyTest {
    @Test public void olderSettingsKeepTheirExistingTitlePlacement(){assertEquals("hero",AppTitlePolicy.mode(null));assertEquals("hero",AppTitlePolicy.mode("unknown"));assertEquals("focused",AppTitlePolicy.mode("focused"));}
    @Test public void captionClearsAnInstantFrameBeforeZoomHasFinished(){assertEquals(222f,AppTitlePolicy.captionTop(100,100,1f,1.12f,10,6),.001f);assertEquals(226f,AppTitlePolicy.captionTop(100,100,1.20f,1.12f,10,6),.001f);}
    @Test public void reservedSpaceKeepsCaptionInsideTheScreen(){float room=AppTitlePolicy.room(100,1.12f,10,24,6);float cardTop=540-100-room;assertEquals(540,AppTitlePolicy.captionTop(cardTop,100,1.12f,1.12f,10,6)+24,.001f);}
    @Test public void outerDockSlotsKeepTheirNamesOnScreen(){assertEquals(8,AppTitlePolicy.captionLeft(0,180,960,8),.001f);assertEquals(772,AppTitlePolicy.captionLeft(960,180,960,8),.001f);assertEquals(390,AppTitlePolicy.captionLeft(480,180,960,8),.001f);}
}
