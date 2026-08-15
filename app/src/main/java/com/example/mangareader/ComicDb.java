package com.example.mangareader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class ComicDb {
    private static final int VERSION = 2;
    private static ComicDb instance;

    private final SQLiteOpenHelper helper;

    public static synchronized ComicDb get(Context c) {
        if (instance == null) {
            instance = new ComicDb(c.getApplicationContext());
        }
        return instance;
    }

    private ComicDb(Context c) {
        helper = new SQLiteOpenHelper(c, "comics.db", null, VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE history("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uri TEXT UNIQUE, name TEXT, page INTEGER, thumb TEXT, opened INTEGER)");
                db.execSQL("CREATE TABLE bookmarks("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "uri TEXT, name TEXT, page INTEGER, thumb TEXT, created INTEGER)");
                db.execSQL("CREATE TABLE resume("
                        + "uri TEXT PRIMARY KEY, page INTEGER, updated INTEGER)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.execSQL("DROP TABLE IF EXISTS history");
                db.execSQL("DROP TABLE IF EXISTS bookmarks");
                db.execSQL("DROP TABLE IF EXISTS resume");
                onCreate(db);
            }
        };
    }

    public void addHistory(String uri, String name, int page, String thumb) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("uri", uri);
        v.put("name", name);
        v.put("page", page);
        v.put("thumb", thumb);
        v.put("opened", System.currentTimeMillis());
        db.insertWithOnConflict("history", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static class HistoryRow {
        public String uri, name, thumb;
        public int page;
        public long opened;
    }

    public List<HistoryRow> history() {
        List<HistoryRow> out = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query("history",
                null, null, null, null, null, "opened DESC");
        while (c.moveToNext()) {
            HistoryRow r = new HistoryRow();
            r.uri = c.getString(c.getColumnIndexOrThrow("uri"));
            r.name = c.getString(c.getColumnIndexOrThrow("name"));
            r.page = c.getInt(c.getColumnIndexOrThrow("page"));
            r.thumb = c.getString(c.getColumnIndexOrThrow("thumb"));
            r.opened = c.getLong(c.getColumnIndexOrThrow("opened"));
            out.add(r);
        }
        c.close();
        return out;
    }

    public void removeHistory(String uri) {
        helper.getWritableDatabase().delete("history", "uri=?", new String[]{uri});
    }

    public void clearHistory() {
        helper.getWritableDatabase().delete("history", null, null);
    }

    public void toggleBookmark(String uri, String name, int page, String thumb) {
        SQLiteDatabase db = helper.getWritableDatabase();
        if (isBookmarked(uri, page)) {
            db.delete("bookmarks", "uri=? AND page=?", new String[]{uri, String.valueOf(page)});
            return;
        }
        ContentValues v = new ContentValues();
        v.put("uri", uri);
        v.put("name", name);
        v.put("page", page);
        v.put("thumb", thumb);
        v.put("created", System.currentTimeMillis());
        db.insert("bookmarks", null, v);
    }

    public boolean isBookmarked(String uri, int page) {
        Cursor c = helper.getReadableDatabase().query("bookmarks",
                null, "uri=? AND page=?", new String[]{uri, String.valueOf(page)},
                null, null, null);
        boolean ok = c.moveToFirst();
        c.close();
        return ok;
    }

    public static class BookmarkRow {
        public String uri, name, thumb;
        public int page;
        public long created;
    }

    public List<BookmarkRow> bookmarks() {
        List<BookmarkRow> out = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().query("bookmarks",
                null, null, null, null, null, "created DESC");
        while (c.moveToNext()) {
            BookmarkRow r = new BookmarkRow();
            r.uri = c.getString(c.getColumnIndexOrThrow("uri"));
            r.name = c.getString(c.getColumnIndexOrThrow("name"));
            r.page = c.getInt(c.getColumnIndexOrThrow("page"));
            r.thumb = c.getString(c.getColumnIndexOrThrow("thumb"));
            r.created = c.getLong(c.getColumnIndexOrThrow("created"));
            out.add(r);
        }
        c.close();
        return out;
    }

    public void clearBookmarks() {
        helper.getWritableDatabase().delete("bookmarks", null, null);
    }

    public void setResume(String uri, int page) {
        ContentValues v = new ContentValues();
        v.put("uri", uri);
        v.put("page", page);
        v.put("updated", System.currentTimeMillis());
        helper.getWritableDatabase().insertWithOnConflict("resume", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getResume(String uri) {
        Cursor c = helper.getReadableDatabase().query("resume", null,
                "uri=?", new String[]{uri}, null, null, null);
        int page = -1;
        if (c.moveToFirst()) {
            page = c.getInt(c.getColumnIndexOrThrow("page"));
        }
        c.close();
        return page;
    }

    public void clearResume() {
        helper.getWritableDatabase().delete("resume", null, null);
    }
}
