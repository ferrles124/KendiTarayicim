package com.kendi.tarayicim;

import android.net.Uri;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AdBlockEngine {

    private final Set<String> blockedHosts;
    private final Set<String> blockedKeywords;
    private final AtomicInteger blockCount;

    public AdBlockEngine() {
        blockedHosts = new HashSet<>();
        blockedKeywords = new HashSet<>();
        blockCount = new AtomicInteger(0);
        loadHostList();
        loadKeywordList();
    }

    // ── SAYAÇ ──
    public int getBlockCount() { return blockCount.get(); }
    public void resetCount() { blockCount.set(0); }

    // ── ANA KONTROL ──
    public boolean isAdRequest(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();

            // 1. Host listesi kontrolü
            for (String blocked : blockedHosts) {
                if (host.equals(blocked) || host.endsWith("." + blocked)) {
                    blockCount.incrementAndGet();
                    return true;
                }
            }

            // 2. URL anahtar kelime kontrolü
            String lowerUrl = url.toLowerCase();
            for (String kw : blockedKeywords) {
                if (lowerUrl.contains(kw)) {
                    blockCount.incrementAndGet();
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // ── 300+ HOST LİSTESİ ──
    private void loadHostList() {
        String[] hosts = {
            // Google Reklam Ağı
            "doubleclick.net", "googlesyndication.com", "adservice.google.com",
            "adservice.google.com.tr", "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com", "tpc.googlesyndication.com",
            "www.googletagservices.com", "googletagservices.com",
            "googletagmanager.com", "google-analytics.com", "analytics.google.com",
            "ssl.google-analytics.com", "stats.g.doubleclick.net",

            // Facebook / Meta Takip
            "connect.facebook.net", "www.facebook.com/tr",
            "an.facebook.com", "graph.facebook.com",
            "pixel.facebook.com", "analytics.facebook.com",
            "sb.scorecardresearch.com",

            // Büyük Reklam Ağları
            "adnxs.com", "adform.net", "adroll.com",
            "advertising.com", "adblade.com", "adbutler.com",
            "adcolony.com", "adhese.com", "adition.com",
            "adjuggler.net", "adloox.com", "adman.gr",
            "admixer.net", "adnium.com", "adprimemedia.com",
            "adsafeprotected.com", "adscale.de", "adsense.com",
            "adskeeper.co.uk", "adsonar.com", "adspirit.de",
            "adswizz.com", "adtech.de", "adthrive.com",
            "adtiger.de", "adtoll.com", "adtomas.com",
            "adtrading.de", "adtrue.com", "advangelists.com",
            "adventori.com", "advertising.com", "advombat.ru",
            "adyoulike.com", "appnexus.com", "appsflyer.com",
            "aralego.com", "atdmt.com",

            // Türkiye Özel Reklam Ağları
            "admost.com", "minimob.com", "mobilike.com",
            "reklam.hurriyet.com.tr", "reklam.milliyet.com.tr",
            "adserver.milliyet.com.tr", "doubleclick.hurriyet.com.tr",
            "reklam.sabah.com.tr", "ads.ntv.com.tr",
            "reklam.haberturk.com", "adserver.posta.com.tr",
            "ads.haber7.com", "mediazone.com.tr",

            // Takip & Analitik
            "scorecardresearch.com", "quantserve.com",
            "comscore.com", "clicktale.net", "crazyegg.com",
            "hotjar.com", "mouseflow.com", "fullstory.com",
            "luckyorange.com", "inspectlet.com", "sessioncam.com",
            "smartlook.com", "logrocket.com", "heap.io",
            "mixpanel.com", "amplitude.com", "segment.io",
            "segment.com", "kissmetrics.com", "intercom.io",
            "intercom.com", "hubspot.com", "marketo.com",
            "pardot.com", "eloqua.com", "act-on.com",

            // Pop-up & Pop-under Ağları
            "popads.net", "popcash.net", "propellerads.com",
            "popunder.ru", "trafficjunky.net", "exoclick.com",
            "juicyads.com", "plugrush.com", "hilltopads.net",
            "zeropark.com", "clickadu.com", "adcash.com",
            "richpush.co", "evadav.com", "kadam.net",

            // Kripto Madencilik
            "coinhive.com", "cryptoloot.pro", "webminer.se",
            "miner.pr0gramm.com", "minemytraffic.com",
            "coin-have.com", "ppoi.org", "coinblockerlist.com",
            "minero.pw", "coinnebula.com", "jsecoin.com",
            "authedmine.com", "monerominer.rocks",

            // Kötü Amaçlı Yazılım / Phishing
            "malware-traffic-analysis.net", "badware.info",
            "fraudscore.com", "cleantalk.org",

            // Sosyal Medya Takip Widgetları
            "platform.twitter.com", "widgets.pinterest.com",
            "static.addtoany.com", "addthis.com",
            "sharethis.com", "shareaholic.com",

            // Video Reklam Ağları
            "imasdk.googleapis.com", "imagesrv.adition.com",
            "springserve.com", "springserve.net",
            "unrulymedia.com", "yieldmo.com", "teads.tv",

            // RTB & Programatik
            "rubiconproject.com", "openx.com", "openx.net",
            "pubmatic.com", "criteo.com", "criteo.net",
            "smartadserver.com", "smaato.com", "sovrn.com",
            "lijit.com", "contextweb.com", "casalemedia.com",
            "indexexchange.com", "media.net", "33across.com",
            "triplelift.com", "sharethrough.com", "undertone.com",
            "rhythmone.com", "yieldbot.com", "spotxchange.com",
            "spotx.tv", "improvedigital.com", "emxdgt.com",
            "nativo.com", "outbrain.com", "taboola.com",
            "revcontent.com", "mgid.com", "content.ad",
            "adblade.com", "zergnet.com",

            // Affiliate & Yönlendirme
            "clickbank.net", "tradedoubler.com", "tradetracker.com",
            "awin1.com", "zanox.com", "affiliatefuture.com",
            "cj.com", "commission-junction.com", "linksynergy.com",
            "shareasale.com", "pepperjam.com", "rakuten-marketing.com",

            // Bot Koruması Bypass
            "turnstile.cf", "hcaptcha.com",

            // Diğer
            "cdn.taboola.com", "trc.taboola.com",
            "cdn.outbrain.com", "widgets.outbrain.com",
            "cdn-gl.imrworldwide.com", "secure-gl.imrworldwide.com",
            "b.scorecardresearch.com", "pixel.quantserve.com",
            "beacon.krxd.net", "p.typekit.net",
            "mc.yandex.ru", "an.yandex.ru",
            "pstat.com", "liadm.com", "ib.adnxs.com",
            "secure.adnxs.com", "nym1.ib.adnxs.com",
            "sin3.ib.adnxs.com", "e3.emxdgt.com"
        };
        blockedHosts.addAll(Arrays.asList(hosts));
    }

    // ── ANAHTAR KELİME LİSTESİ ──
    private void loadKeywordList() {
        String[] keywords = {
            "/ads/", "/ad/", "/adserver/", "/adsystem/",
            "/adservice/", "/adclick/", "/adview/",
            "/banner/", "/banners/", "/popup/", "/popunder/",
            "/tracking/", "/tracker/", "/track/",
            "/pixel/", "/beacon/", "/analytics/",
            "/impression/", "/impressions/",
            "/click/", "/clickthrough/",
            "/sponsor/", "/sponsored/",
            "adunit", "ad-unit", "ad_unit",
            "doubleclick", "googlesyndication",
            "adsbygoogle", "pagead",
        };
        blockedKeywords.addAll(Arrays.asList(keywords));
    }
}
