package io.jbnu.test;

public interface GameWorldListener {
    void onMonsterCollision();    // 몬스터 사망시 호출
    void onStageClear();
}
