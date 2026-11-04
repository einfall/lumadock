package com.gathin.tvhome.aura;
import android.content.Context;
/** Receives the already configured OpenWeather city/cache; never geolocates or fetches a key. */
final class Weather {
    static final class Now {String city="";int code;}
    private static volatile Now current=new Now();
    static Now now(Context c){return current;}
    static void place(String city,boolean cloudy){Now n=new Now();n.city=city==null?"":city;n.code=cloudy?2:0;current=n;}
}
