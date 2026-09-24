package com.mid.timeboxing.model;

/** One committed block of time: a single task, a fixed start, a fixed length. */
public final class TimeBox {

    public final String id;
    public final String title;
    /** Minute of day, always on a 5-minute boundary. */
    public final int start;
    /** Length in minutes, always a positive multiple of 5. */
    public final int duration;
    public final boolean done;

    public TimeBox(String id, String title, int start, int duration, boolean done) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.duration = duration;
        this.done = done;
    }

    public int end() {
        return start + duration;
    }

    public boolean contains(int minute) {
        return minute >= start && minute < end();
    }

    public boolean overlaps(int otherStart, int otherEnd) {
        return otherStart < end() && start < otherEnd;
    }

    public TimeBox withDone(boolean isDone) {
        return new TimeBox(id, title, start, duration, isDone);
    }
}
