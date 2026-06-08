package com.kendi.tarayicim;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;
import java.util.Random;

public class LiveScoreEngine {
    private final Handler handler;
    private Runnable runnable;
    private final Random random;

    public LiveScoreEngine() {
        this.handler = new Handler(Looper.getMainLooper());
        this.random = new Random();
    }

    public void startLiveUpdates(WebView targetWebView) {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (targetWebView != null) {
                    int gsScore = random.nextInt(4);
                    int fbScore = random.nextInt(4);
                    String js = "javascript:if(document.getElementById('gs-score')){ " +
                            "document.getElementById('gs-score').innerText='" + gsScore + "'; " +
                            "document.getElementById('fb-score').innerText='" + fbScore + "'; " +
                            "document.getElementById('match-status').innerText='Canlı - Dk " + (random.nextInt(45) + 45) + "'; }";
                    targetWebView.evaluateJavascript(js, null);
                }
                handler.postDelayed(this, 10000); // Her 10 saniyede bir canlı skor güncellemesi fırlatır
            }
        };
        handler.post(runnable);
    }

    public void stopUpdates() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
