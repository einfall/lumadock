package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

final class TimeWidgetView extends View {
    private static final Typeface LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean sample;
    private final Locale locale;
    private final boolean use24Hours;
    private final DateTimeFormatter time, date, month;
    private SurfaceStyle surface;
    private TimeWidgetStyle style;
    private ZonedDateTime now;
    private final Runnable tick = this::refresh;
    private boolean active;

    TimeWidgetView(Context context, boolean sample) {
        super(context);
        this.sample = sample;
        locale = context.getResources().getConfiguration().getLocales().get(0);
        use24Hours = sample || android.text.format.DateFormat.is24HourFormat(context);
        time = DateTimeFormatter.ofPattern(use24Hours ? "HH:mm" : "h:mm", locale);
        date = DateTimeFormatter.ofPattern(use24Hours ? "EEE, d. MMM" : "EEE, d. MMM · a", locale);
        month = DateTimeFormatter.ofPattern("LLLL yyyy", locale);
        now = sample ? ZonedDateTime.now().withHour(9).withMinute(41) : ZonedDateTime.now();
    }

    void style(TimeWidgetStyle style, SurfaceStyle surface) {
        this.style = style;
        this.surface = surface;
        invalidate();
    }

    void active(boolean on) {
        if (active == on) return;
        active = on;
        refresh();
    }

    private void refresh() {
        removeCallbacks(tick);
        if (sample || !active) return;
        now = ZonedDateTime.now();
        setContentDescription(style.label + " · " + time.format(now) + " · " + date.format(now));
        invalidate();
        postDelayed(tick, 60_000 - Math.floorMod(System.currentTimeMillis(), 60_000));
    }

    @Override protected void onDraw(Canvas canvas) {
        if (style == null || surface == null) return;
        float width = style == TimeWidgetStyle.CALENDAR_CLOCK && getWidth() > getHeight() * 1.85f ? 300 : 220;
        float scale = Math.min(getWidth() / width, getHeight() / 120f);
        canvas.save();
        canvas.translate((getWidth() - width * scale) / 2, (getHeight() - 120 * scale) / 2);
        canvas.scale(scale, scale);
        switch (style) {
            case ARC: drawArc(canvas, false); break;
            case HOURS: drawArc(canvas, true); break;
            case DEPARTURE: drawDeparture(canvas); break;
            case CALENDAR_CLOCK:
                if (width == 300) {
                    drawTime(canvas, 71, 54, 40);
                    drawMonth(canvas, 211, 4, 151);
                } else {
                    label(canvas, time.format(now), 110, 28, 29, surface.ink, LIGHT);
                    drawMonth(canvas, 110, 34, 168);
                }
                break;
            case MONTH: drawMonth(canvas, 110, 12, 205); break;
            case WEEK: drawWeek(canvas); break;
        }
        canvas.restore();
    }

    private int alpha(int color, int alpha) { return (color & 0xFFFFFF) | (alpha << 24); }

    private void drawArc(Canvas canvas, boolean marks) {
        int minutes = now.getHour() * 60 + now.getMinute();
        paint.setStrokeWidth(3);
        paint.setStrokeCap(Paint.Cap.ROUND);
        if (marks) {
            for (int hour = 0; hour < 24; hour++) {
                double angle = Math.toRadians(hour * 15 - 90);
                float x = (float) Math.cos(angle);
                float y = (float) Math.sin(angle);
                paint.setColor(hour <= now.getHour() ? surface.ink : alpha(surface.muted, 65));
                canvas.drawLine(110 + 47*x, 60 + 47*y, 110 + 54*x, 60 + 54*y, paint);
            }
        } else {
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(alpha(surface.muted, 65));
            canvas.drawCircle(110, 60, 51, paint);
            paint.setColor(surface.ink);
            canvas.drawArc(59, 9, 161, 111, -90, minutes / 4f, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        drawTime(canvas, 110, 61, 28);
    }

    private void drawTime(Canvas canvas, float x, float y, int size) {
        label(canvas, time.format(now), x, y, size, surface.ink, LIGHT);
        label(canvas, date.format(now), x, y + 19, 10, surface.muted, MEDIUM);
    }

    private void drawDeparture(Canvas canvas) {
        String digits = time.format(now).replace(":", "");
        float x = 110 - (digits.length() * 37 + 12) / 2f;
        for (int i = 0; i < digits.length(); i++) {
            if (i == digits.length() - 2) {
                label(canvas, ":", x + 4, 66, 27, surface.muted, MEDIUM);
                x += 12;
            }
            paint.setColor(alpha(surface.ink, 24));
            canvas.drawRoundRect(x, 26, x + 32, 80, 5, 5, paint);
            label(canvas, digits.substring(i, i + 1), x + 16, 68, 40, surface.ink, Typeface.MONOSPACE);
            paint.setColor(alpha(surface.muted, 100));
            canvas.drawRect(x, 53, x + 32, 54, paint);
            x += 37;
        }
        label(canvas, date.format(now), 110, 100, 11, surface.muted, MEDIUM);
    }

    private void drawMonth(Canvas canvas, float center, float top, float width) {
        LocalDate today = now.toLocalDate();
        LocalDate first = today.withDayOfMonth(1);
        DayOfWeek firstDay = WeekFields.of(locale).getFirstDayOfWeek();
        int offset = Math.floorMod(first.getDayOfWeek().getValue() - firstDay.getValue(), 7);
        float col = width / 7;
        float x = center - 3 * col;
        label(canvas, month.format(today), center, top + 11, 13, surface.ink, MEDIUM);
        for (int i = 0; i < 7; i++) {
            label(canvas, firstDay.plus(i).getDisplayName(TextStyle.NARROW, locale),
                    x + i*col, top + 24, 9, surface.muted, MEDIUM);
        }
        for (int day = 1; day <= first.lengthOfMonth(); day++) {
            int index = offset + day - 1;
            drawDay(canvas, day, day == today.getDayOfMonth(), x + index%7*col, top + 36 + index/7*9.5f, 9);
        }
    }

    private void drawWeek(Canvas canvas) {
        LocalDate today = now.toLocalDate();
        LocalDate first = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).getFirstDayOfWeek()));
        label(canvas, month.format(today), 110, 26, 17, surface.ink, MEDIUM);
        for (int i = 0; i < 7; i++) {
            LocalDate day = first.plusDays(i);
            float x = 20 + i * 30;
            label(canvas, day.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale), x, 57, 9, surface.muted, MEDIUM);
            drawDay(canvas, day.getDayOfMonth(), day.equals(today), x, 85, 16);
        }
    }

    private void drawDay(Canvas canvas, int day, boolean today, float x, float baseline, int size) {
        if (today) {
            paint.setColor(surface.ink);
            canvas.drawCircle(x, baseline - size * .33f, size * .54f, paint);
        }
        label(canvas, Integer.toString(day), x, baseline, size, today ? surface.focusInk : surface.ink, MEDIUM);
    }

    private void label(Canvas canvas, String text, float x, float y, int size, int color, Typeface face) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(face);
        paint.setTextSize(size);
        paint.setColor(color);
        canvas.drawText(text, x, y, paint);
    }
}
