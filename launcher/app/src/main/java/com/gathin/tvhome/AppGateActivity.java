package com.gathin.tvhome;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
/** Private activity. Every launch requires fresh authorization. */
public final class AppGateActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);Intent target=getIntent().getParcelableExtra("target");if(target==null){finish();return;}
        String pkg=target.getComponent()==null?target.getPackage():target.getComponent().getPackageName();if(pkg==null||pkg.equals(getPackageName())){finish();return;}
        String name=pkg;try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        PinDialog.authorize(this,AppAppearance.label(this,pkg,name),()->{try{startActivity(target);AppUsage.record(this,pkg);}catch(android.content.ActivityNotFoundException|SecurityException ignored){}finish();},this::finish);
    }
}
