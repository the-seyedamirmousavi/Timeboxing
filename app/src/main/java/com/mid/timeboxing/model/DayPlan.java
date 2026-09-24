package com.mid.timeboxing.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All boxes of one day, kept sorted by start time. Boxes never overlap. */
public final class DayPlan {

    private final List<TimeBox> boxes = new ArrayList<>();

    public DayPlan(List<TimeBox> initial) {
        boxes.addAll(initial);
        sort();
    }

    public List<TimeBox> boxes() {
        return Collections.unmodifiableList(boxes);
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public TimeBox find(String id) {
        for (TimeBox box : boxes) {
            if (box.id.equals(id)) {
                return box;
            }
        }
        return null;
    }

    public TimeBox boxAt(int minute) {
        for (TimeBox box : boxes) {
            if (box.contains(minute)) {
                return box;
            }
        }
        return null;
    }

    /** First box that starts strictly after {@code minute}. */
    public TimeBox nextAfter(int minute) {
        for (TimeBox box : boxes) {
            if (box.start > minute) {
                return box;
            }
        }
        return null;
    }

    /** Returns the box that would collide with the proposed range, ignoring {@code ignoreId}. */
    public TimeBox conflict(int start, int duration, String ignoreId) {
        int end = start + duration;
        for (TimeBox box : boxes) {
            if (!box.id.equals(ignoreId) && box.overlaps(start, end)) {
                return box;
            }
        }
        return null;
    }

    /** First free 5-minute slot at or after {@code fromMinute}, or -1 if the day is full. */
    public int firstFreeSlot(int fromMinute) {
        for (int m = TimeMath.snapUp(fromMinute); m + TimeMath.SLOT_MINUTES <= TimeMath.MINUTES_PER_DAY;
                m += TimeMath.SLOT_MINUTES) {
            if (boxAt(m) == null) {
                return m;
            }
        }
        return -1;
    }

    /** Minutes from {@code minute} until the next box starts, or until midnight. */
    public int freeMinutesFrom(int minute) {
        TimeBox next = nextAfter(minute);
        return (next != null ? next.start : TimeMath.MINUTES_PER_DAY) - minute;
    }

    public void upsert(TimeBox box) {
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).id.equals(box.id)) {
                boxes.set(i, box);
                sort();
                return;
            }
        }
        boxes.add(box);
        sort();
    }

    public boolean remove(String id) {
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).id.equals(id)) {
                boxes.remove(i);
                return true;
            }
        }
        return false;
    }

    public int firstStart() {
        return boxes.isEmpty() ? -1 : boxes.get(0).start;
    }

    public int lastEnd() {
        return boxes.isEmpty() ? -1 : boxes.get(boxes.size() - 1).end();
    }

    public int boxedMinutes() {
        int total = 0;
        for (TimeBox box : boxes) {
            total += box.duration;
        }
        return total;
    }

    public int doneMinutes() {
        int total = 0;
        for (TimeBox box : boxes) {
            if (box.done) {
                total += box.duration;
            }
        }
        return total;
    }

    /** Share of boxed time that is done, weighted by duration, 0–100. */
    public int completionPercent() {
        int boxed = boxedMinutes();
        return boxed == 0 ? 0 : Math.round(doneMinutes() * 100f / boxed);
    }

    public int openCount() {
        int open = 0;
        for (TimeBox box : boxes) {
            if (!box.done) {
                open++;
            }
        }
        return open;
    }

    private void sort() {
        Collections.sort(boxes, (a, b) -> Integer.compare(a.start, b.start));
    }
}
