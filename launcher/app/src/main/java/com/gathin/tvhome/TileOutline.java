package com.gathin.tvhome;
/** Physical geometry and Nuvio CardDepth visual values; independent of the moving focus frame. */
final class TileOutline {
    private TileOutline(){}
    // Nuvio's default white 1dp vertical edge: 28%, 28% * .33, 0%.
    static int[] nuvioColors(){return new int[]{0x47FFFFFF,0x18FFFFFF,0x00FFFFFF};}
    static float outset(int gap,float width,boolean contrast,int contrastWidth){
        return Math.max(0,gap)+Math.max(0,width)/2f+(contrast?Math.max(0,contrastWidth):0);
    }
    static float focusLift(boolean dock,boolean animations,boolean advanced,int duration,float density){
        return !dock&&animations&&advanced&&duration>0?-4f*density:0f;
    }
}
