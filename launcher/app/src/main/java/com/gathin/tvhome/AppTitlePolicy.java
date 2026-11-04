package com.gathin.tvhome;
final class AppTitlePolicy {
    static int size(int value){return Math.max(18,Math.min(120,value));}
    static int labels(int value){return Math.max(60,Math.min(160,value));}
}
