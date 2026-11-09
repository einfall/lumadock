package com.gathin.tvhome;

import java.util.Arrays;

enum TimeWidgetStyle {
    ARC("arc", "Arc Clock", "Die Uhrzeit im Tagesring", false),
    HOURS("hours", "Arc Hours Clock", "24 Stunden als Leuchtmarken", false),
    CALENDAR_CLOCK("calendar_clock", "Calendar Clock", "Uhrzeit mit Monatsraster", false),
    DEPARTURE("departure", "Departure Clock", "Ziffern wie auf einer Abflugtafel", false),
    MONTH("month", "Monatskalender", "Der ganze Monat auf einen Blick", true),
    WEEK("week", "Wochenkalender", "Die aktuelle Woche mit markiertem Heute", true);

    final String id, label, detail;
    final boolean calendar;

    TimeWidgetStyle(String id, String label, String detail, boolean calendar) {
        this.id = id;
        this.label = label;
        this.detail = detail;
        this.calendar = calendar;
    }

    static TimeWidgetStyle[] forSource(boolean calendar) {
        return Arrays.stream(values()).filter(style -> style.calendar == calendar).toArray(TimeWidgetStyle[]::new);
    }

    static TimeWidgetStyle restore(String id, boolean calendar) {
        for (TimeWidgetStyle style : values()) {
            if (style.calendar == calendar && style.id.equals(id)) return style;
        }
        return calendar ? MONTH : ARC;
    }
}
