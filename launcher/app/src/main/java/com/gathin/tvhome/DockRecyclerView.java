package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import androidx.recyclerview.widget.RecyclerView;

/** Keep adjacent pages inside a narrowed dock, while allowing vertical focus zoom and its glass band. */
public final class DockRecyclerView extends RecyclerView {
    private boolean dockViewport;
    public DockRecyclerView(Context context,AttributeSet attributes){super(context,attributes);}
    void clipDockViewport(boolean enabled){if(dockViewport!=enabled){dockViewport=enabled;invalidate();}}
    @Override protected void dispatchDraw(Canvas canvas){
        int save=canvas.save();
        if(dockViewport)canvas.clipRect(getPaddingLeft(),0,getWidth()-getPaddingRight(),getHeight());
        super.dispatchDraw(canvas);canvas.restoreToCount(save);
    }
}
