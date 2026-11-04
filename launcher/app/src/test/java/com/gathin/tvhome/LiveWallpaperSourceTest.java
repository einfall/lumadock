package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class LiveWallpaperSourceTest {
 @Test public void mediaLocationsSupportOnlineAndLocal(){for(String s:new String[]{"https://example.org/video.mov?token=a%20b","http://192.168.1.2:8080/live.mp4","/sdcard/Movies/My Video.mp4","file:///sdcard/Movies/sky.mp4","content://media/external/video/media/1"})assertTrue(s,LiveWallpaperSource.valid(s));}
 @Test public void rejectsAppCommandsAndBrokenLocations(){for(String s:new String[]{"","   ","intent://something","javascript:alert(1)","https:///missing-host","file://other-machine/video.mov","C:\\video.mp4","relative.mp4","https://example.org:99999/v","/sdcard/a\ncmd"})assertFalse(s,LiveWallpaperSource.valid(s));assertFalse(LiveWallpaperSource.valid(null));}
 @Test public void dimensionlessBuffersNeverDivideByZero(){assertArrayEquals(new float[]{1,1},LiveWallpaperSource.cover(0,1080,1920,1080),0);assertArrayEquals(new float[]{1,1},LiveWallpaperSource.cover(1920,1080,0,0),0);}
 @Test public void cropsWideAndTallVideosWithoutStretching(){assertArrayEquals(new float[]{1,1},LiveWallpaperSource.cover(1920,1080,3840,2160),.001f);assertArrayEquals(new float[]{2,1},LiveWallpaperSource.cover(1920,1080,3840,1080),.001f);assertArrayEquals(new float[]{1,2},LiveWallpaperSource.cover(1920,1080,1920,2160),.001f);}
 @Test public void dimIsBounded(){assertEquals(0,LiveWallpaperSource.dim(-20));assertEquals(90,LiveWallpaperSource.dim(100));assertEquals(35,LiveWallpaperSource.dim(35));}
}
