package com.example.mangareader;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;

public final class Ui {
    public static final int ACCENT = 0xFF7C8CFF;
    public static final int WHITE = 0xFFFFFFFF;

    private Ui() {
    }

    public static boolean dark(Context c) {
        int t = Prefs.get(c).theme();
        if (t == 1) return true;
        if (t == 2) return false;
        int night = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night != Configuration.UI_MODE_NIGHT_NO;
    }

    public static int bg(Context c) { return dark(c) ? 0xFF0E1116 : 0xFFF2F3F7; }
    public static int surface(Context c) { return dark(c) ? 0xFF161B22 : 0xFFFFFFFF; }
    public static int surfaceHigh(Context c) { return dark(c) ? 0xFF1F2631 : 0xFFE9EBF0; }
    public static int text(Context c) { return dark(c) ? 0xFFF1F3F6 : 0xFF14171C; }
    public static int text2(Context c) { return dark(c) ? 0xFF9AA2B0 : 0xFF5A6472; }
    public static int divider(Context c) { return dark(c) ? 0xFF262E3A : 0xFFD8DCE4; }
    public static int pill(Context c) { return dark(c) ? 0x1F7C8CFF : 0x1A5A6BF0; }

    public static int dp(Context c, int v) {
        return Math.round(c.getResources().getDisplayMetrics().density * v);
    }

    public static GradientDrawable rounded(int fill, int stroke, int radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(c, radiusDp));
        g.setColor(fill);
        if (stroke != 0) {
            g.setStroke(dp(c, 1), stroke);
        }
        return g;
    }
}
