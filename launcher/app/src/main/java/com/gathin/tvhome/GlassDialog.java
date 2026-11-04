package com.gathin.tvhome;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
final class GlassDialog {
    static int dp(Activity a,float n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static LinearLayout panel(Activity a){
        LinearLayout p=new LinearLayout(a);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(a,24),dp(a,20),dp(a,24),dp(a,20));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xFA191C24);bg.setCornerRadius(dp(a,24));bg.setStroke(dp(a,1),0x44FFFFFF);p.setBackground(bg);return p;
    }
    static TextView text(Activity a,String text,int size){TextView t=new TextView(a);t.setText(text);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setTypeface(android.graphics.Typeface.create(a.getString(R.string.font_regular),android.graphics.Typeface.NORMAL));return t;}
    static Dialog show(Activity a,LinearLayout panel,int width,int height){
        Dialog d=new Dialog(a);d.requestWindowFeature(Window.FEATURE_NO_TITLE);d.setContentView(panel);Window w=d.getWindow();
        if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.dimAmount=.58f;p.gravity=Gravity.CENTER;p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;w.setAttributes(p);}
        NavigationSounds.attach(a,w);d.show();if(w!=null)w.setLayout(dp(a,width),height<0?height:dp(a,height));return d;
    }
}
