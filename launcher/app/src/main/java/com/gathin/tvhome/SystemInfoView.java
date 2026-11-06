package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import java.util.*;

/** Local resource cards and circular gauges. No blur, bitmap polling or network request. */
final class SystemInfoView extends View {
    private final float density,fontScale;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint text=new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular,bold;
    private final Drawable[] icons={new AppGlyph("cpu"),new AppGlyph("memory"),new AppGlyph("storage"),new AppGlyph("network")};
    private final int[] accents={0xFF2563EB,0xFF7C3AED,0xFF00816B,0xFF087EAD};
    private final int[] tints={0xFFE3EEFF,0xFFF0E5FF,0xFFDFF3EB,0xFFE1F3FA};
    private final RectF rect=new RectF();private String style="filled",title="Systemressourcen";private int mask=7;
    private boolean customTitle;private boolean glass=true;private int opacity=45;private final int[] lightAccents={0xFF91BEFF,0xFFC2A4FF,0xFF6DE0BE,0xFF7AD5FA};
    private String ringAxis="vertical";private int ringGap=10;
    private final int[] ringColors={0xFF4C9BFF,0xFFD12EEA,0xFFFFA12C,0xFF43D0DC};
    private final int[] ringLights={0xFF9CDFFF,0xFFF17BFF,0xFFFFDE6B,0xFFB7F7FF};
    private SystemStatsWidget.Snapshot snapshot;
    SystemInfoView(Context context){super(context);density=getResources().getDisplayMetrics().density;fontScale=getResources().getDisplayMetrics().scaledDensity/density;
        regular=Typeface.create(context.getString(R.string.font_regular),Typeface.NORMAL);bold=Typeface.create(regular,Typeface.BOLD);setFocusable(false);}
    void style(String value,int mask,String title,boolean glass,int opacity,String ringAxis,int ringGap,boolean customTitle){this.customTitle=customTitle;boolean ringLayout=!this.ringAxis.equals(ringAxis)||this.ringGap!=ringGap;this.ringAxis=SystemRingPolicy.axis(ringAxis);this.ringGap=SystemRingPolicy.gap(ringGap);if(ringLayout)requestLayout();this.glass=glass;this.opacity=SystemWidgetPolicy.opacity(opacity);String selected=SystemInfoStyle.id(value);boolean layout=!style.equals(selected)||this.mask!=mask;this.style=selected;this.mask=mask;this.title=title;if(layout)requestLayout();invalidate();}
    void data(SystemStatsWidget.Snapshot data){snapshot=data;StringBuilder description=new StringBuilder(title);for(int bit:new int[]{1,2,4,8})if((mask&bit)!=0){int kind=Integer.numberOfTrailingZeros(bit);description.append(" · ").append(label(kind)).append(' ').append(reading(kind)).append(' ').append(detail(kind));}setContentDescription(description);invalidate();}
    private int count(){return Math.max(1,Integer.bitCount(mask&15));}
    private float rowHeight(){return "resources".equals(style)?56:"pastel".equals(style)?59:"compact".equals(style)?52:50;}
    private float totalHeight(){return SystemWidgetPolicy.natural(style,mask);}
    private int ink(){return glass?0xFFF4F7FC:0xFF182131;}
    private int muted(){return glass?0xFFCDD7E5:0xFF46536A;}
    private int accent(int kind){return glass?lightAccents[kind]:accents[kind];}
    @Override protected void onMeasure(int width,int height){int w=MeasureSpec.getSize(width);int h=SystemInfoStyle.ring(style)?Math.round(SystemRingPolicy.naturalHeight(Math.round(w/density),mask,ringAxis,ringGap)*density):Math.round(totalHeight()*density*Math.min(1f,w/(240*density)));setMeasuredDimension(w,resolveSize(h,height));}
    private int percent(int kind){if(snapshot==null)return -1;return kind==0?snapshot.cpu:kind==1?SystemStatsPolicy.used(snapshot.ramAvailable,snapshot.ramTotal):kind==2?SystemStatsPolicy.used(snapshot.diskAvailable,snapshot.diskTotal):-1;}
    private String label(int kind){return kind==0?"CPU":kind==1?"RAM":kind==2?"HDD":"Netzwerk";}
    private String bytes(long value){return android.text.format.Formatter.formatShortFileSize(getContext(),Math.max(0,value));}
    private String reading(int kind){if(kind==3)return snapshot==null?"↓ — · ↑ —":"↓ "+NetworkTrafficPolicy.format(snapshot.rx)+" · ↑ "+NetworkTrafficPolicy.format(snapshot.tx);int value=percent(kind);return value<0?"—":value+" %";}
    private String detail(int kind){if(snapshot==null)return "Wird geladen …";if(kind==0)return "Auslastung";if(kind==1)return percent(kind)<0?"Nicht verfügbar":bytes(snapshot.ramTotal-Math.min(snapshot.ramAvailable,snapshot.ramTotal))+" / "+bytes(snapshot.ramTotal)+" belegt";if(kind==2)return percent(kind)<0?"Nicht verfügbar":bytes(snapshot.diskTotal-Math.min(snapshot.diskAvailable,snapshot.diskTotal))+" / "+bytes(snapshot.diskTotal)+" belegt";return "LAN + WLAN · Mittelwert";}
    private void box(Canvas canvas,float l,float t,float r,float b,float radius,int color){paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(color);rect.set(l,t,r,b);canvas.drawRoundRect(rect,radius,radius,paint);}
    private void pane(Canvas c,float l,float t,float r,float b,float radius,int lightColor){
        if(!glass){box(c,l,t,r,b,radius,lightColor);return;}int alpha=Math.round(opacity*2.55f);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setShader(new LinearGradient(l,t,l,Math.max(t+1,b),new int[]{Color.argb(Math.round(alpha*.65f),105,118,138),Color.argb(alpha,30,38,51),Color.argb(alpha,12,18,28)},new float[]{0,.38f,1},Shader.TileMode.CLAMP));rect.set(l+.4f,t+.4f,r-.4f,b-.4f);c.drawRoundRect(rect,radius,radius,paint);paint.setShader(null);paint.setColor(0x48FFFFFF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.65f);c.drawRoundRect(rect,radius,radius,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void words(Canvas canvas,String value,float x,float y,float size,int color,boolean strong,float max){text.setTypeface(strong?bold:regular);text.setTextSize(size*fontScale);text.setColor(color);text.setTextAlign(Paint.Align.LEFT);text.setShadowLayer(glass?.7f:0,0,glass?.5f:0,0x70000000);String shown=TextUtils.ellipsize(value,text,Math.max(0,max),TextUtils.TruncateAt.END).toString();canvas.drawText(shown,x,y,text);}
    private void right(Canvas canvas,String value,float right,float y,float size,int color,float max){text.setTypeface(regular);text.setTextSize(size*fontScale);float length=text.measureText(value);words(canvas,value,right-Math.min(length,max),y,size,color,false,max);}
    private void icon(Canvas canvas,int kind,float x,float y,boolean badge){if(badge)box(canvas,x-5,y-5,x+20,y+20,7,glass?(0x44000000|(accents[kind]&0xFFFFFF)):accents[kind]);Drawable glyph=icons[kind];glyph.setTint(badge?Color.WHITE:accent(kind));int save=canvas.save();canvas.translate(x,y);glyph.setBounds(0,0,15,15);glyph.draw(canvas);canvas.restoreToCount(save);}
    private void progress(Canvas canvas,float l,float y,float width,int value,int color){box(canvas,l,y,l+width,y+2.5f,1.25f,glass?0x36FFFFFF:0xFFD9E0EB);if(value>=0)box(canvas,l,y,l+width*Math.min(100,value)/100f,y+2.5f,1.25f,color);}
    private void center(Canvas c,String value,float x,float baseline,float size,int color,boolean strong,float max){text.setTypeface(strong?bold:regular);text.setTextSize(size*fontScale);float length=text.measureText(value);words(c,value,x-Math.min(length,max)/2,baseline,size,color,strong,max);}
    private void ringStroke(Canvas c,float x,float y,float radius,float stroke,int color,float sweep,boolean round){paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(round?Paint.Cap.ROUND:Paint.Cap.BUTT);paint.setStrokeWidth(stroke);paint.setColor(color);rect.set(x-radius,y-radius,x+radius,y+radius);c.drawArc(rect,-90,sweep,false,paint);}
    private void drawRings(Canvas raw){
        int saved=raw.save();raw.scale(density,density);float[][] cells=SystemRingPolicy.cells(getWidth()/density,getHeight()/density,mask,ringAxis,ringGap);int index=0;boolean neon="ring_neon".equals(style);
        for(int kind=0;kind<4;kind++)if((mask&(1<<kind))!=0){float[] cell=cells[index++];float x=cell[0],y=cell[1],side=cell[2],radius=side*(neon?.405f:.385f),stroke=side*(neon?.065f:.13f);int color=ringColors[kind],value=percent(kind),labelColor=glass?0xFFF2F5FA:0xFF192236;
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(glass?Color.argb(Math.round(opacity*2.55f),10,14,22):0xFFF4F6FA);raw.drawCircle(x,y,side*.47f,paint);
            ringStroke(raw,x,y,radius,neon?side*.014f:stroke,glass?(0x30000000|(color&0xFFFFFF)):0xFFD9DDE6,360,false);
            if(value>=0&&kind!=3){float sweep=SystemRingPolicy.sweep(value);if(neon){ringStroke(raw,x,y,radius,stroke*2.6f,0x13000000|(color&0xFFFFFF),sweep,true);ringStroke(raw,x,y,radius,stroke*1.65f,0x2A000000|(color&0xFFFFFF),sweep,true);}
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(neon?Paint.Cap.ROUND:Paint.Cap.BUTT);paint.setStrokeWidth(stroke);paint.setColor(Color.WHITE);paint.setShader(new SweepGradient(x,y,new int[]{color,ringLights[kind],color},null));rect.set(x-radius,y-radius,x+radius,y+radius);raw.drawArc(rect,-90,sweep,false,paint);paint.setShader(null);
            }
            String name=cells.length==1&&customTitle?title:label(kind);float max=side*.65f;
            if(kind==3){center(raw,"NETZ",x,y-side*.16f,side*.10f,labelColor,true,max);center(raw,"↓ "+(snapshot==null?"—":NetworkTrafficPolicy.format(snapshot.rx).replace(" MB/s","")),x,y+side*.01f,side*.115f,color,true,max);center(raw,"↑ "+(snapshot==null?"—":NetworkTrafficPolicy.format(snapshot.tx).replace(" MB/s","")),x,y+side*.16f,side*.115f,color,true,max);center(raw,"MB/s",x,y+side*.29f,side*.075f,labelColor,false,max);}
            else if(neon){center(raw,name,x,y-side*.15f,side*.105f,labelColor,true,max);center(raw,value<0?"—":Integer.toString(value),x,y+side*.115f,side*.24f,color,true,max);center(raw,"%",x,y+side*.285f,side*.085f,labelColor,false,max);}
            else{center(raw,name,x,y-side*.045f,side*.10f,color,true,max);center(raw,value<0?"—":value+"%",x,y+side*.14f,side*.17f,color,true,max);}
        }
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);raw.restoreToCount(saved);
    }
    @Override protected void onDraw(Canvas raw){
        super.onDraw(raw);if(SystemInfoStyle.ring(style)){drawRings(raw);return;}int saved=raw.save();float w=getWidth()/density,h=getHeight()/density;float fit=Math.min(1f,Math.min(w/240f,h/SystemWidgetPolicy.readable("system",style,mask)));fit=Math.max(.01f,fit);raw.scale(density*fit,density*fit);float width=w/fit,height=h/fit;
        if("resources".equals(style)){
            if(glass)pane(raw,0,0,width,height,17,0);else{paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setShader(new LinearGradient(0,0,width,height,new int[]{0xFFE7F0FF,0xFFF2EAF8,0xFFDFF1F3},null,Shader.TileMode.CLAMP));rect.set(0,0,width,height);raw.drawRoundRect(rect,17,17,paint);paint.setShader(null);}
            words(raw,title,14,24,13,ink(),true,width-28);
        }else if("compact".equals(style))pane(raw,0,0,width,height,17,0xFFFDFEFF);
        else if("filled".equals(style))pane(raw,0,0,width,height,19,0xFFE9EDF3);
        float start="resources".equals(style)?32:"pastel".equals(style)?0:8;float extra="resources".equals(style)?38:"pastel".equals(style)?0:16;float stride=(height-extra)/count();int row=0;
        for(int kind=0;kind<4;kind++)if((mask&(1<<kind))!=0){
            float rowTop=start+row*stride,top=rowTop+(stride-rowHeight())/2f;int value=percent(kind);int color=accent(kind);
            if("filled".equals(style)){
                pane(raw,8,rowTop,width-8,rowTop+stride-5,16,0xFFFCFDFF);if(value>0){int clip=raw.save();Path path=new Path();path.addRoundRect(new RectF(8,rowTop,width-8,rowTop+stride-5),16,16,Path.Direction.CW);raw.clipPath(path);box(raw,8,rowTop,8+(width-16)*value/100f,rowTop+stride-5,0,glass?(0x30000000|(color&0xFFFFFF)):tints[kind]);raw.restoreToCount(clip);}
                icon(raw,kind,20,top+15,false);words(raw,label(kind),44,top+20,13,ink(),true,width-135);words(raw,detail(kind),44,top+35,9.5f,muted(),false,width-135);right(raw,reading(kind),width-20,top+28,kind==3?11:17,ink(),kind==3?width-120:80);
            }else if("resources".equals(style)){
                icon(raw,kind,15,top+7,false);words(raw,label(kind),38,top+18,13,ink(),true,width-130);box(raw,width-54,top+1,width-13,top+23,11,glass?0x24FFFFFF:0xBBFFFFFF);right(raw,kind==3?"↕":reading(kind),width-20,top+16,10.5f,ink(),40);
                if(kind!=3)progress(raw,38,top+26,width-52,value,color);words(raw,kind==3?reading(kind):detail(kind),38,top+43,9.5f,muted(),false,width-51);
            }else if("compact".equals(style)){
                if(row>0){paint.setShader(null);paint.setColor(glass?0x28FFFFFF:0xFFE0E5EE);paint.setStrokeWidth(.6f);raw.drawLine(18,rowTop-2,width-18,rowTop-2,paint);}
                icon(raw,kind,21,top+11,true);words(raw,label(kind),51,top+15,13,ink(),true,width-190);words(raw,detail(kind),51,top+32,9.5f,muted(),false,width-190);
                if(kind!=3){progress(raw,width*.56f,top+17,width*.25f,value,color);right(raw,reading(kind),width-18,top+23,11,ink(),50);}else right(raw,reading(kind),width-18,top+24,10,ink(),width-155);
            }else{
                pane(raw,0,rowTop,width,rowTop+stride-8,14,kind==0?0xFFF0F5FF:kind==1?0xFFF7F1FF:kind==2?0xFFF0F9F5:0xFFF0F8FC);
                box(raw,12,top+8,38,top+34,9,glass?(0x28000000|(color&0xFFFFFF)):tints[kind]);icon(raw,kind,18,top+13,false);words(raw,label(kind),47,top+20,13,ink(),true,width-140);words(raw,detail(kind),47,top+35,9.5f,muted(),false,width-140);
                right(raw,reading(kind),width-16,top+28,kind==3?11:17,ink(),kind==3?width-140:80);if(kind!=3)progress(raw,12,rowTop+stride-15,width-24,value,color);
            }
            row++;
        }
        raw.restoreToCount(saved);
    }
}
