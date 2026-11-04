package com.gathin.tvhome.aura;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
/** Small boundary between the adapted Aura scenes and the existing Home saver. */
public final class AuraScenes {
    public interface Caption {void show(String title,String credit);}
    public interface ImageLoader {Bitmap load(Context c,String source,boolean asset,int w,int h)throws Exception;}
    public static final class Handle {
        private final Scene scene;private boolean stopped;
        Handle(Scene s){scene=s;}
        public View view(){return scene;}
        public boolean ownClock(){return scene.ownClock();}
        public void start(){scene.post(()->{if(!stopped&&scene.isAttachedToWindow())scene.start();});}
        public void stop(){if(stopped)return;stopped=true;scene.disposed=true;scene.stop();}
    }
    public static void place(double lat,double lon,String city,boolean cloudy){Sun.place(lat,lon);Weather.place(city,cloudy);}
    public static double sunElevation(long time){return Sun.elevation(time);}
    public static boolean available(Context c,String mode){switch(mode){case "museum":return Museum.available(c);case "wind":return Wind.available(c);case "sky":return Sky.available(c);default:return true;}}
    public static Handle create(Context c,String mode,ImageLoader loader,String image,Caption caption,Runnable error){
        Ui.init(c);Scene s;
        switch(mode){case "museum":s=new Museum(c);break;case "wind":s=new Wind(c);break;case "sky":s=new Sky(c);break;case "mosaic":s=new Mosaic(c,loader,image);break;default:throw new IllegalArgumentException("Unknown scene");}
        s.setListener(new Scene.Listener(){public void onStory(String where,String title,String line,String credit,long ms){caption.show(title,credit);}public void onError(){error.run();}});
        return new Handle(s);
    }
    private AuraScenes() { }
}
