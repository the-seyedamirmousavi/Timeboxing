package com.mid.timeboxing.model;

import java.util.Calendar;

/** Identifies a calendar day as a yyyymmdd integer, e.g. 20260924. */
public final class DayKey {

    private DayKey() {
    }

    public static int today() {
        return of(Calendar.getInstance());
    }

    public static int of(Calendar calendar) {
        return calendar.get(Calendar.YEAR) * 10000
                + (calendar.get(Calendar.MONTH) + 1) * 100
                + calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static Calendar toCalendar(int key) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(key / 10000, (key / 100) % 100 - 1, key % 100);
        return calendar;
    }

    public static int shift(int key, int days) {
        Calendar calendar = toCalendar(key);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return of(calendar);
    }
}
