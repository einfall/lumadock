package com.gathin.tvhome;
import android.content.*;
import org.json.*;
import java.util.*;
/** Versioned, local instances. Source IDs are stable; no executable templates or remote widget code. */
final class HomeWidgetStore {
    static final String P="home_widgets_",ITEMS=P+"items",SUMMARY="summary";static final int LIMIT=12;
    static final class Spec {
        String id,source,theme="inherit",title="",provider="",quoteMode="text",quoteEffect="fade",quoteFont="default",quoteEnterPath="widget",quoteExitPath="widget";boolean quoteEnterFront=false,quoteExitFront=false,quoteSurface=true;int quoteOpacity=100;int quoteSize=23;int x=56,y=210,width=160,height=140,appWidgetId;boolean enabled=true;
        Spec(String id,String source){this.id=id;this.source=source;}
        boolean quote(){return "quote".equals(source);}
        boolean android(){return "android".equals(source);}
    }
    final SharedPreferences prefs;
    HomeWidgetStore(Context c){prefs=c.getSharedPreferences(Config.SETTINGS_NAME,0);}
    List<Spec> all(){List<Spec> result=new ArrayList<>();Set<String> seen=new HashSet<>();try{JSONArray array=new JSONArray(prefs.getString(ITEMS,"[]"));for(int i=0;i<array.length()&&result.size()<LIMIT;i++){
        JSONObject o=array.optJSONObject(i);if(o==null)continue;String id=o.optString("id",""),source=o.optString("source","");if(id.isEmpty()||SUMMARY.equals(id)||!seen.add(id)||(WidgetSource.find(source)==null&&!"android".equals(source)))continue;
        Spec s=new Spec(id,source);s.theme=o.optString("theme","inherit");if(!"inherit".equals(s.theme))s.theme=SurfaceStyle.id(s.theme);s.title=o.optString("title","");if(s.title.length()>48)s.title=s.title.substring(0,48);
        s.quoteMode="image".equals(o.optString("quoteMode"))?"image":"text";s.quoteEffect=o.optString("quoteEffect","fade");if(!Arrays.asList("fade","pixels","none").contains(s.quoteEffect))s.quoteEffect="fade";s.quoteFont=FontPolicy.id(o.optString("quoteFont","default"));s.quoteSize=Math.max(12,Math.min(48,o.optInt("quoteSize",23)));
        s.quoteEnterPath=QuoteMotion.path(o.optString("quoteEnterPath"));s.quoteExitPath=QuoteMotion.path(o.optString("quoteExitPath"));s.quoteEnterFront=o.optBoolean("quoteEnterFront",false);s.quoteExitFront=o.optBoolean("quoteExitFront",false);s.quoteSurface=o.optBoolean("quoteSurface",true);s.quoteOpacity=Math.max(0,Math.min(100,o.optInt("quoteOpacity",100)));
        s.enabled=o.optBoolean("enabled",true);s.x=Math.max(0,o.optInt("x",56));s.y=Math.max(0,o.optInt("y",210));s.width=WidgetSource.width(source,o.optInt("width","system".equals(source)?430:160),960);s.height=Math.max(48,Math.min(1440,o.optInt("height",140)));s.appWidgetId=Math.max(0,o.optInt("appWidgetId",0));s.provider=o.optString("provider","");if(s.android()&&s.appWidgetId==0)continue;result.add(s);
    }}catch(JSONException ignored){}return result;}
    Spec get(String id){for(Spec s:all())if(id.equals(s.id))return s;return null;}
    boolean full(){return all().size()>=LIMIT;}
    Spec add(String source){if(WidgetSource.find(source)==null||full())return null;Spec s=new Spec(java.util.UUID.randomUUID().toString(),source);s.width="system".equals(source)||s.quote()?430:160;s.y=210+all().size()%4*75;if(s.quote()){s.y=200;s.height=150;}List<Spec> list=all();list.add(s);save(list);return s;}
    Spec addAndroid(int appWidgetId,String provider,String title,int width,int height){if(full())return null;Spec s=new Spec(java.util.UUID.randomUUID().toString(),"android");s.appWidgetId=appWidgetId;s.provider=provider;s.title=title;s.width=width;s.height=height;List<Spec> list=all();list.add(s);save(list);return s;}
    void edit(String id,java.util.function.Consumer<Spec> change){List<Spec> list=all();for(Spec s:list)if(id.equals(s.id))change.accept(s);save(list);}
    void remove(String id){List<Spec> list=all();list.removeIf(s->id.equals(s.id));save(list);}
    private void save(List<Spec> list){JSONArray array=new JSONArray();for(Spec s:list)try{JSONObject o=new JSONObject();o.put("id",s.id).put("source",s.source).put("theme",s.theme).put("title",s.title).put("enabled",s.enabled).put("x",s.x).put("y",s.y).put("width",s.width).put("height",s.height).put("appWidgetId",s.appWidgetId).put("provider",s.provider).put("quoteMode",s.quoteMode).put("quoteEffect",s.quoteEffect).put("quoteFont",s.quoteFont).put("quoteSize",s.quoteSize).put("quoteEnterPath",s.quoteEnterPath).put("quoteExitPath",s.quoteExitPath).put("quoteEnterFront",s.quoteEnterFront).put("quoteExitFront",s.quoteExitFront).put("quoteSurface",s.quoteSurface).put("quoteOpacity",s.quoteOpacity);array.put(o);}catch(JSONException ignored){}prefs.edit().putString(ITEMS,array.toString()).apply();}
}
