package com.gathin.tvhome;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
/** Remote-friendly source picker, independently editable instances and optional combined system display. */
final class HomeWidgetsPopup {
    private final Activity a;private final HomeWidgets widgets;private final SharedPreferences prefs;
    static void show(Activity a,HomeWidgets widgets){if(widgets!=null)new HomeWidgetsPopup(a,widgets).home();}
    private HomeWidgetsPopup(Activity a,HomeWidgets widgets){this.a=a;this.widgets=widgets;prefs=widgets.store.prefs;}
    private String s(int id){return a.getString(id);}
    private String title(HomeWidgetStore.Spec spec){return spec.title.isEmpty()?HomeWidgets.sourceName(a,spec.source):spec.title;}
    private void home(){SettingsSheet p=new SettingsSheet(a,s(R.string.widgets_title));Runnable render=()->{
        p.clearRows();
        p.group(s(R.string.widgets_instances));p.row("grid",s(R.string.widgets_add),s(R.string.widgets_sources_hint),this::sources);
        p.row("grid",s(R.string.widgets_android),a.getString(R.string.widgets_provider_count,widgets.android.providers().size()),widgets.android::choose);
        int n=0;for(HomeWidgetStore.Spec spec:widgets.store.all()){String id=spec.id;p.row(spec.android()?"grid":WidgetSource.find(spec.source).glyph,title(spec)+" · "+(++n),s(spec.enabled?R.string.menu_on:R.string.menu_off)+" · "+HomeWidgets.sourceName(a,spec.source),()->item(id));}
        p.group(s(R.string.system_stats_refresh));p.add(new GridSlider(a,s(R.string.system_stats_interval),5,60,SystemStatsPolicy.seconds(prefs.getInt(SystemStatsWidget.SECONDS,10)),seconds->prefs.edit().putInt(SystemStatsWidget.SECONDS,seconds).apply()));
        p.info(s(R.string.widgets_hint));
    };render.run();SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{if(HomeWidgetStore.ITEMS.equals(key))p.body.post(render);};prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();}
    private void sources(){SettingsSheet p=new SettingsSheet(a,s(R.string.widgets_sources));for(WidgetSource source:WidgetSource.values())p.row(source.glyph,HomeWidgets.sourceName(a,source.id),null,()->{HomeWidgetStore.Spec added=widgets.store.add(source.id);if(added==null){Toast.makeText(a,R.string.widgets_limit,Toast.LENGTH_LONG).show();return;}p.dialog.dismiss();item(added.id);});p.info(s(R.string.widgets_source_note));p.show();}
    private void item(String id){HomeWidgetStore.Spec spec=widgets.spec(id);if(spec==null||widgets.view(id)==null)return;SettingsSheet p=new SettingsSheet(a,title(spec));final GridSlider[] heightControl=new GridSlider[1];
        if(spec.quote())quoteOptions(p,id);
        if("system".equals(spec.source)){
            p.options("cpu","Systeminformationen · Layout",SystemInfoStyle.NAMES,i->SystemInfoStyle.IDS[i].equals(widgets.spec(id).metricsStyle),i->widgets.metricsStyle(id,SystemInfoStyle.IDS[i]));
            p.options("photo","Systeminformationen · Oberfläche",new String[]{"Glas · Transparent","Hell · Bisheriger Stil"},i->(i==0)==widgets.spec(id).metricsGlass,i->widgets.metricsGlass(id,i==0));
            p.add(new GridSlider(a,"Glas · Deckkraft (%)",0,90,spec.metricsOpacity,n->widgets.metricsOpacity(id,n)));
            p.toggle("Netzwerk · Download und Upload","LAN + WLAN zusammen · MB/s (Megabyte pro Sekunde)",spec.network,on->widgets.network(id,on));
            p.info("Netzwerkraten sind Mittelwerte über das Aktualisierungsintervall. Tunnel und Loopback werden nicht doppelt gezählt.");
        }
        LinearLayout shown=p.toggle(s(R.string.header_show),null,spec.enabled,on->widgets.show(id,on));
        p.row("move",s(R.string.header_move),s(R.string.header_move_hint),()->HeaderMoveOverlay.show(a,widgets,id,title(spec),()->{}));
        LinearLayout theme=spec.quote()?null:p.row("photo",s(R.string.widgets_theme),SurfaceThemePopup.name(a,spec.theme),()->SurfaceThemePopup.choose(a,s(R.string.widgets_theme),widgets.spec(id).theme,true,choice->widgets.theme(id,choice)));
        p.add(new GridSlider(a,s(R.string.widgets_width),spec.android()?Math.min(widgets.width(),widgets.android.minWidth(spec.appWidgetId)):spec.quote()?240:64,spec.android()?widgets.width():Math.min(480,widgets.width()),spec.width,n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.resize(id,n,now.android()||now.quote()?now.height:now.metricsHeight);}));
        if(spec.quote())p.add(new GridSlider(a,s(R.string.widgets_height),80,Math.min(500,widgets.height()),spec.height,n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.resize(id,now.width,n);}));
        if(!spec.android()&&!spec.quote()){
            int[] actual=widgets.size(id);int minimum=SystemWidgetPolicy.minimum(spec.source,spec.metricsStyle,((HomeWidgetCard)widgets.view(id)).mask);
            GridSlider height=new GridSlider(a,s(R.string.widgets_height),Math.min(minimum,widgets.height()),widgets.height(),spec.metricsHeight>0?spec.metricsHeight:actual[1],n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.resize(id,now.width,n);}).maxFrom(widgets::height).minFrom(()->{HomeWidgetStore.Spec now=widgets.spec(id);return now==null?32:Math.min(widgets.height(),SystemWidgetPolicy.minimum(now.source,now.metricsStyle,((HomeWidgetCard)widgets.view(id)).mask));});heightControl[0]=height;p.add(height);
            p.row("settings","Widget-Höhe automatisch", "Stellt die passende Höhe für Layout und sichtbare Werte wieder her",()->{widgets.automaticHeight(id);p.body.postDelayed(()->height.syncValue(widgets.size(id)[1]),100);});
        }
        if(spec.android()){p.add(new GridSlider(a,s(R.string.widgets_height),Math.min(widgets.height(),widgets.android.minHeight(spec.appWidgetId)),widgets.height(),spec.height,n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.resize(id,now.width,n);}));p.row("grid",s(R.string.widgets_interact),s(R.string.widgets_interact_hint),()->widgets.interact(id));p.info(s(R.string.widgets_android_theme_hint));}
        int[] point=widgets.position(id),size=widgets.size(id);
        GridSlider x=new GridSlider(a,s(R.string.header_x),0,Math.max(0,widgets.width()-size[0]),point[0],n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.set(id,n,now.y);}).maxFrom(()->Math.max(0,widgets.width()-widgets.size(id)[0]));p.add(x);
        GridSlider y=new GridSlider(a,s(R.string.header_y),0,Math.max(0,widgets.height()-size[1]),point[1],n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.set(id,now.x,n);}).maxFrom(()->Math.max(0,widgets.height()-widgets.size(id)[1]));p.add(y);
        if(!HomeWidgetStore.SUMMARY.equals(id)){p.row("tune",s(R.string.widgets_name),title(spec),()->rename(id));p.row("eye_off",s(R.string.widgets_remove),s(R.string.widgets_remove_hint),()->{widgets.remove(id);p.dialog.dismiss();});}
        p.info(s(R.string.widgets_units));
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{if(key!=null&&(key.startsWith(HomeWidgetStore.P)||key.startsWith("system_stats_")))p.body.postDelayed(()->{HomeWidgetStore.Spec now=widgets.spec(id);if(now==null||widgets.view(id)==null)return;int[] position=widgets.position(id);x.syncValue(position[0]);y.syncValue(position[1]);if(heightControl[0]!=null&&!heightControl[0].hasFocus())heightControl[0].syncValue(widgets.size(id)[1]);SettingsSheet.syncToggle(shown,now.enabled);if(theme!=null)subtitle(theme,SurfaceThemePopup.name(a,now.theme));},50);};prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();}
    private void quoteOptions(SettingsSheet p,String id){
        p.add(new SettingsOptions(a,"photo",s(R.string.quote_display),new String[]{"Text","Bild"},i->(i==1)=="image".equals(widgets.spec(id).quoteMode),i->widgets.store.edit(id,w->w.quoteMode=i==1?"image":"text"),false));
        String[] effects={"fade","pixels","none"};p.add(new SettingsOptions(a,"sparkle",s(R.string.quote_transition),new String[]{"Sanft überblenden","Pixel fallen","Ohne"},i->effects[i].equals(widgets.spec(id).quoteEffect),i->widgets.store.edit(id,w->w.quoteEffect=effects[i]),false));
        java.util.List<String> fonts=new java.util.ArrayList<>();for(String font:FontPolicy.IDS)if(FontPolicy.forAppNames(font))fonts.add(font);p.options("text",s(R.string.quote_font),fonts.stream().map(f->Typography.name(a,f)).toArray(String[]::new),i->fonts.get(i).equals(widgets.spec(id).quoteFont),i->widgets.store.edit(id,w->w.quoteFont=fonts.get(i))).fontPreviews("Aa Äöß",i->Typography.face(a,fonts.get(i),R.string.font_medium));
        p.add(new GridSlider(a,s(R.string.quote_size),12,48,widgets.spec(id).quoteSize,n->widgets.store.edit(id,w->w.quoteSize=n)));
        p.add(new GridSlider(a,s(R.string.quote_interval),1,120,Math.max(1,Math.min(120,prefs.getInt(QuoteFeed.MINUTES,1))),n->prefs.edit().putInt(QuoteFeed.MINUTES,n).apply()));
        p.toggle(s(R.string.quote_background),null,widgets.spec(id).quoteSurface,on->widgets.store.edit(id,w->w.quoteSurface=on));
        String[] themes=new String[SurfaceStyle.IDS.length+1];themes[0]="inherit";System.arraycopy(SurfaceStyle.IDS,0,themes,1,SurfaceStyle.IDS.length);p.options("photo",s(R.string.quote_material),java.util.Arrays.stream(themes).map(t->SurfaceThemePopup.name(a,t)).toArray(String[]::new),i->themes[i].equals(widgets.spec(id).theme),i->widgets.theme(id,themes[i]));
        p.add(new GridSlider(a,s(R.string.quote_opacity),0,100,widgets.spec(id).quoteOpacity,n->widgets.store.edit(id,w->w.quoteOpacity=n)));
        p.options("sparkle",s(R.string.quote_enter_path),new String[]{"Im Widget","Vom oberen Bildschirmrand"},i->(i==1)=="screen".equals(widgets.spec(id).quoteEnterPath),i->widgets.store.edit(id,w->w.quoteEnterPath=i==1?"screen":"widget"));
        p.options("dock",s(R.string.quote_enter_layer),new String[]{"Hinter dem Dock","Vor dem Dock"},i->(i==1)==widgets.spec(id).quoteEnterFront,i->widgets.store.edit(id,w->w.quoteEnterFront=i==1));
        p.options("sparkle",s(R.string.quote_exit_path),new String[]{"Im Widget","Bis zum unteren Bildschirmrand"},i->(i==1)=="screen".equals(widgets.spec(id).quoteExitPath),i->widgets.store.edit(id,w->w.quoteExitPath=i==1?"screen":"widget"));
        p.options("dock",s(R.string.quote_exit_layer),new String[]{"Hinter dem Dock","Vor dem Dock"},i->(i==1)==widgets.spec(id).quoteExitFront,i->widgets.store.edit(id,w->w.quoteExitFront=i==1));
        p.row("refresh",s(R.string.quote_refresh),null,widgets.quotes::refresh);
        p.row("play",s(R.string.quote_demo),s(R.string.quote_demo_hint),()->{HomeWidgetCard card=(HomeWidgetCard)widgets.view(id);if(card!=null&&card.quote!=null)card.quote.preview();});
        p.info(s(R.string.quote_hint));
    }
    private void rename(String id){HomeWidgetStore.Spec spec=widgets.spec(id);if(spec==null)return;LinearLayout panel=SettingsSheet.panel(a);int pad=SettingsSheet.dp(a,18);panel.setPadding(pad,pad,pad,pad);panel.addView(GlassDialog.text(a,s(R.string.widgets_name),18));EditText edit=new EditText(a);edit.setSingleLine(true);edit.setTextColor(Color.WHITE);edit.setTextSize(16);edit.setSelectAllOnFocus(true);edit.setText(spec.title);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(48)});edit.setBackgroundResource(R.drawable.weather_field);panel.addView(edit,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,48)));TextView save=DesignPopup.button(a,s(R.string.dock_style_use));panel.addView(save,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,36)));Dialog dialog=GlassDialog.show(a,panel,340,170);save.setOnClickListener(v->{widgets.store.edit(id,w->w.title=edit.getText().toString().trim());dialog.dismiss();});dialog.setOnDismissListener(d->{android.view.inputmethod.InputMethodManager ime=a.getSystemService(android.view.inputmethod.InputMethodManager.class);if(ime!=null)ime.hideSoftInputFromWindow(edit.getWindowToken(),0);});save.requestFocus();}
    private static void subtitle(LinearLayout row,String value){((TextView)((LinearLayout)row.getChildAt(1)).getChildAt(1)).setText(value);}
}
