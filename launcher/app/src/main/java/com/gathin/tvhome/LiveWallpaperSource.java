package com.gathin.tvhome;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
/** Only media locations are accepted: never an Intent, JavaScript or an arbitrary app command. */
final class LiveWallpaperSource {
    static String clean(String s){return s==null?"":s.trim();}
    static boolean valid(String value){
        String s=clean(value);if(s.isEmpty()||s.length()>8192)return false;
        for(int i=0;i<s.length();i++)if(Character.isISOControl(s.charAt(i)))return false;
        if(s.startsWith("/"))return true;
        try{URI u=new URI(s);String scheme=u.getScheme();if(scheme==null)return false;switch(scheme.toLowerCase(Locale.ROOT)){
            case "http":case "https":return u.getHost()!=null&&!u.getHost().isEmpty()&&u.getPort()<=65535;
            case "content":return u.getAuthority()!=null&&!u.getAuthority().isEmpty();
            case "file":return (u.getHost()==null||"localhost".equalsIgnoreCase(u.getHost()))&&u.getPath()!=null&&u.getPath().startsWith("/");
            default:return false;
        }}catch(URISyntaxException e){return false;}
    }
    static boolean network(String source){String s=clean(source).toLowerCase(Locale.ROOT);return s.startsWith("https://")||s.startsWith("http://");}
    static int dim(int n){return Math.max(0,Math.min(90,n));}
    static float[] cover(float viewW,float viewH,float videoW,float videoH){
        if(viewW<=0||viewH<=0||videoW<=0||videoH<=0)return new float[]{1,1};float ratio=(videoW/videoH)/(viewW/viewH);return ratio>1?new float[]{ratio,1}:new float[]{1,1/ratio};
    }
}
