package com.gathin.tvhome;

final class NavigationSoundPolicy {
    static String mode(String value){return "luma".equals(value)||"off".equals(value)?value:"system";}
    static int volume(int value){return Math.max(0,Math.min(100,value));}
    static String output(String value){return "system".equals(value)?value:"media";}
    static boolean key(int action,int code,int repeat){
        if(action!=0)return false;
        boolean arrow=code>=19&&code<=22;
        return arrow||repeat==0&&(code==23||code==66||code==160||code==4||code==111||code==82);
    }
    static boolean due(long now,long previous){return previous<0||now-previous>=65;}
}
