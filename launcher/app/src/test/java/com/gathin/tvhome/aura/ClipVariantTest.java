package com.gathin.tvhome.aura;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClipVariantTest {
    private static final String URL="https://upload.wikimedia.org/wikipedia/commons/transcoded/clip.480p.vp9.webm",TYPE="video/webm; codecs=\"vp9, opus\"";
    @Test public void av1OriginalCannotPassVp9HardwareGate(){assertEquals(0,ClipVariant.rank("video/webm; codecs=\"av01\"",URL,1280,864,100000,120,35000000));assertTrue(ClipVariant.rank(TYPE,URL,720,480,226216,127,35000000)>0);}
    @Test public void highestCompatibleResolutionWins(){assertTrue(ClipVariant.rank(TYPE,URL,720,480,226216,127,35000000)>ClipVariant.rank(TYPE,URL,360,240,50696,127,35000000));assertEquals(0,ClipVariant.rank(TYPE,URL,3840,2160,100000,120,35000000));}
    @Test public void privateAndUntrustedDestinationsAreRejected(){for(String u:new String[]{"http://upload.wikimedia.org/a","https://upload.wikimedia.org.evil.test/a","https://user:pass@upload.wikimedia.org/a","file:///data/a"})assertEquals(0,ClipVariant.rank(TYPE,u,720,480,1000,120,35000000));}
    @Test public void variantsMustFitRemainingBudget(){assertEquals(0,ClipVariant.rank(TYPE,URL,720,480,1000000,120,1000000));assertEquals(0,ClipVariant.rank(TYPE,URL,720,480,1000,Double.NaN,35000000));}
}
