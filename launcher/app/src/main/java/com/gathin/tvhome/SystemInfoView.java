package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import java.util.*;

/** Four lightweight, locally drawn resource layouts. No blur, bitmap polling or network request. */
final class SystemInfoView extends View {
    private final float density,fontScale;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint text=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular,bold;
    private final Drawable[] icons={new AppGlyph("cpu"),new AppGlyph("memory"),new AppGlyph("storage"),new AppGlyph("network")};
    private final int[] accents={0xFF2563EB,0xFF7C3AED,0xFF00816B,0xFF087EAD};
    private final int[] tints={0xFFE3EEFF,0xFFF0E5FF,0xFFDFF3EB,0xFFE1F3FA};
    private final RectF rect=new RectF();private String style="filled",title="Systemressourcen";private int mask=7;
    private SystemStatsWidget.Snapshot snapshot;
    SystemInfoView(Context context){super(context);density=getResources().getDisplayMetrics().density;fontScale=getResources().getDisplayMetrics().scaledDensity/density;
        regular=Typeface.create(context.getString(R.string.font_regular),Typeface.NORMAL);bold=Typeface.create(regular,Typeface.BOLD);setFocusable(false);}
    void style(String value,int mask,String title){String selected=SystemInfoStyle.id(value);boolean layout=!style.equals(selected)||this.mask!=mask;this.style=selected;this.mask=mask;this.title=title;if(layout)requestLayout();invalidate();}
    void data(SystemStatsWidget.Snapshot data){snapshot=data;StringBuilder description=new StringBuilder(title);for(int bit:new int[]{1,2,4,8})if((mask&bit)!=0){int kind=Integer.numberOfTrailingZeros(bit);description.append(" · ").append(label(kind)).append(' ').append(reading(kind)).append(' ').append(detail(kind));}setContentDescription(description);invalidate();}
    private int count(){return Math.max(1,Integer.bitCount(mask&15));}
    private float rowHeight(){return "resources".equals(style)?56:"pastel".equals(style)?59:"compact".equals(style)?52:50;}
    private float totalHeight(){return count()*rowHeight()+("resources".equals(style)?38:"pastel".equals(style)?0:16);}
    @Override protected void onMeasure(int width,int height){setMeasuredDimension(MeasureSpec.getSize(width),resolveSize(Math.round(totalHeight()*density),height));}
    private int percent(int kind){if(snapshot==null)return -1;return kind==0?snapshot.cpu:kind==1?SystemStatsPolicy.used(snapshot.ramAvailable,snapshot.ramTotal):kind==2?SystemStatsPolicy.used(snapshot.diskAvailable,snapshot.diskTotal):-1;}
    private String label(int kind){return kind==0?"CPU":kind==1?"RAM":kind==2?"HDD":"Netzwerk";}
    private String bytes(long value){return android.text.format.Formatter.formatShortFileSize(getContext(),Math.max(0,value));}
    private String reading(int kind){if(kind==3)return snapshot==null?"↓ — · ↑ —":"↓ "+NetworkTrafficPolicy.format(snapshot.rx)+" · ↑ "+NetworkTrafficPolicy.format(snapshot.tx);int value=percent(kind);return value<0?"—":value+" %";}
    private String detail(int kind){if(snapshot==null)return "Wird geladen …";if(kind==0)return "Auslastung";if(kind==1)return percent(kind)<0?"Nicht verfügbar":bytes(snapshot.ramTotal-Math.min(snapshot.ramAvailable,snapshot.ramTotal))+" / "+bytes(snapshot.ramTotal)+" belegt";if(kind==2)return percent(kind)<0?"Nicht verfügbar":bytes(snapshot.diskTotal-Math.min(snapshot.diskAvailable,snapshot.diskTotal))+" / "+bytes(snapshot.diskTotal)+" belegt";return "LAN + WLAN · Mittelwert";}
    private void box(Canvas canvas,float l,float t,float r,float b,float radius,int color){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);rect.set(l,t,r,b);canvas.drawRoundRect(rect,radius,radius,paint);}
    private void words(Canvas canvas,String value,float x,float y,float size,int color,boolean strong,float max){text.setTypeface(strong?bold:regular);text.setTextSize(size*fontScale);text.setColor(color);text.setTextAlign(Paint.Align.LEFT);String shown=TextUtils.ellipsize(value,text,Math.max(0,max),TextUtils.TruncateAt.END).toString();canvas.drawText(shown,x,y,text);}
    private void right(Canvas canvas,String value,float right,float y,float size,int color,float max){text.setTypeface(regular);text.setTextSize(size*fontScale);float length=text.measureText(value);words(canvas,value,right-Math.min(length,max),y,size,color,false,max);}
    private void icon(Canvas canvas,int kind,float x,float y,boolean badge){if(badge)box(canvas,x-5,y-5,x+20,y+20,7,accents[kind]);Drawable glyph=icons[kind];glyph.setTint(badge?Color.WHITE:accents[kind]);int save=canvas.save();canvas.translate(x,y);glyph.setBounds(0,0,15,15);glyph.draw(canvas);canvas.restoreToCount(save);}
    private void progress(Canvas canvas,float l,float y,float width,int value,int color){box(canvas,l,y,l+width,y+2.5f,1.25f,0xFFD9E0EB);if(value>=0)box(canvas,l,y,l+width*Math.min(100,value)/100f,y+2.5f,1.25f,color);}
    @Override protected void onDraw(Canvas raw){
        super.onDraw(raw);int saved=raw.save();raw.scale(density,density);float width=getWidth()/density,height=getHeight()/density;
        if("resources".equals(style)){
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xFFE7F0FF,0xFFF2EAF8,0xFFDFF1F3},null,Shader.TileMode.CLAMP));rect.set(0,0,width,height);raw.drawRoundRect(rect,17,17,paint);paint.setShader(null);
            words(raw,title,14,24,13,0xFF182131,true,width-28);
        }else if("compact".equals(style))box(raw,0,0,width,height,17,0xFFFDFEFF);
        else if("filled".equals(style))box(raw,0,0,width,height,19,0xFFE9EDF3);
        int row=0;
        for(int kind=0;kind<4;kind++)if((mask&(1<<kind))!=0){
            float top=("resources".equals(style)?32:"pastel".equals(style)?0:8)+row*rowHeight();int value=percent(kind);int color=accents[kind];
            if("filled".equals(style)){
                box(raw,8,top,width-8,top+45,16,0xFFFCFDFF);if(value>0){int clip=raw.save();Path path=new Path();path.addRoundRect(new RectF(8,top,width-8,top+45),16,16,Path.Direction.CW);raw.clipPath(path);box(raw,8,top,8+(width-16)*value/100f,top+45,0,tints[kind]);raw.restoreToCount(clip);}
                icon(raw,kind,20,top+15,false);words(raw,label(kind),44,top+20,13,0xFF182131,true,width-135);words(raw,detail(kind),44,top+35,9.5f,0xFF46536A,false,width-135);right(raw,reading(kind),width-20,top+28,kind==3?11:17,0xFF182131,kind==3?width-120:80);
            }else if("resources".equals(style)){
                icon(raw,kind,15,top+7,false);words(raw,label(kind),38,top+18,13,0xFF182131,true,width-130);box(raw,width-54,top+1,width-13,top+23,11,0xBBFFFFFF);right(raw,kind==3?"↕":reading(kind),width-20,top+16,10.5f,0xFF182131,40);
                if(kind!=3)progress(raw,38,top+26,width-52,value,color);words(raw,kind==3?reading(kind):detail(kind),38,top+43,9.5f,0xFF46536A,false,width-51);
            }else if("compact".equals(style)){
                if(row>0){paint.setShader(null);paint.setColor(0xFFE0E5EE);paint.setStrokeWidth(.6f);raw.drawLine(18,top-2,width-18,top-2,paint);}
                icon(raw,kind,21,top+11,true);words(raw,label(kind),51,top+15,13,0xFF182131,true,width-190);words(raw,detail(kind),51,top+32,9.5f,0xFF46536A,false,width-190);
                if(kind!=3){progress(raw,width*.56f,top+17,width*.25f,value,color);right(raw,reading(kind),width-18,top+23,11,0xFF182131,50);}else right(raw,reading(kind),width-18,top+24,10,0xFF182131,width-155);
            }else{
                box(raw,0,top,width,top+51,14,kind==0?0xFFF0F5FF:kind==1?0xFFF7F1FF:kind==2?0xFFF0F9F5:0xFFF0F8FC);
                box(raw,12,top+8,38,top+34,9,tints[kind]);icon(raw,kind,18,top+13,false);words(raw,label(kind),47,top+20,13,0xFF182131,true,width-140);words(raw,detail(kind),47,top+35,9.5f,0xFF46536A,false,width-140);
                right(raw,reading(kind),width-16,top+28,kind==3?11:17,0xFF182131,kind==3?width-140:80);if(kind!=3)progress(raw,12,top+44,width-24,value,color);
            }
            row++;
        }
        raw.restoreToCount(saved);
    }
}
