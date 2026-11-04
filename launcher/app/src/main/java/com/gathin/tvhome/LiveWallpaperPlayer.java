package com.gathin.tvhome;
import android.content.*;
import android.graphics.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import java.io.File;

/** One muted native decoder, allocated only for the visible Home. Never requests audio focus or a wake lock. */
final class LiveWallpaperPlayer implements TextureView.SurfaceTextureListener {
    private final Context context;private final FrameLayout layer;private final TextureView texture;private final View shade;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final LiveWallpaperRotation rotation;
    private MediaPlayer player;private Surface surface;private boolean active,closed,prepared,rendered,failed;private String opened="";private int videoW,videoH;
    private final Runnable timeout=()->{if(player!=null&&!rendered)error();};
    private final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(Intent.ACTION_SCREEN_OFF.equals(i.getAction())){rotation.screenOff();suspend();}else{rotation.screenOn();if(active)refresh();}}};
    LiveWallpaperPlayer(Context c,FrameLayout root){
        context=c;rotation=new LiveWallpaperRotation(c,this::refresh);layer=new FrameLayout(c);layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);layer.setVisibility(View.GONE);
        texture=new TextureView(c);texture.setOpaque(false);texture.setSurfaceTextureListener(this);texture.setFocusable(false);layer.addView(texture,new FrameLayout.LayoutParams(-1,-1));
        shade=new View(c);shade.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);layer.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        // Above static wallpaper / ambient glows, below every Home control.
        int ambient=root.indexOfChild(root.findViewById(R.id.ambient));root.addView(layer,Math.max(0,ambient+1),new FrameLayout.LayoutParams(-1,-1));
        IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);if(Build.VERSION.SDK_INT>=33)c.registerReceiver(screen,f,Context.RECEIVER_NOT_EXPORTED);else c.registerReceiver(screen,f);
    }
    void start(){active=true;failed=false;rotation.start();refresh();}
    void stop(){active=false;rotation.stop();suspend();}
    private boolean enabled(){return LiveWallpaperPrefs.prefs(context).getBoolean("enabled",false);}
    void refresh(){
        if(closed)return;int dim=LiveWallpaperSource.dim(LiveWallpaperPrefs.prefs(context).getInt("dim",35));shade.setBackgroundColor((Math.round(dim*2.55f)<<24));
        String source=LiveWallpaperPrefs.source(context);PowerManager power=context.getSystemService(PowerManager.class);
        if(!active||power==null||!power.isInteractive()||!enabled()){suspend();if(!enabled())state("off");return;}
        if(!LiveWallpaperSource.valid(source)){suspend();state(source.isEmpty()?"empty":"error");return;}
        if(!source.equals(opened)){releasePlayer();opened=source;failed=false;}
        if(failed)return;
        layer.setVisibility(View.VISIBLE);if(player==null){layer.setAlpha(1);shade.setVisibility(View.GONE);state("loading");if(texture.isAvailable())open();}
    }
    private void open(){
        if(closed||!active||failed||player!=null||!texture.isAvailable()||!enabled())return;
        MediaPlayer next=new MediaPlayer();player=next;prepared=false;rendered=false;videoW=videoH=0;
        try{
            next.setVolume(0,0);surface=new Surface(texture.getSurfaceTexture());next.setSurface(surface);next.setScreenOnWhilePlaying(false);
            String source=LiveWallpaperPrefs.source(context);Uri uri=source.startsWith("/")?Uri.fromFile(new File(source)):Uri.parse(source).normalizeScheme();
            if("file".equalsIgnoreCase(uri.getScheme()))next.setDataSource(uri.getPath());else next.setDataSource(context,uri);
            next.setLooping(true);
            next.setOnPreparedListener(mp->{if(mp!=player||!active)return;prepared=true;videoW=mp.getVideoWidth();videoH=mp.getVideoHeight();transform();try{mp.setVolume(0,0);mp.start();}catch(IllegalStateException ex){error();}});
            next.setOnVideoSizeChangedListener((mp,w,h)->{if(mp==player){videoW=w;videoH=h;transform();}});
            next.setOnInfoListener((mp,what,extra)->{if(mp==player&&what==MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START){rendered=true;handler.removeCallbacks(timeout);shade.setVisibility(View.VISIBLE);layer.setAlpha(1);state("playing");}return false;});
            next.setOnErrorListener((mp,what,extra)->{if(mp==player)error();return true;});
            handler.postDelayed(timeout,20000);next.prepareAsync();
        }catch(Exception ex){error();}
    }
    private void error(){failed=true;releasePlayer();layer.setVisibility(View.GONE);state("error");}
    private void suspend(){releasePlayer();layer.setVisibility(View.GONE);opened="";failed=false;state(enabled()?"paused":"off");}
    private void releasePlayer(){handler.removeCallbacks(timeout);if(player!=null){MediaPlayer old=player;player=null;old.setOnPreparedListener(null);old.setOnErrorListener(null);old.setOnInfoListener(null);old.setOnVideoSizeChangedListener(null);old.release();}if(surface!=null){surface.release();surface=null;}prepared=false;}
    private void transform(){float[] scale=LiveWallpaperSource.cover(texture.getWidth(),texture.getHeight(),videoW,videoH);Matrix m=new Matrix();m.setScale(scale[0],scale[1],texture.getWidth()/2f,texture.getHeight()/2f);texture.setTransform(m);}
    private void state(String value){SharedPreferences p=context.getSharedPreferences(LiveWallpaperPrefs.RUNTIME,0);if(value.equals(p.getString("state","")))return;p.edit().putString("state",value).putInt("width",videoW).putInt("height",videoH).apply();Log.i("TvHome.LiveWallpaper",value);}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){if(active)refresh();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){transform();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s){releasePlayer();return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture s){if(player!=null&&prepared&&!rendered){rendered=true;handler.removeCallbacks(timeout);shade.setVisibility(View.VISIBLE);state("playing");}}
    void close(){if(closed)return;closed=true;active=false;rotation.close();releasePlayer();handler.removeCallbacksAndMessages(null);context.unregisterReceiver(screen);layer.setVisibility(View.GONE);}
}
