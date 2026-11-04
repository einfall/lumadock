package com.gathin.tvhome;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
final class SleepTimer {
    static final String SHOW="show_sleep_pill";
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("sleep_timer",Context.MODE_PRIVATE);}
    private static int boot(Context c){return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}
    static long end(Context c){SharedPreferences p=prefs(c);return p.getInt("boot",-2)==boot(c)?p.getLong("end",0):0;}
    static boolean ready(){return HomeKeyService.canSleepTimer();}
    static boolean exact(Context c){AlarmManager am=c.getSystemService(AlarmManager.class);return am!=null&&(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms());}
    static boolean start(Context c,int minutes){
        if(!SleepPolicy.allowed(minutes))return false;if(minutes==0){cancel(c);return true;}
        if(!ready()||!exact(c))return false;
        HomeKeyService.hideSleepCountdown();
        long deadline=SleepPolicy.deadline(SystemClock.elapsedRealtime(),minutes);
        prefs(c).edit().putLong("end",deadline).putInt("minutes",minutes).putInt("boot",boot(c)).remove("error").apply();
        if(!schedule(c,deadline)){cancel(c);return false;}return true;
    }
    static void cancel(Context c){HomeKeyService.hideSleepCountdown();prefs(c).edit().remove("end").remove("minutes").apply();AlarmManager am=c.getSystemService(AlarmManager.class);if(am!=null){am.cancel(PendingIntent.getBroadcast(c,604,new Intent(c,SleepTimerReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));am.cancel(pending(c,0,false));am.cancel(pending(c,0,true));}}
    static void restore(Context c){long e=end(c);if(e==0){cancel(c);return;}if(e<=SystemClock.elapsedRealtime())expire(c,e);else if(!schedule(c,e))cancel(c);}
    private static boolean schedule(Context c,long e){
        AlarmManager am=c.getSystemService(AlarmManager.class);if(am==null)return false;
        try{
            am.cancel(pending(c,0,true));
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,e,pending(c,e,false));
            long warning=e-SleepPolicy.WARNING_MS;
            if(warning>SystemClock.elapsedRealtime())am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,warning,pending(c,e,true));
            else warn(c,e);
            return true;
        }catch(SecurityException ex){return false;}
    }
    private static PendingIntent pending(Context c,long e,boolean warning){return PendingIntent.getBroadcast(c,warning?605:604,new Intent(c,SleepTimerReceiver.class).setAction(warning?"sleep.WARNING":"sleep.EXPIRE").putExtra("deadline",e),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static void warn(Context c,long expected){
        if(SleepPolicy.warningDue(SystemClock.elapsedRealtime(),end(c),expected))HomeKeyService.showSleepCountdown(expected);
    }
    static void expire(Context c,long expected){
        long current=end(c);if(expected==0||current!=expected||SystemClock.elapsedRealtime()+100<current)return;
        cancel(c);boolean slept=HomeKeyService.sleepForTimer();prefs(c).edit().putLong("last_fired",System.currentTimeMillis()).putBoolean("last_success",slept).apply();
        if(!slept)android.widget.Toast.makeText(c,R.string.sleep_service_missing,android.widget.Toast.LENGTH_LONG).show();
    }
}
