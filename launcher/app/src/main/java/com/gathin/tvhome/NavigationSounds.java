package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/** App-owned window feedback only. Original callbacks, key handling and system settings are preserved. */
final class NavigationSounds {
    static final String MODE="navigation_sound_mode",VOLUME="navigation_sound_volume",OUTPUT="navigation_sound_output";
    private static final Map<Window,WeakReference<Feedback>> WINDOWS=new WeakHashMap<>();
    private static final ArrayList<WeakReference<PopupFeedback>> POPUPS=new ArrayList<>();
    private static Context app;private static SharedPreferences prefs;private static SoundPool pool;
    private static int sample;private static boolean loaded,pendingPreview,loggedPlayback;
    private static long lastPlay=-1;
    private static final SharedPreferences.OnSharedPreferenceChangeListener CHANGED=(p,key)->{
        if(!MODE.equals(key)&&!VOLUME.equals(key)&&!OUTPUT.equals(key))return;
        if(OUTPUT.equals(key)&&pool!=null){pool.release();pool=null;loaded=false;pendingPreview=false;}
        for(WeakReference<Feedback> ref:new ArrayList<>(WINDOWS.values())){Feedback feedback=ref.get();if(feedback!=null)feedback.apply();}
        POPUPS.removeIf(ref->ref.get()==null);
        for(WeakReference<PopupFeedback> ref:new ArrayList<>(POPUPS)){PopupFeedback feedback=ref.get();if(feedback!=null)feedback.apply();}
        if("luma".equals(mode()))prepare();else if(pool!=null){pool.release();pool=null;loaded=false;pendingPreview=false;}
    };
    private static String mode(){return NavigationSoundPolicy.mode(prefs.getString(MODE,"system"));}
    static void attach(Context context,Window window){
        if(window==null||WINDOWS.containsKey(window))return;
        if(app==null){app=context.getApplicationContext();prefs=app.getSharedPreferences(Config.SETTINGS_NAME,0);prefs.registerOnSharedPreferenceChangeListener(CHANGED);}
        Feedback feedback=new Feedback(window);WINDOWS.put(window,new WeakReference<>(feedback));feedback.apply();
        if("luma".equals(mode()))prepare();
    }
    static void detach(Window window){WeakReference<Feedback> ref=WINDOWS.remove(window);Feedback feedback=ref==null?null:ref.get();if(feedback!=null)feedback.close();}
    static View wrap(Context context,View content){PopupFeedback wrapper=new PopupFeedback(context);wrapper.addView(content);return wrapper;}
    private static void key(KeyEvent event){
        if("luma".equals(mode())&&NavigationSoundPolicy.key(event.getAction(),event.getKeyCode(),event.getRepeatCount())
                &&NavigationSoundPolicy.due(SystemClock.uptimeMillis(),lastPlay))play();
    }
    private static void prepare(){
        if(pool!=null)return;
        try {
        boolean system="system".equals(NavigationSoundPolicy.output(prefs.getString(OUTPUT,"media")));
        SoundPool created=new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(system?AudioAttributes.USAGE_ASSISTANCE_SONIFICATION:AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool=created;loaded=false;
        created.setOnLoadCompleteListener((sound,id,status)->{
            if(sound!=pool)return;loaded=status==0;
            if(!loaded){Log.w("LumaNavigationSound","Navigation sound could not load: "+status);return;}
            if(pendingPreview){pendingPreview=false;play();}
        });
        sample=created.load(app,R.raw.navigation_click,1);
        }catch(RuntimeException unavailable){loaded=false;if(pool!=null)pool.release();pool=null;Log.w("LumaNavigationSound","Navigation audio unavailable",unavailable);}
    }
    static void preview(Context context){
        if(app==null){app=context.getApplicationContext();prefs=app.getSharedPreferences(Config.SETTINGS_NAME,0);prefs.registerOnSharedPreferenceChangeListener(CHANGED);}
        if("luma".equals(mode())&&lastPlay>=0&&SystemClock.uptimeMillis()-lastPlay<500)return;
        prepare();if(loaded)play();else pendingPreview=true;
    }
    private static void play(){
        float gain=NavigationSoundPolicy.volume(prefs.getInt(VOLUME,40))/100f;
        if(pool==null||!loaded||gain==0)return;
        int stream;
        try{stream=pool.play(sample,gain,gain,1,0,1f);}catch(RuntimeException unavailable){Log.w("LumaNavigationSound","Navigation sound could not play",unavailable);return;}
        if(stream!=0){lastPlay=SystemClock.uptimeMillis();if(!loggedPlayback){loggedPlayback=true;Log.d("LumaNavigationSound","Built-in navigation sound playback started");}}
    }
    private static final class Feedback implements InvocationHandler {
        private final Window window;private final Window.Callback original;private View decor;
        private final MuteState sounds=new MuteState();
        private final ViewTreeObserver.OnGlobalLayoutListener layout=this::apply;
        Feedback(Window window){
            this.window=window;original=window.getCallback();
            Window.Callback callback=(Window.Callback)Proxy.newProxyInstance(NavigationSounds.class.getClassLoader(),new Class<?>[]{Window.Callback.class},this);
            window.setCallback(callback);
        }
        void apply(){
            // Do not inflate a Dialog's decor before requestWindowFeature(NO_TITLE).
            View current=window.peekDecorView();if(current==null)return;
            if(decor!=current){decor=current;decor.getViewTreeObserver().addOnGlobalLayoutListener(layout);}
            sounds.apply(decor);
        }
        @Override public Object invoke(Object proxy,Method method,Object[] args)throws Throwable{
            if("onContentChanged".equals(method.getName())||"onWindowFocusChanged".equals(method.getName())||"dispatchTouchEvent".equals(method.getName()))apply();
            if("dispatchKeyEvent".equals(method.getName())&&args!=null&&args.length==1&&args[0] instanceof KeyEvent){apply();key((KeyEvent)args[0]);}
            try{return method.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
        }
        void close(){sounds.restore();if(decor!=null&&decor.getViewTreeObserver().isAlive())decor.getViewTreeObserver().removeOnGlobalLayoutListener(layout);window.setCallback(original);}
    }
    private static final class MuteState {
        private final Map<View,Boolean> nativeSounds=new WeakHashMap<>();
        void apply(View root){if("system".equals(mode()))restore();else suppress(root);}
        private void suppress(View view){
            if(!nativeSounds.containsKey(view))nativeSounds.put(view,view.isSoundEffectsEnabled());
            view.setSoundEffectsEnabled(false);
            if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)suppress(group.getChildAt(i));}
        }
        private void restore(){for(Map.Entry<View,Boolean> entry:nativeSounds.entrySet())entry.getKey().setSoundEffectsEnabled(entry.getValue());nativeSounds.clear();}
    }
    private static final class PopupFeedback extends android.widget.FrameLayout {
        private final MuteState sounds=new MuteState();
        private final ViewTreeObserver.OnGlobalLayoutListener layout=this::apply;
        PopupFeedback(Context context){super(context);}
        void apply(){sounds.apply(this);}
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();POPUPS.add(new WeakReference<>(this));getViewTreeObserver().addOnGlobalLayoutListener(layout);apply();}
        @Override protected void onDetachedFromWindow(){sounds.restore();if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnGlobalLayoutListener(layout);POPUPS.removeIf(ref->ref.get()==null||ref.get()==this);super.onDetachedFromWindow();}
        @Override public boolean dispatchKeyEvent(KeyEvent event){apply();key(event);return super.dispatchKeyEvent(event);}
    }
}
