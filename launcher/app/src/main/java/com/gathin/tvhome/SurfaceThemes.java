package com.gathin.tvhome;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
/** Native gradient materials: no blur, textures, polling or perpetual animation. */
final class SurfaceThemes {
    static final String P="surface_theme_",COMMON=P+"common",BUTTON=P+"buttons",WIDGET=P+"widgets";
    static String resolve(SharedPreferences p,String key){String own=p.getString(key,"inherit");return "inherit".equals(own)?SurfaceStyle.id(p.getString(COMMON,"original")):SurfaceStyle.id(own);}
    static String widget(Context c){return resolve(c.getSharedPreferences(Config.SETTINGS_NAME,0),WIDGET);}
    static String instance(Context c,String id){return "inherit".equals(id)?widget(c):SurfaceStyle.id(id);}
    static ColorStateList ink(SurfaceStyle s){return new ColorStateList(new int[][]{{android.R.attr.state_focused},{}},new int[]{s.focusInk,s.ink});}
    static Drawable background(Context c,String id,boolean button){
        SurfaceStyle s=SurfaceStyle.of(id,button);float d=c.getResources().getDisplayMetrics().density;
        Drawable rest=new Material(s,d,false);if(!button)return rest;
        StateListDrawable result=new StateListDrawable();result.addState(new int[]{android.R.attr.state_focused},new Material(s,d,true));
        GradientDrawable active=new GradientDrawable();active.setColor(0x596EC1FF);active.setCornerRadius(s.radius*d);active.setStroke(Math.max(1,Math.round(1.5f*d)),0xFF6EC1FF);result.addState(new int[]{android.R.attr.state_activated},active);result.addState(new int[]{},rest);return result;
    }
    static void widget(View view,String id){view.setBackground(background(view.getContext(),id,false));}
    private static final class Material extends Drawable {
        final SurfaceStyle style;final float density;final boolean focus;final Paint fill=new Paint(3),edge=new Paint(3),glint=new Paint(3);final RectF rect=new RectF();
        Material(SurfaceStyle style,float density,boolean focus){this.style=style;this.density=density;this.focus=focus;edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(density*(style.radius==10?.5f:1f));}
        @Override protected void onBoundsChange(Rect b){rect.set(b);float inset=(style.radius==21?1f:.5f)*density/2;rect.inset(inset,inset);fill.setShader(focus?null:new LinearGradient(0,b.top,0,Math.max(b.top+1,b.bottom),style.top,style.bottom,Shader.TileMode.CLAMP));fill.setColor(focus?style.focus:Color.WHITE);edge.setColor(focus?0:style.rim);glint.setShader(new LinearGradient(0,b.top,0,Math.max(b.top+1,b.bottom),0x24FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));}
        @Override public void draw(Canvas c){float radius=Math.min(style.radius*density,rect.height()/2);c.drawRoundRect(rect,radius,radius,fill);if(!focus&&style.sheen)c.drawRoundRect(rect,radius,radius,glint);if(!focus)c.drawRoundRect(rect,radius,radius,edge);}
        @Override public void setAlpha(int alpha){fill.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){fill.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
