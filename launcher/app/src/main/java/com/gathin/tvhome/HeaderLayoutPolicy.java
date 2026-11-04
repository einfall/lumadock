package com.gathin.tvhome;
/** Screen-independent placement in dp. Each item stays wholly inside the current display. */
final class HeaderLayoutPolicy {
    static String mode(String s){return "row".equals(s)||"stack".equals(s)?s:"legacy";}
    static int clamp(int value,int screen,int size){return Math.max(0,Math.min(value,Math.max(0,screen-size)));}
    static int[][] preset(String mode,int[][] base,int[][] size){
        int[][] p=new int[base.length][2];for(int i=0;i<base.length;i++)p[i]=base[i].clone();mode=mode(mode);
        if("row".equals(mode)){p[2][0]=p[1][0]+size[1][0]+24;p[2][1]=p[1][1]+Math.max(0,(size[1][1]-size[2][1])/2);p[3][0]=p[2][0]+size[2][0]+12;p[3][1]=p[1][1]+Math.max(0,(size[1][1]-size[3][1])/2);}
        if("stack".equals(mode)){p[2][0]=p[1][0];p[2][1]=p[1][1]+size[1][1]+8;p[3][0]=p[1][0]+size[1][0]+24;p[3][1]=p[1][1]+Math.max(0,(size[1][1]-size[3][1])/2);for(int i=4;i<p.length;i++){p[i][0]=base[4][0]+(i-4)*(size[4][0]+5);p[i][1]=p[2][1]+size[2][1]+12;}}
        return p;
    }
}
