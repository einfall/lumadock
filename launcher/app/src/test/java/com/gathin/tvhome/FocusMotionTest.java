package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class FocusMotionTest {
 @Test public void hiddenFrameKeepsZoom(){FocusMotion m=new FocusMotion(false,true,true,true,true);assertFalse(m.overlay);assertEquals(1.10f,m.scale(true,false),.0001f);assertEquals(1.12f,m.scale(true,true),.0001f);}
 @Test public void disabledZoomOrAnimationsKeepsOriginalSize(){assertEquals(1f,new FocusMotion(true,false,true,true,true).scale(true,true),0);assertEquals(1f,new FocusMotion(true,true,true,true,true).scale(false,true),0);}
 @Test public void inAndOutCanBeIndependent(){FocusMotion m=new FocusMotion(false,true,false,true,true);assertEquals(0,m.duration(true,false,false));assertEquals(280,m.duration(false,false,false));m=new FocusMotion(false,true,true,false,false);assertEquals(260,m.duration(true,false,false));assertEquals(0,m.duration(false,false,false));}
 @Test public void reducedMotionSuppressesBounce(){FocusMotion m=new FocusMotion(false,true,true,true,true);assertTrue(m.useBounce(false));assertFalse(m.useBounce(true));assertEquals(60,m.duration(false,true,false));assertFalse(new FocusMotion(false,false,true,true,true).useBounce(false));}
}
