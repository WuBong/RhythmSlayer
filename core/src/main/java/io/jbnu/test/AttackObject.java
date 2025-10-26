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

public class AttackObject {
    // === 상태/물리 ===
    public Vector2 position;
    public Vector2 velocity;


    // === 렌더/충돌 ===
    public Rectangle bounds;
    public float CharaterSize_width = 64f;
    public float CharaterSize_height = 64f;

    // === 애니메이션 ===
    enum State { IDLE}
    private State state = State.IDLE;
    private boolean facingRight = true;
    private float stateTime = 0f;

    private Animation<TextureRegion> idleAnim;
    private TextureRegion currentFrame;

    // 시트 정보
    private final Texture sheet;
    private final int FRAME_W, FRAME_H;
    private final float idleFrameDur;

    /**
     * 스프라이트 시트 기반 캐릭터
     * @param sheetTexture 스프라이트 시트 텍스처(그리드형)
     * @param startX 시작 X
     * @param startY 시작 Y
     * @param frameW 프레임 폭(px)  (예: 64)
     * @param frameH 프레임 높이(px) (예: 64)
     * @param idleRow  idle이 있는 행 index (0부터)
     * @param idleFrames idle 프레임 개수
     */
    public AttackObject(Texture sheetTexture, float startX, float startY, float velocity_x, float velocity_y,
                         int frameW, int frameH,
                         int idleRow, int idleFrames) {

        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(velocity_x, velocity_y);

        this.sheet = sheetTexture;
        this.FRAME_W = frameW;
        this.FRAME_H = frameH;

        // 프레임 시간(원하는 속도로 조절)
        this.idleFrameDur = 0.12f;

        // 그리드 분할
        TextureRegion[][] grid = TextureRegion.split(sheet, FRAME_W, FRAME_H);

        // 각 행에서 지정 개수만큼 프레임 추출
        this.idleAnim = new Animation<>(idleFrameDur, takeRow(grid, idleRow, idleFrames), Animation.PlayMode.LOOP);

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

    /** 물리 갱신 이후 애니메이션 상태/프레임 갱신: GameWorld.update()에서 매 프레임 호출 추천 */
    public void updateAnimation(float delta) {
        stateTime += delta;

        // 1) 이동
        position.x += velocity.x * delta;
        position.y += velocity.y * delta;

        // 상태 결정: 피격 우선 점프 우선, 그다음 속도
        if ((state == State.IDLE)) {

        }
        // 2) 애니 프레임 선택 (IDLE만 쓰는 구조라면 아래 한 줄이면 충분)
        currentFrame = idleAnim.getKeyFrame(stateTime, true);

        // 달리기 속도에 따라 가변 프레임 속도 (옵션)
        float speed01 = MathUtils.clamp(Math.abs(velocity.x) / 300f, 0f, 1f);
        idleAnim.setFrameDuration(MathUtils.lerp(0.12f, 0.06f, speed01));

        // 현재 프레임 선택
        switch (state) {
            case IDLE:
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

    public boolean isFacingRight() {
        return facingRight;
    }
}
