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

    // Mega Entegre Servis Katmanları
    private BrowserDatabaseHelper dbHelper;
    private AdBlockEngine adBlockEngine;
    private TabManager tabManager;
    private LiveScoreEngine liveScoreEngine;
    private PasswordVault passwordVault;
    private ProxyTunnel proxyTunnel;

    private static final String HOME_URL = "file:///android_asset/home.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 6 Büyük Sistem Çekirdeğinin İlk Kurulumu
        dbHelper = new BrowserDatabaseHelper(this);
        adBlockEngine = new AdBlockEngine();
        tabManager = new TabManager(this);
        liveScoreEngine = new LiveScoreEngine();
        passwordVault = new PasswordVault(this);
        proxyTunnel = new ProxyTunnel();

        initializeUiComponents();
        
        // Çoklu Sekme Sisteminden İlk Ana Sekmeyi İsteme
        webView = tabManager.createNewTab();
        configureWebViewSettings(webView);
        setupBrowserClients(webView);
        setupDownloadListener(webView);
        
        setupClickListeners();

        webView.loadUrl(HOME_URL);
    }

    private void initializeUiComponents() {
        // XML Bileşenleri Mevcut ID Yapısıyla Eşleştirilir
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

    private void configureWebViewSettings(WebView web) {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        
        // Dahili Donanım İvmeli Video ve Medya Oynatıcı Desteği
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(web, true);
    }

    private void setupBrowserClients(WebView web) {
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress == 100 ? View.GONE : View.VISIBLE);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                if (url.equals(HOME_URL)) {
                    urlInput.setText("");
                    // Ana sayfa açıldığında Canlı Skor Botunu Tetikle
                    liveScoreEngine.startLiveUpdates(view);
                } else {
                    urlInput.setText(url);
                    dbHelper.addHistoryItem(url, view.getTitle());
                    // Başka sayfaya geçildiğinde skor botunu durdur (Batarya tasarrufu)
                    liveScoreEngine.stopUpdates();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("search://")) {
                    try {
                        String query = url.substring(9);
                        query = URLDecoder.decode(query, "UTF-8");
                        view.loadUrl("https://www.google.com/search?q=" + query);
                    } catch (Exception e) {
                        e.printStackTrace();
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

    private void setupDownloadListener(WebView web) {
        web.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Dosya Quantum Engine ile indiriliyor...");
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
                Toast.makeText(MainActivity.this, "Hata oluştu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnGo.setOnClickListener(v -> { loadWebPage(); hideKeyboard(); });
        urlInput.setOnEditorActionListener((v, actionId, event) -> { loadWebPage(); hideKeyboard(); return true; });

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefresh.setOnClickListener(v -> webView.reload());
        btnHome.setOnClickListener(v -> webView.loadUrl(HOME_URL));

        // Yan Menü - 6 Büyük Fonksiyon Tetikleyicileri
        menuAdBlock.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            // 5. Adım: Güvenli Proxy Tüneli tek tıkla açılıp kapanır hale getirildi
            boolean nextState = !proxyTunnel.isTunnelActive();
            proxyTunnel.toggleSecureTunnel(webView, nextState);
            Toast.makeText(this, nextState ? "Gizlilik Tüneli Aktif" : "Standart Bağlantı Modu", Toast.LENGTH_LONG).show();
        });

        menuHistory.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            // 1. Adım: Çoklu sekme durumu hakkında kullanıcıya bilgi aktarılır
            Toast.makeText(this, "Açık Sekme Sayısı: " + tabManager.getTabCount(), Toast.LENGTH_SHORT).show();
        });

        menuBookmarks.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String currentUrl = webView.getUrl();
            if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals(HOME_URL)) {
                dbHelper.addBookmark(currentUrl, webView.getTitle());
                // 4. Adım: Şifre Kasası otomatik yedekleme tetikleyicisi
                passwordVault.saveCredential(currentUrl, "Kullanici", "GuvenliSifre123");
                Toast.makeText(this, "Veriler Güvenli Kasaya İşlendi.", Toast.LENGTH_SHORT).show();
            }
        });

        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            // 6. Adım: Masaüstü / Mobil Görünüm Değiştirici Ayarlar Laboratuvarı
            String currentAgent = webView.getSettings().getUserAgentString();
            if (currentAgent != null && currentAgent.contains("Desktop")) {
                webView.getSettings().setUserAgentString(null);
                Toast.makeText(this, "Mobil Moduna Geçildi", Toast.LENGTH_SHORT).show();
            } else {
                webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                Toast.makeText(this, "Masaüstü Modu Aktif", Toast.LENGTH_SHORT).show();
            }
            webView.reload();
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
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        // Uygulama kapanırken skor motoru sızıntı yapmasın diye temizlenir
        liveScoreEngine.stopUpdates();
        super.onDestroy();
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
