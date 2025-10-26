package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

public class HitSound {
    private final Sound[] laneSounds;   // 각 레인별 사운드
    public HitSound() {
        laneSounds = new Sound[15];

        // 레인별 사운드 로딩
        laneSounds[0] = Gdx.audio.newSound(Gdx.files.internal("clean_0.wav"));
        laneSounds[1] = Gdx.audio.newSound(Gdx.files.internal("clean_1.wav"));
        laneSounds[2] = Gdx.audio.newSound(Gdx.files.internal("clean_2.wav"));
        laneSounds[3] = Gdx.audio.newSound(Gdx.files.internal("clean_3.wav"));
        laneSounds[4] = Gdx.audio.newSound(Gdx.files.internal("clean_4.wav"));

        laneSounds[5] = Gdx.audio.newSound(Gdx.files.internal("drive_0.wav"));
        laneSounds[6] = Gdx.audio.newSound(Gdx.files.internal("drive_1.wav"));
        laneSounds[7] = Gdx.audio.newSound(Gdx.files.internal("drive_2.wav"));
        laneSounds[8] = Gdx.audio.newSound(Gdx.files.internal("drive_3.wav"));
        laneSounds[9] = Gdx.audio.newSound(Gdx.files.internal("drive_4.wav"));

        laneSounds[10] = Gdx.audio.newSound(Gdx.files.internal("hamo_0.wav"));
        laneSounds[11] = Gdx.audio.newSound(Gdx.files.internal("hamo_1.wav"));
        laneSounds[12] = Gdx.audio.newSound(Gdx.files.internal("hamo_2.wav"));
        laneSounds[13] = Gdx.audio.newSound(Gdx.files.internal("hamo_3.wav"));
        laneSounds[14] = Gdx.audio.newSound(Gdx.files.internal("hamo_4.wav"));

        // 추가 효과음

    }

    /** 레인 번호에 해당하는 사운드 재생 */
    public void playLane(int lane) {
        if (lane < 0 || lane >= laneSounds.length) return;
        laneSounds[lane].play(1.0f);
    }

    /** 판정 결과에 따른 사운드 재생 */
    public void playJudgment(String judgment, int lane, int gamemode) {
        switch (judgment) {
            case "Perfect!":
                if(gamemode == 0) {
                    laneSounds[lane].play(1.0f, MathUtils.random(0.95f, 1.05f), 0);
                }
                else if(gamemode == 1){
                    laneSounds[lane+5].play(1.0f, 1.0f, 0);
                }
                else if(gamemode == 2){
                    laneSounds[lane+5+5].play(1.0f, 1.0f, 0);
                }
                break;
            case "Great!":
            case "Good!":
                // 일반 레인 사운드
                if(gamemode == 0){
                    laneSounds[lane].play(1.0f, 1.0f, 0);
                }
                else if(gamemode == 1){
                    laneSounds[lane+5].play(1.0f, 1.0f, 0);
                }
                else if(gamemode == 2){
                    laneSounds[lane+5+5].play(1.0f, 1.0f, 0);
                }
                break;
            case "Miss":
                break;
        }
    }

    /** 리소스 해제 */
    public void dispose() {
        for (Sound s : laneSounds) {
            if (s != null) s.dispose();
        }
    }
}
