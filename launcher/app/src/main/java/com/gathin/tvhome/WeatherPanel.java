package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Staged OpenWeather settings. A city search never overwrites the saved selection. */
final class WeatherPanel {
    static final String PREFS="openweather";
    final ScrollView view;
    private final Activity activity;
    private final SharedPreferences saved;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final CheckBox enabled, showCity;
    private final EditText api, query;
    private final TextView status, selectedLabel;
    private final LinearLayout results;
    private WeatherClient.City selected;
    private boolean closed;
    private int generation;
    private int intervalMinutes;
    private final String language;
    private Runnable changed=()->{};
    void setOnChanged(Runnable callback){changed=callback;enabled.setOnCheckedChangeListener((v,on)->changed.run());showCity.setOnCheckedChangeListener((v,on)->changed.run());api.setOnFocusChangeListener((v,on)->{if(!on)changed.run();});}
    boolean validQuiet(){return !enabled.isChecked()||(!key().isEmpty()&&selected!=null);}

    WeatherPanel(Activity activity) {
        this.activity=activity;
        language=activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
        saved=activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        view=new ScrollView(activity);
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);
        view.addView(body);
        enabled=new CheckBox(activity);enabled.setText(R.string.weather_enable);enabled.setTextColor(Color.WHITE);
        enabled.setTextSize(14);enabled.setChecked(saved.getBoolean("enabled",false));
        body.addView(enabled,new LinearLayout.LayoutParams(-1,dp(36)));
        showCity=new CheckBox(activity);showCity.setText(R.string.weather_show_city);showCity.setTextColor(Color.WHITE);
        showCity.setTextSize(14);showCity.setChecked(saved.getBoolean("show_city",true));
        body.addView(showCity,new LinearLayout.LayoutParams(-1,dp(36)));
        intervalMinutes=WeatherPolicy.intervalMinutes(saved.getInt(WeatherPolicy.INTERVAL_KEY,WeatherPolicy.DEFAULT_MINUTES));
        TextView intervalTitle=label(R.string.weather_interval,12);
        intervalTitle.setPadding(0,dp(8),0,dp(5));body.addView(intervalTitle);
        LinearLayout intervals=new LinearLayout(activity);
        int[] minutes={10,15,30,60,120};
        TextView[] intervalButtons=new TextView[minutes.length];
        for(int i=0;i<minutes.length;i++) {
            final int value=minutes[i];
            TextView button=DesignPopup.button(activity,Integer.toString(value));
            button.setContentDescription(activity.getString(R.string.weather_interval_choice,value));
            button.setActivated(intervalMinutes==value);intervalButtons[i]=button;
            button.setOnClickListener(v -> {
                intervalMinutes=value;
                for(int j=0;j<minutes.length;j++)intervalButtons[j].setActivated(minutes[j]==value);
                changed.run();
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(34),1f);
            if(i>0)lp.setMarginStart(dp(6));intervals.addView(button,lp);
        }
        body.addView(intervals);
        TextView intervalHint=label(R.string.weather_interval_hint,11);
        intervalHint.setTextColor(0xFFB8BFCE);intervalHint.setPadding(0,dp(4),0,dp(8));body.addView(intervalHint);
        body.addView(label(R.string.weather_key,12));
        api=field(R.string.weather_key_hint,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        api.setText(saved.getString("key",""));
        body.addView(api,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView cityTitle=label(R.string.weather_city,12);cityTitle.setPadding(0,dp(10),0,dp(4));body.addView(cityTitle);
        LinearLayout searchLine=new LinearLayout(activity);
        query=field(R.string.weather_city_hint,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        query.setText(saved.getString("city",""));
        searchLine.addView(query,new LinearLayout.LayoutParams(0,dp(38),1f));
        TextView search=DesignPopup.button(activity,activity.getString(R.string.weather_search));
        LinearLayout.LayoutParams searchLp=new LinearLayout.LayoutParams(dp(82),dp(38));searchLp.setMarginStart(dp(8));
        searchLine.addView(search,searchLp);body.addView(searchLine);
        selectedLabel=label(R.string.weather_choose,13);selectedLabel.setTextColor(0xFF9ED6FF);
        selectedLabel.setPadding(0,dp(10),0,dp(5));body.addView(selectedLabel);
        double lat=coordinate(saved,"lat"),lon=coordinate(saved,"lon");
        String city=saved.getString("city","");
        if(!city.isEmpty()&&WeatherPolicy.validCoordinates(lat,lon)) {
            selected=new WeatherClient.City(city,saved.getString("region",""),saved.getString("country",""),lat,lon);
            updateSelected();
        }
        status=label(R.string.weather_attribution,11);status.setTextColor(0xFFB8BFCE);status.setPadding(0,dp(5),0,dp(8));
        body.addView(status);
        results=new LinearLayout(activity);results.setOrientation(LinearLayout.VERTICAL);body.addView(results);
        search.setOnClickListener(v -> {
            String key=key(), text=query.getText().toString().trim();
            if(key.isEmpty()||text.isEmpty()){status.setText(R.string.weather_missing);return;}
            InputMethodManager keyboard=(InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(query.getWindowToken(),0);
            int request=++generation;
            status.setText(R.string.weather_searching);results.removeAllViews();search.setEnabled(false);
            worker.execute(() -> {
                try {
                    java.util.List<WeatherClient.City> cities=WeatherClient.search(text,key,language);
                    activity.runOnUiThread(() -> {
                        if(closed||request!=generation)return;
                        search.setEnabled(true);
                        status.setText(cities.isEmpty()?R.string.weather_no_results:R.string.weather_choose);
                        for(WeatherClient.City cityResult:cities) {
                            TextView choice=DesignPopup.button(activity,cityResult.label());
                            choice.setTextSize(12);choice.setGravity(Gravity.CENTER_VERTICAL);
                            choice.setPadding(dp(14),0,dp(14),0);choice.setMaxLines(2);
                            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(42));lp.bottomMargin=dp(5);
                            results.addView(choice,lp);
                            choice.setOnClickListener(button -> {
                                selected=cityResult;enabled.setChecked(true);updateSelected();changed.run();
                                for(int i=0;i<results.getChildCount();i++)results.getChildAt(i).setActivated(results.getChildAt(i)==choice);
                                preview(key,cityResult);
                            });
                        }
                        if(!cities.isEmpty())results.getChildAt(0).requestFocus();
                    });
                } catch(WeatherClient.Failure failure) { error(request,search,failure.message); }
                catch(java.io.IOException failure) { error(request,search,R.string.weather_network_error); }
            });
        });
    }
    private void preview(String key,WeatherClient.City city) {
        int request=++generation;status.setText(R.string.weather_loading);
        worker.execute(() -> {
            try {
                JSONObject data=WeatherClient.current(city.lat,city.lon,key,language);
                String temperature=Long.toString(Math.round(data.getJSONObject("main").getDouble("temp")));
                String description=data.getJSONArray("weather").getJSONObject(0).optString("description");
                activity.runOnUiThread(() -> {if(!closed&&request==generation)status.setText(activity.getString(R.string.weather_ready,city.name,temperature,description));});
            } catch(WeatherClient.Failure failure) { error(request,null,failure.message); }
            catch(Exception failure) { error(request,null,R.string.weather_bad_response); }
        });
    }
    private void error(int request,View search,int message) {
        activity.runOnUiThread(() -> {if(!closed&&request==generation){status.setText(message);if(search!=null)search.setEnabled(true);}});
    }
    private String key(){return api.getText().toString().trim();}
    private void updateSelected(){selectedLabel.setText(activity.getString(R.string.weather_selected,selected.label()));}
    boolean valid() {
        if(enabled.isChecked()&&(key().isEmpty()||selected==null)) {status.setText(R.string.weather_missing);return false;}
        return true;
    }
    boolean changed() {
        return intervalMinutes!=WeatherPolicy.intervalMinutes(saved.getInt(WeatherPolicy.INTERVAL_KEY,WeatherPolicy.DEFAULT_MINUTES))
                ||showCity.isChecked()!=saved.getBoolean("show_city",true)
                ||enabled.isChecked()!=saved.getBoolean("enabled",false)
                ||!key().equals(saved.getString("key",""))
                ||(selected!=null&&(!selected.name.equals(saved.getString("city",""))
                ||selected.lat!=coordinate(saved,"lat")||selected.lon!=coordinate(saved,"lon")));
    }
    void save() {
        if(!changed())return;
        boolean sourceChanged=!key().equals(saved.getString("key",""))
                ||(selected!=null&&(selected.lat!=coordinate(saved,"lat")||selected.lon!=coordinate(saved,"lon")));
        SharedPreferences.Editor editor=saved.edit().putBoolean("enabled",enabled.isChecked())
                .putBoolean("show_city",showCity.isChecked()).putString("key",key())
                .putInt(WeatherPolicy.INTERVAL_KEY,intervalMinutes);
        if(sourceChanged)editor.remove("cache").remove("fetched").remove("attempted").remove("error");
        if(selected!=null)editor.putString("city",selected.name).putString("region",selected.region).putString("country",selected.country)
                .putString("lat",Double.toString(selected.lat)).putString("lon",Double.toString(selected.lon));
        editor.apply();
    }
    void close(){closed=true;generation++;worker.shutdownNow();}
    static double coordinate(SharedPreferences prefs,String name) {
        try{return Double.parseDouble(prefs.getString(name,"NaN"));}catch(NumberFormatException e){return Double.NaN;}
    }
    private EditText field(int hint,int type) {
        EditText edit=new EditText(activity);edit.setSingleLine(true);edit.setTextSize(14);edit.setTextColor(Color.WHITE);
        edit.setHintTextColor(0xFF949DAE);edit.setHint(hint);edit.setInputType(type);
        edit.setBackgroundResource(R.drawable.weather_field);edit.setPadding(dp(12),0,dp(12),0);
        edit.setSelectAllOnFocus(true);return edit;
    }
    private TextView label(int text,int size){TextView v=new TextView(activity);v.setText(text);v.setTextSize(size);v.setTextColor(Color.WHITE);return v;}
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
