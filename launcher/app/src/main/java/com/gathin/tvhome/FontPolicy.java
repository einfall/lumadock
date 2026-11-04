package com.gathin.tvhome;
/** Stable persisted IDs; unknown or removed choices keep the original layout typeface. */
final class FontPolicy {
    static final String[] IDS={"default","outfit","inter","sans","condensed","serif","mono","cursive","inter_thin","inter_light","inter_bold","big_shoulders","abril","oregano","kenia","dseg","monoton","dm_sans","roboto_medium","pixelify_sans"};
    static String id(String value){for(String id:IDS)if(id.equals(value))return id;return "default";}
    static boolean forAppNames(String id){return !"dseg".equals(id);}
}
