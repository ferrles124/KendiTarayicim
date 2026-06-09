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
import java.util.List;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private ImageButton btnGo;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnBack, btnForward, btnRefresh, btnHome, btnNewTab;
    private Button menuBookmarks, menuHistory, menuAdBlock, menuSettings;
    private RecyclerView tabsRecycler;
    private TabAdapter tabAdapter;
    private FrameLayout webViewContainer;

    private BrowserDatabaseHelper dbHelper;
    private AdBlockEngine adBlockEngine;
    private TabManager tabManager;
    private LiveScoreEngine liveScoreEngine;
    private ProxyTunnel proxyTunnel;

    private int totalBlocked = 0;

    private static final String HOME_URL = "file:///android_asset/home.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionHelper.requestIfNeeded(this);
        
        dbHelper = new BrowserDatabaseHelper(this);
        adBlockEngine = new AdBlockEngine();
        tabManager = new TabManager(this);
        liveScoreEngine = new LiveScoreEngine();
        proxyTunnel = new ProxyTunnel();

        initViews();
        setupTabManager();
        setupTabRecycler();

        webView = tabManager.createNewTab();
        configureWebView(webView);
        attachWebView(webView);
        setupClickListeners();
        webView.loadUrl(HOME_URL);
    }

    // ── VIEWS ──
    private void initViews() {
        urlInput         = findViewById(R.id.url_input);
        btnGo            = findViewById(R.id.btn_go);
        progressBar      = findViewById(R.id.progress_bar);
        drawerLayout     = findViewById(R.id.drawer_layout);
        btnMenu          = findViewById(R.id.btn_menu);
        btnBack          = findViewById(R.id.btn_back);
        btnForward       = findViewById(R.id.btn_forward);
        btnRefresh       = findViewById(R.id.btn_refresh);
        btnHome          = findViewById(R.id.btn_home);
        btnNewTab        = findViewById(R.id.btn_new_tab);
        tabsRecycler     = findViewById(R.id.tabs_recycler);
        webViewContainer = findViewById(R.id.web_view);
        menuBookmarks    = findViewById(R.id.menu_bookmarks);
        menuHistory      = findViewById(R.id.menu_history);
        menuAdBlock      = findViewById(R.id.menu_adblock);
        menuSettings     = findViewById(R.id.menu_settings);
    }

    // ── SEKMELER ──
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
        tabsRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tabAdapter = new TabAdapter(tabManager.getAllTabs(), 0,
                new TabAdapter.OnTabActionListener() {
            @Override
            public void onTabSelected(int index) {
                tabManager.switchTab(index);
            }
            @Override
            public void onTabClosed(int index) {
                if (tabManager.getTabCount() <= 1) {
                    Toast.makeText(MainActivity.this,
                            "En az bir sekme açık olmalı", Toast.LENGTH_SHORT).show();
                    return;
                }
                tabManager.closeTab(index);
                tabAdapter.notifyDataSetChanged();
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
        webViewContainer.removeAllViews();
        if (wv.getParent() != null) ((ViewGroup) wv.getParent()).removeView(wv);
        webViewContainer.addView(wv, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void openNewTab(String url) {
        WebView tab = tabManager.createNewTab();
        configureWebView(tab);
        webView = tab;
        attachWebView(webView);
        tabAdapter.setSelectedIndex(tabManager.getCurrentTabIndex());
        tabAdapter.notifyDataSetChanged();
        webView.loadUrl(url != null ? url : HOME_URL);
    }

    // ── WEBVIEW AYARLARI ──
    private void configureWebView(WebView web) {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progressBar.setProgress(p);
                progressBar.setVisibility(p == 100 ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onReceivedTitle(WebView view, String title) {
                tabManager.updateTabInfo(view.getUrl(), title);
                tabAdapter.notifyDataSetChanged();
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
                if (url != null && !url.equals(HOME_URL)) {
                    urlInput.setText(url);
                    dbHelper.addHistoryItem(url, view.getTitle());
                } else {
                    urlInput.setText("");
                }
                tabManager.updateTabInfo(url, view.getTitle());
                tabAdapter.notifyDataSetChanged();

                // Engelleme sayacını güncelle
                int count = adBlockEngine.getBlockCount();
                if (count != totalBlocked) {
                    totalBlocked = count;
                    runOnUiThread(() -> updateAdBlockButton());
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("search://")) {
                    try {
                        String q = URLDecoder.decode(url.substring(9), "UTF-8");
                        view.loadUrl("https://www.google.com/search?q=" + q);
                    } catch (Exception e) { e.printStackTrace(); }
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                if (adBlockEngine.isAdRequest(request.getUrl().toString())) {
                    return new WebResourceResponse("text/plain", "UTF-8",
                            new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        web.setDownloadListener((url, ua, cd, mime, length) -> {
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.setMimeType(mime);
                req.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
                req.addRequestHeader("User-Agent", ua);
                String fileName = URLUtil.guessFileName(url, cd, mime);
                req.setTitle(fileName);
                req.setDescription("İndiriliyor...");
                req.allowScanningByMediaScanner();
                req.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(req);
                    Toast.makeText(this, "İndirme başladı: " + fileName,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "İndirme hatası", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── ADBLOCK BUTON GÜNCELLE ──
    private void updateAdBlockButton() {
        if (totalBlocked > 0) {
            menuAdBlock.setText("🛡  Engellendi  •  " + totalBlocked);
            menuAdBlock.setTextColor(0xFF4D9EFF);
        } else {
            menuAdBlock.setText("Reklam Engelleme");
            menuAdBlock.setTextColor(0xFF4D9EFF);
        }
    }

    // ── TIKLAMALAR ──
    private void setupClickListeners() {
        menuSettings.setOnClickListener(v -> {
    drawerLayout.closeDrawer(GravityCompat.START);
    startActivity(new Intent(this, SettingsActivity.class));
});

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnRefresh.setOnClickListener(v -> webView.reload());
        btnHome.setOnClickListener(v -> { webView.loadUrl(HOME_URL); urlInput.setText(""); });
        btnNewTab.setOnClickListener(v -> openNewTab(HOME_URL));

        // GEÇMİŞ
        menuHistory.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            List<BrowserDatabaseHelper.HistoryItem> items = dbHelper.getHistoryItems();
            BottomSheetPanel.showHistory(this, items, dbHelper,
                    new BottomSheetPanel.OnItemClickListener() {
                @Override public void onItemClick(String url) { webView.loadUrl(url); }
                @Override public void onItemDelete(int id) { dbHelper.deleteHistoryItem(id); }
            });
        });

        // YER İMLERİ
        menuBookmarks.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String currentUrl = webView.getUrl();
            boolean isHome = currentUrl == null || currentUrl.equals(HOME_URL);
            if (!isHome) {
                if (!dbHelper.isBookmarked(currentUrl)) {
                    boolean added = dbHelper.addBookmark(currentUrl, webView.getTitle());
                    Toast.makeText(this,
                            added ? "⭐ Yer imine eklendi" : "Zaten kayıtlı",
                            Toast.LENGTH_SHORT).show();
                } else {
                    showBookmarkList();
                }
            } else {
                showBookmarkList();
            }
        });

        menuBookmarks.setOnLongClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showBookmarkList();
            return true;
        });

        // REKLAM ENGEL
        menuAdBlock.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            boolean next = !proxyTunnel.isTunnelActive();
            proxyTunnel.toggleSecureTunnel(webView, next);
            int count = adBlockEngine.getBlockCount();
            String msg = next
                    ? "🛡 Aktif — " + count + " istek engellendi"
                    : "Standart bağlantı modu";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // AYARLAR — masaüstü/mobil mod
        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            String ua = webView.getSettings().getUserAgentString();
            if (ua != null && ua.contains("Windows")) {
                webView.getSettings().setUserAgentString(null);
                Toast.makeText(this, "📱 Mobil Mod", Toast.LENGTH_SHORT).show();
            } else {
                webView.getSettings().setUserAgentString(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                Toast.makeText(this, "🖥 Masaüstü Mod", Toast.LENGTH_SHORT).show();
            }
            webView.reload();
        });
    }

    private void showBookmarkList() {
        List<BrowserDatabaseHelper.BookmarkItem> items = dbHelper.getBookmarkItems();
        BottomSheetPanel.showBookmarks(this, items,
                new BottomSheetPanel.OnItemClickListener() {
            @Override public void onItemClick(String url) { webView.loadUrl(url); }
            @Override public void onItemDelete(int id) { dbHelper.deleteBookmark(id); }
        });
    }

    private void navigate() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = url.contains(".") && !url.contains(" ")
                    ? "https://" + url
                    : "https://www.google.com/search?q=" + url;
        }
        webView.loadUrl(url);
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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

    @Override
    protected void onDestroy() {
        liveScoreEngine.stopUpdates();
        super.onDestroy();
    }
}
