package com.kendi.tarayicim;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private Button btnGo;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnBack, btnForward, btnRefresh, btnHome;
    private ImageButton btnNewTab;
    private Button menuBookmarks, menuHistory, menuAdBlock, menuSettings;
    private RecyclerView tabsRecycler;
    private TabAdapter tabAdapter;
    private FrameLayout webViewContainer;

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

        dbHelper = new BrowserDatabaseHelper(this);
        adBlockEngine = new AdBlockEngine();
        tabManager = new TabManager(this);
        liveScoreEngine = new LiveScoreEngine();
        passwordVault = new PasswordVault(this);
        proxyTunnel = new ProxyTunnel();

        initializeUiComponents();
        setupTabManager();
        setupTabRecycler();

        // İlk sekmeyi oluştur
        webView = tabManager.createNewTab();
        configureWebViewSettings(webView);
        setupBrowserClients(webView);
        setupDownloadListener(webView);
        attachWebView(webView);

        setupClickListeners();
        webView.loadUrl(HOME_URL);
    }

    private void initializeUiComponents() {
        urlInput = findViewById(R.id.url_input);
        btnGo = findViewById(R.id.btn_go);
        progressBar = findViewById(R.id.progress_bar);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_menu);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);
        btnNewTab = findViewById(R.id.btn_new_tab);
        tabsRecycler = findViewById(R.id.tabs_recycler);
        webViewContainer = findViewById(R.id.web_view);
        menuBookmarks = findViewById(R.id.menu_bookmarks);
        menuHistory = findViewById(R.id.menu_history);
        menuAdBlock = findViewById(R.id.menu_adblock);
        menuSettings = findViewById(R.id.menu_settings);
    }

    private void setupTabManager() {
        tabManager.setOnTabChangeListener(new TabManager.OnTabChangeListener() {
            @Override
            public void onTabChanged(int index, WebView newWebView) {
                webView = newWebView;
                attachWebView(webView);
                String url = webView.getUrl();
                urlInput.setText(url != null && !url.equals(HOME_URL) ? url : "");
                if (tabAdapter != null) tabAdapter.setSelectedIndex(index);
            }

            @Override
            public void onTabCountChanged(int count) {
                if (tabAdapter != null) tabAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupTabRecycler() {
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        tabsRecycler.setLayoutManager(lm);

        tabAdapter = new TabAdapter(tabManager.getAllTabs(), 0, new TabAdapter.OnTabActionListener() {
            @Override
            public void onTabSelected(int index) {
                tabManager.switchTab(index);
            }

            @Override
            public void onTabClosed(int index) {
                if (tabManager.getTabCount() <= 1) {
                    // Son sekme kapatılamaz, yenile
                    Toast.makeText(MainActivity.this, "En az bir sekme açık olmalı.", Toast.LENGTH_SHORT).show();
                    return;
                }
                tabManager.closeTab(index);
                tabAdapter.notifyDataSetChanged();
                // Aktif WebView'u güncelle
                WebView current = tabManager.getCurrentWebView();
                if (current != null) {
                    webView = current;
                    attachWebView(webView);
                    tabAdapter.setSelectedIndex(tabManager.getCurrentTabIndex());
                }
            }
        });

        tabsRecycler.setAdapter(tabAdapter);
    }

    private void attachWebView(WebView wv) {
        if (webViewContainer != null) {
            webViewContainer.removeAllViews();
            if (wv.getParent() != null) {
                ((ViewGroup) wv.getParent()).removeView(wv);
            }
            webViewContainer.addView(wv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
    }

    private void openNewTab(String url) {
        WebView newTab = tabManager.createNewTab();
        configureWebViewSettings(newTab);
        setupBrowserClients(newTab);
        setupDownloadListener(newTab);
        webView = newTab;
        attachWebView(webView);
        tabAdapter.setSelectedIndex(tabManager.getCurrentTabIndex());
        tabAdapter.notifyDataSetChanged();
        webView.loadUrl(url != null ? url : HOME_URL);
    }

    private void configureWebViewSettings(WebView web) {
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
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

            @Override
            public void onReceivedTitle(WebView view, String title) {
                // Sekme başlığını güncelle
                tabManager.updateTabInfo(view.getUrl(), title);
                tabAdapter.notifyDataSetChanged();
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                tabManager.updateTabInfo(url, url);
                tabAdapter.notifyDataSetChanged();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                if (url.equals(HOME_URL)) {
                    urlInput.setText("");
                    liveScoreEngine.startLiveUpdates(view);
                } else {
                    urlInput.setText(url);
                    dbHelper.addHistoryItem(url, view.getTitle());
                    liveScoreEngine.stopUpdates();
                }
                tabManager.updateTabInfo(url, view.getTitle());
                tabAdapter.notifyDataSetChanged();
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
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new ByteArrayInputStream("".getBytes()));
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
                request.setDescription("İndiriliyor...");
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setTitle(fileName);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(MainActivity.this, "İndirme başlatıldı: " + fileName, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "İndirme hatası oluştu.", Toast.LENGTH_SHORT).show();
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

        // YENİ SEKME BUTONU
        btnNewTab.setOnClickListener(v -> {
            openNewTab(HOME_URL);
            Toast.makeText(this, "Yeni sekme açıldı", Toast.LENGTH_SHORT).show();
        });

        menuAdBlock.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            boolean nextState = !proxyTunnel.isTunnelActive();
            proxyTunnel.toggleSecureTunnel(webView, nextState);
            Toast.makeText(this, nextState ? "🛡 Gizlilik Tüneli Aktif" : "Standart Bağlantı Modu", Toast.LENGTH_LONG).show();
        });

        menuHistory.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Toast.makeText(this, "Açık Sekme: " + tabManager.getTabCount(), Toast.LENGTH_SHORT).show();
        });

        menuBookmarks.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String currentUrl = webView.getUrl();
            if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals(HOME_URL)) {
                dbHelper.addBookmark(currentUrl, webView.getTitle());
                Toast.makeText(this, "⭐ Yer imine eklendi!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ana sayfa yer imlerine eklenemez.", Toast.LENGTH_SHORT).show();
            }
        });

        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String currentAgent = webView.getSettings().getUserAgentString();
            if (currentAgent != null && currentAgent.contains("Desktop")) {
                webView.getSettings().setUserAgentString(null);
                Toast.makeText(this, "📱 Mobil Mod Aktif", Toast.LENGTH_SHORT).show();
            } else {
                webView.getSettings().setUserAgentString(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                Toast.makeText(this, "🖥 Masaüstü Modu Aktif", Toast.LENGTH_SHORT).show();
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
