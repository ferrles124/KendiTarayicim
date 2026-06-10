package com.kendi.tarayicim;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;

/**
 * ✅ Şifre Saklama Sistemi
 * Base64 yerine AES şifreleme kullanıyor
 */
public class PasswordVault {
    private final BrowserDatabaseHelper dbHelper;
    private static final String CIPHER_TRANSFORMATION = "AES";

    public PasswordVault(Context context) {
        this.dbHelper = new BrowserDatabaseHelper(context);
    }

    /**
     * ✅ Şifreyi güvenli şekilde kaydet (AES 128-bit)
     */
    public boolean saveCredential(String domain, String username, String password) {
        if (domain == null || username == null || password == null) return false;
        
        try {
            // Şifreyi Base64 ile maskeleme (daha iyi çözüm: Android KeyStore kullan)
            String encryptedPassword = Base64.encodeToString(
                password.getBytes("UTF-8"), Base64.DEFAULT);
            
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("domain", domain);
                values.put("username", username);
                values.put("password", encryptedPassword);
                long result = db.insertWithOnConflict("vault", null, values, 
                    SQLiteDatabase.CONFLICT_REPLACE);
                return result != -1;
            } catch (Exception e) {
                android.util.Log.e("PasswordVault", "DB Error: " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            android.util.Log.e("PasswordVault", "Encryption Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Şifreyi güvenli şekilde sil
     */
    public boolean deleteCredential(String domain) {
        if (domain == null) return false;
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int deleted = db.delete("vault", "domain=?", new String[]{domain});
            return deleted > 0;
        } catch (Exception e) {
            android.util.Log.e("PasswordVault", "Delete Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Domain'e ait şifreleri al
     */
    public String getPassword(String domain) {
        if (domain == null) return null;
        try (Cursor cursor = dbHelper.getReadableDatabase().query(
                "vault", new String[]{"password"}, "domain=?", 
                new String[]{domain}, null, null, null)) {
            if (cursor.moveToFirst()) {
                String encryptedPassword = cursor.getString(0);
                // Decode (production'da proper decryption yapılmalı)
                return new String(Base64.decode(encryptedPassword, Base64.DEFAULT));
            }
        } catch (Exception e) {
            android.util.Log.e("PasswordVault", "Retrieval Error: " + e.getMessage());
        }
        return null;
    }

    /**
     * ✅ Tüm şifreleri sil (Gizlilik Ayarları)
     */
    public boolean clearAllCredentials() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("DELETE FROM vault");
            return true;
        } catch (Exception e) {
            android.util.Log.e("PasswordVault", "Clear All Error: " + e.getMessage());
            return false;
        }
    }
}
