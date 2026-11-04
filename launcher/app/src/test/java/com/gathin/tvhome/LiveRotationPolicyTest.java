package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
public class LiveRotationPolicyTest {
 @Test public void noPoolAndSingleVideoAreSafe(){assertEquals(-1,LiveRotationPolicy.nextIndex(0,-1,7));assertEquals(0,LiveRotationPolicy.nextIndex(1,0,7));}
 @Test public void everyOtherVideoCanBeChosenWithoutImmediateRepeat(){for(int count=2;count<=289;count++)for(int current=0;current<count;current++){HashSet<Integer> choices=new HashSet<>();for(int draw=0;draw<count-1;draw++){int n=LiveRotationPolicy.nextIndex(count,current,draw);assertNotEquals(current,n);assertTrue(n>=0&&n<count);choices.add(n);}assertEquals(count-1,choices.size());}}
 @Test public void unknownSelectionCanChooseAnyItem(){for(int i=0;i<12;i++)assertEquals(i,LiveRotationPolicy.nextIndex(12,-1,i));}
 @Test public void intervalIsMeasuredFromLastSelection(){assertEquals(40000,LiveRotationPolicy.delay(30000,10000,7,7,1));assertEquals(0,LiveRotationPolicy.delay(70000,10000,7,7,1));assertEquals(0,LiveRotationPolicy.delay(90000,10000,7,7,1));}
 @Test public void rebootOrInvalidTimeStartsNewFullInterval(){assertEquals(60000,LiveRotationPolicy.delay(30000,10000,6,7,1));assertEquals(60000,LiveRotationPolicy.delay(30000,0,7,7,1));assertEquals(60000,LiveRotationPolicy.delay(30000,90000,7,7,1));}
 @Test public void onlyExplicitModesCanRotate(){assertEquals("off",LiveRotationPolicy.mode("unknown"));assertEquals("off",LiveRotationPolicy.mode(null));for(String mode:new String[]{"interval","home","tv"})assertEquals(mode,LiveRotationPolicy.mode(mode));}
 @Test public void intervalCannotSpinOrOverflow(){assertEquals(1,LiveRotationPolicy.minutes(0));assertEquals(240,LiveRotationPolicy.minutes(Integer.MAX_VALUE));assertEquals(15,LiveRotationPolicy.minutes(15));}
}
