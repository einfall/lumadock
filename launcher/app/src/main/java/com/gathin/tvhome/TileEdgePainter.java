package com.gathin.tvhome;
import android.graphics.*;
/** Cached vertical hairline and a small, finite set of outer glow rings. No blur or timers. */
final class TileEdgePainter {
    private final Paint line=new Paint(Paint.ANTI_ALIAS_FLAG),glow=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LinearGradient gradient=new LinearGradient(0,0,0,1,TileOutline.nuvioColors(),null,Shader.TileMode.CLAMP);
    private final Matrix matrix=new Matrix();private final RectF edge=new RectF();
    TileEdgePainter(){line.setStyle(Paint.Style.STROKE);line.setShader(gradient);glow.setStyle(Paint.Style.STROKE);}
    void hairline(Canvas c,RectF art,float radius,float density){
        // Compose's 1dp border is inside the artwork, not outside the configurable focus rim.
        float half=density/2f;edge.set(art);edge.inset(half,half);if(edge.width()<=0||edge.height()<=0)return;
        line.setStrokeWidth(density);matrix.setScale(1,art.height());matrix.postTranslate(art.left,art.top);gradient.setLocalMatrix(matrix);
        c.drawRoundRect(edge,Math.max(0,radius-half),Math.max(0,radius-half),line);
    }
    void glow(Canvas c,RectF edge,float radius,int color,int strength,float spread){
        int rings=Math.min(24,Math.max(2,(int)Math.ceil(spread*2)));float step=spread/rings;glow.setStrokeWidth(step+.5f);
        for(int i=0;i<rings;i++){float distance=step*(i+.5f),fade=1f-(i+.5f)/rings;int alpha=Math.round(255*strength/100f*fade*fade);
            glow.setColor((alpha<<24)|(color&0xFFFFFF));c.drawRoundRect(edge.left-distance,edge.top-distance,edge.right+distance,edge.bottom+distance,radius+distance,radius+distance,glow);}
    }
}
