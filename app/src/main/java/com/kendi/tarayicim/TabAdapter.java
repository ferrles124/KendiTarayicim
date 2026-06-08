package com.kendi.tarayicim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.TabViewHolder> {

    public interface OnTabActionListener {
        void onTabSelected(int index);
        void onTabClosed(int index);
    }

    private final List<TabManager.Tab> tabs;
    private int selectedIndex;
    private final OnTabActionListener listener;

    public TabAdapter(List<TabManager.Tab> tabs, int selectedIndex, OnTabActionListener listener) {
        this.tabs = tabs;
        this.selectedIndex = selectedIndex;
        this.listener = listener;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tab, parent, false);
        return new TabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        TabManager.Tab tab = tabs.get(position);
        String title = (tab.title != null && !tab.title.isEmpty()) ? tab.title : "Yeni Sekme";
        holder.tvTitle.setText(title);

        boolean isActive = (position == selectedIndex);

        // Aktif sekme stili
        holder.itemView.setBackgroundResource(
                isActive ? R.drawable.tab_bg_active : R.drawable.tab_bg_inactive
        );
        holder.tvTitle.setTextColor(isActive ? 0xFFF0F0F0 : 0xFF666666);
        holder.tabAccent.setVisibility(isActive ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onTabSelected(holder.getAdapterPosition()));
        holder.btnClose.setOnClickListener(v -> listener.onTabClosed(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    static class TabViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        ImageButton btnClose;
        View tabAccent;

        TabViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tab_title);
            btnClose = itemView.findViewById(R.id.btn_close_tab);
            tabAccent = itemView.findViewById(R.id.tab_accent);
        }
    }
}
