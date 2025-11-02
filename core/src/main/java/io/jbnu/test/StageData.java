// StageData.java
package io.jbnu.test;

import com.badlogic.gdx.utils.Array;

public class StageData {
    public String name;
    public String bgm;            // 예: "audio/stage1.ogg"
    public String background;     // 예: "textures/bg_stage1.png"
    public float gravity = -9.8f * 200f;

    public float playerSpawnX = 64;
    public float playerSpawnY = 128;

    public float flagX = 2000;
    public float flagY = 64;

    public Array<BlockDef>   blocks   = new Array<>();
    public Array<ItemDef>    items    = new Array<>();
    public Array<MonsterDef> monsters = new Array<>();
    public Array<BossMonsterDef> BossMonsters = new Array<>();
    public Array<SpeedupDef> speedups = new Array<>();
    public Array<HealthDef> healthitems = new Array<>();

    public static class HealthDef { public float x; public float y; public String type;}
    public static class BlockDef { public float x; public float y; }
    public static class ItemDef  { public float x; public float y; public String type; }
    public static class MonsterDef { public float x; public float y; public String type; }
    public static class BossMonsterDef { public float x; public float y; public String type; }
    public static class SpeedupDef {public float x; public float y; public String type;}

}
