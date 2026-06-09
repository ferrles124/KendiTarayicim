package com.kendi.tarayicim;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView logo = findViewById(R.id.splash_logo);
        TextView tagline = findViewById(R.id.splash_tagline);

        // Logo fade-in
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(600);
        fadeIn.setFillAfter(true);
        logo.startAnimation(fadeIn);

        // Tagline biraz gecikmeli
        AlphaAnimation fadeIn2 = new AlphaAnimation(0f, 1f);
        fadeIn2.setDuration(600);
        fadeIn2.setStartOffset(400);
        fadeIn2.setFillAfter(true);
        tagline.startAnimation(fadeIn2);

        // 2 saniye sonra MainActivity'ye geç
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }
}
