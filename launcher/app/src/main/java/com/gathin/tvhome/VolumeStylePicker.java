package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A local sample preview. It never sends volume keys or queries the soundbar. */
final class VolumeStylePicker {
    static void show(Activity activity,VolumeStyle[] selected,Runnable changed){
        Dialog dialog=new Dialog(activity);NavigationSounds.attach(activity,dialog.getWindow());dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Handler handler=new Handler(Looper.getMainLooper());
        VolumeOsdView[] sample={null};
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity,22),dp(activity,18),dp(activity,22),dp(activity,18));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xFA191C23);bg.setCornerRadius(dp(activity,22));
        bg.setStroke(dp(activity,1),0x55FFFFFF);panel.setBackground(bg);
        TextView title=label(activity,R.string.volume_style_title,23);panel.addView(title);
        TextView hint=label(activity,R.string.volume_style_hint,12);hint.setTextColor(0xFFBAC3D3);
        hint.setPadding(0,dp(activity,6),0,dp(activity,12));panel.addView(hint);
        LinearLayout row=new LinearLayout(activity);
        LinearLayout choices=new LinearLayout(activity);choices.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams choicesLp=new LinearLayout.LayoutParams(dp(activity,132),-1);
        choicesLp.setMarginEnd(dp(activity,16));row.addView(choices,choicesLp);
        FrameLayout preview=new FrameLayout(activity);preview.setBackgroundColor(0xFF080B10);
        row.addView(preview,new LinearLayout.LayoutParams(0,-1,1f));
        panel.addView(row,new LinearLayout.LayoutParams(-1,dp(activity,216)));
        TextView[] buttons=new TextView[VolumeStyle.values().length];
        Runnable mount=()->{
            if(sample[0]!=null)sample[0].stop();preview.removeAllViews();
            VolumeStyle style=selected[0];VolumeOsdView view=new VolumeOsdView(activity,style);sample[0]=view;
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(activity,style.width),dp(activity,style.height));
            lp.gravity=gravity(style);lp.setMargins(dp(activity,12),dp(activity,12),dp(activity,12),dp(activity,12));
            preview.addView(view,lp);
            preview.post(()->{
                if(sample[0]!=view)return;
                float scale=Math.min(1f,Math.min((preview.getWidth()-dp(activity,24f))/(float)dp(activity,style.width),
                        (preview.getHeight()-dp(activity,24f))/(float)dp(activity,style.height)));
                view.setScaleX(scale);view.setScaleY(scale);view.setAlpha(0f);
                view.animate().alpha(1f).setDuration(200).start();view.state(35,false);
            });
            for(int i=0;i<buttons.length;i++)if(buttons[i]!=null)buttons[i].setActivated(VolumeStyle.values()[i]==style);
        };
        for(int i=0;i<buttons.length;i++){
            VolumeStyle style=VolumeStyle.values()[i];TextView button=DesignPopup.button(activity,activity.getString(style.label()));
            buttons[i]=button;button.setOnClickListener(v->{selected[0]=style;changed.run();mount.run();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(activity,37));lp.bottomMargin=dp(activity,6);
            choices.addView(button,lp);
        }
        TextView note=label(activity,R.string.volume_style_sample,11);note.setTextColor(0xFFB4BECE);
        note.setPadding(0,dp(activity,12),0,dp(activity,10));panel.addView(note);
        TextView done=DesignPopup.button(activity,activity.getString(R.string.volume_style_done));
        panel.addView(done,new LinearLayout.LayoutParams(-1,dp(activity,36)));done.setOnClickListener(v->dialog.dismiss());
        Runnable cycle=new Runnable(){int step;
            public void run(){if(sample[0]!=null){step=(step+1)%4;sample[0].state(step==0?35:step==1?65:step==2?65:18,step==2);}
                handler.postDelayed(this,1400);}};
        dialog.setOnDismissListener(d->{handler.removeCallbacksAndMessages(null);if(sample[0]!=null)sample[0].stop();});
        dialog.setContentView(panel);Window window=dialog.getWindow();if(window!=null)window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();if(window!=null)window.setLayout(dp(activity,640),-2);
        mount.run();handler.postDelayed(cycle,1400);
        buttons[selected[0].ordinal()].requestFocus();
    }
    static int gravity(VolumeStyle style){
        switch(style){case LG:return Gravity.RIGHT|Gravity.CENTER_VERTICAL;case SAMSUNG:return Gravity.LEFT|Gravity.CENTER_VERTICAL;
            case PHILIPS:return Gravity.LEFT|Gravity.BOTTOM;default:return Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;}
    }
    private static TextView label(Activity a,int id,int size){TextView t=new TextView(a);t.setText(id);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
    private static int dp(Activity a,float value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
}
