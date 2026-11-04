package com.gathin.tvhome;
import android.content.Context;
import android.content.SharedPreferences;
/** Local counts of successful app starts through this launcher. No usage-access permission or telemetry. */
final class AppUsage {
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("app_launch_counts",0);}
    static void record(Context c,String pkg){
        if(pkg==null||pkg.equals(c.getPackageName()))return;
        SharedPreferences p=prefs(c);long old=p.getLong(pkg,0);
        p.edit().putLong(pkg,old==Long.MAX_VALUE?old:old+1).putLong("revision",p.getLong("revision",0)+1).apply();
    }
    static long revision(Context c){return prefs(c).getLong("revision",0);}
}
