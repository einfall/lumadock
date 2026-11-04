package com.gathin.tvhome;
import android.os.SystemClock;
import android.view.*;
import java.util.function.BooleanSupplier;

/** Two-second OK hold, sharing the app-tile timing rules. A held key never also selects. */
final class FavoriteOkKey implements View.OnKeyListener,ViewTreeObserver.OnWindowFocusChangeListener {
    private final View view;private final BooleanSupplier enabled;private final Runnable favorite;private final View.OnKeyListener other;
    private final LongOkPress press=new LongOkPress();private int code=-1;
    private final Runnable fire;
    FavoriteOkKey(View view,BooleanSupplier enabled,Runnable favorite,View.OnKeyListener other){
        this.view=view;this.enabled=enabled;this.favorite=favorite;this.other=other;fire=()->{if(view.hasFocus()&&view.hasWindowFocus()&&view.isShown()&&enabled.getAsBoolean()&&press.fire(SystemClock.uptimeMillis()))favorite.run();};view.setOnKeyListener(this);
        view.setOnLongClickListener(v->{if(!enabled.getAsBoolean())return false;cancel();favorite.run();return true;});
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View v){v.getViewTreeObserver().addOnWindowFocusChangeListener(FavoriteOkKey.this);}
            public void onViewDetachedFromWindow(View v){cancel();if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnWindowFocusChangeListener(FavoriteOkKey.this);}
        });
    }
    void cancel(){view.removeCallbacks(fire);press.cancel();code=-1;}
    @Override public void onWindowFocusChanged(boolean focus){if(!focus)cancel();}
    @Override public boolean onKey(View v,int key,KeyEvent event){
        boolean ok=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER;
        if(!ok){if(event.getAction()==KeyEvent.ACTION_DOWN)cancel();return other!=null&&other.onKey(v,key,event);}
        if(!enabled.getAsBoolean())return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN){if(code<0&&event.getRepeatCount()==0){code=key;press.press(SystemClock.uptimeMillis());view.postDelayed(fire,LongOkPress.DELAY);}return true;}
        if(event.getAction()==KeyEvent.ACTION_UP){view.removeCallbacks(fire);int action=code==key?press.release(SystemClock.uptimeMillis(),event.isCanceled()||!view.hasFocus()||!view.hasWindowFocus()):LongOkPress.NONE;cancel();if(action==LongOkPress.CLICK)view.performClick();else if(action==LongOkPress.MENU)favorite.run();return true;}
        return true;
    }
}
