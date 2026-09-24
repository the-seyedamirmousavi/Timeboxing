package com.mid.timeboxing.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.mid.timeboxing.model.DayPlan;
import com.mid.timeboxing.model.TimeBox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Persists each day's boxes and Top 3 priorities in SharedPreferences. */
public final class TimeboxStore {

    public static final int PRIORITY_COUNT = 3;

    private static final String PREFS = "timeboxing";

    private final SharedPreferences prefs;

    public TimeboxStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public DayPlan load(int dayKey) {
        List<TimeBox> boxes = new ArrayList<>();
        String raw = prefs.getString(boxesKey(dayKey), null);
        if (raw != null) {
            try {
                JSONArray array = new JSONArray(raw);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    boxes.add(new TimeBox(o.getString("id"), o.optString("title"),
                            o.getInt("start"), o.getInt("duration"), o.optBoolean("done")));
                }
            } catch (JSONException e) {
                // A corrupt day starts empty rather than crashing the app.
                boxes.clear();
            }
        }
        return new DayPlan(boxes);
    }

    public void save(int dayKey, DayPlan plan) {
        if (plan.isEmpty()) {
            prefs.edit().remove(boxesKey(dayKey)).apply();
            return;
        }
        JSONArray array = new JSONArray();
        try {
            for (TimeBox box : plan.boxes()) {
                array.put(new JSONObject()
                        .put("id", box.id)
                        .put("title", box.title)
                        .put("start", box.start)
                        .put("duration", box.duration)
                        .put("done", box.done));
            }
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        prefs.edit().putString(boxesKey(dayKey), array.toString()).apply();
    }

    public String priority(int dayKey, int index) {
        return prefs.getString(priorityKey(dayKey, index), "");
    }

    public void setPriority(int dayKey, int index, String text) {
        prefs.edit().putString(priorityKey(dayKey, index), text).apply();
    }

    private static String boxesKey(int dayKey) {
        return "boxes_" + dayKey;
    }

    private static String priorityKey(int dayKey, int index) {
        return "top_" + dayKey + "_" + index;
    }
}
