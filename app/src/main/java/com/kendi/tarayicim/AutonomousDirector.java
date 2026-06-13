package com.kendi.tarayicim;

import android.os.Handler;
import android.os.Looper;
import java.util.Random;

public class AutonomousDirector {

    public interface SceneCallback {
        void onScene(String sceneId);
        void onCharacterCue(CharacterBrain.CharId id, String cue);
    }

    private final CharacterBrain[] brains;
    private final SceneCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private boolean running = false;
    private long idleSeconds = 0;

    // Sahneler listesi
    private static final String[] GROUP_SCENES = {
        "all_look_offscreen",
        "nox_peeks_vigo_stops",
        "zip_jumps_vigo_stares",
        "puf_sleeps_nox_pokes",
        "all_wave",
        "nox_zip_argument",
        "vigo_fixes_posture",
        "puf_rolls"
    };

    public AutonomousDirector(CharacterBrain[] brains, SceneCallback callback) {
        this.brains   = brains;
        this.callback = callback;
    }

    public void start() {
        running = true;
        for (CharacterBrain b : brains) b.startAutonomous();
        scheduleGroupScene();
        startIdleTimer();
    }

    public void stop() {
        running = false;
        for (CharacterBrain b : brains) b.stopAutonomous();
        handler.removeCallbacksAndMessages(null);
    }

    public void resetIdleTimer() { idleSeconds = 0; }

    // ── GRUP SAHNE ZAMANLAYICI ──
    private void scheduleGroupScene() {
        if (!running) return;
        long delay = 15000 + (long)(rnd.nextFloat() * 20000);
        handler.postDelayed(this::playRandomGroupScene, delay);
    }

    private void playRandomGroupScene() {
        if (!running) return;
        String scene = GROUP_SCENES[rnd.nextInt(GROUP_SCENES.length)];
        playScene(scene);
        scheduleGroupScene();
    }

    public void playScene(String sceneId) {
        if (callback != null) callback.onScene(sceneId);

        switch (sceneId) {

            case "all_look_offscreen":
                // Hepsi aynı anda sol yukarıya bakar sonra döner
                for (CharacterBrain b : brains)
                    b.onGroupEvent("all_look");
                break;

            case "nox_peeks_vigo_stops":
                // Nox öne eğilir → Vigo dur işareti → Nox çekilir
                getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.SNEAKING);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.BLOCKING);
                    callback.onCharacterCue(CharacterBrain.CharId.VIGO, "arm_raise");
                }, 600);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.SURRENDERING);
                }, 1000);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.IDLE);
                    getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.IDLE);
                }, 2000);
                break;

            case "zip_jumps_vigo_stares":
                // Zip zıplar → Vigo ona bakar → Zip durur
                getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.CELEBRATING);
                callback.onCharacterCue(CharacterBrain.CharId.ZIP, "jump");
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.CURIOUS);
                    callback.onCharacterCue(CharacterBrain.CharId.VIGO, "look_at_zip");
                }, 400);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.SULKING);
                }, 900);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.IDLE);
                    getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.IDLE);
                }, 2200);
                break;

            case "puf_sleeps_nox_pokes":
                // Puf uyur → Nox dürtür → Puf irkilir
                getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.SLEEPING);
                handler.postDelayed(() -> {
                    callback.onCharacterCue(CharacterBrain.CharId.NOX, "poke_puf");
                }, 1500);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.STARTLED);
                }, 2000);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.LAUGHING);
                }, 2200);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.SULKING);
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.IDLE);
                }, 3000);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.IDLE);
                }, 4500);
                break;

            case "nox_zip_argument":
                // Nox ve Zip birbirine bakar, iki yana döner
                getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.CURIOUS);
                getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.CURIOUS);
                callback.onCharacterCue(CharacterBrain.CharId.NOX, "look_at_zip");
                callback.onCharacterCue(CharacterBrain.CharId.ZIP, "look_at_nox");
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.SULKING);
                    getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.SULKING);
                    callback.onCharacterCue(CharacterBrain.CharId.NOX, "look_away");
                    callback.onCharacterCue(CharacterBrain.CharId.ZIP, "look_away");
                }, 1000);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.IDLE);
                    getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.IDLE);
                }, 3000);
                break;

            case "vigo_fixes_posture":
                callback.onCharacterCue(CharacterBrain.CharId.VIGO, "fix_posture");
                break;

            case "puf_rolls":
                callback.onCharacterCue(CharacterBrain.CharId.PUF, "roll_sway");
                break;

            case "all_wave":
                for (CharacterBrain b : brains)
                    b.onGroupEvent("success");
                break;
        }
    }

    // ── BOŞTA ZAMANLAYICI ──
    private void startIdleTimer() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!running) return;
                idleSeconds += 5;
                if (idleSeconds >= 30) {
                    playScene("puf_sleeps_nox_pokes");
                    idleSeconds = 0;
                } else if (idleSeconds >= 15) {
                    playScene("zip_jumps_vigo_stares");
                }
                handler.postDelayed(this, 5000);
            }
        }, 5000);
    }

    // ── GİRİŞ OLAYLARI ──
    public void onFormEvent(String event) {
        resetIdleTimer();
        switch (event) {
            case "email_start":
                getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.CURIOUS);
                callback.onCharacterCue(CharacterBrain.CharId.ZIP, "jump_small");
                handler.postDelayed(() ->
                    getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.BLOCKING),
                    300);
                break;
            case "email_done":
                getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.IDLE);
                callback.onCharacterCue(CharacterBrain.CharId.VIGO, "nod");
                getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.IDLE);
                break;
            case "password_focus":
                getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.IDLE);
                callback.onCharacterCue(CharacterBrain.CharId.PUF, "turn_back");
                handler.postDelayed(() -> playScene("nox_peeks_vigo_stops"), 500);
                break;
            case "password_show":
                getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.SNEAKING);
                callback.onCharacterCue(CharacterBrain.CharId.NOX, "peek_password");
                getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.BLOCKING);
                getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.BLOCKING);
                break;
            case "error":
                for (CharacterBrain b : brains) b.onGroupEvent("error");
                handler.postDelayed(() -> {
                    // Vigo Nox'a bakar
                    callback.onCharacterCue(CharacterBrain.CharId.VIGO, "look_at_nox");
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.SULKING);
                    getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.LAUGHING);
                }, 600);
                handler.postDelayed(() -> {
                    for (CharacterBrain b : brains) b.goTo(CharacterBrain.State.IDLE);
                }, 2500);
                break;
            case "success":
                getBrain(CharacterBrain.CharId.ZIP).goTo(CharacterBrain.State.CELEBRATING);
                callback.onCharacterCue(CharacterBrain.CharId.ZIP, "jump");
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.PUF).goTo(CharacterBrain.State.CELEBRATING);
                    callback.onCharacterCue(CharacterBrain.CharId.PUF, "turn_front");
                }, 200);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.VIGO).goTo(CharacterBrain.State.CELEBRATING);
                }, 400);
                handler.postDelayed(() -> {
                    getBrain(CharacterBrain.CharId.NOX).goTo(CharacterBrain.State.CELEBRATING);
                    callback.onCharacterCue(CharacterBrain.CharId.NOX, "cool_jump");
                }, 600);
                break;
        }
    }

    private CharacterBrain getBrain(CharacterBrain.CharId id) {
        for (CharacterBrain b : brains)
            if (b.id == id) return b;
        return brains[0];
    }
}
