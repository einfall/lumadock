package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.function.*;

/** One remote focus stop per setting; options stay visible and never open a picker. */
final class SettingsOptions extends LinearLayout {
    private final Activity activity;
    private final String title;
    private final String[] names;
    private final IntPredicate selected;
    private final IntConsumer change;
    private final boolean multiple;
    private final HorizontalScrollView scroll;
    private final TextView[] chips;
    private String fontSample;
    private IntFunction<android.graphics.Typeface> fontFaces;
    private int cursor;

    SettingsOptions(Activity a,String icon,String title,String[] names,IntPredicate selected,IntConsumer change,boolean multiple){
        super(a);activity=a;this.title=title;this.names=names;this.selected=selected;this.change=change;this.multiple=multiple;
        setId(View.generateViewId());setOrientation(VERTICAL);setFocusable(true);setFocusableInTouchMode(true);setClickable(true);
        setPadding(dp(10),dp(8),dp(10),dp(8));
        TextView heading=GlassDialog.text(a,title,13);AppGlyph glyph=new AppGlyph(icon);glyph.setBounds(0,0,dp(15),dp(15));glyph.setTint(0xFFE2E8F0);heading.setCompoundDrawables(glyph,null,null,null);heading.setCompoundDrawablePadding(dp(8));addView(heading);
        scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);scroll.setFocusable(false);scroll.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        LinearLayout strip=new LinearLayout(a);strip.setGravity(Gravity.CENTER_VERTICAL);scroll.addView(strip);
        LayoutParams sp=new LayoutParams(-1,dp(32));sp.topMargin=dp(6);addView(scroll,sp);chips=new TextView[names.length];
        for(int i=0;i<names.length;i++){final int index=i;TextView chip=GlassDialog.text(a,names[i],11);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(11),0,dp(11),0);chip.setMinimumWidth(dp(52));chip.setFocusable(false);chip.setClickable(true);chip.setOnClickListener(v->{requestFocus();cursor=index;choose(index);});LayoutParams cp=new LayoutParams(-2,dp(29));cp.rightMargin=dp(5);strip.addView(chip,cp);chips[i]=chip;}
        for(int i=0;i<names.length;i++)if(selected.test(i)){cursor=i;break;}
        setOnFocusChangeListener((v,on)->{refresh();reveal();});
        setOnClickListener(v->{if(names.length>0)choose(multiple?cursor:(cursor+1)%names.length);});
        setOnKeyListener((v,key,event)->{
            boolean arrow=key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;
            boolean ok=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER;
            if(!arrow&&!ok)return false;
            if(event.getAction()==KeyEvent.ACTION_DOWN){
                if(arrow&&names.length>0){int next=Math.max(0,Math.min(names.length-1,cursor+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1)));if(next!=cursor){cursor=next;if(!multiple)change.accept(cursor);refresh();reveal();}}
                else if(ok&&event.getRepeatCount()==0&&names.length>0)choose(multiple?cursor:(cursor+1)%names.length);
            }return true;
        });refresh();post(this::reveal);
    }
    private int dp(float n){return SettingsSheet.dp(activity,n);}
    SettingsOptions compact(){getChildAt(0).setVisibility(GONE);setPadding(dp(7),dp(5),dp(7),dp(5));LayoutParams lp=(LayoutParams)scroll.getLayoutParams();lp.topMargin=0;lp.height=dp(31);scroll.setLayoutParams(lp);return this;}
    /** Actual font samples above neutral, readable family names; navigation is unchanged. */
    SettingsOptions fontPreviews(String sample,IntFunction<android.graphics.Typeface> faces){
        fontSample=sample;fontFaces=faces;scroll.getLayoutParams().height=dp(68);
        for(TextView chip:chips){chip.getLayoutParams().height=dp(64);chip.setMinimumWidth(dp(112));chip.setMaxWidth(dp(156));chip.setMaxLines(2);chip.setEllipsize(android.text.TextUtils.TruncateAt.END);chip.setPadding(dp(10),dp(4),dp(10),dp(4));}
        refresh();requestLayout();return this;
    }
    private CharSequence label(int index,boolean on){
        if(fontFaces==null)return (multiple&&on?"✓ ":"")+names[index];
        android.text.SpannableString text=new android.text.SpannableString(fontSample+"\n"+names[index]);
        text.setSpan(new FontSampleSpan(fontFaces.apply(index)),0,fontSample.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new android.text.style.RelativeSizeSpan(2.3f),0,fontSample.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }
    private static final class FontSampleSpan extends android.text.style.MetricAffectingSpan {
        private final android.graphics.Typeface face;
        FontSampleSpan(android.graphics.Typeface face){this.face=face;}
        @Override public void updateDrawState(android.text.TextPaint paint){apply(paint);}
        @Override public void updateMeasureState(android.text.TextPaint paint){apply(paint);}
        private void apply(android.text.TextPaint paint){paint.setTypeface(face);paint.setFakeBoldText(false);paint.setTextSkewX(0);}
    }
    private void choose(int at){requestFocus();cursor=at;change.accept(at);refresh();reveal();}
    private void reveal(){if(chips.length>0)scroll.post(()->{TextView chip=chips[cursor];int left=chip.getLeft(),right=chip.getRight(),x=scroll.getScrollX();if(left<x)scroll.smoothScrollTo(left,0);else if(right>x+scroll.getWidth())scroll.smoothScrollTo(right-scroll.getWidth(),0);});}
    void refresh(){
        if(!multiple&&!hasFocus())for(int i=0;i<chips.length;i++)if(selected.test(i)){cursor=i;break;}
        GradientDrawable row=new GradientDrawable();row.setColor(hasFocus()?0x26FFFFFF:0x09000000);row.setCornerRadius(dp(9));row.setStroke(dp(.6f),hasFocus()?0x70FFFFFF:0x12FFFFFF);setBackground(row);
        StringBuilder desc=new StringBuilder(title);
        for(int i=0;i<chips.length;i++){boolean on=selected.test(i),point=hasFocus()&&cursor==i;GradientDrawable bg=new GradientDrawable();bg.setColor(on?0x33FFFFFF:0x08000000);bg.setCornerRadius(dp(7));bg.setStroke(dp(point?1.3f:.7f),point?0xFFFFFFFF:on?0xB0FFFFFF:0x24FFFFFF);chips[i].setBackground(bg);chips[i].setSelected(on);chips[i].setText(label(i,on));chips[i].setTextColor(on||point?0xFFFFFFFF:0xFFB9C1CC);if(on)desc.append(" · ").append(names[i]);}
        setContentDescription(desc+(multiple?" · Links/Rechts wählen, OK umschalten":" · Links/Rechts ändern"));
    }
}
