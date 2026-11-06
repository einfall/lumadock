package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import androidx.recyclerview.widget.RecyclerView;

/** Keep adjacent pages inside a narrowed dock, while allowing vertical focus zoom and its glass band. */
public final class DockRecyclerView extends RecyclerView {
    private boolean dockViewport;
    private final Rect viewportClip=new Rect();
    public DockRecyclerView(Context context,AttributeSet attributes){super(context,attributes);}
    void clipDockViewport(boolean enabled){if(dockViewport!=enabled){dockViewport=enabled;invalidate();}}
    @Override public void draw(Canvas canvas){int save=canvas.save();if(dockViewport){canvas.getClipBounds(viewportClip);canvas.clipRect(0,viewportClip.top,getWidth(),viewportClip.bottom);}super.draw(canvas);canvas.restoreToCount(save);}
    @Override protected void dispatchDraw(Canvas canvas){
        int save=canvas.save();
        if(dockViewport){canvas.getClipBounds(viewportClip);canvas.clipRect(getPaddingLeft(),viewportClip.top,getWidth()-getPaddingRight(),viewportClip.bottom);}
        super.dispatchDraw(canvas);canvas.restoreToCount(save);
    }
}
