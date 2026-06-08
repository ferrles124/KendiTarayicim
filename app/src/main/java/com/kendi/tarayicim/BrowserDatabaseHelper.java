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
    private static final int DB_VERSION = 3;

    // Tablo ve Sütun İsimleri (Sabit Tanımlamalar)
    private static final String TABLE_HISTORY = "history";
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public BrowserDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Geçmiş tablosu tasarımı (Zaman damgası entegreli)
        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_URL + " TEXT, "
                + COLUMN_TITLE + " TEXT, "
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        
        // Yer imleri tablosu tasarımı
        String createBookmarksTable = "CREATE TABLE " + TABLE_BOOKMARKS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_URL + " TEXT UNIQUE, " // Aynı site iki kez eklenemesin
                + COLUMN_TITLE + " TEXT)";

        db.execSQL(createHistoryTable);
        db.execSQL(createBookmarksTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        onCreate(db);
    }

    // --- GEÇMİŞ MODÜLÜ METOTLARI ---

    public void addHistoryItem(String url, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Mükerrer kayıt kontrolü: Son eklenen kayıt aynı URL ise tekrar ekleme
            String lastUrl = getLastHistoryUrl(db);
            if (lastUrl == null || !lastUrl.equals(url)) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_URL, url);
                values.put(COLUMN_TITLE, title != null ? title : url);
                db.insert(TABLE_HISTORY, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private String getLastHistoryUrl(SQLiteDatabase db) {
        String url = null;
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_URL + " FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_ID + " DESC LIMIT 1", null);
        if (cursor.moveToFirst()) {
            url = cursor.getString(0);
        }
        cursor.close();
        return url;
    }

    public List<String> getHistoryList() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_URL + " FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_ID + " DESC", null);
        
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // --- YER İMLERİ MODÜLÜ METOTLARI ---

    public boolean addBookmark(String url, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, url);
        values.put(COLUMN_TITLE, title != null ? title : url);
        
        // INSERT OR IGNORE mantığıyla çalışır, hata kodu yerine -1 döner
        long result = db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
        return result != -1;
    }
}
