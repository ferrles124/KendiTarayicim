package com.kendi.tarayicim;

import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private CharacterView characterView;
    private EditText      inputEmail, inputPassword;
    private ImageButton   btnTogglePassword;
    private View          btnLogin, btnRegister;
    private TextView      loginError;

    private SupabaseAuth auth;
    private boolean      passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = new SupabaseAuth(this);

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

        characterView.startSystem();
    }

    private void setupListeners() {

        inputEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) characterView.setState(CharacterView.STATE_EMAIL_FOCUS);
            else          characterView.setState(CharacterView.STATE_IDLE);
        });

        inputPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                characterView.setState(passwordVisible
                        ? CharacterView.STATE_PASSWORD_SHOW
                        : CharacterView.STATE_PASSWORD_HIDE);
            } else {
                characterView.setState(CharacterView.STATE_IDLE);
            }
        });

        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                inputPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_on);
                if (inputPassword.hasFocus())
                    characterView.setState(CharacterView.STATE_PASSWORD_SHOW);
            } else {
                inputPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
                if (inputPassword.hasFocus())
                    characterView.setState(CharacterView.STATE_PASSWORD_HIDE);
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        View rootView = getWindow().getDecorView().getRootView();
        rootView.setOnTouchListener((v, event) -> {
            if (characterView.getWidth() == 0) return false;
            int[] loc = new int[2];
            characterView.getLocationOnScreen(loc);
            float charCX = loc[0] + characterView.getWidth()  * 0.5f;
            float charCY = loc[1] + characterView.getHeight() * 0.5f;
            float normX  = (event.getRawX() - charCX) / (rootView.getWidth()  * 0.45f);
            float normY  = (event.getRawY() - charCY) / (rootView.getHeight() * 0.45f);
            normX = Math.max(-1f, Math.min(1f, normX));
            normY = Math.max(-1f, Math.min(1f, normY));
            characterView.updateGaze(normX, normY);
            return false;
        });

        btnLogin.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass  = inputPassword.getText().toString();
            if (!validate(email, pass)) return;

            setLoading(true);
            auth.signIn(email, pass, new SupabaseAuth.AuthCallback() {
                @Override public void onSuccess(String e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        characterView.setState(CharacterView.STATE_SUCCESS);
                        new Handler().postDelayed(() -> goToMain(), 700);
                    });
                }
                @Override public void onError(String msg) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError(translateError(msg));
                        characterView.setState(CharacterView.STATE_ERROR);
                    });
                }
            });
        });

        btnRegister.setOnClickListener(v -> {
            String email = inputEmail.getText().toString().trim();
            String pass  = inputPassword.getText().toString();
            if (!validate(email, pass)) return;

            setLoading(true);
            auth.signUp(email, pass, new SupabaseAuth.AuthCallback() {
                @Override public void onSuccess(String e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        loginError.setTextColor(0xFF4CAF50);
                        loginError.setText("Kayit basarili! Simdi giris yapabilirsin.");
                        loginError.setVisibility(View.VISIBLE);
                        characterView.setState(CharacterView.STATE_SUCCESS);
                    });
                }
                @Override public void onError(String msg) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showError(translateError(msg));
                        characterView.setState(CharacterView.STATE_ERROR);
                    });
                }
            });
        });
    }

    private boolean validate(String email, String pass) {
        if (email.isEmpty() || !email.contains("@")) {
            showError("Gecerli bir e-posta gir");
            characterView.setState(CharacterView.STATE_ERROR);
            return false;
        }
        if (pass.length() < 6) {
            showError("Sifre en az 6 karakter olmali");
            characterView.setState(CharacterView.STATE_ERROR);
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
        if (raw == null) return "Bir hata olustu";
        String r = raw.toLowerCase();
        if (r.contains("invalid login"))       return "E-posta veya sifre hatali";
        if (r.contains("email not confirmed")) return "E-postani dogrula";
        if (r.contains("already registered"))  return "Bu e-posta zaten kayitli";
        if (r.contains("password"))            return "Sifre en az 6 karakter olmali";
        if (r.contains("connection"))          return "Internet baglantisi yok";
        return raw;
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (characterView != null) characterView.stopSystem();
    }
}
