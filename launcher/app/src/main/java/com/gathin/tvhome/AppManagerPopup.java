package com.gathin.tvhome;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
/** Remote-first app rows with independent icon, name, visibility, lock and move controls. */
final class AppManagerPopup {
    private final Runnable changed;private final Activity a;private final AppAppearance draft;private Dialog dialog;private final LinearLayout list;private final ScrollView scroll;private final TextView hint,pin;
    private AppAppearance.Item moving;private int moveStart;private final List<View> handles=new ArrayList<>();
    static void show(Activity a,AppAppearance draft,Runnable changed){PinDialog.authorize(a,a.getString(R.string.design_apps),()->new AppManagerPopup(a,draft,changed));}
    private AppManagerPopup(Activity activity,AppAppearance data,Runnable callback){
        changed=callback;
        a=activity;draft=data;LinearLayout panel=GlassDialog.panel(a);
        LinearLayout head=new LinearLayout(a);head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(GlassDialog.text(a,a.getString(R.string.design_apps),19),new LinearLayout.LayoutParams(0,-2,1));
        pin=DesignPopup.button(a,a.getString(AppLock.configured(a)?R.string.apps_pin_change:R.string.apps_pin_create));pin.setTextSize(12);head.addView(pin,new LinearLayout.LayoutParams(dp(130),dp(28)));panel.addView(head);
        pin.setOnClickListener(v->{if(AppLock.configured(a))pinOptions();else PinDialog.create(a,this::pinLabel);});
        hint=GlassDialog.text(a,a.getString(R.string.apps_manage_hint),10);hint.setTextColor(0xFFACB9CD);hint.setPadding(0,dp(7),0,dp(7));panel.addView(hint);
        scroll=new ScrollView(a);scroll.setFillViewport(false);list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=GlassDialog.text(a,a.getString(R.string.apps_apply_hint),9);footer.setTextColor(0xFFACB9CD);footer.setPadding(0,dp(8),0,dp(8));panel.addView(footer);
        TextView done=DesignPopup.button(a,a.getString(R.string.apps_done));panel.addView(done,new LinearLayout.LayoutParams(-1,dp(30)));done.setOnClickListener(v->{moving=null;dialog.dismiss();});
        render(null);dialog=GlassDialog.show(a,panel,700,480);
        dialog.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&moving!=null){if(event.getAction()==KeyEvent.ACTION_UP){AppAppearance.Item item=moving;draft.items.remove(item);draft.items.add(moveStart,item);moving=null;hint.setText(R.string.apps_manage_hint);render(item);changed.run();}return true;}return false;});
        pin.requestFocus();
    }
    private int dp(float n){return GlassDialog.dp(a,n);}
    private void pinLabel(){pin.setText(a.getString(AppLock.configured(a)?R.string.apps_pin_change:R.string.apps_pin_create));}
    private void pinOptions(){
        LinearLayout p=GlassDialog.panel(a);p.addView(GlassDialog.text(a,a.getString(R.string.apps_pin_title),22));
        TextView change=DesignPopup.button(a,a.getString(R.string.apps_pin_change)),remove=DesignPopup.button(a,a.getString(R.string.apps_pin_remove));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(38));lp.topMargin=dp(12);p.addView(change,lp);p.addView(remove,lp);
        Dialog options=GlassDialog.show(a,p,350,185);change.setOnClickListener(v->{options.dismiss();PinDialog.change(a,this::pinLabel);});
        remove.setOnClickListener(v->{options.dismiss();PinDialog.authorize(a,a.getString(R.string.apps_pin_remove),()->{AppLock.prefs(a).edit().clear().apply();draft.locked.clear();a.getSharedPreferences(Config.SETTINGS_NAME,0).edit().remove(AppLock.LOCKS).apply();pinLabel();render(null);changed.run();});});change.requestFocus();
    }
    private TextView action(String glyph,String description,boolean active){
        TextView b=DesignPopup.button(a,"");Drawable d=new AppGlyph(glyph);d.setBounds(0,0,dp(17),dp(17));b.setCompoundDrawables(d,null,null,null);b.setCompoundDrawableTintList(b.getTextColors());b.setPadding(dp(8),0,dp(8),0);b.setActivated(active);b.setContentDescription(description);return b;
    }
    private void render(AppAppearance.Item focus){
        int y=scroll.getScrollY();list.removeAllViews();handles.clear();
        for(AppAppearance.Item item:draft.items){
            LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(6),dp(3),dp(6),dp(3));
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(item==moving?0x443C96D8:0x18FFFFFF);bg.setCornerRadius(dp(10));if(item==moving)bg.setStroke(dp(1),0xFF8BCFFF);row.setBackground(bg);
            ImageView image=new ImageView(a);image.setId(View.generateViewId());image.setFocusable(true);image.setFocusableInTouchMode(true);image.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_DOWN)v.requestFocus();return false;});image.setClickable(true);image.setBackgroundResource(R.drawable.pill_bg);image.setPadding(dp(5),dp(5),dp(5),dp(5));image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription(a.getString(R.string.apps_icon_for,draft.name(item)));
            Drawable icon=AppAppearance.icon(a,item.pkg,draft.icons.get(item.pkg));image.setImageDrawable(icon==null?item.info.loadIcon(a.getPackageManager()):icon);if(icon instanceof AppGlyph)image.setImageTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{0xFF111827,Color.WHITE}));row.addView(image,new LinearLayout.LayoutParams(dp(34),dp(34)));image.setOnClickListener(v->chooseIcon(item,image));
            TextView name=DesignPopup.button(a,draft.name(item));name.setTextSize(12);name.setGravity(Gravity.CENTER_VERTICAL);name.setPadding(dp(12),0,dp(10),0);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setContentDescription(a.getString(R.string.apps_rename_for,draft.name(item)));
            LinearLayout.LayoutParams nameLp=new LinearLayout.LayoutParams(0,dp(34),1);nameLp.setMargins(dp(8),0,dp(10),0);row.addView(name,nameLp);name.setOnClickListener(v->rename(item,name));
            boolean visible=!draft.hidden.contains(item.pkg),locked=draft.locked.contains(item.pkg);
            TextView eye=action(visible?"eye":"eye_off",a.getString(visible?R.string.apps_hide:R.string.apps_show,draft.name(item)),visible);
            TextView lock=action(locked?"lock":"unlock",a.getString(locked?R.string.apps_unlock:R.string.apps_lock,draft.name(item)),locked);
            TextView move=action("move",a.getString(R.string.apps_move,draft.name(item)),item==moving);handles.add(move);
            for(TextView b:new TextView[]{eye,lock,move}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(34),dp(32));lp.setMarginStart(dp(6));row.addView(b,lp);}
            eye.setOnClickListener(v->{if(draft.hidden.contains(item.pkg))draft.hidden.remove(item.pkg);else draft.hidden.add(item.pkg);boolean on=!draft.hidden.contains(item.pkg);eye.setActivated(on);eye.setContentDescription(a.getString(on?R.string.apps_hide:R.string.apps_show,draft.name(item)));Drawable d=new AppGlyph(on?"eye":"eye_off");d.setBounds(0,0,dp(17),dp(17));eye.setCompoundDrawables(d,null,null,null);changed.run();});
            lock.setOnClickListener(v->{Runnable toggle=()->{if(draft.locked.contains(item.pkg))draft.locked.remove(item.pkg);else draft.locked.add(item.pkg);boolean on=draft.locked.contains(item.pkg);lock.setActivated(on);lock.setContentDescription(a.getString(on?R.string.apps_unlock:R.string.apps_lock,draft.name(item)));Drawable d=new AppGlyph(on?"lock":"unlock");d.setBounds(0,0,dp(17),dp(17));lock.setCompoundDrawables(d,null,null,null);pinLabel();changed.run();};if(AppLock.configured(a))toggle.run();else PinDialog.create(a,toggle);});
            move.setOnClickListener(v->{if(moving==item){moving=null;hint.setText(R.string.apps_manage_hint);}else{moving=item;moveStart=draft.items.indexOf(item);hint.setText(R.string.apps_moving_hint);}render(item);});
            move.setOnKeyListener((v,key,event)->{if(moving!=item)return false;if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){if(event.getAction()==KeyEvent.ACTION_DOWN){int from=draft.items.indexOf(item),to=Math.max(0,Math.min(draft.items.size()-1,from+(key==KeyEvent.KEYCODE_DPAD_UP?-1:1)));if(to!=from){draft.items.remove(from);draft.items.add(to,item);render(item);changed.run();}}return true;}return key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;});
            LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,dp(40));rowLp.bottomMargin=dp(4);list.addView(row,rowLp);
            if(item==focus)move.post(move::requestFocus);
        }
        if(focus==null)scroll.post(()->scroll.scrollTo(0,y));
    }
    private void rename(AppAppearance.Item item,TextView label){
        LinearLayout p=GlassDialog.panel(a);p.addView(GlassDialog.text(a,a.getString(R.string.apps_rename),21));
        EditText input=new EditText(a);input.setText(draft.name(item));input.setSingleLine(true);input.setTextColor(Color.WHITE);input.setTextSize(17);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});input.setBackgroundResource(R.drawable.weather_field);input.setSelectAllOnFocus(true);
        LinearLayout.LayoutParams fieldLp=new LinearLayout.LayoutParams(-1,dp(44));fieldLp.topMargin=dp(14);fieldLp.bottomMargin=dp(14);p.addView(input,fieldLp);
        LinearLayout buttons=new LinearLayout(a);TextView reset=DesignPopup.button(a,a.getString(R.string.apps_original)),ok=DesignPopup.button(a,a.getString(R.string.apps_done));buttons.addView(reset,new LinearLayout.LayoutParams(0,dp(38),1));LinearLayout.LayoutParams bl=new LinearLayout.LayoutParams(0,dp(38),1);bl.setMarginStart(dp(10));buttons.addView(ok,bl);p.addView(buttons);
        Dialog d=GlassDialog.show(a,p,420,220);Runnable save=()->{String value=input.getText().toString().trim();if(value.isEmpty()||value.equals(item.original))draft.labels.remove(item.pkg);else draft.labels.put(item.pkg,Cards.cleanLabel(value,item.pkg));label.setText(draft.name(item));changed.run();InputMethodManager keyboard=(InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);d.dismiss();};ok.setOnClickListener(v->save.run());reset.setOnClickListener(v->{input.setText(item.original);save.run();});input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);input.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){save.run();return true;}return false;});ok.requestFocus();
    }
    private void chooseIcon(AppAppearance.Item item,ImageView image){
        LinearLayout p=GlassDialog.panel(a);p.addView(GlassDialog.text(a,a.getString(R.string.apps_choose_icon),22));
        String[] ids={"original","app","video","music","game","tv","star","kids","photo"};int[] names={R.string.apps_original,R.string.apps_app_icon,R.string.apps_icon_video,R.string.apps_icon_music,R.string.apps_icon_game,R.string.apps_icon_tv,R.string.apps_icon_star,R.string.apps_icon_kids,R.string.apps_icon_photo};
        TextView[] choices=new TextView[ids.length];
        for(int r=0;r<3;r++){LinearLayout line=new LinearLayout(a);for(int c=0;c<3;c++){int index=r*3+c;TextView b=DesignPopup.button(a,a.getString(names[index]));b.setTextSize(12);b.setActivated(ids[index].equals(draft.icons.getOrDefault(item.pkg,"original")));Drawable glyph=AppAppearance.icon(a,item.pkg,ids[index]);if(glyph==null)glyph=item.info.loadIcon(a.getPackageManager());glyph.setBounds(0,0,dp(28),dp(28));b.setCompoundDrawables(null,glyph,null,null);if(glyph instanceof AppGlyph)b.setCompoundDrawableTintList(b.getTextColors());b.setCompoundDrawablePadding(dp(5));b.setPadding(0,dp(12),0,dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(78),1);lp.setMargins(dp(4),dp(5),dp(4),dp(5));line.addView(b,lp);choices[index]=b;}p.addView(line);}
        TextView cancel=DesignPopup.button(a,a.getString(R.string.apps_done));p.addView(cancel,new LinearLayout.LayoutParams(-1,dp(34)));Dialog d=GlassDialog.show(a,p,450,365);if(d.getWindow()!=null)d.getWindow().setDimAmount(.12f);cancel.setOnClickListener(v->d.dismiss());
        for(int i=0;i<choices.length;i++){final String id=ids[i];choices[i].setOnClickListener(v->{if(id.equals("original"))draft.icons.remove(item.pkg);else draft.icons.put(item.pkg,id);Drawable icon=AppAppearance.icon(a,item.pkg,id);image.setImageDrawable(icon==null?item.info.loadIcon(a.getPackageManager()):icon);image.setImageTintList(icon instanceof AppGlyph?new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{0xFF111827,Color.WHITE}):null);changed.run();for(int n=0;n<choices.length;n++){boolean selected=ids[n].equals(id);choices[n].setActivated(selected);choices[n].setText(a.getString(names[n])+(selected?" ✓":""));}});}for(int n=0;n<choices.length;n++)if(ids[n].equals(draft.icons.getOrDefault(item.pkg,"original")))choices[n].setText(a.getString(names[n])+" ✓");choices[0].requestFocus();
    }
}
