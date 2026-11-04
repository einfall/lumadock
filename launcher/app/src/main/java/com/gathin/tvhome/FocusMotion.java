package com.gathin.tvhome;
/** Focus motion policy. Every transition is finite and may be reversed from its current scale. */
final class FocusMotion {
    final boolean overlay,zoom,animateIn,animateOut,bounce;
    FocusMotion(boolean overlay,boolean zoom,boolean animateIn,boolean animateOut,boolean bounce){
        this.overlay=overlay;this.zoom=zoom;this.animateIn=animateIn;this.animateOut=animateOut;this.bounce=bounce;
    }
    float scale(boolean animations,boolean advanced){return animations&&zoom?(advanced?1.12f:1.10f):1f;}
    int duration(boolean focused,boolean reduced,boolean advanced){
        if(!(focused?animateIn:animateOut))return 0;
        if(reduced)return focused?Motion.REDUCED_MS:60;
        if(bounce&&zoom)return focused?380:280;
        return advanced?(focused?220:170):(focused?260:180);
    }
    boolean useBounce(boolean reduced){return zoom&&bounce&&!reduced;}
}
