package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
/** A glass settings list over the actual Home, with three-column media previews. */
final class DesignStudio {
    static void show(MainActivity a,Runnable changed){new DesignStudio(a,changed,false);}
    static void showSaver(ClockActivity a){SaverSettings.show(a);}
    static void background(Activity a,String tab){new DesignStudio(a,()->{if(a instanceof MainActivity)((MainActivity)a).refreshLiveWallpaper();},false,tab);}
    static void media(Activity a,boolean saver,boolean videos){new DesignStudio(a,()->{},saver,videos?"Live-Hintergründe":"Bilder");}
    private final Activity a;private final Runnable changed;private final boolean saver,standaloneMedia;private final Dialog dialog;
    private final LinearLayout page;private final TextView heading;private final SettingsSheet sheet;
    private final Handler handler=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final android.util.LruCache<String,Bitmap> thumbs=new android.util.LruCache<String,Bitmap>(2*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
    private final SharedPreferences config,background,live;private boolean closed,capturing;private int galleryGeneration;private String current="root";
    private final Runnable refreshFavoriteItems = this::trimFavorites;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener=(p,k)->{
        requestPreview();
        if(LiveWallpaperSelection.FAVORITES.equals(k)||YouTubeFavorites.KEY.equals(k)){
            handler.removeCallbacks(refreshFavoriteItems);
            handler.post(refreshFavoriteItems);
        }
    };
    private Gallery adapter;
    private LinearLayout wallpaperBody, youtubeContent;
    private RecyclerView galleryView;
    private YouTubeBrowser youtube;
    private String wallpaperTab = "", videoPool = "all";

    private DesignStudio(Activity a,Runnable changed,boolean saver){this(a,changed,saver,null);}
    private DesignStudio(Activity a,Runnable changed,boolean saver,String media){this.a=a;this.changed=changed;this.saver=saver;this.standaloneMedia=media!=null;config=a.getSharedPreferences(Config.SETTINGS_NAME,0);background=SaverPrefs.get(a);live=LiveWallpaperPrefs.prefs(a);videoPool=live.getString(LiveWallpaperSelection.CATEGORY,"all");
        sheet=new SettingsSheet(a,saver?"Bildschirmschoner":"Design & Apps");dialog=sheet.dialog;sheet.backAction(this::back);page=new LinearLayout(a);page.setOrientation(LinearLayout.VERTICAL);sheet.content(page);heading=text("",16);heading.setPadding(dp(4),0,0,dp(7));
        dialog.setOnKeyListener((d,key,e)->{if(key==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_UP)back();return true;}if(key==KeyEvent.KEYCODE_MENU){View focused=dialog.getCurrentFocus();if(focused!=null&&focused.getTag() instanceof String&&((String)focused.getTag()).startsWith("http"))return false;if(e.getAction()==KeyEvent.ACTION_UP)SettingsPreview.show(a);return true;}return false;});
        dialog.setOnDismissListener(d->{closed=true;closeYouTube();handler.removeCallbacksAndMessages(null);config.unregisterOnSharedPreferenceChangeListener(listener);background.unregisterOnSharedPreferenceChangeListener(listener);live.unregisterOnSharedPreferenceChangeListener(listener);worker.shutdownNow();thumbs.evictAll();});
        config.registerOnSharedPreferenceChangeListener(listener);background.registerOnSharedPreferenceChangeListener(listener);live.registerOnSharedPreferenceChangeListener(listener);
        if(media!=null)wallpaperPage(media);else root();sheet.show();
    }
    private int dp(float n){return SettingsSheet.dp(a,n);}private TextView text(String value,int size){return GlassDialog.text(a,value,size);}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private Drawable cardBackground(){StateListDrawable d=new StateListDrawable();GradientDrawable focused=round(0xFFF7F8FA,10);focused.setStroke(dp(1),0xFFFFFFFF);d.addState(new int[]{android.R.attr.state_focused},focused);GradientDrawable selected=round(0xFF424952,10);selected.setStroke(dp(1),0xFF9DC3EF);d.addState(new int[]{android.R.attr.state_selected},selected);d.addState(new int[]{},round(0xFF303235,10));return d;}
    private void reset(String name){closeYouTube();galleryView=null;wallpaperBody=null;wallpaperTab="";handler.removeCallbacks(refreshFavoriteItems);sheet.hint("Live-Vorschau · ◀ ▶ ändern · OK wählen · MENÜ: Vollbild");galleryGeneration++;adapter=null;page.removeAllViews();page.addView(heading,new LinearLayout.LayoutParams(-1,-2));current=name;heading.setVisibility(name.equals("root")?View.GONE:View.VISIBLE);heading.setText(name.equals("root")?"Design & Apps":(saver?"Bildschirmschoner · ":"")+name);}
    private void back(){if(!"root".equals(current)&&!saver&&!standaloneMedia){root();return;}dialog.dismiss();}
    private void root(){reset("root");ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        listRow(list,"photo","Hintergrund","Verläufe, Bilder, Live-Hintergründe und Wechsel",()->wallpaperPage("Verläufe"));
        listRow(list,"grid","Geöffnete Apps","Langes Zurück, einzelne Apps oder alle schließen",()->RecentAppsSettings.show(a));
        listRow(list,"grid","Apps verwalten","Sichtbarkeit, Reihenfolge und PIN",()->DesignPopup.openCategory(a,changed,"apps"));
        listRow(list,"settings","Profile & Sicherung","Layouts wechseln, sichern und übertragen",
                () -> { if (a instanceof MainActivity) DesignProfilesPopup.show((MainActivity) a); });
        listRow(list,"folder","Dock-Ordner","Apps im Dock gruppieren",() -> DockFolderSettings.show(a, changed));
        listRow(list,"dock","Layout & Dock","Raster, Navigation und Material",()->DesignPopup.openLayout(a,changed));
        listRow(list,"star","Fokus & Effekte","Rahmen, Glanz, Zoom und Mehrfachauswahl",()->FocusMotionPopup.show(a,changed));
        listRow(list,"tune","Schriften & Icons","Schriftfamilien, App-Namen und Icon-Packs",()->TypographyPopup.show(a,changed));
        listRow(list,"cloud","Uhr, Wetter & Kopfzeile","OpenWeather, Anordnung und Buttons",()->DesignPopup.openCategory(a,changed,"status"));
        listRow(list,"cpu","Widgets","Eigene Quellen und Android-Widgets",()->{if(a instanceof MainActivity)HomeWidgetsPopup.show(a,((MainActivity)a).homeWidgets());});
        listRow(list,"grid","Buttons & Widget-Stil","Gemeinsame und getrennte Themes",()->SurfaceThemePopup.show(a));
        listRow(list,"moon","Bildschirmschoner","Uhr, Szenen, Bilder und Videos",()->SaverSettings.open(a));
        listRow(list,"volume","Ton & HDMI-CEC","Lautstärkeanzeige und Stil",()->DesignPopup.openCategory(a,changed,"audio"));
        listRow(list,"timer","Ruhe & Sleeptimer","Inaktivität und Ausschalt-Timer",()->DesignPopup.openCategory(a,changed,"rest"));
        listRow(list,"video","Content-Vorschau","Kanäle der installierten Apps",()->ContentPreviewSettings.show(a));
        listRow(list,"settings","System","Launcher-Verhalten und Android",()->DesignPopup.openCategory(a,changed,"system"));listRow(list,"text","Info","Luma Dock · Version, Updates und Projekt",()->AppInfoPopup.show(a));list.getChildAt(0).requestFocus();}
    private void listRow(LinearLayout parent,String icon,String title,String subtitle,Runnable use){LinearLayout row=SettingsSheet.row(a,icon,title,subtitle,"›");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);parent.addView(row,lp);row.setOnClickListener(v->use.run());}
    private void wallpaperPage(String tab) {
        reset("Hintergrund");
        if ("YouTube".equals(tab)) { tab = "Live-Hintergründe"; videoPool = "youtube"; }
        String[] tabs = saver ? new String[]{"Bilder", "Live-Hintergründe"}
                : new String[]{"Verläufe", "Bilder", "Live-Hintergründe", "Wechsel"};
        int selected = 0;
        for (int i = 0; i < tabs.length; i++) if (tabs[i].equals(tab)) selected = i;
        SettingsTabs navigation = new SettingsTabs(a, "Hintergrund-Tabs", tabs, selected,
                index -> renderWallpaper(tabs[index], true));
        page.addView(navigation, new LinearLayout.LayoutParams(-1, dp(42)));
        wallpaperBody = new LinearLayout(a);
        wallpaperBody.setOrientation(LinearLayout.VERTICAL);
        wallpaperBody.setBackground(round(0x80131922, 10));
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, 0, 1);
        bodyParams.topMargin = dp(5);
        page.addView(wallpaperBody, bodyParams);
        renderWallpaper(tabs[selected], false);
        navigation.post(navigation::requestFocus);
    }

    private void closeYouTube() {
        if (youtube != null) youtube.close();
        youtube = null;
        youtubeContent = null;
    }

    private void clearGallery(LinearLayout container) {
        galleryGeneration++;
        adapter = null;
        galleryView = null;
        container.removeAllViews();
    }

    private void renderWallpaper(String tab, boolean animate) {
        if (closed || wallpaperBody == null) return;
        int direction = tabIndex(tab) >= tabIndex(wallpaperTab) ? 1 : -1;
        wallpaperTab = tab;
        wallpaperBody.animate().cancel();
        clearGallery(wallpaperBody);
        sheet.hint("◀ ▶: Tab wechseln · ↓ / OK: Inhalt öffnen");
        switch (tab) {
            case "Verläufe": gradients(wallpaperBody); break;
            case "Bilder": photos(wallpaperBody, saver); break;
            case "Wechsel":
                wallpaperBody.addView(BackgroundSwitchPopup.content(a, changed),
                        new LinearLayout.LayoutParams(-1, 0, 1));
                break;
            default: videos(wallpaperBody, saver, videoPool);
        }
        animateContent(wallpaperBody, animate, direction);
        requestPreview();
    }

    private int tabIndex(String tab) {
        switch (tab) { case "Bilder": return 1; case "Live-Hintergründe": return 2; case "Wechsel": return 3; default: return 0; }
    }

    private void animateContent(View view, boolean animate, int direction) {
        view.animate().cancel();
        if (animate && !Config.load(a).reducedMotion) {
            view.setAlpha(.15f);
            view.setTranslationX(dp(12) * direction);
            view.animate().alpha(1f).translationX(0).setDuration(180)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        } else { view.setAlpha(1f); view.setTranslationX(0); }
    }

    private void gradients(LinearLayout content) {
        List<Entry> entries = new ArrayList<>();
        Entry original = new Entry("original", "Theme-Hintergrund");
        original.selected = () -> "original".equals(config.getString(HomeBackdrop.MODE, "original"))
                && !live.getBoolean("enabled", false);
        original.use = () -> HomeBackdrop.select(a, "original", "0");
        entries.add(original);
        for (int i = 0; i < HomeBackdrop.COLORS.length; i++) {
            final int at = i;
            Entry entry = new Entry("gradient" + i, HomeBackdrop.NAMES[i]);
            entry.gradient = i;
            entry.selected = () -> "gradient".equals(config.getString(HomeBackdrop.MODE, "original"))
                    && String.valueOf(at).equals(config.getString(HomeBackdrop.ID, "0")) && !live.getBoolean("enabled", false);
            entry.use = () -> HomeBackdrop.select(a, "gradient", String.valueOf(at));
            entries.add(entry);
        }
        gallery(content, entries, 112, 100);
    }

    private void photos(LinearLayout content, boolean forSaver) {
        List<Entry> entries = new ArrayList<>();
        for (SaverCatalog.Photo photo : SaverCatalog.photos(a)) {
            Entry entry = new Entry(photo.file, photo.title);
            entry.photo = photo.file;
            entry.selected = () -> forSaver ? "photos".equals(SaverPrefs.mode(a))
                    && photo.file.equals(background.getString(SaverPrefs.PHOTO, "w00.jpg"))
                    : "photo".equals(config.getString(HomeBackdrop.MODE, "original"))
                    && photo.file.equals(config.getString(HomeBackdrop.ID, "")) && !live.getBoolean("enabled", false);
            entry.use = () -> { if (forSaver) SaverPrefs.photo(a, photo.file); else HomeBackdrop.select(a, "photo", photo.file); };
            entries.add(entry);
        }
        gallery(content, entries, 134, 100);
    }

    private void videos(LinearLayout content, boolean forSaver, String pool) {
        String[] groups = forSaver ? new String[]{"all", "favorites", "A", "Z", "C"}
                : new String[]{"all", "favorites", "A", "Z", "C", "youtube", "custom"};
        String[] names = Arrays.stream(groups).map(id -> LiveWallpaperSelection.name(a, id)).toArray(String[]::new);
        int selected = 0;
        for (int i = 0; i < groups.length; i++) if (groups[i].equals(pool)) selected = i;
        LinearLayout results = new LinearLayout(a);
        results.setOrientation(LinearLayout.VERTICAL);
        SettingsTabs filters = new SettingsTabs(a, "Live-Quellen", names, selected, index -> {
            String group = groups[index];
            if (!forSaver) {
                videoPool = group;
                if (!"favorites".equals(group) && !"youtube".equals(group)) {
                    live.edit().putString(LiveWallpaperSelection.CATEGORY, group).apply();
                }
            }
            renderVideos(results, forSaver, group);
            animateContent(results, true, 1);
        });
        LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, dp(42));
        filterParams.topMargin = dp(4);
        content.addView(filters, filterParams);
        content.addView(results, new LinearLayout.LayoutParams(-1, 0, 1));
        renderVideos(results, forSaver, groups[selected]);
    }

    private void renderVideos(LinearLayout content, boolean forSaver, String pool) {
        clearGallery(content);
        if ("custom".equals(pool) && !forSaver) {
            sheet.hint("Datei / URL festlegen · Abdunklung direkt ändern");
            content.addView(LiveWallpaperPopup.ownContent(a, changed), new LinearLayout.LayoutParams(-1, 0, 1));
            return;
        }
        if ("youtube".equals(pool) && !forSaver) {
            sheet.hint("OK: YouTube-Vorschau · OK ca. 2 s halten: Zu Favoriten / entfernen");
            if (youtube == null) {
                youtubeContent = new LinearLayout(a);
                youtubeContent.setOrientation(LinearLayout.VERTICAL);
                youtube = YouTubeBrowser.embed(a, changed, youtubeContent);
            }
            if (youtubeContent.getParent() instanceof ViewGroup) ((ViewGroup) youtubeContent.getParent()).removeView(youtubeContent);
            content.addView(youtubeContent, new LinearLayout.LayoutParams(-1, 0, 1));
            return;
        }
        sheet.hint("OK: Auswählen · OK ca. 2 s halten: Favorit ★");
        List<Entry> entries = new ArrayList<>();
        for (LiveWallpaperCatalog.Item item : LiveWallpaperSelection.pool(a, pool)) {
            String youtubeId = YouTubeVideo.id(item.url);
            if (forSaver && !youtubeId.isEmpty()) continue;
            Entry entry = new Entry(item.url, item.title);
            entry.video = item.url;
            entry.origin = "A".equals(item.source) ? "Apple" : "youtube".equals(item.source) ? "YT"
                    : LiveWallpaperCatalog.sourceName(item.source);
            entry.selected = () -> forSaver ? "video".equals(SaverPrefs.mode(a))
                    && item.url.equals(background.getString(SaverPrefs.VIDEO, ""))
                    : live.getBoolean("enabled", false) && item.url.equals(LiveWallpaperPrefs.source(a));
            entry.use = () -> {
                if (forSaver) SaverPrefs.video(a, item.url, item.title);
                else if (!youtubeId.isEmpty()) YouTubePreview.show(a, new YouTubeVideo(youtubeId, item.title, ""), changed);
                else LiveWallpaperPrefs.select(a, item.url, item.title);
            };
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            TextView empty = text("Noch keine Favoriten. In einer Sammlung oder der YouTube-Suche OK ca. 2 Sekunden halten.", 12);
            empty.setPadding(dp(12), dp(15), dp(12), dp(15));
            content.addView(empty);
        } else gallery(content, entries, 134, 100);
    }

    private void trimFavorites() {
        if (closed || adapter == null || saver || !"favorites".equals(videoPool)
                || !"Live-Hintergründe".equals(wallpaperTab) || galleryView == null) return;
        RecyclerView view = galleryView;
        boolean refocus = view.hasFocus();
        View focused = view.getFocusedChild();
        int at = focused == null ? 0 : Math.max(0, view.getChildAdapterPosition(focused));
        if (!adapter.entries.removeIf(entry -> entry.video != null && !LiveWallpaperSelection.isFavorite(a, entry.video))) return;
        adapter.notifyDataSetChanged();
        adapter.count.setText(adapter.entries.isEmpty() ? "0 / 0" : Math.min(at + 1, adapter.entries.size()) + " / " + adapter.entries.size());
        if (refocus && !adapter.entries.isEmpty()) {
            int position = Math.min(at, adapter.entries.size() - 1);
            view.scrollToPosition(position);
            view.post(() -> {
                RecyclerView.ViewHolder holder = view.findViewHolderForAdapterPosition(position);
                if (holder != null) holder.itemView.requestFocus();
            });
        }
    }
    private static final class Entry{final String key,title;String font,sample,photo,video,origin;int gradient=-1;java.util.function.BooleanSupplier selected=()->false;Runnable use=()->{};Entry(String k,String t){key=k;title=t;}}
    private void gallery(LinearLayout parent,List<Entry> entries,int width,int height){int token=++galleryGeneration;TextView count=text("1 / "+entries.size(),10);count.setGravity(Gravity.END);parent.addView(count,new LinearLayout.LayoutParams(-1,dp(17)));RecyclerView rv=new RecyclerView(a);galleryView=rv;rv.setLayoutManager(new GridLayoutManager(a,3){@Override public boolean onRequestChildFocus(RecyclerView parent,RecyclerView.State state,View child,View focused){return true;}});rv.setClipToPadding(true);rv.setPadding(dp(3),dp(3),dp(3),dp(4));rv.setItemAnimator(null);Gallery mounted=new Gallery(entries,width,height,count,token);adapter=mounted;rv.setAdapter(mounted);rv.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int pixels=Math.max(dp(20),(rv.getHeight()-rv.getPaddingTop()-rv.getPaddingBottom())/3-dp(4));if(mounted.cellHeight!=pixels){mounted.cellHeight=pixels;rv.post(()->{if(!closed&&rv.getAdapter()==mounted)mounted.notifyItemRangeChanged(0,mounted.getItemCount(),"size");});}});parent.addView(rv,new LinearLayout.LayoutParams(-1,0,1));}
    private final class Holder extends RecyclerView.ViewHolder{final LinearLayout box;final ImageView image;final TextView sample,label,mark,favorite,origin;final FavoriteOkKey ok;Entry entry;
        Holder(LinearLayout b,ImageView i,TextView s,TextView l,TextView m,TextView f,TextView o){super(b);box=b;image=i;sample=s;label=l;mark=m;favorite=f;origin=o;ok=new FavoriteOkKey(b,()->entry!=null&&entry.video!=null,()->toggleFavorite(entry),(v,key,event)->{if(entry!=null&&entry.video!=null&&key==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())toggleFavorite(entry);return true;}return false;});}}
    private void toggleFavorite(Entry e){if(closed||e==null||e.video==null)return;boolean on=LiveWallpaperSelection.toggle(a,e.video);Toast.makeText(a,on?"Zu Favoriten hinzugefügt":"Aus Favoriten entfernt",Toast.LENGTH_SHORT).show();requestPreview();}
    private final class Gallery extends RecyclerView.Adapter<Holder>{final List<Entry> entries;final int width,height,token;final TextView count;int cellHeight;Gallery(List<Entry> e,int w,int h,TextView c,int t){entries=e;width=w;height=h;count=c;token=t;cellHeight=dp(h);}
        public int getItemCount(){return entries.size();}
        public Holder onCreateViewHolder(ViewGroup parent,int type){LinearLayout b=new LinearLayout(a);b.setId(View.generateViewId());b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(6),dp(6),dp(6),dp(5));b.setFocusable(true);b.setFocusableInTouchMode(true);b.setClickable(true);b.setBackground(cardBackground());RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,cellHeight);lp.setMargins(dp(2),dp(2),dp(2),dp(2));b.setLayoutParams(lp);FrameLayout media=new FrameLayout(a);media.setDuplicateParentStateEnabled(true);ImageView image=new ImageView(a);image.setScaleType(ImageView.ScaleType.CENTER_CROP);media.addView(image,new FrameLayout.LayoutParams(-1,-1));TextView sample=text("",36);sample.setGravity(Gravity.CENTER);sample.setDuplicateParentStateEnabled(true);sample.setTextColor(SettingsSheet.ink(Color.WHITE,0xFF15191F));media.addView(sample,new FrameLayout.LayoutParams(-1,-1));TextView mark=text("✓",13);mark.setGravity(Gravity.CENTER);mark.setBackground(round(0xBB131820,8));media.addView(mark,new FrameLayout.LayoutParams(dp(19),dp(19),Gravity.RIGHT|Gravity.TOP));TextView favorite=text("★",13);favorite.setGravity(Gravity.CENTER);favorite.setTextColor(0xFFFFD58E);favorite.setBackground(round(0xC4131820,8));favorite.setContentDescription("Favorit");media.addView(favorite,new FrameLayout.LayoutParams(dp(19),dp(19),Gravity.LEFT|Gravity.TOP));TextView origin=text("",8);origin.setTextColor(0xFFE8F1FA);origin.setPadding(dp(4),dp(1),dp(4),dp(1));origin.setBackground(round(0xD018202B,4));FrameLayout.LayoutParams originParams=new FrameLayout.LayoutParams(-2,-2,Gravity.RIGHT|Gravity.BOTTOM);originParams.setMargins(dp(3),dp(3),dp(3),dp(3));media.addView(origin,originParams);b.addView(media,new LinearLayout.LayoutParams(-1,0,1));TextView label=text("",10);label.setDuplicateParentStateEnabled(true);label.setTextColor(SettingsSheet.ink(0xFFE5E8ED,0xFF171C24));label.setGravity(Gravity.CENTER);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams ll=new LinearLayout.LayoutParams(-1,dp(17));ll.topMargin=dp(3);b.addView(label,ll);return new Holder(b,image,sample,label,mark,favorite,origin);}
        public void onBindViewHolder(Holder h,int position){if(h.box.getLayoutParams().height!=cellHeight){h.box.getLayoutParams().height=cellHeight;h.box.requestLayout();}Entry e=entries.get(position);h.ok.cancel();h.entry=e;h.box.setTag(e.key);h.label.setText(e.title);h.origin.setText(e.origin);h.origin.setVisibility(e.origin==null?View.GONE:View.VISIBLE);marks(h,e);h.image.setImageDrawable(null);h.sample.setText("");h.sample.setVisibility(View.GONE);
            if(e.font!=null){h.sample.setVisibility(View.VISIBLE);h.sample.setText(e.sample);h.sample.setTypeface(Typography.face(a,e.font,R.string.font_light));}else if(e.gradient>=0)h.image.setImageDrawable(HomeBackdrop.gradient(e.gradient));else if(e.photo!=null||e.video!=null){Bitmap cached=thumbs.get(e.key);if(cached!=null)h.image.setImageBitmap(cached);else loadThumb(h,e,token);}else{h.sample.setVisibility(View.VISIBLE);h.sample.setText("◌");}
            h.box.setOnFocusChangeListener((v,on)->{if(on){int pos=h.getBindingAdapterPosition();if(pos!=RecyclerView.NO_POSITION)count.setText((pos+1)+" / "+entries.size());snapRows(h.box,pos);}else h.ok.cancel();});h.box.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN)v.requestFocus();return false;});h.box.setOnClickListener(v->{e.use.run();for(int i=0;i<entries.size();i++)notifyItemChanged(i,"selection");requestPreview();});
        }
        private void marks(Holder h,Entry e){boolean on=e.selected.getAsBoolean(),star=e.video!=null&&LiveWallpaperSelection.favorites(a).contains(e.video);h.mark.setVisibility(on?View.VISIBLE:View.INVISIBLE);h.box.setSelected(on);h.favorite.setVisibility(star?View.VISIBLE:View.GONE);h.box.setContentDescription(e.title+(e.origin==null?"":" · "+e.origin)+(star?" · Favorit":""));}
        public void onBindViewHolder(Holder h,int pos,List<Object> payloads){if(payloads.isEmpty())onBindViewHolder(h,pos);else{if(h.box.getLayoutParams().height!=cellHeight){h.box.getLayoutParams().height=cellHeight;h.box.requestLayout();}marks(h,entries.get(pos));}}
    }
    private void snapRows(View item,int position){if(!(item.getParent() instanceof RecyclerView))return;RecyclerView rv=(RecyclerView)item.getParent();rv.post(()->{if(closed||!item.hasFocus()||!(rv.getLayoutManager() instanceof GridLayoutManager))return;rv.stopScroll();GridLayoutManager layout=(GridLayoutManager)rv.getLayoutManager();int first=Math.max(0,layout.findFirstVisibleItemPosition()/3),row=position/3;first=Math.min(first,row);first=Math.max(first,row-2);View start=layout.findViewByPosition(first*3);if(start==null||layout.getDecoratedTop(start)!=rv.getPaddingTop())layout.scrollToPositionWithOffset(first*3,0);});}
    private void loadThumb(Holder holder,Entry e,int token){if(closed)return;worker.execute(()->{Bitmap b=null;try{if(e.photo!=null)b=SaverCatalog.thumb(a,e.photo);else if(!YouTubeVideo.id(e.video).isEmpty())b=YouTubeBrowser.fetchThumbnail(YouTubeVideo.id(e.video));else{MessageDigest md=MessageDigest.getInstance("SHA-256");StringBuilder name=new StringBuilder();for(byte n:md.digest(e.video.getBytes("UTF-8")))name.append(String.format(Locale.ROOT,"%02x",n));File file=new File(new File(a.getExternalFilesDir(null),"video-thumbs"),name+".jpg");if(file.isFile()&&file.length()<512*1024){BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.RGB_565;b=BitmapFactory.decodeFile(file.getAbsolutePath(),o);}}}catch(Exception ignored){}Bitmap result=b;a.runOnUiThread(()->{if(closed){if(result!=null)result.recycle();return;}if(result!=null)thumbs.put(e.key,result);if(token==galleryGeneration&&e.key.equals(holder.box.getTag())){if(result!=null)holder.image.setImageBitmap(result);else{holder.sample.setVisibility(View.VISIBLE);holder.sample.setText("▶");holder.image.setContentDescription("Kein lokales Vorschaubild");}}});});}
    private void requestPreview(){if(closed)return;if("Hintergrund".equals(current)&&!saver)heading.setText("Hintergrund");if(adapter!=null)adapter.notifyItemRangeChanged(0,adapter.getItemCount(),"selection");}
}
