package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppTitlePolicyTest {
    @Test public void olderSettingsKeepTheirExistingTitlePlacement(){assertEquals("hero",AppTitlePolicy.mode(null));assertEquals("hero",AppTitlePolicy.mode("unknown"));assertEquals("focused",AppTitlePolicy.mode("focused"));}
    @Test public void alignmentUsesTheDockEdgesAndClampsWideLabels() {
        assertEquals(100, AppTitlePolicy.alignedLeft("left", 100, 800, 160, 960));
        assertEquals(370, AppTitlePolicy.alignedLeft("center", 100, 800, 160, 960));
        assertEquals(640, AppTitlePolicy.alignedLeft("right", 100, 800, 160, 960));
        assertEquals(60, AppTitlePolicy.alignedLeft("right", 100, 800, 900, 960));
    }

    @Test public void existingManualCoordinatesAndSlideAreKept() {
        assertEquals("manual", AppTitlePolicy.alignment(null, true));
        assertEquals("left", AppTitlePolicy.alignment(null, false));
        assertEquals("center", AppTitlePolicy.alignment("center", true));
        assertEquals("slide", AppTitlePolicy.transition(null));
        assertEquals("fade", AppTitlePolicy.transition("fade"));
        assertEquals("none", AppTitlePolicy.transition("none"));
    }

    @Test public void captionClearsAnInstantFrameBeforeZoomHasFinished(){assertEquals(222f,AppTitlePolicy.captionTop(100,100,1f,1.12f,10,6),.001f);assertEquals(226f,AppTitlePolicy.captionTop(100,100,1.20f,1.12f,10,6),.001f);}
    @Test public void reservedSpaceKeepsCaptionInsideTheScreen(){float room=AppTitlePolicy.room(100,1.12f,10,24,6);float cardTop=540-100-room;assertEquals(540,AppTitlePolicy.captionTop(cardTop,100,1.12f,1.12f,10,6)+24,.001f);}
    @Test public void outerDockSlotsKeepTheirNamesOnScreen(){assertEquals(8,AppTitlePolicy.captionLeft(0,180,960,8),.001f);assertEquals(772,AppTitlePolicy.captionLeft(960,180,960,8),.001f);assertEquals(390,AppTitlePolicy.captionLeft(480,180,960,8),.001f);}
}
