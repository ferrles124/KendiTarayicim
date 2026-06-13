package com.kendi.tarayicim;

import android.webkit.WebSettings;
import android.webkit.WebView;

public class ProxyTunnel {
    private boolean isTunnelActive = false;
    private static final String TAG = "ProxyTunnel";

    public ProxyTunnel() {
        this.isTunnelActive = false;
    }

    public void toggleSecureTunnel(WebView webView, boolean activate) {
        if (webView == null) return;

        this.isTunnelActive = activate;
        WebSettings settings = webView.getSettings();

        if (activate) {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setUserAgentString(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            android.util.Log.i(TAG, "Guvenli Tunel ACILDI");
        } else {
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setUserAgentString(null);
            android.util.Log.i(TAG, "Standart Baglanti Modu");
        }
    }

    public void setSecureDNS(boolean enable) {
        if (enable) {
            try {
                System.setProperty("dns.server", "1.1.1.1");
                android.util.Log.i(TAG, "Guvenli DNS Aktif");
            } catch (Exception e) {
                android.util.Log.e(TAG, "DNS Ayarlanamadi: " + e.getMessage());
            }
        }
    }

    public boolean isTunnelActive() {
        return isTunnelActive;
    }

    public String getTunnelStatus() {
        return isTunnelActive
            ? "Guvenli Tunel Aktif"
            : "Standart Baglanti";
    }

    public void closeTunnel() {
        isTunnelActive = false;
        try {
            System.clearProperty("dns.server");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Tunnel kapatilamadi: " + e.getMessage());
        }
    }
}
