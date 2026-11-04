package com.gathin.tvhome.aura;
import android.content.*;
/** Scene caches are isolated from Home and from the user's saver selection. */
final class Prefs {
    static SharedPreferences device(Context c){return c.getSharedPreferences("aura_screensaver_cache",0);}
}
