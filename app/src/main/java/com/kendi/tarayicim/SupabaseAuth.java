package com.kendi.tarayicim;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.io.IOException;

public class SupabaseAuth {

    private static final String URL     = "https://bsqdlbsmeyggugdtxvxm.supabase.co";
    private static final String API_KEY = "sb_publishable_P2l7AnwaanF-72tJoKAPMA_tRbqO4C3";
    private static final String PREFS   = "auth_prefs";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_EMAIL = "user_email";

    public interface AuthCallback {
        void onSuccess(String email);
        void onError(String message);
    }

    private final OkHttpClient client = new OkHttpClient();
    private final SharedPreferences prefs;

    public SupabaseAuth(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── KAYIT OL ──
    public void signUp(String email, String password, AuthCallback cb) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        post("/auth/v1/signup", body.toString(), cb);
    }

    // ── GİRİŞ YAP ──
    public void signIn(String email, String password, AuthCallback cb) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        Request req = new Request.Builder()
                .url(URL + "/auth/v1/token?grant_type=password")
                .addHeader("apikey", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(),
                        MediaType.parse("application/json")))
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onError("Bağlantı hatası: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                String json = resp.body().string();
                if (resp.isSuccessful()) {
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    String token = obj.get("access_token").getAsString();
                    prefs.edit()
                            .putString(KEY_TOKEN, token)
                            .putString(KEY_EMAIL, email)
                            .apply();
                    cb.onSuccess(email);
                } else {
                    String msg = parseError(json);
                    cb.onError(msg);
                }
            }
        });
    }

    // ── ÇIKIŞ YAP ──
    public void signOut() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_EMAIL).apply();
    }

    // ── OTURUM KONTROLÜ ──
    public boolean isLoggedIn() {
        return prefs.getString(KEY_TOKEN, null) != null;
    }

    public String getSavedEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    // ── YARDIMCI ──
    private void post(String path, String bodyJson, AuthCallback cb) {
        Request req = new Request.Builder()
                .url(URL + path)
                .addHeader("apikey", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyJson,
                        MediaType.parse("application/json")))
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onError("Bağlantı hatası: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                String json = resp.body().string();
                if (resp.isSuccessful()) {
                    cb.onSuccess("");
                } else {
                    cb.onError(parseError(json));
                }
            }
        });
    }

    private String parseError(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("error_description"))
                return obj.get("error_description").getAsString();
            if (obj.has("msg"))
                return obj.get("msg").getAsString();
            if (obj.has("message"))
                return obj.get("message").getAsString();
        } catch (Exception ignored) {}
        return "Bir hata oluştu";
    }
}
