package com.gathin.tvhome;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
/** End room belongs to the last item's decoration; it never shrinks the actual app cells. */
final class DockBrowseDecoration extends RecyclerView.ItemDecoration {
    private final AppAdapter adapter;private final int columns,pivot;private final String mode;private final boolean rtl;
    DockBrowseDecoration(AppAdapter adapter,int columns,String mode,int pivot,boolean rtl){this.adapter=adapter;this.columns=columns;this.mode=mode;this.pivot=pivot;this.rtl=rtl;}
    @Override public void getItemOffsets(Rect out,View view,RecyclerView parent,RecyclerView.State state){
        if(parent.getChildAdapterPosition(view)!=adapter.appCount()-1)return;
        int cell=(parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight())/Math.max(1,columns);
        int room=cell*DockBrowsePolicy.trailing(mode,adapter.appCount(),columns,pivot);
        if(rtl)out.left=room;else out.right=room;
    }
}
