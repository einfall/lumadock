package com.gathin.tvhome;

import java.net.URI;
import org.junit.Test;
import static org.junit.Assert.*;

public class EchoConnectionTest {
    @Test public void acceptsHttpsAndPrivateHttpButRejectsPublicCleartext() {
        assertEquals(URI.create("http://192.168.1.2:8123/"), EchoConnection.base("http://192.168.1.2:8123"));
        assertNotNull(EchoConnection.base("https://ha.example.com/home"));
        assertNull(EchoConnection.base("http://8.8.8.8:8123"));
        assertNull(EchoConnection.base("http://192.168.1.2.example.com"));
        assertNull(EchoConnection.base("https://user@ha.example.com"));
    }

    @Test public void coverMayOnlyUseTheConfiguredOrigin() {
        URI base = URI.create("https://ha.example.com/");
        assertEquals(URI.create("https://ha.example.com/api/cover?key=value"),
                EchoConnection.cover(base, "/api/cover?key=value"));
        assertNotNull(EchoConnection.cover(base, "https://ha.example.com:443/cover"));
        assertNull(EchoConnection.cover(base, "//other.example.com/cover"));
        assertNull(EchoConnection.cover(base, "http://ha.example.com/cover"));
        assertNull(EchoConnection.cover(base, "https://ha.example.com:8123/cover"));
    }

    @Test public void entityIdCannotAlterTheApiPath() {
        assertTrue(EchoConnection.validEntity("media_player.echo_show"));
        assertFalse(EchoConnection.validEntity("media_player.echo_show/other"));
        assertFalse(EchoConnection.validEntity("sensor.echo_show"));
    }
}
