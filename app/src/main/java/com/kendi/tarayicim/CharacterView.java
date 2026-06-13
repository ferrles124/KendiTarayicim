package com.kendi.tarayicim;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class CharacterView extends View {

    public static final int STATE_IDLE          = 0;
    public static final int STATE_EMAIL_FOCUS   = 1;
    public static final int STATE_PASSWORD_HIDE = 2;
    public static final int STATE_PASSWORD_SHOW = 3;

    private int state = STATE_IDLE;

    // Göz takibi — hedef ve mevcut (smooth interpolation)
    private float targetGazeX = 0f, targetGazeY = 0f;
    private float currentGazeX = 0f, currentGazeY = 0f;

    // El kapama animasyonu 0=açık 1=kapalı
    private float handProgress = 0f;
    // Şifre göster — eller ayrılma 0=kapalı 1=ayrık
    private float peekProgress = 0f;
    // Küçük karakterlerin merak animasyonu
    private float curiousProgress = 0f;

    // Boyalar
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Animatörler
    private ValueAnimator handAnim;
    private ValueAnimator peekAnim;
    private ValueAnimator curiousAnim;

    // Smooth gaze için Runnable
    private final Runnable gazeUpdater = new Runnable() {
        @Override public void run() {
            float dx = targetGazeX - currentGazeX;
            float dy = targetGazeY - currentGazeY;
            currentGazeX += dx * 0.12f;
            currentGazeY += dy * 0.12f;
            invalidate();
            if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
                postDelayed(this, 16);
            }
        }
    };

    public CharacterView(Context ctx) { super(ctx); }
    public CharacterView(Context ctx, AttributeSet a) { super(ctx, a); }

    // ── DIŞ API ──
    public void setState(int newState) {
        if (state == newState) return;
        int old = state;
        state = newState;

        switch (newState) {
            case STATE_IDLE:
                animateHand(0f);
                animatePeek(0f);
                animateCurious(0f);
                break;
            case STATE_EMAIL_FOCUS:
                animateHand(0f);
                animatePeek(0f);
                animateCurious(1f);
                break;
            case STATE_PASSWORD_HIDE:
                animateHand(1f);
                animatePeek(0f);
                animateCurious(0f);
                break;
            case STATE_PASSWORD_SHOW:
                animateHand(1f);
                animatePeek(1f);
                animateCurious(0f);
                break;
        }
    }

    public void updateGaze(float normX, float normY) {
        // normX, normY: -1..1 arası normalize edilmiş
        targetGazeX = normX * 9f;
        targetGazeY = normY * 6f;
        removeCallbacks(gazeUpdater);
        post(gazeUpdater);
    }

    // ── ANİMATÖRLER ──
    private void animateHand(float to) {
        if (handAnim != null) handAnim.cancel();
        handAnim = ValueAnimator.ofFloat(handProgress, to);
        handAnim.setDuration(500);
        handAnim.setInterpolator(new OvershootInterpolator(0.8f));
        handAnim.addUpdateListener(a -> { handProgress = (float)a.getAnimatedValue(); invalidate(); });
        handAnim.start();
    }

    private void animatePeek(float to) {
        if (peekAnim != null) peekAnim.cancel();
        peekAnim = ValueAnimator.ofFloat(peekProgress, to);
        peekAnim.setDuration(350);
        peekAnim.setInterpolator(new OvershootInterpolator(1.5f));
        peekAnim.addUpdateListener(a -> { peekProgress = (float)a.getAnimatedValue(); invalidate(); });
        peekAnim.start();
    }

    private void animateCurious(float to) {
        if (curiousAnim != null) curiousAnim.cancel();
        curiousAnim = ValueAnimator.ofFloat(curiousProgress, to);
        curiousAnim.setDuration(400);
        curiousAnim.setInterpolator(new DecelerateInterpolator());
        curiousAnim.addUpdateListener(a -> { curiousProgress = (float)a.getAnimatedValue(); invalidate(); });
        curiousAnim.start();
    }

    // ── ÇİZİM ──
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float cx = w / 2f;

        drawSmallCharacters(canvas, w, h, cx);
        drawMainCharacter(canvas, w, h, cx);
    }

    // ── KÜÇÜK KARAKTERLER ──
    private void drawSmallCharacters(Canvas canvas, float w, float h, float cx) {
        float size   = w * 0.17f;
        float startY = h * 0.04f + size;
        float gap    = h * 0.135f;

        int[][] colors = {
            {0xFFFFD600, 0xFFFFEB3B}, // sarı
            {0xFF7B1FA2, 0xFF9C27B0}, // mor
            {0xFFE65100, 0xFFFF6D00}  // turuncu
        };

        for (int i = 0; i < 3; i++) {
            float cy = startY + gap * i;
            // Merak animasyonu: email focus'ta biraz öne eğiliyorlar
            float lean = curiousProgress * h * 0.018f * (i + 1);
            drawSmallChar(canvas, cx, cy + lean, size, colors[i][0], colors[i][1]);
        }
    }

    private void drawSmallChar(Canvas canvas, float cx, float cy,
                                float size, int colorDark, int colorLight) {
        // Gölge
        shadowPaint.setColor(0x22000000);
        shadowPaint.setMaskFilter(new BlurMaskFilter(size * 0.3f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(cx, cy + size * 0.15f, size * 0.52f, shadowPaint);

        // Gövde — gradient
        p.setShader(new RadialGradient(cx, cy - size * 0.1f, size * 0.7f,
                colorLight, colorDark, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, size * 0.5f, p);
        p.setShader(null);

        // Kulaklar
        p.setColor(colorDark);
        canvas.drawCircle(cx - size * 0.38f, cy - size * 0.3f, size * 0.14f, p);
        canvas.drawCircle(cx + size * 0.38f, cy - size * 0.3f, size * 0.14f, p);

        // Göz beyazları
        float eyeRW = size * 0.14f, eyeRH = size * 0.16f;
        float eyeY  = cy - size * 0.06f;
        p.setColor(Color.WHITE);
        canvas.drawOval(cx - size*0.22f - eyeRW, eyeY - eyeRH,
                        cx - size*0.22f + eyeRW, eyeY + eyeRH, p);
        canvas.drawOval(cx + size*0.22f - eyeRW, eyeY - eyeRH,
                        cx + size*0.22f + eyeRW, eyeY + eyeRH, p);

        // Pupiller — merak animasyonunda aşağı kayıyor
        float pupilDY = curiousProgress * eyeRH * 0.5f;
        p.setColor(0xFF222222);
        float pr = eyeRW * 0.5f;
        canvas.drawCircle(cx - size*0.22f, eyeY + pupilDY, pr, p);
        canvas.drawCircle(cx + size*0.22f, eyeY + pupilDY, pr, p);

        // Parlaklık
        p.setColor(Color.WHITE);
        canvas.drawCircle(cx - size*0.22f + pr*0.4f, eyeY + pupilDY - pr*0.4f, pr*0.32f, p);
        canvas.drawCircle(cx + size*0.22f + pr*0.4f, eyeY + pupilDY - pr*0.4f, pr*0.32f, p);
    }

    // ── ANA KARAKTER ──
    private void drawMainCharacter(Canvas canvas, float w, float h, float cx) {
        float bw   = w * 0.78f;
        float bh   = h * 0.38f;
        float by   = h * 0.72f;

        // Gölge
        shadowPaint.setColor(0x33000000);
        shadowPaint.setMaskFilter(new BlurMaskFilter(bw * 0.2f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawOval(cx - bw*0.45f, by + bh*0.35f,
                        cx + bw*0.45f, by + bh*0.55f, shadowPaint);

        // Gövde — koyu gradient
        RectF body = new RectF(cx - bw/2f, by - bh/2f, cx + bw/2f, by + bh/2f);
        p.setShader(new RadialGradient(cx, by - bh*0.1f, bw * 0.7f,
                0xFF2C2C2C, 0xFF111111, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, bw * 0.38f, bw * 0.38f, p);
        p.setShader(null);

        // Kulaklar
        p.setColor(0xFF1A1A1A);
        canvas.drawCircle(cx - bw*0.42f, by - bh*0.28f, bw*0.1f, p);
        canvas.drawCircle(cx + bw*0.42f, by - bh*0.28f, bw*0.1f, p);

        // Gözler ya da eller
        if (handProgress < 0.02f) {
            drawMainEyes(canvas, cx, by, bw, bh);
        } else if (peekProgress > 0.05f) {
            drawPeekingEyes(canvas, cx, by, bw, bh);
            drawHands(canvas, cx, by, bw, bh);
        } else {
            drawHands(canvas, cx, by, bw, bh);
        }
    }

    private void drawMainEyes(Canvas canvas, float cx, float cy, float bw, float bh) {
        float eyeSpacing = bw * 0.21f;
        float eyeY  = cy - bh * 0.05f;
        float erw   = bw * 0.155f;
        float erh   = bw * 0.19f;

        // Beyazlar
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        canvas.drawOval(cx-eyeSpacing-erw, eyeY-erh, cx-eyeSpacing+erw, eyeY+erh, p);
        canvas.drawOval(cx+eyeSpacing-erw, eyeY-erh, cx+eyeSpacing+erw, eyeY+erh, p);

        // Gaze sınırlandırma
        float maxX = erw  * 0.42f;
        float maxY = erh  * 0.38f;
        float ox = Math.max(-maxX, Math.min(maxX, currentGazeX));
        float oy = Math.max(-maxY, Math.min(maxY, currentGazeY));

        // Pupiller
        float pr = erw * 0.54f;
        p.setColor(0xFF1A1A1A);
        canvas.drawCircle(cx - eyeSpacing + ox, eyeY + oy, pr, p);
        canvas.drawCircle(cx + eyeSpacing + ox, eyeY + oy, pr, p);

        // İç parlak nokta
        p.setColor(Color.WHITE);
        canvas.drawCircle(cx - eyeSpacing + ox + pr*0.32f, eyeY + oy - pr*0.36f, pr*0.28f, p);
        canvas.drawCircle(cx + eyeSpacing + ox + pr*0.32f, eyeY + oy - pr*0.36f, pr*0.28f, p);

        // Mavi iris halkası
        p.setColor(0x554D9EFF);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(pr * 0.25f);
        canvas.drawCircle(cx - eyeSpacing + ox, eyeY + oy, pr * 0.72f, p);
        canvas.drawCircle(cx + eyeSpacing + ox, eyeY + oy, pr * 0.72f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawHands(Canvas canvas, float cx, float cy, float bw, float bh) {
        // Eller yanlarda başlayıp ortaya doğru kayıyor
        float hw    = bw * 0.36f;
        float hh    = bh * 0.24f;
        float handY = cy - bh * 0.06f;

        // handProgress: 0=yanda, 1=gözlerin önünde
        float leftStartX  = cx - bw * 0.55f;
        float leftEndX    = cx - hw * 0.55f;
        float rightStartX = cx + bw * 0.55f;
        float rightEndX   = cx + hw * 0.55f;

        float lx = leftStartX  + (leftEndX  - leftStartX)  * handProgress;
        float rx = rightStartX + (rightEndX - rightStartX) * handProgress;

        // Sol el gölgesi
        shadowPaint.setMaskFilter(new BlurMaskFilter(hh*0.4f, BlurMaskFilter.Blur.NORMAL));
        shadowPaint.setColor(0x44000000);
        canvas.drawRoundRect(lx - hw, handY - hh + hh*0.1f,
                             lx,      handY + hh + hh*0.1f, hh*0.55f, hh*0.55f, shadowPaint);
        canvas.drawRoundRect(rx,      handY - hh + hh*0.1f,
                             rx + hw, handY + hh + hh*0.1f, hh*0.55f, hh*0.55f, shadowPaint);

        // Sol el
        p.setShader(new LinearGradient(lx - hw, handY - hh, lx, handY + hh,
                0xFF2C2C2C, 0xFF111111, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(lx - hw, handY - hh, lx, handY + hh, hh*0.55f, hh*0.55f, p);

        // Sağ el
        p.setShader(new LinearGradient(rx, handY - hh, rx + hw, handY + hh,
                0xFF111111, 0xFF2C2C2C, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rx, handY - hh, rx + hw, handY + hh, hh*0.55f, hh*0.55f, p);
        p.setShader(null);

        // Parmak çizgileri
        p.setColor(0xFF333333);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f);
        float fingerSpacing = hw * 0.28f;
        for (int i = 1; i <= 2; i++) {
            float fx = lx - hw*0.2f - fingerSpacing * i;
            canvas.drawLine(fx, handY - hh*0.5f, fx, handY + hh*0.5f, p);
            float frx = rx + hw*0.2f + fingerSpacing * i;
            canvas.drawLine(frx, handY - hh*0.5f, frx, handY + hh*0.5f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawPeekingEyes(Canvas canvas, float cx, float cy, float bw, float bh) {
        float handY  = cy - bh * 0.06f;
        float hh     = bh * 0.24f;
        // Gözler eller arasındaki boşluktan bakıyor
        float peekY  = handY - hh * (0.1f + peekProgress * 0.5f);
        float peekER = bw * 0.085f * peekProgress;

        if (peekER < 4f) return;

        float eyeSpacing = bw * 0.21f;
        p.setColor(Color.WHITE);
        canvas.drawCircle(cx - eyeSpacing, peekY, peekER, p);
        canvas.drawCircle(cx + eyeSpacing, peekY, peekER, p);

        p.setColor(0xFF1A1A1A);
        float pr = peekER * 0.55f;
        canvas.drawCircle(cx - eyeSpacing, peekY, pr, p);
        canvas.drawCircle(cx + eyeSpacing, peekY, pr, p);

        p.setColor(Color.WHITE);
        canvas.drawCircle(cx - eyeSpacing + pr*0.3f, peekY - pr*0.3f, pr*0.28f, p);
        canvas.drawCircle(cx + eyeSpacing + pr*0.3f, peekY - pr*0.3f, pr*0.28f, p);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(gazeUpdater);
        if (handAnim   != null) handAnim.cancel();
        if (peekAnim   != null) peekAnim.cancel();
        if (curiousAnim!= null) curiousAnim.cancel();
    }
}
