package com.example.mangareader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

public final class Thumbs {
    private Thumbs() {
    }

    public static File ensure(Context c, File page, String tag) {
        if (page == null) return null;
        File dir = CacheManager.thumbsDir(c);
        File out = new File(dir, tag.replaceAll("[^\\w.-]", "_") + ".jpg");
        if (out.exists()) return out;
        Bitmap b = decode(page, 256);
        if (b == null) return null;
        try (FileOutputStream o = new FileOutputStream(out)) {
            b.compress(Bitmap.CompressFormat.JPEG, 80, o);
        } catch (Exception ignored) {
        }
        b.recycle();
        CacheManager.trimThumbs(c);
        return out.exists() ? out : null;
    }

    public static Bitmap decode(File file, int target) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), opts);
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null;
        int sample = 1;
        while (opts.outWidth / sample > target || opts.outHeight / sample > target) {
            sample *= 2;
        }
        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;
        return BitmapFactory.decodeFile(file.getPath(), opts);
    }
}
