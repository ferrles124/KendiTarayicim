package com.kendi.tarayicim;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private CharacterView characterView;
    private EditText inputEmail, inputPassword;
    private ImageButton btnTogglePassword;
    private View btnLogin, btnRegister;
    private TextView loginError;

    private SupabaseAuth auth;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = new SupabaseAuth(this);

        // Zaten giriş yapılmışsa direkt ana ekrana
        if (auth.isLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }

    private void initViews() {
        characterView     = findViewById(R.id.character_view);
        inputEmail        = findViewById(R.id.input_email);
        inputPassword     = findViewById(R.id.input_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnLogin          = findViewById(R.id.btn_login);
        btnRegister       = findViewById(R.id.btn_register);
        loginError        = findViewById(R.id.login_error);
    }

    private void setupListeners() {

        // ── E-POSTA ODAK ──
        inputEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) characterView.setState(CharacterView.STATE_EMAIL_FOCUS);
            else          characterView.setState(CharacterView.STATE_IDLE);
        });

        // ── ŞİFRE ODAK ──
        inputPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                characterView.setState(passwordVisible
                        ? CharacterView.STATE_PASSWORD_SHOW
                        : CharacterView.STATE_PASSWORD_HIDE);
            } else {
                characterView.setState(CharacterView.STATE_IDLE);
            }
        });

        // ── ŞİFRE GÖSTER/GİZLE ──
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                inputPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_on);
                if (inputPassword.hasFocus())
                    characterView.setState(CharacterView.STATE_PASSWORD_SHOW);
            } else {
                inputPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
                if (inputPassword.hasFocus())
                    characterView.setState(CharacterView.STATE_PASSWORD_HIDE);
            }
            // İmleç sona gitsin
            inputPassword.setSelection(inputPassword.getText().length());
        });

        // ── DOKUNMA → GÖZ TAKİBİ ──
        // ── DOKUNMA → GÖZ TAKİBİ ──
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setOnTouchListener((v, event) -> {
            if (characterView.getWidth() == 0) return false;

        // Karakterin ekrandaki merkez koordinatları
            int[] loc = new int[2];
            characterView.getLocationOnScreen(loc);
            float charCX = loc[0] + characterView.getWidth()  * 0.5f;
            float charCY = loc[1] + characterView.getHeight() * 0.5f;

        // Normalize et: -1..1
            float normX = (event.getRawX() - charCX) / (rootView.getWidth()  * 0.45f);
            float normY = (event.getRawY() - charCY) / (rootView.getHeight() * 0.45f);

        // Sınırla
            normX = Math.max(-1f, Math.min(1f, normX));
            normY = Math.max(-1f, Math.min(1f, normY));

            characterView.updateGaze(normX, normY);
            return false;
        });
        // ── GİRİŞ YAP ──
        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass  = inputPassword.getText().toString();
            if (!validate(email, pass)) return;

            setLoading(true);
            auth.signIn(email, pass, new SupabaseAuth.AuthCallback() {
                @Override public void onSuccess(String e) {
                    runOnUiThread(() -> goToMain());
                }
                @Override public void onError(String msg) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError(translateError(msg));
                    });
                }
            });
        });

        // ── KAYIT OL ──
        btnRegister.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass  = inputPassword.getText().toString();
            if (!validate(email, pass)) return;

            setLoading(true);
            auth.signUp(email, pass, new SupabaseAuth.AuthCallback() {
                @Override public void onSuccess(String e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError("Kayıt başarılı! Şimdi giriş yapabilirsin.");
                        loginError.setTextColor(0xFF4CAF50);
                        loginError.setVisibility(View.VISIBLE);
                    });
                }
                @Override public void onError(String msg) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError(translateError(msg));
                    });
                }
            });
        });
    }

    private boolean validate(String email, String pass) {
        if (email.isEmpty() || !email.contains("@")) {
            showError("Geçerli bir e-posta gir");
            return false;
        }
        if (pass.length() < 6) {
            showError("Şifre en az 6 karakter olmalı");
            return false;
        }
        loginError.setVisibility(View.GONE);
        return true;
    }

    private void setLoading(boolean loading) {
        btnLogin.setAlpha(loading ? 0.5f : 1f);
        btnRegister.setAlpha(loading ? 0.5f : 1f);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
    }

    private void showError(String msg) {
        loginError.setTextColor(0xFFFF5252);
        loginError.setText(msg);
        loginError.setVisibility(View.VISIBLE);
    }

    private String translateError(String raw) {
        if (raw == null) return "Bir hata oluştu";
        String r = raw.toLowerCase();
        if (r.contains("invalid login"))       return "E-posta veya şifre hatalı";
        if (r.contains("email not confirmed")) return "E-postanı doğrula";
        if (r.contains("already registered"))  return "Bu e-posta zaten kayıtlı";
        if (r.contains("password"))            return "Şifre en az 6 karakter olmalı";
        if (r.contains("bağlantı"))            return "İnternet bağlantısı yok";
        return raw;
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
