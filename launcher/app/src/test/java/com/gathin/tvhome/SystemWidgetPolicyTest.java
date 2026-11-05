package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SystemWidgetPolicyTest {
 @Test public void automaticSizesPreservePreviousLayouts(){assertEquals(166,SystemWidgetPolicy.natural("filled",7));assertEquals(206,SystemWidgetPolicy.natural("resources",7));assertEquals(172,SystemWidgetPolicy.natural("compact",7));assertEquals(177,SystemWidgetPolicy.natural("pastel",7));}
 @Test public void NetworkAddsOneRowAndMinimumLeavesRoomForAllValues(){for(String style:new String[]{"filled","resources","compact","pastel"}){assertTrue(SystemWidgetPolicy.minimum("system",style,15)>SystemWidgetPolicy.minimum("system",style,7));assertTrue(SystemWidgetPolicy.natural(style,15)>=SystemWidgetPolicy.minimum("system",style,15));}}
 @Test public void hiddenMetricsStillHaveAUsablePlaceholder(){assertEquals(1,SystemWidgetPolicy.rows(0));assertEquals(4,SystemWidgetPolicy.rows(255));assertEquals(SystemWidgetPolicy.natural("compact",1),SystemWidgetPolicy.natural("compact",0));}
 @Test public void userHeightCanExpandAndIsClampedToReadableScreenBounds(){assertEquals(148,SystemWidgetPolicy.height(10,148,540));assertEquals(400,SystemWidgetPolicy.height(400,148,540));assertEquals(540,SystemWidgetPolicy.height(900,148,540));assertEquals(120,SystemWidgetPolicy.height(900,148,120));}
 @Test public void glassOpacityCannotProduceAnOpaqueOrInvalidSurface(){assertEquals(0,SystemWidgetPolicy.opacity(-1));assertEquals(45,SystemWidgetPolicy.opacity(45));assertEquals(90,SystemWidgetPolicy.opacity(100));}
}
