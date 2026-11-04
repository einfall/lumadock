package com.gathin.tvhome;
import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
/** Compact glass list over the real Home or saver; values apply without rebuilding the window. */
final class SettingsSheet {
    private static final java.util.Map<Activity,java.util.List<java.lang.ref.WeakReference<Dialog>>> WINDOWS=new java.util.WeakHashMap<>();
    private static final java.util.Map<Dialog,Boolean> SHEETS=new java.util.WeakHashMap<>();
    private static final java.util.Map<Dialog,java.lang.ref.WeakReference<View>> SAVED_FOCUS=new java.util.WeakHashMap<>();
    static void track(Activity a,Dialog dialog){NavigationSounds.attach(a,dialog.getWindow());WINDOWS.computeIfAbsent(a,k->new java.util.ArrayList<>()).add(new java.lang.ref.WeakReference<>(dialog));}
    private static final java.util.Set<Activity> CLOSING=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    static void dismissAll(Activity a){CLOSING.add(a);try{java.util.List<java.lang.ref.WeakReference<Dialog>> list=WINDOWS.remove(a);if(list!=null)for(int i=list.size()-1;i>=0;i--){Dialog d=list.get(i).get();if(d!=null&&d.getWindow()!=null&&d.getWindow().getDecorView().isAttachedToWindow())d.dismiss();}}finally{CLOSING.remove(a);}}
    static java.util.List<Dialog> hideForPlacement(Activity a){java.util.List<Dialog> hidden=new java.util.ArrayList<>();java.util.List<java.lang.ref.WeakReference<Dialog>> list=WINDOWS.get(a);if(list!=null)for(java.lang.ref.WeakReference<Dialog> ref:list){Dialog d=ref.get();if(d!=null&&d.isShowing()&&d.getWindow()!=null&&d.getWindow().getDecorView().getVisibility()==View.VISIBLE){View focus=d.getCurrentFocus();if(focus!=null)SAVED_FOCUS.put(d,new java.lang.ref.WeakReference<>(focus));d.hide();hidden.add(d);}}return hidden;}
    // hide() makes isShowing() false too. Attachment distinguishes a hidden window from a dismissed one.
    static void restoreAfterPlacement(java.util.List<Dialog> hidden){for(Dialog d:hidden)if(d.getWindow()!=null&&d.getWindow().getDecorView().isAttachedToWindow()){d.show();java.lang.ref.WeakReference<View> saved=SAVED_FOCUS.remove(d);View focus=saved==null?null:saved.get();if(focus!=null&&focus.isAttachedToWindow()){focus.requestFocus();focus.post(()->{if(focus.isAttachedToWindow()&&focus.isShown())focus.requestFocus();});}}}
    static void resizeAll(Activity a){java.util.List<java.lang.ref.WeakReference<Dialog>> list=WINDOWS.get(a);if(list!=null)for(java.lang.ref.WeakReference<Dialog> ref:list){Dialog d=ref.get();if(d!=null&&d.isShowing()&&SHEETS.containsKey(d)&&d.getWindow()!=null)size(a,d.getWindow());}}
    private static void size(Activity a,Window w){int panelWidth=a.getSharedPreferences(Config.SETTINGS_NAME,0).getBoolean("visual_menu_compact",false)?380:424;w.setLayout(dp(a,Math.min(panelWidth,width(a)-32)),Math.max(dp(a,260),a.getResources().getDisplayMetrics().heightPixels-dp(a,28)));}
    private static int width(Activity a){return Math.round(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);}
    final Activity a;final Dialog dialog;final LinearLayout body;private final TextView backButton,footer;private final LinearLayout panel;private final FrameLayout host,stage,accessory;private java.util.List<Dialog> parents=java.util.Collections.emptyList();
    SettingsSheet(Activity a,String title){
        this.a=a;dialog=new Dialog(a){@Override public void dismiss(){boolean attached=getWindow()!=null&&getWindow().getDecorView().isAttachedToWindow();super.dismiss();if(attached&&!CLOSING.contains(a)&&!a.isFinishing()&&!a.isDestroyed())restoreAfterPlacement(parents);}};track(a,dialog);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(a,14),dp(a,12),dp(a,14),dp(a,10));GradientDrawable glass=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xE6333840,0xF0191E25,0xF010141B});glass.setCornerRadius(dp(a,15));glass.setStroke(dp(a,.65f),0x44FFFFFF);panel.setBackground(glass);
        LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=DesignPopup.button(a,"‹");backButton=back;back.setTextSize(23);back.setBackground(rowBackground(a));head.addView(back,new LinearLayout.LayoutParams(dp(a,32),dp(a,32)));back.setContentDescription(a.getString(R.string.menu_back));back.setOnClickListener(v->dialog.dismiss());
        TextView heading=GlassDialog.text(a,title,18);heading.setSingleLine(true);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.setMarginStart(dp(a,10));head.addView(heading,hp);
        TextView full=DesignPopup.button(a,a.getString(R.string.visual_preview));full.setText("Vorschau");full.setTextSize(10);full.setBackground(rowBackground(a));full.setOnClickListener(v->SettingsPreview.show(a));head.addView(full,new LinearLayout.LayoutParams(dp(a,70),dp(a,30)));panel.addView(head,new LinearLayout.LayoutParams(-1,dp(a,36)));SHEETS.put(dialog,true);
        stage=new FrameLayout(a);stage.setVisibility(View.GONE);accessory=new FrameLayout(a);stage.addView(accessory,new FrameLayout.LayoutParams(-1,-1));panel.addView(stage,new LinearLayout.LayoutParams(-1,dp(a,90)));
        dialog.setOnKeyListener((d,key,event)->{if(key!=KeyEvent.KEYCODE_MENU)return false;if(event.getAction()==KeyEvent.ACTION_UP)SettingsPreview.show(a);return true;});
        host=new FrameLayout(a);LinearLayout.LayoutParams content=new LinearLayout.LayoutParams(-1,0,1);content.topMargin=dp(a,8);panel.addView(host,content);
        ScrollView scroll=new ScrollView(a);scroll.setVerticalScrollBarEnabled(false);scroll.setClipToPadding(false);body=new LinearLayout(a);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);host.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        footer=GlassDialog.text(a,"Live-Vorschau · ◀ ▶ ändern · OK wählen · MENÜ: Vollbild",9);footer.setTextColor(0xFFADB4BE);footer.setPadding(dp(a,6),dp(a,6),0,0);panel.addView(footer);dialog.setContentView(panel);
    }
    void hint(String text){footer.setText(text);}
    void backAction(Runnable action){backButton.setOnClickListener(v->action.run());}
    void content(View view){host.removeAllViews();host.addView(view,new FrameLayout.LayoutParams(-1,-1));}
    void preview(View view,int height){accessory.removeAllViews();accessory.addView(view,new FrameLayout.LayoutParams(-1,-1));stage.getLayoutParams().height=dp(a,Math.min(height,112));stage.setVisibility(View.VISIBLE);}
    void show(){parents=hideForPlacement(a);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;lp.x=dp(a,16);lp.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;w.setAttributes(lp);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}dialog.show();if(w!=null)size(a,w);for(int i=0;i<body.getChildCount();i++)if(body.getChildAt(i).isFocusable()){body.getChildAt(i).requestFocus();break;}}
    void group(String label){TextView t=GlassDialog.text(a,label,9);t.setTextColor(0xFF85909F);t.setLetterSpacing(.08f);t.setPadding(dp(a,7),dp(a,6),0,dp(a,4));body.addView(t);}
    void info(String label){TextView t=GlassDialog.text(a,label,11);t.setTextColor(0xFFA9B5C6);t.setPadding(dp(a,7),dp(a,5),dp(a,7),dp(a,7));body.addView(t);}
    LinearLayout row(String icon,String title,String subtitle,Runnable click){LinearLayout row=row(a,icon,title,subtitle,"›");add(row);row.setOnClickListener(v->click.run());return row;}
    private final java.util.List<Runnable> choiceUpdates=new java.util.ArrayList<>();
    /** Live choice: keep this window, row focus and scroll position while its value changes. */
    LinearLayout choice(String icon,String title,String subtitle,java.util.function.BooleanSupplier selected,Runnable use){
        LinearLayout row=row(icon,title,subtitle,()->{use.run();refreshChoices();});
        TextView mark=(TextView)row.getChildAt(row.getChildCount()-1);
        Runnable update=()->{boolean on=selected.getAsBoolean();mark.setText(on?"✓":"");row.setSelected(on);};
        choiceUpdates.add(update);update.run();return row;
    }
    SettingsOptions options(String icon,String title,String[] names,java.util.function.IntPredicate selected,java.util.function.IntConsumer change){return options(icon,title,names,selected,change,false);}
    SettingsOptions options(String icon,String title,String[] names,java.util.function.IntPredicate selected,java.util.function.IntConsumer change,boolean multiple){SettingsOptions row=new SettingsOptions(a,icon,title,names,selected,change,multiple);add(row);choiceUpdates.add(row::refresh);return row;}
    void refreshChoices(){for(Runnable update:choiceUpdates)update.run();}
    void clearRows(){choiceUpdates.clear();body.removeAllViews();}
    void add(View row){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(a,3);body.addView(row,lp);}
    interface Toggle {void changed(boolean on);}
    private static final class ToggleState{final boolean[] value;final Runnable render;ToggleState(boolean[] value,Runnable render){this.value=value;this.render=render;}}
    static void syncToggle(LinearLayout row,boolean on){Object state=row.getTag();if(state instanceof ToggleState){ToggleState t=(ToggleState)state;t.value[0]=on;t.render.run();}}
    LinearLayout toggle(String title,String subtitle,boolean on,Toggle changed){
        LinearLayout row=row(a,MenuIcon.of(title),title,subtitle,"");TextView check=(TextView)row.getChildAt(row.getChildCount()-1);boolean[] value={on};Runnable render=()->{check.setText(value[0]?"✓":"");GradientDrawable bg=new GradientDrawable();bg.setColor(value[0]?0xFF8DBCF4:0xFF333D4B);bg.setCornerRadius(dp(a,3));check.setBackground(bg);check.setTextColor(0xFF142639);check.setTextSize(13);check.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(a,17),dp(a,17));lp.setMarginStart(dp(a,12));check.setLayoutParams(lp);row.setContentDescription(title+" · "+a.getString(value[0]?R.string.menu_on:R.string.menu_off));};render.run();row.setOnClickListener(v->{value[0]=!value[0];render.run();changed.changed(value[0]);});row.setTag(new ToggleState(value,render));add(row);return row;
    }
    static LinearLayout panel(Activity a){LinearLayout p=new LinearLayout(a);p.setOrientation(LinearLayout.VERTICAL);GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFF11171F,0xFF090D13});bg.setCornerRadius(dp(a,18));bg.setStroke(dp(a,.5f),0x22FFFFFF);p.setBackground(bg);return p;}
    static ColorStateList ink(int normal,int focused){return new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{focused,normal});}
    static android.graphics.drawable.Drawable rowBackground(Activity a){StateListDrawable states=new StateListDrawable();GradientDrawable focus=new GradientDrawable();focus.setColor(0xFFF5F6F8);focus.setCornerRadius(dp(a,8));states.addState(new int[]{android.R.attr.state_focused},focus);GradientDrawable normal=new GradientDrawable();normal.setColor(0x12FFFFFF);normal.setStroke(dp(a,.5f),0x12FFFFFF);normal.setCornerRadius(dp(a,8));states.addState(new int[]{},normal);return states;}
    static LinearLayout row(Activity a,String icon,String title,String subtitle,String value){
        LinearLayout row=new LinearLayout(a);row.setId(View.generateViewId());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(a,10),dp(a,8),dp(a,10),dp(a,8));row.setMinimumHeight(dp(a,48));row.setFocusable(true);row.setFocusableInTouchMode(true);row.setClickable(true);row.setBackground(rowBackground(a));row.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN)v.requestFocus();return false;});
        if(icon!=null){ImageView image=new ImageView(a);image.setImageDrawable(new AppGlyph(icon));image.setDuplicateParentStateEnabled(true);image.setImageTintList(ink(0xFFADB7C4,0xFF303740));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(a,19),dp(a,19));ip.setMarginEnd(dp(a,12));row.addView(image,ip);}
        LinearLayout text=new LinearLayout(a);text.setOrientation(LinearLayout.VERTICAL);text.setDuplicateParentStateEnabled(true);TextView h=GlassDialog.text(a,title,13);h.setDuplicateParentStateEnabled(true);h.setTextColor(ink(Color.WHITE,0xFF171D25));text.addView(h);if(subtitle!=null&&!subtitle.isEmpty()){TextView sub=GlassDialog.text(a,subtitle,10);sub.setDuplicateParentStateEnabled(true);sub.setTextColor(ink(0xFF8E9BAC,0xFF525C69));sub.setPadding(0,dp(a,2),0,0);text.addView(sub);}row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView arrow=GlassDialog.text(a,value,15);arrow.setDuplicateParentStateEnabled(true);arrow.setTextColor(ink(0xFF8D9DAF,0xFF44566B));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-2,-2);ap.setMarginStart(dp(a,12));row.addView(arrow,ap);return row;
    }
    static int dp(Activity a,float n){return Math.round(a.getResources().getDisplayMetrics().density*n);}
}
