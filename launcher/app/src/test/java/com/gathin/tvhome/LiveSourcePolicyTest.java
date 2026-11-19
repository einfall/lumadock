package com.gathin.tvhome;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class LiveSourcePolicyTest {
    @Test public void keepsLegacyCategoryAndIncludedYoutube() {
        assertEquals(set("A", "youtube"), LiveSourcePolicy.legacy("category", "A", true));
        assertEquals(set("youtube"), LiveSourcePolicy.legacy("youtube", "all", false));
        assertEquals(set("favorites"), LiveSourcePolicy.legacy("favorites", "Z", false));
    }
    @Test public void specificSourcesCanBeCombined() {
        Set<String> sources = LiveSourcePolicy.toggle(set("A"), "favorites", true);
        assertEquals(set("A", "favorites"), sources);
        assertTrue(LiveSourcePolicy.includes(sources, "A", false));
        assertTrue(LiveSourcePolicy.includes(sources, "Z", true));
        assertTrue(LiveSourcePolicy.includes(sources, "youtube", true));
        assertFalse(LiveSourcePolicy.includes(sources, "Z", false));
    }
    @Test public void selectingASpecificSourceReplacesAll() {
        assertEquals(set("A"), LiveSourcePolicy.toggle(set("all"), "A", true));
        assertEquals(set("all"), LiveSourcePolicy.toggle(set("A", "favorites"), "all", true));
    }
    @Test public void anExplicitEmptySelectionStaysEmpty() {
        Set<String> sources = LiveSourcePolicy.toggle(set("A"), "A", false);
        assertTrue(sources.isEmpty());
        assertFalse(LiveSourcePolicy.includes(sources, "A", true));
        assertFalse(LiveSourcePolicy.includes(sources, "youtube", true));
    }
    @Test public void communitySubcategoriesAndUnknownSourcesAreHandled() {
        assertTrue(LiveSourcePolicy.includes(set("C"), "C1", false));
        assertTrue(LiveSourcePolicy.includes(set("all"), "youtube", true));
        assertEquals(set("A"), LiveSourcePolicy.normalize(Arrays.asList("A", "A", "unsupported")));
        assertEquals(set("A"), LiveSourcePolicy.toggle(set("A"), "unsupported", true));
        assertTrue(LiveSourcePolicy.normalize(Collections.emptySet()).isEmpty());
    }
    private static Set<String> set(String... values) { return new LinkedHashSet<>(Arrays.asList(values)); }
}
