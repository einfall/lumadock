package com.gathin.tvhome;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.*;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
/** Standard installed appfilter.xml packs. Resource access only, no package code or remote downloads. */
final class IconPacks {
    static final String KEY="appearance_icon_pack";
    static final String[] ACTIONS={"org.adw.launcher.THEMES","com.novalauncher.THEME","ch.deletescape.lawnchair.ICONPACK","app.lawnchair.icons.THEMED_ICON"};
    static final class Info{final String pkg,label;Info(String p,String l){pkg=p;label=l;}}
    static List<Info> installed(Context c){
        PackageManager pm=c.getPackageManager();Map<String,Info> found=new TreeMap<>();List<ResolveInfo> handlers=new ArrayList<>();
        for(String action:ACTIONS)handlers.addAll(pm.queryIntentActivities(new Intent(action),0));
        handlers.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME"),0));
        for(ResolveInfo r:handlers)if(r.activityInfo!=null){String pkg=r.activityInfo.packageName;found.put(pkg,new Info(pkg,Cards.cleanLabel(String.valueOf(r.loadLabel(pm)),pkg)));}
        List<Info> list=new ArrayList<>(found.values());list.sort((a,b)->a.label.compareToIgnoreCase(b.label));return list;
    }
    static String selected(Context c){return c.getSharedPreferences(Config.SETTINGS_NAME,0).getString(KEY,"");}
    static final class Pack{
        final String pkg;final long version;final Resources res;final Map<String,String> items=new HashMap<>(),packages=new HashMap<>();
        Pack(String p,long v,Resources r){pkg=p;version=v;res=r;}
        Drawable icon(ComponentName component){
            String name=items.get(IconPackPolicy.component(component.flattenToString()));if(name==null)name=packages.get(component.getPackageName());if(name==null)return null;
            int id=res.getIdentifier(name,"drawable",pkg);if(id==0)id=res.getIdentifier(name,"mipmap",pkg);if(id==0)return null;
            try{
                TypedValue value=new TypedValue();res.getValue(id,value,true);
                if(value.string!=null&&!value.string.toString().endsWith(".xml")){
                    BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeResource(res,id,o);if(o.outWidth<1||o.outHeight<1)return null;
                    o.inSampleSize=1;while(o.outWidth/o.inSampleSize>384||o.outHeight/o.inSampleSize>384)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inScaled=false;
                    Bitmap bitmap=BitmapFactory.decodeResource(res,id,o);return bitmap==null?null:new BitmapDrawable(res,bitmap);
                }
                return res.getDrawable(id,null);
            }catch(RuntimeException|OutOfMemoryError ignored){return null;}
        }
    }
    private static Pack cached;
    static Pack selectedPack(Context c){return load(c,selected(c));}
    static synchronized Pack load(Context c,String pkg){
        if(pkg==null||pkg.isEmpty())return null;
        try{
            PackageInfo info=c.getPackageManager().getPackageInfo(pkg,0);if(cached!=null&&cached.pkg.equals(pkg)&&cached.version==info.lastUpdateTime)return cached;
            Resources res=c.getPackageManager().getResourcesForApplication(pkg);Pack result=new Pack(pkg,info.lastUpdateTime,res);int id=res.getIdentifier("appfilter","xml",pkg);
            if(id!=0){try(XmlResourceParser parser=res.getXml(id)){read(result,parser);}}
            else {Context other=c.createPackageContext(pkg,0);try(InputStream raw=other.getAssets().open("appfilter.xml");InputStream in=new Limited(raw)){XmlPullParser parser=Xml.newPullParser();parser.setInput(in,"UTF-8");read(result,parser);}}
            if(result.items.isEmpty())return null;cached=result;return result;
        }catch(Exception|OutOfMemoryError ignored){return null;}
    }
    private static void read(Pack pack,XmlPullParser parser)throws Exception{
        int events=0;for(int type=parser.getEventType();type!=XmlPullParser.END_DOCUMENT;type=parser.next()){
            if(++events>IconPackPolicy.MAX_EVENTS||Thread.currentThread().isInterrupted())throw new IOException("Pack limit");
            if(type!=XmlPullParser.START_TAG||!"item".equals(parser.getName()))continue;
            String component=IconPackPolicy.component(parser.getAttributeValue(null,"component")),drawable=parser.getAttributeValue(null,"drawable");
            if(component.isEmpty()||!IconPackPolicy.drawable(drawable))continue;
            pack.items.put(component,drawable);pack.packages.putIfAbsent(component.substring(0,component.indexOf('/')),drawable);if(pack.items.size()>=IconPackPolicy.MAX_ITEMS)break;
        }
    }
    private static final class Limited extends FilterInputStream{
        int remaining=IconPackPolicy.MAX_BYTES;Limited(InputStream in){super(in);}
        @Override public int read()throws IOException{if(remaining<=0)throw new IOException("Pack too large");int n=in.read();if(n>=0)remaining--;return n;}
        @Override public int read(byte[] b,int off,int len)throws IOException{if(remaining<=0)throw new IOException("Pack too large");int n=in.read(b,off,Math.min(remaining,len));if(n>0)remaining-=n;return n;}
    }
}
