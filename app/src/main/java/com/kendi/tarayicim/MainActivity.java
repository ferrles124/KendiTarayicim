package com.kendi.tarayicim;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private Button btnGo;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;
    
    private ImageButton btnBack, btnForward, btnRefresh, btnHome;
    private Button menuBookmarks, menuHistory, menuAdBlock, menuSettings;

    private BrowserDatabaseHelper dbHelper;
    private AdBlockEngine adBlockEngine;

    // Varsayılan Adres Yerel V3 Ana Sayfamız Olarak Ayarlandı
    private static final String HOME_URL = "file:///android_asset/home.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new BrowserDatabaseHelper(this);
        adBlockEngine = new AdBlockEngine();

        initializeUiComponents();
        configureWebViewSettings();
        setupBrowserClients();
        setupDownloadListener();
        setupClickListeners();

        webView.loadUrl(HOME_URL);
    }

    private void initializeUiComponents() {
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
        menuAdBlock = findViewById(R.id.menu_adblock);
        menuSettings = findViewById(R.id.menu_settings);
    }

    private void configureWebViewSettings() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true); // Yerel dosya erişimi aktif edildi
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    private void setupBrowserClients() {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                
                // Eğer ana sayfadaysak üst barı temiz tut, değilse URL'i yaz
                if (url.equals(HOME_URL)) {
                    urlInput.setText("");
                } else {
                    urlInput.setText(url);
                    dbHelper.addHistoryItem(url, view.getTitle());
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Yerel ana sayfadaki arama tetikleyicisini yakala ve Google'a aktar
                if (url.startsWith("search://")) {
                    try {
                        String query = url.substring(9);
                        query = URLDecoder.decode(query, "UTF-8");
                        view.loadUrl("https://www.google.com/search?q=" + query);
                    } catch (Exception e) {
                        // Çözümleme hatası koruması
                    }
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (adBlockEngine.isAdRequest(url)) {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
    }

    private void setupDownloadListener() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Dosya indiriliyor...");
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(MainActivity.this, "İndirme başlatıldı.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "İndirme başarısız oldu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnGo.setOnClickListener(v -> {
            loadWebPage();
            hideKeyboard();
        });

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            loadWebPage();
            hideKeyboard();
            return true;
        });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefresh.setOnClickListener(v -> webView.reload());
        btnHome.setOnClickListener(v -> webView.loadUrl(HOME_URL));

        menuAdBlock.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "AdBlocker Motoru Aktif: Koruma Sağlanıyor.", Toast.LENGTH_LONG).show();
        });

        menuHistory.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            List<String> history = dbHelper.getHistoryList();
            if (history.isEmpty()) {
                Toast.makeText(this, "Tarama geçmişi temiz.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Son Ziyaret: " + history.get(0), Toast.LENGTH_LONG).show();
            }
        });

        menuBookmarks.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String currentUrl = webView.getUrl();
            if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals(HOME_URL)) {
                boolean isAdded = dbHelper.addBookmark(currentUrl, webView.getTitle());
                if (isAdded) {
                    Toast.makeText(this, "Koleksiyona başarıyla eklendi.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Bu sayfa zaten yer imlerinde mevcut.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Ana sayfa yer imlerine eklenemez.", Toast.LENGTH_SHORT).show();
            }
        });

        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Ayarlar Sistemi Hazırlanıyor.", Toast.LENGTH_SHORT).show();
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

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (webView.canGoBack()) {
            // Eğer geri gidildiğinde ana sayfaya dönüyorsa arama çubuğunu temizle
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
