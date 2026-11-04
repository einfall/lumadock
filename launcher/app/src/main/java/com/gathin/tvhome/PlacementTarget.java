package com.gathin.tvhome;
import android.graphics.Rect;
import android.view.View;
/** A placement surface shared by original header elements and independently created widgets. */
interface PlacementTarget {
    View view(String key);int[] size(String key);int[] position(String key);Rect bounds(String key);int width();int height();
    void preview(String key,int x,int y);void endPreview();void set(String key,int x,int y);
}
