package com.gathin.tvhome;
import android.app.*;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.Toast;
/** Remote-first live placement. Cancel never writes an edited coordinate. */
final class HeaderMoveOverlay extends View {
    private final Activity a;private final PlacementTarget layout;private final String key,title;private final Runnable changed;private final Dialog dialog;
    private final Paint paint=new Paint(3);private int x,y;private final int step;private final float density;private boolean accepted;
    static void show(Activity a,PlacementTarget layout,String key,String title,Runnable changed){if(layout.view(key)==null||!layout.view(key).isShown()){Toast.makeText(a,R.string.header_show_first,Toast.LENGTH_LONG).show();return;}new HeaderMoveOverlay(a,layout,key,title,changed);}
    private HeaderMoveOverlay(Activity a,PlacementTarget layout,String key,String title,Runnable changed){
        super(a);this.a=a;this.layout=layout;this.key=key;this.title=title;this.changed=changed;density=a.getResources().getDisplayMetrics().density;int[] p=layout.position(key);x=p[0];y=p[1];SharedPreferences prefs=a.getSharedPreferences(Config.SETTINGS_NAME,0);step=Math.max(1,Math.min(20,prefs.getInt(HeaderLayoutController.STEP,5)));setFocusable(true);setFocusableInTouchMode(true);
        java.util.List<Dialog> hidden=SettingsSheet.hideForPlacement(a);dialog=new Dialog(a);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);dialog.setContentView(this);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
        dialog.setOnKeyListener((d,code,event)->{
            if(code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_DPAD_UP||code==KeyEvent.KEYCODE_DPAD_DOWN){if(event.getAction()==KeyEvent.ACTION_DOWN){int[] size=layout.size(key);x=HeaderLayoutPolicy.clamp(x+(code==KeyEvent.KEYCODE_DPAD_LEFT?-step:code==KeyEvent.KEYCODE_DPAD_RIGHT?step:0),layout.width(),size[0]);y=HeaderLayoutPolicy.clamp(y+(code==KeyEvent.KEYCODE_DPAD_UP?-step:code==KeyEvent.KEYCODE_DPAD_DOWN?step:0),layout.height(),size[1]);layout.preview(key,x,y);postInvalidateOnAnimation();}return true;}
            if(code==KeyEvent.KEYCODE_DPAD_CENTER||code==KeyEvent.KEYCODE_ENTER){if(event.getAction()==KeyEvent.ACTION_UP){accepted=true;layout.set(key,x,y);dialog.dismiss();}return true;}
            if(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_ESCAPE){if(event.getAction()==KeyEvent.ACTION_UP)dialog.dismiss();return true;}return false;
        });
        NavigationSounds.attach(a,dialog.getWindow());
        dialog.setOnDismissListener(d->{layout.endPreview();SettingsSheet.restoreAfterPlacement(hidden);if(accepted)changed.run();});dialog.show();if(w!=null)w.setLayout(-1,-1);requestFocus();
    }
    @Override protected void onDraw(Canvas c){
        setContentDescription(title+" · X "+x+" · Y "+y);
        Rect actual=layout.bounds(key);RectF box=new RectF(actual);box.inset(-5*density,-5*density);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);paint.setColor(0xFF98CCFF);c.drawRoundRect(box,10*density,10*density,paint);
        float pad=18*density,barH=66*density;float top=box.centerY()>getHeight()/2?pad:getHeight()-barH-pad;RectF bar=new RectF(pad,top,getWidth()-pad,top+barH);paint.setStyle(Paint.Style.FILL);paint.setColor(0xF0101824);c.drawRoundRect(bar,14*density,14*density,paint);paint.setColor(Color.WHITE);paint.setTextSize(16*density);c.drawText(title+" · X "+x+" · Y "+y,bar.left+16*density,bar.top+25*density,paint);paint.setColor(0xFFB8C8DD);paint.setTextSize(11*density);c.drawText(a.getString(R.string.header_move_keys,step),bar.left+16*density,bar.top+47*density,paint);
    }
}
