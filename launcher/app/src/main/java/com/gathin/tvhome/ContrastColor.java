package com.gathin.tvhome;
/** Neutral contrast against a colored rim, without changing the chosen accent. */
final class ContrastColor {
    static final int DARK=0xFF11141A,LIGHT=0xFFF4F7FC;
    private static double channel(int n){double c=n/255.0;return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4);}
    static double luminance(int color){return .2126*channel((color>>16)&255)+.7152*channel((color>>8)&255)+.0722*channel(color&255);}
    static double ratio(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    static int against(int color){return ratio(color,DARK)>=ratio(color,LIGHT)?DARK:LIGHT;}
}
