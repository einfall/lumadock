package com.gathin.tvhome;
import android.content.*;
import android.graphics.*;
import org.json.*;
import java.io.*;
import java.util.*;
final class SaverCatalog {
    static final class Photo {final String file,title,region,author,license,source,licenseUrl;
        Photo(JSONObject j){file=j.optString("file");title=j.optString("place");region=j.optString("region");author=j.optString("author");license=j.optString("license");source=j.optString("source");licenseUrl=j.optString("license_url");}
        String caption(){return title+" · "+author+" · "+license;}
    }
    static List<Photo> photos(Context c){List<Photo> list=new ArrayList<>();try(InputStream in=c.getAssets().open("saver_photos/photos.json")){
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0){if(bytes.size()+n>65536)throw new IOException();bytes.write(buf,0,n);}JSONArray json=new JSONArray(bytes.toString("UTF-8"));
        for(int i=0;i<json.length()&&list.size()<100;i++){JSONObject j=json.optJSONObject(i);if(j!=null&&SaverPolicy.photoId(j.optString("file")))list.add(new Photo(j));}
    }catch(IOException|JSONException ignored){}return list;}
    static Photo find(List<Photo> list,String id){for(Photo p:list)if(p.file.equals(id))return p;return list.isEmpty()?null:list.get(0);}
    static Bitmap thumb(Context c,String file){try(InputStream in=c.getAssets().open("saver_photos/"+file)){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=16;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeStream(in,null,o);}catch(IOException|OutOfMemoryError e){return null;}}
}
