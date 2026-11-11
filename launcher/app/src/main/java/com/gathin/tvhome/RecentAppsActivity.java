package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A task list with one on-demand reading and explicit close actions. */
public final class RecentAppsActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final int user = Process.myUid() / 100_000;
    private final List<String> displayed = new ArrayList<>();
    private final Map<String, TextView> metricLabels = new HashMap<>();
    private LinearLayout row;
    private TextView status, all, done;
    private boolean busy;
    private int generation;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.dimAmount = .8f;
        window.setAttributes(attrs);
        window.getDecorView().setSystemUiVisibility(5894);

        FrameLayout root = new FrameLayout(this);
        LinearLayout panel = GlassDialog.panel(this);
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xF03E4857, 0xF61C2531, 0xF6101721});
        glass.setCornerRadius(dp(24));
        glass.setStroke(dp(1), 0x99CADBF2);
        panel.setBackground(glass);
        panel.setElevation(dp(18));
        panel.setPadding(dp(26), dp(22), dp(26), dp(22));
        panel.addView(GlassDialog.text(this, "Geöffnete Apps", 26));
        status = GlassDialog.text(this, "Apps werden geladen …", 12);
        status.setTextColor(0xFFBDCADB);
        panel.addView(status);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), dp(16), dp(5), dp(12));
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -1));
        LinearLayout.LayoutParams scrollSize = new LinearLayout.LayoutParams(-1, dp(255));
        scrollSize.topMargin = dp(8);
        panel.addView(scroll, scrollSize);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        all = button("Alle schließen");
        all.setOnClickListener(v -> closeApps(new ArrayList<>(displayed)));
        actions.addView(all, new LinearLayout.LayoutParams(dp(190), dp(42)));
        done = button("Zurück");
        done.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams doneSize = new LinearLayout.LayoutParams(dp(130), dp(42));
        doneSize.setMarginStart(dp(12));
        actions.addView(done, doneSize);
        panel.addView(actions);
        TextView help = GlassDialog.text(this, "CPU: 100 % je Kern · RAM: anteilig (PSS) · Speicher: letzte Android-Erfassung", 11);
        help.setPadding(0, dp(7), 0, 0);
        panel.addView(help);

        root.addView(panel, new FrameLayout.LayoutParams(-1, -2));
        setContentView(root);
        window.setLayout(Math.min(dp(850), getResources().getDisplayMetrics().widthPixels - dp(48)), -2);
        window.setGravity(Gravity.CENTER);
        RecentAppsAppearance.apply(this);
        NavigationSounds.attach(this, window);
        refresh();
    }

    private int dp(float value) { return GlassDialog.dp(this, value); }

    private TextView button(String title) {
        TextView view = DesignPopup.button(this, title);
        view.setTextSize(15);
        view.setBackground(RecentAppsAppearance.control(this, 0xFF7EBCFF));
        view.setTextColor(Color.WHITE);
        return view;
    }

    private Set<String> protectedPackages() {
        Set<String> packages = new HashSet<>();
        packages.add(getPackageName());
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo info : getPackageManager().queryIntentActivities(home, 0)) {
            packages.add(info.activityInfo.packageName);
        }
        String bound = Settings.Secure.getString(getContentResolver(), "enabled_accessibility_services");
        if (bound != null) {
            for (String value : bound.split(":")) {
                ComponentName component = ComponentName.unflattenFromString(value);
                if (component != null) packages.add(component.getPackageName());
            }
        }
        TvInputManager tv = getSystemService(TvInputManager.class);
        if (tv != null) {
            try {
                for (TvInputInfo input : tv.getTvInputList()) packages.add(input.getServiceInfo().packageName);
            } catch (RuntimeException ignored) {}
        }
        return packages;
    }

    private boolean eligible(String pkg, Set<String> protectedPackages) {
        if (!RecentAppsPolicy.safe(pkg) || protectedPackages.contains(pkg)) return false;
        try {
            ApplicationInfo app = getPackageManager().getApplicationInfo(pkg, 0);
            return app.enabled && app.uid % 100_000 >= 10_000 && launchIntent(pkg) != null;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private Intent launchIntent(String pkg) {
        PackageManager pm = getPackageManager();
        Intent target = pm.getLeanbackLaunchIntentForPackage(pkg);
        return target == null ? pm.getLaunchIntentForPackage(pkg) : target;
    }

    private void refresh() {
        int request = ++generation;
        busy = true;
        all.setEnabled(false);
        worker.execute(() -> {
            List<String> visible = new ArrayList<>();
            try {
                Set<String> protectedApps = protectedPackages();
                for (String pkg : RecentAppsPolicy.packages(LocalCecReader.readRecentApps(), user)) {
                    if (eligible(pkg, protectedApps)) visible.add(pkg);
                }
            } catch (Exception failure) {
                main.post(() -> {
                    if (!current(request)) return;
                    busy = false;
                    status.setText("Lokale ADB-Verbindung benötigt. In Ton & HDMI-CEC die lokale Verbindung freigeben.");
                });
                return;
            }
            main.post(() -> {
                if (!current(request)) return;
                displayed.clear();
                displayed.addAll(visible);
                render();
            });
            Map<String, RecentAppMetrics> metrics = RecentAppMetrics.read(this, visible, user);
            main.post(() -> {
                if (current(request)) showMetrics(metrics);
            });
        });
    }

    private boolean current(int request) {
        return !isFinishing() && !isDestroyed() && request == generation;
    }

    private void render() {
        busy = false;
        row.removeAllViews();
        metricLabels.clear();
        status.setText(displayed.isEmpty() ? "Keine geöffneten Apps zum Schließen"
                : displayed.size() + (displayed.size() == 1 ? " App · Links/Rechts wählen" : " Apps · Links/Rechts wählen"));
        all.setEnabled(!displayed.isEmpty());
        for (String pkg : displayed) {
            try {
                addCard(pkg, getPackageManager().getApplicationInfo(pkg, 0));
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        if (row.getChildCount() == 0) done.requestFocus();
    }

    private void addCard(String pkg, ApplicationInfo info) {
        String name = AppAppearance.label(this, pkg, getPackageManager().getApplicationLabel(info).toString());
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cardSize = new LinearLayout.LayoutParams(dp(190), -1);
        cardSize.setMarginEnd(dp(16));
        row.addView(card, cardSize);

        Drawable image = artwork(pkg, info);
        int dominant = AppAdapter.dominantColor(image);
        if (dominant == 0xFF6E6E73) dominant = 0xFFB8C0CC;
        FrameLayout artwork = new FrameLayout(this);
        card.addView(artwork, new LinearLayout.LayoutParams(-1, dp(116)));
        float radius = dp(12);
        GlassView frame = RecentAppsAppearance.frame(this, dominant, radius);
        frame.setVisibility(View.INVISIBLE);
        int inset = (int) Math.ceil(frame.padding() + frame.outset());
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(image);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setBackground(RecentAppsAppearance.plate(this, 0xFF121C29, 0, radius));
        icon.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        icon.setClipToOutline(true);
        icon.setFocusable(true);
        icon.setFocusableInTouchMode(true);
        icon.setId(View.generateViewId());
        icon.setContentDescription(name + " öffnen");
        FrameLayout.LayoutParams imageSize = new FrameLayout.LayoutParams(-1, -1);
        imageSize.setMargins(inset, inset, inset, inset);
        artwork.addView(icon, imageSize);
        artwork.addView(frame, new FrameLayout.LayoutParams(-1, -1));
        icon.setOnClickListener(v -> {
            if (busy) return;
            Intent target = launchIntent(pkg);
            if (target != null) {
                AppLock.launch(this, target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });
        icon.setOnFocusChangeListener((v, focused) -> {
            frame.setVisibility(focused ? View.VISIBLE : View.INVISIBLE);
            artwork.animate().scaleX(focused ? 1.04f : 1).scaleY(focused ? 1.04f : 1).setDuration(160).start();
        });

        TextView label = GlassDialog.text(this, name, 12);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(label, new LinearLayout.LayoutParams(-1, dp(24)));
        TextView metrics = GlassDialog.text(this, "CPU  …\nRAM  …\nSpeicher  …", 11);
        metrics.setGravity(Gravity.CENTER);
        metrics.setTextColor(0xFFD3DEEC);
        metricLabels.put(pkg, metrics);
        card.addView(metrics, new LinearLayout.LayoutParams(-1, -2));

        TextView close = button("×");
        close.setTextSize(25);
        close.setBackground(RecentAppsAppearance.control(this, dominant));
        close.setId(View.generateViewId());
        close.setContentDescription(name + " schließen");
        LinearLayout.LayoutParams closeSize = new LinearLayout.LayoutParams(dp(64), dp(34));
        closeSize.topMargin = dp(6);
        card.addView(close, closeSize);
        close.setOnClickListener(v -> closeApps(Collections.singletonList(pkg)));
        icon.setNextFocusDownId(close.getId());
        close.setNextFocusUpId(icon.getId());
        if (row.getChildCount() == 1) icon.requestFocus();
    }

    private Drawable artwork(String pkg, ApplicationInfo info) {
        Drawable image = AppAppearance.icon(this, pkg);
        if (image != null) return image;
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (launch != null && launch.getComponent() != null) {
            try {
                ActivityInfo activity = pm.getActivityInfo(launch.getComponent(), 0);
                image = activity.loadBanner(pm);
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return image == null ? pm.getApplicationIcon(info) : image;
    }

    private void showMetrics(Map<String, RecentAppMetrics> values) {
        NumberFormat percent = NumberFormat.getPercentInstance();
        percent.setMaximumFractionDigits(1);
        for (Map.Entry<String, TextView> label : metricLabels.entrySet()) {
            RecentAppMetrics reading = values.get(label.getKey());
            String cpu = reading == null || reading.cpu == null ? "—" : percent.format(reading.cpu / 100d);
            String ram = reading == null || reading.ram == null ? "—" : Formatter.formatShortFileSize(this, reading.ram);
            String disk = reading == null || reading.storage == null ? "—" : Formatter.formatShortFileSize(this, reading.storage);
            label.getValue().setText("CPU  " + cpu + "\nRAM  " + ram + "\nSpeicher  " + disk);
        }
    }

    private void closeApps(List<String> packages) {
        if (busy || packages.isEmpty()) return;
        int request = generation;
        busy = true;
        all.setEnabled(false);
        status.setText("Apps werden geschlossen …");
        worker.execute(() -> {
            String error = null;
            try {
                Set<String> protectedApps = protectedPackages();
                for (String pkg : packages) {
                    if (!eligible(pkg, protectedApps)) continue;
                    String result = LocalCecReader.stopRecentApp(pkg);
                    if (result.contains("Exception") || result.contains("Error")) throw new IllegalStateException();
                }
                List<String> remaining = RecentAppsPolicy.packages(LocalCecReader.readRecentApps(), user);
                for (String pkg : packages) {
                    if (remaining.contains(pkg)) throw new IllegalStateException();
                }
            } catch (Exception failure) {
                error = "Schließen nicht bestätigt. Lokale ADB-Verbindung prüfen.";
            }
            String message = error;
            main.post(() -> {
                if (!current(request)) return;
                if (message == null) refresh();
                else {
                    busy = false;
                    all.setEnabled(true);
                    status.setText(message);
                }
            });
        });
    }

    @Override protected void onDestroy() {
        generation++;
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        super.onDestroy();
    }
}
