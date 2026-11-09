package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IconArtworkFitTest {
    @Test public void squareAndWideIconsFitTheSameSlotWithoutDistortion() {
        assertEquals(0.5f, IconPackPolicy.fitScale(320, 320, 280, 160), 0.0001f);
        assertEquals(0.875f, IconPackPolicy.fitScale(320, 120, 280, 160), 0.0001f);
    }

    @Test public void differentResolutionKeepsTheSameRenderedDimensions() {
        float small = IconPackPolicy.fitScale(160, 90, 280, 160);
        float large = IconPackPolicy.fitScale(640, 360, 280, 160);
        assertEquals(160 * small, 640 * large, 0.0001f);
        assertEquals(90 * small, 360 * large, 0.0001f);
    }
}
