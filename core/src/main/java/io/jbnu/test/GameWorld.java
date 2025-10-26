//GameWorld.java
package io.jbnu.test;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

public class GameWorld {
    //
    public final float WORLD_GRAVITY = -9.8f * 200; // 초당 중력 값
    public final float FLOOR_LEVEL = 0;          // 바닥의 Y 좌표

    //각종 매니저

    // --- 2. 월드 객체 ---
    private GameCharacter player;
    private final float OBJECT_SPAWN_TIME = 2.0f; // 2초마다 오브젝트 생성
    private float objectSpawnTimer = OBJECT_SPAWN_TIME; // 타이머
    private Array<CoinObject> objects; // 떨어지는 오브젝트들을 담을 배열
    private Array<Block> blocks;
    private Array<Item> items;
    private Array<AttackObject> attackObjects;

    private Monster currentOpponent;// 현재 리듬게임 대상 중인 몬스터

    private Array<Monster> monsters;
    private int score;

    private Texture playerTexture;
    private Texture objectTexture;
    private Texture blockTexture;
    private Texture monsterTexture;
    private Texture itemTexture;
    private Texture attackObjectTexture;

    private float worldWidth; // 랜덤 위치 생성을 위해 월드 너비 저장
    private float worldheight;

    //main과 간접 통신할 콜백 리스너
    private GameWorldListener listener;
    //리듬게임 매니저

    private int level = 1; //game의 레벨

    public GameWorld (Texture playerTexture, Texture objectTexture,
                     Texture blockTexture, Texture monsterTexture, Texture itemTexture, Texture attackObjectTexture,
                     float worldWidth, float worldheight, GameWorldListener listener)  {


        //텍스쳐 불러오기
        this.playerTexture = playerTexture;
        this.objectTexture = objectTexture;
        this.blockTexture = blockTexture;
        this.monsterTexture = monsterTexture;
        this.itemTexture = itemTexture;
        this.attackObjectTexture = attackObjectTexture;

        this.worldWidth = worldWidth;
        this.worldheight = worldheight;

        //콜백 리스너
        this.listener = listener;

        //리듬게임 매니저
        //오브젝트 위치 선언
        //player = new GameCharacter(playerTexture, worldWidth / 2, FLOOR_LEVEL, 0);
        player = new GameCharacter(playerTexture, worldWidth /2, 300
                                    ,0,
            128, 128,    // frameW, frameH
            8, 14,      // idle: row 8, 14 frames
            0, 9,      // run : row 0, 9 frames
            9, 12,       // jump: row 9, 12 frames
            4, 6,                   //Ondameged row 2, 4 frames
            3, 4                    // attack : row 3, 4 frames
        );

        monsters = new Array<>();
        objects = new Array<>();
        blocks = new Array<>();
        items = new Array<>();
        attackObjects = new Array<>();
        score = 0;

        loadGround(10, 100);
        monsterSpawning(1, 200);
        ItemSpawning(1, 300);

        //충돌 매니저
    }

    public void update(float delta) { //물리업데이트
        // --- 1. 힘 적용 (중력, 저항) ---
        player.velocity.y += WORLD_GRAVITY * delta;
        updateSpawning(delta);

        if(player.isGrounded == true){
            player.velocity.x *= player.frition;
        }

        // --- 2. '예상' 위치 계산 ---
        // (이번 프레임에 이동할 거리)
        float newX = player.position.x + player.velocity.x * delta;
        float newY = player.position.y + player.velocity.y * delta;

        for (Iterator<CoinObject> iter = objects.iterator(); iter.hasNext(); ) {
            CoinObject obj = iter.next();
            obj.update(delta);
            // 화면 밖으로 나간 오브젝트는 제거
            if (obj.position.y < FLOOR_LEVEL - obj.sprite.getHeight()) {
                iter.remove();
            }
        }

        // === 공격 오브젝트 업데이트 ===
        for (Iterator<AttackObject> iter = attackObjects.iterator(); iter.hasNext();) {
            AttackObject atk = iter.next();
            atk.updateAnimation(delta); // ← 이동 및 애니메이션
            // 화면 밖이면 제거
            if (atk.position.x < -100 || atk.position.x > worldWidth + 100) {
                iter.remove();
            }
        }

        // --- 3 & 4. 충돌 검사 및 반응 ---

        // 스크린 바닥(FLOOR_LEVEL)과 충돌 검사
        if (newY <= FLOOR_LEVEL) {
            newY = FLOOR_LEVEL;       // 바닥에 강제 고정
            player.velocity.y = 0;    // Y축 속도 리셋
            player.isGrounded = true; // '땅에 닿음' 상태로 변경
        } else {
            player.isGrounded = false; // 공중에 떠 있음
        }


        checkCoinCollisions();
        checkBlockCollisions(player.velocity.x * delta);
        checkBlockCollisionsY(player.velocity.y * delta);
        checkMonsterCollision();
        checkItemCollisions();

        // --- 6. 그래픽 동기화 ---
        player.updateAnimation(delta);
    }


    //오브젝트 소환 부
    private void monsterSpawning(int numberOfMonster, float spaceSize){
        // 첫 블록의 시작 X 위치 (400을 기준으로 왼쪽으로 블록 너비의 절반만큼 이동)
        float startX = 100;
        float startY = 100; // 바닥 높이

        for(int i = 0; i < numberOfMonster; i++){
            // i * spaceSize 만큼 오른쪽으로 이동하며 블록 배치
            float x = startX + (i * spaceSize);

            // blocks 배열에 Block 객체 추가 (코드를 blocks로 수정)
            monsters.add(new Monster(monsterTexture,x, startY));
        }
    }
    private void ItemSpawning(int numberOfItem, float spaceSize){
        float startX = 900;
        float startY = 100; // 바닥 높이

        for(int i = 0; i < numberOfItem; i++){
            // i * spaceSize 만큼 오른쪽으로 이동하며 배치
            float x = startX + (i * spaceSize);

            // blocks 배열에 Block 객체 추가 (코드를 blocks로 수정)
            items.add(new Item(itemTexture, x, startY));
        }
    }
    private void updateSpawning(float delta) {
        objectSpawnTimer -= delta;
        if (objectSpawnTimer <= 0) {
            objectSpawnTimer = OBJECT_SPAWN_TIME; // 타이머 리셋

            // 월드 너비 안에서 랜덤한 X 위치 선정
            float randomX = MathUtils.random(0, worldWidth - CoinObject.CoinWidth);
            float startY = 720; // 월드 높이 (예시)
            float speed = -100f; // 떨어지는 속도

            for(int i = 1; i < level; i++) {
                speed *= 2;
            }
            CoinObject newObject = new CoinObject(objectTexture, randomX, startY, speed);
            objects.add(newObject);
        }
    }

    private void loadGround(int numberOfBlocks, float spaceSize){
        // 첫 블록의 시작 X 위치 (400을 기준으로 왼쪽으로 블록 너비의 절반만큼 이동)
        float startX = 400 - Block.BlockWidth/2;
        float startY = 0; // 바닥 높이

        for(int i = 0; i < numberOfBlocks; i++){
            // i * spaceSize 만큼 오른쪽으로 이동하며 블록 배치
            float x = startX + (i * spaceSize);

            // blocks 배열에 Block 객체 추가 (코드를 blocks로 수정)
            blocks.add(new Block(blockTexture, x, startY));
        }
    }

    private void attackObjects_spawning() {
        if(getCanAttack()) return; //쿨타임과 소환 오브젝트 일치화
        final int projW = 64;
        final int projH = 64;

        boolean facingRight = player.isFacingRight();
        float speed = facingRight ? 600f : - 600f;

        float startX = player.position.x + (facingRight ? player.CharaterSize_width : - projW);
        float startY = player.position.y + player.CharaterSize_height * 0.5f - projH* 0.5f;

        // 실제로 AttackObject
        attackObjects.add(new AttackObject(
                attackObjectTexture,
            startX, startY,
            speed, 0f,
            projW, projH,
            0, 6));

    }

    //충돌관리영역
    private void checkBlockCollisions(float moveAmount){
        float expectedX = player.position.x + moveAmount;

        player.position.x = (expectedX);
        Rectangle playerBounds = player.bounds;
        player.syncBoundsToPosition();

        boolean collision = false;
        for(Block block : getBlocks()){
            if(playerBounds.overlaps(block.bounds)) {

                collision = true;
                if (moveAmount > 0) {
                    player.velocity.x = 0;
                    player.position.x = block.bounds.x - player.CharaterSize_width;
                    System.out.println("왼쪽 벽에서 충돌!");
                    System.out.println("플레이어 위치:");
                    System.out.println(player.position.x);
                    System.out.println(block.position.x);
                } else if (moveAmount < 0) {
                    player.velocity.x = 0;
                    player.position.x = block.bounds.x + block.bounds.width;
                    System.out.println("오른 벽에서 충돌!");
                    System.out.println("플레이어 위치:");
                    System.out.println(player.position.x);
                    System.out.println(block.position.x);
                }
                player.syncBoundsToPosition();
                break;

            }
        }
        if(!collision){
            player.position.x = expectedX;
            player.syncBoundsToPosition();
        }
        player.position.x = (player.position.x);
     //   player.syncSpriteToPosition();
    }

    private void checkBlockCollisionsY(float moveAmount) {
        float expectedY = player.position.y + moveAmount;

        // 위치를 잠시 이동해서 충돌 판정
        player.position.y= (expectedY);
        player.syncBoundsToPosition();
        Rectangle playerBounds = player.bounds;

        boolean collision = false;
        for (Block block : getBlocks()) {
            if (playerBounds.overlaps(block.bounds)) {

                collision = true;
                if (moveAmount > 0) {
                    // 위로 이동 중 천장 충돌
                    player.velocity.y = 0; // 위로 더 못 가게
                    player.position.y = block.bounds.y - player.CharaterSize_height;

                    System.out.println("천장에 충돌!");
                } else if (moveAmount < 0) {
                    // 아래로 이동 중 바닥 충돌
                    player.velocity.y = 0;
                    player.position.y = block.bounds.y + block.bounds.height;

                    player.isGrounded = true; // 바닥에 닿았다고 표시
                    System.out.println("바닥에 충돌!");
                }
                player.syncBoundsToPosition();
                break;
            }
        }

        if (!collision) {
            player.position.y = expectedY;
            if (moveAmount < 0) player.isGrounded = false;
            player.syncBoundsToPosition();
        }

        player.position.y = (player.position.y);
        //player.syncSpriteToPosition();
    }

    private void checkItemCollisions(){
        player.syncBoundsToPosition();
        for(Item item : getItems()) {
            if (player.bounds.overlaps(item.bounds)){
                removeItem(item);
                player.drive = 1;
            }
        }
    }
    private void checkCoinCollisions() {
        player.syncBoundsToPosition();
        // 플레이어와 떨어지는 오브젝트들의 충돌 검사
        for (Iterator<CoinObject> iter = objects.iterator(); iter.hasNext(); ) {
            CoinObject obj = iter.next();
            if (player.bounds.overlaps(obj.bounds)) {
                // 충돌 발생!
                score++; // 점수 1점 증가
                if(score == 10){
                    score = 0;
                    level++;
                }
                System.out.println("Score: " + score); // 콘솔에 점수 출력 (테스트용)
                iter.remove(); // 충돌한 오브젝트는 즉시 제거
            }
        }
    }

    private void checkMonsterCollision() {
        boolean collision = false;
        for(Monster monster : getMonsters()) {
            if(player.bounds.overlaps(monster.bounds)){
                collision = true;
                System.out.println("몬스터와 충돌!");
                currentOpponent = monster;
                if(listener != null){
                    listener.onMonsterCollision();
                }
            }
            break;
        }
        if(!collision) {
            collision = false;
        }
    }

    public void onRhythmBattleEnd(boolean playerWon) {
        if (currentOpponent == null) return;

        if (playerWon) {
            // 플레이어가 이겼을 때
            System.out.println("전투 승리! 몬스터를 제거합니다.");
            removeMonster(currentOpponent);
        } else {
            // 플레이어가 졌을 때
            player.damagedPlayer(player.position.x >= currentOpponent.position.x);
            System.out.println("전투 패배! 플레이어 HP가 감소합니다. 현재 HP: " + player.hp);
        }
        currentOpponent = null; // 전투 상태 해제

        // 플레이어 사망 체크
        if (player.hp <= 0) {
            System.out.println("GAME OVER");
            // TODO: 게임 오버 로직 구현
        }
    }

    //게터 함수

    public int getScore() {
        return score;
    }

    public Array<CoinObject> getObjects() {
        return objects;
    }

    public Array<Block> getBlocks() {return blocks;}

    // GameScreen으로부터 '점프' 입력을 받음
    public void onPlayerJump() {
        player.jump();
    }

    public void onPlayerLeft() {
        player.moveLeft();
    }

    public void onPlayerRight() {
        player.moveRight();
    }

    public void onPlayerAttack(){
        player.attack();
        attackObjects_spawning();
    }

    // GameScreen이 그릴 수 있도록 객체를 제공
    public GameCharacter getPlayer() {
        return player;
    }

    public boolean getCanAttack() {
        return player.canAttack();
    }

    public Array<Monster> getMonsters() {
        return monsters;
    }
    public Array<Item> getItems() {return items;}

    public Array<AttackObject> getAttackObjects() {return attackObjects;}
    public void removeMonster(Monster monster) {
        if (monsters.contains(monster, true)) {
            monsters.removeValue(monster, true);
            System.out.println("몬스터가 제거되었습니다!");
        }
    }

    public void removeItem(Item item) {
        if (items.contains(item, true)) {
            items.removeValue(item, true);
            System.out.println("아이템이 제거되었습니다!");
        }
    }

    public float getWorldWidth() {
        return worldWidth; // 이미 정의되어있다면 그대로, 없다면 상수/캠뷰 기준으로 반환
    }
    public float getWorldHeight() {
        return worldheight; // 마찬가지로
    }

}
