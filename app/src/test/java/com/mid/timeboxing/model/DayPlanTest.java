package com.mid.timeboxing.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DayPlanTest {

    private static TimeBox box(String id, int start, int duration) {
        return new TimeBox(id, id, start, duration, false);
    }

    @Test
    public void boxesAreSortedByStart() {
        DayPlan plan = new DayPlan(Arrays.asList(box("b", 600, 30), box("a", 540, 60)));
        assertEquals("a", plan.boxes().get(0).id);
        assertEquals("b", plan.boxes().get(1).id);
    }

    @Test
    public void boxAtUsesHalfOpenRange() {
        TimeBox a = box("a", 540, 30);
        DayPlan plan = new DayPlan(Collections.singletonList(a));
        assertNull(plan.boxAt(539));
        assertSame(a, plan.boxAt(540));
        assertSame(a, plan.boxAt(569));
        assertNull(plan.boxAt(570));
    }

    @Test
    public void conflictDetectsOverlapButAllowsTouching() {
        DayPlan plan = new DayPlan(Collections.singletonList(box("a", 540, 30)));
        assertNull(plan.conflict(570, 15, "new"));
        assertNull(plan.conflict(510, 30, "new"));
        assertEquals("a", plan.conflict(565, 10, "new").id);
        assertEquals("a", plan.conflict(500, 120, "new").id);
    }

    @Test
    public void conflictIgnoresTheBoxBeingEdited() {
        DayPlan plan = new DayPlan(Collections.singletonList(box("a", 540, 30)));
        assertNull(plan.conflict(545, 60, "a"));
    }

    @Test
    public void firstFreeSlotSkipsBoxes() {
        DayPlan plan = new DayPlan(Arrays.asList(box("a", 540, 30), box("b", 570, 10)));
        assertEquals(580, plan.firstFreeSlot(540));
        assertEquals(585, plan.firstFreeSlot(583));
        assertEquals(500, plan.firstFreeSlot(500));
    }

    @Test
    public void firstFreeSlotReturnsMinusOneWhenDayIsFull() {
        DayPlan plan = new DayPlan(Collections.singletonList(box("all", 0, TimeMath.MINUTES_PER_DAY)));
        assertEquals(-1, plan.firstFreeSlot(0));
    }

    @Test
    public void upsertReplacesAndResorts() {
        DayPlan plan = new DayPlan(Arrays.asList(box("a", 540, 30), box("b", 600, 30)));
        plan.upsert(box("a", 700, 15));
        assertEquals(2, plan.boxes().size());
        assertEquals("b", plan.boxes().get(0).id);
        assertEquals(700, plan.find("a").start);
    }

    @Test
    public void removeDropsBox() {
        DayPlan plan = new DayPlan(Collections.singletonList(box("a", 540, 30)));
        assertTrue(plan.remove("a"));
        assertFalse(plan.remove("a"));
        assertTrue(plan.isEmpty());
    }

    @Test
    public void statsAreWeightedByDuration() {
        DayPlan plan = new DayPlan(Arrays.asList(
                new TimeBox("a", "a", 540, 90, true),
                new TimeBox("b", "b", 630, 30, false)));
        assertEquals(120, plan.boxedMinutes());
        assertEquals(90, plan.doneMinutes());
        assertEquals(75, plan.completionPercent());
        assertEquals(1, plan.openCount());
    }

    @Test
    public void completionIsZeroForEmptyDay() {
        assertEquals(0, new DayPlan(Collections.emptyList()).completionPercent());
    }

    @Test
    public void freeMinutesRunUntilNextBoxOrMidnight() {
        DayPlan plan = new DayPlan(Collections.singletonList(box("a", 600, 30)));
        assertEquals(60, plan.freeMinutesFrom(540));
        assertEquals(TimeMath.MINUTES_PER_DAY - 700, plan.freeMinutesFrom(700));
    }

    @Test
    public void nextAfterIsStrict() {
        DayPlan plan = new DayPlan(Arrays.asList(box("a", 540, 30), box("b", 600, 30)));
        assertEquals("b", plan.nextAfter(540).id);
        assertEquals("a", plan.nextAfter(539).id);
        assertNull(plan.nextAfter(600));
    }
}
