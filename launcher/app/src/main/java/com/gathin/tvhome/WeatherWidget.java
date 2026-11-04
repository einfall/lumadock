package com.gathin.tvhome;



import android.app.Activity;

import android.content.Context;

import android.content.SharedPreferences;

import android.graphics.Color;

import android.os.Handler;

import android.os.Looper;

import android.view.Gravity;

import android.view.View;

import android.widget.LinearLayout;

import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;

import java.util.Date;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;



/** Refreshes only while Home is visible, with a private disk cache and bounded retries. */

final class WeatherWidget {

    private final Activity activity;

    private final LinearLayout host;

    private final SharedPreferences saved;

    private final TextView city,temperature,description;

    private final WeatherIcon icon;

    private final Handler main=new Handler(Looper.getMainLooper());

    private final ExecutorService worker=Executors.newSingleThreadExecutor();

    private boolean active,busy,closed;

    private android.app.Dialog details;

    private final Runnable tick=()->{if(active){refresh();schedule();}};

    WeatherWidget(Activity activity) {

        this.activity=activity;host=activity.findViewById(R.id.weatherBlock);

        saved=activity.getSharedPreferences(WeatherPanel.PREFS,Context.MODE_PRIVATE);

        city=text(9,0xFFB8C6D7);host.addView(city);

        LinearLayout line=new LinearLayout(activity);line.setGravity(Gravity.CENTER_VERTICAL);

        icon=new WeatherIcon(activity);line.addView(icon,new LinearLayout.LayoutParams(dp(22),dp(22)));

        temperature=text(20,Color.WHITE);LinearLayout.LayoutParams temperatureLp=new LinearLayout.LayoutParams(-2,-2);temperatureLp.setMarginStart(dp(4));line.addView(temperature,temperatureLp);

        host.addView(line);

        description=text(8,0xFFA9B7C8);host.addView(description);

        host.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        host.setFocusable(true);host.setClickable(true);host.setBackground(null);
        host.setPadding(dp(4),dp(2),dp(4),dp(2));

        host.setOnClickListener(v -> showDetails());

        render();

    }

    void start(){if(closed)return;active=true;refresh();schedule();}

    void stop(){active=false;main.removeCallbacks(tick);if(details!=null)details.dismiss();}

    void close(){closed=true;stop();worker.shutdownNow();}

    private int intervalMinutes(){return WeatherPolicy.intervalMinutes(saved.getInt(WeatherPolicy.INTERVAL_KEY,WeatherPolicy.DEFAULT_MINUTES));}

    private void schedule(){
        main.removeCallbacks(tick);
        if(!active||closed||!configured()||!HeaderLayoutController.weatherShown(activity))return;
        long delay=WeatherPolicy.nextDelay(System.currentTimeMillis(),saved.getLong("fetched",0),saved.getLong("attempted",0),intervalMinutes());
        main.postDelayed(tick,Math.max(1000,delay));
    }

    private boolean configured(){return saved.getBoolean("enabled",false)&&!saved.getString("key","").isEmpty()

            &&!saved.getString("city","").isEmpty()&&WeatherPolicy.validCoordinates(WeatherPanel.coordinate(saved,"lat"),WeatherPanel.coordinate(saved,"lon"));}

    private void refresh() {

        render();if(!configured()||!HeaderLayoutController.weatherShown(activity)||busy||closed)return;

        long now=System.currentTimeMillis();

        if(!WeatherPolicy.shouldFetch(now,saved.getLong("fetched",0),saved.getLong("attempted",0),intervalMinutes()))return;

        busy=true;saved.edit().putLong("attempted",now).apply();

        String key=saved.getString("key","");double lat=WeatherPanel.coordinate(saved,"lat"),lon=WeatherPanel.coordinate(saved,"lon");

        String language=activity.getResources().getConfiguration().getLocales().get(0).getLanguage();

        worker.execute(()->{

            JSONObject data=null;int error=0;

            try{data=WeatherClient.current(lat,lon,key,language);}

            catch(WeatherClient.Failure failure){error=failure.message;}

            catch(java.io.IOException failure){error=R.string.weather_network_error;}

            final JSONObject received=data;final int failed=error;

            main.post(()->{

                busy=false;

                if(closed||!key.equals(saved.getString("key",""))||lat!=WeatherPanel.coordinate(saved,"lat")||lon!=WeatherPanel.coordinate(saved,"lon"))return;

                SharedPreferences.Editor editor=saved.edit();

                if(received!=null)editor.putString("cache",received.toString()).putLong("fetched",System.currentTimeMillis()).remove("error");

                else editor.putInt("error",failed);

                editor.apply();if(active){render();schedule();}

            });

        });

    }

    private void render() {

        host.setVisibility(!configured()?View.GONE:HeaderLayoutController.weatherShown(activity)?View.VISIBLE:View.INVISIBLE);if(!configured()||!HeaderLayoutController.weatherShown(activity))return;

        String name=saved.getString("city","");city.setText(name);
        city.setVisibility(saved.getBoolean("show_city",true)?View.VISIBLE:View.GONE);

        try {

            JSONObject data=new JSONObject(saved.getString("cache",""));

            JSONObject condition=data.getJSONArray("weather").getJSONObject(0),values=data.getJSONObject("main");

            String temp=Long.toString(Math.round(values.getDouble("temp")));

            temperature.setText(temp+"°");icon.setCode(condition.optString("icon"));

            long fetched=saved.getLong("fetched",0);

            boolean stale=saved.contains("error")||System.currentTimeMillis()-fetched>=WeatherPolicy.intervalMs(intervalMinutes());

            description.setText(stale?activity.getString(R.string.weather_offline,DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(fetched))):condition.optString("description"));

            JSONObject wind=data.optJSONObject("wind");

            host.setContentDescription(activity.getString(R.string.weather_details,name,temp,condition.optString("description"),Long.toString(Math.round(values.optDouble("feels_like",values.getDouble("temp")))),Integer.toString(values.optInt("humidity")),Long.toString(Math.round(wind==null?0:wind.optDouble("speed",0)*3.6)))+" OpenWeather");

        } catch(org.json.JSONException e) {

            temperature.setText("—");icon.setCode("");

            description.setText(saved.contains("error")?R.string.weather_unavailable:R.string.weather_loading);

            host.setContentDescription(name+": "+activity.getString(saved.getInt("error",R.string.weather_loading)));

        }

    }

    private void showDetails() {

        if(details!=null&&details.isShowing())return;

        details=new android.app.Dialog(activity);

        details.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);

        panel.setPadding(dp(24),dp(22),dp(24),dp(22));

        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();

        background.setColor(0xFA1B1D24);background.setCornerRadius(dp(22));background.setStroke(dp(1),0x55FFFFFF);panel.setBackground(background);

        TextView title=text(22,Color.WHITE);title.setText(saved.getString("city",""));panel.addView(title);

        try {

            JSONObject data=new JSONObject(saved.getString("cache",""));

            JSONObject values=data.getJSONObject("main"),condition=data.getJSONArray("weather").getJSONObject(0);

            LinearLayout summary=new LinearLayout(activity);summary.setGravity(Gravity.CENTER_VERTICAL);

            WeatherIcon largeIcon=new WeatherIcon(activity);largeIcon.setCode(condition.optString("icon"));

            summary.addView(largeIcon,new LinearLayout.LayoutParams(dp(62),dp(62)));

            LinearLayout texts=new LinearLayout(activity);texts.setOrientation(LinearLayout.VERTICAL);

            TextView current=text(34,Color.WHITE);current.setText(Math.round(values.getDouble("temp"))+" °C");texts.addView(current);

            TextView state=text(14,0xFFB8C6D7);state.setText(condition.optString("description"));texts.addView(state);

            summary.addView(texts);LinearLayout.LayoutParams summaryLp=new LinearLayout.LayoutParams(-1,-2);summaryLp.topMargin=dp(14);summaryLp.bottomMargin=dp(18);panel.addView(summary,summaryLp);

            detailRow(panel,R.string.weather_feels,Math.round(values.optDouble("feels_like",values.getDouble("temp")))+" °C");

            detailRow(panel,R.string.weather_humidity,values.optInt("humidity")+" %");

            JSONObject wind=data.optJSONObject("wind");

            detailRow(panel,R.string.weather_wind,Math.round(wind==null?0:wind.optDouble("speed",0)*3.6)+" km/h");

            String updated=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(saved.getLong("fetched",0)));

            TextView time=text(11,0xFFA9B7C8);time.setText(activity.getString(R.string.weather_updated,updated));time.setPadding(0,dp(16),0,dp(4));panel.addView(time);

        } catch(org.json.JSONException exception) {

            TextView unavailable=text(14,0xFFB8C6D7);unavailable.setText(saved.getInt("error",R.string.weather_loading));unavailable.setSingleLine(false);unavailable.setPadding(0,dp(16),0,dp(16));panel.addView(unavailable);

        }

        TextView credit=text(10,0xFF8997A8);credit.setText("OpenWeather");panel.addView(credit);

        TextView close=DesignPopup.button(activity,activity.getString(R.string.weather_close));

        LinearLayout.LayoutParams closeLp=new LinearLayout.LayoutParams(-1,dp(38));closeLp.topMargin=dp(18);panel.addView(close,closeLp);

        close.setOnClickListener(v -> details.dismiss());details.setContentView(panel);

        android.view.Window window=details.getWindow();

        if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);android.view.WindowManager.LayoutParams lp=window.getAttributes();lp.dimAmount=0.4f;window.setAttributes(lp);}

        details.show();if(window!=null)window.setLayout(dp(340),android.view.ViewGroup.LayoutParams.WRAP_CONTENT);close.requestFocus();

    }

    private void detailRow(LinearLayout panel,int label,String value){

        LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);

        TextView name=text(14,0xFFB8C6D7);name.setText(label);row.addView(name,new LinearLayout.LayoutParams(0,dp(34),1f));name.setGravity(Gravity.CENTER_VERTICAL);

        TextView content=text(16,Color.WHITE);content.setText(value);row.addView(content);

        panel.addView(row);

    }

    private TextView text(int size,int color){TextView v=new TextView(activity);v.setTextSize(size);v.setTextColor(color);v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);return v;}

    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}

}

