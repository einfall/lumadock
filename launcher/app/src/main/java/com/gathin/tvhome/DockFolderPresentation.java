package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.function.Consumer;

final class DockFolderPresentation {
    private static final int ART_WIDTH = 320;
    private static final int ART_HEIGHT = 180;
    private static final int MAX_ICONS = 6;
    private static final double MIN_LABEL_LUMINANCE = 4.5 * (ContrastColor.luminance(0xFF263143) + .05) - .05;

    static Drawable banner(Context context, Config config, List<AppAdapter.Entry> apps) {
        Bitmap bitmap = Bitmap.createBitmap(ART_WIDTH, ART_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        DockSurface material = new DockSurface(1f, config.cinematicHome, config.dockOpacity, config.dockGlass);
        material.setStyle(DockStyle.load(context, config));
        material.draw(canvas, new RectF(0, 0, ART_WIDTH, ART_HEIGHT), 0, false);

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
        if (app.banner instanceof IconPackArtwork) return app.banner;
        if (app.icon != null) return app.icon;
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


    private final Config config;
    private final Consumer<AppAdapter.Entry> launch;
    private final RecyclerView grid;
    private final AppAdapter adapter;
    private final TileEffectsDecoration effects;
    private final GlassView glass;
    private final SpringAnimation springX, springY;
    private final FocusedAppName caption;
    private final TextView topName;
    private final FrameLayout stage;
    private final Dialog dialog;
    private final MainActivity home;
    private final View anchor;
    private final LinearLayout panel;
    private final boolean inline, pivot, automatic;
    private final int slots, panelWidth, panelHeight;
    private final ViewTreeObserver.OnPreDrawListener trackAnchor;
    private final Rect rect = new Rect();
    private final Rect artwork = new Rect();
    private final float density, focusEdge, focusScale;
    private final String nameMode, frameMode, nameTransition;
    private final boolean dominantNames, animate;
    private final int tileWidth, tilePadding, rowHeight, labelHeight;
    private View focusedCard;
    private int focusedPosition = RecyclerView.NO_POSITION;
    private int pivotFirst;
    private boolean disposed;
    private boolean entering;

    static void show(MainActivity activity, AppAdapter.Entry folder, View anchor, Consumer<AppAdapter.Entry> launch) {
        new DockFolderPresentation(activity, folder, anchor, launch, false);
    }

    static DockFolderPresentation list(MainActivity activity, AppAdapter.Entry folder, View anchor,
                                       Consumer<AppAdapter.Entry> launch) {
        return new DockFolderPresentation(activity, folder, anchor, launch, true);
    }

    private DockFolderPresentation(MainActivity activity, AppAdapter.Entry folder, View anchor,
                                   Consumer<AppAdapter.Entry> launch, boolean inline) {
        home = activity;
        this.anchor = anchor;
        this.inline = inline;
        pivot = inline && folder.folderPivot;
        automatic = inline && "list_focus".equals(folder.folderOpening);
        this.launch = launch;
        config = Config.load(activity);
        SharedPreferences prefs = activity.getSharedPreferences(Config.SETTINGS_NAME, 0);
        density = activity.getResources().getDisplayMetrics().density;
        String names = DockFolders.nameMode(folder.folderNames);
        nameMode = inline ? "none" : !"dock".equals(names) ? names : AppTitleStyle.below(activity)
                ? "focused" : config.alwaysShowNames ? "all" : "top";
        dominantNames = prefs.getBoolean(AppTitleStyle.DOMINANT, false);
        nameTransition = AppTitlePolicy.transition(prefs.getString(AppTitleStyle.TRANSITION, null));
        frameMode = FocusStyle.frameMode(activity);
        focusEdge = FocusedAppName.frameEdge(activity);
        animate = config.animations && !config.reducedMotion;

        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        Rect space = home.folderArea(anchor);
        boolean headerName = "top".equals(nameMode);
        int headerHeight = inline ? 0 : (headerName ? dp(42) : dp(26));
        int horizontalPadding = dp(inline ? 14 : 24);
        int verticalPadding = dp(inline ? 3 : 12);
        int maxGrid = Math.max(1, space.height() - headerHeight - 2 * verticalPadding);
        int available = Math.max(dp(108), Math.min(dp(800), screenWidth - dp(44)) - 2 * horizontalPadding);
        int configured = prefs.getInt("dock_columns", config.columns);
        if (configured < 4 || configured > 7 || !config.dockRow) configured = config.columns;
        int count = folder.folderApps.size();
        int columns = inline ? 1 : Math.max(1, Math.min(count, Math.min(configured, available / dp(118))));
        focusScale = FocusStyle.motion(activity, config).scale(config.animations, config.advancedEffects);
        float textSize = 14f * AppTitlePolicy.labels(prefs.getInt(AppTitleStyle.LABELS, 100)) / 100f;
        Paint font = new Paint();
        font.setTextSize(textSize * activity.getResources().getDisplayMetrics().scaledDensity);
        font.setTypeface(Typography.face(activity, Typography.selected(activity, Typography.APPS), R.string.font_medium));
        labelHeight = (int) Math.ceil(Math.max(dp(22), font.getFontMetrics().bottom - font.getFontMetrics().top + dp(2)));
        int labelRoom = inline || headerName ? 0 : labelHeight + dp(4);
        int preferredSlots = inline ? Math.min(count, pivot ? 5 : 4) : 1;
        int innerWidth = Math.min(available, dp(182) * columns);
        int maxWidth = (int) ((innerWidth / columns - 2 * (focusEdge + dp(5))) / focusScale);
        int heightForArt = (int) ((maxGrid / preferredSlots - labelRoom - 2 * (focusEdge + tileGap()) - 2) / focusScale);
        tileWidth = inline ? anchor.getWidth() : Math.max(1, Math.min(dp(AppAdapter.DESIGN_W_DP), Math.min(maxWidth,
                (int) (heightForArt * AppAdapter.DESIGN_W_DP / AppAdapter.DESIGN_H_DP))));
        int tileHeight = AppAdapter.tileHeightPx(tileWidth);
        int frameOverhang = (int) Math.ceil((focusScale - 1f) * tileHeight / 2 + focusEdge);
        // Only one card is focused. Adjacent cards need one shared gap for its zoom and frame.
        tilePadding = inline ? (frameOverhang + dp(1) + 1) / 2 : labelGap(tileHeight);
        int frameRoom = inline ? Math.max(0, frameOverhang - tilePadding + dp(1)) : 0;
        rowHeight = tileHeight + 2 * tilePadding + labelRoom;
        if (inline) innerWidth = (int) Math.ceil(tileWidth * focusScale + 2 * (focusEdge + dp(5)));
        int rows = (count + columns - 1) / columns;
        slots = Math.min(inline ? preferredSlots : rows, Math.max(1, (maxGrid - 2 * frameRoom) / rowHeight));
        panelWidth = innerWidth + 2 * horizontalPadding;
        int stageHeight = slots * rowHeight + 2 * frameRoom;
        panelHeight = headerHeight + 2 * verticalPadding + stageHeight;

        panel = GlassDialog.panel(activity);
        DockSurface material = new DockSurface(density, config.cinematicHome, config.dockOpacity, config.dockGlass);
        material.setStyle(DockStyle.load(activity, config));
        panel.setBackground(material.background());
        panel.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        if (headerName) {
            LinearLayout heading = new LinearLayout(activity);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = title(activity, folder.label, 18);
            title.setMaxWidth(Math.round(innerWidth * .36f));
            heading.addView(title, new LinearLayout.LayoutParams(-2, -1));
            topName = title(activity, folder.folderApps.get(0).label,
                    Math.min(33, AppTitlePolicy.size(prefs.getInt(AppTitleStyle.SIZE, 48))));
            topName.setGravity(Gravity.CENTER);
            Typography.apply(topName, Typography.APPS, R.string.font_light);
            LinearLayout.LayoutParams selected = new LinearLayout.LayoutParams(0, -1, 1f);
            selected.setMarginStart(dp(16));
            heading.addView(topName, selected);
            panel.addView(heading, new LinearLayout.LayoutParams(-1, headerHeight));
        } else {
            topName = null;
            if (!inline) panel.addView(title(activity, folder.label, 18), new LinearLayout.LayoutParams(-1, headerHeight));
        }

        stage = new FrameLayout(activity);
        panel.addView(stage, new LinearLayout.LayoutParams(-1, stageHeight));

        grid = new RecyclerView(activity);
        grid.setLayoutManager(new GridLayoutManager(activity, columns, RecyclerView.VERTICAL, inline));
        if (inline) {
            grid.setPadding(0, frameRoom, 0, frameRoom);
            grid.setClipToPadding(false);
        }
        grid.setItemAnimator(null);
        grid.setClipChildren(false);
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        stage.addView(grid, new FrameLayout.LayoutParams(-1, -1));

        glass = new GlassView(activity, null);
        glass.setStyle(config.glassStyle);
        FocusStyle.apply(glass, activity);
        glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        glass.setElevation(dp(2));
        glass.setVisibility(View.INVISIBLE);
        stage.addView(glass, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));
        springX = spring(DynamicAnimation.TRANSLATION_X);
        springY = spring(DynamicAnimation.TRANSLATION_Y);

        adapter = new AppAdapter(activity, columns, config, this, folder.folderApps);
        effects = new TileEffectsDecoration(grid, adapter);
        grid.addItemDecoration(effects);
        grid.setAdapter(adapter);
        caption = "focused".equals(nameMode) ? new FocusedAppName(activity, stage, grid, true) : null;

        grid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                if (dx != 0 || dy != 0) placeFrame(false);
            }
        });
        stage.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (l != ol || t != ot || r != or || b != ob) placeFrame(false);
        });
        if (inline) {
            dialog = null;
            trackAnchor = () -> {
                placeList();
                return true;
            };
            previewList();
            FrameLayout root = home.folderRoot();
            float highestLayer = 0f;
            for (int i = 0; i < root.getChildCount(); i++) {
                highestLayer = Math.max(highestLayer, root.getChildAt(i).getZ());
            }
            panel.setTranslationZ(highestLayer + dp(1));
            root.addView(panel, new FrameLayout.LayoutParams(panelWidth, panelHeight));
            placeList();
            root.getViewTreeObserver().addOnPreDrawListener(trackAnchor);
        } else {
            trackAnchor = null;
            dialog = GlassDialog.show(activity, panel, Math.round(panelWidth / density), -2);
            Window window = dialog.getWindow();
            if (window != null) {
                int[] origin = new int[2];
                home.folderRoot().getLocationOnScreen(origin);
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                window.getDecorView().setSystemUiVisibility(activity.getWindow().getDecorView().getSystemUiVisibility());
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                attributes.y = origin[1] + space.top + Math.max(0, (space.height() - panelHeight) / 2);
                attributes.width = panelWidth;
                attributes.height = panelHeight;
                attributes.dimAmount = .54f;
                window.setAttributes(attributes);
            }
            SettingsSheet.track(activity, dialog);
            dialog.setOnDismissListener(d -> dispose());
            focusFirst();
        }
    }

    private void focusFirst() {
        grid.post(() -> {
            if (disposed || (inline && !entering)) return;
            RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(0);
            if (holder == null) {
                grid.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        grid.getViewTreeObserver().removeOnPreDrawListener(this);
                        focusFirst();
                        return true;
                    }
                });
                return;
            }
            View card = holder.itemView.findViewById(R.id.card);
            if (card != null && card.requestFocus()) focused(0, card);
        });
    }

    private void placeList() {
        if (!anchor.isAttachedToWindow()) return;
        FrameLayout root = home.folderRoot();
        rect.set(0, 0, anchor.getWidth(), anchor.getHeight());
        root.offsetDescendantRectToMyCoords(anchor, rect);
        Rect area = home.folderArea(anchor);
        int x = Math.max(dp(12), Math.min(rect.centerX() - panelWidth / 2, root.getWidth() - panelWidth - dp(12)));
        int y = Math.max(area.top, area.bottom <= rect.top ? area.bottom - panelHeight : area.top);
        boolean fits = panelHeight <= area.height();
        panel.setTranslationX(x);
        panel.setTranslationY(y);
        panel.setVisibility(fits ? View.VISIBLE : View.INVISIBLE);
        rect.set(x, y, x + panelWidth, y + panelHeight);
        home.avoidFolderTitleOverlap(fits ? rect : null);
    }

    View anchor() { return anchor; }
    boolean hasAnchor(View view) { return view == anchor; }
    boolean automatic() { return automatic; }

    boolean ownsFocus(View view) {
        if (view == anchor) return true;
        for (ViewParent parent = view.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == panel) return true;
        }
        return false;
    }

    void previewList() {
        entering = false;
        boolean scrolled = pivotFirst != 0 || focusedPosition > 0;
        grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        grid.setFocusable(false);
        focusedCard = null;
        focusedPosition = RecyclerView.NO_POSITION;
        glass.setVisibility(View.INVISIBLE);
        glass.stopShine();
        effects.focusChanged(RecyclerView.NO_POSITION);
        if (scrolled) {
            pivotFirst = 0;
            grid.stopScroll();
            ((GridLayoutManager) grid.getLayoutManager()).scrollToPositionWithOffset(0, 0);
        }
    }

    void enterList() {
        if (entering || grid.hasFocus() || panel.getVisibility() != View.VISIBLE) return;
        entering = true;
        grid.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        focusFirst();
    }

    boolean handleKey(KeyEvent event) {
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (!grid.hasFocus()) {
            if (automatic && anchor.isFocused() && code == KeyEvent.KEYCODE_DPAD_UP
                    && panel.getVisibility() == View.VISIBLE) {
                if (down && event.getRepeatCount() == 0) enterList();
                return true;
            }
            return false;
        }
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            if (!down && !event.isCanceled()) home.leaveFolderList();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_DOWN && focusedPosition == 0) {
            if (down) home.leaveFolderList();
            return true;
        }
        return code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || (code == KeyEvent.KEYCODE_DPAD_UP && focusedPosition == adapter.appCount() - 1);
    }

    void dispose() {
        if (disposed) return;
        disposed = true;
        effects.dispose();
        springX.cancel();
        springY.cancel();
        glass.stopShine();
        if (topName != null) topName.animate().cancel();
        if (caption != null) caption.close();
        if (inline) {
            FrameLayout root = home.folderRoot();
            root.getViewTreeObserver().removeOnPreDrawListener(trackAnchor);
            root.removeView(panel);
            home.avoidFolderTitleOverlap(null);
        }
    }

    private TextView title(Activity activity, String text, int size) {
        TextView title = GlassDialog.text(activity, text, size);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setShadowLayer(dp(2), 0f, dp(1), 0xD9000000);
        return title;
    }

    private SpringAnimation spring(androidx.dynamicanimation.animation.FloatPropertyCompat<View> property) {
        boolean bounce = FocusStyle.motion(grid.getContext(), config).useBounce(config.reducedMotion);
        float stiffness = bounce ? (config.advancedEffects ? 320f : 260f)
                : (config.advancedEffects ? 520f : 440f);
        float damping = bounce ? (config.advancedEffects ? .76f : .58f) : 1f;
        return new SpringAnimation(glass, property).setSpring(new SpringForce()
                .setStiffness(stiffness).setDampingRatio(damping));
    }

    void focused(int position, View card) {
        boolean changed = focusedPosition != position;
        focusedPosition = position;
        focusedCard = card;
        effects.focusChanged(position);
        if (pivot && changed) {
            int first = DockBrowsePolicy.first("pivot", position, adapter.appCount(), slots, (slots + 1) / 2);
            first = Math.min(first, adapter.appCount() - slots);
            pivotFirst = first;
            View row = (View) card.getParent();
            int targetTop = grid.getHeight() - grid.getPaddingBottom() - (position - first + 1) * rowHeight;
            int delta = row.getTop() - targetTop;
            if (delta != 0) {
                grid.stopScroll();
                if (animate && Math.abs(delta) <= rowHeight * 2) {
                    grid.smoothScrollBy(0, delta, new android.view.animation.DecelerateInterpolator(), 180);
                } else {
                    ((GridLayoutManager) grid.getLayoutManager()).scrollToPositionWithOffset(first, 0);
                }
            }
        }
        if (topName != null) {
            String label = adapter.labelAt(position);
            if (animate && !label.contentEquals(topName.getText()) && !"none".equals(nameTransition)) {
                topName.animate().cancel();
                topName.setAlpha(0f);
                topName.setTranslationX("slide".equals(nameTransition) ? dp(10) : 0f);
                topName.animate().alpha(1f).translationX(0f).setDuration(180).start();
            }
            topName.setText(label);
            topName.setTextColor(nameColor(position));
        }
        String color = FocusStyle.color(grid.getContext());
        if ("original".equals(color)) {
            boolean festive = config.festival.active(java.time.LocalDate.now());
            glass.setAccent(festive ? config.festival.accentColor()
                    : config.appGlow || (config.cinematicHome && !config.cinematicWhiteFocus)
                    ? adapter.ambientColorAt(position) : 0);
        } else {
            glass.setFocusColor("dominant".equals(color) ? adapter.iconDominantAt(position) : FocusStyle.fixed(color));
        }
        glass.focusChanged(adapter.keyAt(position));
        stage.post(() -> {
            if (focusedCard == card && card.isFocused() && stage.isAttachedToWindow()) placeFrame(changed);
        });
    }

    private void placeFrame(boolean changed) {
        View card = focusedCard;
        if (card == null || !card.isFocused() || card.getWidth() == 0 || !adapter.focusOverlay()) return;
        rect.set(0, 0, card.getWidth(), card.getHeight());
        stage.offsetDescendantRectToMyCoords(card, rect);
        AppAdapter.artBounds(card, artwork);
        float scale = adapter.focusScale();
        float inset = glass.padding() + glass.outset();
        int width = Math.round(artwork.width() * scale + 2 * inset);
        int height = Math.round(artwork.height() * scale + 2 * inset);
        if (glass.getWidth() != width || glass.getHeight() != height) {
            glass.setLayoutParams(new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.LEFT));
            glass.setRadius(AppAdapter.cornerRadius(artwork.width(), density) * scale + glass.outset());
        }
        float x = rect.left + artwork.exactCenterX() - width / 2f;
        float y = rect.top + artwork.exactCenterY() - height / 2f;
        boolean visible = glass.getVisibility() == View.VISIBLE;
        if (animate && changed && visible && "moving".equals(frameMode)) {
            springX.animateToFinalPosition(x);
            springY.animateToFinalPosition(y);
        } else {
            springX.cancel();
            springY.cancel();
            glass.setTranslationX(x);
            glass.setTranslationY(y);
        }
        if (!visible) glass.setVisibility(View.VISIBLE);
        glass.invalidate();
        if (animate && changed && "fade".equals(frameMode)) {
            glass.animate().cancel();
            glass.setAlpha(0f);
            glass.animate().alpha(1f).setDuration(180).start();
        } else if (!visible) {
            glass.setAlpha(1f);
        }
    }

    boolean isInline() { return inline; }
    boolean allNames() { return "all".equals(nameMode); }
    int nameColor(int position) { return dominantNames ? readableNameColor(adapter.iconDominantAt(position)) : 0xF2FFFFFF; }

    static int readableNameColor(int color) {
        if (ContrastColor.luminance(color) >= MIN_LABEL_LUMINANCE) return color;
        for (int step = 1; step <= 10; step++) {
            int lighter = DockStyle.mix(color, 0xFFFFFFFF, step / 10f);
            if (ContrastColor.luminance(lighter) >= MIN_LABEL_LUMINANCE) return lighter;
        }
        return 0xFFFFFFFF;
    }
    int tileWidth() { return tileWidth; }
    int tilePadding() { return tilePadding; }
    int rowHeight() { return rowHeight; }
    int labelHeight() { return labelHeight; }

    int labelGap(int tileHeight) {
        return (int) Math.ceil((focusScale - 1f) * tileHeight / 2 + focusEdge + tileGap());
    }

    private int tileGap() { return dp(inline ? 4 : 6); }

    void open(AppAdapter.Entry entry) {
        if (dialog != null) dialog.dismiss();
        else home.leaveFolderList();
        launch.accept(entry);
    }

    private int dp(float value) { return Math.round(value * density); }
}
