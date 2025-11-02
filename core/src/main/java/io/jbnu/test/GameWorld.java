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
    private Array<Flag> flag;
    private Array<BossMonster> bossMonsters;
    private Array<Speedup> speedups;
    private BossMonster currentOpponent;// 현재 리듬게임 대상 중인 몬스터


    private Array<Monster> monsters;
    private int score;

    private Texture playerTexture;
    private Texture objectTexture;
    private Texture blockTexture;
    private Texture monsterTexture;
    private Texture itemTexture;
    private Texture attackObjectTexture;
    private Texture flagTexture;
    private Texture BossMonsterTexture;
    private Texture speedupTexture;

    private float worldWidth; // 랜덤 위치 생성을 위해 월드 너비 저장
    private float worldheight;

    //main과 간접 통신할 콜백 리스너
    private GameWorldListener listener;
    //리듬게임 매니저

    private int level = 0; //game의 레벨

    public GameWorld (Texture playerTexture, Texture objectTexture,
                     Texture blockTexture, Texture monsterTexture, Texture itemTexture, Texture attackObjectTexture, Texture flagTexture,
                     Texture BossMonsterTexture, Texture speedupTexture,
                     float worldWidth, float worldheight, GameWorldListener listener)  {


        //텍스쳐 불러오기
        this.playerTexture = playerTexture;
        this.objectTexture = objectTexture;
        this.blockTexture = blockTexture;
        this.monsterTexture = monsterTexture;
        this.itemTexture = itemTexture;
        this.attackObjectTexture = attackObjectTexture;
        this.flagTexture = flagTexture;
        this.BossMonsterTexture = BossMonsterTexture;
        this.speedupTexture = speedupTexture;

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
        flag = new Array<>();
        bossMonsters = new Array<>();
        speedups = new Array<>();
        score = 0;
    }

    public void update(float delta) { //물리업데이트
        // --- 1. 힘 적용 (중력, 저항) ---
        player.velocity.y += WORLD_GRAVITY * delta;

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
        if(!player.getDamagedState()) checkMonsterCollision();
        checkItemCollisions();
        checkAttackObjectCollisions();
        checkFlagCollisions();
        checkBossMonsterCollision();
        checkSpeedupCollisionsY();

        // --- 6. 그래픽 동기화 ---
        player.updateAnimation(delta);
    }

    public void resetWith(StageData data) {
        // 1) 중력/환경
        // WORLD_GRAVITY는 상수라 그대로 두고, 필요하면 별도 gravity 변수 써도 됨.
        // 여기선 그대로 두되, 필요하면 data.gravity를 참고해도 된다.

        // 2) 기존 오브젝트 정리
        objects.clear();
        blocks.clear();
        items.clear();
        attackObjects.clear();
        monsters.clear();
        flag.clear();
        bossMonsters.clear();
        speedups.clear();

        // 3) 플레이어 위치 초기화 + 상태 리셋
        if (player == null) {
            player = new GameCharacter(playerTexture, worldWidth /2, 300, 0,
                128, 128,    // frameW, frameH
                8, 14,       // idle
                0, 9,        // run
                9, 12,       // jump
                4, 6,        // damaged
                3, 4         // attack
            );
        }
        player.position.set(data.playerSpawnX, data.playerSpawnY);
        player.velocity.set(0, 0);
        player.isGrounded = false;
        // player.setDamagedState(false); // 네 구현에 맞춰 필요 시 호출
        // player.resetAnimation() 등이 있으면 같이 호출

        // 4) 블록/아이템/몬스터 재생성
        for (StageData.BlockDef b : data.blocks) {
            blocks.add(new Block(blockTexture, b.x, b.y));
        }
        for (StageData.ItemDef it : data.items) {
            items.add(new Item(itemTexture, it.x, it.y));
        }
        for (StageData.MonsterDef m : data.monsters) {
            monsters.add(new Monster(monsterTexture, m.x, m.y));
        }
        for (StageData.BossMonsterDef k : data.BossMonsters){
            bossMonsters.add(new BossMonster(BossMonsterTexture, k.x, k.y));
        }
        for (StageData.SpeedupDef s : data.speedups){
            speedups.add(new Speedup(speedupTexture, s.x, s.y));
        }

        // 5) 플래그 생성
        flag.add(new Flag(flagTexture, data.flagX, data.flagY));

        // 6) 점수/레벨 등은 네 선택
        // score 유지할지 초기화할지 결정. 일단 유지:
        // this.score = this.score;
    }


    private void attackObjects_spawning() {
        final int projW = 64;
        final int projH = 64;

        boolean facingRight = player.isFacingRight();
        float speed = facingRight ? 700f : - 700f;

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

                } else if (moveAmount < 0) {
                    player.velocity.x = 0;
                    player.position.x = block.bounds.x + block.bounds.width;

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

                    //System.out.println("천장에 충돌!");
                } else if (moveAmount < 0) {
                    // 아래로 이동 중 바닥 충돌
                    player.velocity.y = 0;
                    player.position.y = block.bounds.y + block.bounds.height;

                    player.isGrounded = true; // 바닥에 닿았다고 표시
                    //System.out.println("바닥에 충돌!");
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



    private void checkSpeedupCollisionsY() {

        Rectangle playerBounds = player.bounds;

        boolean collision = false;
        for (Speedup speedup : getSpeedups()) {

            if (playerBounds.overlaps(speedup.bounds)) {
                System.out.println(player.velocity.x);
                player.velocity.x += 20;
            }
        }

        if (!collision) {
            player.syncBoundsToPosition();
        }

    }

    private void checkItemCollisions(){
        player.syncBoundsToPosition();
        for(Item item : getItems()) {
            if (player.bounds.overlaps(item.bounds)){
                removeItem(item);
                player.drive += 1;
            }
        }
    }

    private void checkFlagCollisions(){
        player.syncBoundsToPosition();
        for(Flag flag : getFlag()) {
            if (player.bounds.overlaps(flag.bounds)){

                if(getScore() >= 1000) {
                    score = 0;
                    if (listener != null) {
                        listener.onStageClear();
                    }
                }
                return; // 중복 호출 방지
            }
        }
    }


    private void checkAttackObjectCollisions() {
        // 공격 오브젝트가 하나도 없으면 바로 종료
        if (attackObjects.size == 0 || monsters.size == 0) return;

        for (Iterator<AttackObject> atkIter = attackObjects.iterator(); atkIter.hasNext();) {
            AttackObject atk = atkIter.next();
            Rectangle atkBounds = new Rectangle(atk.position.x, atk.position.y, atk.CharaterSize_width, atk.CharaterSize_height);

            for (Iterator<Monster> monIter = monsters.iterator(); monIter.hasNext();) {
                Monster monster = monIter.next();

                if (atkBounds.overlaps(monster.bounds)) {
                    // 🔥 충돌 발생!
                    System.out.println("몬스터 피격!");
                    score += 200;
                    // 몬스터 제거
                    monIter.remove();
                    atkIter.remove();

                    break;
                }
            }
        }
    }

    private void checkCoinCollisions() {
        player.syncBoundsToPosition();
        // 플레이어와 떨어지는 오브젝트들의 충돌 검사
        for (Iterator<CoinObject> iter = objects.iterator(); iter.hasNext(); ) {
            CoinObject obj = iter.next();
            if (player.bounds.overlaps(obj.bounds)) {
                player.hp++;
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
                player.damagedPlayer(player.position.x >= monster.position.x);
            }
        }
        if(!collision) {
            collision = false;
        }
    }

    private void checkBossMonsterCollision() {
        boolean collision = false;
        for(BossMonster boss : getBossMonsters()) {
            if(player.bounds.overlaps(boss.bounds)){
                collision = true;
                System.out.println("몬스터와 충돌!");
                currentOpponent = boss;
                if(listener != null){
                    listener.onMonsterCollision();
                }
            }
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
            removeBossMonster(currentOpponent);
            score += 400;
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
    public Array<BossMonster> getBossMonsters() {return bossMonsters;}

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
        if(!player.canAttack()){
            return;
        }

        player.attack();    //공격 실행 및 애니메이션 쿨타임 시작
        // 투사체 생성
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
    public Array<Speedup> getSpeedups() {return speedups;}
    public Array<AttackObject> getAttackObjects() {return attackObjects;}
    public Array<Flag> getFlag(){return flag;}
    public void removeMonster(Monster monster) {
        if (monsters.contains(monster, true)) {
            monsters.removeValue(monster, true);
            System.out.println("몬스터가 제거되었습니다!");
        }
    }

    public void removeBossMonster(BossMonster boss){
        if (bossMonsters.contains(boss, true)) {
            bossMonsters.removeValue(boss, true);
            System.out.println("보스몬스터가 제거되었습니다!");
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
