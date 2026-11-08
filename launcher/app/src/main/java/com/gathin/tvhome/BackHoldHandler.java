package com.gathin.tvhome;
import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.os.*;
import android.view.KeyEvent;
/** Consume only BACK while opted in; short BACK is replayed once, other keys never change. */
final class BackHoldHandler {
    private final AccessibilityService service;private final Handler handler=new Handler(Looper.getMainLooper());private final LongOkPress press=new LongOkPress();private long replayUntil;private boolean held;
    private final Runnable fire=()->{if(held&&press.fire(SystemClock.uptimeMillis()))open();};
    BackHoldHandler(AccessibilityService service){this.service=service;}
    boolean handle(KeyEvent e){if(e.getKeyCode()!=KeyEvent.KEYCODE_BACK)return false;if(SystemClock.uptimeMillis()<replayUntil)return false;
        if(!held&&!service.getSharedPreferences(Config.SETTINGS_NAME,0).getBoolean(RecentAppsPolicy.ENABLED,false))return false;
        if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getRepeatCount()==0&&!held){held=true;press.press(SystemClock.uptimeMillis());handler.postDelayed(fire,LongOkPress.DELAY);}return true;}
        if(e.getAction()==KeyEvent.ACTION_UP){handler.removeCallbacks(fire);int action=press.release(SystemClock.uptimeMillis(),e.isCanceled());held=false;if(action==LongOkPress.MENU)open();else if(action==LongOkPress.CLICK){replayUntil=SystemClock.uptimeMillis()+250;service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);}return true;}return true;
    }
    private void open(){service.startActivity(new Intent(service,RecentAppsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
    void close(){handler.removeCallbacksAndMessages(null);press.cancel();held=false;}
}
