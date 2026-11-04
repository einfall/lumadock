package com.gathin.tvhome;
/** Pure selection / timing rules. A second item always changes the video; elapsed time never depends on wall clock. */
final class LiveRotationPolicy {
    static final String[] TRIGGERS={"interval","home","tv"};
    /** Null means an older installation; an explicit empty set always means Off. */
    static java.util.Set<String> triggers(java.util.Set<String> saved,String legacy){
        java.util.Set<String> out=new java.util.LinkedHashSet<>();
        if(saved==null){String one=mode(legacy);if(!"off".equals(one))out.add(one);}
        else if(!saved.contains("off"))for(String id:TRIGGERS)if(saved.contains(id))out.add(id);
        return out;
    }
    static java.util.Set<String> withTrigger(java.util.Set<String> current,String id,boolean on){
        java.util.Set<String> next=triggers(current,"off");
        if("off".equals(id)){if(on)next.clear();}
        else if(!"off".equals(mode(id))){if(on)next.add(id);else next.remove(id);}
        return next;
    }
    /** An older APK can still use one meaningful trigger after a downgrade. */
    static String legacyMode(java.util.Set<String> selected){for(String id:TRIGGERS)if(selected.contains(id))return id;return "off";}
    static String mode(String value){return "interval".equals(value)||"home".equals(value)||"tv".equals(value)?value:"off";}
    static int minutes(int value){return Math.max(1,Math.min(240,value));}
    static int nextIndex(int size,int current,int draw){
        if(size<=0)return -1;if(size==1)return 0;
        boolean known=current>=0&&current<size;int n=Math.floorMod(draw,known?size-1:size);
        return known&&n>=current?n+1:n;
    }
    static long delay(long now,long last,int savedBoot,int boot,int minutes){
        long duration=minutes(minutes)*60000L;
        if(savedBoot!=boot||last<=0||last>now)return duration;
        return Math.max(0,duration-(now-last));
    }
}
