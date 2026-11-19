package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** TV search with image results, persistent list position and two-second OK favorites. */
final class YouTubeBrowser {
    private final Activity activity;
    private final Runnable changed;
    private final EditText query;
    private final TextView status, count, more, credential;
    private final RecyclerView grid;
    private final Videos adapter = new Videos();
    private final List<YouTubeVideo> videos = new ArrayList<>();
    private final ExecutorService worker = Executors.newFixedThreadPool(3);
    private final ExecutorService requests = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> thumbnails = new LruCache<String, Bitmap>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private final SharedPreferences prefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (p, key) -> preferencesChanged(key);
    private void preferencesChanged(String key) {
        if (!closed && (LiveWallpaperSelection.FAVORITES.equals(key) || YouTubeFavorites.KEY.equals(key))) {
            adapter.notifyItemRangeChanged(0, videos.size(), "marks");
        }
    }
    private boolean closed, loading;
    private int generation, cellHeight;
    private String nextPage = "", search = "";

    static void show(Activity activity, Runnable changed) { DesignStudio.background(activity, "YouTube"); }

    static YouTubeBrowser embed(Activity activity, Runnable changed, LinearLayout content) {
        return new YouTubeBrowser(activity, changed, content);
    }

    private YouTubeBrowser(Activity activity, Runnable changed, LinearLayout content) {
        this.activity = activity;
        this.changed = changed;
        prefs = LiveWallpaperPrefs.prefs(activity);
        LinearLayout searchBar = new LinearLayout(activity);
        query = new EditText(activity);
        query.setTextColor(0xFFFFFFFF);
        query.setTextSize(12);
        query.setSingleLine(true);
        query.setHint("YouTube suchen oder Video-Link einfügen");
        query.setHintTextColor(0xFFADB7C4);
        query.setInputType(InputType.TYPE_CLASS_TEXT);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});
        query.setBackgroundResource(R.drawable.weather_field);
        searchBar.addView(query, new LinearLayout.LayoutParams(0, dp(36), 1));
        TextView find = DesignPopup.button(activity, "Suchen");
        find.setTextSize(11);
        LinearLayout.LayoutParams findParams = new LinearLayout.LayoutParams(dp(67), dp(36));
        findParams.setMarginStart(dp(5));
        searchBar.addView(find, findParams);
        content.addView(searchBar, new LinearLayout.LayoutParams(-1, -2));
        find.setOnClickListener(v -> search(false));
        query.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            search(false);
            return true;
        });
        LinearLayout meta = new LinearLayout(activity);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        credential = DesignPopup.button(activity, YouTubeApi.key(activity).isEmpty() ? "Ohne API-Key" : "API-Schlüssel ✓");
        credential.setTextSize(10);
        credential.setContentDescription("Optionale YouTube-Sucheinstellungen");
        credential.setOnClickListener(v -> credentials());
        meta.addView(credential, new LinearLayout.LayoutParams(dp(94), dp(29)));
        count = GlassDialog.text(activity, "0 / 0", 10);
        count.setGravity(Gravity.END);
        meta.addView(count, new LinearLayout.LayoutParams(0, dp(29), 1));
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(-1, -2);
        metaParams.topMargin = dp(4);
        content.addView(meta, metaParams);
        status = GlassDialog.text(activity, "YouTube suchen oder Link einfügen. Lange OK speichert in den gemeinsamen Favoriten.", 10);
        status.setMaxLines(2);
        status.setTextColor(0xFFB8C5D4);
        content.addView(status, new LinearLayout.LayoutParams(-1, dp(30)));
        grid = new RecyclerView(activity);
        grid.setLayoutManager(new GridLayoutManager(activity, 3));
        grid.setItemAnimator(null);
        grid.setPadding(dp(2), dp(2), dp(2), dp(2));
        grid.setClipToPadding(true);
        grid.setAdapter(adapter);
        cellHeight = dp(90);
        grid.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int height = Math.max(dp(58), (b - t - grid.getPaddingTop() - grid.getPaddingBottom()) / 3 - dp(4));
            if (height == cellHeight) return;
            cellHeight = height;
            grid.post(() -> { if (!closed) adapter.notifyItemRangeChanged(0, videos.size(), "size"); });
        });
        content.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        more = DesignPopup.button(activity, "Mehr Ergebnisse laden");
        more.setTextSize(11);
        more.setVisibility(View.GONE);
        more.setOnClickListener(v -> search(true));
        content.addView(more, new LinearLayout.LayoutParams(-1, dp(30)));
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    void close() {
        if (closed) return;
        closed = true;
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
        worker.shutdownNow();
        requests.shutdownNow();
        main.removeCallbacksAndMessages(null);
        thumbnails.evictAll();
    }

    private int dp(int value) { return SettingsSheet.dp(activity, value); }

    private void search(boolean append) {
        if (closed || loading) return;
        String text = append ? search : query.getText().toString().trim();
        if (text.isEmpty()) { query.requestFocus(); return; }
        if (append && (nextPage.isEmpty() || videos.size() >= 200)) return;
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(query.getWindowToken(), 0);
        search = text;
        String id = YouTubeVideo.id(text), token = append ? nextPage : "";
        int request = ++generation;
        loading = true;
        status.setText("YouTube wird durchsucht …");
        more.setEnabled(false);
        requests.execute(() -> {
            YouTubeApi.Page result = new YouTubeApi.Page();
            String failure = null;
            try {
                if (!id.isEmpty()) result.videos.add(YouTubeApi.lookup(activity, id));
                else result = YouTubeApi.search(activity, text, token);
            } catch (Exception error) {
                String message = error.getMessage();
                failure = message != null && (message.startsWith("YouTube") || message.startsWith("Für ") || message.startsWith("Dieses "))
                        ? message : "YouTube ist gerade nicht erreichbar. Internetverbindung prüfen.";
            }
            YouTubeApi.Page page = result;
            String problem = failure;
            main.post(() -> {
                if (closed || request != generation) return;
                loading = false;
                more.setEnabled(true);
                if (problem != null) { status.setText(problem); return; }
                int before = append ? videos.size() : 0;
                if (!append) videos.clear();
                java.util.Set<String> seen = new java.util.HashSet<>();
                for (YouTubeVideo item : videos) seen.add(item.id);
                for (YouTubeVideo item : page.videos) if (seen.add(item.id)) videos.add(item);
                nextPage = page.next;
                more.setVisibility(!nextPage.isEmpty() && videos.size() < 200 ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
                status.setText(videos.isEmpty() ? "Keine passenden YouTube-Videos gefunden." : "YouTube · OK: Vorschau · lange OK: Favorit ★");
                updateCount(before);
                if (!videos.isEmpty()) focus(Math.min(before, videos.size() - 1));
            });
        });
    }

    private void updateCount(int at) {
        count.setText(videos.isEmpty() ? "0 / 0" : Math.min(at + 1, videos.size()) + " / " + videos.size());
    }
    private void focus(int at) {
        if (!grid.isShown()) return;
        grid.scrollToPosition(at);
        grid.post(() -> {
            RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(at);
            if (holder != null) holder.itemView.requestFocus();
        });
    }

    private void credentials() {
        LinearLayout panel = GlassDialog.panel(activity);
        panel.addView(GlassDialog.text(activity, "YouTube · Sucheinstellungen", 18));
        TextView hint = GlassDialog.text(activity, "Die öffentliche YouTube-Suche funktioniert ohne API-Schlüssel. Optional kannst du einen eigenen Schlüssel für die YouTube Data API v3 verwenden. Er bleibt auf diesem Gerät. Leer speichern aktiviert die öffentliche Suche.", 12);
        hint.setPadding(0, dp(10), 0, dp(10));
        panel.addView(hint);
        EditText key = new EditText(activity);
        key.setTextColor(0xFFFFFFFF);
        key.setTextSize(12);
        key.setSingleLine(true);
        key.setSelectAllOnFocus(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(256)});
        key.setText(YouTubeApi.key(activity));
        panel.addView(key, new LinearLayout.LayoutParams(-1, dp(40)));
        LinearLayout actions = new LinearLayout(activity);
        TextView save = DesignPopup.button(activity, "Speichern"), cancel = DesignPopup.button(activity, "Abbrechen");
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(36), 1));
        actions.addView(cancel, new LinearLayout.LayoutParams(0, dp(36), 1));
        panel.addView(actions);
        android.app.Dialog dialog = GlassDialog.show(activity, panel, 420, -2);
        SettingsSheet.track(activity, dialog);
        save.setOnClickListener(v -> {
            YouTubeApi.key(activity, key.getText().toString());
            credential.setText(YouTubeApi.key(activity).isEmpty() ? "Ohne API-Key" : "API-Schlüssel ✓");
            dialog.dismiss();
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        save.requestFocus();
    }

    static void favorite(Activity activity, YouTubeVideo video) {
        boolean existed = YouTubeFavorites.contains(activity, video.id);
        boolean added = YouTubeFavorites.toggle(activity, video);
        String message = added ? "Zu Live-Hintergrund-Favoriten hinzugefügt" : existed ? "Aus Live-Hintergrund-Favoriten entfernt"
                : "Die Favoritenliste enthält bereits 200 YouTube-Videos.";
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    private final class Holder extends RecyclerView.ViewHolder {
        final LinearLayout box;
        final ImageView image;
        final TextView title, marks;
        final FavoriteOkKey ok;
        YouTubeVideo video;
        Holder(LinearLayout box, ImageView image, TextView title, TextView marks) {
            super(box);
            this.box = box;
            this.image = image;
            this.title = title;
            this.marks = marks;
            ok = new FavoriteOkKey(box, () -> video != null, () -> favorite(activity, video), null);
            box.setOnFocusChangeListener((v, on) -> {
                if (on && getBindingAdapterPosition() != RecyclerView.NO_POSITION) updateCount(getBindingAdapterPosition());
                if (!on) ok.cancel();
            });
            box.setOnClickListener(v -> { if (video != null) YouTubePreview.show(activity, video, changed); });
        }
    }

    private final class Videos extends RecyclerView.Adapter<Holder> {
        @Override public int getItemCount() { return videos.size(); }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            LinearLayout box = new LinearLayout(activity);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(5), dp(5), dp(5), dp(5));
            box.setFocusable(true);
            box.setFocusableInTouchMode(true);
            box.setClickable(true);
            box.setBackground(SettingsSheet.rowBackground(activity));
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, cellHeight);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            box.setLayoutParams(params);
            FrameLayout media = new FrameLayout(activity);
            ImageView image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(0xFF10141B);
            media.addView(image, new FrameLayout.LayoutParams(-1, -1));
            TextView marks = GlassDialog.text(activity, "", 13);
            marks.setTextColor(0xFFFFD88A);
            marks.setBackgroundColor(0xA010141B);
            marks.setPadding(dp(3), 0, dp(3), 0);
            media.addView(marks, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT));
            box.addView(media, new LinearLayout.LayoutParams(-1, 0, 1));
            TextView title = GlassDialog.text(activity, "", 10);
            title.setDuplicateParentStateEnabled(true);
            title.setTextColor(SettingsSheet.ink(0xFFFFFFFF, 0xFF15202E));
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setGravity(Gravity.CENTER);
            box.addView(title, new LinearLayout.LayoutParams(-1, dp(19)));
            return new Holder(box, image, title, marks);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int at) {
            holder.ok.cancel();
            YouTubeVideo video = videos.get(at);
            holder.video = video;
            holder.box.setTag(video.id);
            holder.title.setText(video.title);
            holder.image.setImageDrawable(null);
            holder.box.getLayoutParams().height = cellHeight;
            mark(holder);
            Bitmap cached = thumbnails.get(video.id);
            if (cached != null) holder.image.setImageBitmap(cached);
            else thumbnail(holder, video);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int at, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) onBindViewHolder(holder, at);
            else {
                holder.box.getLayoutParams().height = cellHeight;
                holder.box.requestLayout();
                mark(holder);
            }
        }
        private void mark(Holder holder) {
            if (holder.video == null) return;
            boolean favorite = YouTubeFavorites.contains(activity, holder.video.id);
            holder.marks.setText(favorite ? "★ YT" : "YT");
            holder.box.setContentDescription(holder.video.title + " · YouTube" + (favorite ? " · Favorit" : ""));
        }
    }

    private void thumbnail(Holder holder, YouTubeVideo video) {
        if (closed) return;
        worker.execute(() -> {
            Bitmap result = fetchThumbnail(video.id);
            main.post(() -> {
                if (closed || result == null) return;
                thumbnails.put(video.id, result);
                if (video.id.equals(holder.box.getTag())) holder.image.setImageBitmap(result);
            });
        });
    }

    static Bitmap fetchThumbnail(String id) {
        if (!YouTubeVideo.validId(id)) return null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(new YouTubeVideo(id, "", "").thumbnail()).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setInstanceFollowRedirects(false);
            if (connection.getResponseCode() != 200) return null;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] data = new byte[8192];
                int size;
                while ((size = input.read(data)) != -1) {
                    if (output.size() + size > 256 * 1024) return null;
                    output.write(data, 0, size);
                }
                byte[] raw = output.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeByteArray(raw, 0, raw.length, options);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
