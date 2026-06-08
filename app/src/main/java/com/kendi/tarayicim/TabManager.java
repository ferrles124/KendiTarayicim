package com.kendi.tarayicim;

import android.content.Context;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.List;

public class TabManager {
    private final List<WebView> tabList;
    private int currentTabIndex;
    private final Context context;

    public TabManager(Context context) {
        this.context = context;
        this.tabList = new ArrayList<>();
        this.currentTabIndex = -1;
    }

    public WebView createNewTab() {
        WebView newWebView = new WebView(context);
        tabList.add(newWebView);
        currentTabIndex = tabList.size() - 1;
        return newWebView;
    }

    public WebView getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabList.size()) {
            return tabList.get(currentTabIndex);
        }
        return null;
    }

    public void switchTab(int index) {
        if (index >= 0 && index < tabList.size()) {
            this.currentTabIndex = index;
        }
    }

    public void closeTab(int index) {
        if (index >= 0 && index < tabList.size()) {
            WebView webView = tabList.remove(index);
            webView.destroy();
            if (currentTabIndex >= tabList.size()) {
                currentTabIndex = tabList.size() - 1;
            }
        }
    }

    public int getTabCount() {
        return tabList.size();
    }
}
