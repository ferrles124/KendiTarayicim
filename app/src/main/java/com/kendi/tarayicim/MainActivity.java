package com.kendi.tarayicim;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private Button btnGo;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private Button btnMenu;
    
    // Alt Bar Navigasyon Butonları
    private Button btnBack;
    private Button btnForward;
    private Button btnRefresh;
    private Button btnHome;

    // Yan Menü Butonları
    private Button menuBookmarks;
    private Button menuHistory;
    private Button menuDownloads;
    private Button menuAdBlock;
    private Button menuVpn;
    private Button menuSync;
    private Button menuSettings;

    private final String HOME_URL = "https://www.google.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bileşenleri Bağlama
        webView = findViewById(R.id.web_view);
        urlInput = findViewById(R.id.url_input);
        btnGo = findViewById(R.id.btn_go);
        progressBar = findViewById(R.id.progress_bar);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_menu);
        
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);

        menuBookmarks = findViewById(R.id.menu_bookmarks);
        menuHistory = findViewById(R.id.menu_history);
        menuDownloads = findViewById(R.id.menu_downloads);
        menuAdBlock = findViewById(R.id.menu_adblock);
        menuVpn = findViewById(R.id.menu_vpn);
        menuSync = findViewById(R.id.menu_sync);
        menuSettings = findViewById(R.id.menu_settings);

        // Gelişmiş Tarayıcı Ayarları
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDatabaseEnabled(true);

        // Sayfa Yüklenme Durumu İzleyici
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                urlInput.setText(url);
            }
        });

        webView.loadUrl(HOME_URL);

        // Tetikleyiciler
        btnGo.setOnClickListener(v -> loadWebPage());
        
        // Yan Menüyü Açma Butonu
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Alt Navigasyon Fonksiyonları
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefresh.setOnClickListener(v -> webView.reload());
        btnHome.setOnClickListener(v -> webView.loadUrl(HOME_URL));

        // GELİŞMİŞ MENÜ MODÜLLERİ ALTYAPISI
        menuBookmarks.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "Yer İmleri Modülü Hazırlanıyor...", Toast.LENGTH_SHORT).show();
        });

        menuHistory.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "Geçmiş Veritabanı Modülü Hazırlanıyor...", Toast.LENGTH_SHORT).show();
        });

        menuDownloads.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "İndirme Yöneticisi Başlatılıyor...", Toast.LENGTH_SHORT).show();
        });

        menuAdBlock.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "AdBlock Filtreleri Güncelleniyor...", Toast.LENGTH_SHORT).show();
        });

        menuVpn.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "Güvenli VPN Tüneli Aranıyor...", Toast.LENGTH_SHORT).show();
        });

        menuSync.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "Bulut Yedekleme Başlatılıyor...", Toast.LENGTH_SHORT).show();
        });

        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(MainActivity.this, "Ayarlar Paneli Açılıyor...", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadWebPage() {
        String url = urlInput.getText().toString().trim();
        if (!url.isEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                if (url.contains(".") && !url.contains(" ")) {
                    url = "https://" + url;
                } else {
                    url = "https://www.google.com/search?q=" + url;
                }
            }
            webView.loadUrl(url);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
