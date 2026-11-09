package com.gathin.tvhome;
import android.content.*;
final class AppTitlePopup {
    static void show(MainActivity a,Runnable changed){
        AppTitleStyle style=a.appTitleStyle();if(style==null)return;SharedPreferences p=a.getSharedPreferences(Config.SETTINGS_NAME,0);SettingsSheet s=new SettingsSheet(a,a.getString(R.string.title_style));
        java.util.function.Supplier<PlacementTarget> target=()->AppTitleStyle.below(a)?style.focused():style;
        final GridSlider[] coords = new GridSlider[2];
        Runnable sync = () -> {
            s.refreshChoices();
            int[] now = target.get().position("title");
            if (coords[0] != null) coords[0].syncValue(now[0]);
            if (coords[1] != null) coords[1].syncValue(now[1]);
        };
        s.options("text",a.getString(R.string.title_position),new String[]{a.getString(R.string.title_position_hero),a.getString(R.string.title_position_focus)},i->(i==1)==AppTitleStyle.below(a),i->{p.edit().putString(AppTitleStyle.MODE,i==1?"focused":"hero").apply();changed.run();s.body.post(sync);});
        s.info(a.getString(R.string.title_position_hint));
        String[] alignments = {"left", "center", "right", "manual"};
        String[] transitions = {"slide", "fade", "none"};
        s.options("move", a.getString(R.string.title_alignment),
                new String[]{a.getString(R.string.title_align_left), a.getString(R.string.title_align_center),
                        a.getString(R.string.title_align_right), a.getString(R.string.title_align_manual)},
                i -> alignments[i].equals(style.alignment()), i -> {
                    style.align(alignments[i]);
                    sync.run();
                });
        s.options("sparkle", a.getString(R.string.title_transition),
                new String[]{a.getString(R.string.title_transition_slide), a.getString(R.string.title_transition_fade),
                        a.getString(R.string.title_transition_none)},
                i -> transitions[i].equals(style.transition()),
                i -> p.edit().putString(AppTitleStyle.TRANSITION, transitions[i]).apply());
        s.toggle("App-Name · Dominante Icon-Farbe","Übernimmt die Farbe des ausgewählten App-Icons",p.getBoolean(AppTitleStyle.DOMINANT,false),on->p.edit().putBoolean(AppTitleStyle.DOMINANT,on).apply());
        GridSlider size=new GridSlider(a,a.getString(R.string.title_size),18,120,AppTitlePolicy.size(p.getInt(AppTitleStyle.SIZE,style.originalSize)),n->p.edit().putInt(AppTitleStyle.SIZE,n).apply());s.add(size);
        GridSlider labels=new GridSlider(a,a.getString(R.string.title_labels),60,160,AppTitlePolicy.labels(p.getInt(AppTitleStyle.LABELS,100)),n->{p.edit().putInt(AppTitleStyle.LABELS,n).apply();changed.run();});s.add(labels);

        s.row("move",a.getString(R.string.title_move),a.getString(R.string.title_move_hint),()->HeaderMoveOverlay.show(a,target.get(),"title","App-Name verschieben",()->{sync.run();changed.run();}));
        int[] dimensions=target.get().size("title"),pos=target.get().position("title");
        coords[0]=new GridSlider(a,a.getString(R.string.header_x),0,Math.max(0,target.get().width()-dimensions[0]),pos[0],n->{int[] now=target.get().position("title");target.get().set("title",n,now[1]);s.refreshChoices();});s.add(coords[0]);
        coords[1]=new GridSlider(a,a.getString(R.string.header_y),0,Math.max(0,target.get().height()-dimensions[1]),pos[1],n->{int[] now=target.get().position("title");target.get().set("title",now[0],n);s.refreshChoices();});s.add(coords[1]);
        s.row("settings","Fokus-Beschriftung · Position zurücksetzen",null,()->{style.focused().reset();changed.run();});
        s.row("settings",a.getString(R.string.title_reset),null,()->{style.reset();style.focused().reset();size.syncValue(style.originalSize);labels.syncValue(100);changed.run();a.getWindow().getDecorView().postDelayed(sync,150);});s.show();
    }
}
