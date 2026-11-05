package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class WidgetThemePolicyTest {
 @Test public void originalKeepsBothExistingMaterialPalettes(){SurfaceStyle b=SurfaceStyle.of("original",true),w=SurfaceStyle.of("original",false);assertEquals(0x2EFFFFFF,b.top);assertEquals(0x14FFFFFF,b.bottom);assertEquals(21,b.radius);assertEquals(0xB3242B35,w.top);assertEquals(0xB30D121B,w.bottom);assertEquals(10,w.radius);}
 @Test public void explicitExistingLooksWorkForEitherComponent(){assertEquals(SurfaceStyle.of("glass",true).top,SurfaceStyle.of("glass",false).top);assertEquals(SurfaceStyle.of("card",false).bottom,SurfaceStyle.of("card",true).bottom);}
 @Test public void themesHaveReadableOpaqueInkAndKnownFallback(){for(String id:SurfaceStyle.IDS){SurfaceStyle s=SurfaceStyle.of(id,false);assertEquals(255,s.ink>>>24);assertEquals(255,s.muted>>>24);assertEquals(255,s.focusInk>>>24);assertTrue(s.radius>0);}assertEquals("original",SurfaceStyle.id(null));assertEquals("original",SurfaceStyle.id("unknown"));assertEquals(0xFF101928,SurfaceStyle.of("light",false).ink);}
 @Test public void everyInternalSourceHasOneStableDemandMask(){assertEquals(1,WidgetSource.CPU.mask);assertEquals(2,WidgetSource.RAM.mask);assertEquals(4,WidgetSource.STORAGE.mask);assertEquals(7,WidgetSource.SYSTEM.mask);assertSame(WidgetSource.RAM,WidgetSource.find("ram"));assertNull(WidgetSource.find("remote executable"));}
 @Test public void instancesUnionTheirDemandAndNeverMultiplyIt(){int external=WidgetSource.CPU.mask|WidgetSource.CPU.mask|WidgetSource.SYSTEM.mask;assertEquals(7,WidgetSource.demand(true,7,external));assertEquals(1,WidgetSource.demand(false,7,1));assertEquals(6,WidgetSource.demand(true,2,4));}
 @Test public void allHiddenMeansNoReaderButSeparateInstancesStillWork(){assertEquals(0,WidgetSource.demand(false,7,0));assertEquals(0,WidgetSource.demand(true,0,0));assertEquals(4,WidgetSource.demand(false,7,4));assertEquals(15,WidgetSource.demand(false,0,255));}
 @Test public void widgetWidthStaysOnScreenAcrossResolutions(){assertEquals(480,WidgetSource.width("system",900,960));assertEquals(140,WidgetSource.width("system",430,140));assertEquals(64,WidgetSource.width("cpu",-10,960));assertEquals(960,WidgetSource.width("android",1200,960));assertEquals(1,WidgetSource.width("system",500,0));}
 @Test public void externalAndroidSourcesDoNotRequestCpuData(){assertNull(WidgetSource.find("android"));assertEquals(0,WidgetSource.demand(false,0,0));}
}
