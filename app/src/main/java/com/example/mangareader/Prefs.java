package com.example.mangareader;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Prefs {
    private static final String FILE = "mangareader_prefs";
    private static Prefs instance;

    private final SharedPreferences sp;

    public static synchronized Prefs get(Context c) {
        if (instance == null) {
            instance = new Prefs(c.getApplicationContext());
        }
        return instance;
    }

    private Prefs(Context c) {
        sp = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor edit() {
        return sp.edit();
    }

    // theme: 0 system, 1 dark, 2 light
    public int theme() { return sp.getInt("theme", 0); }
    public void setTheme(int v) { edit().putInt("theme", v).apply(); }

    public boolean fullscreen() { return sp.getBoolean("fullscreen", true); }
    public void setFullscreen(boolean v) { edit().putBoolean("fullscreen", v).apply(); }

    // 0 auto, 1 single, 2 dual
    public int pageLayout() { return sp.getInt("page_layout", 0); }
    public void setPageLayout(int v) { edit().putInt("page_layout", v).apply(); }

    public boolean rtl() { return sp.getBoolean("rtl", true); }
    public void setRtl(boolean v) { edit().putBoolean("rtl", v).apply(); }

    // 0 horizontal, 1 vertical
    public int scroll() { return sp.getInt("scroll", 0); }
    public void setScroll(int v) { edit().putInt("scroll", v).apply(); }

    // percent, -50..50
    public int spacing() { return sp.getInt("spacing", 0); }
    public void setSpacing(int v) { edit().putInt("spacing", v).apply(); }

    public boolean dualDivider() { return sp.getBoolean("dual_divider", false); }
    public void setDualDivider(boolean v) { edit().putBoolean("dual_divider", v).apply(); }

    public boolean scrollDivider() { return sp.getBoolean("scroll_divider", false); }
    public void setScrollDivider(boolean v) { edit().putBoolean("scroll_divider", v).apply(); }

    public boolean punchHolePortrait() { return sp.getBoolean("punch_portrait", false); }
    public void setPunchHolePortrait(boolean v) { edit().putBoolean("punch_portrait", v).apply(); }

    public boolean punchHoleLandscape() { return sp.getBoolean("punch_landscape", false); }
    public void setPunchHoleLandscape(boolean v) { edit().putBoolean("punch_landscape", v).apply(); }

    // 0 fit screen, 1 fit width, 2 fit height, 3 original
    public int fitMode() { return sp.getInt("fit_mode", 0); }
    public void setFitMode(int v) { edit().putInt("fit_mode", v).apply(); }

    public float brightness() { return sp.getFloat("brightness", 1f); }
    public float contrast() { return sp.getFloat("contrast", 1f); }
    public boolean grayscale() { return sp.getBoolean("grayscale", false); }
    public boolean invert() { return sp.getBoolean("invert", false); }
    public void setAdjust(float brightness, float contrast, boolean grayscale, boolean invert) {
        edit().putFloat("brightness", brightness)
                .putFloat("contrast", contrast)
                .putBoolean("grayscale", grayscale)
                .putBoolean("invert", invert)
                .apply();
    }

    public boolean cutMargin() { return sp.getBoolean("cut_margin", false); }
    public void setCutMargin(boolean v) { edit().putBoolean("cut_margin", v).apply(); }

    // 0 system, 1 portrait, 2 landscape
    public int orientationLock() { return sp.getInt("orientation", 0); }
    public void setOrientationLock(int v) { edit().putInt("orientation", v).apply(); }

    public boolean fab() { return sp.getBoolean("fab", true); }
    public void setFab(boolean v) { edit().putBoolean("fab", v).apply(); }

    public boolean askedAllFiles() { return sp.getBoolean("asked_all_files", false); }
    public void setAskedAllFiles(boolean v) { edit().putBoolean("asked_all_files", v).apply(); }

    public boolean showStatusbar() { return sp.getBoolean("show_statusbar", false); }
    public void setShowStatusbar(boolean v) { edit().putBoolean("show_statusbar", v).apply(); }

    public boolean showFolderPath() { return sp.getBoolean("show_folder_path", true); }
    public void setShowFolderPath(boolean v) { edit().putBoolean("show_folder_path", v).apply(); }

    public boolean resumeEnabled() { return sp.getBoolean("resume_enabled", true); }
    public void setResumeEnabled(boolean v) { edit().putBoolean("resume_enabled", v).apply(); }

    public String lastUri() { return sp.getString("last_uri", null); }
    public String lastName() { return sp.getString("last_name", null); }
    public int lastPage() { return sp.getInt("last_page", 0); }
    public void setLastComic(String uri, String name, int page) {
        edit().putString("last_uri", uri)
                .putString("last_name", name)
                .putInt("last_page", page)
                .apply();
    }

    public String treeUri() { return sp.getString("tree_uri", null); }
    public String treeName() { return sp.getString("tree_name", null); }
    public void setTree(String uri, String name) {
        edit().putString("tree_uri", uri).putString("tree_name", name).apply();
    }

    public static class Recent {
        public String uri;
        public String name;
        public int page;
        public long time;
    }

    public List<Recent> recent() {
        List<Recent> out = new ArrayList<>();
        String raw = sp.getString("recent", null);
        if (raw == null) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Recent r = new Recent();
                r.uri = o.getString("uri");
                r.name = o.getString("name");
                r.page = o.optInt("page", 0);
                r.time = o.optLong("time", 0);
                out.add(r);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public void addRecent(String uri, String name, int page) {
        List<Recent> list = recent();
        list.removeIf(r -> r.uri.equals(uri));
        Recent r = new Recent();
        r.uri = uri;
        r.name = name;
        r.page = page;
        r.time = System.currentTimeMillis();
        list.add(0, r);
        if (list.size() > 20) {
            list = list.subList(0, 20);
        }
        try {
            JSONArray arr = new JSONArray();
            for (Recent x : list) {
                JSONObject o = new JSONObject();
                o.put("uri", x.uri);
                o.put("name", x.name);
                o.put("page", x.page);
                o.put("time", x.time);
                arr.put(o);
            }
            edit().putString("recent", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void clearRecent() {
        edit().remove("recent").apply();
    }

    public void resetAll() {
        edit().clear().apply();
    }
}
