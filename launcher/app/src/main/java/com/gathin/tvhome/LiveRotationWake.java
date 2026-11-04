package com.gathin.tvhome;
/** Coalesces SCREEN_ON and Home resume for one physical wake, in either callback order. */
final class LiveRotationWake {
    private static final long RESUME_WINDOW_MS=1000;
    private boolean handled;
    private long changedAt=-1;
    LiveRotationWake(boolean alreadyHandled){handled=alreadyHandled;}
    boolean handled(){return handled;}
    void off(){handled=false;changedAt=-1;}
    void prime(){handled=true;changedAt=-1;}
    boolean screenOn(boolean enabled,boolean ready,long now){if(!enabled||!ready||handled)return false;handled=true;changedAt=now;return true;}
    boolean start(boolean home,boolean tv,boolean due,long now){
        boolean television=tv&&due&&!handled;
        if(television){handled=true;changedAt=now;}
        boolean sameWake=changedAt>=0&&now>=changedAt&&now-changedAt<=RESUME_WINDOW_MS;
        return television||(home&&!sameWake);
    }
}
