package io.jbnu.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Array;

public class RhythmChartLoader {
    public static RhythmChart loadChart(String path) {
        Json json = new Json();
        RhythmChart chart = new RhythmChart();

        // 예시 JSON 구조:
        // [
        //   {"time": 0.5, "lane": 2},
        //   {"time": 1.2, "lane": 4},
        //   {"time": 1.8, "lane": 1}
        // ]
        try {
            Array<RhythmChart.NoteData> data = json.fromJson(Array.class, RhythmChart.NoteData.class, Gdx.files.internal(path));
            for (RhythmChart.NoteData d : data) {
                chart.addNote(d.time, d.lane);
            }
        } catch (Exception e) {
            System.err.println("차트 로딩 실패: " + e.getMessage());
        }
        return chart;
    }
}
