package com.gathin.tvhome;
/** Alpha is independent from the metric color and the widget surface. */
final class RingAlphaPolicy {
    static int percent(int value){return Math.max(0,Math.min(100,value));}
    static int color(int rgb,int alpha,int percent){return (Math.round(alpha*percent(percent)/100f)<<24)|(rgb&0xFFFFFF);}
}
