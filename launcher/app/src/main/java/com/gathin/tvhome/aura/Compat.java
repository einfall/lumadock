package com.gathin.tvhome.aura;
import android.content.Context;
import android.net.*;
final class Compat {
    static int mod(int n,int size){return Math.floorMod(n,size);}
    static <T>T svc(Context c,Class<T> type){return c.getSystemService(type);}
    static boolean online(ConnectivityManager cm){if(cm==null)return false;NetworkCapabilities n=cm.getNetworkCapabilities(cm.getActiveNetwork());return n!=null&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}
}
