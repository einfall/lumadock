package com.gathin.tvhome;
import java.util.Random;
/** Independent saver choices: a clock background never changes the Home wallpaper. */
final class SaverPolicy {
    static final String[] MODES={"legacy","black","photos","image","video","museum","wind","sky","space","mosaic","auto"};
    static String mode(String value){for(String mode:MODES)if(mode.equals(value))return mode;return "legacy";}
    static int minutes(int value){return Math.max(0,Math.min(120,value));}
    static int next(int count,int current,boolean random,Random rng){if(count<2)return 0;current=Math.max(0,Math.min(count-1,current));if(!random)return (Math.max(0,current)+1)%count;int next=rng.nextInt(count-1);return next>=current?next+1:next;}
    static boolean photoId(String value){return value!=null&&(value.matches("w[0-9]{2}\\.jpg")||value.matches("projectivy0[12]\\.webp"));}
}
