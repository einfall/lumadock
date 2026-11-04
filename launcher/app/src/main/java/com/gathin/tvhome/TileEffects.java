package com.gathin.tvhome;
/** Independent appearance for one tile state. Gap is in physical screen pixels, never dp. */
final class TileEffects {
    final boolean frame,glass,shine,thin,contrast,nuvio,glow;
    final String color;
    final int gap,contrastWidth,width,glowStrength,glowSpread;
    TileEffects(boolean frame,boolean glass,boolean shine,boolean thin,String color,int gap){
        this(frame,glass,shine,thin,color,gap,false,1);
    }
    TileEffects(boolean frame,boolean glass,boolean shine,boolean thin,String color,int gap,boolean contrast,int contrastWidth){
        this(frame,glass,shine,thin,color,gap,contrast,contrastWidth,0);
    }
    TileEffects(boolean frame,boolean glass,boolean shine,boolean thin,String color,int gap,boolean contrast,int contrastWidth,int width){
        this(frame,glass,shine,thin,color,gap,contrast,contrastWidth,width,false,false,18,6);
    }
    TileEffects(boolean frame,boolean glass,boolean shine,boolean thin,String color,int gap,boolean contrast,int contrastWidth,int width,boolean nuvio,boolean glow,int glowStrength,int glowSpread){
        this.nuvio=nuvio;this.glow=glow;this.glowStrength=Math.max(1,Math.min(60,glowStrength));this.glowSpread=Math.max(1,Math.min(12,glowSpread));
        this.width=width<=0?0:Math.max(1,Math.min(8,width));
        this.contrast=contrast;this.contrastWidth=Math.max(1,Math.min(3,contrastWidth));
        this.frame=frame;this.glass=glass;this.shine=shine;this.thin=thin;this.color=validColor(color);this.gap=Math.max(0,Math.min(5,gap));
    }
    static String validColor(String s){for(String id:new String[]{"original","white","blue","cyan","purple","pink","orange","red","green","dominant"})if(id.equals(s))return id;return "original";}
    boolean line(){return frame||thin;}
}
