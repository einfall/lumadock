package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.function.Consumer;

final class DockFolderPresentation {
    private static final int ART_WIDTH = 320;
    private static final int ART_HEIGHT = 180;
    private static final int MAX_ICONS = 6;

    static Drawable banner(Context context, List<AppAdapter.Entry> apps) {
        Bitmap bitmap = Bitmap.createBitmap(ART_WIDTH, ART_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable glass = SurfaceThemes.background(context, "glass", false);
        glass.setBounds(0, 0, ART_WIDTH, ART_HEIGHT);
        glass.draw(canvas);

        int count = Math.min(apps.size(), MAX_ICONS);
        int columns = count <= 4 ? Math.min(count, 2) : 3;
        int rows = (count + columns - 1) / columns;
        int side = 64;
        int gap = 12;
        int top = (ART_HEIGHT - (rows * side + (rows - 1) * gap)) / 2;
        Paint plate = new Paint(Paint.ANTI_ALIAS_FLAG);
        plate.setColor(0x36202A3C);
        for (int row = 0; row < rows; row++) {
            int cells = Math.min(columns, count - row * columns);
            int left = (ART_WIDTH - (cells * side + (cells - 1) * gap)) / 2;
            for (int column = 0; column < cells; column++) {
                AppAdapter.Entry app = apps.get(row * columns + column);
                canvas.drawRoundRect(left, top, left + side, top + side, 13, 13, plate);
                Drawable icon = smallIcon(context, app);
                drawFit(canvas, icon, left + 7, top + 7, side - 14);
                left += side + gap;
            }
            top += side + gap;
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static Drawable smallIcon(Context context, AppAdapter.Entry app) {
        if (app.icon != null) return app.icon;
        if (app.banner instanceof IconPackArtwork) return app.banner;
        try {
            return context.getPackageManager().getApplicationIcon(app.pkg);
        } catch (PackageManager.NameNotFoundException missing) {
            return app.banner == null ? new AppGlyph("grid") : app.banner;
        }
    }

    private static void drawFit(Canvas canvas, Drawable icon, int left, int top, int side) {
        Rect oldBounds = new Rect(icon.getBounds());
        int width = icon.getIntrinsicWidth(), height = icon.getIntrinsicHeight();
        float scale = side / (float) Math.max(1, Math.max(width, height));
        int w = width > 0 ? Math.round(width * scale) : side;
        int h = height > 0 ? Math.round(height * scale) : side;
        int x = left + (side - w) / 2, y = top + (side - h) / 2;
        icon.setBounds(x, y, x + w, y + h);
        icon.draw(canvas);
        icon.setBounds(oldBounds);
    }

    static void show(Activity activity, AppAdapter.Entry folder, Consumer<AppAdapter.Entry> launch) {
        List<AppAdapter.Entry> apps = folder.folderApps;
        LinearLayout panel = GlassDialog.panel(activity);
        TextView title = GlassDialog.text(activity, folder.label, 24);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(title);
        int columns = Math.min(3, apps.size());
        int rows = (apps.size() + columns - 1) / columns;
        float density = activity.getResources().getDisplayMetrics().density;
        int screenWidth = Math.round(activity.getResources().getDisplayMetrics().widthPixels / density);
        int screenHeight = Math.round(activity.getResources().getDisplayMetrics().heightPixels / density);

        RecyclerView grid = new RecyclerView(activity);
        grid.setLayoutManager(new GridLayoutManager(activity, columns));
        grid.setItemAnimator(null);
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        int gridHeight = Math.min(rows * 112, Math.max(112, screenHeight - 132));
        LinearLayout.LayoutParams gridSize = new LinearLayout.LayoutParams(-1, dp(activity, gridHeight));
        gridSize.topMargin = dp(activity, 12);
        panel.addView(grid, gridSize);

        Dialog dialog = GlassDialog.show(activity, panel, Math.min(screenWidth - 32, 72 + 160 * columns), -2);
        SettingsSheet.track(activity, dialog);
        grid.setAdapter(new RecyclerView.Adapter<AppTile>() {
            @NonNull @Override public AppTile onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new AppTile(activity);
            }

            @Override public void onBindViewHolder(@NonNull AppTile tile, int position) {
                AppAdapter.Entry app = apps.get(position);
                tile.title.setText(app.label);
                Drawable art = app.banner == null ? smallIcon(activity, app) : app.banner;
                Drawable.ConstantState state = art.getConstantState();
                tile.image.setImageDrawable(state == null ? new AppGlyph("grid") : state.newDrawable(activity.getResources()));
                tile.itemView.setContentDescription(app.label + " öffnen");
                tile.itemView.setOnClickListener(view -> {
                    dialog.dismiss();
                    launch.accept(app);
                });
            }

            @Override public int getItemCount() { return apps.size(); }
        });
        grid.requestFocus();
        grid.post(() -> {
            RecyclerView.ViewHolder first = grid.findViewHolderForAdapterPosition(0);
            if (first != null) first.itemView.requestFocus();
        });
    }

    private static final class AppTile extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageView image;

        AppTile(Activity activity) {
            super(new LinearLayout(activity));
            LinearLayout card = (LinearLayout) itemView;
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setFocusableInTouchMode(true);
            card.setBackground(SettingsSheet.rowBackground(activity));
            card.setPadding(dp(activity, 8), dp(activity, 6), dp(activity, 8), dp(activity, 6));
            RecyclerView.LayoutParams size = new RecyclerView.LayoutParams(-1, dp(activity, 104));
            size.setMargins(dp(activity, 4), 0, dp(activity, 4), dp(activity, 8));
            card.setLayoutParams(size);

            image = new ImageView(activity);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
            title = GlassDialog.text(activity, "", 12);
            title.setGravity(Gravity.CENTER);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setDuplicateParentStateEnabled(true);
            title.setTextColor(SettingsSheet.ink(Color.WHITE, 0xFF17202B));
            card.addView(title, new LinearLayout.LayoutParams(-1, dp(activity, 24)));
        }
    }

    private static int dp(Activity activity, int value) { return GlassDialog.dp(activity, value); }
}
