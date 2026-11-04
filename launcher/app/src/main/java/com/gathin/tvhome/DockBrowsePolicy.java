package com.gathin.tvhome;
/** Horizontal viewport policy. Slots are zero-based internally, one-based in the UI. */
final class DockBrowsePolicy {
    static String mode(String value){return "paging".equals(value)||"pivot".equals(value)?value:"edge";}
    static int pivot(int requested,int columns){return Math.max(1,Math.min(Math.max(1,columns),requested));}
    static int first(String mode,int position,int count,int columns,int pivot){
        if(count<=0)return 0;columns=Math.max(1,columns);position=Math.max(0,Math.min(count-1,position));
        if("paging".equals(mode))return position/columns*columns;
        if("pivot".equals(mode))return Math.max(0,position-(pivot(pivot,columns)-1));
        return 0;
    }
    /** Empty room at the end allows the last app to retain its pivot slot / last page alignment. */
    static int trailing(String mode,int count,int columns,int pivot){
        if(count<=0)return 0;columns=Math.max(1,columns);
        if("paging".equals(mode))return (columns-count%columns)%columns;
        if("pivot".equals(mode))return columns-pivot(pivot,columns);
        return 0;
    }
}
