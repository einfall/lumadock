package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class DockPlacementPolicyTest {
    @Test public void dockStaysInsideHorizontalViewport() {
        assertEquals(600,DockPlacementPolicy.width(1000,60));
        assertEquals(200,DockPlacementPolicy.offset(1000,600,50));
        assertEquals(400,DockPlacementPolicy.offset(1000,600,100));
        assertEquals(0,DockPlacementPolicy.offset(500,600,100));
    }
    @Test public void positionRespectsBothEdgesAndMalformedPreferences() {
        assertEquals(22,DockPlacementPolicy.top(22,400,-5));
        assertEquals(400,DockPlacementPolicy.top(22,400,150));
        assertEquals(211,DockPlacementPolicy.top(22,400,50));
        assertEquals(22,DockPlacementPolicy.top(22,10,50));
        assertEquals(600,DockPlacementPolicy.width(1000,-1));
        assertEquals(1000,DockPlacementPolicy.width(1000,200));
    }
    @Test public void lastPartialPageAndInvalidCountsAreSafe() {
        assertEquals(3,DockPlacementPolicy.pages(13,5));
        assertEquals(2,DockPlacementPolicy.page(12,13,5));
        assertEquals(2,DockPlacementPolicy.page(100,13,5));
        assertEquals(0,DockPlacementPolicy.page(-2,13,5));
        assertEquals(0,DockPlacementPolicy.pages(0,5));
        assertEquals(3,DockPlacementPolicy.pages(3,0));
    }
}
