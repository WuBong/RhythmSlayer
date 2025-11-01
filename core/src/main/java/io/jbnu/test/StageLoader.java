// StageLoader.java
package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

public class StageLoader {
    private static final Json json = new Json();

    public static StageData load(String path) {
        String raw = Gdx.files.internal(path).readString("UTF-8");
        return json.fromJson(StageData.class, raw);
    }
}
