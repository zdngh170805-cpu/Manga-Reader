package com.example.mangareader;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

public final class ImageAdjuster {
    private ImageAdjuster() {
    }

    public static ColorMatrixColorFilter filter(float brightness, float contrast,
                                                boolean grayscale, boolean invert) {
        float scale = contrast;
        float trans = (1f - scale) * 128f + (brightness - 1f) * 128f;
        ColorMatrix cm = new ColorMatrix();
        cm.set(new float[]{
                scale, 0, 0, 0, trans,
                0, scale, 0, 0, trans,
                0, 0, scale, 0, trans,
                0, 0, 0, 1, 0
        });
        if (grayscale) {
            ColorMatrix gray = new ColorMatrix();
            gray.setSaturation(0);
            cm.postConcat(gray);
        }
        if (invert) {
            ColorMatrix inv = new ColorMatrix(new float[]{
                    -1, 0, 0, 0, 255,
                    0, -1, 0, 0, 255,
                    0, 0, -1, 0, 255,
                    0, 0, 0, 1, 0
            });
            cm.postConcat(inv);
        }
        return new ColorMatrixColorFilter(cm);
    }
}
