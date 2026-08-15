package com.example.mangareader;

import android.content.Context;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public final class CacheManager {
    private CacheManager() {
    }

    public static File thumbsDir(Context c) {
        File f = new File(c.getCacheDir(), "thumb");
        f.mkdirs();
        return f;
    }

    public static File pagesDir(Context c) {
        File f = new File(c.getCacheDir(), "manga/pages");
        f.mkdirs();
        return f;
    }

    public static File zipDir(Context c) {
        File f = new File(c.getCacheDir(), "manga/cbz");
        f.mkdirs();
        return f;
    }

    public static long size(File dir) {
        long s = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                s += f.isDirectory() ? size(f) : f.length();
            }
        }
        return s;
    }

    public static void clear(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRec(f);
            }
        }
    }

    private static void deleteRec(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File x : files) {
                    deleteRec(x);
                }
            }
        }
        f.delete();
    }

    public static String human(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.0f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static void trimThumbs(Context c) {
        int limit = Prefs.get(c).thumbLimitMb();
        if (limit <= 0) return;
        long max = (long) limit * 1024 * 1024;
        File dir = thumbsDir(c);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;
        long total = size(dir);
        if (total <= max) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (File f : files) {
            if (total <= max) break;
            total -= f.length();
            f.delete();
        }
    }

    public static void cleanupZip(Context c) {
        int days = Prefs.get(c).zipRetentionDays();
        if (days <= 0) return;
        long cutoff = System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000;
        File dir = zipDir(c);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.lastModified() < cutoff) {
                f.delete();
            }
        }
    }

    public static void cleanup(Context c) {
        trimThumbs(c);
        cleanupZip(c);
    }
}
