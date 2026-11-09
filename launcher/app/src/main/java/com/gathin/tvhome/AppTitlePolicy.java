package com.gathin.tvhome;
final class AppTitlePolicy {
    static int size(int value){return Math.max(18,Math.min(120,value));}
    static int labels(int value){return Math.max(60,Math.min(160,value));}
    static String mode(String value){return "focused".equals(value)?"focused":"hero";}
    static String alignment(String value, boolean hasSavedPosition) {
        if ("left".equals(value) || "center".equals(value) || "right".equals(value) || "manual".equals(value)) {
            return value;
        }
        return hasSavedPosition ? "manual" : "left";
    }

    static int alignedLeft(String alignment, int dockLeft, int dockRight, int textWidth, int screenWidth) {
        int free = Math.max(0, dockRight - dockLeft - textWidth);
        int offset = "right".equals(alignment) ? free : "center".equals(alignment) ? free / 2 : 0;
        return HeaderLayoutPolicy.clamp(dockLeft + offset, screenWidth, textWidth);
    }

    static String transition(String value) {
        return "fade".equals(value) || "none".equals(value) ? value : "slide";
    }

    /** Reserve the final zoom/frame footprint, so an instant frame never covers the caption. */
    static float captionTop(float top,float height,float currentScale,float targetScale,float edge,float gap){
        return top+height+(Math.max(1f,Math.max(currentScale,targetScale))-1f)*height/2f+Math.max(0,edge)+Math.max(0,gap);
    }
    static float captionLeft(float center,float captionWidth,float screenWidth,float margin){
        return Math.max(margin,Math.min(center-captionWidth/2f,Math.max(margin,screenWidth-margin-captionWidth)));
    }
    static float room(float tileHeight,float scale,float edge,float textHeight,float gap){
        return Math.max(0,scale-1f)*tileHeight/2f+Math.max(0,edge)+Math.max(0,textHeight)+Math.max(0,gap);
    }
}
