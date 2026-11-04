package com.gathin.tvhome;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public final class SleepTimerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        long deadline=intent.getLongExtra("deadline",0);
        if("sleep.WARNING".equals(intent.getAction()))SleepTimer.warn(c,deadline);else SleepTimer.expire(c,deadline);
    }
}
