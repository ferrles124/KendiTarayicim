package com.kendi.tarayicim;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.*;
import java.util.Random;

public class CharacterView extends View {

    // ── DIŞ STATE ──
    public static final int STATE_IDLE          = 0;
    public static final int STATE_EMAIL_FOCUS   = 1;
    public static final int STATE_PASSWORD_HIDE = 2;
    public static final int STATE_PASSWORD_SHOW = 3;
    public static final int STATE_ERROR         = 4;
    public static final int STATE_SUCCESS       = 5;

    private int formState = STATE_IDLE;

    // ── KARAKTERLER ──
    // 0=Turuncu(yuvarlak), 1=Mor(dikdörtgen), 2=Siyah(ince), 3=Sarı(küçük)
    private static final int PUF  = 0;
    private static final int VIGO = 1;
    private static final int NOX  = 2;
    private static final int ZIP  = 3;

    // Flat renkler — videodaki gibi
    private static final int COLOR_PUF  = 0xFFFF6B35;
    private static final int COLOR_VIGO = 0xFF7C3AED;
    private static final int COLOR_NOX  = 0xFF1A1A1A;
    private static final int COLOR_ZIP  = 0xFFFFD60A;

    // Göz boyutu & pozisyon (her karakter için ayrı)
    // Gözler: küçük siyah oval, başka bir şey yok
    private float[] eyeGazeX = {0f, 0f, 0f, 0f};
    private float[] eyeGazeY = {0f, 0f, 0f, 0f};

    // Pozisyon (ev konumundan sapma)
    private float[] posX = {0f, 0f, 0f, 0f};
    private float[] posY = {0f, 0f, 0f, 0f};
    private float[] velX = {0f, 0f, 0f, 0f};
    private float[] velY = {0f, 0f, 0f, 0f};

    // Animasyon parametreleri
    private float bobPhase     = 0f;
    private float noxLean      = 0f;   // Siyah öne eğilme (email)
    private float noxTurn      = 0f;   // Siyah arkasını dönme (şifre)
    private float vigoLean     = 0f;   // Mor öne eğilme
    private float zipBob       = 0f;   // Sarı ekstra zıplama
    private float pufSquish    = 0f;   // Turuncu ezilme (hata)
    private float groupShock   = 0f;   // Toplu irkilme
    private float entryProgress= 0f;   // Giriş animasyonu (siyah tepeden düşer)

    // Gaze
    private float targetGX = 0f, targetGY = 0f;
    private float curGX    = 0f, curGY    = 0f;

    // Otonom
    private final Handler h   = new Handler(Looper.getMainLooper());
    private final Random  rnd = new Random();
    private boolean running   = false;

    // Dokunma
    private int   dragChar    = -1;
    private float lastTX, lastTY;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── KURUCU ──
    public CharacterView(Context c) { super(c); init(); }
    public CharacterView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setOnTouchListener(this::onTouch);
    }

    // ── BAŞLAT ──
    public void startSystem() {
        if (running) return;
        running = true;
        // Açılış: siyah karakter tepeden düşer
        anim(0f, 1f, 700, new OvershootInterpolator(0.8f), v -> entryProgress = v);
        startRenderLoop();
        h.postDelayed(this::scheduleAutoScene, 5000);
    }

    public void stopSystem() {
        running = false;
        h.removeCallbacksAndMessages(null);
    }

    private void startRenderLoop() {
        h.post(new Runnable() {
            @Override public void run() {
                if (!running) return;
                bobPhase += 0.04f;
                if (bobPhase > (float)(Math.PI * 2)) bobPhase -= (float)(Math.PI * 2);
                // Smooth gaze
                curGX += (targetGX - curGX) * 0.10f;
                curGY += (targetGY - curGY) * 0.10f;
                // Her karakter için göz gaze'i smooth güncelle
                for (int i = 0; i < 4; i++) {
                    eyeGazeX[i] += (curGX - eyeGazeX[i]) * 0.15f;
                    eyeGazeY[i] += (curGY - eyeGazeY[i]) * 0.15f;
                }
                // Fizik
                for (int i = 0; i < 4; i++) {
                    if (dragChar == i) continue;
                    velX[i] += -posX[i] * 0.1f;
                    velY[i] += -posY[i] * 0.1f;
                    velX[i] *= 0.75f;
                    velY[i] *= 0.75f;
                    posX[i] += velX[i];
                    posY[i] += velY[i];
                }
                invalidate();
                h.postDelayed(this, 16);
            }
        });
    }

    // ── DIŞ API ──
    public void setState(int s) {
        if (formState == s) return;
        formState = s;
        switch (s) {
            case STATE_IDLE:          playIdle();        break;
            case STATE_EMAIL_FOCUS:   playEmailFocus();  break;
            case STATE_PASSWORD_HIDE: playPassHide();    break;
            case STATE_PASSWORD_SHOW: playPassShow();    break;
            case STATE_ERROR:         playError();       break;
            case STATE_SUCCESS:       playSuccess();     break;
        }
    }

    public void updateGaze(float nx, float ny) {
        targetGX = nx * 8f;
        targetGY = ny * 5f;
    }

    // ── SAHNELER ──
    private void playIdle() {
        anim(noxLean,   0f, 450, new OvershootInterpolator(), v -> noxLean   = v);
        anim(noxTurn,   0f, 500, new OvershootInterpolator(), v -> noxTurn   = v);
        anim(vigoLean,  0f, 400, new DecelerateInterpolator(),v -> vigoLean  = v);
        anim(pufSquish, 0f, 400, new OvershootInterpolator(), v -> pufSquish = v);
    }

    private void playEmailFocus() {
        // Siyah ve mor öne eğilir, forma merakla bakar
        anim(noxLean,  1f, 500, new OvershootInterpolator(1.3f), v -> noxLean  = v);
        anim(vigoLean, 0.6f, 450, new OvershootInterpolator(1.0f), v -> vigoLean = v);
        anim(noxTurn,  0f, 400, new DecelerateInterpolator(),      v -> noxTurn  = v);
        // Sarı hafif zıplar (heyecan)
        anim(0f, 1f, 280, new OvershootInterpolator(2.5f), v -> zipBob = v);
        h.postDelayed(() -> anim(zipBob, 0f, 350, new DecelerateInterpolator(), v -> zipBob = v), 300);
    }

    private void playPassHide() {
        // Siyah arkasını döner (gizlilik için)
        anim(noxTurn,  1f, 600, new OvershootInterpolator(0.7f), v -> noxTurn  = v);
        anim(noxLean,  0f, 350, new DecelerateInterpolator(),     v -> noxLean  = v);
        anim(vigoLean, 0f, 350, new DecelerateInterpolator(),     v -> vigoLean = v);
    }

    private void playPassShow() {
        // Siyah hâlâ arkası dönük ama hafif eğilir — çaktırmadan bakmaya çalışır
        anim(noxTurn,  0.7f, 400, new DecelerateInterpolator(),    v -> noxTurn  = v);
        anim(noxLean,  0.5f, 450, new OvershootInterpolator(1.5f), v -> noxLean  = v);
    }

    private void playError() {
        // Hepsi irkilir — squish animasyonu
        anim(0f, 1f, 120, new AccelerateInterpolator(), v -> groupShock = v);
        anim(0f, 1f, 120, new AccelerateInterpolator(), v -> pufSquish  = v);
        h.postDelayed(() -> {
            anim(groupShock, 0f, 500, new BounceInterpolator(), v -> groupShock = v);
            anim(pufSquish,  0f, 500, new BounceInterpolator(), v -> pufSquish  = v);
        }, 150);
        h.postDelayed(() -> { if (formState == STATE_ERROR) setState(STATE_IDLE); }, 1800);
    }

    private void playSuccess() {
        // Sırayla zıplarlar — dalga gibi
        h.postDelayed(() -> anim(0f, 1f, 300, new OvershootInterpolator(2f), v -> {
            posY[PUF] = -v * 25f; }), 0);
        h.postDelayed(() -> anim(0f, 1f, 300, new OvershootInterpolator(2f), v -> {
            posY[VIGO] = -v * 28f; }), 120);
        h.postDelayed(() -> anim(0f, 1f, 300, new OvershootInterpolator(2f), v -> {
            posY[NOX] = -v * 22f; }), 240);
        h.postDelayed(() -> anim(0f, 1f, 300, new OvershootInterpolator(2f), v -> {
            posY[ZIP] = -v * 30f; }), 360);
        // Geri in
        h.postDelayed(() -> {
            for (int i = 0; i < 4; i++) { posY[i] = 0f; velY[i] = 2f; }
        }, 700);
    }

    // ── OTONOM SAHNELER ──
    private void scheduleAutoScene() {
        if (!running) return;
        long delay = 7000 + (long)(rnd.nextFloat() * 12000);
        h.postDelayed(() -> {
            if (formState == STATE_IDLE) playAutoScene();
            scheduleAutoScene();
        }, delay);
    }

    private void playAutoScene() {
        int scene = rnd.nextInt(5);
        switch (scene) {
            case 0: // Siyah Mora doğru eğilir, Mor fark eder geri iter
                anim(0f, 0.7f, 400, new OvershootInterpolator(), v -> noxLean = v);
                h.postDelayed(() -> {
                    anim(0f, 0.5f, 300, new OvershootInterpolator(2f), v -> vigoLean = v * (-1));
                    h.postDelayed(() -> {
                        anim(noxLean,  0f, 350, new OvershootInterpolator(), v -> noxLean  = v);
                        anim(vigoLean, 0f, 350, new OvershootInterpolator(), v -> vigoLean = v);
                    }, 800);
                }, 500);
                break;

            case 1: // Sarı zıplar, diğerleri bakar
                anim(0f, 1f, 280, new OvershootInterpolator(3f), v -> zipBob = v);
                h.postDelayed(() -> anim(zipBob, 0f, 400, new BounceInterpolator(), v -> zipBob = v), 300);
                // Diğerleri bir an bakar (gaze sağa kayar)
                float savedGX = targetGX;
                targetGX = 8f;
                h.postDelayed(() -> targetGX = savedGX, 1200);
                break;

            case 2: // Turuncu hafifçe sola sağa sallanır
                anim(0f, 1f, 500, new AccelerateDecelerateInterpolator(), v -> pufSquish = v * 0.3f);
                h.postDelayed(() -> anim(pufSquish, 0f, 500, new DecelerateInterpolator(), v -> pufSquish = v), 550);
                break;

            case 3: // Siyah aniden dik oturur (posture)
                velY[NOX] = -3f;
                h.postDelayed(() -> velY[NOX] = 0f, 100);
                break;

            case 4: // Hepsi bir yöne bakar (ekran dışı bir şey)
                float prevGX = targetGX, prevGY = targetGY;
                targetGX = -10f; targetGY = -5f;
                h.postDelayed(() -> { targetGX = prevGX; targetGY = prevGY; }, 1500);
                break;
        }
    }

    // ── ÇİZİM ──
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();

        // Bob
        float b0 = (float)Math.sin(bobPhase)        * 3.5f;
        float b1 = (float)Math.sin(bobPhase + 0.8f) * 3.0f;
        float b2 = (float)Math.sin(bobPhase + 1.6f) * 4.0f;
        float b3 = (float)Math.sin(bobPhase + 2.4f) * 2.5f;

        // Irkilme
        float shockScale = 1f + groupShock * 0.08f;
        canvas.save();
        canvas.scale(shockScale, shockScale, w * 0.5f, h * 0.8f);

        // Çizim sırası — arka > ön
        drawPuf (canvas, w, h, b0);
        drawVigo(canvas, w, h, b1);
        drawNox (canvas, w, h, b2);
        drawZip (canvas, w, h, b3);

        canvas.restore();
    }

    // ── TURUNCU (yarım daire, en önde solda) ──
    private void drawPuf(Canvas canvas, float w, float h, float bob) {
        float r  = w * 0.35f;
        float cx = w * 0.32f + posX[PUF];
        float cy = h * 0.88f + bob + posY[PUF];

        // Squish: yatay genişler, dikey daralır (hata)
        float scaleX = 1f + pufSquish * 0.15f;
        float scaleY = 1f - pufSquish * 0.10f;

        canvas.save();
        canvas.scale(scaleX, scaleY, cx, cy);

        // Gövde — sadece düz renk daire
        p.setColor(COLOR_PUF);
        p.setStyle(Paint.Style.FILL);
        // Yarım daire: üst yarısı görünür, alt yarısı zemine gömülü
        canvas.drawArc(new RectF(cx-r, cy-r, cx+r, cy+r), 180f, 180f, false, p);
        // Alt düz kap
        canvas.drawRect(cx-r, cy, cx+r, cy+r*0.3f, p);

        // Gözler — sadece 2 küçük siyah oval
        float eyeR = r * 0.095f;
        float eyeY = cy - r * 0.32f;
        float eyeSp = r * 0.30f;
        float ox = Math.max(-eyeSp*0.35f, Math.min(eyeSp*0.35f, eyeGazeX[PUF]));
        float oy = Math.max(-eyeR*0.4f,   Math.min(eyeR*0.4f,   eyeGazeY[PUF]));
        p.setColor(0xFF111111);
        canvas.drawOval(cx-eyeSp-eyeR+ox, eyeY-eyeR*0.7f+oy,
                        cx-eyeSp+eyeR+ox, eyeY+eyeR*0.7f+oy, p);
        canvas.drawOval(cx+eyeSp-eyeR+ox, eyeY-eyeR*0.7f+oy,
                        cx+eyeSp+eyeR+ox, eyeY+eyeR*0.7f+oy, p);

        // Küçük gülümseme (normal durumda)
        if (formState == STATE_IDLE || formState == STATE_EMAIL_FOCUS) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(r * 0.045f);
            p.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawArc(new RectF(cx-r*0.22f, eyeY+eyeR, cx+r*0.22f, eyeY+eyeR*3f),
                    0f, 180f, false, p);
            p.setStyle(Paint.Style.FILL);
        }

        canvas.restore();
    }

    // ── MOR (büyük dikdörtgen, sol-orta) ──
    private void drawVigo(Canvas canvas, float w, float h, float bob) {
        float bw = w * 0.30f;
        float bh = h * 0.52f;
        float cx = w * 0.40f + posX[VIGO];
        float by = h * 0.88f - bh + bob + posY[VIGO];

        // Öne eğilme
        float leanAngle = vigoLean * 8f;
        canvas.save();
        canvas.rotate(leanAngle, cx, by + bh);

        // Gövde
        p.setColor(COLOR_VIGO);
        p.setStyle(Paint.Style.FILL);
        RectF body = new RectF(cx - bw/2f, by, cx + bw/2f, by + bh);
        canvas.drawRoundRect(body, bw*0.18f, bw*0.18f, p);

        // Gözler
        float eyeW = bw * 0.13f, eyeH = bw * 0.09f;
        float eyeY = by + bh * 0.30f;
        float eyeSp = bw * 0.20f;
        float ox = Math.max(-eyeSp*0.4f, Math.min(eyeSp*0.4f, eyeGazeX[VIGO]));
        float oy = Math.max(-eyeH*0.5f,  Math.min(eyeH*0.5f,  eyeGazeY[VIGO]));
        p.setColor(0xFF111111);

        if (formState == STATE_ERROR) {
            // Hata: gözler V şeklinde (kaşlar aşağı) — oval gözler biraz sıkışır
            canvas.drawOval(cx-eyeSp-eyeW, eyeY, cx-eyeSp+eyeW, eyeY+eyeH*1.4f, p);
            canvas.drawOval(cx+eyeSp-eyeW, eyeY, cx+eyeSp+eyeW, eyeY+eyeH*1.4f, p);
        } else {
            canvas.drawOval(cx-eyeSp-eyeW+ox, eyeY+oy, cx-eyeSp+eyeW+ox, eyeY+eyeH*2f+oy, p);
            canvas.drawOval(cx+eyeSp-eyeW+ox, eyeY+oy, cx+eyeSp+eyeW+ox, eyeY+eyeH*2f+oy, p);
        }

        canvas.restore();
    }

    // ── SİYAH (ince uzun dikdörtgen, ortada) ──
    private void drawNox(Canvas canvas, float w, float h, float bob) {
        float bw = w * 0.19f;
        float bh = h * 0.44f;
        float cx = w * 0.58f + posX[NOX];

        // Öne eğilme
        float leanOffY = -noxLean * bh * 0.12f;
        float leanAngle = noxLean * 14f;

        // Arkasını dönme: cx sağa kayar ve döner
        float turnOffX = noxTurn * bw * 0.2f;
        float turnAngle = noxTurn * 175f; // 0=öne, 175=neredeyse tamamen arka

        float by = h * 0.88f - bh + bob + leanOffY + posY[NOX];

        canvas.save();

        if (noxTurn > 0.05f) {
            // Dönerken: yatay sıkışır (perspektif etkisi)
            float squeezed = 1f - noxTurn * 0.85f;
            canvas.scale(squeezed, 1f, cx + turnOffX, by + bh/2f);
        } else {
            canvas.rotate(leanAngle, cx, by + bh);
        }

        // Gövde
        p.setColor(COLOR_NOX);
        p.setStyle(Paint.Style.FILL);
        RectF body = new RectF(cx + turnOffX - bw/2f, by,
                               cx + turnOffX + bw/2f, by + bh);
        canvas.drawRoundRect(body, bw*0.30f, bw*0.30f, p);

        // Gözler — sadece arkasını dönmemişse
        if (noxTurn < 0.5f) {
            float eyeAlpha = 1f - noxTurn * 2f;
            float eyeW = bw * 0.19f, eyeH = bw * 0.13f;
            float eyeY = by + bh * 0.27f;
            float eyeSp = bw * 0.22f;
            float ox = Math.max(-eyeSp*0.4f, Math.min(eyeSp*0.4f, eyeGazeX[NOX]));
            float oy = Math.max(-eyeH*0.4f,  Math.min(eyeH*0.4f,  eyeGazeY[NOX]));
            p.setColor(0xFF111111);
            p.setAlpha((int)(255 * eyeAlpha));
            canvas.drawOval(cx+turnOffX-eyeSp-eyeW+ox, eyeY+oy,
                            cx+turnOffX-eyeSp+eyeW+ox, eyeY+eyeH*2f+oy, p);
            canvas.drawOval(cx+turnOffX+eyeSp-eyeW+ox, eyeY+oy,
                            cx+turnOffX+eyeSp+eyeW+ox, eyeY+eyeH*2f+oy, p);
            p.setAlpha(255);
        }

        canvas.restore();
    }

    // ── SARI (küçük dikdörtgen, sağda) ──
    private void drawZip(Canvas canvas, float w, float h, float bob) {
        float bw = w * 0.22f;
        float bh = h * 0.36f;
        float cx = w * 0.76f + posX[ZIP];
        float by = h * 0.88f - bh + bob - zipBob * 18f + posY[ZIP];

        canvas.save();

        // Gövde
        p.setColor(COLOR_ZIP);
        p.setStyle(Paint.Style.FILL);
        RectF body = new RectF(cx - bw/2f, by, cx + bw/2f, by + bh);
        canvas.drawRoundRect(body, bw*0.22f, bw*0.22f, p);

        // Gözler
        float eyeW = bw * 0.13f, eyeH = bw * 0.09f;
        float eyeY = by + bh * 0.28f;
        float eyeSp = bw * 0.18f;
        float ox = Math.max(-eyeSp*0.4f, Math.min(eyeSp*0.4f, eyeGazeX[ZIP]));
        float oy = Math.max(-eyeH*0.5f,  Math.min(eyeH*0.5f,  eyeGazeY[ZIP]));
        p.setColor(0xFF111111);
        canvas.drawOval(cx-eyeSp-eyeW+ox, eyeY+oy, cx-eyeSp+eyeW+ox, eyeY+eyeH*2f+oy, p);
        canvas.drawOval(cx+eyeSp-eyeW+ox, eyeY+oy, cx+eyeSp+eyeW+ox, eyeY+eyeH*2f+oy, p);

        canvas.restore();
    }

    // ── DOKUNMA ──
    private boolean onTouch(View v, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragChar = findChar(e.getX(), e.getY());
                lastTX = e.getX(); lastTY = e.getY();
                if (dragChar >= 0) {
                    // Hafif sıkışma
                    velY[dragChar] = -2f;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragChar >= 0) {
                    float dx = e.getX() - lastTX;
                    float dy = e.getY() - lastTY;
                    float maxD = getWidth() * 0.22f;
                    posX[dragChar] = Math.max(-maxD, Math.min(maxD, posX[dragChar] + dx));
                    posY[dragChar] = Math.max(-maxD, Math.min(maxD, posY[dragChar] + dy));
                    velX[dragChar] = dx * 0.4f;
                    velY[dragChar] = dy * 0.4f;
                    lastTX = e.getX(); lastTY = e.getY();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragChar >= 0) {
                    // Bırakınca fırla ve geri döner
                    velX[dragChar] *= 1.8f;
                    velY[dragChar] *= 1.8f;
                    dragChar = -1;
                }
                return true;
        }
        return false;
    }

    private int findChar(float tx, float ty) {
        float w = getWidth(), h = getHeight();
        float[][] centers = {
            {w*0.32f, h*0.75f},
            {w*0.40f, h*0.62f},
            {w*0.58f, h*0.66f},
            {w*0.76f, h*0.70f}
        };
        float[] radii = {w*0.32f, w*0.18f, w*0.12f, w*0.14f};
        for (int i = 3; i >= 0; i--) { // öndekini önce
            float dx = tx - (centers[i][0] + posX[i]);
            float dy = ty - (centers[i][1] + posY[i]);
            if (Math.sqrt(dx*dx+dy*dy) < radii[i]) return i;
        }
        return -1;
    }

    // ── YARDIMCI ──
    interface Setter { void set(float v); }

    private void anim(float from, float to, int ms, TimeInterpolator interp, Setter s) {
        ValueAnimator va = ValueAnimator.ofFloat(from, to);
        va.setDuration(ms);
        va.setInterpolator(interp);
        va.addUpdateListener(a -> s.set((float) a.getAnimatedValue()));
        va.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSystem();
    }
}
