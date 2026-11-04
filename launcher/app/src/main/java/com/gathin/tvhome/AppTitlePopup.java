package com.gathin.tvhome;
import android.content.*;
final class AppTitlePopup {
    static void show(MainActivity a,Runnable changed){
        AppTitleStyle style=a.appTitleStyle();if(style==null)return;SharedPreferences p=a.getSharedPreferences(Config.SETTINGS_NAME,0);SettingsSheet s=new SettingsSheet(a,a.getString(R.string.title_style));
        s.options("text",a.getString(R.string.title_position),new String[]{a.getString(R.string.title_position_hero),a.getString(R.string.title_position_focus)},i->(i==1)==AppTitleStyle.below(a),i->{p.edit().putString(AppTitleStyle.MODE,i==1?"focused":"hero").apply();changed.run();});
        s.info(a.getString(R.string.title_position_hint));
        GridSlider size=new GridSlider(a,a.getString(R.string.title_size),18,120,AppTitlePolicy.size(p.getInt(AppTitleStyle.SIZE,style.originalSize)),n->p.edit().putInt(AppTitleStyle.SIZE,n).apply());s.add(size);
        GridSlider labels=new GridSlider(a,a.getString(R.string.title_labels),60,160,AppTitlePolicy.labels(p.getInt(AppTitleStyle.LABELS,100)),n->{p.edit().putInt(AppTitleStyle.LABELS,n).apply();changed.run();});s.add(labels);
        java.util.function.Supplier<PlacementTarget> target=()->AppTitleStyle.below(a)?style.focused():style;
        final GridSlider[] coords=new GridSlider[2];Runnable sync=()->{int[] now=target.get().position("title");if(coords[0]!=null)coords[0].syncValue(now[0]);if(coords[1]!=null)coords[1].syncValue(now[1]);};
        s.row("move",a.getString(R.string.title_move),a.getString(R.string.title_move_hint),()->HeaderMoveOverlay.show(a,target.get(),"title","App-Name verschieben",()->{sync.run();changed.run();}));
        int[] dimensions=target.get().size("title"),pos=target.get().position("title");
        coords[0]=new GridSlider(a,a.getString(R.string.header_x),0,Math.max(0,target.get().width()-dimensions[0]),pos[0],n->{int[] now=target.get().position("title");target.get().set("title",n,now[1]);});s.add(coords[0]);
        coords[1]=new GridSlider(a,a.getString(R.string.header_y),0,Math.max(0,target.get().height()-dimensions[1]),pos[1],n->{int[] now=target.get().position("title");target.get().set("title",now[0],n);});s.add(coords[1]);
        s.row("settings","Fokus-Beschriftung · Position zurücksetzen",null,()->{style.focused().reset();changed.run();});
        s.row("settings",a.getString(R.string.title_reset),null,()->{style.reset();style.focused().reset();size.syncValue(style.originalSize);labels.syncValue(100);changed.run();a.getWindow().getDecorView().postDelayed(sync,150);});s.show();
    }
}
