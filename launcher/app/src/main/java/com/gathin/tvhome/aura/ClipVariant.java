package com.gathin.tvhome.aura;
import java.net.URI;
import java.util.Locale;
/** Only an actual VP9 derivative qualifies; an arbitrary .webm original may contain AV1. */
final class ClipVariant {
    static int rank(String type,String source,int width,int height,long bandwidth,double duration,long room){
        String t=type==null?"":type.toLowerCase(Locale.ROOT);
        if(!t.startsWith("video/webm")||!t.contains("vp9")||t.contains("av01")||width<=0||height<=0||width>1920||height>1080||room<=0)return 0;
        if(!Double.isFinite(duration)||duration<=0||duration>3600||bandwidth<0)return 0;
        if(bandwidth>0&&bandwidth/8.0*duration*1.25>room)return 0;
        try{URI u=new URI(source);if(!"https".equals(u.getScheme())||!"upload.wikimedia.org".equals(u.getHost())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return 0;}
        catch(Exception e){return 0;}
        return width*height;
    }
}
