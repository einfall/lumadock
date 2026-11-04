package com.gathin.tvhome;
import android.content.*;
import android.graphics.Typeface;
import android.os.Build;
import android.widget.TextView;
import java.util.*;
/** Independent font scopes. No downloads; uses bundled OFL fonts and Android's own families. */
public final class Typography {
    static final String APPS="typography_apps",HOME_CLOCK="typography_home_clock",SAVER_CLOCK="typography_saver_clock";
    private static final Map<String,Typeface> CACHE=new HashMap<>();
    static String selected(Context c,String scope){return FontPolicy.id(c.getSharedPreferences(Config.SETTINGS_NAME,0).getString(scope,"default"));}
    static String name(Context c,String id){String[] names=c.getResources().getStringArray(R.array.typography_fonts);for(int i=0;i<FontPolicy.IDS.length;i++)if(FontPolicy.IDS[i].equals(id))return names[i];return names[0];}
    static synchronized Typeface face(Context c,String id,int original){
        id=FontPolicy.id(id);if("default".equals(id))return UiFont.get(c,original);
        int weight=original==R.string.font_extralight?200:original==R.string.font_light?300:original==R.string.font_medium?500:400;
        String key=id+weight;Typeface found=CACHE.get(key);if(found!=null)return found;
        try{
            if("outfit".equals(id)){Typeface base=c.getResources().getFont(R.font.outfit);found=Build.VERSION.SDK_INT>=28?Typeface.create(base,weight,false):base;}
            else if(id.startsWith("inter")){int w="inter_thin".equals(id)?100:"inter_light".equals(id)?300:"inter_bold".equals(id)?700:weight;found=new Typeface.Builder(c.getAssets(),"aura/fonts/Inter.ttf").setWeight(w).setFontVariationSettings("'wght' "+w+", 'opsz' 32").build();}
            else if("pixelify_sans".equals(id))found=new Typeface.Builder(c.getAssets(),"reference_fonts/pixelify_sans.ttf").setFontVariationSettings("'wght' 600").setWeight(600).build();
            else if("dm_sans".equals(id))found=new Typeface.Builder(c.getAssets(),"reference_fonts/dm_sans.ttf").setFontVariationSettings("'wght' "+weight+", 'opsz' 9").setWeight(weight).build();
            else if(java.util.Arrays.asList("big_shoulders","abril","oregano","kenia","dseg","monoton").contains(id))found=new Typeface.Builder(c.getAssets(),"reference_fonts/"+id+".ttf").setWeight("big_shoulders".equals(id)?500:weight).build();
            else if("roboto_medium".equals(id))found=Typeface.create("sans-serif-medium",Typeface.NORMAL);
            else {String family="serif".equals(id)?"serif":"mono".equals(id)?"monospace":"condensed".equals(id)?"sans-serif-condensed":"cursive".equals(id)?"cursive":"sans-serif";Typeface base=Typeface.create(family,Typeface.NORMAL);found=Build.VERSION.SDK_INT>=28?Typeface.create(base,weight,false):base;}
        }catch(RuntimeException e){found=UiFont.get(c,original);}
        if(found==null)found=UiFont.get(c,original);CACHE.put(key,found);return found;
    }
    static void apply(TextView view,String scope,int original){view.setTypeface(face(view.getContext(),selected(view.getContext(),scope),original));}
    public static Typeface saverClock(Context c,Typeface original){String id=selected(c,SAVER_CLOCK);return "default".equals(id)?original:face(c,id,R.string.font_extralight);}
}
