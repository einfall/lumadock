package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Small native weather artwork; no image downloads or animation. */
final class WeatherIcon extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private String code="";
    WeatherIcon(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    void setCode(String value){code=value==null?"":value;invalidate();}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/64f,getHeight()/64f);
        String type=code.length()>=2?code.substring(0,2):"";
        if(type.isEmpty()){paint.setColor(0xFF9BA9BD);for(int x=20;x<=44;x+=12)canvas.drawCircle(x,32,3,paint);canvas.restore();return;}
        boolean night=code.endsWith("n");
        if(type.equals("01")||type.equals("02")) {
            paint.setColor(night?0xFFBCDFFF:0xFFFFD56E);
            if(night){Path moon=new Path();moon.moveTo(39,9);moon.cubicTo(8,8,8,49,39,47);moon.cubicTo(22,38,22,19,39,9);canvas.drawPath(moon,paint);}
            else {
                float cx=type.equals("02")?25:32,cy=type.equals("02")?24:32;
                paint.setStrokeWidth(3);paint.setStrokeCap(Paint.Cap.ROUND);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(cx+(float)Math.cos(a)*19,cy+(float)Math.sin(a)*19,cx+(float)Math.cos(a)*24,cy+(float)Math.sin(a)*24,paint);}
                canvas.drawCircle(cx,cy,13,paint);
            }
        }
        if(!type.equals("01")&&!type.equals("50")) {
            paint.setColor(0xFFDCE7F3);canvas.drawCircle(27,31,12,paint);canvas.drawCircle(39,35,11,paint);
            canvas.drawCircle(17,37,9,paint);canvas.drawRoundRect(9,32,52,46,7,7,paint);
        }
        paint.setStrokeWidth(3);paint.setStrokeCap(Paint.Cap.ROUND);
        if(type.equals("09")||type.equals("10")){paint.setColor(0xFF77C9FF);for(int x=20;x<49;x+=12)canvas.drawLine(x,50,x-3,57,paint);}
        if(type.equals("11")){paint.setColor(0xFFFFD56E);Path bolt=new Path();bolt.moveTo(32,43);bolt.lineTo(23,54);bolt.lineTo(31,54);bolt.lineTo(28,63);bolt.lineTo(42,48);bolt.lineTo(34,48);bolt.close();canvas.drawPath(bolt,paint);}
        if(type.equals("13")){paint.setColor(0xFFBBE5FF);for(int x=18;x<50;x+=14){canvas.drawLine(x-3,53,x+3,57,paint);canvas.drawLine(x+3,53,x-3,57,paint);}}
        if(type.equals("50")){paint.setColor(0xFFB9CBDD);canvas.drawLine(12,24,49,24,paint);canvas.drawLine(18,34,54,34,paint);canvas.drawLine(10,44,44,44,paint);}
        canvas.restore();
    }
}
