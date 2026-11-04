package com.gathin.tvhome;

import android.app.Activity;

import android.content.res.ColorStateList;

import android.view.*;

import android.widget.*;

/** One focus stop per slider: left/right changes exactly one integer, up/down changes the setting. */

final class GridSlider extends LinearLayout {

    interface Changed {void value(int n);}

    private final int min;private int max;private java.util.function.IntSupplier maxSource;private int value;private final TextView title,number;private final SeekBar bar;private final Changed changed;

    GridSlider(Activity a,String label,int min,int max,int initial,Changed changed){

        super(a);this.min=min;this.max=max;this.value=Math.max(min,Math.min(max,initial));this.changed=changed;setOrientation(VERTICAL);setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,7));setBackground(SettingsSheet.rowBackground(a));setFocusable(true);setFocusableInTouchMode(true);

        LinearLayout head=new LinearLayout(a);title=GlassDialog.text(a,label,13);AppGlyph glyph=new AppGlyph(MenuIcon.of(label));glyph.setBounds(0,0,dp(a,15),dp(a,15));title.setCompoundDrawables(glyph,null,null,null);title.setCompoundDrawablePadding(dp(a,8));head.addView(title,new LinearLayout.LayoutParams(0,-2,1));number=GlassDialog.text(a,Integer.toString(value),14);head.addView(number);addView(head);

        bar=new SeekBar(a);bar.setMax(max-min);bar.setProgress(value-min);bar.setFocusable(false);bar.setSplitTrack(false);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(a,23));bp.topMargin=dp(a,2);addView(bar,bp);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){requestFocus();}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int progress,boolean user){if(user)update(min+progress);}});

        setOnFocusChangeListener((v,on)->{if(on)refreshMax();paint(on);});setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){if(event.getAction()==KeyEvent.ACTION_DOWN)update(value+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1));return true;}return false;});paint(false);setContentDescription(label+" · "+value);

    }

    GridSlider maxFrom(java.util.function.IntSupplier source){maxSource=source;refreshMax();return this;}
    private void refreshMax(){if(maxSource!=null){int next=Math.max(min,maxSource.getAsInt());if(next!=max){max=next;bar.setMax(max-min);syncValue(value);}}}
    void syncValue(int n){value=Math.max(min,Math.min(max,n));bar.setProgress(value-min);number.setText(Integer.toString(value));setContentDescription(title.getText()+" · "+value);}
    private void update(int n){refreshMax();n=Math.max(min,Math.min(max,n));if(n==value)return;syncValue(n);changed.value(value);}

    private void paint(boolean focus){for(android.graphics.drawable.Drawable icon:title.getCompoundDrawables())if(icon!=null)icon.setTintList(ColorStateList.valueOf(focus?0xFF171D25:0xFFECF1F8));title.setTextColor(focus?0xFF171D25:0xFFECF1F8);number.setTextColor(focus?0xFF295C90:0xFF91C5FF);bar.setProgressTintList(ColorStateList.valueOf(focus?0xFF487CB4:0xFF91BEF2));bar.setProgressBackgroundTintList(ColorStateList.valueOf(focus?0xFFCAD3DD:0xFF364253));bar.setThumbTintList(ColorStateList.valueOf(focus?0xFF487CB4:0xFFE6EFFA));}

    private static int dp(Activity a,int n){return SettingsSheet.dp(a,n);}

}

