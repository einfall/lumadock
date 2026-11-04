package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SystemStatsPolicyTest {
    @Test public void aggregateCountersIncludeStealButNotGuestTwice(){SystemStatsPolicy.Cpu c=SystemStatsPolicy.parse("cpu 10 20 30 40 5 6 7 8 100 200\n");assertNotNull(c);assertEquals(126,c.total);assertEquals(45,c.idle);}
    @Test public void percentUsesDeltasAcrossAllCores(){SystemStatsPolicy.Cpu a=new SystemStatsPolicy.Cpu(1000,600),b=new SystemStatsPolicy.Cpu(1400,900);assertEquals(25,SystemStatsPolicy.cpu(a,b));assertEquals(0,SystemStatsPolicy.cpu(b,new SystemStatsPolicy.Cpu(1800,1300)));assertEquals(100,SystemStatsPolicy.cpu(b,new SystemStatsPolicy.Cpu(1800,900)));}
    @Test public void noPercentageBeforeSecondSampleOrAfterReset(){SystemStatsPolicy.Cpu a=new SystemStatsPolicy.Cpu(100,50);assertEquals(-1,SystemStatsPolicy.cpu(null,a));assertEquals(-1,SystemStatsPolicy.cpu(a,a));assertEquals(-1,SystemStatsPolicy.cpu(a,new SystemStatsPolicy.Cpu(10,5)));assertEquals(-1,SystemStatsPolicy.cpu(a,new SystemStatsPolicy.Cpu(150,110)));assertEquals(-1,SystemStatsPolicy.cpu(a,new SystemStatsPolicy.Cpu(150,40)));}
    @Test public void missingDeniedMalformedAndOverflowStayUnknown(){for(String s:new String[]{null,"","head: Permission denied","cpu0 1 2 3 4","cpu a 1 2 3","cpu -1 2 3 4","cpu 9223372036854775807 1 1 1"})assertNull(SystemStatsPolicy.parse(s));}
    @Test public void availableMemoryAndSpaceProduceBoundedUsedRatios(){assertEquals(75,SystemStatsPolicy.used(25,100));assertEquals(0,SystemStatsPolicy.used(150,100));assertEquals(100,SystemStatsPolicy.used(0,100));assertEquals(-1,SystemStatsPolicy.used(-1,100));assertEquals(-1,SystemStatsPolicy.used(0,0));assertEquals(50,SystemStatsPolicy.used(Long.MAX_VALUE/2,Long.MAX_VALUE));}
    @Test public void pollingIsBounded(){assertEquals(5,SystemStatsPolicy.seconds(0));assertEquals(10,SystemStatsPolicy.seconds(10));assertEquals(60,SystemStatsPolicy.seconds(999));}
}
