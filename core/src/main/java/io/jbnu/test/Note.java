package io.jbnu.test;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;


public class Note {
    public int lane;
    public float y;
    public float speed;
    private Texture texture;
    public Sprite sprite;
    //public Rectangle bounds;

    private float width;
    private float height;

    public boolean isVisible = true;


    public Note(int lane, float startY, float speed, Texture texture, float width, float height){
        this.lane = lane;
        this.y = startY;
        this.speed = speed;
        this.texture = texture;
        this.width = width;
        this.height = height;

        this.sprite = new Sprite(texture);
        this.sprite.setSize(width, height);
        this.sprite.setOriginCenter();

        //bound도 추가해야될까?
    }

    public void update(float delta, float laneX, float laneWidth){
        //y 좌표 갱신
        y -= speed * delta;

        //x 좌표 계산
        float drawX = laneX + (laneWidth - width) / 2f;

        //노트 위치 갱신
        sprite.setPosition(drawX, y);
    }

    public void render(SpriteBatch batch){
        if(!isVisible) return;
        sprite.draw(batch);
    }


    public boolean isOutOfScreen(float hitLineY){
        return y + height < hitLineY - 200f;
    }

    public boolean isMissed(float hitLineY, float missRange){
        //노트의 y 좌표가 판정선보다 missRange 만큼 더 아래로 내려가면 true
        return y < hitLineY - missRange;
    }

}
