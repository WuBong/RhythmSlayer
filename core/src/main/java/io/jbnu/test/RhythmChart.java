package io.jbnu.test;

import com.badlogic.gdx.utils.Array;

public class RhythmChart {
    public static class NoteData {
        public float time; // 노트 생성 타이밍 (초)
        public int lane;   // 레인 번호 (0~4)

        public NoteData(float time, int lane) {
            this.time = time;
            this.lane = lane;
        }
    }

    private final Array<NoteData> notes = new Array<>();

    public void addNote(float time, int lane) {
        notes.add(new NoteData(time, lane));
    }

    public Array<NoteData> getNotes() {
        return notes;
    }
}
