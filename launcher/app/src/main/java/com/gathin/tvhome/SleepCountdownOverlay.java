package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.graphics.*;
import android.os.*;
import android.view.*;

/** A ten-second, non-interactive warning. Never wakes the display or intercepts remote keys. */
final class SleepCountdownOverlay {
    private final AccessibilityService service;
    private final WindowManager windows;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Countdown view;
    private long deadline;
    SleepCountdownOverlay(AccessibilityService s){service=s;windows=s.getSystemService(WindowManager.class);}
    void show(long end){
        close();PowerManager power=service.getSystemService(PowerManager.class);
        if(windows==null||power==null||!power.isInteractive())return;
        deadline=end;view=new Countdown();
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(dp(236),dp(256),WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.CENTER;lp.setTitle("Sleep timer countdown");
        try{windows.addView(view,lp);view.setAlpha(0);view.setScaleX(.94f);view.setScaleY(.94f);view.animate().alpha(1).scaleX(1).scaleY(1).setDuration(200).start();handler.post(tick);}
        catch(RuntimeException ex){close();}
    }
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(view==null)return;
        PowerManager power=service.getSystemService(PowerManager.class);
        if(SleepTimer.end(service)!=deadline||power==null||!power.isInteractive()){close();return;}
        if(SystemClock.elapsedRealtime()>=deadline){long expected=deadline;close();SleepTimer.expire(service,expected);return;}
        view.invalidate();handler.postDelayed(this,33);
    }};
    void close(){handler.removeCallbacks(tick);if(view!=null){view.animate().cancel();try{windows.removeView(view);}catch(RuntimeException ignored){}view=null;}deadline=0;}
    private int dp(float n){return Math.round(n*service.getResources().getDisplayMetrics().density);}
    private final class Countdown extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF panel=new RectF(),ring=new RectF();
        Countdown(){super(service);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();panel.set(dp(2),dp(2),w-dp(2),h-dp(2));
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xF51A1D26);c.drawRoundRect(panel,dp(27),dp(27),paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0x38FFFFFF);c.drawRoundRect(panel,dp(27),dp(27),paint);
            float cx=w/2,cy=dp(123),radius=dp(71);ring.set(cx-radius,cy-radius,cx+radius,cy+radius);
            paint.setStrokeWidth(dp(5));paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0x24FFFFFF);c.drawOval(ring,paint);
            long now=SystemClock.elapsedRealtime();float progress=SleepPolicy.progress(now,deadline);
            paint.setColor(0xFFFFB36B);c.drawArc(ring,-90,360*progress,false,paint);paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.FILL);paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
            paint.setColor(Color.WHITE);paint.setTextSize(dp(58));float baseline=cy-(paint.ascent()+paint.descent())/2;c.drawText(Integer.toString(SleepPolicy.countdown(now,deadline)),cx,baseline,paint);
            paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(dp(13));paint.setColor(0xFFEDF1F8);c.drawText(service.getString(R.string.sleep_countdown_title),cx,dp(224),paint);
        }
    }
}
