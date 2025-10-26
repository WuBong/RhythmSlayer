//RythmGameManager.java
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class RhythmGameManager {
    private boolean active = false;
    private final RhythmGameListener listener;

    private HitSound hitSound;
    private int gamemode; // 0 clean 1 drive 2 hamo

    // 시각화용
    private final BitmapFont font;
    private final ShapeRenderer shape;

    // 레이아웃 기본값 (Main의 viewport 800x600 기준)
    private final float viewWidth  = 800f;
    private final float viewHeight = 600f;

    // 레인 설정 (5레인: A, S, D, J, K)
    private final int laneCount = 5;
    private final float laneWidth = 100f;
    private final float laneGap   = 30f;
    private final float lanesY0   = 80f;   // 레인 하단 시작점
    private final float hitLineY  = 150f;  // 판정선 높이

    //노트관리
    private Array<Note> notes;
    private Texture noteTexture;
    private float spwanTimer;
    private float spawnInterval= 1.2f; // 나중에는 랜덤으로 바꿀 예정
    private float notespeed = 300f;
    private int totalNotes = 5;

    float noteWidth = laneWidth * 0.8f;
    float noteHeight = 20f;

    private final float[] laneX; // 각 레인의 X 위치

    //입력키와 판정 범위
    private final int[] keyCodes = {Input.Keys.A, Input.Keys.S, Input.Keys.D, Input.Keys.J, Input.Keys.K};
    private final float perfectRange = 25f;
    private final float greateRange = 50f;
    private final float goodRange = 75f;


    //점수 관련 변수
    private int combo = 0;
    private int score = 0;
    private String lastJudgement;
    private float judgmentTimer;

    //게임 결과 판정을 위한 변수
    private int successCount = 0; // 성공 판정
    private int missCount = 0;   //  Miss 횟수
    private final int VICTORY_SCORE = 800; //승리 판정을 위한 최소 점수

    // --- 레인 하이라이트 효과를 위한 변수 ---
    private final float[] laneHighlightTimers;
    private final float HIGHLIGHT_DURATION = 0.5f;
    private Color highlightColor = new Color(1, 1, 0.6f, 0.4f);

    // 리듬 게임 차트
    private RhythmChart chart;
    private int nextNoteIndex = 0;
    private float songTimer = 0f;
    public RhythmGameManager(RhythmGameListener listener) {
        this.listener = listener;
        this.font = new BitmapFont();
        this.font.setColor(Color.BLACK);
        this.font.getData().setScale(2f);

        //노트 텍스쳐
        this.noteTexture = new Texture("note.png");

        this.shape = new ShapeRenderer();

        // --- 레인 X 좌표 계산 ---
        this.laneX = new float[laneCount];
        float totalWidth = (laneWidth * laneCount) + (laneGap * (laneCount - 1));
        float startX = (viewWidth - totalWidth) / 2f;
        for (int i = 0; i < laneCount; i++) {
            laneX[i] = startX + i * (laneWidth + laneGap);
        }
        this.notes =  new Array<>();

        // --- 레인 하이라이트, 색 ,타이머 배열 초기화 ---
        this.laneHighlightTimers = new float[laneCount];

        hitSound = new HitSound();
        gamemode = 0;
    }

    public void enterRythmMode(int drive) {  //리듬게임 초기화
        Gdx.app.log("Rhythm", "리듬게임 (5레인)");
        active = true;
        spwanTimer = 0;
        notes.clear();

        gamemode = drive;

        score = 0;
        combo = 0;
        successCount = 0;
        missCount = 0;
        lastJudgement = "";
        judgmentTimer = 0f;

        for(int i = 0; i < laneCount; i++){
            laneHighlightTimers[i] = 0;
        }
    }

    public void update(float delta) {
        if (!active) return;

        if(judgmentTimer > 0){
            judgmentTimer -= delta;
        }

        // --- 하이라이트 타이머 감소 로직 ---
        for(int i = 0; i < laneCount; i++){
            if(laneHighlightTimers[i] > 0){
                laneHighlightTimers[i] -= delta;
            }
        }


        // 테스트용: Q 키로 종료
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            System.out.println("리듬모드 종료 입력 감지됨!");
            endRhythmGame();
        }

        if(successCount + missCount >= totalNotes){
            endRhythmGame();
            return;
        }

        //노트 생성 타이머
        spwanTimer += delta;
        if(spwanTimer > spawnInterval){
            spwanTimer = 0f;
            int lane = MathUtils.random(0, laneCount - 1);
            notes.add(new Note(lane, viewHeight+50f, notespeed, noteTexture, noteWidth, noteHeight));
        }


        for (int i = notes.size - 1; i >= 0; i--) {
            Note n = notes.get(i);
            n.update(delta, laneX[n.lane], laneWidth);

            if (n.isMissed(hitLineY, goodRange)){   //MISS 처리
                handleMiss();
                notes.removeIndex(i);
            }
        }

        handleInput();
    }

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        if (!active) return;

        // --- 렌더링 순서 정리 ---
        // 0. 렌더러들의 카메라 설정
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // 1. SpriteBatch를 잠시 멈추고 ShapeRenderer로 모든 도형을 먼저 그린다.
        batch.end();

        // 1-1. 하이라이트 효과 그리기 (채워진 사각형)
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(highlightColor);
        for(int i = 0; i < laneCount; i++){
            if(laneHighlightTimers[i] > 0){
                shape.rect(laneX[i], lanesY0, laneWidth, viewHeight - lanesY0 - 40f);
            }
        }

        shape.end();

        // 1-2. 레인 외곽선과 판정선 그리기 (선)
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.DARK_GRAY);
        for (int i = 0; i < laneCount; i++) {
            drawLaneRect(laneX[i], lanesY0, laneWidth, viewHeight - lanesY0 - 40f);
        }
        shape.setColor(Color.FOREST);
        shape.line(laneX[0] - 20f, hitLineY - (noteHeight/2), laneX[laneCount - 1] + laneWidth + 20f, hitLineY - (noteHeight/2));
        shape.line(laneX[0] - 20f, hitLineY+(noteHeight/2), laneX[laneCount - 1] + laneWidth + 20f, hitLineY+(noteHeight/2));
        shape.end();

        // 2. ShapeRenderer 작업이 모두 끝났으니, SpriteBatch를 다시 시작한다.
        batch.begin();

        // 3. 이제 모든 노트와 폰트를 그린다.
        for (Note n : notes) {
            n.render(batch);
        }

        // HUD 렌더링
        font.draw(batch, "Score: " + score, 20f, viewHeight - 20f);
        font.draw(batch, (successCount + missCount) + " / " + totalNotes, viewWidth - 150f, viewHeight - 20f);
        if (combo > 1) {
            font.draw(batch, "Combo: " + combo, viewWidth / 2f - 70f, viewHeight / 2f + 100f);
        }
        if (judgmentTimer > 0) {
            font.draw(batch, lastJudgement, viewWidth / 2f - 70f, viewHeight / 2f + 200f);
        }
        font.draw(batch, "[A]", laneX[0] + laneWidth/2 -15, lanesY0+60);
        font.draw(batch, "[S]", laneX[1] + laneWidth/2 -15, lanesY0+60);
        font.draw(batch, "[D]", laneX[2] + laneWidth/2 -15, lanesY0+60);
        font.draw(batch, "[J]", laneX[3] + laneWidth/2 -15, lanesY0+60);
        font.draw(batch, "[K]", laneX[4] + laneWidth/2 -15, lanesY0+60);
    }

    public void dispose() {
        font.dispose();
        shape.dispose();
        noteTexture.dispose();
    }

    private void handleMiss(){  //miss
        combo = 0;
        missCount++;
        lastJudgement = "Miss";
        judgmentTimer = 0.5f;
        System.out.println("Miss! Total misses: " + missCount);
    }
    private void handleInput() {
        for(int i = 0; i < laneCount; i++){
            if(Gdx.input.isKeyJustPressed(keyCodes[i])){
                judge(i);
                laneHighlightTimers[i] = HIGHLIGHT_DURATION;
            }
        }
    }

    private void judge(int lane){
        for(int i = 0; i < notes.size; i++){
            Note note = notes.get(i);
            if(note.lane == lane){
                float distance = Math.abs(note.y - hitLineY);
                if(distance <= perfectRange){
                    lastJudgement = "Perfect!";
                    score += 300;
                    combo++;
                    successCount++;
                    notes.removeIndex(i);
                    judgmentTimer = 0.5f;
                    hitSound.playJudgment(lastJudgement, lane, gamemode);
                    return;
                } else if (distance <= greateRange){
                    lastJudgement = "Great!";
                    score += 200;
                    combo++;
                    successCount++;
                    notes.removeIndex(i);
                    judgmentTimer = 0.5f;
                    hitSound.playJudgment(lastJudgement, lane, gamemode);
                    return;
                } else if(distance <= goodRange){
                    lastJudgement = "Good!";
                    score += 100;
                    combo++;
                    successCount++;
                    notes.removeIndex(i);
                    judgmentTimer = 0.5f;
                    hitSound.playJudgment(lastJudgement, lane, gamemode);
                    return;
                }
            }
        }
    }

    private void drawLaneRect(float x, float y, float w, float h){
        shape.rect(x,y,w,h);
    }

    private void endRhythmGame() {
        if(!active) return; // 중복 호출 방지

        active = false;
        boolean isSuccess = (score >= VICTORY_SCORE);
        System.out.println("리듬 게임 종료! 최종 점수: " + score + ", 승리 여부: " + isSuccess);

        if (listener != null) {
            listener.onRhythmGameEnd(isSuccess); //  승리/패배 결과를 담아 콜백 호출
        }
    }

    public boolean isActive() { return active; }
}
