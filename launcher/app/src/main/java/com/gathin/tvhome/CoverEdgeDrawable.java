package com.gathin.tvhome;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** A one-dp, fading cover edge, drawn without a bitmap or animation. */
final class CoverEdgeDrawable extends Drawable {
    private final Paint paint=new Paint(3);private final RectF box=new RectF();private final float density;private final int color;
    CoverEdgeDrawable(float d,int c){density=d;color=c&0xFFFFFF;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(d);}
    @Override protected void onBoundsChange(Rect b){paint.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{0xC0000000|color,0x50000000|color,0x1C000000|color},null,Shader.TileMode.CLAMP));}
    @Override public void draw(Canvas c){Rect b=getBounds();float d=density*.6f;box.set(b.left+d,b.top+d,b.right-d,b.bottom-d);float r=AppAdapter.cornerRadius(b.width(),density);c.drawRoundRect(box,r,r,paint);}
    @Override public void setAlpha(int n){paint.setAlpha(n);invalidateSelf();}@Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);invalidateSelf();}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
