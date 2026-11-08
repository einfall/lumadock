package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.LinearLayout;
import java.util.*;
/** Layout offsets only. Existing views, weather actions and the clock's lifecycle stay intact. */
final class HeaderLayoutController implements PlacementTarget {
    static final String P="header_layout_",MODE=P+"mode",STEP=P+"step";
    static final String[] KEYS={"greeting","clock","date","weather","cpu","ram","storage"};
    private static final int[] IDS={R.id.greeting,R.id.clock,R.id.dateBlock,R.id.weatherBlock,R.id.systemCpu,R.id.systemRam,R.id.systemStorage};
    private final Activity a;private final View root;private final SharedPreferences prefs;private final View[] views=new View[7];private final float density;private final boolean greetingDefault,statsDefault;
    private final android.widget.TextClock dateClock;private final CharSequence date12,date24;private final int dateHeight;private final int greetingWidth,dateWidth,dividerVisibility;private final float dateWeight,headerElevation;
    private boolean pending,closed;private String previewKey;private int previewX,previewY;
    private final ViewTreeObserver.OnGlobalLayoutListener layout=()->schedule();
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(key!=null&&(key.startsWith(P)||key.startsWith("system_stats_")))schedule();};
    HeaderLayoutController(Activity a,Config cfg){
        this.a=a;root=a.findViewById(R.id.root);prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);density=a.getResources().getDisplayMetrics().density;greetingDefault=cfg.showGreeting;statsDefault=cfg.showStats;
        for(int i=0;i<views.length;i++){views[i]=a.findViewById(IDS[i]);for(View v=views[i];v.getParent() instanceof ViewGroup;){ViewGroup parent=(ViewGroup)v.getParent();parent.setClipChildren(false);parent.setClipToPadding(false);v=parent;}}
        dateClock=a.findViewById(R.id.date);date12=dateClock.getFormat12Hour();date24=dateClock.getFormat24Hour();dateHeight=views[2].getLayoutParams().height;greetingWidth=views[0].getLayoutParams().width;LinearLayout.LayoutParams date=(LinearLayout.LayoutParams)views[2].getLayoutParams();dateWidth=date.width;dateWeight=date.weight;dividerVisibility=a.findViewById(R.id.clockDivider).getVisibility();headerElevation=((View)a.findViewById(R.id.leftBlock).getParent()).getElevation();
        prefs.registerOnSharedPreferenceChangeListener(listener);root.getViewTreeObserver().addOnGlobalLayoutListener(layout);schedule();
    }
    static boolean weatherShown(Context c){return c.getSharedPreferences(Config.SETTINGS_NAME,0).getBoolean(P+"show_weather",true);}
    static int index(String key){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;throw new IllegalArgumentException("Unknown header item");}
    public View view(String key){return views[index(key)];}
    int dp(float px){return Math.round(px/density);}
    public int width(){return dp(root.getWidth());}public int height(){return dp(root.getHeight());}
    public int[] size(String key){View v=view(key);return new int[]{dp(v.getWidth()),dp(v.getHeight())};}
    public int[] position(String key){Rect b=bounds(key);return new int[]{dp(b.left),dp(b.top)};}
    public Rect bounds(String key){View v=view(key);int[] loc=new int[2],origin=new int[2];v.getLocationOnScreen(loc);root.getLocationOnScreen(origin);float sx=v.getScaleX(),sy=v.getScaleY();return new Rect(Math.round(loc[0]-origin[0]+v.getWidth()*(sx-1)/2),Math.round(loc[1]-origin[1]+v.getHeight()*(sy-1)/2),Math.round(loc[0]-origin[0]+v.getWidth()*(sx+1)/2),Math.round(loc[1]-origin[1]+v.getHeight()*(sy+1)/2));}
    boolean shown(String key){int i=index(key);if(i<4)return prefs.getBoolean(P+"show_"+key,i!=0||greetingDefault);return false;}
    private static String metricKey(int i){return i==4?SystemStatsWidget.CPU:i==5?SystemStatsWidget.RAM:SystemStatsWidget.STORAGE;}
    void show(String key,boolean on){int i=index(key);SharedPreferences.Editor edit=prefs.edit();if(i<4)edit.putBoolean(P+"show_"+key,on);else{if(on&&!prefs.getBoolean(SystemStatsWidget.ENABLED,statsDefault))edit.putBoolean(SystemStatsWidget.CPU,false).putBoolean(SystemStatsWidget.RAM,false).putBoolean(SystemStatsWidget.STORAGE,false);if(on)edit.putBoolean(SystemStatsWidget.ENABLED,true);edit.putBoolean(metricKey(i),on);}edit.apply();}
    public void set(String key,int x,int y){prefs.edit().putInt(P+key+"_x",x).putInt(P+key+"_y",y).apply();apply();}
    void preset(String mode){SharedPreferences.Editor edit=prefs.edit().putString(MODE,HeaderLayoutPolicy.mode(mode));for(String key:KEYS)edit.remove(P+key+"_x").remove(P+key+"_y");edit.apply();apply();}
    void reset(String key){prefs.edit().remove(P+key+"_x").remove(P+key+"_y").apply();apply();}
    public void preview(String key,int x,int y){previewKey=key;previewX=x;previewY=y;apply();}
    public void endPreview(){previewKey=null;apply();}
    void schedule(){if(pending||closed)return;pending=true;root.post(()->{pending=false;apply();});}
    private boolean custom(){if(!"legacy".equals(HeaderLayoutPolicy.mode(prefs.getString(MODE,"legacy")))||previewKey!=null)return true;for(String key:KEYS)if(prefs.contains(P+key+"_x")||prefs.contains(P+key+"_y"))return true;return false;}
    private int[] base(View v){float x=0,y=0;for(View n=v;n!=root&&n.getParent() instanceof View;){x+=n.getLeft()-((View)n.getParent()).getScrollX();y+=n.getTop()-((View)n.getParent()).getScrollY();n=(View)n.getParent();}return new int[]{dp(x),dp(y)};}
    void apply(){
        if(closed||root.getWidth()==0)return;boolean custom=custom();boolean resized=false;
        ViewGroup.LayoutParams greeting=views[0].getLayoutParams();int gw=custom?ViewGroup.LayoutParams.WRAP_CONTENT:greetingWidth;if(greeting.width!=gw){greeting.width=gw;views[0].setLayoutParams(greeting);resized=true;}
        String mode=HeaderLayoutPolicy.mode(prefs.getString(MODE,"legacy"));boolean two="clock_date".equals(mode)||"date_clock".equals(mode);CharSequence f12=two?"EEE, d. MMMM":date12,f24=two?"EEE, d. MMMM":date24;if(!android.text.TextUtils.equals(dateClock.getFormat12Hour(),f12))dateClock.setFormat12Hour(f12);if(!android.text.TextUtils.equals(dateClock.getFormat24Hour(),f24))dateClock.setFormat24Hour(f24);if(dateClock.getMaxLines()!=(two?1:2))dateClock.setMaxLines(two?1:2);
        LinearLayout.LayoutParams date=(LinearLayout.LayoutParams)views[2].getLayoutParams();int dh=two?Math.round(26*density):dateHeight;if(date.height!=dh){date.height=dh;views[2].setLayoutParams(date);resized=true;}int dw=custom?Math.round((two?260:154)*density):dateWidth;float weight=custom?0:dateWeight;if(date.width!=dw||date.weight!=weight){date.width=dw;date.weight=weight;views[2].setLayoutParams(date);resized=true;}
        View stats=a.findViewById(R.id.stats);int cardWidth=Math.max(1,Math.min(Math.round(140*density),(stats.getWidth()-Math.round(15*density))/3));
        for(int i=4;i<7;i++){LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)views[i].getLayoutParams();int w=custom?cardWidth:0;float wt=custom?0:1;if(lp.width!=w||lp.weight!=wt){lp.width=w;lp.weight=wt;views[i].setLayoutParams(lp);resized=true;}}
        for(int i=0;i<3;i++){int visibility=shown(KEYS[i])?View.VISIBLE:View.INVISIBLE;if(views[i].getVisibility()!=visibility)views[i].setVisibility(visibility);}
        if(!weatherShown(a)&&views[3].getVisibility()==View.VISIBLE)views[3].setVisibility(View.INVISIBLE);
        a.findViewById(R.id.ampm).setVisibility(shown("clock")?View.VISIBLE:View.INVISIBLE);
        a.findViewById(R.id.clockDivider).setVisibility(custom||!shown("clock")||(!shown("date")&&!shown("weather"))?View.INVISIBLE:dividerVisibility);
        ((View)a.findViewById(R.id.leftBlock).getParent()).setElevation(custom?2*density:headerElevation);
        if(resized){schedule();return;}
        int[][] base=new int[7][2],size=new int[7][2];for(int i=0;i<7;i++){base[i]=base(views[i]);size[i]=size(KEYS[i]);}
        int[][] points=HeaderLayoutPolicy.preset(prefs.getString(MODE,"legacy"),base,size);
        for(int i=0;i<7;i++){
            String key=KEYS[i];int x=prefs.getInt(P+key+"_x",points[i][0]),y=prefs.getInt(P+key+"_y",points[i][1]);if(key.equals(previewKey)){x=previewX;y=previewY;}
            if(custom){x=HeaderLayoutPolicy.clamp(x,width(),size[i][0]);y=HeaderLayoutPolicy.clamp(y,height(),size[i][1]);}
            views[i].setTranslationX((x-base[i][0])*density);views[i].setTranslationY((y-base[i][1])*density);
        }
        View ampm=a.findViewById(R.id.ampm);ampm.setTranslationX(views[1].getTranslationX());ampm.setTranslationY(views[1].getTranslationY());
    }
    void close(){closed=true;prefs.unregisterOnSharedPreferenceChangeListener(listener);if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnGlobalLayoutListener(layout);}
}
