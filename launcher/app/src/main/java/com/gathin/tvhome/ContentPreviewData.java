package com.gathin.tvhome;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import java.util.*;
/** Reads only searchable preview listings exposed through Android's TV provider. No service APIs or scraping. */
final class ContentPreviewData {
 static final String PERMISSION="android.permission.READ_TV_LISTINGS",ENABLED="content_preview_enabled",DISABLED="content_preview_disabled_apps",HIDDEN="content_preview_hidden_channels",Y="content_preview_y";
 static final Uri CHANNELS=Uri.parse("content://android.media.tv/channel?preview=true"),PROGRAMS=Uri.parse("content://android.media.tv/preview_program");
 static final class Program {final String pkg,title,poster,intent;final long id,channel;Program(long id,long c,String pkg,String t,String p,String i){this.id=id;channel=c;this.pkg=pkg;title=t;poster=p;intent=i;}}
 static final class Channel {final long id;final String pkg,name;final List<Program> programs=new ArrayList<>();int total;Channel(long id,String p,String n){this.id=id;pkg=p;name=n;}}
 static final class Snapshot {final List<Channel> channels=new ArrayList<>();String status="";List<Channel> app(String pkg){List<Channel> list=new ArrayList<>();for(Channel c:channels)if(c.pkg.equals(pkg))list.add(c);return list;}int programs(String pkg){int n=0;for(Channel c:app(pkg))n+=c.total;return n;}}
 static Snapshot load(Context c){Snapshot result=new Snapshot();if(c.checkSelfPermission(PERMISSION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){result.status="Zugriff auf TV-Vorschaukanäle noch nicht erlaubt";return result;}try{
  Map<Long,Channel> byId=new LinkedHashMap<>();try(Cursor r=c.getContentResolver().query(CHANNELS,new String[]{"_id","package_name","display_name","type"},null,null,null)){if(r!=null)while(r.moveToNext()&&byId.size()<128){if(!"TYPE_PREVIEW".equals(r.getString(3)))continue;String pkg=clean(r.getString(1),200);if(pkg.isEmpty())continue;Channel channel=new Channel(r.getLong(0),pkg,clean(r.getString(2),100));byId.put(channel.id,channel);}}
  int read=0;try(Cursor r=c.getContentResolver().query(PROGRAMS,new String[]{"_id","channel_id","package_name","title","poster_art_uri","intent_uri"},null,null,null)){if(r!=null)while(r.moveToNext()&&read++<2048){Channel channel=byId.get(r.getLong(1));if(channel==null||!channel.pkg.equals(r.getString(2)))continue;channel.total++;if(channel.programs.size()>=12)continue;channel.programs.add(new Program(r.getLong(0),channel.id,channel.pkg,clean(r.getString(3),120),clean(r.getString(4),2048),clean(r.getString(5),4096)));}}
  result.channels.addAll(byId.values());result.status=result.channels.isEmpty()?"Keine freigegebenen Vorschaukanäle vorhanden":result.channels.size()+" Vorschaukanäle erkannt";
 }catch(SecurityException e){result.status="Der TV gibt diese Vorschaukanäle nicht frei";}catch(RuntimeException e){result.status="TV-Vorschaukanäle momentan nicht verfügbar";}return result;}
 private static String clean(String s,int max){if(s==null)return "";s=s.replaceAll("[\\p{Cntrl}]", " ").trim();return s.length()>max?s.substring(0,max):s;}
 static Intent target(Context c,Program p){try{Intent i=Intent.parseUri(p.intent,Intent.URI_INTENT_SCHEME);ComponentName component=i.getComponent();if(component!=null&&!p.pkg.equals(component.getPackageName()))return null;if(i.getPackage()!=null&&!p.pkg.equals(i.getPackage()))return null;String action=i.getAction();if(action!=null&&!Intent.ACTION_VIEW.equals(action)&&!Intent.ACTION_MAIN.equals(action))return null;Uri data=i.getData();if(data!=null&&Arrays.asList("file","content","javascript","intent").contains(String.valueOf(data.getScheme()).toLowerCase(Locale.ROOT)))return null;i.setSelector(null);i.setClipData(null);i.setPackage(p.pkg);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(i.getAction()==null)i.setAction(Intent.ACTION_VIEW);if(c.getPackageManager().resolveActivity(i,0)==null)return null;return i;}catch(Exception e){return null;}}
 static boolean appEnabled(Context c,String pkg){return !c.getSharedPreferences(Config.SETTINGS_NAME,0).getStringSet(DISABLED,Collections.emptySet()).contains(pkg);}
}
