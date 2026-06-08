package com.kendi.tarayicim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class BrowserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "quantum_browser_pro.db";
    private static final int DB_VERSION = 4; // Versiyon 6 yeni özellik için yükseltildi

    public BrowserDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, title TEXT, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT UNIQUE, title TEXT)");
        // Kasa (Vault) tablosu eklendi
        db.execSQL("CREATE TABLE vault (id INTEGER PRIMARY KEY AUTOINCREMENT, domain TEXT UNIQUE, username TEXT, password TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS history");
        db.execSQL("DROP TABLE IF EXISTS bookmarks");
        db.execSQL("DROP TABLE IF EXISTS vault");
        onCreate(db);
    }

    public void addHistoryItem(String url, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("title", title != null ? title : url);
        db.insert("history", null, values);
        db.close();
    }

    public List<String> getHistoryList() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT url FROM history ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public boolean addBookmark(String url, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("url", url);
        values.put("title", title != null ? title : url);
        long result = db.insertWithOnConflict("bookmarks", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result != -1;
    }
}
