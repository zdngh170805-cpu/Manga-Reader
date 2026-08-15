package com.example.mangareader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReaderActivity extends Activity {

    static final String EXTRA_URI = "uri";
    static final String EXTRA_NAME = "name";
    static final String EXTRA_PAGE = "page";

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final LruCache<File, Bitmap> cache =
            new LruCache<File, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 4)) {
                @Override
                protected int sizeOf(File key, Bitmap value) {
                    return value.getByteCount();
                }
            };

    private Prefs prefs;
    private ComicDb db;
    private String uri;
    private String name;
    private List<File> pages = new ArrayList<>();
    private int lastMode = -1;

    private FrameLayout root;
    private FrameLayout pageHost;
    private ViewPager2 pager;
    private ReaderAdapter adapter;
    private RecyclerView contList;
    private LinearLayoutManager contLm;
    private ContinuousAdapter contAdapter;
    private final RecyclerView.OnScrollListener contScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView rv, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE && !pages.isEmpty()) {
                int pos = currentStartPage();
                if (!updatingSlider) {
                    slider.setProgress(pos);
                }
                updateNavSlider(pos);
                updatePageText();
                updateBookmarkButton();
                saveProgress(pos);
            }
        }
    };
    private LinearLayout topBar;
    private HorizontalScrollView toolBarScroll;
    private LinearLayout toolBar;
    private LinearLayout bottomBar;
    private View loading;
    private TextView pageText;
    private TextView subText;
    private TextView bookmarkBtn;
    private SeekBar slider;
    private boolean updatingSlider;
    private boolean uiVisible = true;
    private TextView cropBtn;
    private TextView chapterBtn;
    private final List<Chapter> chapters = new ArrayList<>();
    private FrameLayout drawerWrap;
    private LinearLayout navRail;
    private SeekBar navSlider;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = Prefs.get(this);
        db = ComicDb.get(this);
        uri = getIntent().getStringExtra(EXTRA_URI);
        String n = getIntent().getStringExtra(EXTRA_NAME);
        if (n == null || n.isEmpty()) {
            n = prefs.lastName() != null ? prefs.lastName() : "Manga";
        }
        name = n;
        applyOrientationLock();
        applyPunchHole();
        buildUi();
        if (uri != null) {
            loadAsync();
        } else {
            Toast.makeText(this, "Tidak ada file.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        pageHost = new FrameLayout(this);
        root.addView(pageHost, new FrameLayout.LayoutParams(MATCH, MATCH));

        pager = new ViewPager2(this);
        pager.setOffscreenPageLimit(1);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (pages.isEmpty()) return;
                int start = itemStartPage(position);
                if (!updatingSlider) {
                    slider.setProgress(start);
                }
                updateNavSlider(start);
                updatePageText();
                updateBookmarkButton();
                saveProgress(start);
            }
        });
        pageHost.addView(pager, new FrameLayout.LayoutParams(MATCH, MATCH));

        topBar = buildTopBar();
        toolBarScroll = buildToolBarScroll();
        bottomBar = buildBottomBar();
        root.addView(topBar, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP));
        root.addView(toolBarScroll, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP));
        root.addView(bottomBar, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));

        buildNavRail();
        root.addView(navRail, new FrameLayout.LayoutParams(WRAP,
                Math.round(getResources().getDisplayMetrics().heightPixels * 0.52f),
                Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        applyNavPosition();

        loading = buildLoading();
        root.addView(loading, new FrameLayout.LayoutParams(MATCH, MATCH));

        hideSystemUi();
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xCC000000);
        bar.setPadding(dp(8), dp(10) + statusBarHeight(), dp(16), dp(8));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34);
        back.setTextColor(Color.WHITE);
        back.setGravity(Gravity.CENTER);
        back.setMinWidth(dp(44));
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        titles.addView(title);

        subText = new TextView(this);
        subText.setTextColor(Color.LTGRAY);
        subText.setTextSize(12);
        subText.setSingleLine(true);
        subText.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        titles.addView(subText);

        bar.addView(titles, new LinearLayout.LayoutParams(0, WRAP, 1));
        return bar;
    }

    private HorizontalScrollView buildToolBarScroll() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackgroundColor(0xAA000000);

        toolBar = new LinearLayout(this);
        toolBar.setOrientation(LinearLayout.HORIZONTAL);
        toolBar.setPadding(dp(8), 0, dp(8), 0);

        toolBar.addView(tool("Tata Letak", v -> showLayoutScrollingDialog()));
        toolBar.addView(tool("Ukuran", v -> showSizeDialog()));
        toolBar.addView(tool("Putar", v -> showRotateDialog()));
        toolBar.addView(tool("Jarak", v -> showSpacingDialog()));
        toolBar.addView(tool("Warna", v -> showAdjustDialog()));

        cropBtn = tool("✂", v -> toggleCrop());
        cropBtn.setMinWidth(dp(48));
        updateCropButton();
        toolBar.addView(cropBtn);

        chapterBtn = tool("☰", v -> toggleDrawer());
        chapterBtn.setMinWidth(dp(48));
        chapterBtn.setVisibility(View.GONE);
        toolBar.addView(chapterBtn);

        bookmarkBtn = tool("☆", v -> toggleBookmark());
        bookmarkBtn.setMinWidth(dp(48));
        toolBar.addView(bookmarkBtn);

        toolBar.addView(tool("⛶", v -> toggleFullscreen()));

        scroll.addView(toolBar);
        return scroll;
    }

    private TextView tool(String label, View.OnClickListener onClick) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(13);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setOnClickListener(onClick);
        return t;
    }

    private LinearLayout buildBottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(0xCC000000);
        bar.setPadding(0, dp(8), 0, dp(8) + navBarHeight());

        pageText = new TextView(this);
        pageText.setText("Loading...");
        pageText.setTextColor(Color.WHITE);
        pageText.setTextSize(14);
        pageText.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        pageText.setGravity(Gravity.CENTER);
        bar.addView(pageText, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(4), dp(8), 0);

        TextView prev = tool("‹", v -> goDelta(-1));
        prev.setTextSize(24);
        prev.setMinWidth(dp(48));

        slider = new SeekBar(this);
        slider.setProgressTintList(ColorStateList.valueOf(0xFF7C8CFF));
        slider.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF3A3F4A));
        slider.setThumbTintList(ColorStateList.valueOf(0xFF7C8CFF));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || pages.isEmpty()) return;
                updatingSlider = true;
                setCurrentPage(progress);
                updatingSlider = false;
                updatePageText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        TextView next = tool("›", v -> goDelta(+1));
        next.setTextSize(24);
        next.setMinWidth(dp(48));

        row.addView(prev, new LinearLayout.LayoutParams(WRAP, WRAP));
        row.addView(slider, new LinearLayout.LayoutParams(0, WRAP, 1));
        row.addView(next, new LinearLayout.LayoutParams(WRAP, WRAP));
        bar.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        return bar;
    }

    private void buildNavRail() {
        navRail = new LinearLayout(this);
        navRail.setOrientation(LinearLayout.VERTICAL);
        navRail.setGravity(Gravity.CENTER);
        navRail.setPadding(dp(3), dp(6), dp(3), dp(6));
        navRail.setBackground(Ui.rounded(0x99000000, 0, 14, this));
        navRail.setVisibility(View.GONE);

        TextView top = tool("⏶", v -> setCurrentPage(0));
        top.setTextSize(15);
        top.setMinWidth(dp(32));
        top.setMinHeight(dp(32));
        navRail.addView(top, new LinearLayout.LayoutParams(dp(34), dp(34)));

        navSlider = new SeekBar(this);
        navSlider.setProgressTintList(ColorStateList.valueOf(0xFF7C8CFF));
        navSlider.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF3A3F4A));
        navSlider.setThumbTintList(ColorStateList.valueOf(0xFF7C8CFF));
        navSlider.setThumbOffset(dp(8));
        navSlider.setPadding(0, dp(6), 0, dp(6));
        navSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || pages.isEmpty()) return;
                updatingSlider = true;
                setCurrentPage(progress);
                updatingSlider = false;
                updatePageText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        navRail.addView(navSlider, new LinearLayout.LayoutParams(dp(32), 0, 1));

        TextView bottom = tool("⏷", v -> setCurrentPage(pages.isEmpty() ? 0 : pages.size() - 1));
        bottom.setTextSize(15);
        bottom.setMinWidth(dp(32));
        bottom.setMinHeight(dp(32));
        navRail.addView(bottom, new LinearLayout.LayoutParams(dp(34), dp(34)));
    }

    private void applyNavPosition() {
        if (navRail == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) navRail.getLayoutParams();
        lp.gravity = prefs.leftScrollbar()
                ? (Gravity.LEFT | Gravity.CENTER_VERTICAL)
                : (Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        navRail.setLayoutParams(lp);
    }

    private void updateNavSlider(int pos) {
        if (navSlider == null) return;
        if (!updatingSlider) {
            navSlider.setProgress(pos);
        }
    }

    private View buildLoading() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.BLACK);

        ProgressBar bar = new ProgressBar(this);
        bar.getIndeterminateDrawable().setColorFilter(0xFF7C8CFF, PorterDuff.Mode.SRC_IN);
        layout.addView(bar);

        TextView text = new TextView(this);
        text.setText("Menyiapkan halaman…");
        text.setTextColor(Color.LTGRAY);
        text.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
        lp.topMargin = dp(16);
        layout.addView(text, lp);
        return layout;
    }

    private void loadAsync() {
        new Thread(() -> {
            try {
                Uri u = Uri.parse(uri);
                if ("file".equalsIgnoreCase(u.getScheme())) {
                    loadDirect(new File(u.getPath()));
                } else {
                    File cbz = new File(CacheManager.zipDir(this), "manga.cbz");
                    copyUri(u, cbz);
                    PageSet ps = extractPages(cbz, CacheManager.pagesDir(this));
                    runOnUiThread(() -> onLoaded(ps));
                }
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Gagal membuka file.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void loadDirect(File src) {
        try {
            if (src.isDirectory()) {
                PageSet ps = new PageSet();
                ps.files.addAll(listImages(src));
                runOnUiThread(() -> onLoaded(ps));
            } else if (isArchiveName(src.getName())) {
                PageSet ps = extractPages(src, CacheManager.pagesDir(this));
                runOnUiThread(() -> onLoaded(ps));
            } else if (hasImageExt(src.getName())) {
                PageSet ps = new PageSet();
                ps.files.add(src);
                runOnUiThread(() -> onLoaded(ps));
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Format tidak didukung.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        } catch (Exception ex) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Gagal membuka file.", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }

    private static List<File> listImages(File dir) {
        List<File> out = new ArrayList<>();
        File[] all = dir.listFiles();
        if (all != null) {
            for (File f : all) {
                if (f.isFile() && hasImageExt(f.getName())) {
                    out.add(f);
                }
            }
        }
        Collections.sort(out, (a, b) -> naturalCompare(a.getName(), b.getName()));
        return out;
    }

    private static boolean isArchiveName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".cbz") || lower.endsWith(".zip");
    }

    private void onLoaded(PageSet ps) {
        if (ps.files.isEmpty()) {
            Toast.makeText(this, "Tidak ada gambar di dalam arsip.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        pages = ps.files;
        if (prefs.encoding() == 2) {
            for (int i = 0; i < ps.labels.size(); i++) {
                String l = ps.labels.get(i);
                if (l != null) {
                    ps.labels.set(i, new String(l.getBytes(StandardCharsets.ISO_8859_1),
                            StandardCharsets.UTF_8));
                }
            }
        }
        buildChapters(ps.labels);
        root.removeView(loading);
        slider.setMax(pages.size() - 1);
        if (navSlider != null) {
            navSlider.setMax(pages.size() - 1);
        }
        setupPager(restoreStart(), false);
        if (navRail != null && uiVisible) {
            navRail.setVisibility(View.VISIBLE);
        }

        int start = currentStartPage();
        File thumb = Thumbs.ensure(this, pages.get(start), uri + "_" + start);
        db.addHistory(uri, name, start, thumb != null ? thumb.getPath() : null);
        prefs.addRecent(uri, name, start);
        prefs.setLastComic(uri, name, start);
        updateBookmarkButton();
    }

    private int restoreStart() {
        int extra = getIntent().getIntExtra(EXTRA_PAGE, -1);
        if (extra >= 0) return extra;
        if (prefs.resumeEnabled()) {
            int r = db.getResume(uri);
            if (r >= 0) return r;
        }
        if (uri != null && uri.equals(prefs.lastUri())) {
            return prefs.lastPage();
        }
        return 0;
    }

    private void setupPager(int startPage, boolean animate) {
        lastMode = layoutMode();
        boolean cont = prefs.scroll() == 2;
        if (cont) {
            if (contList == null) {
                contLm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
                contList = new RecyclerView(this);
                contList.setLayoutManager(contLm);
                contList.setItemAnimator(null);
                contList.addItemDecoration(new ContSpacingDecoration());
                contList.addOnScrollListener(contScrollListener);
                pageHost.addView(contList, new FrameLayout.LayoutParams(MATCH, MATCH));
            } else if (contList.getParent() == null) {
                pageHost.addView(contList, new FrameLayout.LayoutParams(MATCH, MATCH));
            }
            if (pager.getParent() != null) {
                pageHost.removeView(pager);
            }
            contAdapter = new ContinuousAdapter();
            contList.setAdapter(contAdapter);
            if (!pages.isEmpty()) {
                contLm.scrollToPositionWithOffset(clampPage(startPage), 0);
            }
        } else {
            if (pager.getParent() == null) {
                pageHost.addView(pager, new FrameLayout.LayoutParams(MATCH, MATCH));
            }
            if (contList != null && contList.getParent() != null) {
                pageHost.removeView(contList);
            }
            pager.setOrientation(prefs.scroll() == 1 ? ViewPager2.ORIENTATION_VERTICAL
                    : ViewPager2.ORIENTATION_HORIZONTAL);
            pager.setLayoutDirection(prefs.rtl() ? View.LAYOUT_DIRECTION_RTL
                    : View.LAYOUT_DIRECTION_LTR);
            applySpacing();
            adapter = new ReaderAdapter();
            pager.setAdapter(adapter);
            if (!pages.isEmpty()) {
                int item = toItem(startPage);
                pager.setCurrentItem(item, animate);
            }
        }
        updatePageText();
        updateBookmarkButton();
    }

    private int clampPage(int page) {
        return Math.max(0, Math.min(page, pages.size() - 1));
    }

    private int toItem(int startPage) {
        int last = pages.size() - 1;
        int item = modeDual() ? Math.max(0, Math.min(startPage, last)) / 2 : startPage;
        return Math.max(0, Math.min(item, itemCount() - 1));
    }

    private void applyModeChange() {
        if (pages.isEmpty()) return;
        int keep = currentStartPage();
        setupPager(keep, false);
        showUi();
    }

    private void applySpacing() {
        if (pageHost == null) return;
        int gap = spacingPx();
        boolean showDiv = prefs.scrollDivider() && prefs.scroll() == 1 && prefs.spacing() > 0;
        if (pager != null) {
            pager.setClipToPadding(!showDiv);
            pager.setBackgroundColor(showDiv ? 0xFF4A5462 : Color.BLACK);
            if (prefs.scroll() == 1) {
                pager.setPadding(0, gap, 0, gap);
            } else {
                pager.setPadding(gap, 0, gap, 0);
            }
        }
        if (contList != null) {
            contList.invalidate();
        }
    }

    private int spacingPx() {
        if (prefs.spacing() <= 0) return 0;
        float p = prefs.spacing();
        if (pageHost == null || pageHost.getWidth() == 0) return 0;
        if (prefs.scroll() == 1) {
            return (int) (pageHost.getHeight() * p / 100f);
        }
        return (int) (pageHost.getWidth() * p / 100f);
    }

    private int layoutMode() {
        if (prefs.scroll() == 1) return 1;
        int m = prefs.pageLayout();
        if (m != 0) return m;
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        return (land && getResources().getConfiguration().screenWidthDp >= 600) ? 2 : 1;
    }

    private boolean modeDual() {
        return layoutMode() == 2;
    }

    private int itemCount() {
        return modeDual() ? (pages.size() + 1) / 2 : pages.size();
    }

    private int itemStartPage(int item) {
        return modeDual() ? item * 2 : item;
    }

    private int currentStartPage() {
        if (pages.isEmpty()) return 0;
        if (contList != null && contList.getParent() != null) {
            int pos = contLm.findFirstVisibleItemPosition();
            return Math.max(0, pos);
        }
        int item = pager.getCurrentItem();
        if (item < 0) return 0;
        return itemStartPage(item);
    }

    private void updatePageText() {
        if (pages.isEmpty()) return;
        boolean cont = contList != null && contList.getParent() != null;
        if (!cont && pager.getCurrentItem() < 0) {
            pageText.setText("Loading...");
            return;
        }
        int start = currentStartPage();
        int n = pages.size();
        if (modeDual()) {
            int end = Math.min(start + 1, n - 1);
            pageText.setText((start + 1) + "-" + (end + 1) + " / " + n);
        } else {
            pageText.setText((start + 1) + " / " + n);
        }
        subText.setText(pages.get(start).getName());
    }

    private void saveProgress(int start) {
        prefs.setLastComic(uri, name, start);
        if (prefs.resumeEnabled()) {
            db.setResume(uri, start);
        }
    }

    private void goDelta(int delta) {
        if (pages.isEmpty()) return;
        boolean cont = contList != null && contList.getParent() != null;
        int step = cont ? 1 : (modeDual() ? 2 : 1);
        setCurrentPage(currentStartPage() + delta * step);
    }

    private void setCurrentPage(int start) {
        if (pages.isEmpty()) return;
        start = clampPage(start);
        if (contList != null && contList.getParent() != null) {
            contLm.scrollToPositionWithOffset(start, 0);
            updatePageText();
            return;
        }
        pager.setCurrentItem(toItem(start), true);
    }

    private class ReaderAdapter extends RecyclerView.Adapter<ReaderAdapter.VH> {

        private class VH extends RecyclerView.ViewHolder {
            VH(View v) {
                super(v);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (modeDual()) {
                DualPageView v = new DualPageView(ReaderActivity.this);
                v.setLayoutParams(new RecyclerView.LayoutParams(MATCH, MATCH));
                v.setSingleTap(ReaderActivity.this::onPageTap);
                return new VH(v);
            }
            ZoomableImageView iv = new ZoomableImageView(ReaderActivity.this);
            iv.setLayoutParams(new RecyclerView.LayoutParams(MATCH, MATCH));
            iv.setBackgroundColor(Color.BLACK);
            iv.setSingleTapListener(ReaderActivity.this::onPageTap);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            int start = itemStartPage(position);
            int fit = prefs.fitMode();
            ColorMatrixColorFilter filter = ImageAdjuster.filter(
                    prefs.brightness(), prefs.contrast(), prefs.grayscale(), prefs.invert());
            if (modeDual()) {
                DualPageView d = (DualPageView) holder.itemView;
                Bitmap b1 = loadBitmap(pages.get(start));
                Bitmap b2 = (start + 1 < pages.size()) ? loadBitmap(pages.get(start + 1)) : null;
                d.setPages(b1, b2, prefs.rtl(), spacingPx(), prefs.dualDivider());
                d.applyFit(fit);
                d.applyFilter(filter);
            } else {
                ZoomableImageView iv = (ZoomableImageView) holder.itemView;
                iv.setImageBitmap(loadBitmap(pages.get(start)));
                iv.setFitMode(fit);
                iv.resetZoom();
                iv.setColorFilter(filter);
            }
        }

        @Override
        public int getItemCount() {
            return itemCount();
        }
    }

    private class ContinuousAdapter extends RecyclerView.Adapter<ContinuousAdapter.VH> {

        private class VH extends RecyclerView.ViewHolder {
            final ContinuousPageView v;

            VH(ContinuousPageView v) {
                super(v);
                this.v = v;
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ContinuousPageView v = new ContinuousPageView(ReaderActivity.this);
            v.setLayoutParams(new RecyclerView.LayoutParams(MATCH, WRAP));
            v.setSingleTapListener(ReaderActivity.this::onPageTap);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ContinuousPageView v = holder.v;
            v.setImageBitmap(loadBitmap(pages.get(position)));
            v.setColorFilter(ImageAdjuster.filter(
                    prefs.brightness(), prefs.contrast(), prefs.grayscale(), prefs.invert()));
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    private class ContSpacingDecoration extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint();

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            int extra = (prefs.scrollDivider() && prefs.spacing() > 0) ? dp(1) : 0;
            outRect.set(0, 0, 0, spacingPx() + extra);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            if (!prefs.scrollDivider() || prefs.spacing() <= 0) return;
            paint.setColor(0xFF4A5462);
            paint.setStrokeWidth(dp(1));
            int gap = spacingPx();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (parent.getChildAdapterPosition(child) == parent.getAdapter().getItemCount() - 1) {
                    continue;
                }
                float y = child.getBottom() + gap;
                c.drawLine(child.getLeft(), y, child.getRight(), y, paint);
            }
        }
    }

    private Bitmap loadBitmap(File file) {
        Bitmap b = cache.get(file);
        if (b != null) return b;
        b = Thumbs.decode(file, modeDual() ? 1600 : 2200);
        if (b != null) {
            if (prefs.cutMargin()) {
                b = cutMargins(b);
            }
            cache.put(file, b);
        }
        return b;
    }

    private Bitmap cutMargins(Bitmap b) {
        int w = b.getWidth();
        int h = b.getHeight();
        int top = marginEnd(b, 0, h, 1);
        int bottom = marginEnd(b, h - 1, -1, -1);
        if (top < dp(3) || bottom > h - 1 - dp(3)) return b;
        if (bottom - top + 1 < h * 0.3f) return b;
        if (bottom - top + 1 >= h - dp(4)) return b;
        Bitmap out = Bitmap.createBitmap(b, 0, top, w, bottom - top + 1);
        if (out != b) b.recycle();
        return out;
    }

    private static int marginEnd(Bitmap b, int fromY, int stopY, int stepY) {
        int w = b.getWidth();
        int[] row = new int[w];
        int darkLimit = Math.max(1, w / 50);
        for (int y = fromY; (stepY > 0 ? y < stopY : y > stopY); y += stepY) {
            b.getPixels(row, 0, w, 0, y, w, 1);
            int dark = 0;
            for (int x = 0; x < w; x += 4) {
                int c = row[x];
                if (((c >> 24) & 0xFF) > 200) {
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int bl = c & 0xFF;
                    if (r < 230 || g < 230 || bl < 230) dark++;
                }
            }
            if (dark >= darkLimit) return y;
        }
        return stopY;
    }

    private void applyFilterToViews() {
        ColorMatrixColorFilter filter = ImageAdjuster.filter(
                prefs.brightness(), prefs.contrast(), prefs.grayscale(), prefs.invert());
        if (contList != null && contList.getChildCount() > 0) {
            for (int i = 0; i < contList.getChildCount(); i++) {
                View v = contList.getChildAt(i);
                if (v instanceof ContinuousPageView) {
                    ((ContinuousPageView) v).setColorFilter(filter);
                }
            }
        }
        if (pager == null || pager.getChildCount() == 0) return;
        RecyclerView rv = (RecyclerView) pager.getChildAt(0);
        for (int i = 0; i < rv.getChildCount(); i++) {
            View v = rv.getChildAt(i);
            if (v instanceof DualPageView) {
                ((DualPageView) v).applyFilter(filter);
            } else if (v instanceof ZoomableImageView) {
                ((ZoomableImageView) v).setColorFilter(filter);
            }
        }
    }

    // ---------- toolbar actions ----------

    private void toggleCrop() {
        prefs.setCutMargin(!prefs.cutMargin());
        cache.evictAll();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (contAdapter != null) contAdapter.notifyDataSetChanged();
        updateCropButton();
    }

    private void updateCropButton() {
        if (cropBtn == null) return;
        cropBtn.setTextColor(prefs.cutMargin() ? 0xFF7C8CFF : Color.WHITE);
    }

    private int directionIndex() {
        int s = prefs.scroll();
        return (prefs.rtl() ? 1 : 0) + (s == 1 ? 2 : (s == 2 ? 4 : 0));
    }

    private int layoutOptionIndex() {
        int m = prefs.pageLayout();
        return m == 0 ? 3 : (m == 2 ? 2 : 1);
    }

    private void showLayoutScrollingDialog() {
        String[] dirOpts = {"→  Kanan", "←  Kiri", "↓  Kanan+Vertikal",
                "↓  Kiri+Vertikal", "S  Kanan", "S  Kiri"};
        final int[] dir = {directionIndex()};
        final int[] lay = {layoutOptionIndex()};
        final List<TextView> dirCells = new ArrayList<>();
        final List<TextView> layCells = new ArrayList<>();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        layout.setPadding(pad, pad, pad, 0);

        TextView dirLabel = new TextView(this);
        dirLabel.setText("Arah Baca");
        dirLabel.setTextColor(Color.WHITE);
        dirLabel.setTextSize(14);
        dirLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        layout.addView(dirLabel);

        int cols = isLandscape() ? 6 : 3;
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        for (int i = 0; i < dirOpts.length; i++) {
            TextView cell = new TextView(this);
            cell.setText(dirOpts[i]);
            cell.setTextSize(13);
            cell.setGravity(Gravity.CENTER);
            cell.setSingleLine(true);
            boolean sel = i == dir[0];
            cell.setBackground(Ui.rounded(sel ? Ui.ACCENT : 0xFF232731, Ui.divider(this), 10, this));
            cell.setTextColor(sel ? 0xFF0B0E14 : Color.WHITE);
            final int idx = i;
            cell.setOnClickListener(v -> {
                dir[0] = idx;
                for (int k = 0; k < dirCells.size(); k++) {
                    boolean s = k == idx;
                    dirCells.get(k).setBackground(Ui.rounded(s ? Ui.ACCENT : 0xFF232731,
                            Ui.divider(this), 10, this));
                    dirCells.get(k).setTextColor(s ? 0xFF0B0E14 : Color.WHITE);
                }
            });
            dirCells.add(cell);
            GridLayout.LayoutParams gp = new GridLayout.LayoutParams();
            gp.width = 0;
            gp.height = dp(42);
            gp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            gp.setMargins(0, dp(6), dp(4), 0);
            grid.addView(cell, gp);
        }
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(MATCH, WRAP);
        glp.topMargin = dp(6);
        layout.addView(grid, glp);

        TextView layLabel = new TextView(this);
        layLabel.setText("Layout Halaman");
        layLabel.setTextColor(Color.WHITE);
        layLabel.setTextSize(14);
        layLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH, WRAP);
        llp.topMargin = dp(14);
        layout.addView(layLabel, llp);

        String[] layOpts = {"Off", "Single", "Dual", "Auto"};
        LinearLayout layRow = new LinearLayout(this);
        layRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < layOpts.length; i++) {
            TextView cell = new TextView(this);
            cell.setText(layOpts[i]);
            cell.setTextSize(13);
            cell.setGravity(Gravity.CENTER);
            cell.setSingleLine(true);
            boolean sel = i == lay[0];
            cell.setBackground(Ui.rounded(sel ? Ui.ACCENT : 0xFF232731, Ui.divider(this), 10, this));
            cell.setTextColor(sel ? 0xFF0B0E14 : Color.WHITE);
            final int idx = i;
            cell.setOnClickListener(v -> {
                lay[0] = idx;
                for (int k = 0; k < layCells.size(); k++) {
                    boolean s = k == idx;
                    layCells.get(k).setBackground(Ui.rounded(s ? Ui.ACCENT : 0xFF232731,
                            Ui.divider(this), 10, this));
                    layCells.get(k).setTextColor(s ? 0xFF0B0E14 : Color.WHITE);
                }
            });
            layCells.add(cell);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, dp(40), 1);
            if (i > 0) {
                clp.leftMargin = dp(4);
            }
            layRow.addView(cell, clp);
        }
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        rlp.topMargin = dp(6);
        layout.addView(layRow, rlp);

        showDialog(new AlertDialog.Builder(this)
                .setTitle("Tata Letak & Scrolling")
                .setView(layout)
                .setPositiveButton("OK", (d, w) -> {
                    prefs.setRtl((dir[0] % 2) == 1);
                    prefs.setScroll(dir[0] >= 4 ? 2 : (dir[0] >= 2 ? 1 : 0));
                    prefs.setPageLayout(lay[0] == 2 ? 2 : (lay[0] == 3 ? 0 : 1));
                    applyModeChange();
                })
                .setNegativeButton("Batal", null));
    }

    private void showSizeDialog() {
        String[] opts = {"Fit Screen", "Fit Width", "Fit Height", "Original Size"};
        showChoice("Image Scaling", opts, prefs.fitMode(), which -> {
            prefs.setFitMode(which);
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    private void showRotateDialog() {
        String[] opts = {"Ikuti Sistem", "Potret", "Lanskap"};
        showChoice("Orientasi", opts, prefs.orientationLock(), which -> {
            prefs.setOrientationLock(which);
            applyOrientationLock();
        });
    }

    private void showSpacingDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, 0);

        TextView label = new TextView(this);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        label.setGravity(Gravity.CENTER);
        layout.addView(label, new LinearLayout.LayoutParams(MATCH, WRAP));

        SeekBar bar = new SeekBar(this);
        bar.setMax(200);
        bar.setProgress(prefs.spacing() + 100);
        bar.setProgressTintList(ColorStateList.valueOf(0xFF7C8CFF));
        bar.setThumbTintList(ColorStateList.valueOf(0xFF7C8CFF));
        layout.addView(bar, new LinearLayout.LayoutParams(MATCH, WRAP));
        label.setText((prefs.spacing()) + "%");

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText((progress - 100) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        showDialog(new AlertDialog.Builder(this)
                .setTitle("Page Spacing")
                .setView(layout)
                .setPositiveButton("OK", (d, w) -> {
                    prefs.setSpacing(bar.getProgress() - 100);
                    applySpacing();
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .setNegativeButton("Batal", null));
    }

    private void showAdjustDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        layout.setPadding(pad, pad, pad, 0);

        final int[] brightness = {(int) (prefs.brightness() * 100)};
        final int[] contrast = {(int) (prefs.contrast() * 100)};
        final boolean[] gray = {prefs.grayscale()};
        final boolean[] inv = {prefs.invert()};

        layout.addView(adjustRow("Kecerahan", brightness[0], 50, 200, layout,
                value -> brightness[0] = value));
        layout.addView(adjustRow("Kontras", contrast[0], 50, 200, layout,
                value -> contrast[0] = value));

        Switch graySw = new Switch(this);
        graySw.setChecked(gray[0]);
        graySw.setText("Grayscale");
        graySw.setTextColor(Color.WHITE);
        graySw.setOnCheckedChangeListener((b, c) -> gray[0] = c);
        layout.addView(graySw, topLp(dp(16)));

        Switch invSw = new Switch(this);
        invSw.setChecked(inv[0]);
        invSw.setText("Invert");
        invSw.setTextColor(Color.WHITE);
        invSw.setOnCheckedChangeListener((b, c) -> inv[0] = c);
        layout.addView(invSw, topLp(dp(8)));

        showDialog(new AlertDialog.Builder(this)
                .setTitle("Image Adjustment")
                .setView(layout)
                .setPositiveButton("OK", (d, w) -> {
                    prefs.setAdjust(brightness[0] / 100f, contrast[0] / 100f, gray[0], inv[0]);
                    applyFilterToViews();
                })
                .setNeutralButton("Reset", (d, w) -> {
                    prefs.setAdjust(1f, 1f, false, false);
                    applyFilterToViews();
                })
                .setNegativeButton("Batal", null));
    }

    private View adjustRow(String title, int initial, int min, int max, ViewGroup parent,
                           Consumer<Integer> onValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(this);
        label.setText(title + "  " + initial);
        label.setTextColor(Color.WHITE);
        label.setTextSize(14);
        row.addView(label);

        SeekBar bar = new SeekBar(this);
        bar.setMax(max - min);
        bar.setProgress(initial - min);
        bar.setProgressTintList(ColorStateList.valueOf(0xFF7C8CFF));
        bar.setThumbTintList(ColorStateList.valueOf(0xFF7C8CFF));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                label.setText(title + "  " + (progress + min));
                onValue.accept(progress + min);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        row.addView(bar);
        return row;
    }

    private void toggleBookmark() {
        if (pages.isEmpty()) return;
        int page = currentStartPage();
        File thumb = Thumbs.ensure(this, pages.get(page), uri + "_" + page);
        db.toggleBookmark(uri, name, page, thumb != null ? thumb.getPath() : null);
        Toast.makeText(this, db.isBookmarked(uri, page) ? "Bookmark ditambahkan"
                : "Bookmark dihapus", Toast.LENGTH_SHORT).show();
        updateBookmarkButton();
    }

    private void updateBookmarkButton() {
        if (bookmarkBtn == null) return;
        boolean on = !pages.isEmpty() && db.isBookmarked(uri, currentStartPage());
        bookmarkBtn.setText(on ? "★" : "☆");
        bookmarkBtn.setTextColor(on ? 0xFF7C8CFF : Color.WHITE);
    }

    private void toggleFullscreen() {
        prefs.setFullscreen(!prefs.fullscreen());
        if (prefs.fullscreen()) {
            hideSystemUi();
        } else {
            showSystemUi();
        }
    }

    private void showChoice(String title, String[] options, int checked, Consumer<Integer> onSelect) {
        showDialog(new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, checked,
                        (d, which) -> {
                            onSelect.accept(which);
                            d.dismiss();
                        })
                .setNegativeButton("Batal", null));
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

    // ---------- UI visibility ----------

    private void onPageTap(float x, float y) {
        if (pages.isEmpty()) return;
        float w = currentViewportWidth();
        if (x < w * 0.22f) {
            goDelta(prefs.rtl() ? 1 : -1);
        } else if (x > w * 0.78f) {
            goDelta(prefs.rtl() ? -1 : 1);
        } else {
            toggleUi();
        }
    }

    private float currentViewportWidth() {
        if (contList != null && contList.getParent() != null && contList.getWidth() > 0) {
            return contList.getWidth();
        }
        return pager.getWidth();
    }

    private void toggleUi() {
        uiVisible = !uiVisible;
        int v = uiVisible ? View.VISIBLE : View.GONE;
        topBar.setVisibility(v);
        toolBarScroll.setVisibility(v);
        bottomBar.setVisibility(v);
        if (navRail != null) {
            navRail.setVisibility(pages.isEmpty() ? View.GONE : v);
        }
        if (uiVisible) {
            showSystemUi();
        } else {
            hideSystemUi();
        }
    }

    private void showUi() {
        uiVisible = true;
        topBar.setVisibility(View.VISIBLE);
        toolBarScroll.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        if (navRail != null) {
            navRail.setVisibility(pages.isEmpty() ? View.GONE : View.VISIBLE);
        }
        showSystemUi();
    }

    private void hideSystemUi() {
        if (prefs.fullscreen()) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } else {
            showSystemUi();
        }
    }

    private void showSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    // ---------- lifecycle / config ----------

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        applyPunchHole();
        if (pageHost == null) return;
        if (layoutMode() != lastMode) {
            applyModeChange();
        } else {
            applySpacing();
            if (pager != null && pager.getParent() != null) {
                pager.setOrientation(prefs.scroll() == 1 ? ViewPager2.ORIENTATION_VERTICAL
                        : ViewPager2.ORIENTATION_HORIZONTAL);
            }
            hideSystemUi();
        }
    }

    private void applyOrientationLock() {
        switch (prefs.orientationLock()) {
            case 1:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                break;
            case 2:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    private void applyPunchHole() {
        if (Build.VERSION.SDK_INT >= 28) {
            boolean enable = isLandscape() ? prefs.punchHoleLandscape() : prefs.punchHolePortrait();
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = enable
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            getWindow().setAttributes(lp);
            getWindow().getDecorView().setFitsSystemWindows(false);
        }
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    // ---------- archive helpers ----------

    private void copyUri(Uri uri, File dest) throws IOException {
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }

    private static PageSet extractPages(File cbz, File pagesDir) throws IOException {
        if (pagesDir.exists()) {
            File[] old = pagesDir.listFiles();
            if (old != null) {
                for (File f : old) {
                    f.delete();
                }
            }
        } else {
            pagesDir.mkdirs();
        }

        List<String> names = new ArrayList<>();
        try (ZipFile zf = new ZipFile(cbz)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || entryName.contains("__MACOSX") || !hasImageExt(entryName)) {
                    continue;
                }
                names.add(entryName);
            }
        }
        Collections.sort(names, ReaderActivity::naturalCompare);

        PageSet ps = new PageSet();
        try (ZipFile zf = new ZipFile(cbz)) {
            for (int i = 0; i < names.size(); i++) {
                ZipEntry entry = zf.getEntry(names.get(i));
                File file = new File(pagesDir,
                        String.format(Locale.ROOT, "page_%05d%s", i, extOf(names.get(i))));
                try (InputStream in = zf.getInputStream(entry);
                     FileOutputStream o = new FileOutputStream(file)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        o.write(buf, 0, n);
                    }
                }
                ps.files.add(file);
                ps.labels.add(folderOf(names.get(i)));
            }
        }
        return ps;
    }

    private static String folderOf(String entryName) {
        int i = entryName.indexOf('/');
        if (i > 0 && i < entryName.length() - 1) {
            return entryName.substring(0, i);
        }
        return null;
    }

    private static boolean hasImageExt(String name) {
        String ext = extOf(name);
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                || ext.equals(".webp") || ext.equals(".gif") || ext.equals(".bmp");
    }

    private static String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i).toLowerCase(Locale.ROOT) : ".jpg";
    }

    private static int naturalCompare(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int si = i;
                int sj = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                String na = a.substring(si, i).replaceFirst("^0+(?=\\d)", "");
                String nb = b.substring(sj, j).replaceFirst("^0+(?=\\d)", "");
                int cmp = Integer.compare(na.length(), nb.length());
                if (cmp == 0) {
                    cmp = na.compareTo(nb);
                }
                if (cmp != 0) {
                    return cmp;
                }
            } else {
                if (ca != cb) {
                    return Character.compare(ca, cb);
                }
                i++;
                j++;
            }
        }
        return Integer.compare(a.length(), b.length());
    }

    private static class Chapter {
        final String name;
        final int start;
        final int count;

        Chapter(String name, int start, int count) {
            this.name = name;
            this.start = start;
            this.count = count;
        }
    }

    static final class PageSet {
        final List<File> files = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
    }

    private void buildChapters(List<String> labels) {
        chapters.clear();
        String cur = null;
        int start = 0;
        boolean anyFolder = false;
        for (int i = 0; i < labels.size(); i++) {
            String l = labels.get(i);
            if (l != null) anyFolder = true;
            if (l == null) l = name;
            if (cur == null) {
                cur = l;
                start = i;
            } else if (!cur.equals(l)) {
                chapters.add(new Chapter(cur, start, i - start));
                cur = l;
                start = i;
            }
        }
        if (cur != null) {
            chapters.add(new Chapter(cur, start, labels.size() - start));
        }
        if (chapterBtn != null) {
            chapterBtn.setVisibility(anyFolder && chapters.size() >= 2 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!pages.isEmpty()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                goDelta(1);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                goDelta(-1);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void toggleDrawer() {
        if (drawerWrap != null) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    private void openDrawer() {
        if (chapters.isEmpty()) return;
        int panelW = Math.min(dp(280), (int) (root.getWidth() * 0.8f));
        drawerWrap = new FrameLayout(this);
        drawerWrap.setLayoutParams(new FrameLayout.LayoutParams(MATCH, MATCH));

        View scrim = new View(this);
        scrim.setBackgroundColor(0x66000000);
        scrim.setOnClickListener(v -> closeDrawer());
        drawerWrap.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xEE1B1E24);
        panel.setTranslationX(panelW);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(panelW, MATCH, Gravity.RIGHT);
        drawerWrap.addView(panel, plp);
        panel.animate().translationX(0).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();

        TextView header = new TextView(this);
        header.setText("Daftar Bab");
        header.setTextColor(Color.WHITE);
        header.setTextSize(16);
        header.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.setPadding(dp(16), dp(18), dp(16), dp(10));
        panel.addView(header, new LinearLayout.LayoutParams(MATCH, WRAP));

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(MATCH, 0, 1));

        for (Chapter c : chapters) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackgroundColor(Color.TRANSPARENT);
            row.setOnClickListener(v -> {
                setCurrentPage(c.start);
                closeDrawer();
            });
            TextView tv = new TextView(this);
            tv.setText(c.name);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            row.addView(tv, new LinearLayout.LayoutParams(0, WRAP, 1));
            TextView cnt = new TextView(this);
            cnt.setText(String.valueOf(c.count));
            cnt.setTextColor(Color.LTGRAY);
            cnt.setTextSize(12);
            row.addView(cnt, new LinearLayout.LayoutParams(WRAP, WRAP));
            list.addView(row, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        root.addView(drawerWrap);
    }

    private void closeDrawer() {
        if (drawerWrap == null) return;
        FrameLayout wrap = drawerWrap;
        drawerWrap = null;
        View panel = wrap.getChildAt(1);
        panel.animate().translationX(panel.getWidth()).setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> root.removeView(wrap)).start();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private LinearLayout.LayoutParams topLp(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = top;
        return lp;
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int navBarHeight() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }
}
