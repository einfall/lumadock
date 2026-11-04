package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class NetworkTrafficPolicyTest {
    private String row(String name,long rx,long tx){return name+": "+rx+" 0 0 0 0 0 0 0 "+tx+" 0 0 0 0 0 0 0\n";}
    @Test public void lanAndWifiAreAddedWithoutLoopbackOrVpn(){String a=row("eth0",100,200)+row("wlan0",300,400)+row("lo",1000,1000)+row("tun0",1000,1000);String b=row("eth0",1100,2200)+row("wlan0",3300,4400)+row("lo",99999,99999)+row("tun0",99999,99999);NetworkTrafficPolicy.Rate r=NetworkTrafficPolicy.rate(NetworkTrafficPolicy.parse(a),NetworkTrafficPolicy.parse(b),2000);assertEquals(2000,r.rx,0);assertEquals(3000,r.tx,0);}
    @Test public void newlyAppearingInterfacesDoNotAddHistoricalTraffic(){NetworkTrafficPolicy.Rate r=NetworkTrafficPolicy.rate(NetworkTrafficPolicy.parse(row("eth0",10,20)),NetworkTrafficPolicy.parse(row("eth0",10,20)+row("wlan0",999999,999999)),1000);assertEquals(0,r.rx,0);assertEquals(0,r.tx,0);}
    @Test public void resetMissingAndInvalidTimeHaveNoInventedSpeed(){assertEquals(-1,NetworkTrafficPolicy.rate(NetworkTrafficPolicy.parse(row("eth0",100,100)),NetworkTrafficPolicy.parse(row("eth0",1,1)),1000).rx,0);assertEquals(-1,NetworkTrafficPolicy.rate(null,NetworkTrafficPolicy.parse(row("eth0",1,1)),1000).rx,0);assertEquals(-1,NetworkTrafficPolicy.rate(NetworkTrafficPolicy.parse(""),NetworkTrafficPolicy.parse(""),0).rx,0);}
    @Test public void physicalAdaptersAreAcceptedAndAliasesAreExcluded(){assertEquals(3,NetworkTrafficPolicy.parse(row("eth1",1,1)+row("wlan1",1,1)+row("usb0",1,1)+row("br0",1,1)+row("eth0.1",1,1)+row("rmnet0",1,1)).size());}
    @Test public void malformedRowsAndNegativeCountersAreRejected(){assertTrue(NetworkTrafficPolicy.parse("eth0: 1 2\nwlan0: invalid\n"+row("eth1",-1,2)).isEmpty());}
}
