package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class TileEffectsTest {
 @Test public void profileFlagsAreIndependent(){TileEffects e=new TileEffects(false,true,true,false,"dominant",4);assertFalse(e.line());assertTrue(e.glass);assertTrue(e.shine);assertEquals("dominant",e.color);assertEquals(4,e.gap);}
 @Test public void thinLineCanExistWithoutFullRim(){assertTrue(new TileEffects(false,false,false,true,"blue",1).line());assertTrue(new TileEffects(true,false,false,false,"red",1).line());}
 @Test public void corruptSettingsAreBounded(){assertEquals(0,new TileEffects(true,false,false,false,"invalid",-2).gap);assertEquals(5,new TileEffects(true,false,false,false,"blue",100).gap);assertEquals("original",TileEffects.validColor(null));}
}
