package com.kendi.tarayicim;

import android.net.Uri;
import java.util.HashSet;
import java.util.Set;

public class AdBlockEngine {

    private final Set<String> adHosts;

    public AdBlockEngine() {
        adHosts = new HashSet<>();
        initBlacklist();
    }

    private void initBlacklist() {
        // Genişletilmiş ve optimize edilmiş ticari reklam/takipçi sunucu listesi
        adHosts.add("doubleclick.net");
        adHosts.add("googleads.g.doubleclick.net");
        adHosts.add("googlesyndication.com");
        adHosts.add("adservice.google.com");
        adHosts.add("adnxs.com");
        adHosts.add("adform.net");
        adHosts.add("analytics.google.com");
        adHosts.add("scorecardresearch.com");
        adHosts.add("quantserve.com");
        adHosts.add("popads.net");
        adHosts.add("propellerads.com");
        adHosts.add("adcolony.com");
    }

    /**
     * Gelen URL'in bir reklam veya takipçi isteği olup olmadığını doğrular.
     * @param url Kontrol edilecek ham web adresi
     * @return true eğer reklam ise, false eğer güvenli ise
     */
    public boolean isAdRequest(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }

            // Ana domain ve alt domain kontrollerini derinlemesine tara
            for (String adHost : adHosts) {
                if (host.equals(adHost) || host.endsWith("." + adHost)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // URL ayrıştırma hatası durumunda tarayıcının çökmesini engelle, geçişe izin ver
            return false;
        }
        return false;
    }
}
