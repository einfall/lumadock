package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.recyclerview.widget.RecyclerView;

/** Widget-style placement of the complete dock band. Preview coordinates never enter preferences. */
final class DockMoveController implements PlacementTarget {
    static final String FREE="dock_position_free",PX="dock_position_dp_x",PY="dock_position_dp_y";
    private final ViewGroup root,wrapper;private final RecyclerView grid;private final SharedPreferences prefs;
    private final float density;private final boolean wrapperClip;
    private final java.util.function.BooleanSupplier singleRow;
    private final java.util.function.BiConsumer<Integer,Integer> shifted;
    private final RectF band=new RectF();private final Rect origin=new Rect();
    private int baseLeft,baseTop,previewX,previewY;private boolean preview,closed;
    private final View.OnLayoutChangeListener layout=(v,l,t,r,b,ol,ot,or,ob)->{baseLeft=l;baseTop=t;};
    private final ViewTreeObserver.OnPreDrawListener draw=()->{apply();return true;};
    DockMoveController(Activity a,ViewGroup root,RecyclerView grid,java.util.function.BooleanSupplier singleRow,
                       java.util.function.BiConsumer<Integer,Integer> shifted){
        this.root=root;this.grid=grid;this.singleRow=singleRow;this.shifted=shifted;
        wrapper=(ViewGroup)grid.getParent();wrapperClip=wrapper.getClipChildren();density=a.getResources().getDisplayMetrics().density;
        prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);baseLeft=grid.getLeft();baseTop=grid.getTop();
        grid.addOnLayoutChangeListener(layout);root.getViewTreeObserver().addOnPreDrawListener(draw);
    }
    private boolean enabled(){return singleRow.getAsBoolean()&&(preview||prefs.getBoolean(DockPlacementPopup.ENABLED,false)&&prefs.getBoolean(FREE,false));}
    private boolean band(){
        for(int i=0;i<grid.getItemDecorationCount();i++)if(grid.getItemDecorationAt(i) instanceof DockDecoration)
            return ((DockDecoration)grid.getItemDecorationAt(i)).bounds(grid,band);
        return false;
    }
    private void offset(int dx,int dy){
        if(dx==0&&dy==0)return;grid.offsetLeftAndRight(dx);grid.offsetTopAndBottom(dy);shifted.accept(dx,dy);
    }
    private void apply(){
        if(closed)return;boolean active=enabled();wrapper.setClipChildren(active?false:wrapperClip);
        ((DockRecyclerView)grid).clipDockViewport(singleRow.getAsBoolean()&&(active||prefs.getBoolean(DockPlacementPopup.ENABLED,false)));
        if(!active){offset(baseLeft-grid.getLeft(),baseTop-grid.getTop());return;}
        if(!band())return;Rect current=bounds("dock");int[] size=size("dock");
        int x=HeaderLayoutPolicy.clamp(preview?previewX:prefs.getInt(PX,dp(current.left)),width(),size[0]);
        int y=HeaderLayoutPolicy.clamp(preview?previewY:prefs.getInt(PY,dp(current.top)),height(),size[1]);
        offset(Math.round(x*density)-current.left,Math.round(y*density)-current.top);
    }
    public View view(String key){return grid;}
    public int[] size(String key){band();return new int[]{(int)Math.ceil(band.width()/density),(int)Math.ceil(band.height()/density)};}
    public int[] position(String key){Rect actual=bounds(key);return new int[]{dp(actual.left),dp(actual.top)};}
    public Rect bounds(String key){
        if(!band())return new Rect();origin.set(0,0,grid.getWidth(),grid.getHeight());root.offsetDescendantRectToMyCoords(grid,origin);
        return new Rect(Math.round(origin.left+band.left),Math.round(origin.top+band.top),Math.round(origin.left+band.right),Math.round(origin.top+band.bottom));
    }
    public int width(){return dp(root.getWidth());}public int height(){return dp(root.getHeight());}
    public void preview(String key,int x,int y){preview=true;previewX=x;previewY=y;apply();}
    public void endPreview(){preview=false;apply();}
    public void set(String key,int x,int y){
        int[] size=size(key);SharedPreferences.Editor edit=prefs.edit();
        // Enabling free placement must preserve the width that was actually previewed.
        if(!prefs.getBoolean(DockPlacementPopup.ENABLED,false)&&wrapper.getWidth()>0)
            edit.putInt(DockPlacementPopup.WIDTH,Math.round(100f*grid.getWidth()/wrapper.getWidth()));
        edit.putBoolean(DockPlacementPopup.ENABLED,true).putBoolean(FREE,true)
                .putInt(PX,HeaderLayoutPolicy.clamp(x,width(),size[0])).putInt(PY,HeaderLayoutPolicy.clamp(y,height(),size[1]))
                .putLong(Config.SETTINGS_REVISION_KEY,prefs.getLong(Config.SETTINGS_REVISION_KEY,0)+1).apply();
        preview=false;apply();
    }
    private int dp(float pixels){return Math.round(pixels/density);}
    void close(){closed=true;grid.removeOnLayoutChangeListener(layout);if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnPreDrawListener(draw);wrapper.setClipChildren(wrapperClip);}
}
