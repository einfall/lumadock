package com.gathin.tvhome;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Collections;
/** Locks starts performed by this launcher, not other apps or Philips system launch paths. */
final class AppLock {
    static final String LOCKS="app_locks",VERIFIER="pin_verifier";
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("app_security",Context.MODE_PRIVATE);}
    static boolean configured(Context c){return prefs(c).contains(VERIFIER);}
    static boolean locked(Context c,String pkg){return configured(c)&&c.getSharedPreferences(Config.SETTINGS_NAME,Context.MODE_PRIVATE).getStringSet(LOCKS,Collections.emptySet()).contains(pkg);}
    static void launch(Context c,Intent target){
        String pkg=target.getComponent()==null?target.getPackage():target.getComponent().getPackageName();
        if(pkg!=null&&locked(c,pkg))c.startActivity(new Intent(c,AppGateActivity.class).putExtra("target",target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        else {c.startActivity(target);AppUsage.record(c,pkg);}
    }
}
