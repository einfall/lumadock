package com.gathin.tvhome;

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

final class PlacementGuides {
    static final String SHOW = "placement_guides";
    static final String SNAP = "placement_snap";
    static final int DISTANCE = 6;

    final int[] columns;
    final int[] rows;

    PlacementGuides(int width, int height, List<Rect> neighbors, float density) {
        columns = axes(width, neighbors, density, true);
        rows = axes(height, neighbors, density, false);
    }

    private static int[] axes(int length, List<Rect> neighbors, float density, boolean horizontal) {
        TreeSet<Integer> guides = new TreeSet<>();
        guides.add(0);
        guides.add(length / 2);
        guides.add(length);
        for (Rect rect : neighbors) {
            int start = Math.round((horizontal ? rect.left : rect.top) / density);
            int end = Math.round((horizontal ? rect.right : rect.bottom) / density);
            if (start >= 0 && end <= length && end > start) {
                guides.add(start);
                guides.add(start + (end - start) / 2);
                guides.add(end);
            }
        }
        List<Integer> ordered = new ArrayList<>(guides);
        int[] result = new int[ordered.size()];
        for (int i = 0; i < result.length; i++) result[i] = ordered.get(i);
        return result;
    }

    static int snap(int position, int size, int length, int[] guides) {
        int closest = position;
        int distance = DISTANCE + 1;
        for (int guide : guides) {
            for (int anchor : new int[]{0, size / 2, size}) {
                int candidate = guide - anchor;
                int delta = Math.abs(candidate - position);
                if (delta < distance && candidate >= 0 && candidate + size <= length) {
                    closest = candidate;
                    distance = delta;
                }
            }
        }
        return closest;
    }
}
