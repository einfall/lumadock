package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
/** One bounded, sampled image read on the saver worker. Network images are cached locally. */
final class SaverImage {
    private static final int MAX_BYTES=12*1024*1024;
    private static File cache(Context c,String source)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(source.getBytes("UTF-8"));StringBuilder name=new StringBuilder();for(byte b:digest)name.append(String.format(java.util.Locale.ROOT,"%02x",b));return new File(new File(c.getCacheDir(),"saver-images"),name.toString());}
    static void dropCache(Context c,String source){if(LiveWallpaperSource.network(source))try{cache(c,source).delete();}catch(Exception ignored){}}

    static Bitmap load(Context c,String source,boolean asset,int w,int h)throws Exception{
        byte[] data;
        if(asset){try(InputStream in=c.getAssets().open("saver_photos/"+source)){data=read(in);}}
        else if(LiveWallpaperSource.network(source)){
            File dir=new File(c.getCacheDir(),"saver-images");if(!dir.isDirectory())dir.mkdirs();
            File cached=cache(c,source);
            if(cached.isFile()&&cached.length()>0&&cached.length()<=MAX_BYTES){try(InputStream in=new FileInputStream(cached)){data=read(in);}}
            else{HttpURLConnection request=(HttpURLConnection)new URL(source).openConnection();request.setConnectTimeout(8000);request.setReadTimeout(8000);request.setRequestProperty("User-Agent","TvHome-Screensaver/1.0");
                try{int code=request.getResponseCode();if(code!=200||request.getContentLengthLong()>MAX_BYTES)throw new IOException("Image unavailable");try(InputStream in=request.getInputStream()){data=read(in);}}
                finally{request.disconnect();}
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                try(FileOutputStream out=new FileOutputStream(cached)){out.write(data);}catch(IOException ignored){}
                File[] files=dir.listFiles();if(files!=null&&files.length>3){java.util.Arrays.sort(files,java.util.Comparator.comparingLong(File::lastModified));for(int i=0;i<files.length-3;i++)files[i].delete();}
            }
        }else{Uri uri=source.startsWith("/")?Uri.fromFile(new File(source)):Uri.parse(source);
            if("file".equalsIgnoreCase(uri.getScheme())){try(InputStream in=new FileInputStream(uri.getPath())){data=read(in);}}
            else{try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();data=read(in);}}
        }
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);
        if(o.outWidth<=0||o.outHeight<=0){dropCache(c,source);throw new IOException("Invalid image");}
        int sample=WallpaperSize.sampleSize(o.outWidth,o.outHeight,w,h);while(o.outWidth/sample>4096||o.outHeight/sample>4096)sample*=2;
        if(!WallpaperSize.accepted(o.outWidth/sample,o.outHeight/sample))throw new IOException("Image too large");
        int[] scale=WallpaperSize.coverScale(o.outWidth/sample,o.outHeight/sample,w,h);o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.RGB_565;
        if(scale!=null){o.inScaled=true;o.inDensity=scale[0];o.inTargetDensity=scale[1];}
        Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,o);if(bitmap==null)dropCache(c,source);return bitmap;
    }
    private static byte[] read(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>MAX_BYTES)throw new IOException("Image too large");out.write(buf,0,n);}return out.toByteArray();
    }
}
