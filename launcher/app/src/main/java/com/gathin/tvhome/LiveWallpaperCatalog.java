package com.gathin.tvhome;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** The requested original t/u/s manifest, with a user-editable external copy and bundled fallback. */
final class LiveWallpaperCatalog {
    static final String FILE="live_wallpaper.json";
    static final class Item{final String title,url,source;Item(String t,String u,String s){title=t;url=u;source=s;}}
    static File file(Context c){File dir=c.getExternalFilesDir(null);return new File(dir==null?c.getFilesDir():dir,FILE);}
    static void ensureFile(Context c){File f=file(c);if(f.exists())return;try{File parent=f.getParentFile();if(parent!=null&&!parent.isDirectory()&&!parent.mkdirs())return;try(InputStream in=c.getAssets().open(FILE);OutputStream out=new FileOutputStream(f)){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))>=0)out.write(bytes,0,n);}}catch(IOException ignored){}}
    static List<Item> load(Context c){ensureFile(c);try{File f=file(c);if(f.length()>0&&f.length()<=512*1024){try(InputStream in=new FileInputStream(f)){List<Item> data=parse(read(in));if(!data.isEmpty())return data;}}}catch(IOException|JSONException ignored){}
        try(InputStream in=c.getAssets().open(FILE)){return parse(read(in));}catch(IOException|JSONException e){return Collections.emptyList();}
    }
    private static String read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))>=0){if(out.size()+n>512*1024)throw new IOException("Catalog too large");out.write(bytes,0,n);}return out.toString(StandardCharsets.UTF_8.name());}
    private static List<Item> parse(String text)throws JSONException{JSONArray a=new JSONArray(text);List<Item> list=new ArrayList<>();Set<String> seen=new HashSet<>();for(int i=0;i<a.length()&&list.size()<1000;i++){JSONObject j=a.optJSONObject(i);if(j==null)continue;String url=LiveWallpaperSource.clean(j.optString("u",""));String title=j.optString("t","").trim();if(title.isEmpty()||title.length()>160||!LiveWallpaperSource.valid(url)||!seen.add(url))continue;list.add(new Item(title,url,j.optString("s","")));}return list;}
    static String sourceName(String s){switch(s){case "A":return "Apple Aerials";case "Z":return "Amazon";default:return "Community";}}
}
