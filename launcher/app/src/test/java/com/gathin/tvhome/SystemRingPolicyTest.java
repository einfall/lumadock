package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SystemRingPolicyTest {
 @Test public void verticalCellsAreEqualAndDoNotOverlap(){float[][] c=SystemRingPolicy.cells(100,320,7,"vertical",10);assertEquals(3,c.length);for(int i=0;i<3;i++){assertEquals(50,c[i][0],.001);assertEquals(100,c[i][2],.001);if(i>0)assertEquals(110,c[i][1]-c[i-1][1],.001);}}
 @Test public void horizontalCellsStayCircularAndCentered(){float[][] c=SystemRingPolicy.cells(320,100,7,"horizontal",10);for(int i=0;i<3;i++){assertEquals(50,c[i][1],.001);assertEquals(100,c[i][2],.001);if(i>0)assertEquals(110,c[i][0]-c[i-1][0],.001);}}
 @Test public void tinyBoundsReduceTheGapAndKeepEveryCellInside(){for(String axis:new String[]{"vertical","horizontal"}){float[][] c=SystemRingPolicy.cells(64,32,15,axis,32);for(float[] p:c){assertTrue(p[0]-p[2]/2>=-.001);assertTrue(p[0]+p[2]/2<=64.001);assertTrue(p[1]-p[2]/2>=-.001);assertTrue(p[1]+p[2]/2<=32.001);assertTrue(p[2]>0);}}}
 @Test public void automaticHeightUsesAvailableWidthAndOrientation(){assertEquals(160,SystemRingPolicy.naturalHeight(160,1,"vertical",10));assertEquals(320,SystemRingPolicy.naturalHeight(430,7,"vertical",10));assertEquals(100,SystemRingPolicy.naturalHeight(320,7,"horizontal",10));assertEquals(32,SystemRingPolicy.naturalHeight(64,15,"horizontal",32));}
 @Test public void missingAndOutOfRangeMeasurementsDoNotInventProgress(){assertEquals(0,SystemRingPolicy.sweep(-1),0);assertEquals(0,SystemRingPolicy.sweep(0),0);assertEquals(270,SystemRingPolicy.sweep(75),.001);assertEquals(360,SystemRingPolicy.sweep(200),.001);}
 @Test public void unknownPreferencesFallBackAndStylesRemainStable(){assertEquals("vertical",SystemRingPolicy.axis("invalid"));assertEquals(0,SystemRingPolicy.gap(-1));assertEquals(32,SystemRingPolicy.gap(90));assertTrue(SystemInfoStyle.ring("ring_neon"));assertTrue(SystemInfoStyle.ring("ring_donut"));assertFalse(SystemInfoStyle.ring("compact"));assertEquals("original",SystemInfoStyle.id("unknown"));}
}
