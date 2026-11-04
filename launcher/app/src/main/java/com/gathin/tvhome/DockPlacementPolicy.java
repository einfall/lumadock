package com.gathin.tvhome;

/** Percentages describe the free room, so neither edge can move outside its viewport. */
final class DockPlacementPolicy {
    static int percent(int value) { return Math.max(0, Math.min(100, value)); }
    static int width(int available, int requested) {
        return Math.round(Math.max(0, available) * Math.max(60, percent(requested)) / 100f);
    }
    static int offset(int available, int occupied, int percent) {
        return Math.round(Math.max(0, available - occupied) * percent(percent) / 100f);
    }
    static int top(int minimum, int maximum, int percent) {
        return minimum + Math.round(Math.max(0, maximum - minimum) * percent(percent) / 100f);
    }
    static int pages(int apps, int columns) { return apps <= 0 ? 0 : (apps - 1) / Math.max(1, columns) + 1; }
    static int page(int position, int apps, int columns) {
        return apps <= 0 ? 0 : Math.max(0, Math.min(apps - 1, position)) / Math.max(1, columns);
    }
}
