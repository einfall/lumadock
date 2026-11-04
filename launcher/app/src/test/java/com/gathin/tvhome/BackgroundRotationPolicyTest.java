package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class BackgroundRotationPolicyTest {
    @Test public void legacyVideoRotationIsPreservedButAnIdleStaticBackgroundIsFixed(){assertEquals("rotation",BackgroundRotationPolicy.mode(null,true,true));assertEquals("fixed",BackgroundRotationPolicy.mode(null,false,true));assertEquals("fixed",BackgroundRotationPolicy.mode(null,true,false));}
    @Test public void staticItemsDoNotTurnOffAnExplicitMixedRotation(){assertEquals("rotation",BackgroundRotationPolicy.mode("rotation",false,true));assertEquals("fixed",BackgroundRotationPolicy.mode("fixed",true,true));assertEquals("fixed",BackgroundRotationPolicy.mode("bad",true,true));}
    @Test public void absentTypesPreserveVideosOnlyButExplicitEmptyPauses(){assertEquals(Collections.singleton("live"),BackgroundRotationPolicy.types(null));assertTrue(BackgroundRotationPolicy.types(Collections.emptySet()).isEmpty());}
    @Test public void unsupportedAndDuplicateTypesCannotEnterThePool(){assertEquals(new LinkedHashSet<>(Arrays.asList("live","photo","gradient")),BackgroundRotationPolicy.types(new HashSet<>(Arrays.asList("gradient","photo","live","broken"))));}
    @Test public void emptyFavoritesDoNotPreventPicturesAndGradients(){assertEquals(Arrays.asList("photo","gradient"),BackgroundRotationPolicy.available(new HashSet<>(Arrays.asList("live","photo","gradient")),0,12,6));assertTrue(BackgroundRotationPolicy.available(Collections.singleton("live"),0,12,6).isEmpty());}
    @Test public void largeVideoCatalogDoesNotDrownOutTheOtherTypes(){List<String> pool=BackgroundRotationPolicy.available(new HashSet<>(Arrays.asList("live","photo","gradient")),289,12,6);Map<String,Integer> seen=new HashMap<>();for(int n=0;n<300;n++){String kind=BackgroundRotationPolicy.kind(pool,"live",false,n);seen.put(kind,seen.getOrDefault(kind,0)+1);}assertEquals(Integer.valueOf(100),seen.get("live"));assertEquals(seen.get("live"),seen.get("photo"));assertEquals(seen.get("live"),seen.get("gradient"));}
    @Test public void aSingletonDoesNotRepeatWhileAnotherTypeIsAvailable(){for(int n=-5;n<5;n++)assertEquals("photo",BackgroundRotationPolicy.kind(Arrays.asList("live","photo"),"live",true,n));assertEquals("live",BackgroundRotationPolicy.kind(Collections.singletonList("live"),"live",true,7));}
    @Test public void noSourcesAndNegativeRandomDrawAreSafe(){assertNull(BackgroundRotationPolicy.kind(Collections.emptyList(),"gradient",false,0));assertNotNull(BackgroundRotationPolicy.kind(Arrays.asList("photo","gradient"),"live",false,Integer.MIN_VALUE));}
}
