package com.gathin.tvhome;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/** The fourth digit submits automatically. D-pad keypad and real number buttons both work. */
final class PinDialog {
    private final Activity activity;private final boolean creating;private final Runnable success,cancelled;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler handler=new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;private Dialog dialog;private final TextView heading,dots,status;
    private String digits="",first=null;private boolean busy,closed,passed;
    static void authorize(Activity a,String name,Runnable ok){authorize(a,name,ok,()->{});}
    static void authorize(Activity a,String name,Runnable ok,Runnable cancel){if(!AppLock.configured(a)){ok.run();return;}new PinDialog(a,false,name,ok,cancel);}
    static void create(Activity a,Runnable done){new PinDialog(a,true,"",done,()->{});}
    static void change(Activity a,Runnable done){authorize(a,a.getString(R.string.apps_pin_change),()->create(a,done));}
    private PinDialog(Activity a,boolean create,String name,Runnable ok,Runnable cancel){
        activity=a;creating=create;success=ok;cancelled=cancel;prefs=AppLock.prefs(a);
        LinearLayout panel=GlassDialog.panel(a);panel.setGravity(Gravity.CENTER_HORIZONTAL);
        android.widget.ImageView symbol=new android.widget.ImageView(a);symbol.setImageDrawable(new AppGlyph("lock"));panel.addView(symbol,new LinearLayout.LayoutParams(GlassDialog.dp(a,28),GlassDialog.dp(a,32)));
        heading=GlassDialog.text(a,a.getString(create?R.string.apps_pin_create:R.string.apps_pin_enter),22);heading.setGravity(Gravity.CENTER);panel.addView(heading);
        TextView sub=GlassDialog.text(a,create?a.getString(R.string.apps_pin_four):name,12);sub.setGravity(Gravity.CENTER);sub.setTextColor(0xFFADB7C9);panel.addView(sub);
        dots=GlassDialog.text(a,"",28);dots.setLetterSpacing(.3f);dots.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,GlassDialog.dp(a,52));dl.topMargin=GlassDialog.dp(a,8);panel.addView(dots,dl);
        status=GlassDialog.text(a,"",12);status.setGravity(Gravity.CENTER);status.setTextColor(0xFF91CFFF);panel.addView(status,new LinearLayout.LayoutParams(-1,GlassDialog.dp(a,32)));
        TextView initial=null;
        for(int row=0;row<4;row++){
            LinearLayout line=new LinearLayout(a);line.setGravity(Gravity.CENTER);
            for(int col=0;col<3;col++){
                final int n=row*3+col+1;String label=n<=9?Integer.toString(n):n==10?"×":n==11?"0":"⌫";
                TextView b=DesignPopup.button(a,label);b.setTextSize(23);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(GlassDialog.dp(a,64),GlassDialog.dp(a,50));lp.setMargins(GlassDialog.dp(a,6),GlassDialog.dp(a,5),GlassDialog.dp(a,6),GlassDialog.dp(a,5));line.addView(b,lp);
                b.setContentDescription(n==10?a.getString(R.string.design_cancel):n==12?a.getString(R.string.apps_pin_delete):label);
                b.setOnClickListener(v->{if(n==10)dialog.dismiss();else if(n==12)erase();else add(n==11?0:n);});if(n==1)initial=b;
            }panel.addView(line);
        }
        dialog=GlassDialog.show(a,panel,350,460);
        dialog.setOnDismissListener(d->{closed=true;worker.shutdownNow();handler.removeCallbacksAndMessages(null);if(!passed)cancelled.run();});
        dialog.setOnKeyListener((d,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(event.getRepeatCount()>0&&(key>=KeyEvent.KEYCODE_0&&key<=KeyEvent.KEYCODE_9||key>=KeyEvent.KEYCODE_NUMPAD_0&&key<=KeyEvent.KEYCODE_NUMPAD_9))return true;
            if(key>=KeyEvent.KEYCODE_0&&key<=KeyEvent.KEYCODE_9){add(key-KeyEvent.KEYCODE_0);return true;}
            if(key>=KeyEvent.KEYCODE_NUMPAD_0&&key<=KeyEvent.KEYCODE_NUMPAD_9){add(key-KeyEvent.KEYCODE_NUMPAD_0);return true;}
            if(key==KeyEvent.KEYCODE_DEL){erase();return true;}return false;
        });refresh();if(initial!=null)initial.requestFocus();
    }
    private void refresh(){StringBuilder s=new StringBuilder();for(int i=0;i<4;i++)s.append(i<digits.length()?"●":"○");dots.setText(s);}
    private void erase(){if(!busy&&!digits.isEmpty()){digits=digits.substring(0,digits.length()-1);refresh();}}
    private void add(int digit){
        if(busy||closed)return;long now=System.currentTimeMillis(),until=prefs.getLong("blocked_until",0);
        if(!creating&&until>now){status.setText(activity.getString(R.string.apps_pin_wait,(until-now+999)/1000));return;}
        if(digits.length()>=4)return;digits+=digit;refresh();if(digits.length()!=4)return;
        if(creating&&first==null){first=digits;digits="";heading.setText(R.string.apps_pin_confirm);status.setText("");refresh();return;}
        if(creating&&!digits.equals(first)){digits="";status.setText(R.string.apps_pin_mismatch);refresh();return;}
        busy=true;status.setText(R.string.apps_pin_checking);String candidate=digits,verifier=prefs.getString(AppLock.VERIFIER,"");
        worker.execute(()->{String record=creating?PinCode.create(candidate):null;boolean verified=creating||PinCode.verify(candidate,verifier);handler.post(()->{
            if(closed)return;busy=false;
            if(verified){SharedPreferences.Editor e=prefs.edit().putInt("attempts",0).remove("blocked_until");if(creating)e.putString(AppLock.VERIFIER,record);e.apply();passed=true;dialog.dismiss();success.run();}
            else{int attempts=prefs.getInt("attempts",0)+1;SharedPreferences.Editor e=prefs.edit().putInt("attempts",attempts%5);if(attempts>=5)e.putLong("blocked_until",System.currentTimeMillis()+30000);e.apply();digits="";refresh();status.setText(attempts>=5?R.string.apps_pin_blocked:R.string.apps_pin_wrong);dots.animate().translationX(GlassDialog.dp(activity,8)).setDuration(70).withEndAction(()->dots.animate().translationX(0).setDuration(100).start()).start();}
        });});
    }
}
