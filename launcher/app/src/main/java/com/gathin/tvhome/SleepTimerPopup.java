package com.gathin.tvhome;
import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
final class SleepTimerPopup {
    static String duration(Activity a,int minutes){return minutes==0?a.getString(R.string.idle_clock_off):minutes<180?minutes+" min":(minutes/60)+" h";}
    static String status(Activity a){long end=SleepTimer.end(a);if(end==0)return a.getString(R.string.sleep_inactive);long ms=Math.max(0,end-SystemClock.elapsedRealtime());return a.getString(R.string.sleep_remaining,(ms/60000),(ms/1000)%60);}
    static void show(Activity a,Runnable changed){
        LinearLayout p=GlassDialog.panel(a);p.addView(GlassDialog.text(a,a.getString(R.string.sleep_title),23));
        TextView status=GlassDialog.text(a,status(a),13);status.setPadding(0,GlassDialog.dp(a,8),0,GlassDialog.dp(a,14));p.addView(status);
        Dialog[] dialog={null};TextView[] choices=new TextView[16];int saved=SleepTimer.end(a)>0?SleepTimer.prefs(a).getInt("minutes",0):0;
        for(int row=0;row<4;row++){LinearLayout line=new LinearLayout(a);for(int col=0;col<4;col++){
            int i=row*4+col,minutes=SleepPolicy.MINUTES[i];TextView b=DesignPopup.button(a,duration(a,minutes));b.setTextSize(16);b.setActivated(minutes==saved);choices[i]=b;
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,GlassDialog.dp(a,43),1);lp.setMargins(GlassDialog.dp(a,4),GlassDialog.dp(a,4),GlassDialog.dp(a,4),GlassDialog.dp(a,4));line.addView(b,lp);
            b.setOnClickListener(v->{if(SleepTimer.start(a,minutes)){changed.run();dialog[0].dismiss();}else if(!SleepTimer.exact(a)&&android.os.Build.VERSION.SDK_INT>=31){try{a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+a.getPackageName())));}catch(android.content.ActivityNotFoundException ignored){status.setText(R.string.sleep_permission_missing);}}else status.setText(R.string.sleep_service_missing);});
        }p.addView(line);}
        TextView done=DesignPopup.button(a,a.getString(R.string.apps_done));LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,GlassDialog.dp(a,34));dl.topMargin=GlassDialog.dp(a,12);p.addView(done,dl);
        dialog[0]=GlassDialog.show(a,p,490,370);done.setOnClickListener(v->dialog[0].dismiss());
        Handler h=new Handler(Looper.getMainLooper());Runnable tick=new Runnable(){public void run(){status.setText(status(a));h.postDelayed(this,1000);}};h.postDelayed(tick,1000);dialog[0].setOnDismissListener(d->h.removeCallbacksAndMessages(null));
        for(int i=0;i<choices.length;i++)if(SleepPolicy.MINUTES[i]==saved){choices[i].requestFocus();break;}
    }
}
