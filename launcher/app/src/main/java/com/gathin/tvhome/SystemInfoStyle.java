package com.gathin.tvhome;
final class SystemInfoStyle {
    static final String[] IDS={"original","filled","resources","compact","pastel"};
    static final String[] NAMES={"Bisherige Ansicht","Gefüllte Zeilen","Systemressourcen","Kompakte Liste","Pastellkarten"};
    static String id(String value){for(String id:IDS)if(id.equals(value))return id;return "original";}
}
