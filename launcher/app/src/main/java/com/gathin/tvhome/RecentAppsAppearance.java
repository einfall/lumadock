package com.gathin.tvhome;
import android.app.Activity;
import android.os.Build;
import android.view.WindowManager;
/** Optional OS blur; opaque glass and dim remain readable even when the compositor disables blur. */
final class RecentAppsAppearance {
    static final String BLUR="recent_apps_background_blur";
    static boolean supported(Activity a){return Build.VERSION.SDK_INT>=31&&a.getWindowManager().isCrossWindowBlurEnabled();}
    static void apply(Activity a){if(!a.getSharedPreferences(Config.SETTINGS_NAME,0).getBoolean(BLUR,false)||!supported(a))return;try{if(Build.VERSION.SDK_INT>=31){WindowManager.LayoutParams p=a.getWindow().getAttributes();p.flags|=WindowManager.LayoutParams.FLAG_BLUR_BEHIND;p.setBlurBehindRadius(Math.round(28*a.getResources().getDisplayMetrics().density));a.getWindow().setAttributes(p);}}catch(RuntimeException ignored){/* Dim stays active. */}}
}
