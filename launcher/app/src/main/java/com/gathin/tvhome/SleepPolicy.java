package com.gathin.tvhome;
/** Elapsed-time deadlines are unaffected by clock or timezone changes. */
final class SleepPolicy {
    static final int[] MINUTES={0,1,5,15,30,45,60,90,120,180,240,300,360,480,600,720};
    static boolean allowed(int n){for(int m:MINUTES)if(m==n)return true;return false;}
    static long deadline(long now,int minutes){if(!allowed(minutes))throw new IllegalArgumentException("Unsupported duration");return minutes==0?0:now+minutes*60000L;}
    static int remainingMinutes(long now,long end){return end<=now?0:(int)Math.min(720,(end-now+59999)/60000);}
    static final long WARNING_MS=10000;
    static int countdown(long now,long end){return end<=now?0:(int)Math.min(10,(end-now+999)/1000);}
    static float progress(long now,long end){return Math.max(0f,Math.min(1f,1f-(end-now)/(float)WARNING_MS));}
    static boolean warningDue(long now,long end,long expected){return end>now&&end==expected&&end-now<=WARNING_MS;}
    static int color(long now,long end){return end<=0?0:end-now<=300000?0xFFFF6B6B:0xFFFFB347;}
}
