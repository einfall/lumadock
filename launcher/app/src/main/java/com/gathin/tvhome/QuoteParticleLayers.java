package com.gathin.tvhome;
import android.graphics.Canvas;
import android.view.*;
import android.widget.FrameLayout;
import java.util.*;
/** Two input-transparent root layers. Back is below the dock's actual container; front is above Home. */
final class QuoteParticleLayers {
    private final ViewGroup root;private final Layer back,front;private final Set<QuoteWidgetView> views=new LinkedHashSet<>();private final int[] origin=new int[2],point=new int[2];private boolean closed;
    private final class Layer extends View {
        final boolean foreground;
        Layer(boolean front){super(root.getContext());foreground=front;setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);}
        @Override protected void onDraw(Canvas canvas){root.getLocationInWindow(origin);for(QuoteWidgetView view:views)if(view.external(foreground)){view.getLocationInWindow(point);int save=canvas.save();canvas.translate(point[0]-origin[0],point[1]-origin[1]);canvas.clipOutRect(0,0,view.getWidth(),view.getHeight());view.drawExternal(canvas,foreground);canvas.restoreToCount(save);}}
    }
    QuoteParticleLayers(ViewGroup root){this.root=root;back=new Layer(false);front=new Layer(true);View anchor=root.findViewById(R.id.grid);while(anchor!=null&&anchor.getParent()!=root)anchor=anchor.getParent() instanceof View?(View)anchor.getParent():null;int at=anchor==null?root.getChildCount():root.indexOfChild(anchor);root.addView(back,at,new FrameLayout.LayoutParams(-1,-1));front.setElevation(32*root.getResources().getDisplayMetrics().density);root.addView(front,new FrameLayout.LayoutParams(-1,-1));}
    void register(QuoteWidgetView v){views.add(v);}
    void unregister(QuoteWidgetView v){views.remove(v);invalidate();}
    void invalidate(){if(closed)return;boolean behind=false,ahead=false;for(QuoteWidgetView v:views){behind|=v.external(false);ahead|=v.external(true);}back.setVisibility(behind?View.VISIBLE:View.GONE);front.setVisibility(ahead?View.VISIBLE:View.GONE);if(behind)back.invalidate();if(ahead)front.invalidate();}
    float originY(View view){root.getLocationInWindow(origin);view.getLocationInWindow(point);return point[1]-origin[1];}
    int height(){return root.getHeight();}
    void close(){closed=true;views.clear();root.removeView(back);root.removeView(front);}
}
