package com.gathin.tvhome;
import java.util.*;
import java.util.regex.*;
/** Parse actual nonempty recent tasks. Never treat every background system process as an app. */
final class RecentAppsPolicy {
    static final String ENABLED="recent_apps_long_back";
    static boolean safe(String pkg){return pkg!=null&&pkg.matches("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+")&&!pkg.equals("com.gathin.tvhome")&&!pkg.startsWith("com.android.")&&!pkg.startsWith("android.")&&!pkg.startsWith("org.droidtv.")&&!pkg.startsWith("com.mediatek.")&&!pkg.startsWith("com.google.android.gms")&&!pkg.startsWith("com.google.android.gsf")&&!pkg.startsWith("com.google.android.tvlauncher")&&!pkg.startsWith("com.google.android.tungsten.");}
    static List<String> packages(String dump,int user){LinkedHashSet<String> result=new LinkedHashSet<>();for(String block:dump.split("(?m)^\\s*\\* Recent #")){if(!block.contains("userId="+user+" ")||(block.contains("Activities=[]")&&!block.contains("mRootProcess=ProcessRecord{")))continue;Matcher m=Pattern.compile("mActivityComponent=([A-Za-z][A-Za-z0-9_.]+)/").matcher(block);if(m.find()&&safe(m.group(1)))result.add(m.group(1));}return new ArrayList<>(result);}
    static String stopCommand(String pkg){if(!safe(pkg))throw new IllegalArgumentException("Protected or invalid package");return "am force-stop --user current "+pkg;}
}
