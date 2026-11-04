package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * A translucent glass slab: soft sheen fill, bright top specular, crisp rim and a faint outer glow.
 * One instance floats over the grid and is moved with spring animations between tiles.
 */
public class GlassView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private float radius;
    /** Glow colour as 0xRRGGBB: white normally, a festival's accent (diya gold) while a festive skin is on. */
    private int glowRgb = 0xFFFFFF;
    /** glass_style rim or halo: no sheen and no fill, only the crisp rim (and, for halo, a lighter outer glow). */
    private boolean flat;
    private boolean shineEnabled,thinEdge,frameEnabled=true,glassEnabled=true;
    private int gapPx,contrastWidth=1,frameWidth;
    private boolean contrastEnabled,glowEnabled;
    private int smallGlowStrength=18,smallGlowSpread=6;private final TileEdgePainter edgePainter=new TileEdgePainter();
    private final Paint contrastRim=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF artwork=new RectF();
    private final Paint shinePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.graphics.Matrix shineMatrix=new android.graphics.Matrix();
    private LinearGradient shineShader;
    private android.animation.ValueAnimator shineAnimation;
    private float shineProgress=-1;
    private String focusKey="";
    private int chosenColor;
    void setEffects(TileEffects effects){
        glowEnabled=effects.glow;smallGlowStrength=effects.glowStrength;smallGlowSpread=effects.glowSpread;frameWidth=effects.width;shineEnabled=effects.shine;thinEdge=effects.thin;frameEnabled=effects.frame;glassEnabled=effects.glass;gapPx=effects.gap;contrastEnabled=effects.contrast;contrastWidth=effects.contrastWidth;
        focusKey="";stopShine();updateEdgeShader();invalidate();
    }
    float outset(){return TileOutline.outset(gapPx,rim.getStrokeWidth(),contrastEnabled,contrastWidth);}
    float padding(){return Math.max(6f*density,glowEnabled?smallGlowSpread+rim.getStrokeWidth()/2+(contrastEnabled?contrastWidth:0)+1:0);}
    void focusChanged(String key){
        if(key.equals(focusKey))return;focusKey=key;stopShine();
        if(!shineEnabled)return;
        shineAnimation=android.animation.ValueAnimator.ofFloat(-.7f,1.7f);shineAnimation.setDuration(1100);
        shineAnimation.setInterpolator(new android.view.animation.DecelerateInterpolator());
        shineAnimation.addUpdateListener(v->{shineProgress=(float)v.getAnimatedValue();invalidate();});shineAnimation.start();
    }
    void stopShine(){if(shineAnimation!=null){shineAnimation.cancel();shineAnimation=null;}shineProgress=-1;invalidate();}
    void setFocusColor(int color){if(color==chosenColor)return;chosenColor=color;if(color!=0){glowRgb=color&0xFFFFFF;rim.setColor(0xEE000000|glowRgb);}updateEdgeShader();invalidate();}
    private void updateEdgeShader(){
        rim.setStrokeWidth(frameWidth>0?frameWidth:(thinEdge?1f:flat?2f:1.5f)*density);
        contrastRim.setStyle(Paint.Style.STROKE);contrastRim.setStrokeWidth(rim.getStrokeWidth()+2*contrastWidth);
        contrastRim.setColor(0xD0000000|(ContrastColor.against(thinEdge?glowRgb:rim.getColor())&0xFFFFFF));
        rim.setShader(thinEdge?new LinearGradient(0,0,getWidth(),Math.max(1,getHeight()),new int[]{0xF0000000|glowRgb,0x80000000|glowRgb,0x35000000|glowRgb},null,Shader.TileMode.CLAMP):null);
    }
    @Override protected void onDetachedFromWindow(){stopShine();super.onDetachedFromWindow();}


    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 20f * density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f * density);
        rim.setColor(0xCCFFFFFF);
        // Cache the slab as a texture. The spring only changes translation, so the GPU can
        // move it without replaying clipPath and the glow on every frame. That path is
        // software-rendered on the MT5895 and is what makes the highlight crawl.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Tints the rim and the outer glow; 0 restores the plain white glass. */
    void setAccent(int argb) {
        if (argb == 0) {
            glowRgb = 0xFFFFFF;
            rim.setColor(0xCCFFFFFF);
        } else {
            glowRgb = argb & 0xFFFFFF;
            // The rim is the accent lifted towards white, so the edge still reads as glass, not paint.
            int r = (((glowRgb >> 16) & 0xFF) + 255 * 2) / 3, g = (((glowRgb >> 8) & 0xFF) + 255 * 2) / 3,
                    b = ((glowRgb & 0xFF) + 255 * 2) / 3;
            rim.setColor(0xE6000000 | (r << 16) | (g << 8) | b);
        }
        chosenColor=0;updateEdgeShader();invalidate();
    }

    /** One of Config's GLASS_* styles. */
    void setStyle(String style) {
        flat = Config.GLASS_RIM.equals(style) || Config.GLASS_HALO.equals(style);
        updateEdgeShader();
        invalidate();
    }

    void setRadius(float px) {
        radius = px;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fill.setShader(new LinearGradient(0, 0, 0, h, 0x3DFFFFFF, 0x12FFFFFF, Shader.TileMode.CLAMP));
        updateEdgeShader();
        shineShader=new LinearGradient(-w*.3f,-h*.2f,w*.3f,h*.2f,new int[]{0x00FFFFFF,0x44FFFFFF,0x00FFFFFF},new float[]{0,.5f,1},Shader.TileMode.CLAMP);
        shinePaint.setShader(shineShader);
        specular.setShader(new LinearGradient(0, 0, 0, h * 0.45f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = padding();   // room for the glow
        rect.set(inset, inset, getWidth() - inset, getHeight() - inset);

        if(glowEnabled)edgePainter.glow(canvas,rect,radius,glowRgb,smallGlowStrength,smallGlowSpread);
        // The rim is outside the artwork. Fill and gloss stay on the image, leaving a clear gap.
        float gap=outset();artwork.set(rect);artwork.inset(gap,gap);float artRadius=Math.max(0,radius-gap);
        if(glassEnabled){
            canvas.drawRoundRect(artwork,artRadius,artRadius,fill);
            canvas.save();canvas.clipRect(artwork.left,artwork.top,artwork.right,artwork.top+artwork.height()*.45f);
            canvas.drawRoundRect(artwork,artRadius,artRadius,specular);canvas.restore();
        }
        drawShine(canvas);
        if(frameEnabled||thinEdge){if(contrastEnabled)canvas.drawRoundRect(rect,radius,radius,contrastRim);canvas.drawRoundRect(rect,radius,radius,rim);}
    }
    private void drawShine(Canvas canvas){if(!shineEnabled||shineProgress<-.6f||shineProgress>1.6f||shineShader==null)return;shineMatrix.setTranslate(getWidth()*shineProgress,getHeight()*.5f);shineShader.setLocalMatrix(shineMatrix);canvas.drawRoundRect(artwork,Math.max(0,radius-outset()),Math.max(0,radius-outset()),shinePaint);}
}
