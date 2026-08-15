package com.example.mangareader;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_OPEN_DOC = 1;
    private static final int REQ_TREE = 2;

    private Prefs prefs;
    private ComicDb db;

    private LinearLayout content;
    private LinearLayout header;
    private TextView headerTitle;
    private LinearLayout bottomNav;
    private FrameLayout fabHost;

    private int currentTab = 0; // 0 storage, 1 history, 2 bookmark
    private DocumentFile currentDir;
    private final ArrayList<DocumentFile> dirStack = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = Prefs.get(this);
        db = ComicDb.get(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderAll();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        renderAll();
    }

    private void renderAll() {
        buildScreen();
        applyStatusbar();
        renderTab();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.bg(this));

        header = buildHeader();
        root.addView(header);

        fabHost = new FrameLayout(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        fabHost.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        if (prefs.fab()) {
            fabHost.addView(buildFab(), new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END));
        }
        root.addView(fabHost, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        bottomNav = new LinearLayout(this);
        buildBottomNav(bottomNav);
        root.addView(bottomNav);

        setContentView(root);
    }

    private LinearLayout buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(20), dp(20), dp(12), dp(12));

        headerTitle = new TextView(this);
        headerTitle.setText("Manga Reader");
        headerTitle.setTextSize(22);
        headerTitle.setTextColor(Ui.text(this));
        headerTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        h.addView(headerTitle, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextSize(22);
        gear.setTextColor(Ui.text(this));
        gear.setGravity(Gravity.CENTER);
        gear.setPadding(dp(10), dp(6), dp(10), dp(6));
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        h.addView(gear);
        return h;
    }

    private View buildFab() {
        TextView fab = new TextView(this);
        fab.setText("▶");
        fab.setTextSize(22);
        fab.setTextColor(Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        fab.setBackground(Ui.rounded(Ui.ACCENT, 0, 26, this));
        int s = dp(52);
        fab.setLayoutParams(new FrameLayout.LayoutParams(s, s));
        fab.setOnClickListener(v -> {
            if (prefs.lastUri() == null) {
                pickFile();
            } else {
                openReader(prefs.lastUri(), prefs.lastName(), prefs.lastPage());
            }
        });
        return fab;
    }

    private void buildBottomNav(LinearLayout bottomNav) {
        bottomNav.removeAllViews();
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setBackgroundColor(Ui.surface(this));
        bottomNav.setElevation(dp(8));

        String[][] tabs = {
                {"🗀", "Penyimpanan"},
                {"🕘", "Riwayat"},
                {"★", "Bookmark"}
        };
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(6), 0, dp(6));

            TextView icon = new TextView(this);
            icon.setText(tabs[i][0]);
            icon.setTextSize(18);
            icon.setGravity(Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(tabs[i][1]);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);

            boolean active = idx == currentTab;
            icon.setTextColor(active ? Ui.ACCENT : Ui.text2(this));
            label.setTextColor(active ? Ui.ACCENT : Ui.text2(this));

            item.addView(icon);
            item.addView(label);
            item.setBackgroundColor(active ? Ui.surfaceHigh(this) : Ui.surface(this));
            item.setOnClickListener(v -> {
                currentTab = idx;
                renderAll();
            });

            bottomNav.addView(item, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private void renderTab() {
        content.removeAllViews();
        switch (currentTab) {
            case 1:
                renderHistory();
                break;
            case 2:
                renderBookmarks();
                break;
            default:
                renderStorage();
        }
    }

    // ---------------- Storage ----------------

    private void renderStorage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        list.setPadding(pad, dp(6), pad, dp(24));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        if (currentDir != null) {
            renderBrowser(list);
            return;
        }
        renderStorageRoot(list);
    }

    private void renderStorageRoot(LinearLayout list) {
        Button open = new Button(this);
        open.setText("Pilih Manga / CBZ");
        open.setTextColor(Color.WHITE);
        open.setTextSize(16);
        open.setAllCaps(false);
        open.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        open.setBackground(Ui.rounded(Ui.ACCENT, 0, 18, this));
        open.setOnClickListener(v -> pickFile());
        LinearLayout.LayoutParams openLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54));
        list.addView(open, openLp);

        section(list, "PENYIMPANAN");

        String treeUri = prefs.treeUri();
        if (treeUri == null) {
            row(list, "Internal Storage / SD Card", "Ketuk untuk memilih folder",
                    Ui.ACCENT, v -> launchTree());
        } else {
            row(list, prefs.treeName() != null ? prefs.treeName() : "Internal Storage",
                    "Ketuk untuk membuka folder", Ui.ACCENT, v -> {
                        DocumentFile root = DocumentFile.fromTreeUri(this, Uri.parse(treeUri));
                        if (root != null) {
                            currentDir = root;
                            dirStack.clear();
                            renderAll();
                        }
                    });
            row(list, "Ganti folder", "Pilih lokasi lain", null,
                    v -> launchTree());
        }

        section(list, "TERAKHIR DIBUKA");
        List<Prefs.Recent> recents = prefs.recent();
        if (recents.isEmpty()) {
            hint(list, "Belum ada manga yang dibuka. Ketuk tombol di atas untuk memilih file .cbz / .zip.");
        } else {
            for (Prefs.Recent r : recents) {
                row(list, r.name, "Halaman " + (r.page + 1), null,
                        v -> openReader(r.uri, r.name, r.page));
            }
        }
    }

    private void renderBrowser(LinearLayout list) {
        DocumentFile dir = currentDir;
        LinearLayout pathBar = new LinearLayout(this);
        pathBar.setOrientation(LinearLayout.VERTICAL);
        pathBar.setPadding(0, dp(4), 0, dp(12));

        TextView crumb = new TextView(this);
        crumb.setText(breadcrumb());
        crumb.setTextSize(13);
        crumb.setTextColor(Ui.text2(this));
        crumb.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        crumb.setMaxLines(2);
        crumb.setEllipsize(TextUtils.TruncateAt.START);
        pathBar.addView(crumb);
        list.addView(pathBar);

        row(list, "..  (Kembali)", "Folder di atas", null, v -> upDir());

        DocumentFile[] children = dir.listFiles();
        List<DocumentFile> folders = new ArrayList<>();
        List<DocumentFile> comics = new ArrayList<>();
        if (children != null) {
            for (DocumentFile f : children) {
                if (f.isDirectory()) {
                    folders.add(f);
                } else if (isComic(f.getName())) {
                    comics.add(f);
                }
            }
        }
        Comparator<DocumentFile> byName = (a, b) -> {
            String x = a.getName() != null ? a.getName() : "";
            String y = b.getName() != null ? b.getName() : "";
            return x.compareToIgnoreCase(y);
        };
        Collections.sort(folders, byName);
        Collections.sort(comics, byName);

        if (folders.isEmpty() && comics.isEmpty()) {
            hint(list, "Folder kosong.");
        }
        for (DocumentFile f : folders) {
            row(list, "🗀  " + f.getName(), "Folder", null,
                    v -> enterDir(f));
        }
        if (!comics.isEmpty()) {
            gridOfComics(list, comics);
        }
    }

    private String breadcrumb() {
        StringBuilder sb = new StringBuilder();
        String base = prefs.treeName() != null ? prefs.treeName() : "Storage";
        if (!prefs.showFolderPath()) {
            return dirStack.isEmpty() ? base : dirStack.get(dirStack.size() - 1).getName();
        }
        sb.append(base);
        for (DocumentFile d : dirStack) {
            sb.append(" / ").append(d.getName());
        }
        return sb.toString();
    }

    private void enterDir(DocumentFile dir) {
        dirStack.add(dir);
        currentDir = dir;
        renderAll();
    }

    private void upDir() {
        if (dirStack.isEmpty()) {
            currentDir = null;
        } else {
            dirStack.remove(dirStack.size() - 1);
            currentDir = dirStack.isEmpty() ? null : dirStack.get(dirStack.size() - 1);
        }
        renderAll();
    }

    // ---------------- History ----------------

    private void renderHistory() {
        List<ComicDb.HistoryRow> rows = db.history();
        LinearLayout list = listScroll();
        section(list, "RIWAYAT");
        if (rows.isEmpty()) {
            hint(list, "Belum ada riwayat baca.");
        } else {
            for (ComicDb.HistoryRow r : rows) {
                String time = timeText(r.opened);
                rowWithThumb(list, r.thumb, r.name, "Halaman " + (r.page + 1) + " • " + time,
                        v -> openReader(r.uri, r.name, r.page),
                        v -> {
                            db.removeHistory(r.uri);
                            renderAll();
                        });
            }
        }
        Button clear = smallButton(list, "Bersihkan Riwayat");
        clear.setOnClickListener(v -> {
            db.clearHistory();
            renderAll();
        });
        content.addView(list);
    }

    // ---------------- Bookmarks ----------------

    private void renderBookmarks() {
        List<ComicDb.BookmarkRow> rows = db.bookmarks();
        LinearLayout list = listScroll();
        section(list, "BOOKMARK");
        if (rows.isEmpty()) {
            hint(list, "Belum ada bookmark. Ketuk ★ di reader untuk menandai halaman.");
        } else {
            for (ComicDb.BookmarkRow r : rows) {
                rowWithThumb(list, r.thumb, r.name, "Halaman " + (r.page + 1),
                        v -> openReader(r.uri, r.name, r.page),
                        v -> {
                            db.toggleBookmark(r.uri, r.name, r.page, null);
                            renderAll();
                        });
            }
        }
        Button clear = smallButton(list, "Bersihkan Bookmark");
        clear.setOnClickListener(v -> {
            db.clearBookmarks();
            renderAll();
        });
        content.addView(list);
    }

    // ---------------- helpers ----------------

    private LinearLayout listScroll() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        list.setPadding(pad, dp(6), pad, dp(24));
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        return list;
    }

    private void section(LinearLayout list, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(Ui.ACCENT);
        t.setLetterSpacing(0.12f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        list.addView(t, topLp(dp(22)));
    }

    private void hint(LinearLayout list, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(Ui.text2(this));
        t.setLineSpacing(dp(4), 1f);
        t.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 16, this));
        int p = dp(16);
        t.setPadding(p, p, p, p);
        list.addView(t, topLp(dp(12)));
    }

    private void row(LinearLayout list, String title, String sub, Integer accent,
                     View.OnClickListener onClick) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 14, this));
        int p = dp(16);
        item.setPadding(p, dp(12), p, dp(12));
        item.setOnClickListener(onClick);

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(16);
        t1.setTextColor(accent != null ? accent : Ui.text(this));
        t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t1.setSingleLine(true);
        t1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        item.addView(t1);

        if (sub != null) {
            TextView t2 = new TextView(this);
            t2.setText(sub);
            t2.setTextSize(13);
            t2.setTextColor(Ui.text2(this));
            t2.setSingleLine(true);
            t2.setEllipsize(TextUtils.TruncateAt.END);
            item.addView(t2, topLp(dp(3)));
        }
        list.addView(item, fullLp(dp(10)));
    }

    private void rowWithThumb(LinearLayout list, String thumbPath, String title, String sub,
                              View.OnClickListener onClick, View.OnClickListener onX) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 14, this));
        int p = dp(16);
        item.setPadding(p, dp(10), p, dp(10));
        item.setOnClickListener(onClick);

        if (thumbPath != null) {
            Bitmap b = loadThumb(thumbPath);
            if (b != null) {
                ImageView img = new ImageView(this);
                img.setImageBitmap(b);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                int s = dp(46);
                img.setBackground(Ui.rounded(Ui.surfaceHigh(this), Ui.divider(this), 8, this));
                item.addView(img, new LinearLayout.LayoutParams(s, s));
            }
        }

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(this);
        t1.setText(title);
        t1.setTextSize(16);
        t1.setTextColor(Ui.text(this));
        t1.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t1.setSingleLine(true);
        t1.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        texts.addView(t1);

        if (sub != null) {
            TextView t2 = new TextView(this);
            t2.setText(sub);
            t2.setTextSize(13);
            t2.setTextColor(Ui.text2(this));
            texts.addView(t2, topLp(dp(3)));
        }
        LinearLayout.LayoutParams textsLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textsLp.setMargins(dp(14), 0, 0, 0);
        item.addView(texts, textsLp);

        TextView x = new TextView(this);
        x.setText("✕");
        x.setTextSize(16);
        x.setTextColor(Ui.text2(this));
        x.setGravity(Gravity.CENTER);
        x.setPadding(dp(14), dp(6), dp(6), dp(6));
        x.setOnClickListener(onX);
        item.addView(x);

        list.addView(item, fullLp(dp(10)));
    }

    private Button smallButton(LinearLayout list, String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Ui.text(this));
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(Ui.rounded(Ui.surfaceHigh(this), Ui.divider(this), 16, this));
        list.addView(b, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50), 0));
        return b;
    }

    // ---------------- actions ----------------

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"application/vnd.comicbook+zip", "application/zip"})
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_OPEN_DOC);
    }

    private void launchTree() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_TREE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQ_OPEN_DOC) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                openReader(uri.toString(), displayName(uri), -1);
            }
        } else if (requestCode == REQ_TREE) {
            Uri tree = data.getData();
            if (tree != null) {
                try {
                    getContentResolver().takePersistableUriPermission(tree,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                String name = documentName(tree);
                prefs.setTree(tree.toString(), name);
                renderAll();
            }
        }
    }

    private void openReader(String uri, String name, int page) {
        Intent i = new Intent(this, ReaderActivity.class);
        i.putExtra(ReaderActivity.EXTRA_URI, uri);
        i.putExtra(ReaderActivity.EXTRA_NAME, name);
        if (page >= 0) {
            i.putExtra(ReaderActivity.EXTRA_PAGE, page);
        }
        startActivity(i);
    }

    private void applyStatusbar() {
        View decor = getWindow().getDecorView();
        if (prefs.showStatusbar()) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // ---------------- misc helpers ----------------

    private boolean isComic(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".cbz") || lower.endsWith(".zip")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp")
                || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    private boolean isImage(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".webp")
                || lower.endsWith(".gif") || lower.endsWith(".bmp");
    }

    private void gridOfComics(LinearLayout list, List<DocumentFile> comics) {
        int cell = dp(82);
        int cols = Math.max(2, Math.min(4, getResources().getDisplayMetrics().widthPixels
                / (int) (cell * 1.2f)));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        grid.setUseDefaultMargins(true);

        int i = 0;
        for (DocumentFile f : comics) {
            final DocumentFile file = f;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER_HORIZONTAL);
            card.setBackground(Ui.rounded(Ui.surface(this), Ui.divider(this), 12, this));
            int pad = dp(6);
            card.setPadding(pad, pad, pad, dp(8));
            card.setOnClickListener(v -> openReader(file.getUri().toString(), file.getName(), -1));

            boolean withThumb = i < 60 && isImage(file.getName());
            Bitmap b = withThumb ? loadImageThumb(file) : null;
            if (b != null) {
                ImageView img = new ImageView(this);
                img.setImageBitmap(b);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                card.addView(img, new LinearLayout.LayoutParams(dp(64), dp(88)));
            } else {
                card.addView(coverPlaceholder(withThumb ? "🖼" : "📕"));
            }

            TextView name = new TextView(this);
            name.setText(file.getName() != null ? file.getName() : "");
            name.setTextSize(11);
            name.setTextColor(Ui.text(this));
            name.setMaxLines(2);
            name.setGravity(Gravity.CENTER_HORIZONTAL);
            name.setEllipsize(TextUtils.TruncateAt.END);
            card.addView(name, topLp(dp(6)));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            int m = dp(5);
            lp.setMargins(m, m, m, m);
            grid.addView(card, lp);
            i++;
        }
        list.addView(grid, fullLp(dp(10)));
    }

    private TextView coverPlaceholder(String emoji) {
        TextView t = new TextView(this);
        t.setText(emoji);
        t.setTextSize(30);
        t.setTextColor(Ui.text2(this));
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(88)));
        return t;
    }

    private Bitmap loadImageThumb(DocumentFile f) {
        try {
            InputStream in = getContentResolver().openInputStream(f.getUri());
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int sample = 1;
            while (o.outWidth / sample > 240 || o.outHeight / sample > 240) {
                sample *= 2;
            }
            InputStream in2 = getContentResolver().openInputStream(f.getUri());
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            Bitmap b = BitmapFactory.decodeStream(in2, null, o);
            in2.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap loadThumb(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return null;
        int sample = 1;
        while (o.outWidth / sample > 220 || o.outHeight / sample > 220) {
            sample *= 2;
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, o);
    }

    private String displayName(Uri uri) {
        String s = uri.getLastPathSegment();
        if (s == null) return "Manga";
        int idx = s.lastIndexOf('/');
        return idx >= 0 ? s.substring(idx + 1) : s;
    }

    private String documentName(Uri tree) {
        DocumentFile f = DocumentFile.fromTreeUri(this, tree);
        if (f != null && f.getName() != null) return f.getName();
        return displayName(tree);
    }

    private String timeText(long millis) {
        return new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date(millis));
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
