package com.example.mangareader;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private Prefs prefs;
    private ComicDb db;
    private LinearLayout list;

    @Override
    protected void onResume() {
        super.onResume();
        prefs = Prefs.get(this);
        db = ComicDb.get(this);
        build();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        list.setPadding(pad, dp(12), pad, dp(32));
        scroll.addView(list);

        setContentView(scroll);

        section("GENERAL");
        String[] themes = {"System", "Dark", "Light"};
        String[] names = {"System", "Dark", "Light"};
        clickRow("Tema", "Saat ini: " + themes[prefs.theme()], v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Tema")
                    .setSingleChoiceItems(names, prefs.theme(), (d, which) -> {
                        prefs.setTheme(which);
                        d.dismiss();
                        recreate();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });
        clickRow("Reset Settings", "Kembalikan semua pengaturan ke default", v -> {
            confirm("Reset Settings", "Semua pengaturan akan dikembalikan ke default.",
                    () -> {
                        prefs.resetAll();
                        recreate();
                    });
        });

        section("LIST VIEW");
        switchRow("Quick view floating button", "Buka file terakhir dengan tombol melayang",
                prefs.fab(), (b, c) -> prefs.setFab(c));
        switchRow("Show statusbar", "Tampilkan statusbar untuk waktu, baterai",
                prefs.showStatusbar(), (b, c) -> prefs.setShowStatusbar(c));
        switchRow("Show folder path", "Tampilkan path folder di browser",
                prefs.showFolderPath(), (b, c) -> prefs.setShowFolderPath(c));

        section("RESUME PAGE");
        switchRow("Resume page", "Lanjutkan dari halaman terakhir",
                prefs.resumeEnabled(), (b, c) -> prefs.setResumeEnabled(c));
        clickRow("Sync resume page (beta)", "Sinkronkan posisi baca via Google Drive", v ->
                toast("Fitur beta belum diimplementasikan."));

        section("IMAGE VIEW");
        switchRow("Full screen", "Sembunyikan status bar & navigation bar",
                prefs.fullscreen(), (b, c) -> prefs.setFullscreen(c));
        switchRow("Page dividing line - dual pages",
                "Tampilkan garis pemisah antar dua halaman",
                prefs.dualDivider(), (b, c) -> prefs.setDualDivider(c));
        switchRow("Page dividing line - scroll pages",
                "Garis pemisah antar halaman saat scroll vertikal / kontinu",
                prefs.scrollDivider(), (b, c) -> prefs.setScrollDivider(c));
        switchRow("Punch hole display - portrait",
                "Perluas area gambar di sekitar punch-hole (potret)",
                prefs.punchHolePortrait(), (b, c) -> prefs.setPunchHolePortrait(c));
        switchRow("Punch hole display - landscape",
                "Perluas area gambar di sekitar punch-hole (lanskap)",
                prefs.punchHoleLandscape(), (b, c) -> prefs.setPunchHoleLandscape(c));

        section("CACHE DATA");
        infoRow("Thumbnails", CacheManager.human(CacheManager.size(CacheManager.thumbsDir(this))));
        infoRow("Halaman terekstrak", CacheManager.human(CacheManager.size(CacheManager.pagesDir(this))));
        infoRow("Arsip ZIP/CBZ", CacheManager.human(CacheManager.size(CacheManager.zipDir(this))));

        clickRow("Clear Thumbnails", "Hapus semua thumbnail", v ->
                confirm("Clear Thumbnails", "Hapus semua thumbnail?",
                        () -> {
                            CacheManager.clear(CacheManager.thumbsDir(this));
                            rebuild();
                        }));
        clickRow("Clear halaman & arsip", "Hapus cache ZIP dan halaman terekstrak", v ->
                confirm("Clear cache", "Hapus cache ZIP dan halaman terekstrak?",
                        () -> {
                            CacheManager.clear(CacheManager.zipDir(this));
                            CacheManager.clear(CacheManager.pagesDir(this));
                            rebuild();
                        }));
        clickRow("Clear resume cache", "Hapus semua data posisi baca", v ->
                confirm("Clear resume cache", "Hapus semua data posisi baca?", () -> {
                    db.clearResume();
                    rebuild();
                }));
        clickRow("Clear bookmarks", "Hapus semua bookmark", v ->
                confirm("Clear bookmarks", "Hapus semua bookmark?", () -> {
                    db.clearBookmarks();
                    rebuild();
                }));
        clickRow("Clear history", "Hapus semua riwayat", v ->
                confirm("Clear history", "Hapus semua riwayat?", () -> {
                    db.clearHistory();
                    rebuild();
                }));
        clickRow("Clear all cache", "Hapus semua data cache", v ->
                confirm("Clear all cache", "Hapus semua data cache?", () -> {
                    CacheManager.clear(CacheManager.thumbsDir(this));
                    CacheManager.clear(CacheManager.pagesDir(this));
                    CacheManager.clear(CacheManager.zipDir(this));
                    db.clearResume();
                    rebuild();
                }));

        section("INFORMATION");
        infoRow("Version", "1.0");
        clickRow("About", "Tentang aplikasi", v ->
                new AlertDialog.Builder(this)
                        .setTitle("Manga Reader")
                        .setMessage("Comic / manga reader offline.\nDukung format CBZ, ZIP, JPG, PNG, WEBP, GIF, BMP.\nDark & light theme, RTL/LTR, dual page, zoom, bookmark, history, resume.")
                        .setPositiveButton("OK", null)
                        .show());
        clickRow("Licenses", "Lisensi open source", v ->
                new AlertDialog.Builder(this)
                        .setTitle("Licenses")
                        .setMessage("Aplikasi dibangun di atas Android SDK, Jetpack (AndroidX).\nSemua aset visual original.")
                        .setPositiveButton("OK", null)
                        .show());
    }

    private void section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(0xFF7C8CFF);
        t.setLetterSpacing(0.12f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        list.addView(t, topLp(dp(26)));
    }

    private void switchRow(String title, String desc, boolean initial,
                           CompoundButton.OnCheckedChangeListener onChange) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 14, this));
        int p = dp(16);
        item.setPadding(p, dp(10), p, dp(10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(15);
        t1.setTextColor(Ui.text(this));
        t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        texts.addView(t1);

        if (desc != null) {
            TextView t2 = new TextView(this);
            t2.setText(desc);
            t2.setTextSize(12);
            t2.setTextColor(Ui.text2(this));
            t2.setLineSpacing(dp(2), 1f);
            texts.addView(t2, topLp(dp(3)));
        }
        item.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Switch sw = new Switch(this);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener(onChange);
        sw.setPadding(dp(8), 0, 0, 0);
        item.addView(sw);

        list.addView(item, fullLp(dp(10)));
    }

    private void clickRow(String title, String desc, View.OnClickListener onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 14, this));
        int p = dp(16);
        item.setPadding(p, dp(12), p, dp(12));
        item.setOnClickListener(onClick);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(15);
        t1.setTextColor(Ui.text(this));
        t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        texts.addView(t1);

        if (desc != null) {
            TextView t2 = new TextView(this);
            t2.setText(desc);
            t2.setTextSize(12);
            t2.setTextColor(Ui.text2(this));
            t2.setLineSpacing(dp(2), 1f);
            texts.addView(t2, topLp(dp(3)));
        }
        item.addView(texts, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextSize(22);
        chevron.setTextColor(Ui.text2(this));
        item.addView(chevron);

        list.addView(item, fullLp(dp(10)));
    }

    private void infoRow(String title, String value) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 14, this));
        int p = dp(16);
        item.setPadding(p, dp(12), p, dp(12));

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(15);
        t1.setTextColor(Ui.text(this));
        item.addView(t1, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView t2 = new TextView(this);
        t2.setText(value);
        t2.setTextSize(14);
        t2.setTextColor(Ui.text2(this));
        t2.setGravity(Gravity.END);
        item.addView(t2);

        list.addView(item, fullLp(dp(10)));
    }

    private void confirm(String title, String message, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Hapus", (d, w) -> action.run())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    private void rebuild() {
        build();
    }

    private LinearLayout.LayoutParams topLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = top;
        return lp;
    }

    private LinearLayout.LayoutParams fullLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0);
        lp.topMargin = top;
        return lp;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
