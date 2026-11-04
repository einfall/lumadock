package com.gathin.tvhome;

import java.util.*;

/** Physical LAN/WLAN counters only: loopback, tunnels and bridge aliases cannot be counted twice. */
final class NetworkTrafficPolicy {
    static final class Counters {final long rx,tx;Counters(long rx,long tx){this.rx=rx;this.tx=tx;}}
    static final class Rate {final double rx,tx;Rate(double rx,double tx){this.rx=rx;this.tx=tx;}}
    static Map<String,Counters> parse(String text){
        Map<String,Counters> result=new LinkedHashMap<>();if(text==null)return result;
        for(String line:text.split("\\r?\\n")){
            int colon=line.indexOf(':');if(colon<0)continue;String name=line.substring(0,colon).trim();
            if(!name.matches("(?:eth[0-9]+|wlan[0-9]+|en[a-zA-Z0-9_-]+|wl[a-zA-Z0-9_-]+|usb[0-9]+|rndis[0-9]+)"))continue;
            String[] values=line.substring(colon+1).trim().split("\\s+");if(values.length<16)continue;
            try{long rx=Long.parseLong(values[0]),tx=Long.parseLong(values[8]);if(rx>=0&&tx>=0)result.put(name,new Counters(rx,tx));}catch(NumberFormatException ignored){}
        }
        return result;
    }
    static Rate rate(Map<String,Counters> before,Map<String,Counters> after,long elapsedMs){
        if(before==null||after==null||elapsedMs<=0)return new Rate(-1,-1);
        long rx=0,tx=0;boolean valid=false;
        try{for(Map.Entry<String,Counters> entry:after.entrySet()){
            Counters old=before.get(entry.getKey()),now=entry.getValue();if(old==null||now.rx<old.rx||now.tx<old.tx)continue;
            rx=Math.addExact(rx,now.rx-old.rx);tx=Math.addExact(tx,now.tx-old.tx);valid=true;
        }}catch(ArithmeticException overflow){return new Rate(-1,-1);}
        return valid?new Rate(rx*1000.0/elapsedMs,tx*1000.0/elapsedMs):new Rate(-1,-1);
    }
    static String format(double bytesPerSecond){return bytesPerSecond<0?"—":String.format(java.util.Locale.getDefault(),"%.2f MB/s",bytesPerSecond/1_000_000.0);}
}
