package com.kendi.tarayicim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

public class PasswordVault {
    private final BrowserDatabaseHelper dbHelper;

    public PasswordVault(Context context) {
        // Mevcut veritabanı yardımcımıza bağlanır
        this.dbHelper = new BrowserDatabaseHelper(context);
    }

    public void saveCredential(String domain, String username, String password) {
        try {
            // Şifreyi ham olarak değil, Base64 korumasıyla maskeleyerek veritabanına işler
            String encryptedPassword = Base64.encodeToString(password.getBytes("UTF-8"), Base64.DEFAULT);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("domain", domain);
            values.put("username", username);
            values.put("password", encryptedPassword);
            db.insertWithOnConflict("vault", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
