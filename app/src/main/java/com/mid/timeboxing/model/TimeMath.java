package com.mid.timeboxing.model;

import java.util.Locale;

/** Minute-of-day arithmetic. The day is divided into 288 five-minute slots. */
public final class TimeMath {

    public static final int SLOT_MINUTES = 5;
    public static final int MINUTES_PER_DAY = 24 * 60;

    private TimeMath() {
    }

    /** Rounds down to the slot containing {@code minute}, clamped to the day. */
    public static int snapDown(int minute) {
        int clamped = Math.max(0, Math.min(MINUTES_PER_DAY - SLOT_MINUTES, minute));
        return clamped - clamped % SLOT_MINUTES;
    }

    /** Rounds up to the next slot boundary, clamped to the day. */
    public static int snapUp(int minute) {
        int clamped = Math.max(0, Math.min(MINUTES_PER_DAY, minute));
        return (clamped + SLOT_MINUTES - 1) / SLOT_MINUTES * SLOT_MINUTES;
    }

    /** "HH:mm"; the end of the day renders as "24:00". */
    public static String clock(int minuteOfDay) {
        return String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60);
    }

    /** "mm:ss" below an hour, "h:mm:ss" above. Negative values render as zero. */
    public static String countdown(long seconds) {
        long s = Math.max(0, seconds);
        long hours = s / 3600;
        long minutes = (s % 3600) / 60;
        long secs = s % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, secs);
    }

    /** Compact duration: "45m", "2h", "1h 30m". */
    public static String duration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours == 0) {
            return rest + "m";
        }
        return rest == 0 ? hours + "h" : hours + "h " + rest + "m";
    }

    /** Minutes as hours with one decimal, e.g. 390 → "6.5". */
    public static String hours(int minutes) {
        return String.format(Locale.US, "%.1f", minutes / 60f);
    }
}
