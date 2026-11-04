package com.gathin.tvhome;
/** Shared material tokens. Original preserves each component's existing appearance. */
final class SurfaceStyle {
    static final String[] IDS={"original","glass","card","ice","graphite","outline","light","aurora"};
    final int top,bottom,rim,ink,muted,focus,focusInk;final int radius;final boolean sheen;
    SurfaceStyle(int top,int bottom,int rim,int ink,int muted,int focus,int focusInk,int radius,boolean sheen){this.top=top;this.bottom=bottom;this.rim=rim;this.ink=ink;this.muted=muted;this.focus=focus;this.focusInk=focusInk;this.radius=radius;this.sheen=sheen;}
    static String id(String value){for(String id:IDS)if(id.equals(value))return id;return "original";}
    static SurfaceStyle of(String value,boolean button){switch(id(value)){
        case "ice":return new SurfaceStyle(0x66DBEDFF,0x283B668E,0x77D6E8FF,0xFFF5FAFF,0xFFD0E1F2,0xFFF0F8FF,0xFF11283C,16,true);
        case "graphite":return new SurfaceStyle(0xF21B212B,0xF20E131A,0x6649586B,0xFFF4F7FC,0xFFADBBD0,0xFFE4EEFB,0xFF101928,12,false);
        case "outline":return new SurfaceStyle(0x100A101A,0x080A101A,0xB3D5E6FF,0xFFFFFFFF,0xFFD6E1F0,0xFFF0F5FC,0xFF152339,14,false);
        case "light":return new SurfaceStyle(0xF2F5F8FD,0xE6DFE7F1,0x99FFFFFF,0xFF101928,0xFF40536D,0xFF80B8FF,0xFF071324,14,true);
        case "aurora":return new SurfaceStyle(0xC43B2B66,0xC40B2B45,0x886AC7FF,0xFFF5F7FF,0xFFCDD4F4,0xFFD9EAFF,0xFF142039,18,true);
        case "card":return card();
        case "glass":return glass();
        default:return button?glass():card();
    }}
    private static SurfaceStyle glass(){return new SurfaceStyle(0x2EFFFFFF,0x14FFFFFF,0x38FFFFFF,0xFFFFFFFF,0xFFBCC8DA,0xF5FFFFFF,0xFF111111,21,false);}
    private static SurfaceStyle card(){return new SurfaceStyle(0xB3242B35,0xB30D121B,0x2EFFFFFF,0xFFF6F8FC,0xFF99A9BF,0xF5FFFFFF,0xFF111111,10,false);}
}
