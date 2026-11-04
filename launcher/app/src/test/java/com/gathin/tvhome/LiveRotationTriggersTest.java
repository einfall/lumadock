package com.gathin.tvhome;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LiveRotationTriggersTest {
    @Test public void existingSingleChoiceMigratesWithoutEnablingOthers(){for(String id:LiveRotationPolicy.TRIGGERS)assertEquals(Collections.singleton(id),LiveRotationPolicy.triggers(null,id));assertTrue(LiveRotationPolicy.triggers(Collections.emptySet(),"home").isEmpty());}
    @Test public void allThreeCanRemainEnabled(){Set<String> s=new HashSet<>();for(String id:LiveRotationPolicy.TRIGGERS)s=LiveRotationPolicy.withTrigger(s,id,true);assertEquals(3,s.size());assertEquals("interval",LiveRotationPolicy.legacyMode(s));}
    @Test public void offClearsEveryTriggerAndLastUntickReturnsOff(){Set<String> s=new HashSet<>(Arrays.asList(LiveRotationPolicy.TRIGGERS));assertTrue(LiveRotationPolicy.withTrigger(s,"off",true).isEmpty());assertTrue(LiveRotationPolicy.withTrigger(Collections.singleton("home"),"home",false).isEmpty());assertEquals(3,s.size());}
    @Test public void storedSetsAreCopiedAndInvalidModesCannotRun(){Set<String> saved=new HashSet<>(Arrays.asList("home","unknown"));Set<String> next=LiveRotationPolicy.triggers(saved,"tv");next.clear();assertEquals(2,saved.size());assertTrue(LiveRotationPolicy.triggers(new HashSet<>(Arrays.asList("off","tv")),"home").isEmpty());}
    @Test public void screenOnThenResumeSelectsOnlyOnce(){LiveRotationWake gate=new LiveRotationWake(false);assertTrue(gate.screenOn(true,true,1000));assertFalse(gate.start(true,true,false,1100));assertTrue(gate.start(true,true,false,5000));}
    @Test public void resumeThenScreenOnSelectsOnlyOnce(){LiveRotationWake gate=new LiveRotationWake(false);assertTrue(gate.start(true,true,true,1000));assertFalse(gate.screenOn(true,true,1100));}
    @Test public void pausedScreenOnIsDeferredUntilHomeAndEachWakeCanSelect(){LiveRotationWake gate=new LiveRotationWake(true);gate.off();assertFalse(gate.screenOn(true,false,1000));assertTrue(gate.start(true,true,true,4000));gate.off();assertTrue(gate.screenOn(true,true,8000));}
    @Test public void enablingTvIsNotItselfAWake(){LiveRotationWake gate=new LiveRotationWake(false);gate.prime();assertFalse(gate.screenOn(true,true,1000));gate.off();assertTrue(gate.screenOn(true,true,2000));}
    @Test public void disabledTvDoesNotSuppressHome(){LiveRotationWake gate=new LiveRotationWake(false);assertFalse(gate.screenOn(false,true,1000));assertTrue(gate.start(true,false,true,1100));assertFalse(gate.start(false,false,true,1200));}
}
