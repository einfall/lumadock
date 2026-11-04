package com.gathin.tvhome;
import java.util.*;
/** Aura's quarter-hour rotation: clear nights favor sky, daylight can show ISS footage. */
final class SaverAutoPolicy {
    static final long INTERVAL=15*60_000L;
    static String choose(int turn,boolean dark,boolean cloudy,boolean museum,boolean wind,boolean sky,boolean space){
        List<String> pool=new ArrayList<>();if(museum)pool.add("museum");if(wind)pool.add("wind");
        if(dark){if(!cloudy&&sky)pool.add("sky");pool.add("mosaic");}
        else{if(space)pool.add("space");pool.add("mosaic");pool.add("photos");}
        return pool.get(Math.floorMod(turn,pool.size()));
    }
    private SaverAutoPolicy() { }
}
