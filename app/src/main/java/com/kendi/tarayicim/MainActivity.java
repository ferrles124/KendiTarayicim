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
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText urlInput;
    private Button btnGo;
    private ProgressBar progressBar;
    
    // Yeni Navigasyon Butonları
    private Button btnBack;
    private Button btnForward;
    private Button btnRefresh;
    private Button btnHome;

    private final String HOME_URL = "https://www.google.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Görsel bileşenleri bağlıyoruz
        webView = findViewById(R.id.web_view);
        urlInput = findViewById(R.id.url_input);
        btnGo = findViewById(R.id.btn_go);
        progressBar = findViewById(R.id.progress_bar);
        
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);

        // Gelişmiş Webview Ayarları
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDatabaseEnabled(true);

        // Sayfa yüklenme yüzdesini takip eden ve ProgressBar'ı güncelleyen mekanizma
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.getSystemUiVisibility());
                }
            }
        });

        // Sayfa olaylarını takip eden istemci
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
                urlInput.setText(url); // Gezinilen sitenin adresini çubuğa yaz
            }
        });

        // İlk açılışta ana sayfayı yükle
        webView.loadUrl(HOME_URL);

        // Git Butonu tetikleyicisi
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadWebPage();
            }
        });

        // NAVİGASYON BUTONLARININ GÖREVLERİ
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        btnForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(HOME_URL);
            }
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
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
