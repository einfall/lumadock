package com.gathin.tvhome;
import java.util.*;
/** A type gets an equal chance, regardless of how many catalog items it contains. */
final class BackgroundRotationPolicy {
    static final String[] TYPES={"live","photo","gradient"};
    static String mode(String saved,boolean legacyLive,boolean legacyTriggers){return saved==null?(legacyLive&&legacyTriggers?"rotation":"fixed"):"rotation".equals(saved)?"rotation":"fixed";}
    static Set<String> types(Set<String> saved){Set<String> result=new LinkedHashSet<>();if(saved==null){result.add("live");return result;}for(String id:TYPES)if(saved.contains(id))result.add(id);return result;}
    static List<String> available(Set<String> types,int live,int photo,int gradient){List<String> result=new ArrayList<>();int[] counts={live,photo,gradient};for(int i=0;i<TYPES.length;i++)if(types.contains(TYPES[i])&&counts[i]>0)result.add(TYPES[i]);return result;}
    static String kind(List<String> available,String current,boolean currentHasOne,int draw){List<String> candidates=new ArrayList<>(available);if(currentHasOne&&candidates.size()>1)candidates.remove(current);return candidates.isEmpty()?null:candidates.get(Math.floorMod(draw,candidates.size()));}
}
