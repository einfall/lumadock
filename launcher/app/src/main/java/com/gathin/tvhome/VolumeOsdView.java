package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/** Small native canvas: redraws only during a level transition, with no blur or bitmap assets. */
final class VolumeOsdView extends View {
    final VolumeStyle style;
    private VolumeAppearance appearance;private final Typeface themedFace;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private final Path speaker=new Path();
    private final Typeface regular=Typeface.create("sans-serif",Typeface.NORMAL);
    private final Typeface light=Typeface.create("sans-serif-light",Typeface.NORMAL);
    private final float density,fontScale;
    private final String source,shortSource,mutedLabel;
    private ValueAnimator fill,pulse;
    private float level,pop;
    private boolean known,muted;
    private int target,messageId;
    private String number="…",message="…";

    VolumeOsdView(Context context,VolumeStyle style){
        this(context, style, context.getString(R.string.cec_volume_source), "HDMI");
    }

    VolumeOsdView(Context context,VolumeStyle style,String source,String shortSource){
        super(context);this.style=style;appearance=VolumeAppearance.load(context);themedFace=Typography.face(context,Typography.selected(context,Typography.APPS),R.string.font_light);
        density=getResources().getDisplayMetrics().density;
        fontScale=getResources().getDisplayMetrics().scaledDensity/density;
        this.source=source;
        this.shortSource=shortSource;
        mutedLabel=context.getString(R.string.volume_muted_label);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    void state(int volume,boolean mute){
        appearance=VolumeAppearance.load(getContext());invalidate();
        volume=Math.max(0,Math.min(100,volume));
        if(known&&target==volume&&muted==mute)return;
        known=true;target=volume;muted=mute;number=Integer.toString(volume);messageId=0;
        setContentDescription(source+": "+(mute?mutedLabel+" ":"")+number);
        if(fill!=null)fill.cancel();
        fill=ValueAnimator.ofFloat(level,volume);
        fill.setDuration(style==VolumeStyle.LG?380:style==VolumeStyle.SONY?240:180);
        fill.setInterpolator(style==VolumeStyle.LG?new OvershootInterpolator(1.1f):new DecelerateInterpolator());
        fill.addUpdateListener(a->{level=(float)a.getAnimatedValue();invalidate();});fill.start();
        if(pulse!=null)pulse.cancel();
        pulse=ValueAnimator.ofFloat(0,1);pulse.setDuration(style==VolumeStyle.LG?380:220);
        pulse.addUpdateListener(a->{pop=(float)Math.sin((float)a.getAnimatedValue()*Math.PI);invalidate();});pulse.start();
    }

    void message(int resource){
        appearance=VolumeAppearance.load(getContext());invalidate();
        if(!known&&messageId==resource)return;
        known=false;messageId=resource;message=getContext().getString(resource);level=0;pop=0;
        if(fill!=null)fill.cancel();if(pulse!=null)pulse.cancel();
        setContentDescription(source+": "+message);invalidate();
    }

    void stop(){if(fill!=null)fill.cancel();if(pulse!=null)pulse.cancel();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}

    @Override protected void onDraw(Canvas raw){
        super.onDraw(raw);int save=raw.save();raw.scale(density,density);
        switch(style){case SONY:sony(raw);break;case LG:lg(raw);break;case PHILIPS:philips(raw);break;
            case SAMSUNG:samsung(raw);break;default:glass(raw);}
        raw.restoreToCount(save);
    }

    private void glass(Canvas c){
        panel(c,300,68,22,0xEE171B23,0x66FFFFFF);
        speaker(c,19,18,23,ink(Color.WHITE));
        text(c,known?source:shortSource,56,30,14,mutedInk(0xFFCED5E2),Paint.Align.LEFT,known?155:65);
        text(c,known?(muted?mutedLabel+" · "+number:number):message,282,35,
                known?(muted?17:23):12,ink(Color.WHITE),Paint.Align.RIGHT,known?100:197);
        track(c,18,50,264,4,trackInk(0xFF424954),accent(0xFFE2F1FF));
    }
    private void sony(Canvas c){
        panel(c,300,64,7,0xF0131518,0x337C8188);
        speaker(c,14,18,24,ink(Color.WHITE));
        text(c,source,50,24,11,mutedInk(0xFFBFC3CB),Paint.Align.LEFT,170);
        text(c,known?(muted?mutedLabel+" · "+number:number):message,282,38,
                known?(muted?18:29):11,ink(Color.WHITE),Paint.Align.RIGHT,known?100:190);
        track(c,50,46,232,2,trackInk(0xFF4E5157),accent(Color.WHITE));
    }
    private void lg(Canvas c){
        panel(c,122,166,30,0xF014101A,0x554E3D60);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(7);paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(trackInk(0xFF423246));c.drawCircle(61,64,43,paint);
        paint.setColor(muted?accent(0xFFEE94BA):accent(0xFFFF4D9D));bounds.set(18,21,104,107);
        c.drawArc(bounds,-90,3.6f*clampedLevel(),false,paint);
        paint.setStyle(Paint.Style.FILL);
        int saved=c.save();float scale=1+0.075f*pop;c.scale(scale,scale,61,65);
        text(c,known?number:"…",61,77,35,ink(Color.WHITE),Paint.Align.CENTER,78);c.restoreToCount(saved);
        speaker(c,51,111,20,muted?accent(0xFFFF85B7):ink(0xFFECDDEE));
        text(c,known?(muted?mutedLabel:source):message,61,149,10,mutedInk(0xFFE6C5D9),Paint.Align.CENTER,104);
    }
    private void philips(Canvas c){
        panel(c,340,74,7,0xF00C1827,0x55567C9E);
        speaker(c,15,20,24,ink(0xFFD9EFFF));
        text(c,source,50,27,12,mutedInk(0xFFBEDAF2),Paint.Align.LEFT,190);
        text(c,known?(muted?mutedLabel+" · "+number:number):message,322,40,
                known?(muted?19:30):12,ink(0xFFE3F5FF),Paint.Align.RIGHT,known?110:210);
        float step=272f/28f,lit=clampedLevel()*28f/100f;
        for(int i=0;i<28;i++){
            float x=50+i*step;
            round(c,x,50,step-2,8,1,trackInk(0xFF293E52));
            float fraction=Math.max(0,Math.min(1,lit-i));
            if(fraction>0)round(c,x,50,step-2,8,1,((int)(255*fraction)<<24)|(accent(0xFF74CFFF)&0xFFFFFF));
        }
    }
    private void samsung(Canvas c){
        panel(c,80,254,39,0xF5F5F6F8,0xFFCFD4DB);
        text(c,"+",40,33,26,ink(0xFF202632),Paint.Align.CENTER,50);
        round(c,36,51,8,123,4,trackInk(0xFFCFD4DA));
        float h=123*clampedLevel()/100f;
        if(h>0)round(c,36,174-h,8,h,4,muted?accent(0xFF7D848F):accent(0xFF17212F));
        speaker(c,30,186,20,muted?accent(0xFFBC4163):ink(0xFF252E3B));
        text(c,known?number:message,40,232,known?28:9,ink(0xFF17212F),Paint.Align.CENTER,66);
        text(c,known&&muted?mutedLabel:"HDMI",40,246,8,mutedInk(0xFF616A76),Paint.Align.CENTER,62);
    }
    private float clampedLevel(){return Math.max(0,Math.min(100,level));}
    private int ink(int original){return appearance.original?original:appearance.ink;}
    private int mutedInk(int original){return appearance.original?original:appearance.muted;}
    private int accent(int original){return appearance.original?original:appearance.accent;}
    private int trackInk(int original){return appearance.original?original:appearance.track;}
    private void panel(Canvas c,int w,int h,int radius,int color,int border){
        if(!appearance.original){
            paint.setStyle(Paint.Style.FILL);bounds.set(.6f,.6f,w-.6f,h-.6f);paint.setColor(Color.WHITE);
            paint.setShader(new LinearGradient(0,0,w,h,appearance.top,appearance.bottom,Shader.TileMode.CLAMP));c.drawRoundRect(bounds,radius,radius,paint);paint.setShader(null);
            if(appearance.sheen){paint.setShader(new LinearGradient(0,0,0,h,new int[]{0x20FFFFFF,0x04FFFFFF,0x00FFFFFF},new float[]{0,.42f,1},Shader.TileMode.CLAMP));c.drawRoundRect(bounds,radius,radius,paint);paint.setShader(null);}
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.7f);paint.setColor(appearance.rim);c.drawRoundRect(bounds,radius,radius,paint);paint.setStyle(Paint.Style.FILL);return;
        }
        round(c,.6f,.6f,w-1.2f,h-1.2f,radius,color);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.7f);paint.setColor(border);
        bounds.set(.6f,.6f,w-.6f,h-.6f);c.drawRoundRect(bounds,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void track(Canvas c,float x,float y,float width,float height,int background,int color){
        round(c,x,y,width,height,height/2,background);
        float w=width*clampedLevel()/100f;if(w>0)round(c,x,y,w,height,height/2,color);
    }
    private void round(Canvas c,float x,float y,float w,float h,float r,int color){
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);bounds.set(x,y,x+w,y+h);c.drawRoundRect(bounds,r,r,paint);
    }
    private void text(Canvas c,String text,float x,float y,float size,int color,Paint.Align align,float maxWidth){
        paint.setStyle(Paint.Style.FILL);paint.setTypeface(appearance.original?(style==VolumeStyle.LG||style==VolumeStyle.SONY?light:regular):themedFace);
        paint.setTextSize(size*fontScale);paint.setTextAlign(align);paint.setColor(color);
        float measured=paint.measureText(text);if(measured>maxWidth)paint.setTextSize(paint.getTextSize()*maxWidth/measured);
        c.drawText(text,x,y,paint);
    }
    private void speaker(Canvas c,float x,float y,float size,int color){
        int save=c.save();c.translate(x,y);c.scale(size/24,size/24);
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);speaker.reset();
        speaker.moveTo(2,8);speaker.lineTo(7,8);speaker.lineTo(13,3);speaker.lineTo(13,21);
        speaker.lineTo(7,16);speaker.lineTo(2,16);speaker.close();c.drawPath(speaker,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);
        if(muted){c.drawLine(17,8,23,16,paint);c.drawLine(23,8,17,16,paint);}
        else {bounds.set(8,5,21,19);c.drawArc(bounds,-58,116,false,paint);bounds.set(8,1,27,23);c.drawArc(bounds,-52,104,false,paint);}
        paint.setStyle(Paint.Style.FILL);c.restoreToCount(save);
    }
}
