package com.gathin.tvhome;
import android.app.Activity;
import android.widget.TextView;
import android.widget.LinearLayout;
final class TileEffectsPopup {
    private final Activity a;private final boolean focused;private final Runnable changed;private final boolean[] values;private String color;private int gap,contrastWidth,width,glowStrength,glowSpread;private LinearLayout colorRow;
    static void show(Activity a,boolean focused,Runnable changed){new TileEffectsPopup(a,focused,changed,null);}
    static void append(Activity a,boolean focused,Runnable changed,SettingsSheet sheet){new TileEffectsPopup(a,focused,changed,sheet);}
    private TileEffectsPopup(Activity a,boolean focused,Runnable changed,SettingsSheet embedded){
        this.a=a;this.focused=focused;this.changed=changed;TileEffects e=FocusStyle.profile(a,focused);values=new boolean[]{e.frame,e.glass,e.shine,e.thin,e.contrast,e.nuvio,e.glow};color=e.color;gap=e.gap;contrastWidth=e.contrastWidth;width=e.width;glowStrength=e.glowStrength;glowSpread=e.glowSpread;
        SettingsSheet p=embedded==null?new SettingsSheet(a,a.getString(focused?R.string.effects_focus_title:R.string.effects_idle_title)):embedded;
        
        if(!focused)geometry(p);
        p.group(a.getString(R.string.menu_appearance));
        int[] indices=focused?new int[]{1,2,3,4,5,6}:new int[]{0,1,2,3,4,5,6};
        int[] labels={R.string.effects_frame,R.string.effects_glass,R.string.effects_shine,R.string.effects_edge,R.string.outline_contrast,R.string.effects_nuvio,R.string.effects_glow};
        String[] names=new String[indices.length];for(int i=0;i<indices.length;i++)names[i]=a.getString(labels[indices[i]]);
        p.options("star","Effekte · kombinierbar",names,i->values[indices[i]],i->{int at=indices[i];values[at]=!values[at];save();},true);
        String[] colors=a.getResources().getStringArray(R.array.focus_color_names);p.options("photo",a.getString(R.string.focus_color_title),colors,i->FocusStyle.COLORS[i].equals(color),i->{color=FocusStyle.COLORS[i];save();});
        p.add(new GridSlider(a,a.getString(R.string.effects_glow_strength),1,60,glowStrength,n->{glowStrength=n;save();}));
        p.add(new GridSlider(a,a.getString(R.string.effects_glow_spread),1,12,glowSpread,n->{glowSpread=n;save();}));
        p.add(new GridSlider(a,a.getString(R.string.outline_width),1,3,contrastWidth,n->{contrastWidth=n;save();}));p.info(a.getString(R.string.outline_hint));if(embedded==null)p.show();
    }
    private void geometry(SettingsSheet p){
        p.group(a.getString(R.string.effects_geometry));
        p.add(new GridSlider(a,a.getString(R.string.effects_gap_pixels),0,5,gap,n->{gap=n;save();}));p.info(a.getString(R.string.effects_gap_hint));
    }
    static void focusGeometry(Activity a,Runnable changed,SettingsSheet p){
        p.group(a.getString(R.string.effects_geometry));TileEffects e=FocusStyle.profile(a,true);
        p.add(new GridSlider(a,a.getString(R.string.focus_frame_width),1,8,e.width>0?e.width:Math.max(1,Math.round(a.getResources().getDisplayMetrics().density*(e.thin?1f:1.5f))),n->saveGeometry(a,changed,n,null)));
        p.add(new GridSlider(a,a.getString(R.string.effects_gap_pixels),0,5,e.gap,n->saveGeometry(a,changed,null,n)));
        p.info(a.getString(R.string.effects_geometry_hint));
    }
    private static void saveGeometry(Activity a,Runnable changed,Integer width,Integer gap){
        // Read the current profile: the other controls on this sheet may have changed in the meantime.
        TileEffects e=FocusStyle.profile(a,true);FocusStyle.save(a,true,new TileEffects(e.frame,e.glass,e.shine,e.thin,e.color,gap==null?e.gap:gap,e.contrast,e.contrastWidth,width==null?e.width:width,e.nuvio,e.glow,e.glowStrength,e.glowSpread));changed.run();
    }
    private void save(){TileEffects current=FocusStyle.profile(a,focused);FocusStyle.save(a,focused,new TileEffects(focused||values[0],values[1],values[2],values[3],color,focused?current.gap:gap,values[4],contrastWidth,focused?current.width:width,values[5],values[6],glowStrength,glowSpread));changed.run();}
    private String colorName(String id){int i=0;for(int n=0;n<FocusStyle.COLORS.length;n++)if(FocusStyle.COLORS[n].equals(id))i=n;return a.getResources().getStringArray(R.array.focus_color_names)[i];}
    private void chooseColor(){SettingsSheet p=new SettingsSheet(a,a.getString(R.string.focus_color_title));for(String id:FocusStyle.COLORS){p.choice("star",colorName(id),null,()->color.equals(id),()->{color=id;save();((TextView)((LinearLayout)colorRow.getChildAt(1)).getChildAt(1)).setText(a.getString(R.string.focus_color_choice,colorName(color)));});}p.show();}
}
