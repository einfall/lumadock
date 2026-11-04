package com.gathin.tvhome;
import android.content.*;
final class SaverPrefs {
    static final String NAME="screensaver_background",MODE="mode",PHOTO="photo",IMAGE="image",VIDEO="video",TITLE="video_title",DIM="dim",MINUTES="minutes",RANDOM="random";
    static SharedPreferences get(Context c){return c.getSharedPreferences(NAME,0);}
    static String mode(Context c){return SaverPolicy.mode(get(c).getString(MODE,"legacy"));}
    static void photo(Context c,String file){if(SaverPolicy.photoId(file))get(c).edit().putString(PHOTO,file).putString(MODE,"photos").apply();}
    static void video(Context c,String source,String title){if(LiveWallpaperSource.valid(source))get(c).edit().putString(VIDEO,source).putString(TITLE,title).putString(MODE,"video").apply();}
    static void image(Context c,String source){if(LiveWallpaperSource.valid(source))get(c).edit().putString(IMAGE,source).putString(MODE,"image").apply();}
}
