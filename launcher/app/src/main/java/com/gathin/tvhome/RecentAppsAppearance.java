package com.gathin.tvhome;
import android.app.Activity;
import android.os.Build;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.WindowManager;
/** Optional OS blur; opaque glass and dim remain readable even when the compositor disables blur. */
final class RecentAppsAppearance {
    static final String BLUR="recent_apps_background_blur";
    static GlassView frame(Activity activity, int color, float radius) {
        TileEffects style = FocusStyle.profile(activity, true);
        GlassView frame = new GlassView(activity, null);
        frame.setStyle(Config.GLASS_RIM);
        frame.setEffects(new TileEffects(true, false, false, style.thin, "dominant", style.gap,
                style.contrast, style.contrastWidth, style.width, false, style.glow, style.glowStrength, style.glowSpread));
        frame.setFocusColor(color);
        frame.setRadius(radius + frame.outset());
        return frame;
    }

    static GradientDrawable plate(Activity activity, int fill, int stroke, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        if (stroke != 0) shape.setStroke(GlassDialog.dp(activity, 1.5f), stroke);
        return shape;
    }

    static Drawable control(Activity activity, int color) {
        float radius = GlassDialog.dp(activity, 10);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, plate(activity, 0xFF273746, color, radius));
        states.addState(new int[0], plate(activity, 0x243F5165, 0, radius));
        return states;
    }

    static boolean supported(Activity a){return Build.VERSION.SDK_INT>=31&&a.getWindowManager().isCrossWindowBlurEnabled();}
    static void apply(Activity a){if(!a.getSharedPreferences(Config.SETTINGS_NAME,0).getBoolean(BLUR,false)||!supported(a))return;try{if(Build.VERSION.SDK_INT>=31){WindowManager.LayoutParams p=a.getWindow().getAttributes();p.flags|=WindowManager.LayoutParams.FLAG_BLUR_BEHIND;p.setBlurBehindRadius(Math.round(28*a.getResources().getDisplayMetrics().density));a.getWindow().setAttributes(p);}}catch(RuntimeException ignored){/* Dim stays active. */}}
}
