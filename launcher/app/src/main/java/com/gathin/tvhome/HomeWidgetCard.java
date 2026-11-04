package com.gathin.tvhome;
import android.app.Activity;
import android.appwidget.AppWidgetHostView;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;
/** One view factory for all internal sources, plus an outer surface for native Android widgets. */
final class HomeWidgetCard extends LinearLayout {
    final String source;final int mask,appWidgetId;final AppWidgetHostView nativeView;final QuoteWidgetView quote;private final Activity a;private final List<Metric> metrics=new ArrayList<>();private final TextView heading;private String title;private SystemInfoView systemView;private LinearLayout metricColumns;private SystemStatsWidget.Snapshot latest;
    private final class Metric{final int kind;final LinearLayout box;final TextView label,value,detail;final ImageView icon;final ProgressBar bar;
        Metric(int kind){this.kind=kind;box=new LinearLayout(a);box.setOrientation(VERTICAL);LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);icon=new ImageView(a);icon.setImageDrawable(new AppGlyph(kind==1?"cpu":kind==2?"memory":kind==8?"network":"storage"));top.addView(icon,new LinearLayout.LayoutParams(dp(11),dp(11)));label=text(kind==8?"Netzwerk":a.getString(kind==1?R.string.system_stats_cpu:kind==2?R.string.system_stats_ram:R.string.system_stats_storage),9);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginStart(dp(5));top.addView(label,lp);box.addView(top);value=text("—",13);value.setAutoSizeTextTypeUniformWithConfiguration(9,13,1,TypedValue.COMPLEX_UNIT_SP);box.addView(value,new LinearLayout.LayoutParams(-1,dp(21)));detail=text(a.getString(R.string.system_stats_loading),8);box.addView(detail,new LinearLayout.LayoutParams(-1,dp(13)));bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);if(kind==8)bar.setVisibility(GONE);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(2));bp.topMargin=dp(4);box.addView(bar,bp);}
        void update(SystemStatsWidget.Snapshot s){int used;String reading,caption;if(kind==1){used=s.cpu;reading=used<0?"—":used+" %";caption=a.getString(used<0?R.string.system_stats_cpu_missing:R.string.system_stats_cpu_detail);}else if(kind==2){used=SystemStatsPolicy.used(s.ramAvailable,s.ramTotal);reading=used<0?"—":a.getString(R.string.system_stats_of,size(s.ramTotal-Math.min(s.ramAvailable,s.ramTotal)),size(s.ramTotal));caption=used<0?a.getString(R.string.system_stats_unavailable):a.getString(R.string.system_stats_used,used);}else if(kind==8){used=-1;reading="↓ "+NetworkTrafficPolicy.format(s.rx);caption="↑ "+NetworkTrafficPolicy.format(s.tx);}else{used=SystemStatsPolicy.used(s.diskAvailable,s.diskTotal);reading=used<0?"—":a.getString(R.string.system_stats_free,size(s.diskAvailable));caption=used<0?a.getString(R.string.system_stats_unavailable):a.getString(R.string.system_stats_total,used,size(s.diskTotal));}value.setText(reading);detail.setText(caption);bar.setProgress(Math.max(0,used));bar.setProgressTintList(ColorStateList.valueOf(used>=90?0xFFF28A83:used>=75?0xFFF2BA75:0xFF94BFF5));box.setContentDescription(label.getText()+" · "+reading+" · "+caption);}
    }
    HomeWidgetCard(Activity a,HomeWidgetStore.Spec spec,int mask,AndroidWidgetBridge android,QuoteParticleLayers layers){super(a);this.a=a;source=spec.source;this.mask=mask;appWidgetId=spec.appWidgetId;setOrientation(VERTICAL);setPadding(dp(9),dp(7),dp(9),dp(7));setFocusable(false);setClipChildren(true);setClipToPadding(true);setElevation(dp(2));heading=text("",11);addView(heading,new LinearLayout.LayoutParams(-1,dp(21)));
        if(spec.quote()){nativeView=null;quote=new QuoteWidgetView(a,layers);addView(quote,new LinearLayout.LayoutParams(-1,0,1));}
        else if(spec.android()){quote=null;nativeView=android.create(spec.appWidgetId);if(nativeView!=null)addView(nativeView,new LinearLayout.LayoutParams(-1,0,1));else{TextView missing=text(a.getString(R.string.widgets_android_unavailable),11);missing.setSingleLine(false);addView(missing);}}
        else{quote=null;nativeView=null;LinearLayout columns=new LinearLayout(a);metricColumns=columns;for(int bit:new int[]{1,2,4,8})if((mask&bit)!=0){Metric metric=new Metric(bit);metrics.add(metric);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,-2,1);if(columns.getChildCount()>0)mp.setMarginStart(dp(12));columns.addView(metric.box,mp);}addView(columns,new LinearLayout.LayoutParams(-1,-2));}style(spec);
    }
    void style(HomeWidgetStore.Spec spec){title=spec.title.isEmpty()?HomeWidgets.sourceName(a,spec.source):spec.title;heading.setText(title);heading.setVisibility(spec.android()||"system".equals(source)||!spec.title.isEmpty()?VISIBLE:GONE);String id=SurfaceThemes.instance(a,spec.theme);SurfaceStyle s=SurfaceStyle.of(id,false);SurfaceThemes.widget(this,id);heading.setTextColor(s.ink);if(quote!=null){setBackground(spec.quoteSurface&&spec.quoteOpacity>0?new WidgetBackdrop(SurfaceThemes.background(a,id,false),spec.quoteOpacity):null);quote.style(spec,spec.quoteSurface&&spec.quoteOpacity>0?s.ink:android.graphics.Color.WHITE);}for(Metric m:metrics){m.label.setTextColor(s.muted);m.icon.setImageTintList(ColorStateList.valueOf(s.muted));m.value.setTextColor(s.ink);m.detail.setTextColor(s.muted);m.bar.setProgressBackgroundTintList(ColorStateList.valueOf("light".equals(id)?0x28102038:0x28FFFFFF));}setContentDescription(title);
        boolean custom="system".equals(source)&&!"original".equals(SystemInfoStyle.id(spec.metricsStyle));
        if(metricColumns!=null)metricColumns.setVisibility(custom?GONE:VISIBLE);
        if(custom){if(systemView==null){systemView=new SystemInfoView(a);addView(systemView,new LinearLayout.LayoutParams(-1,-2));}systemView.style(spec.metricsStyle,mask,title);systemView.data(latest);systemView.setVisibility(VISIBLE);heading.setVisibility(GONE);setPadding(0,0,0,0);setBackground(null);setElevation(0);}
        else{if(systemView!=null)systemView.setVisibility(GONE);setPadding(dp(9),dp(7),dp(9),dp(7));setElevation(dp(2));}
    }
    void quote(QuoteFeed.State state){if(quote!=null)quote.data(state);}
    void active(boolean on){if(quote!=null)quote.active(on);}
    void close(){if(quote!=null)quote.close();}
    void update(SystemStatsWidget.Snapshot s){latest=s;for(Metric metric:metrics)metric.update(s);if(systemView!=null)systemView.data(s);}
    void nativeSize(int width,int height){if(nativeView!=null)try{nativeView.updateAppWidgetSize(null,Math.max(1,width-18),Math.max(1,height-35),Math.max(1,width-18),Math.max(1,height-35));}catch(RuntimeException ignored){}}
    private TextView text(String label,int sp){TextView t=GlassDialog.text(a,label,sp);t.setSingleLine(true);t.setEllipsize(TextUtils.TruncateAt.END);t.setIncludeFontPadding(false);return t;}
    private int dp(float n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    private String size(long n){return Formatter.formatShortFileSize(a,Math.max(0,n));}
}
