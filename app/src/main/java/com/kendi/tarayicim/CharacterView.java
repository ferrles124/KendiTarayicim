package com.kendi.tarayicim;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class CharacterView extends View {

    // ── DURUMLAR ──
    public static final int STATE_IDLE          = 0;
    public static final int STATE_EMAIL_FOCUS   = 1;
    public static final int STATE_PASSWORD_HIDE = 2;
    public static final int STATE_PASSWORD_SHOW = 3;

    private int state = STATE_IDLE;

    // ── GÖZ TAKİBİ ──
    private float gazeX = 0f, gazeY = 0f;     // hedef
    private float eyeOffsetX = 0f, eyeOffsetY = 0f; // mevcut

    // ── KAPI / GİZLEME ANİMASYONU ──
    private float coverProgress = 0f; // 0=açık, 1=kapalı

    // ── BOYALAR ──
    private final Paint bodyPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyeWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint eyePupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallCharPaint= new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yellowPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint purplePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orangePaint   = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ValueAnimator coverAnimator;
    private ValueAnimator eyeAnimator;

    public CharacterView(Context ctx) { super(ctx); init(); }
    public CharacterView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        bodyPaint.setColor(0xFF1A1A1A);
        eyeWhitePaint.setColor(Color.WHITE);
        eyePupilPaint.setColor(0xFF111111);
        handPaint.setColor(0xFF1A1A1A);
        smallCharPaint.setColor(0xFF2A2A2A);
        yellowPaint.setColor(0xFFFFD600);
        purplePaint.setColor(0xFF9C27B0);
        orangePaint.setColor(0xFFFF6D00);
    }

    // ── DIŞ API ──
    public void setState(int newState) {
        if (state == newState) return;
        state = newState;
        animateToState();
    }

    public void setGaze(float x, float y) {
        gazeX = x;
        gazeY = y;
        if (state == STATE_IDLE || state == STATE_EMAIL_FOCUS) {
            animateEyeToGaze();
        }
    }

    // ── ANİMASYONLAR ──
    private void animateToState() {
        float targetCover = (state == STATE_PASSWORD_HIDE) ? 1f : 0f;

        if (coverAnimator != null) coverAnimator.cancel();
        coverAnimator = ValueAnimator.ofFloat(coverProgress, targetCover);
        coverAnimator.setDuration(400);
        coverAnimator.setInterpolator(new OvershootInterpolator(1.2f));
        coverAnimator.addUpdateListener(a -> {
            coverProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        coverAnimator.start();
        invalidate();
    }

    private void animateEyeToGaze() {
        if (eyeAnimator != null) eyeAnimator.cancel();
        float[] from = {eyeOffsetX, eyeOffsetY};
        float[] to   = {gazeX * 6f, gazeY * 4f};
        eyeAnimator = ValueAnimator.ofFloat(0f, 1f);
        eyeAnimator.setDuration(120);
        eyeAnimator.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            eyeOffsetX = from[0] + (to[0] - from[0]) * t;
            eyeOffsetY = from[1] + (to[1] - from[1]) * t;
            invalidate();
        });
        eyeAnimator.start();
    }

    // ── ÇİZİM ──
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float cx = w * 0.5f;

        // Küçük karakterler (üstten alta: sarı, mor, turuncu)
        float smallY = h * 0.08f;
        float smallSize = w * 0.18f;
        float smallSpacing = h * 0.14f;

        float smallAlpha = 1f - coverProgress * 0.6f;
        yellowPaint.setAlpha((int)(255 * smallAlpha));
        purplePaint.setAlpha((int)(255 * smallAlpha));
        orangePaint.setAlpha((int)(255 * smallAlpha));

        // Sarı karakter
        drawSmallChar(canvas, cx, smallY + smallSize * 0.5f, smallSize, yellowPaint);
        // Mor karakter
        drawSmallChar(canvas, cx, smallY + smallSpacing + smallSize * 0.5f, smallSize, purplePaint);
        // Turuncu karakter
        drawSmallChar(canvas, cx, smallY + smallSpacing * 2f + smallSize * 0.5f, smallSize, orangePaint);

        // Ana büyük karakter
        float mainCY   = h * 0.72f;
        float mainSize = w * 0.72f;

        // Gizleme animasyonu: yukarı fırlama
        float bodyLift = coverProgress * h * 0.28f;
        canvas.save();
        canvas.translate(0, -bodyLift);
        drawMainCharacter(canvas, cx, mainCY, mainSize);
        canvas.restore();
    }

    private void drawSmallChar(Canvas canvas, float cx, float cy, float size, Paint paint) {
        // Yuvarlak gövde
        RectF body = new RectF(cx - size * 0.5f, cy - size * 0.5f,
                cx + size * 0.5f, cy + size * 0.5f);
        canvas.drawRoundRect(body, size * 0.4f, size * 0.4f, paint);

        // Küçük beyaz gözler
        float eyeR = size * 0.12f;
        float eyeY = cy - size * 0.05f;
        eyeWhitePaint.setAlpha(200);
        canvas.drawCircle(cx - size * 0.18f, eyeY, eyeR, eyeWhitePaint);
        canvas.drawCircle(cx + size * 0.18f, eyeY, eyeR, eyeWhitePaint);

        // Küçük pupil
        eyePupilPaint.setAlpha(200);
        float pupilR = eyeR * 0.5f;
        canvas.drawCircle(cx - size * 0.18f, eyeY, pupilR, eyePupilPaint);
        canvas.drawCircle(cx + size * 0.18f, eyeY, pupilR, eyePupilPaint);
    }

    private void drawMainCharacter(Canvas canvas, float cx, float cy, float size) {
        float halfW = size * 0.5f;
        float halfH = size * 0.42f;

        // Gövde
        RectF body = new RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH * 0.9f);
        canvas.drawRoundRect(body, size * 0.35f, size * 0.35f, bodyPaint);

        if (state == STATE_PASSWORD_HIDE || coverProgress > 0.05f) {
            drawCoveringHands(canvas, cx, cy, size);
        } else {
            drawEyes(canvas, cx, cy, size);
        }
    }

    private void drawEyes(Canvas canvas, float cx, float cy, float size) {
        float eyeSpacing = size * 0.22f;
        float eyeY = cy - size * 0.05f;
        float eyeRW = size * 0.16f;
        float eyeRH = size * 0.20f;

        // Sol göz beyazı
        RectF leftEye = new RectF(cx - eyeSpacing - eyeRW, eyeY - eyeRH,
                cx - eyeSpacing + eyeRW, eyeY + eyeRH);
        canvas.drawOval(leftEye, eyeWhitePaint);

        // Sağ göz beyazı
        RectF rightEye = new RectF(cx + eyeSpacing - eyeRW, eyeY - eyeRH,
                cx + eyeSpacing + eyeRW, eyeY + eyeRH);
        canvas.drawOval(rightEye, eyeWhitePaint);

        // Pupiller — gaze takibi
        float pupilR = eyeRW * 0.52f;
        float maxOffset = eyeRW - pupilR - 2f;
        float ox = Math.max(-maxOffset, Math.min(maxOffset, eyeOffsetX));
        float oy = Math.max(-maxOffset * 0.7f, Math.min(maxOffset * 0.7f, eyeOffsetY));

        canvas.drawCircle(cx - eyeSpacing + ox, eyeY + oy, pupilR, eyePupilPaint);
        canvas.drawCircle(cx + eyeSpacing + ox, eyeY + oy, pupilR, eyePupilPaint);

        // Parlama noktası
        Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shinePaint.setColor(Color.WHITE);
        float shineR = pupilR * 0.3f;
        canvas.drawCircle(cx - eyeSpacing + ox + pupilR * 0.3f, eyeY + oy - pupilR * 0.35f,
                shineR, shinePaint);
        canvas.drawCircle(cx + eyeSpacing + ox + pupilR * 0.3f, eyeY + oy - pupilR * 0.35f,
                shineR, shinePaint);
    }

    private void drawCoveringHands(Canvas canvas, float cx, float cy, float size) {
        float progress = coverProgress;

        // Eller yukarıdan inerek gözleri kapatıyor
        float handY = cy - size * 0.30f - (1f - progress) * size * 0.5f;
        float handW = size * 0.38f;
        float handH = size * 0.22f;

        // Sol el
        RectF leftHand = new RectF(cx - handW * 1.1f, handY - handH,
                cx - handW * 0.05f, handY + handH);
        canvas.drawRoundRect(leftHand, handH * 0.6f, handH * 0.6f, handPaint);

        // Sağ el
        RectF rightHand = new RectF(cx + handW * 0.05f, handY - handH,
                cx + handW * 1.1f, handY + handH);
        canvas.drawRoundRect(rightHand, handH * 0.6f, handH * 0.6f, handPaint);

        // Şifre gösteriliyorsa eller biraz ayrılıp gözler arasından bakıyor
        if (state == STATE_PASSWORD_SHOW) {
            float peekOffset = size * 0.08f;
            leftHand.offset(-peekOffset, 0);
            rightHand.offset(peekOffset, 0);
            canvas.drawRoundRect(leftHand,  handH * 0.6f, handH * 0.6f, handPaint);
            canvas.drawRoundRect(rightHand, handH * 0.6f, handH * 0.6f, handPaint);

            // Aralıktan bakan gözler
            Paint peekEye = new Paint(Paint.ANTI_ALIAS_FLAG);
            peekEye.setColor(Color.WHITE);
            float peekEyeR = size * 0.09f;
            canvas.drawCircle(cx - size * 0.18f, handY, peekEyeR, peekEye);
            canvas.drawCircle(cx + size * 0.18f, handY, peekEyeR, peekEye);
            eyePupilPaint.setAlpha(255);
            canvas.drawCircle(cx - size * 0.18f, handY, peekEyeR * 0.5f, eyePupilPaint);
            canvas.drawCircle(cx + size * 0.18f, handY, peekEyeR * 0.5f, eyePupilPaint);
        }
    }
}
