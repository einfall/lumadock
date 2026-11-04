package com.gathin.tvhome;
import android.graphics.*;
/** Native #dock material from QuickAccess TV (MIT); source/license in assets/quickaccess-dock/. */
final class QuickAccessDock {
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),edge=new Paint(Paint.ANTI_ALIAS_FLAG),shadowPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path outline=new Path();
    private Bitmap mask;
    private int width=-1,height=-1,pad;
    private float cachedRadius=-1,cachedDensity=-1;
    void clear(){mask=null;width=height=-1;}
    void draw(Canvas canvas,RectF rect,DockStyle style,float density){
        float radius=Math.min(style.radius*density,Math.min(rect.width(),rect.height())/2);
        int w=(int)Math.ceil(rect.width()),h=(int)Math.ceil(rect.height());
        if(w!=width||h!=height||radius!=cachedRadius||density!=cachedDensity){
            mask=null;width=w;height=h;cachedRadius=radius;cachedDensity=density;
            float blur=25*density;pad=(int)Math.ceil(blur*2);
            int mw=w+pad*2,mh=h+pad*2;
            if(w>0&&h>0&&mw>0&&mh>0&&(long)mw*mh<=8_400_000L){
                try{
                    Bitmap made=Bitmap.createBitmap(mw,mh,Bitmap.Config.ALPHA_8);
                    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setMaskFilter(new BlurMaskFilter(blur,BlurMaskFilter.Blur.NORMAL));
                    new Canvas(made).drawRoundRect(new RectF(pad,pad,pad+rect.width(),pad+rect.height()),radius,radius,p);mask=made;
                }catch(RuntimeException|OutOfMemoryError ignored){mask=null;}
            }
        }
        if(mask!=null){
            outline.reset();outline.addRoundRect(rect,radius,radius,Path.Direction.CW);
            int saved=canvas.save();canvas.clipOutPath(outline);
            shadowPaint.setColor(0x2E000000);canvas.drawBitmap(mask,rect.left-pad,rect.top-pad+10*density,shadowPaint);canvas.restoreToCount(saved);
        }
        fill.setColor(DockStyle.alpha(style.top,100-style.transparency));canvas.drawRoundRect(rect,radius,radius,fill);
        if(style.rim){edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(density);edge.setColor(DockStyle.alpha(style.rimColor,style.rimOpacity));canvas.drawRoundRect(rect,radius,radius,edge);}
    }
}
