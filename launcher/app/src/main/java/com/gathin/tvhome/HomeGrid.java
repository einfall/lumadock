package com.gathin.tvhome;

/** Row geometry for a dock and app grid with independent column counts. */
final class HomeGrid {
    final int dock, columns;
    HomeGrid(int dock,int columns){this.dock=dock;this.columns=columns;}
    int rowStart(int position){return position<dock?0:dock+(position-dock)/columns*columns;}
    int rowWidth(int position){return position<dock?dock:columns;}
    int column(int position){return position-rowStart(position);}
    int toDock(int position){return position<dock?position:map(column(position),columns,dock);}
    int toGridColumn(int position){return position<dock?map(position,dock,columns):column(position);}
    int up(int position){
        if(position<dock)return -1;
        if(position<dock+columns)return toDock(position);
        return position-columns;
    }
    int down(int position,int count){
        int target=position<dock?dock+map(position,dock,columns):position+columns;
        if(rowStart(position)==rowStart(Math.max(0,count-1)))return position;
        return Math.min(target,count-1);
    }
    boolean edge(int position,int count,boolean towardStart){
        return towardStart?column(position)==0:column(position)==rowWidth(position)-1||position==count-1;
    }
    private static int map(int column,int from,int to){return Math.min(to-1,(int)((column+0.5)*to/from));}
}
