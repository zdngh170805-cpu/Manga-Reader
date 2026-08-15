package com.example.mangareader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

    private String t(String key) {
        return Strings.get(this, key);
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

        section(t("general"));
        String[] themes = {"System", "Dark", "Light"};
        clickRow(t("theme"), t("current") + themes[prefs.theme()], v -> {
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(t("theme"))
                    .setSingleChoiceItems(themes, prefs.theme(), (d, which) -> {
                        prefs.setTheme(which);
                        d.dismiss();
                        recreate();
                    })
                    .setNegativeButton(t("cancel"), null));
        });

        final String[] langNames = {"System", "English", "Bahasa Indonesia"};
        clickRow(t("language"), t("current") + langNames[prefs.language()], v -> {
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(t("language"))
                    .setSingleChoiceItems(langNames, prefs.language(), (d, which) -> {
                        prefs.setLanguage(which);
                        d.dismiss();
                        recreate();
                    })
                    .setNegativeButton(t("cancel"), null));
        });

        final String[] encNames = {"System", "UTF-8", "ISO-8859-1"};
        clickRow(t("encoding"), t("current") + encNames[prefs.encoding()], v -> {
            showDialog(new AlertDialog.Builder(this)
                    .setTitle(t("encoding"))
                    .setSingleChoiceItems(encNames, prefs.encoding(), (d, which) -> {
                        prefs.setEncoding(which);
                        d.dismiss();
                        rebuild();
                    })
                    .setNegativeButton(t("cancel"), null));
        });

        clickRow(t("reset"), t("reset_desc"), v -> {
            confirm(t("reset"), t("reset_desc"),
                    () -> {
                        prefs.resetAll();
                        recreate();
                    });
        });
        clickRow(t("all_files"), hasAllFilesAccess()
                ? t("all_files_on") : t("all_files_off"), v -> openAllFilesSettings());

        section(t("list_view"));
        switchRow(t("fab"), t("fab_desc"),
                prefs.fab(), (b, c) -> prefs.setFab(c));
        switchRow(t("show_statusbar"), t("show_statusbar_desc"),
                prefs.showStatusbar(), (b, c) -> prefs.setShowStatusbar(c));
        switchRow(t("show_folder"), t("show_folder_desc"),
                prefs.showFolderPath(), (b, c) -> prefs.setShowFolderPath(c));

        section(t("resume_sec"));
        switchRow(t("resume"), t("resume_desc"),
                prefs.resumeEnabled(), (b, c) -> prefs.setResumeEnabled(c));
        clickRow(t("sync"), t("sync_desc"), v ->
                toast(t("sync_beta")));

        section(t("image_view"));
        switchRow(t("fullscreen"), t("fullscreen_desc"),
                prefs.fullscreen(), (b, c) -> prefs.setFullscreen(c));
        switchRow(t("dual_divider"), t("dual_divider_desc"),
                prefs.dualDivider(), (b, c) -> prefs.setDualDivider(c));
        switchRow(t("scroll_divider"), t("scroll_divider_desc"),
                prefs.scrollDivider(), (b, c) -> prefs.setScrollDivider(c));
        switchRow(t("punch_portrait"), t("punch_portrait_desc"),
                prefs.punchHolePortrait(), (b, c) -> prefs.setPunchHolePortrait(c));
        switchRow(t("punch_landscape"), t("punch_landscape_desc"),
                prefs.punchHoleLandscape(), (b, c) -> prefs.setPunchHoleLandscape(c));
        switchRow(t("left_scrollbar"), t("left_scrollbar_desc"),
                prefs.leftScrollbar(), (b, c) -> prefs.setLeftScrollbar(c));

        section(t("cache_data"));
        infoRow(t("thumbs"), CacheManager.human(CacheManager.size(CacheManager.thumbsDir(this))));
        infoRow(t("extracted"), CacheManager.human(CacheManager.size(CacheManager.pagesDir(this))));
        infoRow(t("ziparchives"), CacheManager.human(CacheManager.size(CacheManager.zipDir(this))));

        final String[] limOpts = {t("unlimited"), "50 MB", "100 MB", "200 MB", "500 MB"};
        final int[] limVals = {0, 50, 100, 200, 500};
        clickRow(t("thumb_limit"), t("current") + limOpts[indexOf(limVals, prefs.thumbLimitMb())], v ->
                showDialog(new AlertDialog.Builder(this)
                        .setTitle(t("thumb_limit"))
                        .setSingleChoiceItems(limOpts, indexOf(limVals, prefs.thumbLimitMb()),
                                (d, which) -> {
                                    prefs.setThumbLimitMb(limVals[which]);
                                    d.dismiss();
                                    rebuild();
                                })
                        .setNegativeButton(t("cancel"), null)));

        final String[] retOpts = {retentionLabel(1), retentionLabel(3), retentionLabel(7),
                retentionLabel(14), retentionLabel(30)};
        final int[] retVals = {1, 3, 7, 14, 30};
        clickRow(t("zip_retention"), t("current") + retOpts[indexOf(retVals, prefs.zipRetentionDays())],
                v -> showDialog(new AlertDialog.Builder(this)
                        .setTitle(t("zip_retention"))
                        .setMessage(t("zip_retention_desc"))
                        .setSingleChoiceItems(retOpts, indexOf(retVals, prefs.zipRetentionDays()),
                                (d, which) -> {
                                    prefs.setZipRetentionDays(retVals[which]);
                                    d.dismiss();
                                    rebuild();
                                })
                        .setNegativeButton(t("cancel"), null)));

        clickRow(t("clear_thumbs"), t("clear_thumbs_desc"), v ->
                confirm(t("clear_thumbs"), t("clear_thumbs_q"),
                        () -> {
                            CacheManager.clear(CacheManager.thumbsDir(this));
                            rebuild();
                        }));
        clickRow(t("clear_pages"), t("clear_pages_desc"), v ->
                confirm(t("clear_cache_title"), t("clear_cache_q"),
                        () -> {
                            CacheManager.clear(CacheManager.zipDir(this));
                            CacheManager.clear(CacheManager.pagesDir(this));
                            rebuild();
                        }));
        clickRow(t("clear_resume"), t("clear_resume_desc"), v ->
                confirm(t("clear_resume"), t("clear_resume_q"), () -> {
                    db.clearResume();
                    rebuild();
                }));
        clickRow(t("clear_bookmarks"), t("clear_bookmarks_desc"), v ->
                confirm(t("clear_bookmarks"), t("clear_bookmarks_q"), () -> {
                    db.clearBookmarks();
                    rebuild();
                }));
        clickRow(t("clear_history"), t("clear_history_desc"), v ->
                confirm(t("clear_history"), t("clear_history_q"), () -> {
                    db.clearHistory();
                    rebuild();
                }));
        clickRow(t("clear_all"), t("clear_all_desc"), v ->
                confirm(t("clear_all"), t("clear_all_q"), () -> {
                    CacheManager.clear(CacheManager.thumbsDir(this));
                    CacheManager.clear(CacheManager.pagesDir(this));
                    CacheManager.clear(CacheManager.zipDir(this));
                    db.clearResume();
                    rebuild();
                }));

        section(t("information"));
        infoRow(t("version"), "1.0");
        clickRow(t("about"), t("about_desc"), v ->
                showDialog(new AlertDialog.Builder(this)
                        .setTitle("Manga Reader")
                        .setMessage(t("about_msg"))
                        .setPositiveButton(t("ok"), null)));
        clickRow(t("licenses"), t("licenses_desc"), v ->
                showDialog(new AlertDialog.Builder(this)
                        .setTitle(t("licenses"))
                        .setMessage(t("licenses_msg"))
                        .setPositiveButton(t("ok"), null)));
    }

    private String retentionLabel(int days) {
        return days == 1 ? t("retention_1day")
                : String.format(java.util.Locale.ROOT, t("retention_days"), days);
    }

    private int indexOf(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return 0;
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
        showDialog(new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(t("delete"), (d, w) -> action.run())
                .setNegativeButton(t("cancel"), null));
    }

    private void showDialog(AlertDialog.Builder builder) {
        AlertDialog d = builder.create();
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private boolean hasAllFilesAccess() {
        return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
    }

    private void openAllFilesSettings() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        }
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
