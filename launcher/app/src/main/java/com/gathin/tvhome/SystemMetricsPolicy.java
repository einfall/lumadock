package com.gathin.tvhome;
/** One system-widget source; legacy metric instances retain their own selected data. */
final class SystemMetricsPolicy {
    static String source(String old){return "cpu".equals(old)||"ram".equals(old)||"storage".equals(old)||"network".equals(old)?"system":old;}
    static int normalize(int value){return value&15;}
    static int legacy(String source,boolean network){switch(source){case "cpu":return 1;case "ram":return 2;case "storage":return 4;case "network":return 8;default:return "system".equals(source)?7|(network?8:0):0;}}
    static int restore(String source,boolean hasMask,int mask,boolean network){return hasMask?normalize(mask):legacy(source,network);}
}
