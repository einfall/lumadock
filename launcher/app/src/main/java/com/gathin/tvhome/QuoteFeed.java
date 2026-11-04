package com.gathin.tvhome;
import android.content.*;
import android.graphics.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;
/** Shared, foreground-only source: one page request for every visible quote instance. */
final class QuoteFeed {
    static final String MINUTES=HomeWidgetStore.P+"quote_minutes";
    static final String PAGE="https://www.spruch-des-tages.de/zufaellige-sprueche/";
    static final class State {final String text,imageUrl,note;final Bitmap image;State(String text,String url,Bitmap image,String note){this.text=text;imageUrl=url;this.image=image;this.note=note;}}
    private final Context context;private final SharedPreferences prefs;private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Consumer<State> changed;private final File cache;
    private volatile HttpURLConnection connection;private boolean active,images,busy,closed;private int minutes=1,generation;private long lastAttempt,lastImageAttempt;private String lastImageUrl="";private State state;
    QuoteFeed(Context c,Consumer<State> changed){context=c.getApplicationContext();prefs=c.getSharedPreferences(Config.SETTINGS_NAME,0);this.changed=changed;cache=new File(c.getCacheDir(),"quote-last-image");state=new State(prefs.getString("quote_cache_text",""),prefs.getString("quote_cache_url",""),null,"");}
    State state(){return state;}
    private final Runnable tick=()->request(false);
    void configure(boolean on,boolean needImages){int interval=Math.max(1,Math.min(1440,prefs.getInt(MINUTES,1)));boolean reschedule=interval!=minutes;minutes=interval;images=needImages;if(closed)return;if(on&&!active){active=true;request(false);}else if(!on&&active)stop();else if(active&&(reschedule||needImages&&state.image==null&&!state.imageUrl.isEmpty()))request(false);}
    void refresh(){if(active)request(true);}
    private void request(boolean force){if(!active||closed||busy)return;main.removeCallbacks(tick);long wait=minutes*60000L-(SystemClock.elapsedRealtime()-lastAttempt);boolean page=force||lastAttempt==0||wait<=0;if(!page&&(!images||state.image!=null||state.imageUrl.isEmpty()||state.imageUrl.equals(lastImageUrl)&&SystemClock.elapsedRealtime()-lastImageAttempt<60000)){main.postDelayed(tick,Math.max(1000,wait));return;}busy=true;int token=generation;boolean imageWanted=images;State previous=state;if(imageWanted){lastImageUrl=state.imageUrl;lastImageAttempt=SystemClock.elapsedRealtime();}if(page)lastAttempt=SystemClock.elapsedRealtime();worker.execute(()->{State next=previous;try{
            QuotePageParser.Entry entry=page?QuotePageParser.parse(new String(download(PAGE,QuotePageParser.MAX_HTML,false),StandardCharsets.UTF_8)):new QuotePageParser.Entry(previous.text,previous.imageUrl);
            Bitmap bitmap=entry.image.equals(previous.imageUrl)?previous.image:null;String cached=prefs.getString("quote_cache_image_url","");
            if(imageWanted&&bitmap==null){byte[] bytes=entry.image.equals(cached)&&cache.isFile()?read(new FileInputStream(cache),3145728):download(entry.image,3145728,true);BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<1||o.outHeight<1||o.outWidth>12000||o.outHeight>12000)throw new IOException("Bildgröße");o.inSampleSize=1;while(o.outWidth/o.inSampleSize>1024||o.outHeight/o.inSampleSize>1024)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.RGB_565;bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(bitmap==null)throw new IOException("Bildformat");try(FileOutputStream f=new FileOutputStream(cache)){f.write(bytes);}prefs.edit().putString("quote_cache_image_url",entry.image).apply();}
            next=new State(entry.text,entry.image,bitmap,"");
        }catch(Exception e){next=new State(previous.text,previous.imageUrl,previous.image,previous.text.isEmpty()?"Spruch nicht erreichbar · später erneut versuchen":"Letzter geladener Spruch · Quelle gerade nicht erreichbar");}
        State result=next;main.post(()->{if(token!=generation||closed)return;busy=false;state=result;if(!state.text.isEmpty())prefs.edit().putString("quote_cache_text",state.text).putString("quote_cache_url",state.imageUrl).apply();changed.accept(state);if(page&&!state.text.isEmpty()&&state.note.isEmpty())prefs.edit().putLong("quote_cache_loaded_at",System.currentTimeMillis()).apply();if(active&&images&&state.image==null&&!state.imageUrl.isEmpty()&&state.note.isEmpty()){request(false);return;}if(active)main.postDelayed(tick,Math.max(1000,minutes*60000L-(SystemClock.elapsedRealtime()-lastAttempt)));});});}
    private byte[] download(String url,int limit,boolean image)throws IOException {if(image&&!QuotePageParser.safeImage(url))throw new IOException("Bildquelle");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();connection=c;try{c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setRequestProperty("Cache-Control","no-cache");c.setRequestProperty("User-Agent","CleanYourTV-QuoteWidget/1.0");if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());String type=c.getContentType();if(type==null||!(image?type.startsWith("image/"):type.contains("text/html")))throw new IOException("Inhaltstyp");if(c.getContentLengthLong()>limit)throw new IOException("Dateigröße");return read(c.getInputStream(),limit);}finally{connection=null;c.disconnect();}}
    private static byte[] read(InputStream input,int limit)throws IOException {try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Datei zu groß");out.write(b,0,n);}return out.toByteArray();}}
    void stop(){active=false;generation++;busy=false;main.removeCallbacks(tick);HttpURLConnection c=connection;if(c!=null)c.disconnect();}
    void close(){closed=true;stop();worker.shutdownNow();}
}
