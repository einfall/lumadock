package com.gathin.tvhome;
import android.content.*;
import org.json.*;
import java.util.*;
/** Versioned, local instances. Source IDs are stable; no executable templates or remote widget code. */
final class HomeWidgetStore {
    static final String P="home_widgets_",ITEMS=P+"items",SUMMARY="summary";static final int LIMIT=12;
    static final class Spec {
        int ringGlow=100,ringOpacity=100;int metricsMask=7;String ringAxis="vertical";int ringGap=10;String metricsStyle="original";boolean network=false,metricsGlass=true;int metricsOpacity=45,metricsHeight=0;
        String textContent="Hallo du",textFont="default";int textSize=18,textOpacity=100;boolean textSurface=false;String id,source,theme="inherit",title="",provider="",quoteMode="text",quoteEffect="fade",quoteFont="default",quoteEnterPath="widget",quoteExitPath="widget";boolean quoteEnterFront=false,quoteExitFront=false,quoteSurface=true;int quoteOpacity=100;int quoteSize=23;int x=56,y=210,width=160,height=140,appWidgetId;boolean enabled=true;
        EchoLayout echoLayout = EchoLayout.COMPACT;
        TimeWidgetStyle timeStyle;
        Spec(String id,String source){this.id=id;this.source=SystemMetricsPolicy.source(source);this.metricsMask=SystemMetricsPolicy.legacy(source,false);timeStyle=TimeWidgetStyle.restore("",calendar());}
        boolean text(){return "text".equals(source);}
        boolean quote(){return "quote".equals(source);}
        boolean echo(){return "echo".equals(source);}
        boolean calendar(){return WidgetSource.CALENDAR.id.equals(source);}
        boolean temporal(){return calendar()||WidgetSource.CLOCK.id.equals(source);}
        boolean system(){return WidgetSource.SYSTEM.id.equals(source);}
        boolean fixedHeight(){return android()||quote()||text()||echo()||temporal();}
        boolean android(){return "android".equals(source);}
    }
    final SharedPreferences prefs;
    HomeWidgetStore(Context c){prefs=c.getSharedPreferences(Config.SETTINGS_NAME,0);}
    List<Spec> all(){List<Spec> result=new ArrayList<>();Spec currentSystem=null;int systemPriority=-1;Set<String> seen=new HashSet<>();try{JSONArray array=new JSONArray(prefs.getString(ITEMS,"[]"));for(int i=0;i<array.length();i++){
        JSONObject o=array.optJSONObject(i);if(o==null)continue;String id=o.optString("id",""),source=o.optString("source","");if(id.isEmpty()||SUMMARY.equals(id)||!seen.add(id)||(WidgetSource.find(source)==null&&!"android".equals(source)))continue;
        Spec s=new Spec(id,source);s.theme=o.optString("theme","inherit");if(!"inherit".equals(s.theme))s.theme=SurfaceStyle.id(s.theme);s.metricsStyle=SystemInfoStyle.id(o.optString("metricsStyle","original"));s.ringGlow=RingAlphaPolicy.percent(o.optInt("ringGlow",100));s.ringOpacity=RingAlphaPolicy.percent(o.optInt("ringOpacity",100));s.ringAxis=SystemRingPolicy.axis(o.optString("ringAxis","vertical"));s.ringGap=SystemRingPolicy.gap(o.optInt("ringGap",10));s.network=o.optBoolean("network",false);s.metricsMask=SystemMetricsPolicy.restore(source,o.has("metricsMask"),o.optInt("metricsMask",7),s.network);s.network=(s.metricsMask&8)!=0;s.metricsGlass=o.optBoolean("metricsGlass",true);s.metricsOpacity=SystemWidgetPolicy.opacity(o.optInt("metricsOpacity",45));s.metricsHeight=Math.max(0,Math.min(1440,o.optInt("metricsHeight",0)));s.title=o.optString("title","");if(s.title.length()>48)s.title=s.title.substring(0,48);
        s.textContent=o.optString("textContent","Hallo du");if(s.textContent.length()>500)s.textContent=s.textContent.substring(0,500);s.textFont=FontPolicy.id(o.optString("textFont","default"));s.textSize=Math.max(8,Math.min(120,o.optInt("textSize",18)));s.textOpacity=RingAlphaPolicy.percent(o.optInt("textOpacity",100));s.textSurface=o.optBoolean("textSurface",false);
        s.quoteMode="image".equals(o.optString("quoteMode"))?"image":"text";s.quoteEffect=o.optString("quoteEffect","fade");if(!Arrays.asList("fade","pixels","none").contains(s.quoteEffect))s.quoteEffect="fade";s.quoteFont=FontPolicy.id(o.optString("quoteFont","default"));s.quoteSize=Math.max(12,Math.min(48,o.optInt("quoteSize",23)));
        s.quoteEnterPath=QuoteMotion.path(o.optString("quoteEnterPath"));s.quoteExitPath=QuoteMotion.path(o.optString("quoteExitPath"));s.quoteEnterFront=o.optBoolean("quoteEnterFront",false);s.quoteExitFront=o.optBoolean("quoteExitFront",false);s.quoteSurface=o.optBoolean("quoteSurface",true);s.quoteOpacity=Math.max(0,Math.min(100,o.optInt("quoteOpacity",100)));
        if(s.echo())s.echoLayout=EchoLayout.selected(o.optString("echoLayout"));
        if(s.temporal())s.timeStyle=TimeWidgetStyle.restore(o.optString("timeStyle"),s.calendar());
        s.enabled=o.optBoolean("enabled",true);s.x=Math.max(0,o.optInt("x",56));s.y=Math.max(0,o.optInt("y",210));s.width=WidgetSource.width(source,o.optInt("width","system".equals(source)?430:160),960);s.height=Math.max(s.text()?24:48,Math.min(1440,o.optInt("height",140)));s.appWidgetId=Math.max(0,o.optInt("appWidgetId",0));s.provider=o.optString("provider","");if(s.android()&&s.appWidgetId==0)continue;
        if(s.system()){
            int priority=SystemMetricsPolicy.retentionPriority(s.enabled,!"original".equals(s.metricsStyle),
                    WidgetSource.SYSTEM.id.equals(source),s.metricsMask);
            if(priority<systemPriority)continue;
            if(currentSystem!=null)result.remove(currentSystem);
            currentSystem=s;
            systemPriority=priority;
        }
        result.add(s);
    }}catch(JSONException ignored){}return result.size()>LIMIT?new ArrayList<>(result.subList(0,LIMIT)):result;}
    Spec get(String id){for(Spec s:all())if(id.equals(s.id))return s;return null;}
    boolean full(){return all().size()>=LIMIT;}
    Spec system(){for(Spec spec:all())if(spec.system())return spec;return null;}
    Spec add(String source){return add(source,null);}
    Spec add(String source,TimeWidgetStyle style){
        List<Spec> list=all();
        if(WidgetSource.find(source)==null||list.size()>=LIMIT)return null;
        Spec s=new Spec(java.util.UUID.randomUUID().toString(),source);
        if(s.system()&&list.stream().anyMatch(Spec::system))return null;
        s.width=s.system()||s.quote()?430:160;
        s.y=210+list.size()%4*75;
        if(s.quote()){s.y=200;s.height=150;}
        if(s.echo()){s.width=s.echoLayout.width;s.height=s.echoLayout.height;}
        if(s.text()){s.width=240;s.height=48;}
        if(s.temporal()){
            if(style!=null)s.timeStyle=TimeWidgetStyle.restore(style.id,s.calendar());
            s.width=s.timeStyle==TimeWidgetStyle.CALENDAR_CLOCK?300:220;
            s.height=132;
        }
        list.add(s);
        save(list);
        return s;
    }
    Spec addAndroid(int appWidgetId,String provider,String title,int width,int height){if(full())return null;Spec s=new Spec(java.util.UUID.randomUUID().toString(),"android");s.appWidgetId=appWidgetId;s.provider=provider;s.title=title;s.width=width;s.height=height;if(s.text()){s.width=240;s.height=48;}List<Spec> list=all();list.add(s);save(list);return s;}
    void edit(String id,java.util.function.Consumer<Spec> change){List<Spec> list=all();for(Spec s:list)if(id.equals(s.id))change.accept(s);save(list);}
    void remove(String id){List<Spec> list=all();list.removeIf(s->id.equals(s.id));save(list);}
    private void save(List<Spec> list){JSONArray array=new JSONArray();for(Spec s:list)try{JSONObject o=new JSONObject();o.put("id",s.id).put("source",s.source).put("theme",s.theme).put("title",s.title).put("enabled",s.enabled).put("x",s.x).put("y",s.y).put("width",s.width).put("height",s.height).put("appWidgetId",s.appWidgetId).put("provider",s.provider).put("quoteMode",s.quoteMode).put("quoteEffect",s.quoteEffect).put("quoteFont",s.quoteFont).put("quoteSize",s.quoteSize).put("quoteEnterPath",s.quoteEnterPath).put("quoteExitPath",s.quoteExitPath).put("quoteEnterFront",s.quoteEnterFront).put("quoteExitFront",s.quoteExitFront).put("quoteSurface",s.quoteSurface).put("quoteOpacity",s.quoteOpacity).put("metricsMask",s.metricsMask).put("metricsStyle",s.metricsStyle).put("network",s.network).put("metricsGlass",s.metricsGlass).put("metricsOpacity",s.metricsOpacity).put("metricsHeight",s.metricsHeight).put("textContent",s.textContent).put("textFont",s.textFont).put("textSize",s.textSize).put("textOpacity",s.textOpacity).put("textSurface",s.textSurface).put("ringGlow",s.ringGlow).put("ringOpacity",s.ringOpacity).put("ringAxis",s.ringAxis).put("ringGap",s.ringGap);if(s.echo())o.put("echoLayout",s.echoLayout.id);if(s.temporal())o.put("timeStyle",s.timeStyle.id);array.put(o);}catch(JSONException ignored){}prefs.edit().putString(ITEMS,array.toString()).apply();}
}
