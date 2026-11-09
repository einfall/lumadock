package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SystemMetricsPolicyTest {
 @Test public void activeThemedOverviewSurvivesLegacyDuplicates(){
     int overview=SystemMetricsPolicy.retentionPriority(true,true,true,7);
     assertTrue(overview>SystemMetricsPolicy.retentionPriority(false,false,true,7));
     assertTrue(overview>SystemMetricsPolicy.retentionPriority(true,true,true,4));
     assertTrue(SystemMetricsPolicy.retentionPriority(true,true,false,1)>SystemMetricsPolicy.retentionPriority(false,true,true,7));
     assertTrue(SystemMetricsPolicy.retentionPriority(true,false,true,7)>SystemMetricsPolicy.retentionPriority(true,true,true,0));
 }
 @Test public void legacySingleMetricsBecomeOneSourceWithoutChangingTheirData(){assertEquals("system",SystemMetricsPolicy.source("cpu"));assertEquals(1,SystemMetricsPolicy.restore("cpu",false,0,false));assertEquals(2,SystemMetricsPolicy.restore("ram",false,0,false));assertEquals(4,SystemMetricsPolicy.restore("storage",false,0,false));assertEquals(8,SystemMetricsPolicy.restore("network",false,0,false));}
 @Test public void legacyOverviewKeepsItsNetworkSelection(){assertEquals(7,SystemMetricsPolicy.restore("system",false,0,false));assertEquals(15,SystemMetricsPolicy.restore("system",false,0,true));}
 @Test public void explicitSelectionWinsOverTheOldNetworkFlag(){assertEquals(2,SystemMetricsPolicy.restore("system",true,2,true));assertEquals(8,SystemMetricsPolicy.restore("cpu",true,8,false));}
 @Test public void emptySelectionStaysEmptyAndDoesNotReviveDefaultCards(){assertEquals(0,SystemMetricsPolicy.restore("system",true,0,true));assertEquals(0,SystemMetricsPolicy.restore("system",true,0,false));}
 @Test public void futureBitsAreIgnoredAndMigrationIsIdempotent(){assertEquals(15,SystemMetricsPolicy.normalize(255));int mask=SystemMetricsPolicy.restore("ram",false,0,false);assertEquals(mask,SystemMetricsPolicy.restore(SystemMetricsPolicy.source("ram"),true,mask,true));}
 @Test public void nonMetricWidgetsKeepTheirSourceAndDoNotRequestSystemData(){assertEquals("quote",SystemMetricsPolicy.source("quote"));assertEquals("android",SystemMetricsPolicy.source("android"));assertEquals(0,SystemMetricsPolicy.legacy("quote",false));assertEquals(0,SystemMetricsPolicy.legacy("android",false));}
}
