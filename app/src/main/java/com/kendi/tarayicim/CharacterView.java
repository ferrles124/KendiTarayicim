package com.kendi.tarayicim;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.*;

public class CharacterView extends View {

    public static final int STATE_IDLE          = 0;
    public static final int STATE_EMAIL_FOCUS   = 1;
    public static final int STATE_PASSWORD_HIDE = 2;
    public static final int STATE_PASSWORD_SHOW = 3;
    public static final int STATE_ERROR         = 4;
    public static final int STATE_SUCCESS       = 5;

    private int state = STATE_IDLE;

    // ── GAZE ──
    private float targetGX = 0f, targetGY = 0f;
    private float curGX    = 0f, curGY    = 0f;

    // ── ANİMASYON DEĞERLERİ ──
    private float idleBob       = 0f;   // sürekli sallanma
    private float leanForward   = 0f;   // öne eğilme (email)
    private float orangeTurn    = 0f;   // turuncu arkasını döner (0=öne, 1=arka)
    private float blockerRaise  = 0f;   // mor+sarı engel kaldırır
    private float peekLean      = 0f;   // siyah yan bakar
    private float shockScale    = 1f;   // hata irkilmesi
    private float jumpY         = 0f;   // başarı zıplar
    private float eyebrowRaise  = 0f;   // kaş kaldırma

    private final Paint p  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG); // shadow

    // Animatörler
    private ValueAnimator idleAnim;
    private AnimatorSet   stateAnimSet;

    // Gaze smooth
    private final Runnable gazeRunner = new Runnable() {
        @Override public void run() {
            curGX += (targetGX - curGX) * 0.14f;
            curGY += (targetGY - curGY) * 0.14f;
            invalidate();
            if (Math.abs(targetGX-curGX)>0.3f || Math.abs(targetGY-curGY)>0.3f)
                postDelayed(this, 16);
        }
    };

    public CharacterView(Context c) { super(c); init(); }
    public CharacterView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        sp.setColor(0x30000000);
        startIdleBob();
    }

    // ── SÜREKLİ BOB ANİMASYONU ──
    private void startIdleBob() {
        idleAnim = ValueAnimator.ofFloat(0f, (float)(Math.PI * 2));
        idleAnim.setDuration(2800);
        idleAnim.setRepeatCount(ValueAnimator.INFINITE);
        idleAnim.setInterpolator(new LinearInterpolator());
        idleAnim.addUpdateListener(a -> {
            idleBob = (float) a.getAnimatedValue();
            invalidate();
        });
        idleAnim.start();
    }

    // ── DIŞ API ──
    public void setState(int newState) {
        if (state == newState) return;
        state = newState;
        if (stateAnimSet != null) stateAnimSet.cancel();
        switch (newState) {
            case STATE_IDLE:          animateIdle();        break;
            case STATE_EMAIL_FOCUS:   animateEmailFocus();  break;
            case STATE_PASSWORD_HIDE: animatePassHide();    break;
            case STATE_PASSWORD_SHOW: animatePassShow();    break;
            case STATE_ERROR:         animateError();       break;
            case STATE_SUCCESS:       animateSuccess();     break;
        }
    }

    public void updateGaze(float nx, float ny) {
        targetGX = nx * 10f;
        targetGY = ny *  6f;
        removeCallbacks(gazeRunner);
        post(gazeRunner);
    }

    // ── DURUM ANİMASYONLARI ──
    private void animateIdle() {
        stateAnimSet = buildSet(
            anim(leanForward,  0f, 400, new DecelerateInterpolator(), v -> leanForward  = v),
            anim(orangeTurn,   0f, 500, new OvershootInterpolator(),  v -> orangeTurn   = v),
            anim(blockerRaise, 0f, 400, new DecelerateInterpolator(), v -> blockerRaise = v),
            anim(peekLean,     0f, 300, new DecelerateInterpolator(), v -> peekLean     = v),
            anim(eyebrowRaise, 0f, 300, new DecelerateInterpolator(), v -> eyebrowRaise = v)
        );
        stateAnimSet.start();
    }

    private void animateEmailFocus() {
        // Hepsi öne eğilir, kaşlar yukarı
        stateAnimSet = buildSet(
            anim(leanForward,  1f, 500, new OvershootInterpolator(1.2f), v -> leanForward  = v),
            anim(orangeTurn,   0f, 400, new DecelerateInterpolator(),    v -> orangeTurn   = v),
            anim(blockerRaise, 0f, 300, new DecelerateInterpolator(),    v -> blockerRaise = v),
            anim(eyebrowRaise, 1f, 400, new OvershootInterpolator(),     v -> eyebrowRaise = v)
        );
        stateAnimSet.start();
    }

    private void animatePassHide() {
        // Turuncu arkasını döner, mor+sarı engel çıkarır
        stateAnimSet = buildSet(
            anim(orangeTurn,   1f, 600, new OvershootInterpolator(0.8f), v -> orangeTurn   = v),
            anim(blockerRaise, 1f, 500, new OvershootInterpolator(1.5f), v -> blockerRaise = v),
            anim(leanForward,  0f, 300, new DecelerateInterpolator(),    v -> leanForward  = v),
            anim(peekLean,     0f, 300, new DecelerateInterpolator(),    v -> peekLean     = v),
            anim(eyebrowRaise, 0f, 300, new DecelerateInterpolator(),    v -> eyebrowRaise = v)
        );
        stateAnimSet.start();
    }

    private void animatePassShow() {
        // Siyah yan gözle bakar, diğerleri onu iter
        stateAnimSet = buildSet(
            anim(orangeTurn,   1f, 400, new DecelerateInterpolator(),    v -> orangeTurn   = v),
            anim(peekLean,     1f, 500, new OvershootInterpolator(2f),   v -> peekLean     = v),
            anim(blockerRaise, 1f, 600, new OvershootInterpolator(1.8f), v -> blockerRaise = v),
            anim(eyebrowRaise, 1f, 400, new OvershootInterpolator(),     v -> eyebrowRaise = v)
        );
        stateAnimSet.start();
    }

    private void animateError() {
        // Hepsi irkilir, sonra birbirine şaşkın bakar
        AnimatorSet shock = buildSet(
            anim(shockScale,   1.18f, 150, new OvershootInterpolator(3f), v -> shockScale = v),
            anim(eyebrowRaise, 1f,    150, new AccelerateInterpolator(),   v -> eyebrowRaise = v)
        );
        AnimatorSet settle = buildSet(
            anim(shockScale,   1f, 300, new BounceInterpolator(), v -> shockScale = v)
        );
        settle.setStartDelay(200);
        stateAnimSet = new AnimatorSet();
        stateAnimSet.playSequentially(shock, settle);
        stateAnimSet.start();
        // 1.5sn sonra idle'a dön
        postDelayed(() -> { if (state == STATE_ERROR) setState(STATE_IDLE); }, 1500);
    }

    private void animateSuccess() {
        stateAnimSet = buildSet(
            anim(jumpY,        1f, 400, new OvershootInterpolator(2f),   v -> jumpY = v),
            anim(eyebrowRaise, 1f, 300, new AccelerateInterpolator(),    v -> eyebrowRaise = v)
        );
        stateAnimSet.start();
    }

    // ── ANİMATÖR YARDIMCISI ──
    interface Setter { void set(float v); }

    private ValueAnimator anim(float from, float to, int ms,
                                TimeInterpolator interp, Setter setter) {
        ValueAnimator va = ValueAnimator.ofFloat(from, to);
        va.setDuration(ms);
        va.setInterpolator(interp);
        va.addUpdateListener(a -> { setter.set((float)a.getAnimatedValue()); invalidate(); });
        return va;
    }

    private AnimatorSet buildSet(ValueAnimator... anims) {
        AnimatorSet s = new AnimatorSet();
        s.playTogether(anims);
        return s;
    }

    // ── ÇİZİM ──
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();

        // Bob offset — her karakter biraz farklı fazda
        float bob1 = (float) Math.sin(idleBob)           * h * 0.012f;
        float bob2 = (float) Math.sin(idleBob + 0.8f)    * h * 0.010f;
        float bob3 = (float) Math.sin(idleBob + 1.6f)    * h * 0.014f;
        float bob4 = (float) Math.sin(idleBob + 2.4f)    * h * 0.009f;

        float shockOff = (shockScale - 1f) * h * 0.06f;

        // Hata irkilmesi + başarı zıplaması
        float globalY = -jumpY * h * 0.06f - shockOff;

        canvas.save();
        canvas.translate(0, globalY);
        canvas.scale(shockScale, shockScale, w * 0.5f, h * 0.65f);

        drawScene(canvas, w, h, bob1, bob2, bob3, bob4);

        canvas.restore();
    }

    private void drawScene(Canvas canvas, float w, float h,
                           float bob1, float bob2, float bob3, float bob4) {
        float baseY = h * 0.88f; // zemin çizgisi

        // Öne eğilme offseti
        float lean = leanForward * h * 0.04f;

        // ── TURUNCU (yuvarlak, en solda/önde) ──
        float orCX = w * 0.20f;
        float orCY = baseY - w * 0.20f + bob3;
        float orR  = w * 0.20f;
        // Arkasını döner: 0=öne bakıyor, 1=arka
        drawOrangeChar(canvas, orCX, orCY, orR, orangeTurn, lean);

        // ── MOR (dikdörtgen, sol-orta) ──
        float purX = w * 0.28f;
        float purW = w * 0.22f;
        float purH = h * 0.42f;
        float purY = baseY - purH + bob1 - lean;
        // Engel kaldırır: kolunu uzatır
        drawPurpleChar(canvas, purX, purY, purW, purH, blockerRaise, eyebrowRaise);

        // ── SİYAH (ince, ortada) ──
        float blkX = w * 0.50f;
        float blkW = w * 0.15f;
        float blkH = h * 0.35f;
        float blkY = baseY - blkH + bob2 - lean * 0.5f;
        // Yan bakar
        drawBlackChar(canvas, blkX, blkY, blkW, blkH, peekLean, eyebrowRaise, blockerRaise);

        // ── SARI (dikdörtgen, sağ) ──
        float yelX = w * 0.68f;
        float yelW = w * 0.20f;
        float yelH = h * 0.32f;
        float yelY = baseY - yelH + bob4 - lean;
        drawYellowChar(canvas, yelX, yelY, yelW, yelH, blockerRaise, eyebrowRaise);

        // Zemin gölgesi
        drawGroundShadows(canvas, w, baseY);
    }

    // ── TURUNCU KARAKTER ──
    private void drawOrangeChar(Canvas canvas, float cx, float cy, float r,
                                 float turnProg, float lean) {
        // Gövde
        p.setShader(new RadialGradient(cx - r*0.2f, cy - r*0.2f, r*1.1f,
                0xFFFF8C42, 0xFFE65100, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy - lean, r, p);
        p.setShader(null);

        // Yanak allığı
        p.setColor(0x33FF3D00);
        canvas.drawCircle(cx - r*0.45f, cy - lean + r*0.15f, r*0.22f, p);
        canvas.drawCircle(cx + r*0.45f, cy - lean + r*0.15f, r*0.22f, p);

        if (turnProg < 0.5f) {
            // Öne bakıyor
            float alpha = 1f - turnProg * 2f;
            drawFace(canvas, cx, cy - lean, r, alpha, false, 0f);
        } else {
            // Arkasını dönüyor — arka yüz (basit)
            float alpha = (turnProg - 0.5f) * 2f;
            p.setColor(lerpColor(0xFFE65100, 0xFFBF360C, alpha));
            canvas.drawCircle(cx, cy - lean, r * 0.85f, p);
            // Saç gibi detay
            p.setColor(0xFFBF360C);
            canvas.drawArc(new RectF(cx-r*0.5f, cy-lean-r*0.8f, cx+r*0.5f, cy-lean-r*0.2f),
                    0, 180, false, p);
        }
    }

    // ── MOR KARAKTER ──
    private void drawPurpleChar(Canvas canvas, float x, float y, float w, float h,
                                 float blockerProg, float eyebrow) {
        float cx = x + w/2f;
        // Gövde
        RectF body = new RectF(x, y, x + w, y + h);
        p.setShader(new LinearGradient(x, y, x+w, y+h,
                0xFF9C27B0, 0xFF6A0080, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, w*0.3f, w*0.3f, p);
        p.setShader(null);

        // Yüz
        float faceY = y + h * 0.28f;
        drawRectFace(canvas, cx, faceY, w, eyebrow, blockerProg > 0.3f);

        // Engel kolu — sağa uzanır
        if (blockerProg > 0.05f) {
            float armLen = w * 1.0f * blockerProg;
            float armY   = y + h * 0.35f;
            float armH   = h * 0.09f;
            p.setColor(0xFF7B1FA2);
            RectF arm = new RectF(x + w, armY, x + w + armLen, armY + armH);
            canvas.drawRoundRect(arm, armH/2f, armH/2f, p);
            // El
            p.setColor(0xFF9C27B0);
            canvas.drawCircle(x + w + armLen, armY + armH/2f, armH * 0.7f, p);
        }
    }

    // ── SİYAH KARAKTER ──
    private void drawBlackChar(Canvas canvas, float x, float y, float w, float h,
                                float peekProg, float eyebrow, float blockerProg) {
        float cx = x + w/2f;
        // Siyah ince karakter sağa kayar (peek durumunda)
        float peekOffX = peekProg * w * 0.6f;

        // Gövde
        RectF body = new RectF(x + peekOffX, y, x + w + peekOffX, y + h);
        p.setShader(new LinearGradient(x, y, x+w, y+h,
                0xFF2C2C2C, 0xFF0A0A0A, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, w*0.35f, w*0.35f, p);
        p.setShader(null);

        // Yüz — peek'te gözler sağa kayar
        float faceCX = cx + peekOffX + peekProg * w * 0.3f;
        float faceY  = y + h * 0.25f;
        drawTallFace(canvas, faceCX, faceY, w, eyebrow, peekProg, curGX, curGY);
    }

    // ── SARI KARAKTER ──
    private void drawYellowChar(Canvas canvas, float x, float y, float w, float h,
                                 float blockerProg, float eyebrow) {
        float cx = x + w/2f;
        // Gövde
        RectF body = new RectF(x, y, x + w, y + h);
        p.setShader(new LinearGradient(x, y, x, y+h,
                0xFFFFD600, 0xFFFFA000, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, w*0.28f, w*0.28f, p);
        p.setShader(null);

        // Yüz
        float faceY = y + h * 0.26f;
        drawRectFace(canvas, cx, faceY, w, eyebrow, blockerProg > 0.3f);

        // Engel kolu — sola uzanır
        if (blockerProg > 0.05f) {
            float armLen = w * 0.9f * blockerProg;
            float armY   = y + h * 0.33f;
            float armH   = h * 0.08f;
            p.setColor(0xFFFFA000);
            RectF arm = new RectF(x - armLen, armY, x, armY + armH);
            canvas.drawRoundRect(arm, armH/2f, armH/2f, p);
            p.setColor(0xFFFFD600);
            canvas.drawCircle(x - armLen, armY + armH/2f, armH * 0.7f, p);
        }
    }

    // ── YÜZ ÇİZİCİLER ──
    private void drawFace(Canvas canvas, float cx, float cy, float r,
                           float alpha, boolean shocked, float peekX) {
        int a = (int)(255 * alpha);
        if (a < 10) return;

        float eyeSpacing = r * 0.32f;
        float eyeY = cy - r * 0.08f;
        float erw  = r * 0.16f, erh = r * 0.20f;

        // Göz beyazları
        p.setColor(Color.WHITE); p.setAlpha(a);
        canvas.drawOval(cx-eyeSpacing-erw, eyeY-erh, cx-eyeSpacing+erw, eyeY+erh, p);
        canvas.drawOval(cx+eyeSpacing-erw, eyeY-erh, cx+eyeSpacing+erw, eyeY+erh, p);

        // Pupiller
        float pr = erw * 0.55f;
        float ox  = Math.max(-erw*0.4f, Math.min(erw*0.4f, curGX + peekX));
        float oy  = Math.max(-erh*0.35f, Math.min(erh*0.35f, curGY));
        p.setColor(0xFF111111); p.setAlpha(a);
        canvas.drawCircle(cx - eyeSpacing + ox, eyeY + oy, pr, p);
        canvas.drawCircle(cx + eyeSpacing + ox, eyeY + oy, pr, p);

        // Parlaklık
        p.setColor(Color.WHITE); p.setAlpha((int)(a * 0.85f));
        canvas.drawCircle(cx-eyeSpacing+ox+pr*0.3f, eyeY+oy-pr*0.35f, pr*0.3f, p);
        canvas.drawCircle(cx+eyeSpacing+ox+pr*0.3f, eyeY+oy-pr*0.35f, pr*0.3f, p);

        // Ağız
        p.setColor(0xFF222222); p.setAlpha(a);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(r * 0.045f);
        p.setStrokeCap(Paint.Cap.ROUND);
        if (shocked) {
            // Şaşkın — oval ağız
            canvas.drawOval(cx - r*0.12f, cy + r*0.25f,
                            cx + r*0.12f, cy + r*0.42f, p);
        } else {
            // Normal gülümseme
            canvas.drawArc(new RectF(cx-r*0.28f, cy+r*0.15f,
                                     cx+r*0.28f, cy+r*0.42f), 0, 180, false, p);
        }
        p.setStyle(Paint.Style.FILL); p.setAlpha(255);
    }

    private void drawRectFace(Canvas canvas, float cx, float topY, float charW,
                               float eyebrow, boolean angry) {
        float ew = charW * 0.13f, eh = charW * 0.08f;
        float eyeSpacing = charW * 0.20f;
        float eyeY = topY;

        // Kaşlar
        p.setColor(angry ? 0xFFFFFFFF : 0xFFDDDDDD);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(charW * 0.045f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float browLift = eyebrow * charW * 0.06f;
        if (angry) {
            // V şekli — endişeli
            canvas.drawLine(cx - eyeSpacing - ew, eyeY - browLift - charW*0.05f,
                            cx - eyeSpacing + ew, eyeY - browLift,              p);
            canvas.drawLine(cx + eyeSpacing - ew, eyeY - browLift,
                            cx + eyeSpacing + ew, eyeY - browLift - charW*0.05f, p);
        } else {
            canvas.drawLine(cx - eyeSpacing - ew, eyeY - browLift,
                            cx - eyeSpacing + ew, eyeY - browLift, p);
            canvas.drawLine(cx + eyeSpacing - ew, eyeY - browLift,
                            cx + eyeSpacing + ew, eyeY - browLift, p);
        }
        p.setStyle(Paint.Style.FILL);

        // Göz beyazları
        p.setColor(Color.WHITE);
        canvas.drawOval(cx-eyeSpacing-ew, eyeY, cx-eyeSpacing+ew, eyeY+eh*2, p);
        canvas.drawOval(cx+eyeSpacing-ew, eyeY, cx+eyeSpacing+ew, eyeY+eh*2, p);

        // Pupiller
        float pr = ew * 0.52f;
        float ox = Math.max(-ew*0.38f, Math.min(ew*0.38f, curGX));
        float oy = Math.max(-eh*0.3f,  Math.min(eh*0.3f,  curGY));
        p.setColor(0xFF111111);
        canvas.drawCircle(cx - eyeSpacing + ox, eyeY + eh + oy, pr, p);
        canvas.drawCircle(cx + eyeSpacing + ox, eyeY + eh + oy, pr, p);

        // Parlaklık
        p.setColor(Color.WHITE);
        canvas.drawCircle(cx-eyeSpacing+ox+pr*0.3f, eyeY+eh+oy-pr*0.3f, pr*0.28f, p);
        canvas.drawCircle(cx+eyeSpacing+ox+pr*0.3f, eyeY+eh+oy-pr*0.3f, pr*0.28f, p);

        // Ağız
        p.setColor(0xFFDDDDDD);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(charW * 0.04f);
        float mouthY = eyeY + eh * 3.2f;
        if (eyebrow > 0.5f && angry) {
            // Düz - endişeli
            canvas.drawLine(cx - ew*0.8f, mouthY, cx + ew*0.8f, mouthY, p);
        } else {
            canvas.drawArc(new RectF(cx - ew*1.0f, mouthY - ew*0.3f,
                                     cx + ew*1.0f, mouthY + ew*0.5f), 0, 180, false, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    private void drawTallFace(Canvas canvas, float cx, float topY, float charW,
                               float eyebrow, float peekProg,
                               float gazeX, float gazeY) {
        float ew = charW * 0.35f, eh = charW * 0.22f;
        float eyeSpacing = charW * 0.42f;
        float eyeY = topY;

        // Tek büyük göz (ince karakter — tek sıra göz)
        p.setColor(Color.WHITE);
        canvas.drawOval(cx-eyeSpacing-ew, eyeY, cx-eyeSpacing+ew, eyeY+eh*2, p);
        canvas.drawOval(cx+eyeSpacing-ew, eyeY, cx+eyeSpacing+ew, eyeY+eh*2, p);

        float pr = ew * 0.5f;
        // Peek'te gözler sağa dönüyor
        float peekOX = peekProg * ew * 0.55f;
        float ox = Math.max(-ew*0.4f, Math.min(ew*0.4f, gazeX + peekOX));
        float oy = Math.max(-eh*0.3f,  Math.min(eh*0.3f, gazeY));

        p.setColor(0xFF111111);
        canvas.drawCircle(cx - eyeSpacing + ox, eyeY + eh + oy, pr, p);
        canvas.drawCircle(cx + eyeSpacing + ox, eyeY + eh + oy, pr, p);

        // Parlaklık
        p.setColor(Color.WHITE);
        canvas.drawCircle(cx-eyeSpacing+ox+pr*0.3f, eyeY+eh+oy-pr*0.3f, pr*0.28f, p);
        canvas.drawCircle(cx+eyeSpacing+ox+pr*0.3f, eyeY+eh+oy-pr*0.3f, pr*0.28f, p);

        // Kaş — peek'te kurnaz
        p.setColor(0xFFAAAAAA);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(charW * 0.06f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float browLift = eyebrow * charW * 0.08f;
        if (peekProg > 0.3f) {
            // Tek kaş kalkar — kurnaz ifade
            canvas.drawArc(new RectF(cx-eyeSpacing-ew, eyeY-browLift-charW*0.15f,
                                     cx-eyeSpacing+ew, eyeY-browLift),
                    180, 180, false, p);
            canvas.drawArc(new RectF(cx+eyeSpacing-ew, eyeY-browLift*1.6f-charW*0.15f,
                                     cx+eyeSpacing+ew, eyeY-browLift*1.6f),
                    180, 180, false, p);
        } else {
            canvas.drawLine(cx-eyeSpacing-ew*0.7f, eyeY-browLift-charW*0.1f,
                            cx-eyeSpacing+ew*0.7f, eyeY-browLift-charW*0.1f, p);
            canvas.drawLine(cx+eyeSpacing-ew*0.7f, eyeY-browLift-charW*0.1f,
                            cx+eyeSpacing+ew*0.7f, eyeY-browLift-charW*0.1f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    // ── ZEMIN GÖLGELERİ ──
    private void drawGroundShadows(Canvas canvas, float w, float baseY) {
        sp.setMaskFilter(new BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL));
        sp.setColor(0x28000000);
        float[] xs = {w*0.20f, w*0.35f, w*0.50f, w*0.70f};
        float[] rs = {w*0.18f, w*0.12f, w*0.08f, w*0.11f};
        for (int i = 0; i < 4; i++) {
            canvas.drawOval(xs[i]-rs[i], baseY-rs[i]*0.3f,
                            xs[i]+rs[i], baseY+rs[i]*0.3f, sp);
        }
        sp.setMaskFilter(null);
    }

    // ── YARDIMCI ──
    private int lerpColor(int a, int b, float t) {
        int ar = (a>>16)&0xFF, ag = (a>>8)&0xFF, ab = a&0xFF;
        int br = (b>>16)&0xFF, bg = (b>>8)&0xFF, bb = b&0xFF;
        return Color.rgb(
            (int)(ar + (br-ar)*t),
            (int)(ag + (bg-ag)*t),
            (int)(ab + (bb-ab)*t));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(gazeRunner);
        if (idleAnim    != null) idleAnim.cancel();
        if (stateAnimSet!= null) stateAnimSet.cancel();
    }
}
 
