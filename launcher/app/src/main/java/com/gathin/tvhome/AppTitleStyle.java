package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.TextView;
/** Large selected app title: independent text size and optional absolute screen placement. */
final class AppTitleStyle implements PlacementTarget {
    static final String P="app_title_",SIZE=P+"size",LABELS=P+"labels",X=P+"x",Y=P+"y",MODE=P+"mode",DOMINANT=P+"dominant_color";
    static boolean below(Context c){return "focused".equals(AppTitlePolicy.mode(c.getSharedPreferences(Config.SETTINGS_NAME,0).getString(MODE,"hero")));}
    private final FocusedAppName focusedName;
    private final Activity a;private final TextView name;private final ViewGroup root;private final SharedPreferences prefs;
    FocusedAppName focused(){return focusedName;}
    private AppAdapter selectedAdapter;private int selectedPosition;private final int originalInk;
    final int originalSize;private final float density;private boolean pending,closed,preview;private int px,py;
    private final ViewTreeObserver.OnGlobalLayoutListener layout=this::schedule;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,k)->{if(k!=null&&k.startsWith(P))schedule();};
    AppTitleStyle(Activity a,TextView name){this.a=a;this.name=name;originalInk=name.getCurrentTextColor();density=a.getResources().getDisplayMetrics().density;root=a.findViewById(R.id.root);prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);originalSize=Math.round(name.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity);
        for(View v=name;v.getParent() instanceof ViewGroup;){ViewGroup parent=(ViewGroup)v.getParent();parent.setClipChildren(false);parent.setClipToPadding(false);v=parent;}
        focusedName=new FocusedAppName(a,root);prefs.registerOnSharedPreferenceChangeListener(listener);root.getViewTreeObserver().addOnGlobalLayoutListener(layout);schedule();}
    void focused(AppAdapter adapter,int position){selectedAdapter=adapter;selectedPosition=position;applyInk();}
    private void applyInk(){int color=originalInk;if(prefs.getBoolean(DOMINANT,false)){if(selectedAdapter==null){androidx.recyclerview.widget.RecyclerView grid=a.findViewById(R.id.grid);if(grid.getAdapter() instanceof AppAdapter){selectedAdapter=(AppAdapter)grid.getAdapter();View child=grid.getFocusedChild();selectedPosition=child==null?0:grid.getChildAdapterPosition(child);}}if(selectedAdapter!=null&&selectedPosition>=0&&selectedPosition<selectedAdapter.appCount())color=selectedAdapter.iconDominantAt(selectedPosition);}name.setTextColor(color);}
    void schedule(){if(pending||closed)return;pending=true;root.post(()->{pending=false;if(closed)return;apply();});}
    private void apply(){applyInk();name.setVisibility(below(a)?View.INVISIBLE:View.VISIBLE);focusedName.refresh();int size=AppTitlePolicy.size(prefs.getInt(SIZE,originalSize));if(Math.abs(name.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity-size)>.1f)name.setTextSize(size);
        Rect r=bounds("title");float bx=(r.left-name.getTranslationX())/density,by=(r.top-name.getTranslationY())/density;
        name.setTranslationX((preview?px-bx:prefs.contains(X)?HeaderLayoutPolicy.clamp(prefs.getInt(X,0),width(),dp(name.getWidth()))-bx:0)*density);
        name.setTranslationY((preview?py-by:prefs.contains(Y)?HeaderLayoutPolicy.clamp(prefs.getInt(Y,0),height(),dp(name.getHeight()))-by:0)*density);
    }
    public View view(String key){return name;}public int[] size(String key){return new int[]{dp(name.getWidth()),dp(name.getHeight())};}
    public int[] position(String key){Rect r=bounds(key);return new int[]{dp(r.left),dp(r.top)};}
    public Rect bounds(String key){int[] loc=new int[2],origin=new int[2];name.getLocationOnScreen(loc);root.getLocationOnScreen(origin);return new Rect(loc[0]-origin[0],loc[1]-origin[1],loc[0]-origin[0]+name.getWidth(),loc[1]-origin[1]+name.getHeight());}
    public int width(){return dp(root.getWidth());}public int height(){return dp(root.getHeight());}
    public void preview(String key,int x,int y){preview=true;px=x;py=y;apply();}public void endPreview(){preview=false;apply();}
    public void set(String key,int x,int y){prefs.edit().putInt(X,x).putInt(Y,y).apply();preview=false;apply();}
    private int dp(float px){return Math.round(px/density);}
    void reset(){preview=false;prefs.edit().remove(MODE).remove(DOMINANT).remove(SIZE).remove(LABELS).remove(X).remove(Y).apply();apply();}
    void close(){closed=true;focusedName.close();prefs.unregisterOnSharedPreferenceChangeListener(listener);if(root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnGlobalLayoutListener(layout);}
}
