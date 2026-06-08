package com.kendi.tarayicim;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.List;

public class BottomSheetPanel {

    public interface OnItemClickListener {
        void onItemClick(String url);
        void onItemDelete(int id);
    }

    // ── GEÇMİŞ PANELİ ──
    public static void showHistory(Context context,
                                   List<BrowserDatabaseHelper.HistoryItem> items,
                                   BrowserDatabaseHelper db,
                                   OnItemClickListener listener) {

        BottomSheetDialog dialog = new BottomSheetDialog(context,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        View root = buildPanelView(context, "Geçmiş", items.isEmpty());
        RecyclerView rv = root.findViewById(R.id.panel_recycler);
        TextView btnClear = root.findViewById(R.id.btn_panel_action);

        if (!items.isEmpty()) {
            btnClear.setVisibility(View.VISIBLE);
            btnClear.setText("Tümünü Temizle");
            btnClear.setOnClickListener(v -> {
                db.clearHistory();
                dialog.dismiss();
            });
        }

        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new HistoryAdapter(items, new HistoryAdapter.Listener() {
            @Override public void onClick(String url) { listener.onItemClick(url); dialog.dismiss(); }
            @Override public void onDelete(int id, int pos) {
                listener.onItemDelete(id);
                items.remove(pos);
                rv.getAdapter().notifyItemRemoved(pos);
            }
        }));

        dialog.setContentView(root);
        dialog.show();
    }

    // ── YER İMLERİ PANELİ ──
    public static void showBookmarks(Context context,
                                     List<BrowserDatabaseHelper.BookmarkItem> items,
                                     OnItemClickListener listener) {

        BottomSheetDialog dialog = new BottomSheetDialog(context,
                com.google.android.material.R.style.Theme_Material3_Light_BottomSheetDialog);

        View root = buildPanelView(context, "Yer İmleri", items.isEmpty());
        RecyclerView rv = root.findViewById(R.id.panel_recycler);

        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new BookmarkAdapter(items, new BookmarkAdapter.Listener() {
            @Override public void onClick(String url) { listener.onItemClick(url); dialog.dismiss(); }
            @Override public void onDelete(int id, int pos) {
                listener.onItemDelete(id);
                items.remove(pos);
                rv.getAdapter().notifyItemRemoved(pos);
            }
        }));

        dialog.setContentView(root);
        dialog.show();
    }

    private static View buildPanelView(Context context, String title, boolean isEmpty) {
        View root = LayoutInflater.from(context).inflate(R.layout.panel_bottom_sheet, null);
        TextView tvTitle = root.findViewById(R.id.panel_title);
        TextView tvEmpty = root.findViewById(R.id.panel_empty);
        tvTitle.setText(title);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        return root;
    }

    // ── GEÇMİŞ ADAPTÖRÜ ──
    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        interface Listener { void onClick(String url); void onDelete(int id, int pos); }
        private final List<BrowserDatabaseHelper.HistoryItem> items;
        private final Listener listener;

        HistoryAdapter(List<BrowserDatabaseHelper.HistoryItem> items, Listener l) {
            this.items = items; this.listener = l;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_panel_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BrowserDatabaseHelper.HistoryItem item = items.get(pos);
            h.title.setText(item.title);
            h.subtitle.setText(item.timestamp);
            h.itemView.setOnClickListener(v -> listener.onClick(item.url));
            h.btnDelete.setOnClickListener(v -> listener.onDelete(item.id, h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle, btnDelete;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.row_title);
                subtitle = v.findViewById(R.id.row_subtitle);
                btnDelete = v.findViewById(R.id.row_delete);
            }
        }
    }

    // ── YER İMİ ADAPTÖRÜ ──
    static class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.VH> {
        interface Listener { void onClick(String url); void onDelete(int id, int pos); }
        private final List<BrowserDatabaseHelper.BookmarkItem> items;
        private final Listener listener;

        BookmarkAdapter(List<BrowserDatabaseHelper.BookmarkItem> items, Listener l) {
            this.items = items; this.listener = l;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_panel_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            BrowserDatabaseHelper.BookmarkItem item = items.get(pos);
            h.title.setText(item.title);
            h.subtitle.setText(item.url);
            h.itemView.setOnClickListener(v -> listener.onClick(item.url));
            h.btnDelete.setOnClickListener(v -> listener.onDelete(item.id, h.getAdapterPosition()));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView title, subtitle, btnDelete;
            VH(View v) {
                super(v);
                title = v.findViewById(R.id.row_title);
                subtitle = v.findViewById(R.id.row_subtitle);
                btnDelete = v.findViewById(R.id.row_delete);
            }
        }
    }
}
