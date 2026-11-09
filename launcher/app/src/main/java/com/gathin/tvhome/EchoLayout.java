package com.gathin.tvhome;

enum EchoLayout {
    COMPACT("compact", "Kompakt", 290, 82),
    WIDE("wide", "16:9", 320, 180),
    SQUARE("square", "Quadratisch", 208, 208),
    TALL("tall", "Hoch", 174, 256),
    SMALL("small", "Klein", 240, 104),
    AUTO("auto", "Automatisch", 0, 0);

    final String id;
    final String label;
    final int width;
    final int height;

    EchoLayout(String id, String label, int width, int height) {
        this.id = id;
        this.label = label;
        this.width = width;
        this.height = height;
    }

    static EchoLayout selected(String id) {
        for (EchoLayout layout : values()) {
            if (layout.id.equals(id)) return layout;
        }
        return COMPACT;
    }

    EchoLayout resolve(int width, int height) {
        if (this != AUTO) return this;
        float ratio = width / (float) Math.max(1, height);
        if (height <= 92) return COMPACT;
        if (height <= 120 && ratio >= 1.5f) return SMALL;
        if (ratio < 0.85f) return TALL;
        return ratio <= 1.25f ? SQUARE : WIDE;
    }
}
