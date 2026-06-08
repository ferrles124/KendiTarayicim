package com.kendi.tarayicim;

import android.content.Context;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.List;

public class TabManager {

    public static class Tab {
        public WebView webView;
        public String title;
        public String url;

        public Tab(WebView webView) {
            this.webView = webView;
            this.title = "Yeni Sekme";
            this.url = "";
        }
    }

    private final List<Tab> tabList;
    private int currentTabIndex;
    private final Context context;
    private OnTabChangeListener listener;

    public interface OnTabChangeListener {
        void onTabChanged(int index, WebView webView);
        void onTabCountChanged(int count);
    }

    public TabManager(Context context) {
        this.context = context;
        this.tabList = new ArrayList<>();
        this.currentTabIndex = -1;
    }

    public void setOnTabChangeListener(OnTabChangeListener listener) {
        this.listener = listener;
    }

    public WebView createNewTab() {
        WebView newWebView = new WebView(context);
        Tab tab = new Tab(newWebView);
        tabList.add(tab);
        currentTabIndex = tabList.size() - 1;
        if (listener != null) {
            listener.onTabCountChanged(tabList.size());
            listener.onTabChanged(currentTabIndex, newWebView);
        }
        return newWebView;
    }

    public WebView switchTab(int index) {
        if (index >= 0 && index < tabList.size()) {
            currentTabIndex = index;
            WebView wv = tabList.get(index).webView;
            if (listener != null) listener.onTabChanged(index, wv);
            return wv;
        }
        return null;
    }

    public void closeTab(int index) {
        if (index >= 0 && index < tabList.size()) {
            tabList.get(index).webView.destroy();
            tabList.remove(index);
            if (currentTabIndex >= tabList.size()) {
                currentTabIndex = tabList.size() - 1;
            }
            if (listener != null) listener.onTabCountChanged(tabList.size());
        }
    }

    public void updateTabInfo(String url, String title) {
        if (currentTabIndex >= 0 && currentTabIndex < tabList.size()) {
            Tab tab = tabList.get(currentTabIndex);
            tab.url = url != null ? url : "";
            tab.title = (title != null && !title.isEmpty()) ? title : url;
        }
    }

    public List<Tab> getAllTabs() {
        return tabList;
    }

    public WebView getCurrentWebView() {
        if (currentTabIndex >= 0 && currentTabIndex < tabList.size()) {
            return tabList.get(currentTabIndex).webView;
        }
        return null;
    }

    public int getCurrentTabIndex() {
        return currentTabIndex;
    }

    public int getTabCount() {
        return tabList.size();
    }
}
