package com.kendi.tarayicim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BrowserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "kendibrowser.db";
    private static final int DB_VERSION = 1;

    public static class HistoryItem {
        public int id;
        public String url;
        public String title;
        public String timestamp;

        public HistoryItem(int id, String url, String title, String timestamp) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.timestamp = timestamp;
        }
    }

    public static class BookmarkItem {
        public int id;
        public String url;
        public String title;

        public BookmarkItem(int id, String url, String title) {
            this.id = id;
            this.url = url;
            this.title = title;
        }
    }

    public BrowserDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "url TEXT," +
                "title TEXT," +
                "timestamp TEXT)");
        db.execSQL("CREATE TABLE bookmarks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "url TEXT UNIQUE," +
                "title TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS history");
        db.execSQL("DROP TABLE IF EXISTS bookmarks");
        onCreate(db);
    }

    // GEÇMİŞ
    public void addHistoryItem(String url, String title) {
        if (url == null || url.isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("title", title != null && !title.isEmpty() ? title : url);
        values.put("timestamp", new SimpleDateFormat("dd MMM, HH:mm", new Locale("tr")).format(new Date()));
        db.insert("history", null, values);
        db.close();
    }

    public List<HistoryItem> getHistoryItems() {
        List<HistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, url, title, timestamp FROM history ORDER BY id DESC LIMIT 100", null);
        while (cursor.moveToNext()) {
            list.add(new HistoryItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)));
        }
        cursor.close();
        db.close();
        return list;
    }

    public void deleteHistoryItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("history", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM history");
        db.close();
    }

    // YER İMLERİ
    public boolean addBookmark(String url, String title) {
        if (url == null || url.isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("title", title != null && !title.isEmpty() ? title : url);
        long result = db.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result != -1;
    }

    public List<BookmarkItem> getBookmarkItems() {
        List<BookmarkItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, url, title FROM bookmarks ORDER BY id DESC", null);
        while (cursor.moveToNext()) {
            list.add(new BookmarkItem(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
        }
        cursor.close();
        db.close();
        return list;
    }

    public void deleteBookmark(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("bookmarks", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public boolean isBookmarked(String url) {
        if (url == null) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("bookmarks", new String[]{"id"}, "url=?", new String[]{url}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
}
