package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

/** One caption follows the actual focused card through zoom, pivot and scrolling. No timer or focusable overlay. */
final class FocusedAppName {
    private final Activity activity;
    private final ViewGroup root;
    private final RecyclerView grid;
    private final TextView caption;
    private final SharedPreferences prefs;
    private final float density;
    private final Rect cardBounds=new Rect();
    private boolean enabled,closed;
    private float edge,targetScale,textHeight;
    private final ViewTreeObserver.OnPreDrawListener draw=()->{place();return true;};
    private final SharedPreferences.OnSharedPreferenceChangeListener change=(p,key)->{
        if(key!=null&&(key.startsWith(AppTitleStyle.P)||key.startsWith("effects_focus_")||key.startsWith("motion_")||Typography.APPS.equals(key)))refresh();
    };

    FocusedAppName(Activity a,ViewGroup root){
        activity=a;this.root=root;grid=a.findViewById(R.id.grid);density=a.getResources().getDisplayMetrics().density;
        prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);caption=new TextView(a);
        caption.setSingleLine(true);caption.setEllipsize(TextUtils.TruncateAt.END);caption.setGravity(Gravity.CENTER);
        caption.setIncludeFontPadding(false);caption.setTextColor(0xF2FFFFFF);caption.setLetterSpacing(.02f);
        caption.setShadowLayer(2*density,0,density,0xCC000000);
        caption.setFocusable(false);caption.setClickable(false);caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        caption.setVisibility(View.INVISIBLE);root.addView(caption,new FrameLayout.LayoutParams(1,1));
        prefs.registerOnSharedPreferenceChangeListener(change);root.getViewTreeObserver().addOnPreDrawListener(draw);refresh();
    }
    void refresh(){
        if(closed)return;enabled=AppTitleStyle.below(activity);
        float size=14f*AppTitlePolicy.labels(prefs.getInt(AppTitleStyle.LABELS,100))/100f;
        caption.setTextSize(size);Typography.apply(caption,Typography.APPS,R.string.font_medium);
        textHeight=Math.max(22*density,caption.getPaint().getFontMetrics().bottom-caption.getPaint().getFontMetrics().top+2*density);
        Config cfg=Config.load(activity);targetScale=FocusStyle.motion(activity,cfg).scale(cfg.animations,cfg.advancedEffects);
        edge=frameEdge(activity);if(!enabled)caption.setVisibility(View.INVISIBLE);root.invalidate();
    }
    static float frameEdge(Activity a){
        TileEffects e=FocusStyle.profile(a,true);float density=a.getResources().getDisplayMetrics().density;
        float stroke=e.width>0?e.width:(e.thin?1f:1.5f)*density;
        float outer=!"none".equals(FocusStyle.frameMode(a))&&e.line()?e.gap+stroke+(e.contrast?2*e.contrastWidth:0):0;
        return Math.max(outer,e.glow?e.glowSpread:0);
    }
    static int reservedRoom(Activity a,int tileHeight){
        if(!AppTitleStyle.below(a))return 0;float density=a.getResources().getDisplayMetrics().density;
        Config cfg=Config.load(a);float scale=FocusStyle.motion(a,cfg).scale(cfg.animations,cfg.advancedEffects);
        float sp=14f*AppTitlePolicy.labels(a.getSharedPreferences(Config.SETTINGS_NAME,0).getInt(AppTitleStyle.LABELS,100))/100f;
        android.graphics.Paint paint=new android.graphics.Paint();paint.setTextSize(sp*a.getResources().getDisplayMetrics().scaledDensity);
        paint.setTypeface(Typography.face(a,Typography.selected(a,Typography.APPS),R.string.font_medium));
        float line=Math.max(22*density,paint.getFontMetrics().bottom-paint.getFontMetrics().top+2*density);
        return (int)Math.ceil(AppTitlePolicy.room(tileHeight,scale,frameEdge(a),line,6*density));
    }
    private void place(){
        if(!enabled||closed)return;
        View item=grid.getFocusedChild();View card=item==null?null:item.findViewById(R.id.card);
        if(card==null||!card.isFocused()||card.getVisibility()!=View.VISIBLE||card.getWidth()==0){caption.setVisibility(View.INVISIBLE);return;}
        RecyclerView.ViewHolder holder=grid.getChildViewHolder(item);RecyclerView.Adapter<?> adapter=grid.getAdapter();
        int pos=holder.getBindingAdapterPosition();
        if(!(adapter instanceof AppAdapter)||pos<0||pos>=((AppAdapter)adapter).appCount()){caption.setVisibility(View.INVISIBLE);return;}
        String label=((AppAdapter)adapter).labelAt(pos);if(!label.contentEquals(caption.getText()))caption.setText(label);
        // Layout coordinates deliberately exclude the zoom matrix; include the item's pivot animation separately.
        cardBounds.set(0,0,card.getWidth(),card.getHeight());root.offsetDescendantRectToMyCoords(card,cardBounds);
        float left=cardBounds.left+item.getTranslationX()+card.getTranslationX(),top=cardBounds.top+item.getTranslationY()+card.getTranslationY();
        int width=Math.min(Math.round(card.getWidth()*Math.max(targetScale,card.getScaleX())),Math.max(1,root.getWidth()-Math.round(16*density)));
        ViewGroup.LayoutParams lp=caption.getLayoutParams();int height=(int)Math.ceil(textHeight);
        if(lp.width!=width||lp.height!=height){lp.width=width;lp.height=height;caption.setLayoutParams(lp);}
        float y=AppTitlePolicy.captionTop(top,card.getHeight(),card.getScaleY(),targetScale,edge,6*density);
        // A partially clipped row must not leave a detached caption over the header or another row.
        if(y<0||y+height>root.getHeight()||!item.isShown()){caption.setVisibility(View.INVISIBLE);return;}
        caption.setX(AppTitlePolicy.captionLeft(left+card.getWidth()/2f,width,root.getWidth(),8*density));caption.setY(y);
        caption.setVisibility(View.VISIBLE);
    }
    void close(){closed=true;prefs.unregisterOnSharedPreferenceChangeListener(change);if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnPreDrawListener(draw);root.removeView(caption);}
}
