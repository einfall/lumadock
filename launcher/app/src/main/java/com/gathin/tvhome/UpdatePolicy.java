package com.gathin.tvhome;
import java.net.URI;
import java.util.*;
/** Public update origins are fixed. A release cannot redirect downloads to arbitrary hosts. */
final class UpdatePolicy {
 static final String REPO="https://github.com/einfall/lumadock", MANIFEST=REPO+"/releases/latest/download/update.json";
 static final long MAX_APK=128L*1024*1024, CHECK_MS=24L*60*60*1000;
 static boolean transport(String raw){try{URI u=new URI(raw);String h=u.getHost();return "https".equalsIgnoreCase(u.getScheme())&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&h!=null&&Arrays.asList("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","github-releases.githubusercontent.com").contains(h.toLowerCase(Locale.ROOT));}catch(Exception e){return false;}}
 static boolean artifact(String raw){try{URI u=new URI(raw);return transport(raw)&&"github.com".equalsIgnoreCase(u.getHost())&&u.getPath().startsWith("/einfall/lumadock/releases/download/")&&u.getPath().endsWith(".apk")&&!u.getPath().contains("..");}catch(Exception e){return false;}}
 static boolean hash(String value){return value!=null&&value.matches("[a-fA-F0-9]{64}");}
 static boolean compatible(String installed,String pkg,int sdk,int minimum,long current,long offered){return installed!=null&&installed.equals(pkg)&&minimum>=26&&minimum<=sdk&&offered>=current&&offered>0;}
 static boolean due(long now,long previous){return previous<=0||now<previous||now-previous>=CHECK_MS;}
 static boolean sameSigners(Set<String> current,Set<String> offered){return current!=null&&!current.isEmpty()&&current.equals(offered);}
}
