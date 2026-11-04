package com.gathin.tvhome;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
/** Compatibility entry point: all launcher settings now open the structured editor on the real Home. */
public final class LauncherSettingsActivity extends Activity {
 static final String EXTRA_REORDER="com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
 @Override protected void onCreate(Bundle state){super.onCreate(state);startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS));finish();}
}
