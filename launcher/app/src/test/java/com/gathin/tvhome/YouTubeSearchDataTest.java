package com.gathin.tvhome;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class YouTubeSearchDataTest {
    @Test public void extractsDataWithoutEvaluatingSurroundingScripts() throws Exception {
        String json = "{\"contents\":{\"title\":\"A } \\\"quoted\\\" { title\"}}";
        assertEquals(json, YouTubeSearchData.initialData("<script>var ytInitialData = " + json
                + ";window.unrelated();</script>"));
    }
    @Test public void supportsWindowAssignmentAndNestedArrays() throws Exception {
        String json = "{\"contents\":[{\"id\":\"1\"},{\"id\":\"2\"}]}";
        assertEquals(json, YouTubeSearchData.initialData("window[\"ytInitialData\"]=" + json + ";"));
    }
    @Test(expected = IOException.class) public void rejectsConsentPagesWithoutResults() throws Exception {
        YouTubeSearchData.initialData("<html>Please sign in</html>");
    }
    @Test(expected = IOException.class) public void rejectsTruncatedResults() throws Exception {
        YouTubeSearchData.initialData("var ytInitialData = {\"contents\":[");
    }
}
