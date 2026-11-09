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
        java.util.List<View> tiles=new java.util.ArrayList<>();
        int n=0;
        for(HomeWidgetStore.Spec spec:widgets.store.all()){
            String detail=spec.temporal()?spec.timeStyle.label:spec.system()?HomeWidgets.metricNames(a,spec.metricsMask):HomeWidgets.sourceName(a,spec.source);
            tiles.add(WidgetPreview.tile(a,preview(spec),title(spec)+" · "+(++n),
                    s(spec.enabled?R.string.menu_on:R.string.menu_off)+" · "+detail,()->item(spec.id)));
        }
        grid(p,tiles);
        p.group(s(R.string.system_stats_refresh));p.add(new GridSlider(a,s(R.string.system_stats_interval),5,60,SystemStatsPolicy.seconds(prefs.getInt(SystemStatsWidget.SECONDS,10)),seconds->prefs.edit().putInt(SystemStatsWidget.SECONDS,seconds).apply()));
        p.info(s(R.string.widgets_hint));
    };render.run();SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{if(HomeWidgetStore.ITEMS.equals(key))p.body.post(render);};prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();}
    private View preview(HomeWidgetStore.Spec spec){
        View fallback=spec.android()?widgets.android.preview(spec.appWidgetId):WidgetPreview.sample(a,spec);
        return WidgetPreview.live(a,()->widgets.view(spec.id),fallback);
    }
    private void sources(){
        SettingsSheet p=new SettingsSheet(a,s(R.string.widgets_sources));
        java.util.List<View> clocks=timeTiles(p,WidgetSource.CLOCK);
        timeTiles(p,WidgetSource.CALENDAR);
        p.group("WEITERE WIDGETS");
        java.util.List<View> others=new java.util.ArrayList<>();
        WidgetSource[] sources={WidgetSource.SYSTEM,WidgetSource.QUOTE,WidgetSource.TEXT,WidgetSource.ECHO};
        String[] descriptions={"CPU, RAM, Speicher und Netzwerk","Als Text oder Bild","Dein eigener Text","Echo-Wiedergabe über Home Assistant"};
        for(int i=0;i<sources.length;i++){
            WidgetSource source=sources[i];
            HomeWidgetStore.Spec existing=source==WidgetSource.SYSTEM?widgets.store.system():null;
            HomeWidgetStore.Spec example=new HomeWidgetStore.Spec("",source.id);
            if(example.system()){example.width=320;example.metricsStyle="resources";}
            View view=existing==null?WidgetPreview.sample(a,example):preview(existing);
            others.add(WidgetPreview.tile(a,view,HomeWidgets.sourceName(a,source.id),
                    existing==null?descriptions[i]:"Vorhandenes Widget bearbeiten",()->{
                if(existing==null)add(p,source,null);
                else {p.dialog.dismiss();item(existing.id);}
            }));
        }
        grid(p,others);
        p.info(s(R.string.widgets_source_note));
        p.show();
        clocks.get(0).requestFocus();
    }
    private java.util.List<View> timeTiles(SettingsSheet p,WidgetSource source){
        p.group(HomeWidgets.sourceName(a,source.id).toUpperCase(java.util.Locale.ROOT));
        java.util.List<View> tiles=new java.util.ArrayList<>();
        for(TimeWidgetStyle style:TimeWidgetStyle.forSource(source==WidgetSource.CALENDAR)){
            HomeWidgetStore.Spec sample=new HomeWidgetStore.Spec("",source.id);
            sample.timeStyle=style;
            tiles.add(WidgetPreview.tile(a,WidgetPreview.sample(a,sample),style.label,style.detail,()->add(p,source,style)));
        }
        grid(p,tiles);
        return tiles;
    }
    private void add(SettingsSheet p,WidgetSource source,TimeWidgetStyle style){
        HomeWidgetStore.Spec added=widgets.store.add(source.id,style);
        if(added==null){Toast.makeText(a,R.string.widgets_limit,Toast.LENGTH_LONG).show();return;}
        p.dialog.dismiss();
        item(added.id);
    }
    private void grid(SettingsSheet p,java.util.List<View> tiles){
        for(int i=0;i<tiles.size();i+=2){
            LinearLayout row=new LinearLayout(a);
            row.setBaselineAligned(false);
            LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);
            left.setMarginEnd(SettingsSheet.dp(a,3));
            row.addView(tiles.get(i),left);
            row.addView(i+1<tiles.size()?tiles.get(i+1):new View(a),new LinearLayout.LayoutParams(0,-2,1));
            p.add(row);
        }
    }
    private void item(String id){HomeWidgetStore.Spec spec=widgets.spec(id);if(spec==null||widgets.view(id)==null)return;SettingsSheet p=new SettingsSheet(a,title(spec));final GridSlider[] heightControl=new GridSlider[1];
        p.preview(preview(spec),88);
        if(spec.quote())quoteOptions(p,id);if(spec.text())textOptions(p,id);
        if(spec.temporal()){
            TimeWidgetStyle[] styles=TimeWidgetStyle.forSource(spec.calendar());
            String[] names=java.util.Arrays.stream(styles).map(style->style.label).toArray(String[]::new);
            p.options("timer","Darstellung",names,i->widgets.spec(id).timeStyle==styles[i],
                    i->widgets.store.edit(id,w->w.timeStyle=styles[i]));
        }
        if (spec.echo()) {
            EchoLayout[] layouts = EchoLayout.values();
            String[] names = java.util.Arrays.stream(layouts).map(layout -> layout.label).toArray(String[]::new);
            p.options("grid", "Wiedergabe-Layout", names, i -> widgets.spec(id).echoLayout == layouts[i],
                    i -> widgets.store.edit(id, widget -> {
                        EchoLayout chosen = layouts[i];
                        widget.echoLayout = chosen;
                        if (chosen != EchoLayout.AUTO) {
                            widget.width = Math.min(chosen.width, widgets.width());
                            widget.height = Math.min(chosen.height, widgets.height());
                        }
                    }));
            p.info("Automatisch wählt das Layout passend zu Breite und Höhe.");
            p.row("music", "Mediaplayer wechseln", "Geräte mit dem gespeicherten Zugang auswählen",
                    () -> EchoSetup.switchPlayer(a, widgets.echo));
            p.row("network", "Home Assistant verbinden", "Adresse und Zugriffstoken einrichten oder ändern",
                    () -> EchoSetup.show(a, widgets.echo));
            p.row("refresh", "Wiedergabe aktualisieren", null, widgets.echo::refresh);
            p.row("eye_off", "Home Assistant trennen", null,
                    () -> DesignDialogs.confirm(a, "Home Assistant trennen", "Die gespeicherte Verbindung entfernen?",
                            "Trennen", () -> { widgets.echo.settings.clear(); widgets.echo.refresh(); }));
            p.info("Das Widget liest den Status des Mediaplayers und zeigt ein Cover, wenn Home Assistant es bereitstellt.");
        }
        if(spec.system()){
            p.options("cpu","Messwerte",new String[]{"CPU","RAM","Speicher","Netzwerk"},i->(widgets.spec(id).metricsMask&(1<<i))!=0,i->widgets.metrics(id,widgets.spec(id).metricsMask^(1<<i)),true);
            p.options("cpu","Systeminformationen · Layout",SystemInfoStyle.NAMES,i->SystemInfoStyle.IDS[i].equals(widgets.spec(id).metricsStyle),i->widgets.metricsStyle(id,SystemInfoStyle.IDS[i]));
            p.options("grid","Ringe · Anordnung",new String[]{"Untereinander","Nebeneinander"},i->(i==1)=="horizontal".equals(widgets.spec(id).ringAxis),i->widgets.ringAxis(id,i==1?"horizontal":"vertical"));
            p.add(new GridSlider(a,"Ringe · Abstand (dp)",0,32,spec.ringGap,n->widgets.ringGap(id,n)));
            p.add(new GridSlider(a,"Ringe · Leuchten (%)",0,100,spec.ringGlow,n->widgets.store.edit(id,w->w.ringGlow=n)));p.add(new GridSlider(a,"Ringe · Linien-Deckkraft (%)",0,100,spec.ringOpacity,n->widgets.store.edit(id,w->w.ringOpacity=n)));
            p.info("Ringe zeigen CPU-, RAM- und Speicherbelegung in Prozent. Die Anordnung gilt für alle sichtbaren Ringe.");
            p.options("photo","Systeminformationen · Oberfläche",new String[]{"Glas · Transparent","Hell · Bisheriger Stil"},i->(i==0)==widgets.spec(id).metricsGlass,i->widgets.metricsGlass(id,i==0));
            p.add(new GridSlider(a,"Glas · Deckkraft (%)",0,90,spec.metricsOpacity,n->widgets.metricsOpacity(id,n)));
            p.info("Alle Messwerte gehören zu diesem Widget. Ohne ausgewählte Werte ist es ausgeblendet. Netzwerk zeigt LAN + WLAN zusammen in MB/s.");
            p.info("Netzwerkraten sind Mittelwerte über das Aktualisierungsintervall. Tunnel und Loopback werden nicht doppelt gezählt.");
        }
        LinearLayout shown=p.toggle(s(R.string.header_show),null,spec.enabled,on->widgets.show(id,on));
        p.row("move",s(R.string.header_move),s(R.string.header_move_hint),()->HeaderMoveOverlay.show(a,widgets,id,title(spec),()->{}));
        LinearLayout theme=spec.quote()||spec.text()?null:p.row("photo",s(R.string.widgets_theme),SurfaceThemePopup.name(a,spec.theme),()->SurfaceThemePopup.choose(a,s(R.string.widgets_theme),widgets.spec(id).theme,true,choice->widgets.theme(id,choice)));
        GridSlider widthControl=new GridSlider(a,s(R.string.widgets_width),spec.android()?Math.min(widgets.width(),widgets.android.minWidth(spec.appWidgetId)):spec.quote()?240:spec.echo()?160:64,spec.android()?widgets.width():Math.min(480,widgets.width()),spec.width,n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.resize(id,n,now.fixedHeight()?now.height:now.metricsHeight);});
        p.add(widthControl);
        if(spec.fixedHeight()&&!spec.android()){
            GridSlider height=new GridSlider(a,s(R.string.widgets_height),spec.text()?24:spec.echo()?68:80,Math.min(500,widgets.height()),spec.height,n->{HomeWidgetStore.Spec now=widgets.spec(id);if(now!=null)widgets.resize(id,now.width,n);});
            if(spec.echo())heightControl[0]=height;
            p.add(height);
        }
        if(!spec.fixedHeight()){
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
        SharedPreferences.OnSharedPreferenceChangeListener update=(sp,key)->{if(key!=null&&(key.startsWith(HomeWidgetStore.P)||key.startsWith("system_stats_")))p.body.postDelayed(()->{HomeWidgetStore.Spec now=widgets.spec(id);if(now==null||widgets.view(id)==null)return;int[] position=widgets.position(id);x.syncValue(position[0]);y.syncValue(position[1]);if(now.echo()&&!widthControl.hasFocus())widthControl.syncValue(now.width);if(heightControl[0]!=null&&!heightControl[0].hasFocus())heightControl[0].syncValue(now.echo()?now.height:widgets.size(id)[1]);SettingsSheet.syncToggle(shown,now.enabled);if(theme!=null)subtitle(theme,SurfaceThemePopup.name(a,now.theme));},50);};prefs.registerOnSharedPreferenceChangeListener(update);p.dialog.setOnDismissListener(d->prefs.unregisterOnSharedPreferenceChangeListener(update));p.show();}
    private void textOptions(SettingsSheet p,String id){
        p.row("text","Text bearbeiten",widgets.spec(id).textContent,()->editText(id));
        java.util.List<String> fonts=new java.util.ArrayList<>();for(String font:FontPolicy.IDS)if(FontPolicy.forAppNames(font))fonts.add(font);
        p.options("text","Text · Schriftart",fonts.stream().map(f->Typography.name(a,f)).toArray(String[]::new),i->fonts.get(i).equals(widgets.spec(id).textFont),i->widgets.store.edit(id,w->w.textFont=fonts.get(i))).fontPreviews(widgets.spec(id).textContent,i->Typography.face(a,fonts.get(i),R.string.font_regular));
        p.add(new GridSlider(a,"Text · Schriftgröße (sp)",8,120,widgets.spec(id).textSize,n->widgets.store.edit(id,w->w.textSize=n)));
        p.add(new GridSlider(a,"Text · Deckkraft (%)",0,100,widgets.spec(id).textOpacity,n->widgets.store.edit(id,w->w.textOpacity=n)));
        p.toggle("Text · Glasfläche",null,widgets.spec(id).textSurface,on->widgets.store.edit(id,w->w.textSurface=on));
        p.info("Eigener Text ohne Internet. Schrift, Größe, Deckkraft, Breite, Höhe und Position gelten nur für dieses Widget.");
    }
    private void editText(String id){LinearLayout panel=GlassDialog.panel(a);panel.addView(GlassDialog.text(a,"Eigener Text",20));EditText edit=new EditText(a);edit.setTextColor(Color.WHITE);edit.setText(widgets.spec(id).textContent);edit.setSelectAllOnFocus(true);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});panel.addView(edit,new LinearLayout.LayoutParams(-1,SettingsSheet.dp(a,96)));TextView save=DesignPopup.button(a,"Übernehmen");panel.addView(save);Dialog d=GlassDialog.show(a,panel,400,210);save.setOnClickListener(v->{widgets.store.edit(id,w->w.textContent=edit.getText().toString());d.dismiss();});save.requestFocus();}
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
