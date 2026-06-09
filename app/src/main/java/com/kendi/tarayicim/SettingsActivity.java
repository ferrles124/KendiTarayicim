package com.kendi.tarayicim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "kendibrowser_prefs";

    // Anahtar sabitler
    public static final String KEY_SEARCH_ENGINE   = "search_engine";
    public static final String KEY_HOMEPAGE        = "homepage";
    public static final String KEY_ADBLOCK         = "adblock_enabled";
    public static final String KEY_TRACKER         = "tracker_enabled";
    public static final String KEY_HTTPS           = "https_force";
    public static final String KEY_COOKIE_BLOCK    = "cookie_block";
    public static final String KEY_JAVASCRIPT      = "javascript_enabled";
    public static final String KEY_LOCATION        = "location_enabled";
    public static final String KEY_CAMERA          = "camera_enabled";
    public static final String KEY_MICROPHONE      = "microphone_enabled";
    public static final String KEY_POPUP_BLOCK     = "popup_block";
    public static final String KEY_REDIRECT_BLOCK  = "redirect_block";
    public static final String KEY_FINGERPRINT     = "fingerprint_protect";
    public static final String KEY_AUTO_CLEAR      = "auto_clear_days";
    public static final String KEY_DARK_MODE       = "dark_mode";
    public static final String KEY_FONT_SIZE       = "font_size";
    public static final String KEY_DESKTOP_MODE    = "desktop_mode";
    public static final String KEY_FULLSCREEN      = "fullscreen";
    public static final String KEY_DOWNLOAD_ASK    = "download_ask";
    public static final String KEY_DOWNLOAD_NOTIFY = "download_notify";
    public static final String KEY_SAVE_PASSWORD   = "save_password";
    public static final String KEY_AUTOFILL        = "autofill";
    public static final String KEY_NOTIF_SITES     = "notif_sites";
    public static final String KEY_NOTIF_DOWNLOAD  = "notif_download";

    private SettingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        RecyclerView rv = findViewById(R.id.settings_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        adapter = new SettingsAdapter(this, prefs, this::onSettingAction);
        rv.setAdapter(adapter);
    }

    private void onSettingAction(String action) {
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        switch (action) {
            case "clear_history":
                new BrowserDatabaseHelper(this).clearHistory();
                Toast.makeText(this, "✓ Geçmiş temizlendi", Toast.LENGTH_SHORT).show();
                break;
            case "clear_cookies":
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                Toast.makeText(this, "✓ Çerezler temizlendi", Toast.LENGTH_SHORT).show();
                break;
            case "clear_cache":
                // WebView cache temizleme MainActivity'de yapılır, burada bildir
                prefs.edit().putBoolean("pending_clear_cache", true).apply();
                Toast.makeText(this, "✓ Önbellek temizlenecek", Toast.LENGTH_SHORT).show();
                break;
            case "clear_all":
                new BrowserDatabaseHelper(this).clearHistory();
                CookieManager.getInstance().removeAllCookies(null);
                CookieManager.getInstance().flush();
                prefs.edit().putBoolean("pending_clear_cache", true).apply();
                Toast.makeText(this, "✓ Tüm veriler temizlendi", Toast.LENGTH_SHORT).show();
                break;
            case "privacy_policy":
                Toast.makeText(this, "Gizlilik politikası yakında", Toast.LENGTH_SHORT).show();
                break;
            case "feedback":
                Toast.makeText(this, "Geri bildirim yakında", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
