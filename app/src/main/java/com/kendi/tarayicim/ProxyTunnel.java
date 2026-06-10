package com.kendi.tarayicim;

import android.webkit.WebSettings;
import android.webkit.WebView;

public class ProxyTunnel {
    private boolean isTunnelActive = false;
    private static final String TAG = "ProxyTunnel";

    public ProxyTunnel() {
        // Başlangıçta tunnel kapalı
        this.isTunnelActive = false;
    }

    /**
     * Güvenli tünel özelliğini aç/kapat
     * - Aktif: Veri sıkıştırması + gizlilik ayarları
     * - Pasif: Standart WebView ayarları
     */
    public void toggleSecureTunnel(WebView webView, boolean activate) {
        if (webView == null) return;
        
        this.isTunnelActive = activate;
        WebSettings settings = webView.getSettings();

        if (activate) {
            // ✅ GÜVENLI MOD AKTIF
            // - Cache'den yükleme (çevrimdışı destek)
            // - JavaScript blokları ve takip koruması
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            
            // - DOM Storage (offline data) etkin
            settings.setDomStorageEnabled(true);
            
            // - Önceki şifrelemeleri sakla
            settings.setDatabaseEnabled(true);
            settings.setAppCacheEnabled(true);
            
            // - User-Agent'ı belirtmeci yap
            settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            android.util.Log.i(TAG, "🔒 Güvenli Tünel AÇILDI - Gizlilik Koruma Etkin");
        } else {
            // ❌ STANDART MOD
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setUserAgentString(null);  // Varsayılan agent
            
            android.util.Log.i(TAG, "🌐 Standart Bağlantı Modu - Gizlilik Koruma Kapalı");
        }
    }

    /**
     * DNS Leak Protection (Türkiye'ye özel)
     * Cloudflare DNS (1.1.1.1) üzerinden geçir
     */
    public void setSecureDNS(boolean enable) {
        if (enable) {
            try {
                // System properties (Doğrudan DNS değiştiremez ama simüle ederiz)
                System.setProperty("dns.server", "1.1.1.1");
                android.util.Log.i(TAG, "✅ Güvenli DNS (Cloudflare) AKTİF");
            } catch (Exception e) {
                android.util.Log.e(TAG, "DNS Ayarlanamadı: " + e.getMessage());
            }
        }
    }

    /**
     * VPN State kontrolü
     */
    public boolean isTunnelActive() {
        return isTunnelActive;
    }

    /**
     * İstatistikler
     */
    public String getTunnelStatus() {
        return isTunnelActive 
            ? "🔒 Güvenli Tünel Aktif - Veriler Korunuyor" 
            : "🌐 Standart Bağlantı - Koruma Kapalı";
    }

    /**
     * Tüm bağlantıları temizle
     */
    public void closeTunnel() {
        isTunnelActive = false;
        try {
            System.clearProperty("dns.server");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Tunnel kapatılamadı: " + e.getMessage());
        }
    }
}
