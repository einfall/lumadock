package com.gathin.tvhome;
import android.content.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.gathin.tvhome.aura.AuraScenes;
import com.gathin.tvhome.aura.Flyovers;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** Only the active saver owns its scene, image worker or muted native video decoder. */
final class SaverBackground {
    private final Context context;
    private final ClockScene scene;
    private final FrameLayout host;
    private final View shade;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService decoder=Executors.newSingleThreadExecutor(r->new Thread(r,"saver-image"));
    private final ExecutorService downloader=Executors.newSingleThreadExecutor(r->new Thread(r,"saver-space"));
    private final SharedPreferences prefs;
    private final List<SaverCatalog.Photo> photos;
    private final Random random=new Random();
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,k)->refresh();
    private ImageView image;
    private TextureView texture;
    private MediaPlayer player;
    private Surface surface;
    private Future<?> loading,spaceLoading;
    private AuraScenes.Handle aura;
    private File spaceFile;
    private boolean active,closed,rendered,prepared,hasPlace,cloudy;
    private int generation,videoW,videoH;
    private String opened="",requested="",effective="",failed="",autoKind;
    private long autoAt,videoFrameAt;
    private final Runnable rotate=this::advancePhoto;
    private final Runnable autoTick=()->nextAuto(true);

    SaverBackground(Context c,View root,ClockScene scene){
        context=c;this.scene=scene;prefs=SaverPrefs.get(c);photos=SaverCatalog.photos(c);
        View content=root.findViewById(R.id.dreamContent);
        FrameLayout parent=(FrameLayout)content.getParent();
        host=new FrameLayout(c);host.setBackgroundColor(0xFF000000);
        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        host.setVisibility(View.GONE);
        parent.addView(host,parent.indexOfChild(content),new FrameLayout.LayoutParams(-1,-1));
        shade=new View(c);prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    void start(){if(closed||active)return;active=true;android.util.Log.i("TvHome.Saver","start "+SaverPrefs.mode(context));refresh();}
    void reload(){
        if("image".equals(SaverPrefs.mode(context)))SaverImage.dropCache(context,prefs.getString(SaverPrefs.IMAGE,""));
        failed="";opened="";refresh();
    }
    void nextAuto(){nextAuto(true);}
    private void nextAuto(boolean fade){
        if(!active||!"auto".equals(SaverPrefs.mode(context)))return;
        host.animate().cancel();
        Runnable change=()->{
            if(!active||!"auto".equals(SaverPrefs.mode(context)))return;
            autoKind=chooseAuto();autoAt=SystemClock.uptimeMillis()+SaverAutoPolicy.INTERVAL;
            opened="";refresh();host.setAlpha(0);host.animate().alpha(1).setDuration(1200).start();
        };
        if(fade)host.animate().alpha(0).setDuration(700).withEndAction(change).start();else change.run();
    }
    void stop(){
        if(active)android.util.Log.i("TvHome.Saver","stop");
        active=false;release();host.setVisibility(View.GONE);opened="";autoKind=null;autoAt=0;
    }
    void close(){if(closed)return;closed=true;stop();prefs.unregisterOnSharedPreferenceChangeListener(listener);decoder.shutdownNow();downloader.shutdownNow();}
    private void release(){
        generation++;handler.removeCallbacksAndMessages(null);
        host.animate().withEndAction(null);host.animate().cancel();host.setAlpha(1);
        if(loading!=null){loading.cancel(true);loading=null;}
        if(spaceLoading!=null){spaceLoading.cancel(true);spaceLoading=null;}
        if(aura!=null){aura.stop();aura=null;}
        releasePlayer();host.removeAllViews();image=null;texture=null;spaceFile=null;
        scene.overlayVisible(true);
    }
    private void releasePlayer(){
        if(player!=null){
            MediaPlayer old=player;player=null;
            old.setOnPreparedListener(null);old.setOnErrorListener(null);old.setOnInfoListener(null);
            old.setOnVideoSizeChangedListener(null);old.setOnCompletionListener(null);old.release();
        }
        if(surface!=null){surface.release();surface=null;}
        rendered=false;prepared=false;
    }
    private void place(){
        SharedPreferences weather=context.getSharedPreferences(WeatherPanel.PREFS,0);
        double lat=WeatherPanel.coordinate(weather,"lat"),lon=WeatherPanel.coordinate(weather,"lon");
        hasPlace=WeatherPolicy.validCoordinates(lat,lon);cloudy=false;
        try{cloudy=new org.json.JSONObject(weather.getString("cache","")).optJSONObject("clouds").optInt("all",0)>=25;}catch(Exception ignored){}
        if(hasPlace)AuraScenes.place(lat,lon,weather.getString("city",""),cloudy);
    }
    private String chooseAuto(){
        place();SharedPreferences cache=context.getSharedPreferences("aura_screensaver_cache",0);
        int turn=cache.getInt("auto_turn",0);cache.edit().putInt("auto_turn",turn+1).apply();
        return SaverAutoPolicy.choose(turn,AuraScenes.sunElevation(System.currentTimeMillis())<-6,cloudy,
                AuraScenes.available(context,"museum"),hasPlace&&AuraScenes.available(context,"wind"),
                hasPlace&&AuraScenes.available(context,"sky"),Flyovers.supported()&&!Flyovers.ready(context).isEmpty());
    }
    private void refresh(){
        if(!active||closed)return;
        String wanted=SaverPrefs.mode(context);
        if(!wanted.equals(requested)){requested=wanted;failed="";autoKind=null;autoAt=0;}
        if("auto".equals(wanted)&&autoKind==null){autoKind=chooseAuto();autoAt=SystemClock.uptimeMillis()+SaverAutoPolicy.INTERVAL;}
        String mode="auto".equals(wanted)?autoKind:wanted;
        if(wanted.equals(failed))mode="photos";
        String source="";
        if("photos".equals(mode))source=prefs.getString(SaverPrefs.PHOTO,"w00.jpg");
        else if("image".equals(mode))source=prefs.getString(SaverPrefs.IMAGE,"");
        else if("video".equals(mode))source=prefs.getString(SaverPrefs.VIDEO,"");
        shade.setBackgroundColor(Math.round(LiveWallpaperSource.dim(prefs.getInt(SaverPrefs.DIM,45))*2.55f)<<24);
        String key=wanted+":"+mode+":"+source;
        if(key.equals(opened)){schedule();return;}
        release();opened=key;effective=mode;
        boolean custom=!"legacy".equals(mode);scene.externalBackground(custom);
        host.setVisibility(custom?View.VISIBLE:View.GONE);scene.caption("");
        android.util.Log.i("TvHome.Saver","scene "+mode);
        if(!custom||"black".equals(mode)){schedule();return;}
        if(Arrays.asList("museum","wind","sky","mosaic").contains(mode)){
            place();
            if(("wind".equals(mode)||"sky".equals(mode))&&!hasPlace){fallback(R.string.saver_aura_city_missing);return;}
            final int token=generation;
            aura=AuraScenes.create(context,mode,SaverImage::load,prefs.getString(SaverPrefs.IMAGE,""),
                    (title,credit)->{if(active&&token==generation)scene.caption(title+(credit.isEmpty()?"":" · "+credit));},
                    ()->{if(active&&token==generation)fallback(R.string.saver_aura_unavailable);});
            scene.overlayVisible(!aura.ownClock());
            host.addView(aura.view(),new FrameLayout.LayoutParams(-1,-1));
            host.addView(shade,new FrameLayout.LayoutParams(-1,-1));aura.start();schedule();return;
        }
        if("space".equals(mode)){
            scene.caption(context.getString(R.string.saver_space_loading));
            final int token=generation;
            spaceLoading=downloader.submit(()->{
                try{Flyovers.load(context,file->handler.post(()->{
                    if(!active||closed||token!=generation)return;
                    spaceFile=file;showVideo(file.getAbsolutePath(),Flyovers.caption(file));
                }));}
                catch(Exception e){handler.post(()->{if(active&&!closed&&token==generation&&spaceFile==null)fallback(R.string.saver_space_error);});}
            });
            schedule();return;
        }
        if("photos".equals(mode)||"image".equals(mode)){
            boolean asset="photos".equals(mode);
            SaverCatalog.Photo photo=asset?SaverCatalog.find(photos,source):null;
            if(asset&&photo==null){scene.caption(context.getString(R.string.saver_no_photos));return;}
            String location=asset?photo.file:source;
            if(!asset&&!LiveWallpaperSource.valid(location)){scene.caption(context.getString(R.string.saver_no_image));return;}
            image=new ImageView(context);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            host.addView(image,new FrameLayout.LayoutParams(-1,-1));host.addView(shade,new FrameLayout.LayoutParams(-1,-1));
            scene.caption(asset?photo.caption():context.getString(R.string.saver_loading_image));
            int token=generation;
            android.util.DisplayMetrics dm=context.getResources().getDisplayMetrics();
            int w=Math.min(1920,dm.widthPixels),h=Math.min(1080,dm.heightPixels);
            loading=decoder.submit(()->{
                Bitmap bitmap=null;try{bitmap=SaverImage.load(context,location,asset,w,h);}catch(Exception|OutOfMemoryError ignored){}
                Bitmap result=bitmap;
                handler.post(()->{
                    if(!active||closed||token!=generation){if(result!=null)result.recycle();return;}
                    if(result==null){scene.caption(context.getString(R.string.saver_image_error));return;}
                    image.setImageBitmap(result);android.util.Log.i("TvHome.Saver",asset?"photo-ready "+location:"image-ready");
                    if(!asset)scene.caption(context.getString(R.string.saver_own_image));
                });
            });
        }else if("video".equals(mode)){
            if(!LiveWallpaperSource.valid(source)){scene.caption(context.getString(R.string.saver_no_video));return;}
            showVideo(source,prefs.getString(SaverPrefs.TITLE,context.getString(R.string.saver_video)));
        }
        schedule();
    }
    private void fallback(int message){
        android.util.Log.w("TvHome.Saver","fallback "+effective);
        if("auto".equals(requested))autoKind="photos";else failed=requested;
        opened="";refresh();scene.caption(context.getString(message));
    }
    private void advancePhoto(){
        if(!active||!"photos".equals(effective)||photos.size()<2)return;
        String file=prefs.getString(SaverPrefs.PHOTO,"w00.jpg");int current=0;
        for(int i=0;i<photos.size();i++)if(photos.get(i).file.equals(file))current=i;
        String next=photos.get(SaverPolicy.next(photos.size(),current,prefs.getBoolean(SaverPrefs.RANDOM,false),random)).file;
        prefs.edit().putString(SaverPrefs.PHOTO,next).apply();
    }
    private void schedule(){
        handler.removeCallbacks(rotate);handler.removeCallbacks(autoTick);
        if(active&&"auto".equals(requested))handler.postDelayed(autoTick,Math.max(1,autoAt-SystemClock.uptimeMillis()));
        int minutes=SaverPolicy.minutes(prefs.getInt(SaverPrefs.MINUTES,5));
        if(active&&"photos".equals(effective)&&minutes>0&&photos.size()>1)handler.postDelayed(rotate,minutes*60000L);
    }
    private void showVideo(String source,String title){
        if(shade.getParent()==host)host.removeView(shade);
        if(texture!=null){releasePlayer();host.removeView(texture);}
        scene.caption(context.getString(R.string.saver_loading_video));
        TextureView target=new TextureView(context);texture=target;target.setOpaque(true);
        host.addView(target,new FrameLayout.LayoutParams(-1,-1));host.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        target.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture t,int w,int h){if(active&&texture==target)video(source,title,target);}
            public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){transform();}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture t){if(texture==target)releasePlayer();return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture t){
                if(texture==target)videoFrameAt=SystemClock.uptimeMillis();
                if(texture==target&&player!=null&&prepared&&!rendered){rendered=true;android.util.Log.i("TvHome.Saver","video-ready");scene.caption(title);}
            }
        });
    }
    private void video(String source,String title,TextureView target){
        releasePlayer();MediaPlayer next=new MediaPlayer();player=next;videoW=videoH=0;int token=generation;
        try{
            next.setVolume(0,0);next.setScreenOnWhilePlaying(false);surface=new Surface(target.getSurfaceTexture());next.setSurface(surface);
            Uri uri=source.startsWith("/")?Uri.fromFile(new File(source)):Uri.parse(source).normalizeScheme();
            if("file".equalsIgnoreCase(uri.getScheme()))next.setDataSource(uri.getPath());else next.setDataSource(context,uri);
            next.setLooping(!"space".equals(effective));
            next.setOnPreparedListener(mp->{
                if(mp!=player||!active)return;prepared=true;videoW=mp.getVideoWidth();videoH=mp.getVideoHeight();transform();
                try{mp.setVolume(0,0);mp.start();}catch(IllegalStateException e){videoError();}
            });
            next.setOnVideoSizeChangedListener((mp,w,h)->{if(mp==player){videoW=w;videoH=h;transform();}});
            next.setOnErrorListener((mp,what,extra)->{if(mp==player)videoError();return true;});
            next.setOnInfoListener((mp,what,extra)->{
                if(mp==player&&what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){rendered=true;android.util.Log.i("TvHome.Saver","video-ready");scene.caption(title);}return false;
            });
            next.setOnCompletionListener(mp->{
                if(mp!=player||!active||!"space".equals(effective))return;
                File file=Flyovers.next(context,spaceFile);if(file==null){fallback(R.string.saver_space_error);return;}
                spaceFile=file;video(file.getAbsolutePath(),Flyovers.caption(file),target);
            });
            handler.postDelayed(()->{if(active&&token==generation&&player==next&&!rendered)videoError();},20000);
            videoFrameAt=SystemClock.uptimeMillis();
            if("space".equals(effective))watchVideo(next,token);
            next.prepareAsync();
        }catch(Exception e){videoError();}
    }
    private void watchVideo(MediaPlayer watched,int token){
        handler.postDelayed(()->{
            if(!active||closed||generation!=token||player!=watched||!"space".equals(effective))return;
            if(prepared&&SystemClock.uptimeMillis()-videoFrameAt>15000){android.util.Log.w("TvHome.Saver","space-stalled");videoError();return;}
            watchVideo(watched,token);
        },5000);
    }
    private void videoError(){
        releasePlayer();if(texture!=null)texture.setVisibility(View.GONE);
        if("space".equals(effective))fallback(R.string.saver_space_error);else scene.caption(context.getString(R.string.saver_video_error));
    }
    private void transform(){
        if(texture==null)return;float[] scale=LiveWallpaperSource.cover(texture.getWidth(),texture.getHeight(),videoW,videoH);
        Matrix m=new Matrix();m.setScale(scale[0],scale[1],texture.getWidth()/2f,texture.getHeight()/2f);texture.setTransform(m);
    }
}
