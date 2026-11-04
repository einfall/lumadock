package com.gathin.tvhome;
import org.junit.Test;import static org.junit.Assert.*;
public class HeaderLayoutPolicyTest {
 private int[][] base(){return new int[][]{{60,26},{56,50},{320,50},{210,50},{56,116},{201,116},{346,116}};}
 private int[][] sizes(){return new int[][]{{100,14},{125,52},{154,48},{96,48},{140,65},{140,65},{140,65}};}
 @Test public void legacyPreservesEveryOriginalPositionAndDoesNotMutateInput(){int[][] b=base(),p=HeaderLayoutPolicy.preset("legacy",b,sizes());for(int i=0;i<b.length;i++){assertArrayEquals(b[i],p[i]);assertNotSame(b[i],p[i]);}}
 @Test public void rowOrdersClockThenDateThenWeather(){int[][] p=HeaderLayoutPolicy.preset("row",base(),sizes());assertEquals(56,p[1][0]);assertEquals(205,p[2][0]);assertEquals(371,p[3][0]);assertEquals(52,p[2][1]);assertEquals(52,p[3][1]);assertArrayEquals(base()[4],p[4]);}
 @Test public void stackedDateAndSystemPillsDoNotOverlap(){int[][] p=HeaderLayoutPolicy.preset("stack",base(),sizes());assertEquals(p[1][0],p[2][0]);assertEquals(110,p[2][1]);assertEquals(170,p[4][1]);assertEquals(205,p[3][0]);for(int i=4;i<7;i++)assertEquals(56+(i-4)*145,p[i][0]);}
 @Test public void clampingKeepsEachItemOnScreenAfterResolutionChanges(){assertEquals(0,HeaderLayoutPolicy.clamp(-50,960,140));assertEquals(820,HeaderLayoutPolicy.clamp(999,960,140));assertEquals(300,HeaderLayoutPolicy.clamp(300,960,140));assertEquals(0,HeaderLayoutPolicy.clamp(40,80,140));assertEquals(0,HeaderLayoutPolicy.clamp(40,0,0));}
 @Test public void corruptModesUseTheExistingLayout(){assertEquals("legacy",HeaderLayoutPolicy.mode(null));assertEquals("legacy",HeaderLayoutPolicy.mode("invalid"));assertEquals("stack",HeaderLayoutPolicy.mode("stack"));}
}
