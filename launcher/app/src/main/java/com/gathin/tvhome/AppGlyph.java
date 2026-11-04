package com.gathin.tvhome;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
/** Small native symbols, independent of installed fonts. */
final class AppGlyph extends Drawable {
    private android.content.res.ColorStateList tint;
    @Override public void setTintList(android.content.res.ColorStateList value){tint=value;invalidateSelf();}
    @Override public boolean isStateful(){return tint!=null&&tint.isStateful();}
    @Override protected boolean onStateChange(int[] state){invalidateSelf();return true;}
    final String kind;private final Paint p=new Paint(3);private final Path path=new Path();
    AppGlyph(String k){kind=k;MenuGlyphs.watch(this);}
    @Override public void draw(Canvas c){
        int save=c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
        p.setColor(tint==null?0xFFFFFFFF:tint.getColorForState(getState(),tint.getDefaultColor()));p.setStrokeWidth(1.7f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);path.reset();
        if(MenuGlyphs.draw(c,p,kind)){c.restoreToCount(save);return;}
        switch(kind){
            case "network":c.drawLine(7,3,7,21,p);c.drawLine(3,7,7,3,p);c.drawLine(11,7,7,3,p);c.drawLine(17,3,17,21,p);c.drawLine(13,17,17,21,p);c.drawLine(21,17,17,21,p);break;
            case "text":c.drawLine(3,4,21,4,p);c.drawLine(12,4,12,21,p);c.drawLine(8,21,16,21,p);break;
            case "sparkle":path.moveTo(12,2);path.lineTo(15,9);path.lineTo(22,12);path.lineTo(15,15);path.lineTo(12,22);path.lineTo(9,15);path.lineTo(2,12);path.lineTo(9,9);path.close();c.drawPath(path,p);break;
            case "refresh":c.drawArc(new RectF(3,3,21,21),35,290,false,p);c.drawLine(21,3,21,9,p);c.drawLine(15,9,21,9,p);break;
            case "dock":c.drawRoundRect(1,5,23,20,4,4,p);for(int k=4;k<22;k+=6)c.drawRoundRect(k,9,k+4,16,1,1,p);break;
            case "cpu":c.drawRoundRect(6,6,18,18,2,2,p);c.drawRect(9,9,15,15,p);for(int k=8;k<=16;k+=4){c.drawLine(k,2,k,6,p);c.drawLine(k,18,k,22,p);c.drawLine(2,k,6,k,p);c.drawLine(18,k,22,k,p);}break;
            case "memory":c.drawRoundRect(2,5,22,18,2,2,p);for(int k=5;k<21;k+=5){c.drawRect(k,8,k+2,14,p);c.drawLine(k,18,k,21,p);}break;
            case "storage":c.drawRoundRect(3,2,21,22,3,3,p);c.drawCircle(12,11,5,p);c.drawLine(12,11,17,16,p);c.drawPoint(7,19,p);break;
            case "grid":for(int y=0;y<3;y++)for(int x=0;x<3;x++)c.drawRoundRect(3+x*7,3+y*7,7+x*7,7+y*7,1,1,p);break;
            case "tune":case "settings":for(int i=0;i<3;i++){float y=5+i*7;c.drawLine(2,y,22,y,p);c.drawCircle(i==1?15:8,y,2,p);}break;
            case "cloud":path.moveTo(6,19);path.cubicTo(-1,19,0,9,7,10);path.cubicTo(9,1,21,4,19,11);path.cubicTo(26,12,24,19,18,19);path.close();c.drawPath(path,p);break;
            case "moon":path.moveTo(17,2);path.cubicTo(3,0,-1,20,13,22);path.cubicTo(17,23,21,19,22,16);path.cubicTo(11,19,6,8,17,2);c.drawPath(path,p);break;
            case "timer":c.drawCircle(12,13,9,p);c.drawLine(9,1,15,1,p);c.drawLine(12,7,12,13,p);c.drawLine(12,13,16,15,p);break;
            case "volume":path.moveTo(3,9);path.lineTo(7,9);path.lineTo(12,4);path.lineTo(12,21);path.lineTo(7,16);path.lineTo(3,16);path.close();c.drawPath(path,p);c.drawArc(new RectF(8,6,20,19),-65,130,false,p);c.drawArc(new RectF(9,2,25,23),-65,130,false,p);break;
            case "eye":case "eye_off":
                path.moveTo(2,12);path.cubicTo(7,4,17,4,22,12);path.cubicTo(17,20,7,20,2,12);c.drawPath(path,p);c.drawCircle(12,12,3,p);if(kind.equals("eye_off"))c.drawLine(3,3,21,21,p);break;
            case "lock":case "unlock":
                c.drawRoundRect(5,10,19,21,2,2,p);c.drawArc(new RectF(8,2,16,15),180,kind.equals("lock")?180:100,false,p);c.drawLine(12,14,12,17,p);break;
            case "move":for(int i=0;i<3;i++)c.drawLine(5,7+5*i,19,7+5*i,p);break;
            case "music":c.drawLine(10,5,10,17,p);c.drawLine(10,5,19,3,p);c.drawLine(19,3,19,15,p);c.drawOval(5,16,10,20,p);c.drawOval(14,14,19,18,p);break;
            case "star":for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?10:4.5f;float x=12+(float)Math.cos(a)*r,y=12+(float)Math.sin(a)*r;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();c.drawPath(path,p);break;
            case "game":c.drawRoundRect(2,6,22,19,5,5,p);c.drawLine(6,12,12,12,p);c.drawLine(9,9,9,15,p);c.drawCircle(17,10,1,p);c.drawCircle(19,14,1,p);break;
            case "kids":c.drawCircle(12,12,9,p);c.drawPoint(9,10,p);c.drawPoint(15,10,p);c.drawArc(new RectF(7,9,17,17),25,130,false,p);break;
            case "photo":c.drawRoundRect(2,3,22,21,2,2,p);c.drawCircle(16,8,2,p);path.moveTo(3,19);path.lineTo(9,12);path.lineTo(14,17);path.lineTo(17,14);path.lineTo(21,19);c.drawPath(path,p);break;
            case "tv":c.drawRoundRect(2,3,22,18,2,2,p);c.drawLine(8,22,16,22,p);c.drawLine(12,18,12,22,p);break;
            default:c.drawRoundRect(2,4,22,20,3,3,p);path.moveTo(10,8);path.lineTo(17,12);path.lineTo(10,16);path.close();c.drawPath(path,p);
        }c.restoreToCount(save);
    }
    @Override public int getIntrinsicWidth(){return 48;}@Override public int getIntrinsicHeight(){return 48;}
    @Override public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
