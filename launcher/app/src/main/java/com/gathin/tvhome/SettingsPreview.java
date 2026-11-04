package com.gathin.tvhome;
import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
/** Hides the existing menu windows without dismissing their pages or moving their selection. */
final class SettingsPreview {
    static void show(Activity a){
        java.util.List<Dialog> hidden=SettingsSheet.hideForPlacement(a);if(hidden.isEmpty())return;
        Dialog dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);SettingsSheet.track(a,dialog);
        FrameLayout root=new FrameLayout(a);root.setFocusable(true);root.setFocusableInTouchMode(true);
        TextView hint=DesignPopup.button(a,a.getString(R.string.visual_preview_return));hint.setTextSize(11);hint.setPadding(SettingsSheet.dp(a,16),0,SettingsSheet.dp(a,16),0);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-2,SettingsSheet.dp(a,34),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);hp.bottomMargin=SettingsSheet.dp(a,20);root.addView(hint,hp);hint.setOnClickListener(v->dialog.dismiss());dialog.setContentView(root);
        Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
        dialog.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_UP)dialog.dismiss();return true;}return key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;});
        dialog.setOnDismissListener(d->{if(!a.isFinishing()&&!a.isDestroyed())SettingsSheet.restoreAfterPlacement(hidden);});dialog.show();if(w!=null)w.setLayout(-1,-1);hint.requestFocus();
    }
}
