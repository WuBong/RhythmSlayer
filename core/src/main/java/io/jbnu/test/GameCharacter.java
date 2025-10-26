// GameCharacter.java
package io.jbnu.test;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class GameCharacter {
    // === 상태/물리 ===
    public Vector2 position;
    public Vector2 velocity;
    public boolean isGrounded = false;
    public int hp;
    public float frition = 0.8f;          //
    public int drive = 0;

    // === 쿨타임/공격 락 ===
    private float attackCooldown = 0.5f; //쿨타임
    private float attackCDTimer = 0f;
    private float attackLocktimer = 0f;

    // === 렌더/충돌 ===
    public Rectangle bounds;
    public float CharaterSize_width = 64f;
    public float CharaterSize_height = 64f;

    // === 애니메이션 ===
    enum State { IDLE, RUN, JUMP, ATTACK, DAMEGED }
    private State state = State.IDLE;
    public boolean facingRight = true;
    private float stateTime = 0f;

    private Animation<TextureRegion> idleAnim, runAnim, jumpAnim, onDamagedAnim, attackaAnim;
    private TextureRegion currentFrame;

    // 시트 정보
    private final Texture sheet;
    private final int FRAME_W, FRAME_H;
    private final float idleFrameDur, runFrameDur, jumpFrameDur, onDamegedDur, attackFrameDur;

    /**
     * 스프라이트 시트 기반 캐릭터
     * @param sheetTexture 스프라이트 시트 텍스처(그리드형)
     * @param startX 시작 X
     * @param startY 시작 Y
     * @param drive  드라이브(원 코드 호환)
     * @param frameW 프레임 폭(px)  (예: 64)
     * @param frameH 프레임 높이(px) (예: 64)
     * @param idleRow  idle이 있는 행 index (0부터)
     * @param idleFrames idle 프레임 개수
     * @param runRow   run이 있는 행 index
     * @param runFrames run 프레임 개수
     * @param jumpRow  jump가 있는 행 index
     * @param jumpFrames jump 프레임 개수
     */
    public GameCharacter(Texture sheetTexture, float startX, float startY, int drive,
                         int frameW, int frameH,
                         int idleRow, int idleFrames,
                         int runRow, int runFrames,
                         int jumpRow, int jumpFrames,
                         int onDamegedRow, int onDamegedFrame,
                         int attackRow, int attackFrame) {

        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(0, 0);
        this.hp = 10;
        this.drive = drive;

        this.sheet = sheetTexture;
        this.FRAME_W = frameW;
        this.FRAME_H = frameH;

        // 프레임 시간(원하는 속도로 조절)
        this.idleFrameDur = 0.12f;
        this.runFrameDur  = 0.08f;
        this.jumpFrameDur = 0.10f;
        this.onDamegedDur = 0.12f;
        this.attackFrameDur = 0.05f;

        // 그리드 분할
        TextureRegion[][] grid = TextureRegion.split(sheet, FRAME_W, FRAME_H);

        // 각 행에서 지정 개수만큼 프레임 추출
        this.idleAnim = new Animation<>(idleFrameDur, takeRow(grid, idleRow, idleFrames), Animation.PlayMode.LOOP);
        this.runAnim  = new Animation<>(runFrameDur,  takeRow(grid, runRow,  runFrames),  Animation.PlayMode.LOOP);
        this.jumpAnim = new Animation<>(jumpFrameDur, takeRow(grid, jumpRow, jumpFrames), Animation.PlayMode.NORMAL);
        this.onDamagedAnim = new Animation<>(onDamegedDur, takeRow(grid, onDamegedRow, onDamegedFrame), Animation.PlayMode.NORMAL);
        this.attackaAnim = new Animation<>(attackFrameDur, takeRow(grid, attackRow, attackFrame), Animation.PlayMode.NORMAL);

        // 충돌 박스
        this.bounds = new Rectangle(position.x, position.y, CharaterSize_width, CharaterSize_height);
        this.currentFrame = idleAnim.getKeyFrame(0); // 초기 프레임
        syncBoundsToPosition();
    }

    /** 특정 행(row)에서 0..count-1 프레임만 Array로 가져오기 */
    private Array<TextureRegion> takeRow(TextureRegion[][] grid, int row, int count) {
        Array<TextureRegion> arr = new Array<>(count);
        for (int i = 0; i < count; i++) arr.add(grid[row][i]);
        return arr;
    }

    // === 입력/행동 ===
    public void jump() {
        if (isGrounded) {
            velocity.y = 800f;
            isGrounded = false;
            state = State.JUMP;
            stateTime = 0f; // 점프 시작 시 시간 리셋
        }
    }
    public void moveRight() {
        if (isGrounded) {
            velocity.x += 50f;
        }
        facingRight = true;
    }
    public void moveLeft() {
        if (isGrounded) {
            velocity.x -= 50f;
        }
        facingRight = false;
    }

    public boolean canAttack() {
        if (state == State.DAMEGED) return false;

        return attackCDTimer <= 0f && attackLocktimer <= 0f;
    }
    /** 몬스터 피격 리액션 (원 코드 유지) */
    public void damagedPlayer(boolean monsterIsLeft) {
        isGrounded = false;
        hp -= 2f;
        if (monsterIsLeft) { // 몬스터가 왼쪽
            velocity.x = 150f;
            velocity.y = 600f;
            facingRight = true;
        } else {             // 몬스터가 오른쪽
            velocity.x = -150f;
            velocity.y = 600f;
            facingRight = false;
        }
        state = State.DAMEGED;
        stateTime = 0f;
    }

    public void attack(){
        if(!canAttack()) return;

        if (state == State.DAMEGED) return; // 피격 중엔 공격 막기 (선택)
        state = State.ATTACK;
        stateTime = 0f;

        attackCDTimer = attackCooldown;
        attackLocktimer = attackaAnim.getAnimationDuration();
    }

    /** 물리 갱신 이후 애니메이션 상태/프레임 갱신: GameWorld.update()에서 매 프레임 호출 추천 */
    public void updateAnimation(float delta) {
        stateTime += delta;

        //공격 쿨타임
        if(attackCDTimer > 0f) attackCDTimer -= delta;
        if(attackLocktimer > 0f) attackLocktimer -= delta;

        // 상태 결정: 피격 우선 점프 우선, 그다음 속도
        if ((state == State.DAMEGED)) {

        }
        else if (state == State.ATTACK) {

        }
        else if (!isGrounded) {
            state = State.JUMP;
        }
        else {
            state = (Math.abs(velocity.x) > 5f) ? State.RUN : State.IDLE;
        }

        // 달리기 속도에 따라 가변 프레임 속도 (옵션)
        float speed01 = MathUtils.clamp(Math.abs(velocity.x) / 300f, 0f, 1f);
        runAnim.setFrameDuration(MathUtils.lerp(0.12f, 0.06f, speed01));

        // 현재 프레임 선택
        switch (state) {
            case DAMEGED:
                currentFrame = onDamagedAnim.getKeyFrame(stateTime, false);
                //  피격 애니 끝났으면 다음 상태로 전환
                if (onDamagedAnim.isAnimationFinished(stateTime)) {
                    stateTime = 0f;
                    state = isGrounded ? State.IDLE : State.JUMP;
                }
                break;
            case RUN:  currentFrame = runAnim.getKeyFrame(stateTime, true); break;
            case ATTACK:
                currentFrame = attackaAnim.getKeyFrame(stateTime, false);
                if (attackaAnim.isAnimationFinished(stateTime) && attackLocktimer <= 0f) {
                    stateTime = 0f;
                    state = isGrounded ? State.IDLE : State.JUMP;
                }
                break;
            case JUMP: currentFrame = jumpAnim.getKeyFrame(stateTime, false);
                if (jumpAnim.isAnimationFinished(stateTime) && isGrounded) {
                    // 착지 후 자동 IDLE
                    state = State.IDLE;
                    stateTime = 0f;
                    currentFrame = idleAnim.getKeyFrame(stateTime, true);
                }
                break;

            default:   currentFrame = idleAnim.getKeyFrame(stateTime, true);
        }

        // 충돌 박스는 월드 좌표/표시 크기를 기준으로 동기화
        bounds.set(position.x, position.y, CharaterSize_width, CharaterSize_height);
    }


    /** 렌더: 좌/우 반전 시 프레임을 뒤집는 대신 음수 폭으로 그려 깔끔하게 처리 */
    public void draw(SpriteBatch batch) {
        if (facingRight) {
            batch.draw(currentFrame, position.x, position.y, CharaterSize_width, CharaterSize_height);
        } else {
            // x를 폭만큼 오른쪽으로 밀고 음수 폭으로 그리면 좌우 반전
            batch.draw(currentFrame, position.x + CharaterSize_width, position.y, -CharaterSize_width, CharaterSize_height);
        }
    }

    public void syncBoundsToPosition() {
        if (bounds == null) bounds = new Rectangle();
        bounds.set(position.x, position.y, CharaterSize_width, CharaterSize_height);
    }

    //이 밑은 게터임
    public boolean isFacingRight() {
        return facingRight;
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getWidth()  { return CharaterSize_width; }
    public float getHeight() { return CharaterSize_height; }
    public boolean getDamagedState() {
        if(state == State.DAMEGED)
            return true;
        return false;
        }
}
