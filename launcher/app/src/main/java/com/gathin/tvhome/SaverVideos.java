package com.gathin.tvhome;
import android.app.Activity;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import java.util.*;
/** Reuses the available video catalogue, with saver-only selection and no Home preference writes. */
final class SaverVideos {
    static void show(Activity a,String title,List<LiveWallpaperCatalog.Item> items){
        SettingsSheet sheet=new SettingsSheet(a,title);LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);TextView count=GlassDialog.text(a,items.isEmpty()?"0 / 0":"1 / "+items.size(),12);count.setPadding(12,8,12,12);body.addView(count);
        RecyclerView list=new RecyclerView(a);list.setLayoutManager(new LinearLayoutManager(a));list.setItemAnimator(null);body.addView(list,new LinearLayout.LayoutParams(-1,0,1));SharedPreferences prefs=SaverPrefs.get(a);
        class Holder extends RecyclerView.ViewHolder {final LinearLayout row;final TextView title,sub,mark;
            Holder(LinearLayout v){super(v);row=v;LinearLayout text=(LinearLayout)v.getChildAt(1);title=(TextView)text.getChildAt(0);sub=(TextView)text.getChildAt(1);mark=(TextView)v.getChildAt(2);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);sub.setSingleLine(true);
                v.setOnFocusChangeListener((view,on)->{int at=getBindingAdapterPosition();if(on&&at>=0)count.setText((at+1)+" / "+items.size());});v.setOnClickListener(view->{int at=getBindingAdapterPosition();if(at>=0&&at<items.size()){LiveWallpaperCatalog.Item item=items.get(at);SaverPrefs.video(a,item.url,item.title);}});}
        }
        RecyclerView.Adapter<Holder> adapter=new RecyclerView.Adapter<Holder>(){
            @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){LinearLayout row=SettingsSheet.row(a,"video",""," ","");row.setLayoutParams(new RecyclerView.LayoutParams(-1,SettingsSheet.dp(a,52)));return new Holder(row);}
            public void onBindViewHolder(@NonNull Holder h,int at){LiveWallpaperCatalog.Item item=items.get(at);h.title.setText(item.title);h.sub.setText(LiveWallpaperCatalog.sourceName(item.source));boolean selected=item.url.equals(prefs.getString(SaverPrefs.VIDEO,""));h.mark.setText(selected?"✓":"");h.row.setSelected(selected);}
            public int getItemCount(){return items.size();}
        };list.setAdapter(adapter);SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{if(SaverPrefs.VIDEO.equals(key))adapter.notifyItemRangeChanged(0,items.size(),"mark");};prefs.registerOnSharedPreferenceChangeListener(update);sheet.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));sheet.content(body);sheet.show();
        if(!items.isEmpty())list.post(()->{RecyclerView.ViewHolder h=list.findViewHolderForAdapterPosition(0);if(h!=null)h.itemView.requestFocus();});
    }
}
