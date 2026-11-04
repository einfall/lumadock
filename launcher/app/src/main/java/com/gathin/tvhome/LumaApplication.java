package com.gathin.tvhome;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public final class LumaApplication extends Application {
    @Override public void onCreate(){
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
            public void onActivityCreated(Activity activity,Bundle state){if(!(activity instanceof ClockActivity))NavigationSounds.attach(activity,activity.getWindow());}
            public void onActivityDestroyed(Activity activity){NavigationSounds.detach(activity.getWindow());}
            public void onActivityStarted(Activity activity){}
            public void onActivityResumed(Activity activity){}
            public void onActivityPaused(Activity activity){}
            public void onActivityStopped(Activity activity){}
            public void onActivitySaveInstanceState(Activity activity,Bundle state){}
        });
    }
}
