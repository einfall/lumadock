package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.recyclerview.widget.RecyclerView;

/** Passive page control, anchored to the painted dock. No timer, focus stop or remote interception. */
final class DockPageDots extends View {
    private final RecyclerView grid;private final ViewGroup root;private final float density;
    private final java.util.function.Supplier<int[]> state;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect origin=new Rect();private final RectF band=new RectF();
    private int pages,page;private float cx,cy;private boolean visible;
    private final ViewTreeObserver.OnPreDrawListener draw=()->{place();return true;};
    DockPageDots(Activity a,ViewGroup root,RecyclerView grid,java.util.function.Supplier<int[]> state) {
        super(a);this.root=root;this.grid=grid;this.state=state;density=getResources().getDisplayMetrics().density;
        setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(this,new android.widget.FrameLayout.LayoutParams(-1,-1));root.getViewTreeObserver().addOnPreDrawListener(draw);
    }
    private void place() {
        int[] s=state.get();boolean shown=s[0]!=0;int count=DockPlacementPolicy.pages(s[1],s[2]);
        DockDecoration dock=null;
        if(shown&&count>0)for(int i=0;i<grid.getItemDecorationCount();i++)
            if(grid.getItemDecorationAt(i) instanceof DockDecoration){dock=(DockDecoration)grid.getItemDecorationAt(i);break;}
        shown=shown&&count>0&&dock!=null&&dock.bounds(band)&&grid.isShown();
        float x=0,y=0;
        if(shown){origin.set(0,0,grid.getWidth(),grid.getHeight());root.offsetDescendantRectToMyCoords(grid,origin);
            x=origin.left+band.centerX();y=origin.top+band.bottom-10*density;}
        int selected=DockPlacementPolicy.page(s[3],s[1],s[2]);
        if(shown!=visible||pages!=count||page!=selected||cx!=x||cy!=y){visible=shown;pages=count;page=selected;cx=x;cy=y;invalidate();}
    }
    @Override protected void onDraw(Canvas canvas) {
        place();
        if(!visible)return;
        int shown=Math.min(7,pages),first=Math.max(0,Math.min(pages-shown,page-shown/2));float gap=10*density;
        for(int i=0;i<shown;i++){int actual=first+i;boolean active=actual==page;
            float radius=(active?2.8f:(i==0&&first>0||i==shown-1&&first+shown<pages)?1.5f:2.5f)*density;
            paint.setColor(active?0xFFFFFFFF:0x66FFFFFF);paint.setShadowLayer(density,0,density,0x66000000);
            canvas.drawCircle(cx+(i-(shown-1)/2f)*gap,cy,radius,paint);}
    }
    void close(){if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnPreDrawListener(draw);root.removeView(this);}
}
