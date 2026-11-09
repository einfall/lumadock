package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseArray;

/** Draws outside each unselected card, so the 1-5px rim gap is not clipped by the card outline. */
final class TileEffectsDecoration extends RecyclerView.ItemDecoration {
    private final RecyclerView grid;
    private final AppAdapter adapter;
    private final TileEffects effects,focusedEffects;
    private final TileEdgePainter edgePainter=new TileEdgePainter();
    private final float density;
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),spec=new Paint(Paint.ANTI_ALIAS_FLAG),rim=new Paint(Paint.ANTI_ALIAS_FLAG),shine=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint contrast=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds=new Rect();
    private final RectF art=new RectF(),edge=new RectF();
    private final SparseArray<LinearGradient> edgeShaders=new SparseArray<>();
    private final Matrix matrix=new Matrix();
    private final LinearGradient shineShader,fillShader,specShader;
    private ValueAnimator animation;
    private boolean disposed;
    private float progress=-1;
    private int previousFocus=RecyclerView.NO_POSITION,shineTile=RecyclerView.NO_POSITION;
    TileEffectsDecoration(RecyclerView grid,AppAdapter adapter){
        this.grid=grid;this.adapter=adapter;effects=FocusStyle.profile(grid.getContext(),false);focusedEffects=FocusStyle.profile(grid.getContext(),true);density=grid.getResources().getDisplayMetrics().density;
        contrast.setStyle(Paint.Style.STROKE);rim.setStyle(Paint.Style.STROKE);rim.setStrokeWidth(effects.thin?density:1.5f*density);
        shineShader=new LinearGradient(-.3f,-.2f,.3f,.2f,new int[]{0x00FFFFFF,0x44FFFFFF,0x00FFFFFF},new float[]{0,.5f,1},Shader.TileMode.CLAMP);shine.setShader(shineShader);
        fillShader=new LinearGradient(0,0,0,1,0x3DFFFFFF,0x12FFFFFF,Shader.TileMode.CLAMP);fill.setShader(fillShader);
        specShader=new LinearGradient(0,0,0,.45f,0x66FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP);spec.setShader(specShader);
        if(effects.shine)grid.post(()->{if(!disposed&&grid.isAttachedToWindow())startShine(RecyclerView.NO_POSITION);});
    }
    void focusChanged(int position){if(position==previousFocus)return;int old=previousFocus;previousFocus=position;startShine(old);grid.invalidate();}
    private void startShine(int position){if(disposed)return;stop();shineTile=position;animation=ValueAnimator.ofFloat(-.7f,1.7f);animation.setDuration(effects.shine?1100:420);animation.addUpdateListener(a->{progress=(float)a.getAnimatedValue();grid.invalidate();});animation.start();}
    void stop(){if(animation!=null){animation.cancel();animation=null;}progress=-1;grid.invalidate();}
    void dispose(){disposed=true;stop();}
    @Override public void onDrawOver(@NonNull Canvas canvas,@NonNull RecyclerView parent,@NonNull RecyclerView.State state){
        if(!effects.line()&&!effects.glass&&!effects.shine&&!effects.nuvio&&!effects.glow&&!focusedEffects.nuvio&&!focusedEffects.glow)return;
        for(int i=0;i<parent.getChildCount();i++){
            View child=parent.getChildAt(i);int pos=parent.getChildAdapterPosition(child);
            if(!adapter.effectTileAt(pos))continue;View card=child.findViewById(R.id.card);
            if(card==null||card.getVisibility()!=View.VISIBLE)continue;boolean focused=card.isFocused()||pos==previousFocus;TileEffects current=focused?focusedEffects:effects;
            AppAdapter.artBounds(card, bounds);
            parent.offsetDescendantRectToMyCoords(card, bounds);
            art.set(bounds);
            art.offset(child.getTranslationX()+card.getTranslationX(),child.getTranslationY()+card.getTranslationY());
            float radius = AppAdapter.cornerRadius(bounds.width(), density);
            // The view's transformation is handled here so the edge follows its focus-out animation.
            float sx=card.getScaleX(),sy=card.getScaleY();
            canvas.save();canvas.scale(sx,sy,art.centerX(),art.centerY());
            if(!focused&&effects.glass){matrix.setScale(art.width(),art.height());matrix.postTranslate(art.left,art.top);fillShader.setLocalMatrix(matrix);specShader.setLocalMatrix(matrix);canvas.drawRoundRect(art,radius,radius,fill);canvas.save();canvas.clipRect(art.left,art.top,art.right,art.top+art.height()*.45f);canvas.drawRoundRect(art,radius,radius,spec);canvas.restore();}
            if(!focused&&effects.shine&&progress>=-.6f&&progress<=1.6f&&(shineTile<0||shineTile==pos)){
                matrix.setScale(art.width(),art.height());matrix.postTranslate(art.left+art.width()*progress,art.centerY());shineShader.setLocalMatrix(matrix);canvas.drawRoundRect(art,radius,radius,shine);
            }
            int tint="dominant".equals(current.color)?adapter.iconDominantAt(pos):FocusStyle.fixed(current.color);if(tint==0)tint=0xFFFFFFFF;
            // A focused floating frame draws its own glow; static/idle edges stay attached to the actual card.
            if(current.glow&&(!focused||!adapter.focusOverlay())){
                float stroke=current.width>0?current.width:(current.thin?density:1.5f*density);
                float gap=TileOutline.outset(current.gap,stroke,current.contrast,current.contrastWidth)/sx;
                edge.set(art);edge.inset(-gap,-gap);edgePainter.glow(canvas,edge,radius+gap,tint,current.glowStrength,current.glowSpread/sx);
            }
            if(current.nuvio)edgePainter.hairline(canvas,art,radius,density);
            if(!focused&&effects.line()){
                rim.setStrokeWidth((effects.thin?density:1.5f*density)/sx);
                int color="dominant".equals(effects.color)?adapter.iconDominantAt(pos):FocusStyle.fixed(effects.color);if(color==0)color=0xFFFFFFFF;
                int rgb=color&0xFFFFFF;
                if(effects.thin){LinearGradient gradient=edgeShaders.get(color);if(gradient==null){gradient=new LinearGradient(0,0,1,1,new int[]{0xC0000000|rgb,0x50000000|rgb,0x1C000000|rgb},null,Shader.TileMode.CLAMP);edgeShaders.put(color,gradient);}matrix.setScale(art.width(),art.height());matrix.postTranslate(art.left,art.top);gradient.setLocalMatrix(matrix);rim.setShader(gradient);rim.setColor(Color.WHITE);}
                else {rim.setShader(null);rim.setColor(0xCC000000|rgb);}
                // Divide by the transient scale so the clear gap remains physical pixels.
                float gap=TileOutline.outset(effects.gap,effects.thin?density:1.5f*density,effects.contrast,effects.contrastWidth)/sx;
                edge.set(art);edge.inset(-gap,-gap);
                if(effects.contrast){contrast.setColor(0xD0000000|(ContrastColor.against(color)&0xFFFFFF));contrast.setStrokeWidth(rim.getStrokeWidth()+2*effects.contrastWidth/sx);canvas.drawRoundRect(edge,radius+gap,radius+gap,contrast);}
                canvas.drawRoundRect(edge,radius+gap,radius+gap,rim);
            }
            canvas.restore();
        }
    }
}
