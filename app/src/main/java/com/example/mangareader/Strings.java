package com.example.mangareader;

import android.content.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Strings {
    private Strings() {
    }

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> ID = new HashMap<>();

    private static void put(String key, String en, String id) {
        EN.put(key, en);
        ID.put(key, id);
    }

    static {
        put("general", "GENERAL", "UMUM");
        put("list_view", "LIST VIEW", "TAMPILAN DAFTAR");
        put("resume_sec", "RESUME PAGE", "LANJUTKAN BACA");
        put("image_view", "IMAGE VIEW", "TAMPILAN GAMBAR");
        put("cache_data", "CACHE DATA", "DATA CACHE");
        put("information", "INFORMATION", "INFORMASI");

        put("theme", "Theme", "Tema");
        put("current", "Current: ", "Saat ini: ");
        put("reset", "Reset Settings", "Reset Settings");
        put("reset_desc", "Restore all settings to default",
                "Kembalikan semua pengaturan ke default");
        put("all_files", "All files access", "Akses semua file");
        put("all_files_on", "Permission granted", "Izin aktif");
        put("all_files_off", "Enable direct local folder access",
                "Aktifkan akses folder lokal langsung");

        put("fab", "Quick view floating button", "Quick view floating button");
        put("fab_desc", "Open last file with floating button",
                "Buka file terakhir dengan tombol melayang");
        put("show_statusbar", "Show statusbar", "Show statusbar");
        put("show_statusbar_desc", "Show status bar for time, battery",
                "Tampilkan statusbar untuk waktu, baterai");
        put("show_folder", "Show folder path", "Show folder path");
        put("show_folder_desc", "Show folder path in browser",
                "Tampilkan path folder di browser");

        put("resume", "Resume page", "Resume page");
        put("resume_desc", "Resume from last page", "Lanjutkan dari halaman terakhir");
        put("sync", "Sync resume page (beta)", "Sync resume page (beta)");
        put("sync_desc", "Sync reading position via Google Drive",
                "Sinkronkan posisi baca via Google Drive");
        put("sync_beta", "Beta feature not implemented yet.",
                "Fitur beta belum diimplementasikan.");

        put("fullscreen", "Full screen", "Full screen");
        put("fullscreen_desc", "Hide status bar & navigation bar",
                "Sembunyikan status bar & navigation bar");
        put("dual_divider", "Page dividing line - dual pages",
                "Garis pemisah - dua halaman");
        put("dual_divider_desc", "Show divider between two pages",
                "Tampilkan garis pemisah antar dua halaman");
        put("scroll_divider", "Page dividing line - scroll pages",
                "Garis pemisah - scroll");
        put("scroll_divider_desc", "Divider between pages in vertical / continuous scroll",
                "Garis pemisah antar halaman saat scroll vertikal / kontinu");
        put("punch_portrait", "Punch hole display - portrait", "Punch hole - potret");
        put("punch_portrait_desc", "Extend image area around punch-hole (portrait)",
                "Perluas area gambar di sekitar punch-hole (potret)");
        put("punch_landscape", "Punch hole display - landscape", "Punch hole - lanskap");
        put("punch_landscape_desc", "Extend image area around punch-hole (landscape)",
                "Perluas area gambar di sekitar punch-hole (lanskap)");
        put("left_scrollbar", "Left scrollbar", "Scrollbar kiri");
        put("left_scrollbar_desc", "Place page slider on the left side",
                "Tempatkan slider halaman di sisi kiri");

        put("language", "Language", "Bahasa");
        put("encoding", "Encoding", "Pengodean");

        put("thumb_limit", "Thumbnail storage limit", "Batas penyimpanan thumbnail");
        put("unlimited", "Unlimited", "Tanpa batas");
        put("mb", "%d MB", "%d MB");
        put("zip_retention", "ZIP/CBZ retention", "Retensi arsip ZIP/CBZ");
        put("zip_retention_desc", "Delete unused archives after the set days",
                "Hapus arsip yang tidak dipakai setelah jumlah hari tertentu");
        put("retention_1day", "1 day", "1 hari");
        put("retention_days", "%d days", "%d hari");

        put("thumbs", "Thumbnails", "Thumbnails");
        put("extracted", "Extracted pages", "Halaman terekstrak");
        put("ziparchives", "ZIP/CBZ archives", "Arsip ZIP/CBZ");
        put("clear_thumbs", "Clear Thumbnails", "Bersihkan Thumbnail");
        put("clear_thumbs_desc", "Delete all thumbnails", "Hapus semua thumbnail");
        put("clear_thumbs_q", "Delete all thumbnails?", "Hapus semua thumbnail?");
        put("clear_pages", "Clear pages & archives", "Bersihkan halaman & arsip");
        put("clear_pages_desc", "Delete ZIP and extracted page cache",
                "Hapus cache ZIP dan halaman terekstrak");
        put("clear_cache_title", "Clear cache", "Bersihkan cache");
        put("clear_cache_q", "Delete ZIP and extracted page cache?",
                "Hapus cache ZIP dan halaman terekstrak?");
        put("clear_resume", "Clear resume cache", "Bersihkan cache resume");
        put("clear_resume_desc", "Delete all reading position data",
                "Hapus semua data posisi baca");
        put("clear_resume_q", "Delete all reading position data?",
                "Hapus semua data posisi baca?");
        put("clear_bookmarks", "Clear bookmarks", "Bersihkan bookmark");
        put("clear_bookmarks_desc", "Delete all bookmarks", "Hapus semua bookmark");
        put("clear_bookmarks_q", "Delete all bookmarks?", "Hapus semua bookmark?");
        put("clear_history", "Clear history", "Bersihkan riwayat");
        put("clear_history_desc", "Delete all history", "Hapus semua riwayat");
        put("clear_history_q", "Delete all history?", "Hapus semua riwayat?");
        put("clear_all", "Clear all cache", "Bersihkan semua cache");
        put("clear_all_desc", "Delete all cache data", "Hapus semua data cache");
        put("clear_all_q", "Delete all cache data?", "Hapus semua data cache?");

        put("version", "Version", "Version");
        put("about", "About", "Tentang");
        put("about_desc", "About the app", "Tentang aplikasi");
        put("licenses", "Licenses", "Lisensi");
        put("licenses_desc", "Open source licenses", "Lisensi open source");
        put("about_msg", "Offline comic / manga reader.\nSupports CBZ, ZIP, JPG, PNG, WEBP, GIF, BMP formats.\nDark & light theme, RTL/LTR, dual page, zoom, bookmark, history, resume.",
                "Comic / manga reader offline.\nDukung format CBZ, ZIP, JPG, PNG, WEBP, GIF, BMP.\nDark & light theme, RTL/LTR, dual page, zoom, bookmark, history, resume.");
        put("licenses_msg", "Built on Android SDK and Jetpack (AndroidX).\nAll visual assets are original.",
                "Aplikasi dibangun di atas Android SDK, Jetpack (AndroidX).\nSemua aset visual original.");

        put("ok", "OK", "OK");
        put("cancel", "Cancel", "Batal");
        put("delete", "Delete", "Hapus");
    }

    public static String get(Context c, String key) {
        int lang = Prefs.get(c).language();
        if (lang == 0) {
            String l = Locale.getDefault().getLanguage();
            lang = ("id".equalsIgnoreCase(l)) ? 2 : 1;
        }
        Map<String, String> m = lang == 2 ? ID : EN;
        String s = m.get(key);
        return s != null ? s : key;
    }
}
