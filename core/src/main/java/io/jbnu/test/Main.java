//main.java
package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter implements GameWorldListener, RhythmGameListener {

    private SpriteBatch batch;
    Sound effectSound;
    private RhythmGameManager rythmManager;

    GameWorld world;
    private Texture objectTexture; // 떨어지는 오브젝트 텍스처
    private Texture playerTexture;
    private Texture blockTexture;
    private Texture pauseTexture;
    private Texture monsterTexture;
    private Texture ItemTexture;
    private Texture attackObjectTexture;
    private Texture flagTexture;
    private Texture BossMonsterTexture;
    private Texture speedupTexture;

    //상태창 폰트
    private BitmapFont hpFont;
    private BitmapFont scoreFont;
    private BitmapFont RestartGameFont;

    //월드크기 선언부
    private final float WORLD_WIDTH = 1280;
    private final float WORLD_HEIGHT = 720;

    public enum GameState{
        RUNNING,
        PAUSED,
        RHYTHM_MODE,
        RESTART
    }
    private GameState currentState;
    private OrthographicCamera camera;
    private Viewport viewport;

    private final String[] stages = {
        "stages/stage1.json",
        "stages/stage2.json",
        "stages/stage3.json"
    };
    private int stageIndex = 0;
    private boolean switching = false; // 중복 전환 방지

    @Override
    public void create() {
        currentState = GameState.RUNNING;
        rythmManager = new RhythmGameManager(this);

        batch = new SpriteBatch();

        effectSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        //텍스처 선언부
        playerTexture = new Texture("player_sheet.png");
        objectTexture = new Texture("HP.jpg");
        pauseTexture = new Texture("pause.png");
        blockTexture = new Texture("jbnu.jpg");
        monsterTexture = new Texture("dragon.png");
        ItemTexture = new Texture("ts808.jpg");
        attackObjectTexture = new Texture("attack_object.png");
        flagTexture = new Texture("flag.png");
        BossMonsterTexture = new Texture("yamada.jpg");
        speedupTexture = new Texture("speed_up.png");

        world = new GameWorld(playerTexture,objectTexture,blockTexture,monsterTexture,
            ItemTexture, attackObjectTexture, flagTexture, BossMonsterTexture, speedupTexture, this.WORLD_WIDTH, this.WORLD_HEIGHT, this);
        //상태창 선언부

        loadStage(stageIndex);

        hpFont = new BitmapFont();
        hpFont.getData().setScale(1);
        scoreFont = new BitmapFont();
        scoreFont.getData().setScale(3);
        RestartGameFont = new BitmapFont();
        RestartGameFont.getData().setScale(3);

        //카메라 선언부
        camera = new OrthographicCamera();
        viewport = new FillViewport(800,600, camera);
        camera.setToOrtho(false, 800, 600);
    }
    @Override
    public void onStageClear() {
        if (switching) return; // 중복 호출 방지
        switching = true;
        stageIndex = (stageIndex + 1);
        if(stageIndex == 3){
            stageIndex = 0;
            currentState = GameState.RESTART;
        }
        loadStage(stageIndex);
    }
    private void loadStage(int idx) {
        StageData data = StageLoader.load(stages[idx]);

        // 배경/음악 교체가 필요하면 여기서:
        // setBackground(data.background);
        // playBgm(data.bgm);
        world.getPlayer().hp = 10;
        world.resetWith(data);
        switching = false; // 전환 종료
    }
    @Override
    public void onMonsterCollision() {
        System.out.println("몬스터와 접촉 리듬게임 모드로 변환!");
        enterRhythmMode();
    }
    @Override
    public void onRhythmGameEnd(boolean isSuccess){
        System.out.println("리듬모드 종료, 다시 월드로 복귀합니다. 결과: "+ (isSuccess ? "승리" : "패배"));
        world.onRhythmBattleEnd(isSuccess);
        enterGameMode();
    }

    @Override
    public void render() {
        ScreenUtils.clear(1f, 1f, 1f, 1f);
        input();
        logic();
        draw();
    }

    private void logic(){
        if(currentState==GameState.RUNNING) {
            world.update(Gdx.graphics.getDeltaTime());
            //게임모드 일때만 메인 카메라가 이동해야함
            Vector2 playerposition = world.getPlayer().position;
            float playerx = playerposition.x;
            camera.position.set(playerx, 250, 0);
            camera.update();
            if(world.getPlayer().hp <= 0){
                world.getPlayer().hp = 10;
                currentState = GameState.RESTART;
            }
        }   //리듬게임 설계시
        else if (currentState == GameState.RHYTHM_MODE) {
            rythmManager.update(Gdx.graphics.getDeltaTime());
            //리듬모드일 때 카메라 고정
            camera.position.set(400, 300, 0);
            camera.update();
        }
        else if(currentState == GameState.RESTART){
            camera.position.set(400, 300, 0);
            camera.update();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    private void draw(){
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        if(currentState == GameState.RUNNING) {
            world.getPlayer().draw(batch);
            for (CoinObject obj : world.getObjects()) {
                obj.draw(batch);
            }
            for (Block block : world.getBlocks()) {
                block.draw(batch);
            }
            for (Monster monster : world.getMonsters()) {
                monster.draw(batch);
            }
            for (Item item : world.getItems()) {
                item.draw(batch);
            }
            for (AttackObject atk : world.getAttackObjects()) {
                atk.draw(batch);
            }
            for(Flag flag : world.getFlag()){
                flag.draw(batch);
            }
            for(BossMonster bossMonster : world.getBossMonsters()){
                bossMonster.draw(batch);
            }
            for(Speedup speedup: world.getSpeedups()){
                speedup.draw(batch);
            }
            //폰트 화면에 그리기
            hpFont.draw(batch, "HP: " + world.getPlayer().hp, world.getPlayer().position.x + world.getPlayer().CharaterSize_width / 2, world.getPlayer().position.y + world.getPlayer().bounds.height + 20); //플레이어 위에 hp
            scoreFont.draw(batch, "(1000 is next stage)Score : "+world.getScore(), world.getPlayer().position.x-400, 400);
        }

        else if(currentState == GameState.RHYTHM_MODE){
            batch.setProjectionMatrix(camera.combined);
            rythmManager.render(batch, camera);
        }
        else if(currentState == GameState.RESTART){
            RestartGameFont.draw(batch, "ReStart game? press [R]", 0, 300);
        }

        batch.end();
    }

    private void input() {

        if(Gdx.input.isKeyJustPressed(Keys.ESCAPE)){
            if(currentState == GameState.RUNNING){
                currentState = GameState.PAUSED;
            }
            else if(currentState == GameState.PAUSED) {
                currentState = GameState.RUNNING;
            }
        }

        if(Gdx.input.isKeyJustPressed(Keys.R)) {
            if(currentState == GameState.RESTART) {
                currentState = GameState.RUNNING;
            }
        }

        //플레이어 이동 인식 부
            if(currentState == GameState.RUNNING) {
                if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
                    world.onPlayerRight();
                } else if (Gdx.input.isKeyPressed(Keys.LEFT)) {
                    world.onPlayerLeft();
                }
                if (Gdx.input.isKeyPressed(Keys.SPACE)) {
                    world.onPlayerJump();
                    System.out.println("점프");
                }
                if(Gdx.input.isKeyPressed(Keys.A)){
                    world.onPlayerAttack();
                }
            }

    }


    public void enterRhythmMode() {
        currentState = GameState.RHYTHM_MODE;
        camera.position.set(400, 300, 0);  // 진입 즉시 카메라 중앙으로 고정
        camera.update();
        rythmManager.enterRythmMode(world.getPlayer().drive);
    }

    public void enterGameMode(){
        currentState = GameState.RUNNING;
    }

    @Override
    public void dispose() {
        playerTexture.dispose();
        monsterTexture.dispose();
        objectTexture.dispose();
        attackObjectTexture.dispose();
        hpFont.dispose();
        batch.dispose();
    }
}
