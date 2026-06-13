package com.kendi.tarayicim;

import android.content.Context;

public class SceneManager {

    private final CharacterBrain vigoBrain;
    private final CharacterBrain noxBrain;
    private final CharacterBrain pufBrain;
    private final CharacterBrain zipBrain;
    private final CharacterBrain[] allBrains;

    private final AutonomousDirector director;
    private final CharacterView view;

    public SceneManager(Context ctx, CharacterView view) {
        this.view = view;

        // Kişilik: curiosity, energy, caution, sociability
        vigoBrain = new CharacterBrain(CharacterBrain.CharId.VIGO, 0.3f, 0.2f, 0.8f, 0.6f);
        noxBrain  = new CharacterBrain(CharacterBrain.CharId.NOX,  0.9f, 0.6f, 0.2f, 0.5f);
        pufBrain  = new CharacterBrain(CharacterBrain.CharId.PUF,  0.2f, 0.1f, 0.3f, 0.7f);
        zipBrain  = new CharacterBrain(CharacterBrain.CharId.ZIP,  0.7f, 0.9f, 0.1f, 0.8f);

        allBrains = new CharacterBrain[]{vigoBrain, noxBrain, pufBrain, zipBrain};

        // Brain'leri view'a bağla
        for (CharacterBrain b : allBrains) {
            b.setListener(new CharacterBrain.StateListener() {
                @Override
                public void onStateChanged(CharacterBrain.CharId id,
                                           CharacterBrain.State o,
                                           CharacterBrain.State n) {
                    view.onBrainStateChanged(id, n);
                }
                @Override
                public void onEmotionChanged(CharacterBrain.CharId id,
                                             float eyebrow, float mouth, float eyes) {
                    view.onEmotionChanged(id, eyebrow, mouth, eyes);
                }
                @Override
                public void onPositionIntent(CharacterBrain.CharId id, float dx, float dy) {
                    view.onPositionIntent(id, dx, dy);
                }
            });
        }

        director = new AutonomousDirector(allBrains, new AutonomousDirector.SceneCallback() {
            @Override public void onScene(String sceneId) {
                view.onSceneCue(sceneId);
            }
            @Override public void onCharacterCue(CharacterBrain.CharId id, String cue) {
                view.onCharacterCue(id, cue);
            }
        });
    }

    public void start() {
        director.start();
    }

    public void stop() {
        director.stop();
    }

    // ── FORM OLAYLARI ──
    public void onEmailFocus()     { director.onFormEvent("email_start");    }
    public void onEmailBlur()      { director.onFormEvent("email_done");     }
    public void onPasswordFocus()  { director.onFormEvent("password_focus"); }
    public void onPasswordShow()   { director.onFormEvent("password_show");  }
    public void onError()          { director.onFormEvent("error");          }
    public void onSuccess()        { director.onFormEvent("success");        }
    public void onUserActivity()   { director.resetIdleTimer();              }

    // ── DOKUNMA ──
    public void onCharacterTouched(CharacterBrain.CharId id) {
        getBrain(id).onTouched();
    }

    public void onCharacterFlung(CharacterBrain.CharId id, float vx, float vy) {
        getBrain(id).onFlung(vx, vy);
    }

    public void onCharactersCollided(CharacterBrain.CharId a, CharacterBrain.CharId b) {
        getBrain(a).onCollidedWith(b);
        getBrain(b).onCollidedWith(a);
    }

    private CharacterBrain getBrain(CharacterBrain.CharId id) {
        for (CharacterBrain b : allBrains)
            if (b.id == id) return b;
        return allBrains[0];
    }

    public CharacterBrain getBrainPublic(CharacterBrain.CharId id) {
        return getBrain(id);
    }
}
