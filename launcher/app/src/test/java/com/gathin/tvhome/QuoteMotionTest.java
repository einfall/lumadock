package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class QuoteMotionTest {
 @Test public void screenEntryStartsAboveTheScreenAndEndsAtTheLetter(){for(float origin:new float[]{0,200,850}){assertTrue(origin+QuoteMotion.enter(60,origin,1080,25,0)<0);assertEquals(60,QuoteMotion.enter(60,origin,1080,25,1),.001f);}}
 @Test public void exitEndsBelowTheScreenEvenWhenTheWidgetIsAtTheTop(){for(float origin:new float[]{0,200,850}){assertEquals(60,QuoteMotion.exit(60,origin,1080,25,0),.001f);assertTrue(origin+QuoteMotion.exit(60,origin,1080,25,1)>1080);}}
 @Test public void pathsAlwaysMoveDownAcrossTheirEntireRange(){for(float origin:new float[]{0,200,850}){float a=-Float.MAX_VALUE,b=-Float.MAX_VALUE;for(int i=0;i<=100;i++){float q=i/100f,x=QuoteMotion.enter(60,origin,1080,30,q),y=QuoteMotion.exit(60,origin,1080,30,q);assertTrue(x>=a);assertTrue(y>=b);a=x;b=y;}}}
 @Test public void screenParticlesRemainVisibleAcrossTheDockAndFadeAtTheEnd(){assertEquals(1,QuoteMotion.alpha(false,true,.7f),.001f);assertEquals(0,QuoteMotion.alpha(false,true,1),.001f);assertEquals(0,QuoteMotion.alpha(true,true,0),.001f);assertEquals(1,QuoteMotion.alpha(true,true,1),.001f);for(int i=0;i<=100;i++)for(boolean screen:new boolean[]{true,false})for(boolean entering:new boolean[]{true,false}){float a=QuoteMotion.alpha(entering,screen,i/100f);assertTrue(a>=0&&a<=1);}}
 @Test public void oldAndUnknownPathsKeepWidgetClipping(){assertEquals("widget",QuoteMotion.path(null));assertEquals("widget",QuoteMotion.path("unknown"));assertEquals("screen",QuoteMotion.path("screen"));}
}
