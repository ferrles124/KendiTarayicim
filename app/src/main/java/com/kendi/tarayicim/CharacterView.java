package com.kendi.tarayicim;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.*;
import android.view.animation.TimeInterpolator;
import java.util.Random;

public class CharacterView extends View {

    // ═══════════════════════════════════════════
    //  DIŞ STATE (LoginActivity'den tetiklenir)
    // ═══════════════════════════════════════════
    public static final int STATE_IDLE          = 0;
    public static final int STATE_EMAIL_FOCUS   = 1;
    public static final int STATE_PASSWORD_HIDE = 2;
    public static final int STATE_PASSWORD_SHOW = 3;
    public static final int STATE_ERROR         = 4;
    public static final int STATE_SUCCESS       = 5;

    private int formState = STATE_IDLE;

    // ═══════════════════════════════════════════
    //  KARAKTERLERİN VERİSİ
    // ═══════════════════════════════════════════
    private static final int VIGO = 0; // Mor dikdörtgen — ciddi lider
    private static final int NOX  = 1; // Siyah ince — sinsi meraklı
    private static final int PUF  = 2; // Turuncu yuvarlak — sakin uysal
    private static final int ZIP  = 3; // Sarı dikdörtgen — enerjik

    // Her karakterin anlık duygu değerleri
    private final float[] eyebrow    = {0f, 0f, 0f, 0f}; // -1..1
    private final float[] mouthCurve = {.3f,.3f,.3f,.3f}; // -1..1
    private final float[] eyeOpen    = {1f, 1f, 1f, 1f};  // 0..1.3

    // Pozisyon offsetleri (ev konumundan sapma)
    private final float[] posX = {0f, 0f, 0f, 0f};
    private final float[] posY = {0f, 0f, 0f, 0f};
    private final float[] velX = {0f, 0f, 0f, 0f};
    private final float[] velY = {0f, 0f, 0f, 0f};

    // Özel animasyon parametreleri
    private float pufTurnAngle  = 0f;   // 0=öne, 1=arka (şifreyi görmez)
    private float noxPeekLean   = 0f;   // Nox öne eğilme
    private float noxArmReach   = 0f;   // Nox'un uzanan kolu
    private float vigoArmRaise  = 0f;   // Vigo'nun dur kolu
    private float zipArmRaise   = 0f;   // Zip'in dur kolu
    private float noxSurrenderY = 0f;   // Nox teslim elleri
    private float vigoNodAngle  = 0f;   // Vigo baş sallama
    private float noxPokeArm    = 0f;   // Nox'un Puf'u dürtme kolu
    private float pufRollAngle  = 0f;   // Puf sallanma açısı
    private float zipJumpBonus  = 0f;   // Zip'in ekstra zıplaması
    private float groupShock    = 0f;   // Toplu irkilme
    private float successWave   = 0f;   // Başarı dalga fazı

    // Uyku
    private float[] sleepZ      = {0f, 0f, 0f, 0f};

    // Göz takibi
    private float targetGX = 0f, targetGY = 0f;
    private float curGX    = 0f, curGY    = 0f;

    // Boşta sallanma
    private float bobPhase = 0f;

    // Dokunma
    private int   draggedChar = -1;
    private float dragStartX, dragStartY;
    private float lastTouchX, lastTouchY;
    private long  lastTouchTime;

    // ═══════════════════════════════════════════
    //  BOYALAR
    // ═══════════════════════════════════════════
    private final Paint p   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sp  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ═══════════════════════════════════════════
    //  OTONOM SİSTEM
    // ═══════════════════════════════════════════
    private final Handler    h   = new Handler(Looper.getMainLooper());
    private final Random     rnd = new Random();
    private boolean          autonomousRunning = false;
    private long             idleMs = 0;
    private long             lastActivityMs = 0;

    // ═══════════════════════════════════════════
    //  RENDER LOOP
    // ═══════════════════════════════════════════
    private final Runnable renderLoop = new Runnable() {
        @Override public void run() {
            if (!autonomousRunning) return;
            bobPhase += 0.045f;
            if (bobPhase > Math.PI * 2) bobPhase -= (float)(Math.PI * 2);

            // Smooth gaze
            curGX += (targetGX - curGX) * 0.13f;
            curGY += (targetGY - curGY) * 0.13f;

            // Fizik adımı
            physicsStep();

            // Boşta kalma sayacı
            long now = System.currentTimeMillis();
            idleMs = now - lastActivityMs;

            invalidate();
            h.postDelayed(this, 16);
        }
    };

    // ═══════════════════════════════════════════
    //  KURUCU
    // ═══════════════════════════════════════════
    public CharacterView(Context ctx) { super(ctx); init(); }
    public CharacterView(Context ctx, AttributeSet a) { super(ctx, a); init(); }

    private void init() {
        sp.setColor(0x28000000);
        tp.setColor(Color.WHITE);
        tp.setTextSize(28f);
        tp.setFakeBoldText(true);
        lastActivityMs = System.currentTimeMillis();
        setOnTouchListener(this::handleTouch);
    }

    // ═══════════════════════════════════════════
    //  BAŞLAT / DURDUR
    // ═══════════════════════════════════════════
    public void startSystem() {
        if (autonomousRunning) return;
        autonomousRunning = true;
        h.post(renderLoop);
        scheduleAutonomousEvents();
    }

    public void stopSystem() {
        autonomousRunning = false;
        h.removeCallbacksAndMessages(null);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopSystem();
    }

    // ═══════════════════════════════════════════
    //  DIŞ API — LoginActivity çağırır
    // ═══════════════════════════════════════════
    public void setState(int state) {
        if (formState == state) return;
        formState = state;
        notifyActivity();

        switch (state) {
            case STATE_IDLE:
                playScene("idle_return");
                break;
            case STATE_EMAIL_FOCUS:
                playScene("email_focus");
                break;
            case STATE_PASSWORD_HIDE:
                playScene("password_hide");
                break;
            case STATE_PASSWORD_SHOW:
                playScene("password_show");
                break;
            case STATE_ERROR:
                playScene("error");
                break;
            case STATE_SUCCESS:
                playScene("success");
                break;
        }
    }

    public void updateGaze(float nx, float ny) {
        targetGX = nx * 11f;
        targetGY = ny * 7f;
        notifyActivity();
    }

    // ═══════════════════════════════════════════
    //  SAHNELER
    // ═══════════════════════════════════════════
    private void playScene(String sceneId) {
        switch (sceneId) {

            // ── IDLE'A DÖNÜŞ ──
            case "idle_return":
                anim(pufTurnAngle, 0f,  500, new OvershootInterpolator(), v -> pufTurnAngle  = v);
                anim(noxPeekLean,  0f,  400, new DecelerateInterpolator(),v -> noxPeekLean   = v);
                anim(noxArmReach,  0f,  350, new DecelerateInterpolator(),v -> noxArmReach   = v);
                anim(vigoArmRaise, 0f,  350, new DecelerateInterpolator(),v -> vigoArmRaise  = v);
                anim(zipArmRaise,  0f,  350, new DecelerateInterpolator(),v -> zipArmRaise   = v);
                anim(noxSurrenderY,0f,  300, new DecelerateInterpolator(),v -> noxSurrenderY = v);
                setEmotion(VIGO, 0f,  0.3f, 1f,  400);
                setEmotion(NOX,  0f,  0.3f, 1f,  400);
                setEmotion(PUF,  0f,  0.3f, 1f,  400);
                setEmotion(ZIP,  0f,  0.3f, 1f,  400);
                break;

            // ── EMAIL ODAK ──
            case "email_focus":
                // Zip heyecanlanır, Vigo onu susturur
                setEmotion(ZIP, 0.8f, 0.9f, 1.2f, 250);
                anim(0f, 1f, 250, new OvershootInterpolator(2f), v -> zipJumpBonus = v);
                h.postDelayed(() -> {
                    anim(zipJumpBonus, 0f, 300, new DecelerateInterpolator(), v -> zipJumpBonus = v);
                    setEmotion(VIGO, 0.3f, 0.0f, 1f, 200);
                    anim(0f, 0.6f, 200, new OvershootInterpolator(), v -> vigoArmRaise = v);
                    h.postDelayed(() -> {
                        anim(vigoArmRaise, 0f, 300, new DecelerateInterpolator(), v -> vigoArmRaise = v);
                        setEmotion(ZIP, 0f, 0.2f, 1f, 300);
                        // Hepsi öne eğilip forma bakar
                        setEmotion(NOX,  0.5f, 0.1f, 1.1f, 300);
                        setEmotion(PUF,  0.3f, 0.2f, 1f,   300);
                        setEmotion(VIGO, 0.2f, 0.1f, 1f,   300);
                    }, 700);
                }, 400);
                break;

            // ── ŞİFRE GİZLE ──
            case "password_hide":
                // Puf arkasını döner, sevimli şekilde
                anim(pufTurnAngle, 1f, 600, new OvershootInterpolator(0.6f), v -> pufTurnAngle = v);
                setEmotion(PUF, 0f, 0.5f, 1f, 300); // Puf mutlu
                h.postDelayed(() -> {
                    // Nox meraklanıp uzanmaya başlar
                    setEmotion(NOX, 0.9f, 0.1f, 0.8f, 200);
                    anim(0f, 0.7f, 500, new OvershootInterpolator(0.8f), v -> noxPeekLean = v);
                    anim(0f, 0.5f, 600, new OvershootInterpolator(),     v -> noxArmReach = v);
                    h.postDelayed(() -> {
                        // Vigo fark eder, dur der
                        setEmotion(VIGO, 0.5f, -0.2f, 1f, 200);
                        anim(0f, 1f, 300, new OvershootInterpolator(1.5f), v -> vigoArmRaise = v);
                        // Zip de katılır
                        h.postDelayed(() -> {
                            anim(0f, 0.8f, 350, new OvershootInterpolator(1.2f), v -> zipArmRaise = v);
                            setEmotion(ZIP, 0.4f, -0.1f, 1f, 200);
                            // Nox teslim olur
                            h.postDelayed(() -> {
                                anim(noxArmReach,  0f, 400, new DecelerateInterpolator(), v -> noxArmReach  = v);
                                anim(noxPeekLean,  0f, 400, new OvershootInterpolator(),  v -> noxPeekLean  = v);
                                anim(0f, 1f, 300, new OvershootInterpolator(), v -> noxSurrenderY = v);
                                setEmotion(NOX, 0.2f, 0.3f, 1f, 300);
                                h.postDelayed(() ->
                                    anim(noxSurrenderY, 0f, 400, new DecelerateInterpolator(), v -> noxSurrenderY = v),
                                    1000);
                            }, 600);
                        }, 300);
                    }, 700);
                }, 400);
                break;

            // ── ŞİFRE GÖSTER ──
            case "password_show":
                // Nox çaktırmadan tekrar uzanır, bu sefer hızlı
                setEmotion(NOX, 1f, 0.2f, 0.7f, 150);
                anim(noxPeekLean, 0.9f, 350, new OvershootInterpolator(1.5f), v -> noxPeekLean = v);
                anim(noxArmReach, 0.8f, 400, new OvershootInterpolator(),     v -> noxArmReach = v);
                // Vigo ve Zip panikler
                h.postDelayed(() -> {
                    setEmotion(VIGO, 0.8f, -0.3f, 1.2f, 150);
                    setEmotion(ZIP,  0.8f, -0.2f, 1.2f, 150);
                    anim(vigoArmRaise, 1f, 200, new OvershootInterpolator(2f), v -> vigoArmRaise = v);
                    anim(zipArmRaise,  1f, 200, new OvershootInterpolator(2f), v -> zipArmRaise  = v);
                }, 200);
                break;

            // ── HATA ──
            case "error":
                // Toplu irkilme
                anim(0f, 1f, 120, new AccelerateInterpolator(), v -> groupShock = v);
                setEmotion(VIGO, 1f, -0.5f, 1.3f, 100);
                setEmotion(NOX,  1f, -0.3f, 1.3f, 100);
                setEmotion(PUF,  1f, -0.4f, 1.3f, 80);
                setEmotion(ZIP,  1f, -0.6f, 1.4f, 80);
                h.postDelayed(() -> {
                    anim(groupShock, 0f, 400, new BounceInterpolator(), v -> groupShock = v);
                    // Vigo Nox'a bakar — "senin yüzünden" ifadesi
                    setEmotion(VIGO, 0.3f, -0.4f, 1f, 300);
                    h.postDelayed(() -> {
                        // Nox suçsuz görünmeye çalışır
                        setEmotion(NOX, 0f, 0.1f, 0.9f, 300);
                        anim(noxPeekLean, 0f, 300, new DecelerateInterpolator(), v -> noxPeekLean = v);
                        // Puf güler
                        setEmotion(PUF, 0.4f, 1f, 1f, 200);
                        anim(0f, 1f, 200, new OvershootInterpolator(3f), v -> pufRollAngle = v);
                        h.postDelayed(() -> anim(pufRollAngle, 0f, 300,
                                new DecelerateInterpolator(), v -> pufRollAngle = v), 250);
                        // Zip hâlâ şaşkın
                        setEmotion(ZIP, 0.8f, -0.5f, 1.3f, 200);
                        h.postDelayed(() -> {
                            setEmotion(VIGO, 0f, 0.2f, 1f, 400);
                            setEmotion(NOX,  0f, 0.3f, 1f, 400);
                            setEmotion(PUF,  0f, 0.3f, 1f, 400);
                            setEmotion(ZIP,  0f, 0.3f, 1f, 400);
                        }, 1200);
                    }, 600);
                }, 180);
                break;

            // ── BAŞARI ──
            case "success":
                // Zip önce — dalga gibi sırayla
                setEmotion(ZIP, 1f, 1f, 1.1f, 150);
                anim(0f, 1f, 250, new OvershootInterpolator(2.5f), v -> zipJumpBonus = v);
                h.postDelayed(() -> {
                    anim(zipJumpBonus, 0f, 350, new DecelerateInterpolator(), v -> zipJumpBonus = v);
                    // Puf döner ve sevinir
                    anim(pufTurnAngle, 0f, 400, new OvershootInterpolator(), v -> pufTurnAngle = v);
                    setEmotion(PUF, 0.8f, 1f, 1.1f, 200);
                }, 200);
                h.postDelayed(() -> {
                    setEmotion(VIGO, 0.6f, 0.8f, 1f, 200);
                    anim(0f, 1f, 300, new OvershootInterpolator(3f), v -> vigoNodAngle = v);
                    h.postDelayed(() -> anim(vigoNodAngle, 0f, 400,
                            new DecelerateInterpolator(), v -> vigoNodAngle = v), 320);
                }, 400);
                h.postDelayed(() -> {
                    // Nox cool durmaya çalışır ama zıplar
                    setEmotion(NOX, 0.5f, 0.7f, 1f, 200);
                    anim(0f, 0.7f, 300, new OvershootInterpolator(2f), v -> zipJumpBonus = v * 0.6f);
                    h.postDelayed(() -> anim(zipJumpBonus, 0f, 400,
                            new DecelerateInterpolator(), v -> zipJumpBonus = v), 320);
                    // Başarı dalga animasyonu
                    anim(0f, (float)(Math.PI * 4), 1200,
                            new LinearInterpolator(), v -> successWave = v);
                }, 600);
                break;

            // ── OTONOM SAHNELER ──
            case "auto_nox_peeks":
                if (formState != STATE_IDLE) break;
                setEmotion(NOX, 0.7f, 0.1f, 0.8f, 200);
                anim(0f, 0.5f, 400, new OvershootInterpolator(), v -> noxPeekLean = v);
                h.postDelayed(() -> {
                    setEmotion(VIGO, 0.3f, 0f, 1f, 150);
                    anim(0f, 0.5f, 250, new OvershootInterpolator(), v -> vigoArmRaise = v);
                    h.postDelayed(() -> {
                        anim(noxPeekLean,  0f, 300, new DecelerateInterpolator(), v -> noxPeekLean  = v);
                        anim(vigoArmRaise, 0f, 350, new DecelerateInterpolator(), v -> vigoArmRaise = v);
                        setEmotion(NOX,  0f, 0.3f, 1f, 300);
                        setEmotion(VIGO, 0f, 0.3f, 1f, 300);
                    }, 800);
                }, 600);
                break;

            case "auto_zip_jump_vigo_stare":
                if (formState != STATE_IDLE) break;
                setEmotion(ZIP, 0.7f, 0.8f, 1.1f, 150);
                anim(0f, 1f, 220, new OvershootInterpolator(2f), v -> zipJumpBonus = v);
                h.postDelayed(() -> {
                    anim(zipJumpBonus, 0f, 320, new DecelerateInterpolator(), v -> zipJumpBonus = v);
                    setEmotion(VIGO, 0.2f, -0.2f, 1f, 200);
                    h.postDelayed(() -> {
                        setEmotion(ZIP,  0f, 0.1f, 1f, 300);
                        setEmotion(VIGO, 0f, 0.3f, 1f, 400);
                    }, 1000);
                }, 250);
                break;

            case "auto_puf_sleep":
                if (formState != STATE_IDLE) break;
                setEmotion(PUF, -0.3f, 0f, 0.05f, 900);
                anim(0f, 1f, 800, new DecelerateInterpolator(), v -> sleepZ[PUF] = v);
                h.postDelayed(() -> {
                    // Nox dürtür
                    anim(0f, 1f, 200, new OvershootInterpolator(2f), v -> noxPokeArm = v);
                    h.postDelayed(() -> {
                        anim(noxPokeArm, 0f, 250, new DecelerateInterpolator(), v -> noxPokeArm = v);
                        // Puf irkilir
                        setEmotion(PUF, 1f, -0.3f, 1.4f, 80);
                        anim(sleepZ[PUF], 0f, 200, new AccelerateInterpolator(), v -> sleepZ[PUF] = v);
                        h.postDelayed(() -> {
                            // Nox güler
                            setEmotion(NOX, 0.4f, 1f, 1f, 200);
                            anim(0f, 1f, 200, new OvershootInterpolator(3f), v -> zipJumpBonus = v * 0.4f);
                            h.postDelayed(() -> {
                                anim(zipJumpBonus, 0f, 300, new DecelerateInterpolator(), v -> zipJumpBonus = v);
                                // Puf küser
                                setEmotion(PUF, -0.4f, -0.5f, 0.9f, 300);
                                setEmotion(NOX, 0f, 0.3f, 1f, 500);
                                h.postDelayed(() -> setEmotion(PUF, 0f, 0.3f, 1f, 600), 2000);
                            }, 250);
                        }, 300);
                    }, 1500);
                }, 2200);
                break;

            case "auto_nox_zip_argument":
                if (formState != STATE_IDLE) break;
                setEmotion(NOX, 0.3f, -0.2f, 1f, 200);
                setEmotion(ZIP, 0.3f, -0.2f, 1f, 200);
                h.postDelayed(() -> {
                    setEmotion(NOX, -0.2f, -0.4f, 0.9f, 250);
                    setEmotion(ZIP, -0.2f, -0.4f, 0.9f, 250);
                }, 900);
                h.postDelayed(() -> {
                    setEmotion(NOX, 0f, 0.3f, 1f, 500);
                    setEmotion(ZIP, 0f, 0.3f, 1f, 500);
                }, 2500);
                break;

            case "auto_vigo_posture":
                if (formState != STATE_IDLE) break;
                anim(0f, 1f, 200, new OvershootInterpolator(), v -> vigoNodAngle = v * 0.3f);
                h.postDelayed(() ->
                    anim(vigoNodAngle, 0f, 350, new DecelerateInterpolator(), v -> vigoNodAngle = v),
                    250);
                break;

            case "auto_puf_roll":
                if (formState != STATE_IDLE) break;
                anim(0f, (float)(Math.PI * 2), 1000, new AccelerateDecelerateInterpolator(),
                        v -> pufRollAngle = v);
                break;

            case "auto_all_look":
                if (formState != STATE_IDLE) break;
                setEmotion(VIGO, 0.6f, 0.1f, 1.1f, 200);
                setEmotion(NOX,  0.6f, 0.1f, 1.1f, 200);
                setEmotion(PUF,  0.6f, 0.1f, 1.1f, 200);
                setEmotion(ZIP,  0.6f, 0.1f, 1.1f, 200);
                h.postDelayed(() -> {
                    setEmotion(VIGO, 0f, 0.3f, 1f, 400);
                    setEmotion(NOX,  0f, 0.3f, 1f, 400);
                    setEmotion(PUF,  0f, 0.3f, 1f, 400);
                    setEmotion(ZIP,  0f, 0.3f, 1f, 400);
                }, 1800);
                break;
        }
    }

    // ═══════════════════════════════════════════
    //  OTONOM ZAMANLAYICI
    // ═══════════════════════════════════════════
    private static final String[] AUTO_SCENES = {
        "auto_nox_peeks",
        "auto_zip_jump_vigo_stare",
        "auto_puf_sleep",
        "auto_nox_zip_argument",
        "auto_vigo_posture",
        "auto_puf_roll",
        "auto_all_look"
    };

    private void scheduleAutonomousEvents() {
        if (!autonomousRunning) return;
        long delay = 8000 + (long)(rnd.nextFloat() * 14000);
        h.postDelayed(() -> {
            if (formState == STATE_IDLE) {
                String scene = AUTO_SCENES[rnd.nextInt(AUTO_SCENES.length)];
                playScene(scene);
            }
            scheduleAutonomousEvents();
        }, delay);

        // Boşta kalma kontrolü
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (!autonomousRunning) return;
                if (formState == STATE_IDLE && idleMs > 25000) {
                    playScene("auto_puf_sleep");
                }
                h.postDelayed(this, 5000);
            }
        }, 5000);
    }

    // ═══════════════════════════════════════════
    //  FİZİK ADIMI
    // ═══════════════════════════════════════════
    private void physicsStep() {
        for (int i = 0; i < 4; i++) {
            if (draggedChar == i) continue;
            // Elastik ip — eve döner
            velX[i] += -posX[i] * 0.09f;
            velY[i] += -posY[i] * 0.09f;
            // Sönümleme
            velX[i] *= 0.78f;
            velY[i] *= 0.78f;
            // İntegrasyon
            posX[i] += velX[i];
            posY[i] += velY[i];
        }
        // Basit çarpışma
        checkCollisions();
    }

    private void checkCollisions() {
        float[] charR = getCharRadii();
        for (int i = 0; i < 4; i++) {
            for (int j = i+1; j < 4; j++) {
                float[] ci = getCharCenter(i), cj = getCharCenter(j);
                float dx = (cj[0]+posX[j]) - (ci[0]+posX[i]);
                float dy = (cj[1]+posY[j]) - (ci[1]+posY[i]);
                float dist = (float)Math.sqrt(dx*dx+dy*dy);
                float minD = charR[i] + charR[j];
                if (dist < minD && dist > 1f) {
                    float nx = dx/dist, ny = dy/dist;
                    float push = (minD - dist) * 0.4f;
                    if (draggedChar != i) { posX[i] -= nx*push*0.5f; posY[i] -= ny*push*0.5f; }
                    if (draggedChar != j) { posX[j] += nx*push*0.5f; posY[j] += ny*push*0.5f; }
                    // Çarpışma tepkisi
                    velX[i] -= nx * 1.5f; velY[i] -= ny * 1.5f;
                    velX[j] += nx * 1.5f; velY[j] += ny * 1.5f;
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    //  DOKUNMA
    // ═══════════════════════════════════════════
    private boolean handleTouch(View v, MotionEvent e) {
        float tx = e.getX(), ty = e.getY();
        notifyActivity();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                draggedChar = findCharAt(tx, ty);
                if (draggedChar >= 0) {
                    dragStartX = tx; dragStartY = ty;
                    lastTouchX = tx; lastTouchY = ty;
                    lastTouchTime = System.currentTimeMillis();
                    // Dokunma irkilmesi
                    int c = draggedChar;
                    setEmotion(c, 0.8f, -0.3f, 1.3f, 80);
                    h.postDelayed(() -> setEmotion(c, eyebrow[c]*0.5f, mouthCurve[c], 1f, 300), 300);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (draggedChar >= 0) {
                    float w = getWidth(), h2 = getHeight();
                    float[] base = getCharCenter(draggedChar);
                    // Sınır: karakterin bölgesinden çok uzaklaşmasın
                    float maxDrift = w * 0.28f;
                    float ndx = tx - base[0], ndy = ty - base[1];
                    float ndist = (float)Math.sqrt(ndx*ndx+ndy*ndy);
                    if (ndist > maxDrift) {
                        ndx = ndx/ndist * maxDrift;
                        ndy = ndy/ndist * maxDrift;
                    }
                    posX[draggedChar] = ndx;
                    posY[draggedChar] = ndy;
                    velX[draggedChar] = (tx - lastTouchX) * 0.5f;
                    velY[draggedChar] = (ty - lastTouchY) * 0.5f;
                    lastTouchX = tx; lastTouchY = ty;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggedChar >= 0) {
                    long dt = System.currentTimeMillis() - lastTouchTime;
                    if (dt < 120) {
                        // Fling — hız uygula
                        velX[draggedChar] = (tx - lastTouchX) * 2.5f;
                        velY[draggedChar] = (ty - lastTouchY) * 2.5f;
                    }
                    draggedChar = -1;
                }
                return true;
        }
        return false;
    }

    private int findCharAt(float tx, float ty) {
        float[] radii = getCharRadii();
        for (int i = 0; i < 4; i++) {
            float[] c = getCharCenter(i);
            float dx = tx - (c[0] + posX[i]);
            float dy = ty - (c[1] + posY[i]);
            if (Math.sqrt(dx*dx+dy*dy) < radii[i] * 1.3f) return i;
        }
        return -1;
    }

    // ═══════════════════════════════════════════
    //  ÇİZİM
    // ═══════════════════════════════════════════
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        drawScene(canvas, w, h);
    }

    private void drawScene(Canvas canvas, float w, float h) {
        float baseY = h * 0.90f;
        float bob1 = (float)Math.sin(bobPhase)        * h * 0.013f;
        float bob2 = (float)Math.sin(bobPhase + 0.9f) * h * 0.011f;
        float bob3 = (float)Math.sin(bobPhase + 1.8f) * h * 0.016f;
        float bob4 = (float)Math.sin(bobPhase + 2.7f) * h * 0.010f;

        // Şok skalası
        float shockS  = 1f + groupShock * 0.13f;
        canvas.save();
        canvas.scale(shockS, shockS, w*0.5f, baseY);

        // Zemin gölgeleri
        drawGroundShadows(canvas, w, baseY);

        // Çizim sırası: arkadan öne
        drawPuf (canvas, w, h, baseY, bob3); // Turuncu — en önde
        drawVigo(canvas, w, h, baseY, bob1); // Mor
        drawNox (canvas, w, h, baseY, bob2); // Siyah
        drawZip (canvas, w, h, baseY, bob4); // Sarı

        canvas.restore();
    }

        // ── VIGO (Mor dikdörtgen) ──
    private void drawVigo(Canvas canvas, float w, float h, float baseY, float bob) {
        float bw = w * 0.24f, bh = h * 0.40f;
        float cx = w * 0.26f + posX[VIGO];
        float by = baseY - bh + bob + posY[VIGO];
        float lean = vigoNodAngle * bh * 0.04f;

        canvas.save();
        canvas.rotate(lean * 2f, cx, by + bh);

        // Gövde
        RectF body = new RectF(cx - bw/2f, by, cx + bw/2f, by + bh);
        p.setShader(new LinearGradient(cx-bw/2f, by, cx+bw/2f, by+bh,
                0xFF9C27B0, 0xFF4A0072, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, bw*0.28f, bw*0.28f, p);
        p.setShader(null);

        // Yüz
        drawFace(canvas, cx, by + bh*0.28f, bw,
                eyebrow[VIGO], mouthCurve[VIGO], eyeOpen[VIGO], false, 0f);

        // Dur kolu (sağa uzanır)
        if (vigoArmRaise > 0.02f) {
            float armLen = bw * 1.1f * vigoArmRaise;
            float armY   = by + bh * 0.38f;
            float armH   = bh * 0.085f;
            p.setColor(0xFF7B1FA2);
            RectF arm = new RectF(cx + bw/2f, armY, cx + bw/2f + armLen, armY + armH);
            canvas.drawRoundRect(arm, armH/2f, armH/2f, p);
            // El
            p.setColor(0xFF9C27B0);
            canvas.drawCircle(cx + bw/2f + armLen, armY + armH/2f, armH*0.8f, p);
        }

        // Uyku Z'si
        if (sleepZ[VIGO] > 0.1f) drawSleepZ(canvas, cx + bw*0.4f, by, sleepZ[VIGO]);

        canvas.restore();
    }

    // ── NOX (Siyah ince) ──
    private void drawNox(Canvas canvas, float w, float h, float baseY, float bob) {
        float bw = w * 0.16f, bh = h * 0.34f;
        float baseCX = w * 0.50f;
        // Peek lean: öne eğilir (sola)
        float leanOffX = -noxPeekLean * bw * 1.0f;
        float leanOffY = -noxPeekLean * bh * 0.15f;
        float cx = baseCX + leanOffX + posX[NOX];
        float by = baseY - bh + bob + leanOffY + posY[NOX];

        float leanAngle = -noxPeekLean * 22f;
        canvas.save();
        canvas.rotate(leanAngle, cx, by + bh);

        // Gövde
        RectF body = new RectF(cx - bw/2f, by, cx + bw/2f, by + bh);
        p.setShader(new LinearGradient(cx, by, cx, by+bh,
                0xFF2C2C2C, 0xFF080808, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, bw*0.35f, bw*0.35f, p);
        p.setShader(null);

        // Yüz — peek'te gözler sağa kayar
        float peekGazeX = curGX + noxPeekLean * 8f;
        drawFace(canvas, cx, by + bh*0.26f, bw,
                eyebrow[NOX], mouthCurve[NOX], eyeOpen[NOX],
                false, peekGazeX - curGX);

        // Uzanan kol (sağa)
        if (noxArmReach > 0.02f) {
            float armLen = bw * 2.5f * noxArmReach;
            float armY   = by + bh * 0.35f;
            float armH   = bh * 0.075f;
            p.setColor(0xFF1A1A1A);
            RectF arm = new RectF(cx + bw/2f, armY, cx + bw/2f + armLen, armY + armH);
            canvas.drawRoundRect(arm, armH/2f, armH/2f, p);
            p.setColor(0xFF2C2C2C);
            canvas.drawCircle(cx + bw/2f + armLen, armY + armH/2f, armH*0.85f, p);
        }

        // Puf'u dürtme kolu (sola)
        if (noxPokeArm > 0.02f) {
            float armLen = bw * 1.8f * noxPokeArm;
            float armY   = by + bh * 0.40f;
            float armH   = bh * 0.07f;
            p.setColor(0xFF1A1A1A);
            RectF arm = new RectF(cx - bw/2f - armLen, armY, cx - bw/2f, armY + armH);
            canvas.drawRoundRect(arm, armH/2f, armH/2f, p);
        }

        // Teslim elleri (yukarı)
        if (noxSurrenderY > 0.02f) {
            float handY  = by + bh*0.2f - noxSurrenderY * bh*0.25f;
            float handSz = bw * 0.45f;
            p.setColor(0xFF2C2C2C);
            canvas.drawCircle(cx - bw*0.38f, handY, handSz, p);
            canvas.drawCircle(cx + bw*0.38f, handY, handSz, p);
        }

        if (sleepZ[NOX] > 0.1f) drawSleepZ(canvas, cx + bw*0.4f, by, sleepZ[NOX]);

        canvas.restore();
    }

    // ── PUF (Turuncu yuvarlak) ──
    private void drawPuf(Canvas canvas, float w, float h, float baseY, float bob) {
        float r  = w * 0.22f;
        float cx = w * 0.16f + posX[PUF];
        float cy = baseY - r + bob + posY[PUF];

        // Sallanma
        float rollSway = (float)Math.sin(pufRollAngle) * r * 0.22f;

        canvas.save();
        canvas.rotate(rollSway * 8f, cx, cy + r * 0.6f);

        // Gövde
        p.setShader(new RadialGradient(cx - r*0.2f, cy - r*0.2f, r * 1.2f,
                0xFFFF8C42, 0xFFD84315, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, r, p);
        p.setShader(null);

        // Yanak allığı
        p.setColor(0x33FF3D00);
        canvas.drawCircle(cx - r*0.48f, cy + r*0.18f, r*0.22f, p);
        canvas.drawCircle(cx + r*0.48f, cy + r*0.18f, r*0.22f, p);

        if (pufTurnAngle < 0.5f) {
            // Öne bakıyor
            float faceAlpha = 1f - pufTurnAngle * 2f;
            drawFaceRound(canvas, cx, cy, r, faceAlpha,
                    eyebrow[PUF], mouthCurve[PUF], eyeOpen[PUF]);
        } else {
            // Arkasını dönüyor — sırt görünümü
            float backAlpha = (pufTurnAngle - 0.5f) * 2f;
            p.setColor(lerpColor(0xFFD84315, 0xFFBF360C, backAlpha));
            canvas.drawCircle(cx, cy, r * 0.78f, p);
            // Küçük topaç şekli
            p.setColor(0x44000000);
            canvas.drawCircle(cx, cy - r*0.1f, r*0.18f, p);
        }

        if (sleepZ[PUF] > 0.1f) drawSleepZ(canvas, cx + r*0.55f, cy - r*0.4f, sleepZ[PUF]);

        canvas.restore();
    }

    // ── ZIP (Sarı dikdörtgen) ──
    private void drawZip(Canvas canvas, float w, float h, float baseY, float bob) {
        float bw = w * 0.20f, bh = h * 0.30f;
        float cx = w * 0.76f + posX[ZIP];

        // Başarı dalgası
        float waveOff = successWave > 0f
                ? (float)Math.sin(successWave + 2.4f) * h * 0.025f : 0f;
        float jumpOff = -(zipJumpBonus * h * 0.07f) + waveOff;
        float by = baseY - bh + bob + jumpOff + posY[ZIP];

        canvas.save();

        // Gövde
        RectF body = new RectF(cx - bw/2f, by, cx + bw/2f, by + bh);
        p.setShader(new LinearGradient(cx, by, cx, by+bh,
                0xFFFFD600, 0xFFFF8F00, Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(body, bw*0.28f, bw*0.28f, p);
        p.setShader(null);

        // Yüz
        drawFace(canvas, cx, by + bh*0.26f, bw,
                eyebrow[ZIP], mouthCurve[ZIP], eyeOpen[ZIP], false, 0f);

        // Dur kolu (sola)
        if (zipArmRaise > 0.02f) {
            float armLen = bw * 1.0f * zipArmRaise;
            float armY   = by + bh * 0.36f;
            float armH   = bh * 0.08f;
            p.setColor(0xFFFFA000);
            RectF arm = new RectF(cx - bw/2f - armLen, armY, cx - bw/2f, armY + armH);
            canvas.drawRoundRect(arm, armH/2f, armH/2f, p);
            p.setColor(0xFFFFD600);
            canvas.drawCircle(cx - bw/2f - armLen, armY + armH/2f, armH*0.8f, p);
        }

        if (sleepZ[ZIP] > 0.1f) drawSleepZ(canvas, cx + bw*0.4f, by, sleepZ[ZIP]);

        canvas.restore();
    }

    // ═══════════════════════════════════════════
    //  YÜZ ÇİZİCİLER
    // ═══════════════════════════════════════════

    // Dikdörtgen karakterler için (Vigo, Nox, Zip)
    private void drawFace(Canvas canvas, float cx, float topY, float charW,
                           float brow, float mouth, float eyeOpenness,
                           boolean shocked, float extraGazeX) {
        float ew = charW * 0.16f, eh = charW * 0.11f;
        float sp = charW * 0.22f; // göz aralığı

        // Kaşlar
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(charW * 0.055f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float browLift = brow * charW * 0.07f;
        float innerTilt = brow * charW * 0.04f; // iç kenar tilt
        if (brow < -0.1f) {
            // Üzgün — iç köşeler yukarıda
            canvas.drawLine(cx-sp-ew, topY-browLift+innerTilt*1.5f, cx-sp+ew, topY-browLift-innerTilt, p);
            canvas.drawLine(cx+sp-ew, topY-browLift-innerTilt, cx+sp+ew, topY-browLift+innerTilt*1.5f, p);
        } else if (brow > 0.3f) {
            // Şaşkın/endişeli — V şeklinde
            canvas.drawLine(cx-sp-ew, topY-browLift, cx-sp+ew, topY-browLift+innerTilt, p);
            canvas.drawLine(cx+sp-ew, topY-browLift+innerTilt, cx+sp+ew, topY-browLift, p);
        } else {
            // Normal
            canvas.drawLine(cx-sp-ew, topY-browLift, cx-sp+ew, topY-browLift, p);
            canvas.drawLine(cx+sp-ew, topY-browLift, cx+sp+ew, topY-browLift, p);
        }
        p.setStyle(Paint.Style.FILL);

        // Göz beyazları — eyeOpenness ile yükseklik değişir
        float eyeH = eh * eyeOpenness;
        if (eyeH < 1f) eyeH = 1f;
        p.setColor(Color.WHITE);
        canvas.drawOval(cx-sp-ew, topY, cx-sp+ew, topY+eyeH*2f, p);
        canvas.drawOval(cx+sp-ew, topY, cx+sp+ew, topY+eyeH*2f, p);

        // Pupiller
        float pr   = ew * 0.52f;
        float maxX = ew  * 0.40f, maxY = eyeH * 0.35f;
        float ox   = Math.max(-maxX, Math.min(maxX, curGX + extraGazeX));
        float oy   = Math.max(-maxY, Math.min(maxY, curGY));
        p.setColor(0xFF111111);
        canvas.drawCircle(cx - sp + ox, topY + eyeH + oy, pr, p);
        canvas.drawCircle(cx + sp + ox, topY + eyeH + oy, pr, p);

        // Mavi iris
        p.setColor(0x664D9EFF);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(pr * 0.22f);
        canvas.drawCircle(cx - sp + ox, topY + eyeH + oy, pr * 0.68f, p);
        canvas.drawCircle(cx + sp + ox, topY + eyeH + oy, pr * 0.68f, p);
        p.setStyle(Paint.Style.FILL);

        // Parlaklık
        p.setColor(Color.WHITE);
        float shineR = pr * 0.28f;
        canvas.drawCircle(cx-sp+ox+pr*0.28f, topY+eyeH+oy-pr*0.32f, shineR, p);
        canvas.drawCircle(cx+sp+ox+pr*0.28f, topY+eyeH+oy-pr*0.32f, shineR, p);

        // Ağız
        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(charW * 0.048f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float mouthY = topY + eyeH * 3.2f;
        float mouthW = ew * 1.1f;
        if (shocked || mouth < -0.3f) {
            canvas.drawOval(cx-mouthW*0.5f, mouthY, cx+mouthW*0.5f, mouthY+mouthW*0.7f, p);
        } else if (mouth > 0.5f) {
            canvas.drawArc(new RectF(cx-mouthW, mouthY-ew*0.2f,
                                     cx+mouthW, mouthY+ew*0.7f), 0, 180, false, p);
        } else if (mouth < 0f) {
            canvas.drawArc(new RectF(cx-mouthW, mouthY,
                                     cx+mouthW, mouthY+ew*0.5f), 0, -180, false, p);
        } else {
            canvas.drawLine(cx-mouthW*0.6f, mouthY+ew*0.1f, cx+mouthW*0.6f, mouthY+ew*0.1f, p);
        }
        p.setStyle(Paint.Style.FILL);
    }

    // Yuvarlak Puf için
    private void drawFaceRound(Canvas canvas, float cx, float cy, float r,
                                float alpha, float brow, float mouth, float eyeOpenness) {
        int a = (int)(255 * alpha);
        if (a < 5) return;

        float sp = r * 0.30f;
        float ew = r * 0.17f, eh = r * 0.12f * eyeOpenness;
        if (eh < 1f) eh = 1f;

        // Kaş
        p.setColor(Color.WHITE); p.setAlpha(a);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.06f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float browLift = brow * r * 0.07f;
        float eyeY = cy - r * 0.08f;
        canvas.drawLine(cx-sp-ew, eyeY-eh-browLift-r*0.12f,
                        cx-sp+ew, eyeY-eh-browLift-r*0.12f, p);
        canvas.drawLine(cx+sp-ew, eyeY-eh-browLift-r*0.12f,
                        cx+sp+ew, eyeY-eh-browLift-r*0.12f, p);
        p.setStyle(Paint.Style.FILL);

        // Göz beyazları
        p.setColor(Color.WHITE); p.setAlpha(a);
        canvas.drawOval(cx-sp-ew, eyeY-eh, cx-sp+ew, eyeY+eh, p);
        canvas.drawOval(cx+sp-ew, eyeY-eh, cx+sp+ew, eyeY+eh, p);

        // Pupiller + gaze
        float pr = ew * 0.54f;
        float maxX = ew*0.38f, maxY = eh*0.32f;
        float ox = Math.max(-maxX, Math.min(maxX, curGX));
        float oy = Math.max(-maxY, Math.min(maxY, curGY));
        p.setColor(0xFF111111); p.setAlpha(a);
        canvas.drawCircle(cx-sp+ox, eyeY+oy, pr, p);
        canvas.drawCircle(cx+sp+ox, eyeY+oy, pr, p);
        p.setColor(Color.WHITE); p.setAlpha((int)(a*0.85f));
        canvas.drawCircle(cx-sp+ox+pr*0.3f, eyeY+oy-pr*0.32f, pr*0.28f, p);
        canvas.drawCircle(cx+sp+ox+pr*0.3f, eyeY+oy-pr*0.32f, pr*0.28f, p);

        // Ağız
        p.setColor(Color.WHITE); p.setAlpha(a);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.055f);
        p.setStrokeCap(Paint.Cap.ROUND);
        float mouthY = cy + r * 0.22f;
        if (mouth > 0.3f) {
            canvas.drawArc(new RectF(cx-r*0.28f, mouthY-r*0.06f,
                                     cx+r*0.28f, mouthY+r*0.18f), 0, 180, false, p);
        } else if (mouth < -0.2f) {
            canvas.drawArc(new RectF(cx-r*0.25f, mouthY,
                                     cx+r*0.25f, mouthY+r*0.15f), 0, -180, false, p);
        } else {
            canvas.drawLine(cx-r*0.22f, mouthY, cx+r*0.22f, mouthY, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setAlpha(255);
    }

    // ═══════════════════════════════════════════
    //  YARDIMCILAR
    // ═══════════════════════════════════════════

    private void drawSleepZ(Canvas canvas, float x, float y, float alpha) {
        tp.setAlpha((int)(255 * alpha));
        tp.setTextSize(28f * alpha);
        tp.setColor(0xFFAAAAAA);
        canvas.drawText("z", x, y, tp);
        tp.setTextSize(20f * alpha);
        canvas.drawText("z", x + 18f, y - 20f, tp);
        tp.setAlpha(255);
    }

    private void drawGroundShadows(Canvas canvas, float w, float baseY) {
        sp.setMaskFilter(new BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL));
        sp.setColor(0x25000000);
        float[] xs = {w*0.16f, w*0.26f, w*0.50f, w*0.76f};
        float[] rs = {w*0.19f, w*0.13f, w*0.09f, w*0.12f};
        for (int i = 0; i < 4; i++)
            canvas.drawOval(xs[i]-rs[i], baseY-rs[i]*0.25f,
                            xs[i]+rs[i], baseY+rs[i]*0.25f, sp);
        sp.setMaskFilter(null);
    }

    private float[] getCharCenter(int idx) {
        float w = getWidth(), h = getHeight();
        float baseY = h * 0.90f;
        switch (idx) {
            case VIGO: return new float[]{w*0.26f, baseY - h*0.20f};
            case NOX:  return new float[]{w*0.50f, baseY - h*0.17f};
            case PUF:  return new float[]{w*0.16f, baseY - w*0.22f};
            case ZIP:  return new float[]{w*0.76f, baseY - h*0.15f};
        }
        return new float[]{w/2f, h/2f};
    }

    private float[] getCharRadii() {
        float w = getWidth();
        return new float[]{w*0.13f, w*0.09f, w*0.22f, w*0.11f};
    }

    private void setEmotion(int char_, float brow, float mouth, float eyes, int ms) {
        anim(eyebrow[char_],    brow,  ms, new DecelerateInterpolator(), v -> eyebrow[char_]    = v);
        anim(mouthCurve[char_], mouth, ms, new DecelerateInterpolator(), v -> mouthCurve[char_] = v);
        anim(eyeOpen[char_],    eyes,  ms, new DecelerateInterpolator(), v -> eyeOpen[char_]    = v);
    }

    interface Setter { void set(float v); }

    private void anim(float from, float to, int ms, TimeInterpolator interp, Setter s) {
        ValueAnimator va = ValueAnimator.ofFloat(from, to);
        va.setDuration(ms);
        va.setInterpolator(interp);
        va.addUpdateListener(a -> { s.set((float)a.getAnimatedValue()); });
        va.start();
    }

    private int lerpColor(int a, int b, float t) {
        int ar=(a>>16)&0xFF, ag=(a>>8)&0xFF, ab=a&0xFF;
        int br=(b>>16)&0xFF, bg=(b>>8)&0xFF, bb=b&0xFF;
        return Color.rgb((int)(ar+(br-ar)*t),(int)(ag+(bg-ag)*t),(int)(ab+(bb-ab)*t));
    }

    private void notifyActivity() {
        lastActivityMs = System.currentTimeMillis();
        idleMs = 0;
    }
}
                    
