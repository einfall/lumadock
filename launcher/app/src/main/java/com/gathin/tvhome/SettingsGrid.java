package com.gathin.tvhome;
import android.app.Activity;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Keeps existing settings child references stable while laying navigation/choice cards in a real grid. */
final class SettingsGrid extends LinearLayout {
 private final int gap;private final Set<View> spans=Collections.newSetFromMap(new WeakHashMap<>());private final Map<View,Rect> positions=new IdentityHashMap<>();
 SettingsGrid(Activity a){super(a);setOrientation(VERTICAL);gap=SettingsSheet.dp(a,8);setClipToPadding(false);}
 void full(View child){spans.add(child);}
 @Override protected void onMeasure(int ws,int hs){int width=MeasureSpec.getSize(ws),available=Math.max(1,width-getPaddingLeft()-getPaddingRight()),columns=available>=SettingsSheet.dp((Activity)getContext(),680)?3:2;int cell=Math.max(1,(available-(columns-1)*gap)/columns),y=getPaddingTop(),column=0,row=0;positions.clear();
  for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child.getVisibility()==GONE)continue;boolean full=spans.contains(child)||child instanceof GridSlider;int w=full?available:cell;
   if(full&&column!=0){y+=row+gap;column=0;row=0;}
   child.measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));int h=child.getMeasuredHeight();int x=getPaddingLeft()+(full?0:column*(cell+gap));positions.put(child,new Rect(x,y,x+w,y+h));
   if(full){y+=h+gap;}else{row=Math.max(row,h);column++;if(column==columns){y+=row+gap;row=0;column=0;}}
  }if(column!=0)y+=row+gap;setMeasuredDimension(width,resolveSize(y+getPaddingBottom(),hs));}
 @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(Map.Entry<View,Rect> entry:positions.entrySet()){Rect p=entry.getValue();entry.getKey().layout(p.left,p.top,p.right,p.bottom);}}
}
