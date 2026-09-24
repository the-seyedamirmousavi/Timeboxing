package com.mid.timeboxing.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

public class DayKeyTest {

    @Test
    public void roundTripsThroughCalendar() {
        Calendar calendar = DayKey.toCalendar(20260924);
        assertEquals(2026, calendar.get(Calendar.YEAR));
        assertEquals(Calendar.SEPTEMBER, calendar.get(Calendar.MONTH));
        assertEquals(24, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(20260924, DayKey.of(calendar));
    }

    @Test
    public void shiftCrossesMonthAndYearBoundaries() {
        assertEquals(20261001, DayKey.shift(20260930, 1));
        assertEquals(20270101, DayKey.shift(20261231, 1));
        assertEquals(20260228, DayKey.shift(20260301, -1));
        assertEquals(20280229, DayKey.shift(20280301, -1));
    }
}
