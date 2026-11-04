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
    private final Activity a;private final LinearLayout host;private final SharedPreferences prefs;private final boolean defaultEnabled;
    private final Handler main=new Handler(Looper.getMainLooper());private HandlerThread thread;private volatile Handler reader;
    private final Card cpu,ram,storage;private boolean active,closed;private int externalMask,runningMask,runningDelay;
    static final class Snapshot {final int cpu;final long ramTotal,ramAvailable,diskTotal,diskAvailable;final double rx,tx;Snapshot(int cpu,long rt,long ra,long dt,long da){this(cpu,rt,ra,dt,da,-1,-1);}Snapshot(int cpu,long rt,long ra,long dt,long da,double rx,double tx){this.cpu=cpu;ramTotal=rt;ramAvailable=ra;diskTotal=dt;diskAvailable=da;this.rx=rx;this.tx=tx;}}
    interface Observer{void updated(Snapshot snapshot);}
    private final java.util.List<Observer> observers=new java.util.ArrayList<>();private Snapshot latest=new Snapshot(-1,0,-1,0,-1);
    void observe(Observer observer){observers.add(observer);observer.updated(latest);}
    void forget(Observer observer){observers.remove(observer);}
    void demand(int mask){mask&=15;if(mask!=externalMask){externalMask=mask;configure();}}
    void refreshStyle(){cpu.style();ram.style();storage.style();}
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(key!=null&&key.startsWith("system_stats_"))configure();};
    private final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))stopReader();else configure();}};
    SystemStatsWidget(Activity a){
        this.a=a;host=a.findViewById(R.id.stats);prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);defaultEnabled=Config.load(a).showStats;
        cpu=new Card("cpu",a.getString(R.string.system_stats_cpu));ram=new Card("memory",a.getString(R.string.system_stats_ram));storage=new Card("storage",a.getString(R.string.system_stats_storage));
        prefs.registerOnSharedPreferenceChangeListener(listener);IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_OFF);filter.addAction(Intent.ACTION_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=33)a.registerReceiver(screen,filter,Context.RECEIVER_NOT_EXPORTED);else a.registerReceiver(screen,filter);configure();
    }
    void start(){if(closed)return;active=true;configure();}
    void stop(){active=false;stopReader();}
    void close(){if(closed)return;closed=true;stop();prefs.unregisterOnSharedPreferenceChangeListener(listener);a.unregisterReceiver(screen);main.removeCallbacksAndMessages(null);observers.clear();}
    private int dp(float n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
    private boolean interactive(){PowerManager p=a.getSystemService(PowerManager.class);return p!=null&&p.isInteractive();}
    private void configure(){
        if(closed)return;boolean enabled=prefs.getBoolean(ENABLED,defaultEnabled);
        boolean showCpu=prefs.getBoolean(CPU,true),showRam=prefs.getBoolean(RAM,true),showStorage=prefs.getBoolean(STORAGE,true);
        cpu.box.setVisibility(showCpu?View.VISIBLE:View.GONE);ram.box.setVisibility(showRam?View.VISIBLE:View.GONE);storage.box.setVisibility(showStorage?View.VISIBLE:View.GONE);
        host.setVisibility(enabled&&!prefs.getBoolean(COMBINED,false)&&(showCpu||showRam||showStorage)?View.VISIBLE:View.GONE);
        int mask=WidgetSource.demand(enabled,(showCpu?1:0)|(showRam?2:0)|(showStorage?4:0),externalMask);
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
                    if(prefs.getBoolean(CPU,true))cpu.update(cpuLoad<0?"—":cpuLoad+" %",a.getString(cpuLoad<0?R.string.system_stats_cpu_missing:R.string.system_stats_cpu_detail),cpuLoad);
                    int usedRam=SystemStatsPolicy.used(ma,mt);if(prefs.getBoolean(RAM,true))ram.update(usedRam<0?"—":a.getString(R.string.system_stats_of,size(mt-Math.min(ma,mt)),size(mt)),usedRam<0?a.getString(R.string.system_stats_unavailable):a.getString(R.string.system_stats_used,usedRam),usedRam);
                    int usedDisk=SystemStatsPolicy.used(da,dt);if(prefs.getBoolean(STORAGE,true))storage.update(usedDisk<0?"—":a.getString(R.string.system_stats_free,size(da)),usedDisk<0?a.getString(R.string.system_stats_unavailable):a.getString(R.string.system_stats_total,usedDisk,size(dt)),usedDisk);
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
    private String size(long bytes){return Formatter.formatShortFileSize(a,Math.max(0,bytes));}
    private final class Card {
        final LinearLayout box;final TextView value,detail,name;final ImageView icon;final ProgressBar bar;final String title;
        Card(String glyph,String title){
            this.title=title;box=new LinearLayout(a);box.setId("cpu".equals(glyph)?R.id.systemCpu:"memory".equals(glyph)?R.id.systemRam:R.id.systemStorage);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(9),dp(7),dp(9),dp(7));box.setFocusable(false);
            GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xB3242B35,0xB30D121B});background.setCornerRadius(dp(10));background.setStroke(dp(.5f),0x2EFFFFFF);box.setBackground(background);
            LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);icon=new ImageView(a);icon.setImageDrawable(new AppGlyph(glyph));icon.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFB6C8E0));heading.addView(icon,new LinearLayout.LayoutParams(dp(11),dp(11)));
            name=text(title,9,0xFFBCC8DA);LinearLayout.LayoutParams nameParams=new LinearLayout.LayoutParams(0,-2,1);nameParams.setMarginStart(dp(5));heading.addView(name,nameParams);box.addView(heading);
            value=text("—",13,0xFFF6F8FC);value.setAutoSizeTextTypeUniformWithConfiguration(9,13,1,TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams valueParams=new LinearLayout.LayoutParams(-1,dp(21));box.addView(value,valueParams);
            detail=text(a.getString(R.string.system_stats_loading),8,0xFF99A9BF);box.addView(detail,new LinearLayout.LayoutParams(-1,dp(13)));
            bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0x28FFFFFF));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(2));bp.topMargin=dp(4);box.addView(bar,bp);
            LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(0,-2,1);cardParams.setMarginEnd(dp(5));host.addView(box,cardParams);style();
        }
        void style(){String id=SurfaceThemes.widget(a);SurfaceStyle style=SurfaceStyle.of(id,false);SurfaceThemes.widget(box,id);value.setTextColor(style.ink);detail.setTextColor(style.muted);name.setTextColor("original".equals(id)?0xFFBCC8DA:style.muted);icon.setImageTintList(android.content.res.ColorStateList.valueOf("original".equals(id)?0xFFB6C8E0:style.muted));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf("light".equals(id)?0x28102038:0x28FFFFFF));}
        TextView text(String text,int sp,int color){TextView view=GlassDialog.text(a,text,sp);view.setTextColor(color);view.setSingleLine(true);view.setEllipsize(TextUtils.TruncateAt.END);view.setIncludeFontPadding(false);return view;}
        void update(String reading,String caption,int percent){value.setText(reading);detail.setText(caption);bar.setProgress(Math.max(0,percent));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(percent>=90?0xFFF28A83:percent>=75?0xFFF2BA75:0xFF94BFF5));box.setContentDescription(title+" · "+reading+" · "+caption);}
    }
}
