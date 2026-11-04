package com.gathin.tvhome.aura;

// Adapted from Aura TV, MIT (c) 2026 iravi; see assets/aura/NOTICE.txt.

import android.content.Context;
import android.view.View;

/**
 * A screensaver scene that draws itself (the sky, the aurora, rain on glass, the photo mosaic,
 * the gradient clock, a festival). Started when the screensaver starts, stopped when it ends or
 * moves on to another scene; it may hand the story card a caption now and then.
 */
abstract class Scene extends View {
    interface Listener {
        void onStory(String context, String title, String line, String credit, long ms);
        default void onError() { }
    }

    protected Listener listener;
    protected volatile boolean disposed;

    Scene(Context c) {
        super(c);
    }

    void setListener(Listener l) {
        listener = l;
    }

    /** True when the scene shows its own clock, and the story card's should step aside. */
    boolean ownClock() {
        return false;
    }

    abstract void start();

    abstract void stop();

    protected void fail(){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{if(!disposed&&listener!=null)listener.onError();});}
    protected void work(java.util.concurrent.ExecutorService executor,Runnable task){
        if(disposed)return;
        try{executor.execute(()->{if(disposed)return;try{task.run();}catch(RuntimeException|OutOfMemoryError e){android.util.Log.w("TvHome.Aura","scene-error "+getClass().getSimpleName()+" "+e.getClass().getSimpleName());fail();}});}
        catch(java.util.concurrent.RejectedExecutionException e){if(!disposed)fail();}
    }
}

