package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** HTTPS only; keys never appear in logs or user-facing error messages. */
final class WeatherClient {
    private WeatherClient() { }
    static final class Failure extends IOException {
        final int message;
        Failure(int message) { super("Weather request failed"); this.message = message; }
    }
    static final class City {
        final String name, region, country;
        final double lat, lon;
        City(String name, String region, String country, double lat, double lon) {
            this.name=name; this.region=region; this.country=country; this.lat=lat; this.lon=lon;
        }
        String label() { return name + (region.isEmpty() ? "" : ", " + region) + (country.isEmpty() ? "" : " · " + country); }
    }
    static List<City> search(String query, String key, String language) throws IOException {
        try {
            JSONArray rows = new JSONArray(get("/geo/1.0/direct?q=" + encode(query) + "&limit=5&appid=" + encode(key)));
            List<City> cities = new ArrayList<>();
            for(int i=0; i<rows.length(); i++) {
                JSONObject row=rows.getJSONObject(i);
                double lat=row.getDouble("lat"), lon=row.getDouble("lon");
                if (!WeatherPolicy.validCoordinates(lat,lon)) continue;
                JSONObject names=row.optJSONObject("local_names");
                String name=names==null ? row.getString("name") : names.optString(language,row.getString("name"));
                cities.add(new City(name,row.optString("state"),row.optString("country"),lat,lon));
            }
            return cities;
        } catch(JSONException e) { throw new Failure(R.string.weather_bad_response); }
    }
    static JSONObject current(double lat,double lon,String key,String language) throws IOException {
        if (!WeatherPolicy.validCoordinates(lat,lon)) throw new Failure(R.string.weather_missing);
        try {
            JSONObject data=new JSONObject(get(String.format(Locale.US,
                    "/data/2.5/weather?lat=%.6f&lon=%.6f&units=metric&lang=%s&appid=%s",lat,lon,encode(language),encode(key))));
            data.getJSONObject("main").getDouble("temp");
            data.getJSONArray("weather").getJSONObject(0).getString("icon");
            return data;
        } catch(JSONException e) { throw new Failure(R.string.weather_bad_response); }
    }
    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }
    private static String get(String path) throws IOException {
        HttpURLConnection connection=null;
        try {
            connection=(HttpURLConnection)new URL("https://api.openweathermap.org"+path).openConnection();
            connection.setConnectTimeout(10_000); connection.setReadTimeout(10_000);
            connection.setInstanceFollowRedirects(false);
            int code=connection.getResponseCode();
            if(code==401 || code==403) throw new Failure(R.string.weather_invalid_key);
            if(code==429) throw new Failure(R.string.weather_rate_limit);
            if(code!=200) throw new Failure(R.string.weather_service_error);
            try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1) {
                    if(out.size()+n>131072) throw new Failure(R.string.weather_bad_response);
                    out.write(buffer,0,n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } catch(Failure e) { throw e; }
        catch(IOException e) { throw new Failure(R.string.weather_network_error); }
        finally { if(connection!=null) connection.disconnect(); }
    }
}
