package com.gathin.tvhome;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Applies opacity to the whole material, including the outline and sheen, without changing its tokens. */
final class WidgetBackdrop extends Drawable {
    private final Drawable base;private int opacity;
    WidgetBackdrop(Drawable base,int percent){this.base=base;opacity=Math.max(0,Math.min(100,percent))*255/100;}
    @Override protected void onBoundsChange(Rect bounds){base.setBounds(bounds);}
    @Override public void draw(Canvas canvas){if(opacity<=0)return;if(opacity>=255){base.draw(canvas);return;}int save=canvas.saveLayerAlpha(getBounds().left,getBounds().top,getBounds().right,getBounds().bottom,opacity);base.draw(canvas);canvas.restoreToCount(save);}
    @Override public void setAlpha(int value){opacity=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){base.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
