package com.kendi.tarayicim;

import android.os.Handler;
import android.os.Looper;
import java.util.Random;

public class CharacterBrain {

    public enum CharId { VIGO, NOX, PUF, ZIP }

    public enum State {
        IDLE, CURIOUS, BLOCKING, SNEAKING,
        SLEEPING, STARTLED, CELEBRATING,
        SULKING, SURRENDERING, LAUGHING
    }

    public interface StateListener {
        void onStateChanged(CharId id, State oldState, State newState);
        void onEmotionChanged(CharId id, float eyebrow, float mouthCurve, float eyeOpenness);
        void onPositionIntent(CharId id, float dx, float dy); // karakterin gitmek istediği yer
    }

    public final CharId id;
    private State state = State.IDLE;
    private StateListener listener;

    // Kişilik katsayıları (0-1)
    private final float curiosity;   // ne kadar meraklı
    private final float energy;      // ne kadar hareketli
    private final float caution;     // ne kadar ihtiyatlı
    private final float sociability; // diğerlerine ne kadar tepki verir

    // Duygu değerleri (anlık)
    public float eyebrow    = 0f;  // -1=üzgün, 0=normal, 1=şaşkın/mutlu
    public float mouthCurve = 0f;  // -1=üzgün, 0=düz, 1=gülümseyen
    public float eyeOpen    = 1f;  // 0=kapalı, 1=tam açık

    // Fizik
    public float x = 0f, y = 0f;         // mevcut konum (ana view'den offset)
    public float velX = 0f, velY = 0f;   // hız
    public float baseX = 0f, baseY = 0f; // ev konumu

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private boolean autonomousRunning = false;

    public CharacterBrain(CharId id, float curiosity, float energy,
                           float caution, float sociability) {
        this.id          = id;
        this.curiosity   = curiosity;
        this.energy      = energy;
        this.caution     = caution;
        this.sociability = sociability;
    }

    public void setListener(StateListener l) { this.listener = l; }

    // ── STATE GEÇİŞİ ──
    public void goTo(State next) {
        if (state == next) return;
        State old = state;
        state = next;
        applyEmotionForState(next);
        if (listener != null) listener.onStateChanged(id, old, next);
    }

    public State getState() { return state; }

    private void applyEmotionForState(State s) {
        switch (s) {
            case IDLE:
                setEmotion(0f, 0.3f, 1f, 400);
                break;
            case CURIOUS:
                setEmotion(0.6f, 0.1f, 1.1f, 300);
                break;
            case BLOCKING:
                setEmotion(0.4f, -0.1f, 1f, 250);
                break;
            case SNEAKING:
                setEmotion(0.8f, 0.2f, 0.7f, 200);
                break;
            case SLEEPING:
                setEmotion(-0.3f, 0f, 0.05f, 800);
                break;
            case STARTLED:
                setEmotion(1f, -0.4f, 1.3f, 80);
                break;
            case CELEBRATING:
                setEmotion(1f, 1f, 1.1f, 200);
                break;
            case SULKING:
                setEmotion(-0.5f, -0.6f, 0.8f, 500);
                break;
            case SURRENDERING:
                setEmotion(0.3f, 0.1f, 1f, 300);
                break;
            case LAUGHING:
                setEmotion(0.5f, 1f, 0.9f, 200);
                break;
        }
    }

    // Duyguları smooth değiştir
    private void setEmotion(float brow, float mouth, float eyes, int ms) {
        float targetBrow  = brow;
        float targetMouth = mouth;
        float targetEyes  = eyes;
        long steps = ms / 16;
        if (steps < 1) steps = 1;
        final long totalSteps = steps;
        final float[] counter = {0};

        Runnable r = new Runnable() {
            @Override public void run() {
                counter[0]++;
                float t = counter[0] / (float) totalSteps;
                if (t > 1f) t = 1f;
                eyebrow    = eyebrow    + (targetBrow  - eyebrow)    * t;
                mouthCurve = mouthCurve + (targetMouth - mouthCurve) * t;
                eyeOpen    = eyeOpen    + (targetEyes  - eyeOpen)    * t;
                if (listener != null)
                    listener.onEmotionChanged(id, eyebrow, mouthCurve, eyeOpen);
                if (counter[0] < totalSteps)
                    handler.postDelayed(this, 16);
            }
        };
        handler.post(r);
    }

    // ── AUTONOMOUS LOOP ──
    public void startAutonomous() {
        if (autonomousRunning) return;
        autonomousRunning = true;
        scheduleNextIdleAction();
    }

    public void stopAutonomous() {
        autonomousRunning = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void scheduleNextIdleAction() {
        if (!autonomousRunning) return;
        // Enerjiye göre bekleme süresi: enerjik = daha sık
        long delay = (long)(2000 + rnd.nextFloat() * (8000 - energy * 5000));
        handler.postDelayed(this::doRandomIdleAction, delay);
    }

    private void doRandomIdleAction() {
        if (!autonomousRunning || state != State.IDLE) {
            scheduleNextIdleAction();
            return;
        }

        float roll = rnd.nextFloat();

        if (roll < curiosity * 0.4f) {
            // Meraklı hareket — öne eğil
            goTo(State.CURIOUS);
            if (listener != null) listener.onPositionIntent(id, 0, -8f * curiosity);
            handler.postDelayed(() -> {
                if (state == State.CURIOUS) {
                    goTo(State.IDLE);
                    if (listener != null) listener.onPositionIntent(id, 0, 0);
                }
                scheduleNextIdleAction();
            }, 1200);

        } else if (roll < 0.3f && energy > 0.6f) {
            // Küçük zıplama (ZIP için çok)
            if (listener != null) listener.onPositionIntent(id, 0, -15f * energy);
            handler.postDelayed(() -> {
                if (listener != null) listener.onPositionIntent(id, 0, 0);
                scheduleNextIdleAction();
            }, 400);

        } else if (roll < 0.45f && curiosity < 0.4f) {
            // Uyuklama (PUF için)
            goTo(State.SLEEPING);
            handler.postDelayed(() -> {
                // İrkilme
                goTo(State.STARTLED);
                handler.postDelayed(() -> {
                    goTo(State.IDLE);
                    scheduleNextIdleAction();
                }, 600);
            }, 2500 + (long)(rnd.nextFloat() * 2000));

        } else if (roll < 0.6f) {
            // Hafif sallanma
            float dir = rnd.nextBoolean() ? 1f : -1f;
            if (listener != null) listener.onPositionIntent(id, dir * 6f, 0);
            handler.postDelayed(() -> {
                if (listener != null) listener.onPositionIntent(id, 0, 0);
                scheduleNextIdleAction();
            }, 600);

        } else {
            scheduleNextIdleAction();
        }
    }

    // ── DIŞ TETİKLEYİCİLER ──
    public void onTouched() {
        goTo(State.STARTLED);
        if (listener != null) listener.onPositionIntent(id, 0, -12f);
        handler.postDelayed(() -> {
            goTo(State.IDLE);
            if (listener != null) listener.onPositionIntent(id, 0, 0);
        }, 700);
    }

    public void onFlung(float vx, float vy) {
        velX = vx;
        velY = vy;
        goTo(State.STARTLED);
    }

    public void onCollidedWith(CharId other) {
        goTo(State.STARTLED);
        // Kaution yüksekse kızar, düşükse güler
        handler.postDelayed(() -> {
            if (caution > 0.5f) goTo(State.SULKING);
            else { goTo(State.LAUGHING); }
            handler.postDelayed(() -> goTo(State.IDLE), 1500);
        }, 400);
    }

    public void onGroupEvent(String eventType) {
        switch (eventType) {
            case "all_look":
                goTo(State.CURIOUS);
                handler.postDelayed(() -> goTo(State.IDLE), 1800);
                break;
            case "success":
                goTo(State.CELEBRATING);
                handler.postDelayed(() -> goTo(State.IDLE), 2000);
                break;
            case "error":
                goTo(State.STARTLED);
                handler.postDelayed(() -> {
                    // Kişiliğe göre farklı tepki
                    if (caution > 0.6f) goTo(State.SULKING);
                    else if (sociability > 0.6f) goTo(State.LAUGHING);
                    else goTo(State.IDLE);
                    handler.postDelayed(() -> goTo(State.IDLE), 1200);
                }, 500);
                break;
        }
    }
}
