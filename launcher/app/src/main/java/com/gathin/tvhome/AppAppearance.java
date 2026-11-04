package com.gathin.tvhome;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import org.json.JSONObject;
import java.util.*;
/** Staged app choices; Apply persists them together with the Design settings. */
final class AppAppearance {
    static final String LABELS="custom_app_labels",ICONS="custom_app_icons";
    static Map<String,String> map(SharedPreferences p,String key){
        Map<String,String> m=new HashMap<>();try{JSONObject j=new JSONObject(p.getString(key,"{}"));Iterator<String> it=j.keys();while(it.hasNext()){String k=it.next();m.put(k,j.getString(k));}}catch(org.json.JSONException ignored){}return m;
    }
    static String label(Context c,String pkg,String fallback){String s=map(c.getSharedPreferences(Config.SETTINGS_NAME,0),LABELS).get(pkg);return s==null||s.trim().isEmpty()?fallback:Cards.cleanLabel(s,pkg);}
    static Drawable icon(Context c,String pkg){String id=map(c.getSharedPreferences(Config.SETTINGS_NAME,0),ICONS).get(pkg);return icon(c,pkg,id);}
    static Drawable icon(Context c,ComponentName component){String id=map(c.getSharedPreferences(Config.SETTINGS_NAME,0),ICONS).get(component.getPackageName());if(id==null||id.equals("original")){IconPacks.Pack pack=IconPacks.selectedPack(c);return pack==null?null:pack.icon(component);}return icon(c,component.getPackageName(),id);}
    static Drawable icon(Context c,String pkg,String id){
        if(id==null||id.equals("original")){IconPacks.Pack pack=IconPacks.selectedPack(c);return pack==null?null:pack.icon(new ComponentName(pkg,pkg+".MainActivity"));}
        if(id.equals("app")){try{return c.getPackageManager().getApplicationIcon(pkg);}catch(android.content.pm.PackageManager.NameNotFoundException e){return null;}}
        if(!Arrays.asList("video","music","game","tv","star","kids","photo").contains(id))return null;
        return new AppGlyph(id);
    }
    static final class Item {
        final ResolveInfo info;final String pkg,key,original;
        Item(Activity a,ResolveInfo r){info=r;pkg=r.activityInfo.packageName;key=new ComponentName(pkg,r.activityInfo.name).flattenToShortString();original=Cards.cleanLabel(String.valueOf(r.loadLabel(a.getPackageManager())),pkg);}
    }
    final Activity activity;final Config config;final Set<String> hidden,locked;final Map<String,String> labels,icons;final List<Item> items=new ArrayList<>();
    private final Set<String> oldHidden,oldLocked;private final Map<String,String> oldLabels,oldIcons;private final List<String> oldOrder=new ArrayList<>();
    AppAppearance(Activity a,Config cfg){
        activity=a;config=cfg;SharedPreferences p=a.getSharedPreferences(Config.SETTINGS_NAME,0);hidden=new HashSet<>(cfg.hiddenApps);hidden.addAll(LibraryApps.loadRemoved(a));locked=new HashSet<>(p.getStringSet(AppLock.LOCKS,Collections.emptySet()));labels=map(p,LABELS);icons=map(p,ICONS);
        oldHidden=new HashSet<>(hidden);oldLocked=new HashSet<>(locked);oldLabels=new HashMap<>(labels);oldIcons=new HashMap<>(icons);
        Set<String> seen=new HashSet<>();List<ResolveInfo> found=new ArrayList<>(a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0));
        Set<String> added=new HashSet<>(LibraryApps.load(a));added.addAll(cfg.libraryApps);
        for(ResolveInfo r:a.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0))if(added.contains(r.activityInfo.packageName))found.add(r);
        for(ResolveInfo r:found)if(r.activityInfo!=null&&r.activityInfo.exported&&!r.activityInfo.packageName.equals(a.getPackageName())&&seen.add(r.activityInfo.packageName))items.add(new Item(a,r));
        Map<String,Integer> ranks=Order.ranks(Order.load(Order.file(a)),cfg.dockApps);
        items.sort((x,y)->{int n=Order.compare(ranks,x.key,y.key);if(n!=0)return n;int f=cfg.dockApps.indexOf(x.pkg),g=cfg.dockApps.indexOf(y.pkg);if(f>=0||g>=0)return f<0?1:g<0?-1:Integer.compare(f,g);return name(x).compareToIgnoreCase(name(y));});
        for(Item i:items)oldOrder.add(i.key);
    }
    String name(Item i){return labels.getOrDefault(i.pkg,i.original);}
    List<String> order(){List<String> r=new ArrayList<>();for(Item i:items)r.add(i.key);return r;}
    boolean changed(){return !hidden.equals(oldHidden)||!locked.equals(oldLocked)||!labels.equals(oldLabels)||!icons.equals(oldIcons)||!order().equals(oldOrder);}
    boolean saveOrder(int dock){
        for(Item i:items)if(hidden.contains(i.pkg)!=oldHidden.contains(i.pkg)){
            if(!LibraryApps.save(activity,i.pkg,!hidden.contains(i.pkg)))return false;
        }
        oldHidden.clear();oldHidden.addAll(hidden);
        if(order().equals(oldOrder))return true;
        List<String> visible=new ArrayList<>(),other=new ArrayList<>();for(Item i:items)(hidden.contains(i.pkg)?other:visible).add(i.key);
        int docked=Math.min(dock,visible.size());visible.addAll(other);boolean saved=Order.save(Order.file(activity),new Order(Order.dockKey(config.dockApps),visible,docked));if(saved){oldOrder.clear();oldOrder.addAll(order());activity.getSharedPreferences(Config.SETTINGS_NAME,0).edit().putString("dock_sort","custom").apply();}return saved;
    }
    void save(SharedPreferences.Editor e){e.putStringSet(Config.DESIGN_HIDDEN_KEY,new HashSet<>(hidden)).putStringSet(AppLock.LOCKS,new HashSet<>(locked)).putString(LABELS,new JSONObject(labels).toString()).putString(ICONS,new JSONObject(icons).toString());}
}
