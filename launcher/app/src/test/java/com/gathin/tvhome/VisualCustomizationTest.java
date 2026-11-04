package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VisualCustomizationTest {
    @Test public void invalidOrMissingFontsKeepTheOriginal(){assertEquals("default",FontPolicy.id(null));assertEquals("default",FontPolicy.id("remote.ttf"));assertEquals("default",FontPolicy.id("INTER"));for(String id:FontPolicy.IDS)assertEquals(id,FontPolicy.id(id));}
    @Test public void packActivityAliasesNormalizeToTheSameComponent(){assertEquals("com.example.tv/com.example.tv.Home",IconPackPolicy.component("ComponentInfo{com.example.tv/.Home}"));assertEquals("com.example.tv/com.example.tv.Home",IconPackPolicy.component("com.example.tv/com.example.tv.Home"));assertEquals("com.example.tv/com.example.tv.Home$Alias",IconPackPolicy.component("com.example.tv/.Home$Alias"));}
    @Test public void malformedMappingsCannotSelectArbitraryResources(){for(String s:new String[]{null,"","no-slash","com.app/","com.app/.A/B","ComponentInfo{com.app/.A","com.app/.Home\nscript","https://icons.com/logo"})assertEquals("",IconPackPolicy.component(s));assertTrue(IconPackPolicy.drawable("tv_netflix_2"));for(String s:new String[]{"../icon","@drawable/logo","Icon","https://icon","","a/b"})assertFalse(IconPackPolicy.drawable(s));}
}
