package com.kendi.tarayicim;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface ActionListener { void onAction(String action); }

    // View tipleri
    static final int TYPE_HEADER  = 0;
    static final int TYPE_TOGGLE  = 1;
    static final int TYPE_CHOICE  = 2;
    static final int TYPE_BUTTON  = 3;
    static final int TYPE_INFO    = 4;

    static class Item {
        int type;
        String title, subtitle, key, action, value;
        boolean defaultVal;
        String[] choices;

        static Item header(String title) {
            Item i = new Item(); i.type = TYPE_HEADER; i.title = title; return i;
        }
        static Item toggle(String title, String subtitle, String key, boolean def) {
            Item i = new Item(); i.type = TYPE_TOGGLE; i.title = title;
            i.subtitle = subtitle; i.key = key; i.defaultVal = def; return i;
        }
        static Item choice(String title, String subtitle, String key, String defVal, String... choices) {
            Item i = new Item(); i.type = TYPE_CHOICE; i.title = title;
            i.subtitle = subtitle; i.key = key; i.value = defVal; i.choices = choices; return i;
        }
        static Item button(String title, String subtitle, String action) {
            Item i = new Item(); i.type = TYPE_BUTTON; i.title = title;
            i.subtitle = subtitle; i.action = action; return i;
        }
        static Item info(String title, String value) {
            Item i = new Item(); i.type = TYPE_INFO; i.title = title; i.value = value; return i;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final SharedPreferences prefs;
    private final ActionListener listener;
    private final Context context;

    public SettingsAdapter(Context ctx, SharedPreferences prefs, ActionListener listener) {
        this.context = ctx;
        this.prefs = prefs;
        this.listener = listener;
        buildItems();
    }

    private void buildItems() {
        // 🌐 GENEL
        items.add(Item.header("🌐  Genel"));
        items.add(Item.choice("Arama Motoru", "Varsayılan arama motoru",
                SettingsActivity.KEY_SEARCH_ENGINE, "Google",
                "Google", "Bing", "DuckDuckGo", "Yandex", "Ecosia"));
        items.add(Item.choice("Ana Sayfa", "Başlangıçta açılacak sayfa",
                SettingsActivity.KEY_HOMEPAGE, "Yeni Sekme",
                "Yeni Sekme", "Google", "Özel URL"));
        items.add(Item.toggle("Masaüstü Modu", "Siteleri masaüstü olarak aç",
                SettingsActivity.KEY_DESKTOP_MODE, false));
        items.add(Item.choice("Yazı Boyutu", "Sayfa yazı tipi boyutu",
                SettingsActivity.KEY_FONT_SIZE, "Normal",
                "Küçük", "Normal", "Büyük", "Çok Büyük"));

        // 🔒 GİZLİLİK & GÜVENLİK
        items.add(Item.header("🔒  Gizlilik & Güvenlik"));
        items.add(Item.toggle("Reklam Engelleme", "Reklamları ve izleyicileri engelle",
                SettingsActivity.KEY_ADBLOCK, true));
        items.add(Item.toggle("Tracker Engelleme", "3. taraf takip scriptlerini engelle",
                SettingsActivity.KEY_TRACKER, true));
        items.add(Item.toggle("HTTPS Zorlaması", "Her zaman güvenli bağlantı kullan",
                SettingsActivity.KEY_HTTPS, true));
        items.add(Item.toggle("3. Taraf Çerez Engelle", "Reklam çerezlerini engelle",
                SettingsActivity.KEY_COOKIE_BLOCK, true));
        items.add(Item.toggle("JavaScript", "Sitelerde JavaScript çalıştır",
                SettingsActivity.KEY_JAVASCRIPT, true));
        items.add(Item.toggle("Konum İzni", "Sitelerin konumuna erişimine izin ver",
                SettingsActivity.KEY_LOCATION, false));
        items.add(Item.toggle("Kamera İzni", "Sitelerin kameraya erişimine izin ver",
                SettingsActivity.KEY_CAMERA, false));
        items.add(Item.toggle("Mikrofon İzni", "Sitelerin mikrofona erişimine izin ver",
                SettingsActivity.KEY_MICROPHONE, false));
        items.add(Item.toggle("Pop-up Engelle", "Açılır pencereleri engelle",
                SettingsActivity.KEY_POPUP_BLOCK, true));
        items.add(Item.toggle("Yönlendirme Engelle", "Otomatik yönlendirmeleri engelle",
                SettingsActivity.KEY_REDIRECT_BLOCK, false));
        items.add(Item.toggle("Parmak İzi Koruması", "User-Agent'ı gizle",
                SettingsActivity.KEY_FINGERPRINT, false));

        // 🗑 VERİ & ÖNBELLEK
        items.add(Item.header("🗑  Veri & Önbellek"));
        items.add(Item.choice("Otomatik Geçmiş Silme", "Geçmiş ne zaman silinsin",
                SettingsActivity.KEY_AUTO_CLEAR, "Hiçbir zaman",
                "Hiçbir zaman", "7 günde bir", "30 günde bir", "Uygulama kapanınca"));
        items.add(Item.button("Geçmişi Temizle", "Tüm gezinti geçmişini sil", "clear_history"));
        items.add(Item.button("Çerezleri Temizle", "Tüm çerezleri sil", "clear_cookies"));
        items.add(Item.button("Önbelleği Temizle", "Önbelleği temizle", "clear_cache"));
        items.add(Item.button("Tüm Verileri Temizle", "Geçmiş, çerez ve önbelleği temizle", "clear_all"));

        // 🎨 GÖRÜNÜM
        items.add(Item.header("🎨  Görünüm"));
        items.add(Item.toggle("Tam Ekran", "Durum çubuğunu gizle",
                SettingsActivity.KEY_FULLSCREEN, false));

        // 📥 İNDİRMELER
        items.add(Item.header("📥  İndirmeler"));
        items.add(Item.toggle("İndirmeden Önce Sor", "Her indirmede konum seçimi yap",
                SettingsActivity.KEY_DOWNLOAD_ASK, true));
        items.add(Item.toggle("İndirme Bildirimleri", "İndirme tamamlandığında bildir",
                SettingsActivity.KEY_DOWNLOAD_NOTIFY, true));

        // 🔑 ŞİFRELER & FORMLAR
        items.add(Item.header("🔑  Şifreler & Formlar"));
        items.add(Item.toggle("Şifre Kaydet", "Sitelerin şifrelerini kaydet",
                SettingsActivity.KEY_SAVE_PASSWORD, false));
        items.add(Item.toggle("Otomatik Doldur", "Formları otomatik doldur",
                SettingsActivity.KEY_AUTOFILL, false));

        // 🔔 BİLDİRİMLER
        items.add(Item.header("🔔  Bildirimler"));
        items.add(Item.toggle("Site Bildirimleri", "Sitelerin bildirim göndermesine izin ver",
                SettingsActivity.KEY_NOTIF_SITES, false));
        items.add(Item.toggle("İndirme Bildirimleri", "İndirme bildirimleri",
                SettingsActivity.KEY_NOTIF_DOWNLOAD, true));

        // ℹ️ HAKKINDA
        items.add(Item.header("ℹ️  Hakkında"));
        items.add(Item.info("Versiyon", "2.0.0"));
        items.add(Item.info("Geliştirici", "KendiTarayicim"));
        items.add(Item.button("Gizlilik Politikası", "Gizlilik politikamızı görüntüle", "privacy_policy"));
        items.add(Item.button("Geri Bildirim Gönder", "Görüşlerinizi paylaşın", "feedback"));
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderVH(inf.inflate(R.layout.item_settings_header, parent, false));
            case TYPE_TOGGLE:
                return new ToggleVH(inf.inflate(R.layout.item_settings_toggle, parent, false));
            case TYPE_CHOICE:
                return new ChoiceVH(inf.inflate(R.layout.item_settings_choice, parent, false));
            case TYPE_BUTTON:
                return new ButtonVH(inf.inflate(R.layout.item_settings_button, parent, false));
            default:
                return new InfoVH(inf.inflate(R.layout.item_settings_info, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        switch (item.type) {
            case TYPE_HEADER:
                ((HeaderVH) holder).title.setText(item.title);
                break;

            case TYPE_TOGGLE:
                ToggleVH tvh = (ToggleVH) holder;
                tvh.title.setText(item.title);
                tvh.subtitle.setText(item.subtitle);
                tvh.toggle.setChecked(prefs.getBoolean(item.key, item.defaultVal));
                tvh.toggle.setOnCheckedChangeListener(null);
                tvh.toggle.setOnCheckedChangeListener((btn, checked) ->
                        prefs.edit().putBoolean(item.key, checked).apply());
                tvh.itemView.setOnClickListener(v -> tvh.toggle.toggle());
                break;

            case TYPE_CHOICE:
                ChoiceVH cvh = (ChoiceVH) holder;
                cvh.title.setText(item.title);
                String current = prefs.getString(item.key, item.value);
                cvh.value.setText(current);
                cvh.itemView.setOnClickListener(v ->
                        showChoiceDialog(item, cvh.value, position));
                break;

            case TYPE_BUTTON:
                ButtonVH bvh = (ButtonVH) holder;
                bvh.title.setText(item.title);
                bvh.subtitle.setText(item.subtitle);
                bvh.itemView.setOnClickListener(v -> listener.onAction(item.action));
                break;

            case TYPE_INFO:
                InfoVH ivh = (InfoVH) holder;
                ivh.title.setText(item.title);
                ivh.value.setText(item.value);
                break;
        }
    }

    private void showChoiceDialog(Item item, TextView valueView, int position) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(context,
                        android.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(item.title);
        builder.setItems(item.choices, (dialog, which) -> {
            String chosen = item.choices[which];
            prefs.edit().putString(item.key, chosen).apply();
            valueView.setText(chosen);
        });
        builder.show();
    }

    @Override public int getItemViewType(int position) { return items.get(position).type; }
    @Override public int getItemCount() { return items.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView title;
        HeaderVH(View v) { super(v); title = v.findViewById(R.id.settings_header_title); }
    }
    static class ToggleVH extends RecyclerView.ViewHolder {
        TextView title, subtitle; SwitchCompat toggle;
        ToggleVH(View v) { super(v);
            title = v.findViewById(R.id.settings_title);
            subtitle = v.findViewById(R.id.settings_subtitle);
            toggle = v.findViewById(R.id.settings_toggle); }
    }
    static class ChoiceVH extends RecyclerView.ViewHolder {
        TextView title, value;
        ChoiceVH(View v) { super(v);
            title = v.findViewById(R.id.settings_title);
            value = v.findViewById(R.id.settings_value); }
    }
    static class ButtonVH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ButtonVH(View v) { super(v);
            title = v.findViewById(R.id.settings_title);
            subtitle = v.findViewById(R.id.settings_subtitle); }
    }
    static class InfoVH extends RecyclerView.ViewHolder {
        TextView title, value;
        InfoVH(View v) { super(v);
            title = v.findViewById(R.id.settings_title);
            value = v.findViewById(R.id.settings_value); }
    }
}
