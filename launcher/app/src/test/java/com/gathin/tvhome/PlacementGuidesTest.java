package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlacementGuidesTest {
    @Test public void alignsLeftCenterAndRight() {
        int[] guides = {0, 50, 100};
        assertEquals(0, PlacementGuides.snap(5, 20, 100, guides));
        assertEquals(40, PlacementGuides.snap(43, 20, 100, guides));
        assertEquals(80, PlacementGuides.snap(76, 20, 100, guides));
    }

    @Test public void outOfRangeAnchorsDoNotPushTheWidgetOffScreen() {
        assertEquals(0, PlacementGuides.snap(0, 20, 100, new int[]{-1, 19}));
        assertEquals(71, PlacementGuides.snap(71, 20, 100, new int[]{100}));
    }

    @Test public void theNearestAnchorWinsAndTheThresholdIsInclusive() {
        assertEquals(30, PlacementGuides.snap(24, 10, 100, new int[]{40}));
        assertEquals(23, PlacementGuides.snap(23, 10, 100, new int[]{40}));
        assertEquals(22, PlacementGuides.snap(23, 8, 100, new int[]{30}));
    }
}
