package com.gathin.tvhome;
/** Circular cells retain their aspect ratio in both orientations, including tiny widget sizes. */
final class SystemRingPolicy {
    static String axis(String value){return "horizontal".equals(value)?"horizontal":"vertical";}
    static int gap(int value){return Math.max(0,Math.min(32,value));}
    static float sweep(int percent){return percent<0?0:Math.min(100,percent)*3.6f;}
    static int naturalHeight(int width,int mask,String axis,int gap){int n=SystemWidgetPolicy.rows(mask);float safeGap=Math.min(gap(gap),Math.max(1,width)/(float)(3*n));float side="horizontal".equals(axis)?Math.max(1,(width-safeGap*(n-1))/n):Math.max(1,width);side=Math.min(side,n>1?100:200);return Math.max(32,Math.round("horizontal".equals(axis)?side:n*side+gap(gap)*(n-1)));}
    /** Each cell is center X, center Y and diameter in the caller's units. */
    static float[][] cells(float width,float height,int mask,String axis,int requestedGap){
        width=Math.max(1,width);height=Math.max(1,height);int n=SystemWidgetPolicy.rows(mask);boolean horizontal="horizontal".equals(axis);
        float major=horizontal?width:height,cross=horizontal?height:width,gap=Math.min(gap(requestedGap),major/(3*n));float side=Math.max(.1f,Math.min(cross,(major-gap*(n-1))/n));float start=(major-side*n-gap*(n-1))/2;
        float[][] result=new float[n][3];for(int i=0;i<n;i++){float along=start+side/2+i*(side+gap);result[i]=new float[]{horizontal?along:cross/2,horizontal?cross/2:along,side};}return result;
    }
}
