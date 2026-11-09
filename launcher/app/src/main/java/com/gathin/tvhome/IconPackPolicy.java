package com.gathin.tvhome;
/** Validates the public appfilter mapping syntax without executing any icon-pack code. */
final class IconPackPolicy {
    static final int MAX_ITEMS=40000,MAX_EVENTS=250000,MAX_BYTES=4*1024*1024;
    static String component(String raw){
        if(raw==null)return "";String value=raw.trim();
        if(value.startsWith("ComponentInfo{")&&value.endsWith("}"))value=value.substring(14,value.length()-1);
        if(value.length()>600)return "";int slash=value.indexOf('/');if(slash<1||slash!=value.lastIndexOf('/')||slash==value.length()-1)return "";
        String pkg=value.substring(0,slash),activity=value.substring(slash+1);
        if(!pkg.matches("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+")||!activity.matches("[A-Za-z0-9_.$]+"))return "";
        if(activity.startsWith("."))activity=pkg+activity;return pkg+"/"+activity;
    }
    static float fitScale(int width, int height, int slotWidth, int slotHeight) {
        return Math.min(slotWidth / (float) width, slotHeight / (float) height);
    }

    static boolean drawable(String value){return value!=null&&value.length()<=180&&value.matches("[a-z][a-z0-9_]*");}
}
