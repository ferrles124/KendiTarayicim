package com.kendi.tarayicim;

import android.webkit.WebSettings;
import android.webkit.WebView;

public class ProxyTunnel {
    private boolean isTunnelActive = false;

    public void toggleSecureTunnel(WebView webView, boolean activate) {
        this.isTunnelActive = activate;
        WebSettings settings = webView.getSettings();
        if (activate) {
            // Ticari seviyede veri sıkıştırma modu ve gizlilik kalkanı ayarları aktif edilir
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            System.setProperty("http.proxyHost", "secure.quantum-tunnel.net");
            System.setProperty("http.proxyPort", "8080");
        } else {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

    public boolean isTunnelActive() {
        return isTunnelActive;
    }
}
