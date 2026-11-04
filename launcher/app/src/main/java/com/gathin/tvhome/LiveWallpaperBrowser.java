package com.gathin.tvhome;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;
/** Full, virtualized category. Selecting a video never dismisses or scrolls the list. */
final class LiveWallpaperBrowser {
    private final Activity a;private final Runnable changed;private final boolean favoritesOnly;
    private final List<LiveWallpaperCatalog.Item> items;private final RecyclerView list;private final TextView counter,empty;
    private final Videos adapter=new Videos();private final SharedPreferences prefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->refreshMarks(key);
    private void refreshMarks(String key){if("source".equals(key)||LiveWallpaperSelection.FAVORITES.equals(key))adapter.notifyItemRangeChanged(0,items.size(),"marks");}
    static void show(Activity a,String title,List<LiveWallpaperCatalog.Item> items,boolean favorites,Runnable changed){new LiveWallpaperBrowser(a,title,items,favorites,changed);}
    private LiveWallpaperBrowser(Activity a,String title,List<LiveWallpaperCatalog.Item> source,boolean favorites,Runnable changed){
        this.a=a;this.changed=changed;favoritesOnly=favorites;items=new ArrayList<>(source);prefs=LiveWallpaperPrefs.prefs(a);
        SettingsSheet sheet=new SettingsSheet(a,title);LinearLayout content=new LinearLayout(a);content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);TextView hint=GlassDialog.text(a,a.getString(R.string.live_browse_keys),10);hint.setTextColor(0xFFB5C6DC);head.addView(hint,new LinearLayout.LayoutParams(0,-2,1));
        counter=GlassDialog.text(a,"",11);counter.setGravity(Gravity.CENTER);counter.setPadding(dp(10),dp(3),dp(10),dp(3));counter.setBackgroundResource(R.drawable.pill_bg);head.addView(counter);content.addView(head,new LinearLayout.LayoutParams(-1,dp(37)));
        empty=GlassDialog.text(a,a.getString(R.string.live_favorites_empty),12);empty.setPadding(dp(8),dp(12),dp(8),dp(12));content.addView(empty);
        list=new RecyclerView(a);list.setLayoutManager(new LinearLayoutManager(a));list.setItemAnimator(null);list.setAdapter(adapter);list.setOverScrollMode(View.OVER_SCROLL_NEVER);content.addView(list,new LinearLayout.LayoutParams(-1,0,1));sheet.content(content);
        updateCount(0);prefs.registerOnSharedPreferenceChangeListener(listener);sheet.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(listener));sheet.show();if(!items.isEmpty())focus(0);
    }
    private int dp(int n){return SettingsSheet.dp(a,n);}
    private void updateCount(int at){counter.setText(items.isEmpty()?"0 / 0":(Math.max(0,Math.min(at,items.size()-1))+1)+" / "+items.size());empty.setVisibility(items.isEmpty()?View.VISIBLE:View.GONE);}
    private void focus(int position){
        list.scrollToPosition(position);list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){RecyclerView.ViewHolder h=list.findViewHolderForAdapterPosition(position);if(h==null)return true;if(list.getViewTreeObserver().isAlive())list.getViewTreeObserver().removeOnPreDrawListener(this);h.itemView.requestFocus();return true;}});list.requestLayout();
    }
    private final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout row;final TextView title,detail,marks;final FavoriteOkKey ok;
        Holder(LinearLayout row){super(row);this.row=row;LinearLayout labels=(LinearLayout)row.getChildAt(1);title=(TextView)labels.getChildAt(0);detail=(TextView)labels.getChildAt(1);marks=(TextView)row.getChildAt(2);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);detail.setSingleLine(true);
            ok=new FavoriteOkKey(row,()->getBindingAdapterPosition()!=RecyclerView.NO_POSITION,this::toggleFavorite,(v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_RIGHT)return true;if(key!=KeyEvent.KEYCODE_DPAD_LEFT)return false;if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)toggleFavorite();return true;});
            row.setOnFocusChangeListener((v,on)->{int at=getBindingAdapterPosition();if(on&&at!=RecyclerView.NO_POSITION)updateCount(at);if(!on)ok.cancel();});
            row.setOnClickListener(v->{int at=getBindingAdapterPosition();if(at==RecyclerView.NO_POSITION)return;LiveWallpaperCatalog.Item i=items.get(at);LiveWallpaperPrefs.select(a,i.url,i.title);changed.run();updateCount(at);});
        }
        void toggleFavorite(){favoriteAt(this);}
    }
    private void favoriteAt(Holder holder){int at=holder.getBindingAdapterPosition();if(at==RecyclerView.NO_POSITION)return;boolean on=LiveWallpaperSelection.toggle(a,items.get(at).url);Toast.makeText(a,on?"Zu Favoriten hinzugefügt":"Aus Favoriten entfernt",Toast.LENGTH_SHORT).show();if(favoritesOnly&&!on){items.remove(at);adapter.notifyItemRemoved(at);updateCount(at);if(!items.isEmpty())focus(Math.min(at,items.size()-1));}}
    private final class Videos extends RecyclerView.Adapter<Holder> {
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){LinearLayout row=SettingsSheet.row(a,"photo",""," ","☆");row.setLayoutParams(new RecyclerView.LayoutParams(-1,dp(52)));return new Holder(row);}
        @Override public void onBindViewHolder(@NonNull Holder h,int position){h.ok.cancel();LiveWallpaperCatalog.Item i=items.get(position);boolean favorite=LiveWallpaperSelection.favorites(a).contains(i.url),selected=i.url.equals(LiveWallpaperPrefs.source(a));h.title.setText(i.title);h.detail.setText(LiveWallpaperCatalog.sourceName(i.source));h.marks.setText((favorite?"★":"☆")+(selected?"  ✓":""));h.row.setContentDescription(i.title+(favorite?" · "+a.getString(R.string.live_favorite):"")+(selected?" · "+a.getString(R.string.live_selected):""));}
        @Override public void onBindViewHolder(@NonNull Holder h,int position,@NonNull java.util.List<Object> payloads){if(payloads.isEmpty())onBindViewHolder(h,position);else{LiveWallpaperCatalog.Item i=items.get(position);boolean favorite=LiveWallpaperSelection.favorites(a).contains(i.url),selected=i.url.equals(LiveWallpaperPrefs.source(a));h.marks.setText((favorite?"★":"☆")+(selected?"  ✓":""));h.row.setContentDescription(i.title+(favorite?" · "+a.getString(R.string.live_favorite):"")+(selected?" · "+a.getString(R.string.live_selected):""));}}
        @Override public int getItemCount(){return items.size();}
    }
}
