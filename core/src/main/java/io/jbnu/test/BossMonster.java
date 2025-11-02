//Monster.java
package io.jbnu.test;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class BossMonster {
    // --- 1. 상태 (데이터) ---
    public Vector2 position; // 위치
    public Vector2 velocity; // 속도
    // --- 2. 그래픽 (데이터) ---
    public Sprite sprite;    // 그리기용 스프라이트
    public Rectangle bounds;
    public boolean isGrounded = false; // '땅에 닿아있는가?' (점프 가능 여부)
    public int hp;
    public float CharaterSize_width = 200;
    public float CharaterSize_height = 200;
    public float frition = 0.8f;
    /**
     * 캐릭터 생성자
     * @param texture 이 캐릭터가 사용할 텍스처 (외부에서 로드해서 전달)
     * @param startX 시작 X 위치
     * @param startY 시작 Y 위치
     */
    public BossMonster(Texture texture, float startX, float startY) {
        // 물리 상태 초기화
        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(0, 0); // 처음엔 정지

        //캐릭터 체력 관리
        this.hp = 10;

        // 그래픽 상태 초기화
        this.sprite = new Sprite(texture);
        this.sprite.setPosition(position.x, position.y);
        this.sprite.setSize(CharaterSize_width, CharaterSize_height);

        this.bounds = new Rectangle(position.x, position.y, sprite.getWidth(), sprite.getHeight());
    }

    public void jump() {
        if (isGrounded) {
            velocity.y = 800f; // Y축으로 점프 속도 설정
            isGrounded = false; // 점프했으니 땅에서 떨어짐
        }
    }

    // --- 3. 행동 (메서드) ---
    public void moveRight() {
        if(isGrounded == true) {
            velocity.x += 20f;
        }
    }
    public void moveLeft() {
        if(isGrounded == true) {
            velocity.x -= 20f;
        }
    }


    public void syncSpriteToPosition() {
        sprite.setPosition(position.x, position.y);
    }

    /**
     * 자신을 그립니다.
     */
    public void draw(SpriteBatch batch) {
        sprite.draw(batch);
    }
}
