package com.gathin.tvhome;
import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;
public class SaverPolicyTest {
 @Test public void oldOrUnknownSettingKeepsExistingSaver(){assertEquals("legacy",SaverPolicy.mode(null));assertEquals("legacy",SaverPolicy.mode("../photos"));assertEquals("video",SaverPolicy.mode("video"));}
 @Test public void assetIdentifiersNeverEscapeThePhotoDirectory(){assertTrue(SaverPolicy.photoId("w00.jpg"));assertFalse(SaverPolicy.photoId("../w00.jpg"));assertFalse(SaverPolicy.photoId("file:///sdcard/w00.jpg"));assertFalse(SaverPolicy.photoId(null));}
 @Test public void sequentialSlidesWrapAndSinglePhotoIsStable(){assertEquals(0,SaverPolicy.next(10,9,false,new Random(1)));assertEquals(4,SaverPolicy.next(10,3,false,new Random(1)));assertEquals(0,SaverPolicy.next(1,0,true,new Random(1)));assertEquals(0,SaverPolicy.next(0,0,true,new Random(1)));}
 @Test public void randomNeverRepeatsCurrentPhoto(){Random r=new Random(42);for(int n=2;n<20;n++)for(int current=0;current<n;current++)for(int i=0;i<50;i++){int next=SaverPolicy.next(n,current,true,r);assertTrue(next>=0&&next<n);assertNotEquals(current,next);}}
 @Test public void corruptedDelayIsBoundedAndZeroStaysOff(){assertEquals(0,SaverPolicy.minutes(-1));assertEquals(0,SaverPolicy.minutes(0));assertEquals(120,SaverPolicy.minutes(Integer.MAX_VALUE));}
}
