package com.gathin.tvhome;
import android.app.Activity;
/** Both states on one page; independent effects are selected with OK. */
final class FocusMotionPopup {
    static void show(Activity a,Runnable changed){SettingsSheet p=new SettingsSheet(a,a.getString(R.string.motion_title));render(a,changed,p,true);p.show();}
    private static void render(Activity a,Runnable changed,SettingsSheet p,boolean focused){
        p.clearRows();SettingsOptions state=p.options("grid","App-Zustand",new String[]{"Im Fokus","Ohne Fokus"},i->(i==0)==focused,i->{render(a,changed,p,i==0);p.body.getChildAt(0).requestFocus();});
        if(focused){String[] ids={"none","moving","instant","fade"};String[] names=java.util.Arrays.stream(ids).map(id->modeName(a,id)).toArray(String[]::new);p.options("star",a.getString(R.string.focus_frame_mode),names,i->ids[i].equals(FocusStyle.frameMode(a)),i->{FocusStyle.saveFrameMode(a,ids[i]);changed.run();});
            TileEffectsPopup.focusGeometry(a,changed,p);FocusMotion m=FocusStyle.motion(a,Config.load(a));boolean[] values={m.zoom,m.animateIn,m.animateOut,m.bounce};
            String[] labels={a.getString(R.string.motion_zoom),a.getString(R.string.motion_in),a.getString(R.string.motion_out),a.getString(R.string.motion_bounce)};
            p.options("move","Bewegung · kombinierbar",labels,i->values[i],i->{values[i]=!values[i];FocusStyle.saveMotion(a,new FocusMotion(!"none".equals(FocusStyle.frameMode(a)),values[0],values[1],values[2],values[3]));changed.run();},true);
        }
        p.info("Links/Rechts: Option wählen · OK: mehrere Effekte kombinieren. Änderungen sind auf Home sofort sichtbar.");TileEffectsPopup.append(a,focused,changed,p);
    }
    private static String modeName(Activity a,String id){return a.getString("none".equals(id)?R.string.focus_frame_none:"fade".equals(id)?R.string.focus_frame_fade:"instant".equals(id)?R.string.focus_frame_instant:R.string.focus_frame_moving);}
}
