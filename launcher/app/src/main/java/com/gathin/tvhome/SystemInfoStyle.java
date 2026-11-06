package com.gathin.tvhome;
final class SystemInfoStyle {
    static final String[] IDS={"original","filled","resources","compact","pastel","ring_neon","ring_donut"};
    static final String[] NAMES={"Bisherige Ansicht","Gefüllte Zeilen","Systemressourcen","Kompakte Liste","Pastellkarten","Ringe · Leuchtend","Ringe · Donut"};
    static boolean ring(String value){return "ring_neon".equals(value)||"ring_donut".equals(value);}
    static String id(String value){for(String id:IDS)if(id.equals(value))return id;return "original";}
}
