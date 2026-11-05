package com.gathin.tvhome;
/** Stable automatic sizing and readable minimums for internal widget instances. */
final class SystemWidgetPolicy {
    static int rows(int mask){return Math.max(1,Integer.bitCount(mask&15));}
    static int minimum(String source,String style,int mask){return 32;}
    static int readable(String source,String style,int mask){if(!"system".equals(source))return 62;if("original".equals(SystemInfoStyle.id(style)))return 82;return rows(mask)*("resources".equals(style)?50:44)+extra(style);}
    static int natural(String style,int mask){return rows(mask)*("resources".equals(style)?56:"pastel".equals(style)?59:"compact".equals(style)?52:50)+extra(style);}
    private static int extra(String style){return "resources".equals(style)?38:"pastel".equals(style)?0:16;}
    static int height(int value,int minimum,int screen){return Math.min(Math.max(1,screen),Math.max(minimum,value));}
    static int opacity(int value){return Math.max(0,Math.min(90,value));}
}
