package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Instances and placement share one sampler, including when the original three cards are disabled. */
final class HomeWidgets implements PlacementTarget {
    private final Activity a;private final ViewGroup root;final HomeWidgetStore store;final AndroidWidgetBridge android;final QuoteFeed quotes;final EchoFeed echo;private final QuoteParticleLayers particleLayers;private final SystemStatsWidget stats;private final float density;
    private final Map<String,HomeWidgetCard> cards=new LinkedHashMap<>();private SystemStatsWidget.Snapshot snapshot;private boolean closed,pending,active;private String preview;private int previewX,previewY;
    private final SystemStatsWidget.Observer observer=s->{snapshot=s;for(HomeWidgetCard card:cards.values())card.update(s);};
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if(key!=null&&(key.startsWith(HomeWidgetStore.P)||key.startsWith("system_stats_")))configure();};
    private final ViewTreeObserver.OnGlobalLayoutListener layout=()->schedule();
    HomeWidgets(Activity a,SystemStatsWidget stats,HeaderLayoutController header){this.a=a;this.stats=stats;root=a.findViewById(R.id.root);store=new HomeWidgetStore(a);android=new AndroidWidgetBridge(a,store);quotes=new QuoteFeed(a,state->{for(HomeWidgetCard c:cards.values())c.quote(state);});echo=new EchoFeed(a,state->{for(HomeWidgetCard c:cards.values())c.echo(state);});density=a.getResources().getDisplayMetrics().density;particleLayers=new QuoteParticleLayers(root);store.prefs.registerOnSharedPreferenceChangeListener(listener);root.getViewTreeObserver().addOnGlobalLayoutListener(layout);stats.observe(observer);configure();root.post(()->migrateGreeting(header));}
    private void migrateGreeting(HeaderLayoutController header){if(closed||store.prefs.getBoolean("text_greeting_migrated",false))return;if(!header.shown("greeting")){store.prefs.edit().putBoolean("text_greeting_migrated",true).apply();return;}if(store.full())return;android.widget.TextView old=(android.widget.TextView)header.view("greeting");if(old.getWidth()==0){root.postDelayed(()->migrateGreeting(header),100);return;}int[] pos=header.position("greeting");HomeWidgetStore.Spec added=store.add("text");if(added==null)return;store.edit(added.id,s->{s.textContent=old.getText().toString();s.textSize=Math.max(8,Math.round(old.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity));s.width=Math.max(160,Math.min(480,Math.round(old.getWidth()/density)));s.height=Math.max(24,Math.round(old.getHeight()/density));s.x=pos[0];s.y=pos[1];});store.prefs.edit().putBoolean("text_greeting_migrated",true).putBoolean(HeaderLayoutController.P+"show_greeting",false).apply();}
    static String sourceName(Context a,String source){if(WidgetSource.CLOCK.id.equals(source))return "Digitaluhr";if(WidgetSource.CALENDAR.id.equals(source))return "Kalender";if("text".equals(source))return "Textwidget";if("network".equals(source))return "Netzwerk · LAN + WLAN";if("android".equals(source))return a.getString(R.string.widgets_android);if("quote".equals(source))return a.getString(R.string.quote_title);if("echo".equals(source))return "Echo · Aktuelle Wiedergabe";WidgetSource s=WidgetSource.find(source);return a.getString(s==WidgetSource.CPU?R.string.system_stats_cpu:s==WidgetSource.RAM?R.string.system_stats_ram:s==WidgetSource.STORAGE?R.string.system_stats_storage:R.string.widgets_summary);}
    static String metricNames(Context c,int mask){java.util.List<String> names=new java.util.ArrayList<>();if((mask&1)!=0)names.add(c.getString(R.string.system_stats_cpu));if((mask&2)!=0)names.add(c.getString(R.string.system_stats_ram));if((mask&4)!=0)names.add(c.getString(R.string.system_stats_storage));if((mask&8)!=0)names.add("Netzwerk");return names.isEmpty()?"Keine Messwerte":String.join(", ",names);}
    HomeWidgetStore.Spec spec(String key){return store.get(key);}
    private void configure(){if(closed)return;List<HomeWidgetStore.Spec> specs=store.all();Set<String> keep=new HashSet<>();int demand=0;boolean nativeOn=false,quoteOn=false,quoteImages=false,echoOn=false;
        for(HomeWidgetStore.Spec s:specs){keep.add(s.id);WidgetSource source=WidgetSource.find(s.source);int mask=source==null?0:source==WidgetSource.SYSTEM?s.metricsMask:source.mask;HomeWidgetCard card=cards.get(s.id);
            if(card==null||!card.source.equals(s.source)||card.mask!=mask||card.appWidgetId!=s.appWidgetId){if(card!=null){card.close();root.removeView(card);}card=new HomeWidgetCard(a,s,mask,android,particleLayers);cards.put(s.id,card);root.addView(card,new android.widget.FrameLayout.LayoutParams(1,-2));if(snapshot!=null)card.update(snapshot);}
            boolean visible=s.enabled&&(!"system".equals(s.source)||mask!=0);card.setVisibility(visible?View.VISIBLE:View.GONE);card.style(s);card.active(active&&visible);card.quote(quotes.state());card.echo(echo.state());echoOn|=visible&&s.echo();if(s.enabled&&s.quote()){quoteOn=true;quoteImages|="image".equals(s.quoteMode);}if(s.enabled){demand|=mask;nativeOn|=s.android();}
            int w=Math.round(WidgetSource.width(s.source,s.width,Math.max(1,width()))*density),h=s.fixedHeight()?Math.round(Math.min(s.height,height())*density):ViewGroup.LayoutParams.WRAP_CONTENT;ViewGroup.LayoutParams lp=card.getLayoutParams();if(lp.width!=w||lp.height!=h){lp.width=w;lp.height=h;card.setLayoutParams(lp);if(s.android())card.nativeSize(Math.round(w/density),Math.min(s.height,height()));}
        }
        for(String id:new ArrayList<>(cards.keySet()))if(!keep.contains(id)){HomeWidgetCard removed=cards.remove(id);removed.close();root.removeView(removed);}
        quotes.configure(active&&quoteOn,quoteImages);echo.visible(active&&echoOn);stats.demand(demand);if(active&&nativeOn)android.start();else android.stop();schedule();
    }
    private void schedule(){if(closed||pending)return;pending=true;root.post(()->{pending=false;apply();});}
    private void apply(){if(closed||root.getWidth()==0)return;for(String id:cards.keySet()){HomeWidgetStore.Spec s=spec(id);if(s==null)continue;HomeWidgetCard card=cards.get(id);int x=id.equals(preview)?previewX:s.x,y=id.equals(preview)?previewY:s.y;card.setTranslationX(HeaderLayoutPolicy.clamp(x,width(),Math.round(card.getWidth()/density))*density);card.setTranslationY(HeaderLayoutPolicy.clamp(y,height(),Math.round(card.getHeight()/density))*density);}}
    void refreshStyle(){for(String id:cards.keySet()){HomeWidgetStore.Spec s=spec(id);if(s!=null)cards.get(id).style(s);}}
    void show(String id,boolean on){store.edit(id,s->s.enabled=on);}
    void metrics(String id,int mask){store.edit(id,s->{s.metricsMask=SystemMetricsPolicy.normalize(mask);s.network=(s.metricsMask&8)!=0;});}
    void metricsStyle(String id,String value){store.edit(id,s->s.metricsStyle=SystemInfoStyle.id(value));}
    void ringAxis(String id,String value){store.edit(id,s->s.ringAxis=SystemRingPolicy.axis(value));}
    void ringGap(String id,int value){store.edit(id,s->s.ringGap=SystemRingPolicy.gap(value));}
    void metricsGlass(String id,boolean on){store.edit(id,s->s.metricsGlass=on);}
    void metricsOpacity(String id,int opacity){store.edit(id,s->s.metricsOpacity=SystemWidgetPolicy.opacity(opacity));}
    void automaticHeight(String id){store.edit(id,s->s.metricsHeight=0);}
    void network(String id,boolean on){store.edit(id,s->{s.network=on;s.metricsMask=on?s.metricsMask|8:s.metricsMask&~8;});}
    void theme(String id,String theme){store.edit(id,s->s.theme=theme);}
    void resize(String id,int width,int height){store.edit(id,s->{s.width=width;if(s.fixedHeight())s.height=height;else s.metricsHeight=height;});}
    void remove(String id){HomeWidgetStore.Spec s=store.get(id);if(s!=null&&s.android())android.delete(s.appWidgetId);store.remove(id);}
    void start(){active=true;configure();}void stop(){active=false;android.stop();quotes.stop();echo.visible(false);for(HomeWidgetCard c:cards.values())c.active(false);}
    void close(){closed=true;stop();stats.forget(observer);stats.demand(0);android.close();quotes.close();echo.close();store.prefs.unregisterOnSharedPreferenceChangeListener(listener);if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnGlobalLayoutListener(layout);for(HomeWidgetCard view:cards.values()){view.close();root.removeView(view);}cards.clear();particleLayers.close();}
    public View view(String key){return cards.get(key);}public int width(){return Math.round((root.getWidth()>0?root.getWidth():a.getResources().getDisplayMetrics().widthPixels)/density);}public int height(){return Math.round((root.getHeight()>0?root.getHeight():a.getResources().getDisplayMetrics().heightPixels)/density);}
    public int[] size(String key){View v=view(key);return new int[]{Math.round(v.getWidth()/density),Math.round(v.getHeight()/density)};}
    public Rect bounds(String key){View v=view(key);return new Rect(Math.round(v.getX()),Math.round(v.getY()),Math.round(v.getX()+v.getWidth()),Math.round(v.getY()+v.getHeight()));}
    public int[] position(String key){Rect r=bounds(key);return new int[]{Math.round(r.left/density),Math.round(r.top/density)};}
    public void set(String key,int x,int y){store.edit(key,s->{s.x=x;s.y=y;});}
    public void preview(String key,int x,int y){preview=key;previewX=x;previewY=y;apply();}public void endPreview(){preview=null;apply();}
    void interact(String key){HomeWidgetCard card=cards.get(key);if(card==null||card.nativeView==null)return;ViewGroup old=(ViewGroup)card.nativeView.getParent();old.removeView(card.nativeView);SettingsSheet p=new SettingsSheet(a,spec(key).title);card.nativeView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);card.nativeView.setOnTouchListener(null);p.content(card.nativeView);p.dialog.setOnDismissListener(d->{((ViewGroup)card.nativeView.getParent()).removeView(card.nativeView);card.nativeView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);card.nativeView.setOnTouchListener((v,e)->true);old.addView(card.nativeView,new LinearLayout.LayoutParams(-1,0,1));});p.show();card.nativeView.requestFocus();}
}
