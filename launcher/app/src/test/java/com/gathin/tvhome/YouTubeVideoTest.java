package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class YouTubeVideoTest {
    @Test public void pastedLinksShareOneIdentity() {
        String id = "aqz-KE-bpKQ";
        for (String value : new String[]{id, "https://www.youtube.com/watch?v=" + id + "&t=45",
                "https://youtu.be/" + id + "?si=example", "https://m.youtube.com/watch?feature=share&v=" + id,
                "https://www.youtube.com/shorts/" + id, "https://www.youtube.com/live/" + id,
                "https://www.youtube-nocookie.com/embed/" + id}) assertEquals(value, id, YouTubeVideo.id(value));
    }

    @Test public void rejectsLookalikeHostsAndNonVideoLinks() {
        for (String value : new String[]{"https://youtube.com.evil.example/watch?v=aqz-KE-bpKQ",
                "https://youtube.com@evil.example/watch?v=aqz-KE-bpKQ", "https://youtu.be/aqz-KE-bpKQ/other",
                "javascript:aqz-KE-bpKQ", "https://www.youtube.com/playlist?list=example",
                "https://www.youtube.com/watch?v=too-short", "https://youtu.be:443/aqz-KE-bpKQ", "", "https://youtu.be"}) {
            assertEquals(value, "", YouTubeVideo.id(value));
        }
        assertEquals("", YouTubeVideo.id(null));
    }

    @Test public void canonicalLinksDoNotCarryTrackingOrStartOffsets() {
        YouTubeVideo video = new YouTubeVideo("aqz-KE-bpKQ", "Test", "Channel");
        assertEquals("https://www.youtube.com/watch?v=aqz-KE-bpKQ", video.url());
        assertEquals("https://i.ytimg.com/vi/aqz-KE-bpKQ/mqdefault.jpg", video.thumbnail());
    }
}
