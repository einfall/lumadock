package com.gathin.tvhome;
/** A short release opens once; a held key opens its menu once, even with repeated DOWN events. */
final class LongOkPress {
    static final long DELAY=2000;
    static final int NONE=0,CLICK=1,MENU=2;
    private long down;private boolean active,fired;
    void press(long now){down=now;active=true;fired=false;}
    boolean fire(long now){if(!active||fired||now-down<DELAY)return false;fired=true;return true;}
    int release(long now,boolean cancelled){if(!active)return NONE;boolean held=fired||now-down>=DELAY;active=false;if(cancelled||fired)return NONE;return held?MENU:CLICK;}
    void cancel(){active=false;}
}
