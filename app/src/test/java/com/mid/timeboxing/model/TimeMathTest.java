package com.mid.timeboxing.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeMathTest {

    @Test
    public void snapDownRoundsToSlotAndClamps() {
        assertEquals(540, TimeMath.snapDown(544));
        assertEquals(545, TimeMath.snapDown(545));
        assertEquals(0, TimeMath.snapDown(-10));
        assertEquals(1435, TimeMath.snapDown(1440));
    }

    @Test
    public void snapUpRoundsToNextSlotAndClamps() {
        assertEquals(545, TimeMath.snapUp(541));
        assertEquals(545, TimeMath.snapUp(545));
        assertEquals(1440, TimeMath.snapUp(2000));
    }

    @Test
    public void clockFormatsMinuteOfDay() {
        assertEquals("00:00", TimeMath.clock(0));
        assertEquals("09:05", TimeMath.clock(545));
        assertEquals("24:00", TimeMath.clock(1440));
    }

    @Test
    public void countdownSwitchesToHoursAboveOneHour() {
        assertEquals("00:00", TimeMath.countdown(-5));
        assertEquals("04:07", TimeMath.countdown(247));
        assertEquals("1:00:00", TimeMath.countdown(3600));
    }

    @Test
    public void durationIsCompact() {
        assertEquals("45m", TimeMath.duration(45));
        assertEquals("2h", TimeMath.duration(120));
        assertEquals("1h 30m", TimeMath.duration(90));
    }

    @Test
    public void hoursHaveOneDecimal() {
        assertEquals("6.5", TimeMath.hours(390));
        assertEquals("0.0", TimeMath.hours(0));
    }
}
