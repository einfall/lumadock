package com.gathin.tvhome.aura;
// Adapted from Aura TV, MIT (c) 2026 iravi; assets/aura/NOTICE.txt.

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.ConnectivityManager;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Screensaver flyovers: time-lapses of Earth shot from the International Space Station, frame by
 * frame, by its crews (NASA, public domain, through Wikimedia Commons), hand-picked from hundreds
 * for clear views: deserts and coastlines, seas of cloud, aurora and night cities. Off until asked
 * for. Five clips live on the TV (80 MB at most), fetched one at a time over Wi-Fi only when the TV
 * has room to spare; each day one is swapped for the next, so it keeps changing. Played by the
 * hardware decoder.
 */
public final class Flyovers {
    static final int JOB = 43;
    private static final String DIR = "aura-flyovers-vp9";
    private static final String API = "https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=videoinfo&viprop=url%7Csize%7Cderivatives&titles=";
    private static final String UA = "TvHome-AuraScenes/1.0 (https://github.com/cookedzera/aura-tv; ISS flyovers)";
    private static final int KEEP = 5;
    private static final long MAX_BYTES = 35_000_000L, MAX_TOTAL = 80_000_000L, MIN_FREE = 400_000_000L, SWAP_MS = 86_400_000L;

    /** Commons file, what it shows, a line about it. Small ones first, so the first arrive quickly; day and night alternate. */
    static final String[][] CLIPS = {
            {"ISS074-E-171382-175181-20260104-Night - 2026-01-04 ISS074 3800 Night.webm", "Night pass", "Cities glow under the clouds as the station crosses the dark side, 4 January 2026."},
            {"ISS074-E-320968-324167-20260221-Night - 2026-02-21 ISS074 3200 Night.webm", "City lights", "Towns and rivers of light drift by in the dark, 21 February 2026."},
            {"ISS072-E-1099547-1101346-20250414-Day - 2025-04-14 ISS072 1800 Day.webm", "Through the window", "A round window frames a whole ocean of cloud, 14 April 2025."},
            {"ISS072-E-1059419-1062418-20250407-Both - 2025-04-07 ISS072 3000 Day and Night.webm", "Into the sun", "The sun blazes past the solar arrays as Earth turns below, 7 April 2025."},
            {"ISS074-E-309463-312662-20260205-Day - 2026-02-05 ISS074 3200 Day.webm", "Above the clouds", "Fields of cloud slide by beneath the solar arrays, 5 February 2026."},
            {"ISS074-E-443330-446529-20260404-Day - 2026-04-04 ISS074 3200 Day.webm", "Beside the arm", "Coastlines and deserts pass the station's robotic arm, 4 April 2026."},
            {"ISS072-E-1137801-1139085-20250419-Day - 2025-04-19 ISS072 1285 Day.webm", "Coastlines", "Peninsulas and blue seas roll past the window, 19 April 2025."},
            {"ISS074-E-267623-270822-20260117-Night - 2026-01-17 ISS074 3200 Night.webm", "Aurora", "Green light glows along the horizon above the lights of cities, 17 January 2026."},
            {"ISS072-E-1114317-1115516-20250415-Day - 2025-04-15 ISS072 1200 Day.webm", "Desert light", "Open ocean gives way to the reds of the desert, 15 April 2025."},
            {"ISS074-E-326638-329837-20260222-Both - 2026-02-22 ISS074 3200 Day and Night.webm", "Night into day", "City lights fade as sunrise races along the edge of the Earth, 22 February 2026."},
            {"ISS072-E-1135498-1136765-20250418-Day - 2025-04-18 ISS072 1268 Day.webm", "Rivers in the sand", "A river threads through the desert far below, 18 April 2025."},
            {"ISS072-E-1047646-1049380-20250410-Day - 2025-04-10 ISS072 1735 Day.webm", "Sahara to the sea", "Sand gives way to surf beside a visiting spacecraft, 10 April 2025."},
            {"ISS074-E-317295-319294-20260213-Day - 2026-02-13 ISS074 2000 Day.webm", "Under the arrays", "Clouds slide past the station's solar arrays, 13 February 2026."},
            {"ISS072-E-1106907-1109406-20250414-Day - 2025-04-14 ISS072 2500 Day.webm", "Seas of cloud", "Weather systems swirl below the station's window, 14 April 2025."},
            {"ISS074-E-202133-205178-20260104-Both - 2026-01-04 ISS074 3046 Day and Night.webm", "Morning over the clouds", "Sunlight spills over the clouds beside a docked spacecraft, 4 January 2026."},
            {"ISS074-E-350124-353323-20260228-Night - 2026-02-28 ISS074 3200 Night.webm", "Night shift", "Cities glimmer below as the station sails through the night, 28 February 2026."},
    };

    private static final Object LOCK = new Object();
    private static Boolean playable;

    private Flyovers() { }

    /** Only with a hardware VP9 decoder: a software one would make an old TV work hard for a screensaver. */
    public static boolean supported() {
        Boolean p = playable;
        if (p != null) return p;
        boolean found = false;
        try {
            for (int i = 0; i < MediaCodecList.getCodecCount() && !found; i++) {
                MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
                if (info.isEncoder()) continue;
                String name = info.getName().toLowerCase(java.util.Locale.ROOT);
                if (name.startsWith("omx.google.") || name.startsWith("c2.android.") || name.contains(".sw.")) continue;
                for (String t : info.getSupportedTypes()) if ("video/x-vnd.on2.vp9".equalsIgnoreCase(t)) found = true;
            }
        } catch (RuntimeException ignored) {
        }
        playable = found;
        return found;
    }

    private static File dir(Context c) {
        if(!Prefs.device(c).getBoolean("fly_vp9_migrated",false)){
            File legacy=new File(c.getFilesDir(),"aura-flyovers");File[] old=legacy.listFiles((folder,name)->name.matches("[0-9]+\\.webm(\\.part)?"));
            if(old!=null)for(File f:old)f.delete();legacy.delete();Prefs.device(c).edit().putBoolean("fly_vp9_migrated",true).apply();
        }
        File d = new File(c.getFilesDir(), DIR);
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    /** Index into {@link #CLIPS} from a file name, or -1. */
    static int index(File f) {
        try {
            return Integer.parseInt(f.getName().replace(".webm", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The clips ready to play, oldest download first. */
    public static List<File> ready(Context c) {
        File[] fs = dir(c).listFiles((d, n) -> n.endsWith(".webm"));
        List<File> out = new ArrayList<>();
        if (fs == null) return out;
        Arrays.sort(fs, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : fs) {
            int i = index(f);
            if (i >= 0 && i < CLIPS.length) out.add(f);
        }
        return out;
    }

    /** Where the screensaver starts this time: a different clip each time it comes on. */
    public static int nextStart(Context c, int count) {
        android.content.SharedPreferences p = Prefs.device(c);
        int at = p.getInt("fly_at", 0);
        p.edit().putInt("fly_at", at + 1).apply();
        return Compat.mod(at, Math.max(1, count));
    }

    /** Only called by the active saver worker. No scheduled/background job is installed. */
    public static void load(Context c,java.util.function.Consumer<File> first)throws IOException,InterruptedException {
        if(!supported())throw new IOException("Hardware VP9 unavailable");
        synchronized(LOCK){
            List<File> have=ready(c);File playing=null;
            if(!have.isEmpty()){playing=have.get(nextStart(c,have.size()));first.accept(playing);}
            ConnectivityManager cm=Compat.svc(c,ConnectivityManager.class);
            if(!Compat.online(cm)||cm.isActiveNetworkMetered()){if(playing==null)throw new IOException("Unmetered network required");return;}
            if(have.size()>=KEEP){
                if(System.currentTimeMillis()-have.get(have.size()-1).lastModified()<SWAP_MS)return;
                for(File f:new ArrayList<>(have))if(!f.equals(playing)){if(f.delete())have.remove(f);break;}
            }
            int next=Prefs.device(c).getInt("fly_next",0);
            for(int tries=0;tries<CLIPS.length&&have.size()<KEEP;tries++,next++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
                int at=Compat.mod(next,CLIPS.length);File f=new File(dir(c),at+".webm");if(f.exists())continue;
                long used=0;for(File file:have)used+=file.length();
                if(dir(c).getUsableSpace()<MIN_FREE||used>=MAX_TOTAL)break;
                try{
                    if(fetch(at,f,Math.min(MAX_BYTES,MAX_TOTAL-used))){have.add(f);android.util.Log.i("TvHome.Aura","space-clip-ready "+at);if(playing==null){playing=f;first.accept(f);}}
                }catch(IOException e){Prefs.device(c).edit().putInt("fly_next",next+1).apply();if(playing==null)throw e;return;}
                Prefs.device(c).edit().putInt("fly_next",next+1).apply();
            }
            if(playing==null)throw new IOException("No clip ready");
        }
    }
    public static File next(Context c,File current){List<File> files=ready(c);if(files.isEmpty())return null;return files.get(Compat.mod(files.indexOf(current)+1,files.size()));}
    public static String caption(File f){int i=index(f);return i>=0&&i<CLIPS.length?CLIPS[i][1]+" · NASA / ISS · Public domain":"NASA / ISS · Public domain";}
    /** One clip: where Commons keeps it, then the file itself, written whole or not at all. */
    private static boolean fetch(int i, File dest, long room) throws IOException, InterruptedException {
        HttpURLConnection con = open(API + URLEncoder.encode("File:" + CLIPS[i][0], "UTF-8"));
        String url="";
        long size=-1;
        try (InputStream in = con.getInputStream()) {
            JSONObject pages = new JSONObject(new String(readAll(in), "UTF-8")).getJSONObject("query").getJSONObject("pages");
            JSONObject info = pages.getJSONObject(pages.keys().next()).getJSONArray("videoinfo").getJSONObject(0);
            JSONArray variants=info.optJSONArray("derivatives");int best=0;
            for(int n=0;variants!=null&&n<variants.length();n++){
                JSONObject item=variants.optJSONObject(n);if(item==null)continue;
                int rank=ClipVariant.rank(item.optString("type"),item.optString("src"),item.optInt("width"),item.optInt("height"),item.optLong("bandwidth"),info.optDouble("duration"),room);
                if(rank>best){best=rank;url=item.optString("src");}
            }
            if(best==0)return false;
        } catch (JSONException | RuntimeException e) {
            return false;
        } finally {
            con.disconnect();
        }
        // too big for what's left of the budget: the next clip in the list gets the turn
        if (url.isEmpty()) return false;
        Thread.sleep(3_000);
        File part = new File(dest.getPath() + ".part");
        if(!url.startsWith("https://upload.wikimedia.org/"))return false;
        con = open(url);size=con.getContentLengthLong();if(size>room){con.disconnect();return false;}
        try (InputStream in = con.getInputStream(); OutputStream out = new FileOutputStream(part)) {
            byte[] buf = new byte[32 * 1024];
            long total = 0;
            for (int n; (n = in.read(buf)) > 0; ) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                total += n;
                if (total > room || total > MAX_BYTES || (size>0&&total>size)) throw new IOException("larger than expected");
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            part.delete();
            throw e;
        } finally {
            con.disconnect();
        }
        if(part.length()==0||(size>0&&part.length()!=size)){part.delete();return false;}
        android.media.MediaExtractor extractor=new android.media.MediaExtractor();boolean vp9=false;
        try{extractor.setDataSource(part.getPath());for(int track=0;track<extractor.getTrackCount();track++)if("video/x-vnd.on2.vp9".equals(extractor.getTrackFormat(track).getString(android.media.MediaFormat.KEY_MIME)))vp9=true;}
        catch(Exception e){part.delete();return false;}finally{extractor.release();}
        if(!vp9){part.delete();return false;}
        return part.renameTo(dest);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestProperty("User-Agent", UA);
        con.setConnectTimeout(15_000);
        con.setReadTimeout(30_000);
        int code = con.getResponseCode();
        if (code != 200) {
            con.disconnect();
            throw new IOException("HTTP " + code);
        }
        return con;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8 * 1024];
        for (int n; (n = in.read(buf)) > 0; ){if(Thread.currentThread().isInterrupted()||bo.size()+n>512*1024)throw new InterruptedIOException();bo.write(buf,0,n);}
        return bo.toByteArray();
    }

}
