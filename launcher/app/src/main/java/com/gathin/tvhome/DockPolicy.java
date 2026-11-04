package com.gathin.tvhome;
/** Deterministic dock ordering and one-row navigation, independent of Android. */
final class DockPolicy {
    static String mode(String id){return "name".equals(id)||"installed".equals(id)||"frequent".equals(id)?id:"custom";}
    static int compare(String mode,String a,String keyA,long installedA,long usedA,String b,String keyB,long installedB,long usedB){
        int n=0;
        if("installed".equals(mode))n=Long.compare(installedB,installedA);
        else if("frequent".equals(mode))n=Long.compare(usedB,usedA);
        if(n==0)n=a.compareToIgnoreCase(b);
        return n!=0?n:keyA.compareTo(keyB);
    }
    static int horizontal(int position,int count,boolean forward,boolean wrap){
        if(count<=0)return -1;int next=position+(forward?1:-1);
        return wrap?Math.floorMod(next,count):Math.max(0,Math.min(count-1,next));
    }
    /** 0 is strictly dock-only at rest; 1 previews the next row, then each step exposes another row. */
    static int fullRows(int setting){return Math.max(0,setting-1);}
    static int top(int height,int dockHeight,int gridItemHeight,int setting,float density){
        if(setting==0)return Math.max(0,height-dockHeight+Math.round(16*density));
        return Math.max(0,height-dockHeight-gridItemHeight*fullRows(setting)-Math.round(30*density));
    }
}
