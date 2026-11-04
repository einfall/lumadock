package com.gathin.tvhome.aura;

// Adapted from Aura TV, MIT (c) 2026 iravi; see assets/aura/NOTICE.txt.

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.json.JSONArray;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Your photographs as an editorial spread: one large on the left, two stacked on the right, on
 * near-black with fine gaps. Every few seconds one tile folds over like a turned card to show the
 * next photograph, and each drifts slowly inside its frame. Photos sent from the phone come first,
 * then the Pictures of the Day, then the built-in set; each is decoded at its tile's size.
 */
final class Mosaic extends Scene {
    private static final long TURN_MS = 7_000, FOLD_MS = 1_100;

    private static final class Tile {
        final RectF box = new RectF();
        Bitmap now, next;
        Photo wall, nextWall;
        long foldAt = -1, shownAt;
        final Matrix m = new Matrix();
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "aura-mosaic"));
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Tile[] tiles = {new Tile(), new Tile(), new Tile()};
    private final List<Photo> deck = new ArrayList<>();
    private final Paint pic = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG), shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Camera camera = new Camera();
    private final Matrix fold = new Matrix();
    private int dealt, turn, generation, failures;
    private volatile boolean running;
    private long started, nextTurn;

    private final AuraScenes.ImageLoader loader;
    private final String custom;
    private static final class Photo {String file,path,place,region,author,license;boolean mine;Photo(org.json.JSONObject o){file=o.optString("file");place=o.optString("place");region=o.optString("region");author=o.optString("author");license=o.optString("license");}}
    Mosaic(Context c,AuraScenes.ImageLoader loader,String custom) {
        super(c);this.loader=loader;this.custom=custom;
        setBackgroundColor(0xFF050608);
    }

    /** Is there anything to show? (The built-in set is always there.) */
    static boolean available() {
        return true;
    }

    @Override
    void start() {
        if (disposed) return;
        running = true;
        generation++;
        started = SystemClock.uptimeMillis();
        nextTurn = started + 4_000;
        deck.clear();
        Context c = getContext();
        if(custom!=null&&!custom.isEmpty()){Photo own=new Photo(new org.json.JSONObject());own.path=custom;own.mine=true;own.place="Eigenes Foto";deck.add(own);}
        List<Photo> built = builtIn(c);
        Collections.shuffle(built);

        deck.addAll(built);
        dealt = 0;
        layout(getWidth(), getHeight());
        if (getWidth() == 0) {
            post(this::start);
            return;
        }
        for (Tile t : tiles) load(t, true);
        invalidate();
    }

    @Override
    void stop() {
        running = false;
        generation++;
        main.removeCallbacksAndMessages(null);
        main.post(() -> {
            for (Tile t : tiles) {
                if (t.now != null) t.now.recycle();
                if (t.next != null) t.next.recycle();
                t.now = t.next = null;
            }
        });
    }

    private static List<Photo> builtIn(Context c) {
        List<Photo> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("saver_photos/photos.json")) {
            java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
            JSONArray a = new JSONArray(new String(b.toByteArray(), "UTF-8"));
            for (int i = 0; i < a.length(); i++) out.add(new Photo(a.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    /** One large tile on the left, two on the right: the proportions of a magazine spread. */
    private void layout(int w, int h) {
        float s = h / 720f, m = 48 * s, gap = 12 * s;
        float leftW = (w - 2 * m - gap) * 0.62f;
        tiles[0].box.set(m, m, m + leftW, h - m);
        float rx = m + leftW + gap, half = (h - 2 * m - gap) / 2;
        tiles[1].box.set(rx, m, w - m, m + half);
        tiles[2].box.set(rx, m + half + gap, w - m, h - m);
    }

    /** The next photograph for {@code t}, decoded off the main thread at the tile's size. */
    private void load(Tile t, boolean first) {
        if (deck.isEmpty()) return;
        final Photo w = deck.get(dealt++ % deck.size());
        final int tw = Math.round(t.box.width()), th = Math.round(t.box.height()), gen = generation;
        final Context app = getContext().getApplicationContext();
        work(worker, () -> {
            Bitmap b = decode(app, w, tw, th);
            main.post(() -> {
                if(gen!=generation||!running){if(b!=null)b.recycle();return;}
                if(b==null){if(++failures>deck.size()){fail();return;}if(t.now==null)main.postDelayed(()->load(t,true),500);return;}
                failures=0;
                if (gen != generation || !running) {
                    b.recycle();
                    return;
                }
                if (first || t.now == null) {
                    t.now = b;
                    t.wall = w;
                    t.shownAt = SystemClock.uptimeMillis();
                    if (t == tiles[0]) caption(w);
                } else {
                    t.next = b;
                    t.nextWall = w;
                    t.foldAt = SystemClock.uptimeMillis();
                }
                android.util.Log.i("TvHome.Aura","mosaic-photo-ready");invalidate();
            });
        });
    }

    private Bitmap decode(Context c,Photo photo,int w,int h) {
        try{return loader.load(c,photo.mine?photo.path:photo.file,!photo.mine,w,h);}
        catch(Exception|OutOfMemoryError e){return null;}
    }

    private void caption(Photo w) {
        if (listener == null || w == null) return;
        String by = w.author.isEmpty() ? w.license : w.author + " · " + w.license;
        listener.onStory(w.mine ? "Eigene Fotos" : w.region.replace(" · ", ", "), w.place.isEmpty() ? "Your photo" : w.place, "", by, 14_000);
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        float s = getHeight() / 720f;
        if (running && now >= nextTurn) {
            nextTurn = now + TURN_MS;
            Tile t = tiles[turn++ % tiles.length];
            if (t.foldAt < 0 && t.now != null) load(t, false);
        }
        boolean moving = false;
        for (Tile t : tiles) {
            if (t.foldAt >= 0) {
                float f = Math.min(1f, (now - t.foldAt) / (float) FOLD_MS);
                moving = true;
                // the old picture folds away around the tile's middle, the new one unfolds into place
                boolean firstHalf = f < 0.5f;
                float ang = firstHalf ? 90 * Ui.SOFT.getInterpolation(f * 2) : -90 * (1 - Ui.SOFT.getInterpolation((f - 0.5f) * 2));
                draw(c, t, firstHalf ? t.now : t.next, ang, now, s);
                if (f >= 1f) {
                    if (t.now != null) t.now.recycle();
                    t.now = t.next;
                    t.wall = t.nextWall;
                    t.next = null;
                    t.foldAt = -1;
                    t.shownAt = now;
                    if (t == tiles[0]) caption(t.wall);
                }
            } else if (t.now != null) {
                draw(c, t, t.now, 0, now, s);
            }
        }
        if (running) {
            if (moving) postInvalidateOnAnimation();
            else postInvalidateDelayed(66);
        }
    }

    /** One tile: the photograph covering it, drifting slowly, folded by {@code angle} around its middle. */
    private void draw(Canvas c, Tile t, Bitmap b, float angle, long now, float s) {
        if (b == null) return;
        RectF box = t.box;
        float age = (now - t.shownAt) / 1000f;
        float k = Math.max(box.width() / b.getWidth(), box.height() / b.getHeight()) * (1.06f + 0.04f * (float) Math.sin(age / 9));
        float dx = (box.width() - b.getWidth() * k) / 2f + (float) Math.sin(age / 13) * 10 * s;
        float dy = (box.height() - b.getHeight() * k) / 2f;
        t.m.setScale(k, k);
        t.m.postTranslate(box.left + dx, box.top + dy);
        BitmapShader sh = new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        sh.setLocalMatrix(t.m);
        pic.setShader(sh);
        c.save();
        if (angle != 0) {
            camera.save();
            camera.rotateX(angle);
            camera.getMatrix(fold);
            camera.restore();
            fold.preTranslate(-box.centerX(), -box.centerY());
            fold.postTranslate(box.centerX(), box.centerY());
            c.concat(fold);
        }
        float r = 10 * s;
        c.drawRoundRect(box, r, r, pic);
        if (angle != 0) {
            // the turning face darkens as it tilts away from the light
            shade.setColor(Ui.withAlpha(0xFF000000, Math.round(200 * Math.abs(angle) / 90f)));
            c.drawRoundRect(box, r, r, shade);
        }
        c.restore();
        if(angle==0 && t.wall!=null){
            Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);label.setTypeface(Ui.regular);label.setTextSize(12*s);label.setColor(0xFFFFFFFF);label.setShadowLayer(3*s,0,1*s,0xFF000000);
            String credit=t.wall.mine?"Eigenes Foto":t.wall.author+" · "+t.wall.license;
            credit=android.text.TextUtils.ellipsize(credit,new android.text.TextPaint(label),box.width()-24*s,android.text.TextUtils.TruncateAt.END).toString();
            c.drawText(credit,box.left+12*s,box.bottom-12*s,label);
        }
        pic.setShader(null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        layout(w, h);
    }

    @Override
    protected void onDetachedFromWindow() {
        disposed = true;
        stop();
        worker.shutdownNow();
        super.onDetachedFromWindow();
    }
}

