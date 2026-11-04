package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class TileOutlineTest {
 @Test public void wideningBorderNeverConsumesRequestedGap(){
  for(int gap=0;gap<=5;gap++)for(int width=1;width<=8;width++){
   float center=TileOutline.outset(gap,width,false,1);
   assertEquals(gap,center-width/2f,0f);
  }
 }
 @Test public void contrastKeepsClearGapOutsideItsOwnWidth(){
  for(int gap=0;gap<=5;gap++)for(int width=1;width<=8;width++)for(int contrast=1;contrast<=3;contrast++){
   float center=TileOutline.outset(gap,width,true,contrast);
   assertEquals(gap,center-(width+2*contrast)/2f,0f);
  }
 }
 @Test public void physicalGapDoesNotDependOnDensity(){
  float expanded=TileOutline.outset(5,3,true,1);
  for(float zoom:new float[]{1f,1.1f,1.12f,1.2f})assertEquals(5,(expanded/zoom-(3/2f+1)/zoom)*zoom,.0001f);
 }
 @Test public void fineCoverEdgeUsesWhiteAndFadesToTransparent(){int[] colors=TileOutline.nuvioColors();assertEquals(71,colors[0]>>>24);assertEquals(24,colors[1]>>>24);assertEquals(0,colors[2]>>>24);for(int c:colors)assertEquals(0xFFFFFF,c&0xFFFFFF);}
 @Test public void noLiftInDockOrWithInstantFocus(){assertEquals(0,TileOutline.focusLift(true,true,true,380,2),0);assertEquals(0,TileOutline.focusLift(false,true,true,0,2),0);assertEquals(0,TileOutline.focusLift(false,false,true,380,2),0);assertEquals(-8,TileOutline.focusLift(false,true,true,380,2),0);}
 @Test public void newEffectsDoNotForceOldFrameOn(){TileEffects e=new TileEffects(false,false,false,false,"dominant",4,false,1,3,true,true,18,6);assertFalse(e.line());assertTrue(e.nuvio);assertTrue(e.glow);assertEquals(3,e.width);}
 @Test public void legacyProfilesDoNotEnableNewEdgeOrGlow(){TileEffects e=new TileEffects(true,false,true,true,"red",5);assertFalse(e.nuvio);assertFalse(e.glow);assertTrue(e.thin);assertTrue(e.shine);}
 @Test public void malformedGlowCannotExpandAcrossTheScreen(){TileEffects e=new TileEffects(true,false,false,false,"white",0,false,1,99,false,true,999,999);assertEquals(8,e.width);assertEquals(60,e.glowStrength);assertEquals(12,e.glowSpread);}
}
