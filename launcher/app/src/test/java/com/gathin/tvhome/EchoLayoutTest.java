package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EchoLayoutTest {
    @Test public void automaticRecognizesAllFivePresetSizes() {
        for (EchoLayout layout : EchoLayout.values()) {
            if (layout == EchoLayout.AUTO) continue;
            assertEquals(layout, EchoLayout.AUTO.resolve(layout.width, layout.height));
        }
    }

    @Test public void oldSettingsAndExplicitChoicesKeepTheirLayout() {
        assertEquals(EchoLayout.COMPACT, EchoLayout.selected(""));
        assertEquals(EchoLayout.TALL, EchoLayout.selected("tall").resolve(320, 82));
    }
}
