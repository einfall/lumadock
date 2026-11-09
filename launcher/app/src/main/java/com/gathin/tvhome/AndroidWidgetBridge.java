package com.gathin.tvhome;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
/** Standard AppWidgetHost binding/configuration. No automatic privileged grants or hidden shell calls. */
final class AndroidWidgetBridge {
    static final int HOST_ID = 60431;
    static final int BIND=7181,CONFIGURE=7182;private static final String PENDING=HomeWidgetStore.P+"pending_android_id",STAGE=HomeWidgetStore.P+"pending_android_stage";
    private final Activity a;private final HomeWidgetStore store;private final AppWidgetManager manager;private final Host host;private boolean listening,active;
    private final BroadcastReceiver display=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){listen();}};
    AndroidWidgetBridge(Activity a,HomeWidgetStore store){this.a=a;this.store=store;manager=AppWidgetManager.getInstance(a);host=new Host(a);IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_SCREEN_OFF);if(Build.VERSION.SDK_INT>=33)a.registerReceiver(display,f,Context.RECEIVER_NOT_EXPORTED);else a.registerReceiver(display,f);}
    List<AppWidgetProviderInfo> providers(){try{List<AppWidgetProviderInfo> list=new ArrayList<>(manager.getInstalledProviders());list.sort((x,y)->label(x).compareToIgnoreCase(label(y)));return list;}catch(RuntimeException ex){return Collections.emptyList();}}
    String label(AppWidgetProviderInfo info){try{String label=info.loadLabel(a.getPackageManager());return label==null?info.provider.getPackageName():label;}catch(RuntimeException ex){return info.provider.getPackageName();}}
    void choose(){SettingsSheet p=new SettingsSheet(a,a.getString(R.string.widgets_android));List<AppWidgetProviderInfo> list=providers();p.info(a.getString(list.isEmpty()?R.string.widgets_android_empty:R.string.widgets_android_hint));for(AppWidgetProviderInfo info:list)p.add(WidgetPreview.tile(a,preview(info),label(info),info.provider.getPackageName(),()->{p.dialog.dismiss();begin(info);}));p.show();}
    View preview(int id){return preview(manager.getAppWidgetInfo(id));}
    private View preview(AppWidgetProviderInfo info){
        android.graphics.drawable.Drawable art=null;
        if(info!=null)try{art=info.loadPreviewImage(a,android.util.DisplayMetrics.DENSITY_MEDIUM);if(art==null)art=info.loadIcon(a,android.util.DisplayMetrics.DENSITY_MEDIUM);}catch(RuntimeException ignored){}
        ImageView image=WidgetPreview.image(a,art==null?new AppGlyph("grid"):art);
        image.setBackground(SurfaceThemes.background(a,"graphite",false));
        return image;
    }
    private void begin(AppWidgetProviderInfo info){if(store.full()){toast(R.string.widgets_limit);return;}cancelPending();try{
        int id=host.allocateAppWidgetId();store.prefs.edit().putInt(PENDING,id).putString(STAGE,"bind").apply();Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        if(manager.bindAppWidgetIdIfAllowed(id,info.getProfile(),info.provider,options)){configure(id);return;}
        Intent intent=new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,info.getProfile()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options);
        a.startActivityForResult(intent,BIND);
    }catch(ActivityNotFoundException|SecurityException ex){cancelPending();toast(R.string.widgets_android_no_bind);}catch(RuntimeException ex){cancelPending();toast(R.string.widgets_android_unavailable);}}
    private void configure(int id){AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null){cancelPending();return;}if(info.configure==null){finish(id);return;}
        store.prefs.edit().putString(STAGE,"configure").apply();try{host.startAppWidgetConfigureActivityForResult(a,id,0,CONFIGURE,null);}catch(RuntimeException ex){cancelPending();toast(R.string.widgets_android_config_missing);}
    }
    boolean result(int request,int result,Intent data){if(request!=BIND&&request!=CONFIGURE)return false;int expected=store.prefs.getInt(PENDING,0);if(expected==0)return true;int returned=data==null?expected:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,expected);if(result!=Activity.RESULT_OK||returned!=expected){cancelPending();return true;}try{if(request==BIND)configure(expected);else finish(expected);}catch(RuntimeException ex){cancelPending();toast(R.string.widgets_android_unavailable);}return true;}
    private void finish(int id){AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null){cancelPending();return;}float density=a.getResources().getDisplayMetrics().density;
        android.graphics.Rect padding=AppWidgetHostView.getDefaultPaddingForWidget(a,info.provider,null);int minWidth=Math.round((info.minWidth+padding.left+padding.right)/density)+18,minHeight=Math.round((info.minHeight+padding.top+padding.bottom)/density)+35;
        if(minWidth>Math.round(a.getResources().getDisplayMetrics().widthPixels/density)||minHeight>Math.round(a.getResources().getDisplayMetrics().heightPixels/density)){cancelPending();toast(R.string.widgets_android_too_large);return;}
        HomeWidgetStore.Spec added=store.addAndroid(id,info.provider.flattenToString(),label(info),Math.max(160,minWidth),Math.max(100,minHeight));
        if(added==null){cancelPending();toast(R.string.widgets_limit);return;}store.prefs.edit().remove(PENDING).remove(STAGE).apply();toast(R.string.widgets_android_added);
    }
    private void cancelPending(){int id=store.prefs.getInt(PENDING,0);if(id>0)delete(id);store.prefs.edit().remove(PENDING).remove(STAGE).apply();}
    int minWidth(int id){AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null)return 120;int n=(info.resizeMode&AppWidgetProviderInfo.RESIZE_HORIZONTAL)!=0&&info.minResizeWidth>0?info.minResizeWidth:info.minWidth;android.graphics.Rect pad=AppWidgetHostView.getDefaultPaddingForWidget(a,info.provider,null);return Math.max(120,Math.round((n+pad.left+pad.right)/a.getResources().getDisplayMetrics().density)+18);}
    int minHeight(int id){AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null)return 48;int n=(info.resizeMode&AppWidgetProviderInfo.RESIZE_VERTICAL)!=0&&info.minResizeHeight>0?info.minResizeHeight:info.minHeight;android.graphics.Rect pad=AppWidgetHostView.getDefaultPaddingForWidget(a,info.provider,null);return Math.max(48,Math.round((n+pad.top+pad.bottom)/a.getResources().getDisplayMetrics().density)+35);}
    void delete(int id){try{host.deleteAppWidgetId(id);}catch(RuntimeException ignored){}}
    AppWidgetHostView create(int id){try{AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null)return null;AppWidgetHostView view=host.createView(a,id,info);view.setFocusable(false);view.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);view.setOnTouchListener((v,event)->true);return view;}catch(RuntimeException ex){return null;}}
    void start(){active=true;listen();}void stop(){active=false;listen();}
    private void listen(){PowerManager power=a.getSystemService(PowerManager.class);boolean on=active&&power!=null&&power.isInteractive();if(on==listening)return;try{if(on)host.startListening();else host.stopListening();listening=on;}catch(RuntimeException ignored){listening=false;}}
    void close(){stop();host.release();a.unregisterReceiver(display);}
    private static final class Host extends AppWidgetHost{Host(Context c){super(c,HOST_ID);}void release(){clearViews();}}
    private void toast(int text){Toast.makeText(a,text,Toast.LENGTH_LONG).show();}
}
