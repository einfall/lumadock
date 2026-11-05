package com.gathin.tvhome;

import android.app.*;
import android.content.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Three optional local metrics. No work while disabled, away from Home, or with the display off. */
final class SystemStatsWidget {
    static final String NETWORK="system_stats_network";
    static final String COMBINED="system_stats_combined";
    static final String ENABLED="system_stats_enabled",CPU="system_stats_cpu",RAM="system_stats_ram",STORAGE="system_stats_storage",SECONDS="system_stats_seconds";
    private final Activity a;private final SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());private HandlerThread thread;private volatile Handler reader;
    private boolean active,closed;private int externalMask,runningMask,runningDelay;
    static final class Snapshot {final int cpu;final long ramTotal,ramAvailable,diskTotal,diskAvailable;final double rx,tx;Snapshot(int cpu,long rt,long ra,long dt,long da){this(cpu,rt,ra,dt,da,-1,-1);}Snapshot(int cpu,long rt,long ra,long dt,long da,double rx,double tx){this.cpu=cpu;ramTotal=rt;ramAvailable=ra;diskTotal=dt;diskAvailable=da;this.rx=rx;this.tx=tx;}}
    interface Observer{void updated(Snapshot snapshot);}
    private final java.util.List<Observer> observers=new java.util.ArrayList<>();private Snapshot latest=new Snapshot(-1,0,-1,0,-1);
    void observe(Observer observer){observers.add(observer);observer.updated(latest);}
    void forget(Observer observer){observers.remove(observer);}
    void demand(int mask){mask&=15;if(mask!=externalMask){externalMask=mask;configure();}}
    void refreshStyle(){}
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(key!=null&&key.startsWith("system_stats_"))configure();};
    private final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))stopReader();else configure();}};
    SystemStatsWidget(Activity a){
        this.a=a;prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);
        // Hidden ID placeholders preserve old header coordinates without creating legacy cards.
        LinearLayout retired=a.findViewById(R.id.stats);retired.setVisibility(View.GONE);
        for(int id:new int[]{R.id.systemCpu,R.id.systemRam,R.id.systemStorage}){LinearLayout v=new LinearLayout(a);v.setId(id);retired.addView(v,new LinearLayout.LayoutParams(0,0,1));}
        prefs.registerOnSharedPreferenceChangeListener(listener);IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=33)a.registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED);else a.registerReceiver(screen,filter);configure();
    }
    void start(){if(closed)return;active=true;configure();}
    void stop(){active=false;stopReader();}
    void close(){if(closed)return;closed=true;stop();prefs.unregisterOnSharedPreferenceChangeListener(listener);a.unregisterReceiver(screen);main.removeCallbacksAndMessages(null);observers.clear();}
    private int dp(float n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
    private boolean interactive(){PowerManager p=a.getSystemService(PowerManager.class);return p!=null&&p.isInteractive();}
    private void configure(){
        if(closed)return;int mask=externalMask&15;
        final boolean readCpu=(mask&1)!=0,readRam=(mask&2)!=0,readStorage=(mask&4)!=0,readNetwork=(mask&8)!=0;
        int delay=1000*SystemStatsPolicy.seconds(prefs.getInt(SECONDS,10));
        if(mask==0||!active||!interactive()){stopReader();return;}
        if(reader!=null&&runningMask==mask&&runningDelay==delay)return;
        stopReader();runningMask=mask;runningDelay=delay;
        thread=new HandlerThread("home-system-stats");thread.start();Handler run=new Handler(thread.getLooper());reader=run;
        run.post(new Runnable(){SystemStatsPolicy.Cpu previous;boolean direct=true;java.util.Map<String,NetworkTrafficPolicy.Counters> previousNetwork;long networkTime;
            @Override public void run(){
                if(reader!=run)return;int load=-1;long memoryTotal=0,memoryAvailable=-1,diskTotal=0,diskAvailable=-1;
                if(readCpu){
                    SystemStatsPolicy.Cpu next=sample();load=SystemStatsPolicy.cpu(previous,next);previous=next;
                    if(load<0&&next!=null){try{Thread.sleep(250);}catch(InterruptedException ex){Thread.currentThread().interrupt();return;}next=sample();load=SystemStatsPolicy.cpu(previous,next);previous=next;}
                }
                if(reader!=run)return;
                if(readRam){try{ActivityManager manager=a.getSystemService(ActivityManager.class);ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();if(manager!=null){manager.getMemoryInfo(info);memoryTotal=info.totalMem;memoryAvailable=info.availMem;}}catch(RuntimeException ignored){}}
                if(readStorage){try{StatFs fs=new StatFs(Environment.getDataDirectory().getAbsolutePath());diskTotal=fs.getTotalBytes();diskAvailable=fs.getAvailableBytes();}catch(RuntimeException ignored){}}
                double rx=-1,tx=-1;
                if(readNetwork){String text=null;try{StringBuilder all=new StringBuilder();try(BufferedReader input=new BufferedReader(new FileReader("/proc/net/dev"))){String line;while((line=input.readLine())!=null&&all.length()<65536)all.append(line).append('\n');}text=all.toString();}catch(IOException|SecurityException ignored){}
                    if(NetworkTrafficPolicy.parse(text).isEmpty())try{text=LocalCecReader.readNetwork();}catch(Exception ignored){}
                    java.util.Map<String,NetworkTrafficPolicy.Counters> next=NetworkTrafficPolicy.parse(text);long now=SystemClock.elapsedRealtime();NetworkTrafficPolicy.Rate rate=NetworkTrafficPolicy.rate(previousNetwork,next,now-networkTime);previousNetwork=next;networkTime=now;rx=rate.rx;tx=rate.tx;}
                final double received=rx,sent=tx;
                final int cpuLoad=load;final long mt=memoryTotal,ma=memoryAvailable,dt=diskTotal,da=diskAvailable;
                main.post(()->{if(reader!=run||closed||!active||!interactive())return;
                    latest=new Snapshot(cpuLoad,mt,ma,dt,da,received,sent);for(Observer observer:new java.util.ArrayList<>(observers))observer.updated(latest);

                });
                if(reader==run)run.postDelayed(this,delay);
            }
            private SystemStatsPolicy.Cpu sample(){
                if(reader!=run)return null;String line=null;
                if(direct){try(BufferedReader input=new BufferedReader(new FileReader("/proc/stat"))){line=input.readLine();}catch(IOException|SecurityException ignored){}if(SystemStatsPolicy.parse(line)==null)direct=false;}
                if(!direct){try{line=LocalCecReader.readCpu();}catch(Exception ignored){return null;}}
                return SystemStatsPolicy.parse(line);
            }
        });
    }
    private void stopReader(){runningMask=0;runningDelay=0;Handler old=reader;reader=null;if(old!=null)old.removeCallbacksAndMessages(null);if(thread!=null){thread.quit();thread.interrupt();thread=null;}}
}
