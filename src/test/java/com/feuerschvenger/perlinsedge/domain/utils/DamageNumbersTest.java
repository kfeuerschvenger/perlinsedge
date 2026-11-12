package com.feuerschvenger.perlinsedge.domain.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageNumbersTest {

    private static DamageNumbers.Entry firstActive(DamageNumbers numbers, double now) {
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i).isActive(now)) {
                return numbers.get(i);
            }
        }
        throw new AssertionError("no number is active");
    }

    @Test
    void aNumberLivesEightTenthsOfASecondOnTheGameClock() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));

        numbers.spawn(DamageNumbers.Kind.ENEMY, 10, 3, 4, 5.0);

        assertEquals(1, numbers.countActive(5.0));
        assertEquals(1, numbers.countActive(5.79));
        assertEquals(0, numbers.countActive(5.81));
        assertEquals(0, numbers.countActive(4.9), "not shown before it starts");
    }

    @Test
    void aNumberRisesTwelveArtPixelsOverItsLife() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));
        numbers.spawn(DamageNumbers.Kind.ENEMY, 10, 3, 4, 2.0);
        DamageNumbers.Entry entry = firstActive(numbers, 2.0);

        assertEquals(0.0, entry.rise(2.0), 1e-9);
        assertEquals(6.0, entry.rise(2.4), 1e-9);
        assertEquals(12.0, entry.rise(2.8), 1e-9);
        assertEquals(12.0, entry.rise(9.0), 1e-9, "it never rises past the top");
    }

    @Test
    void aNumberStaysOpaqueAtFirstThenFadesToNothing() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));
        numbers.spawn(DamageNumbers.Kind.PLAYER, 10, 0, 0, 0.0);
        DamageNumbers.Entry entry = firstActive(numbers, 0.0);

        assertEquals(1.0, entry.opacity(0.0), 1e-9);
        assertEquals(1.0, entry.opacity(0.3), 1e-9);
        assertTrue(entry.opacity(0.5) < 1.0 && entry.opacity(0.5) > 0.0);
        assertTrue(entry.opacity(0.7) < entry.opacity(0.5));
        assertEquals(0.0, entry.opacity(0.8), 1e-9);
    }

    @Test
    void aPausedGameClockFreezesTheNumber() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));
        numbers.spawn(DamageNumbers.Kind.ENEMY, 10, 0, 0, 1.0);
        DamageNumbers.Entry entry = firstActive(numbers, 1.0);

        double riseBefore = entry.rise(1.3);
        double riseAfterAPause = entry.rise(1.3);

        assertEquals(riseBefore, riseAfterAPause);
        assertTrue(entry.isActive(1.3));
    }

    @Test
    void aHealIsShownWithAPlusSignAndAnythingElseWithoutOne() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));
        numbers.spawn(DamageNumbers.Kind.HEAL, 25, 0, 0, 0);
        numbers.spawn(DamageNumbers.Kind.ENEMY, 12, 0, 0, 0);

        Set<String> texts = new HashSet<>();
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i).isActive(0)) {
                texts.add(numbers.get(i).getText());
            }
        }

        assertEquals(Set.of("+25", "12"), texts);
    }

    @Test
    void anAmountThatRoundsToNothingIsNotShown() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));

        numbers.spawn(DamageNumbers.Kind.ENEMY, 0, 0, 0, 0);
        numbers.spawn(DamageNumbers.Kind.ENEMY, 0.4, 0, 0, 0);
        numbers.spawn(DamageNumbers.Kind.ENEMY, -3, 0, 0, 0);

        assertEquals(0, numbers.countActive(0));
    }

    @Test
    void theShiftToTheSideStaysSmallAndDiffersBetweenNumbers() {
        DamageNumbers numbers = new DamageNumbers(new Random(7));
        Set<Double> offsets = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            numbers.spawn(DamageNumbers.Kind.ENEMY, 5, 0, 0, 0);
        }
        for (int i = 0; i < numbers.size(); i++) {
            DamageNumbers.Entry entry = numbers.get(i);
            if (entry.isActive(0)) {
                assertTrue(Math.abs(entry.getOffsetX()) <= DamageNumbers.MAX_OFFSET_ART_PIXELS);
                offsets.add(entry.getOffsetX());
            }
        }

        assertTrue(offsets.size() > 5, "consecutive numbers do not share one offset");
    }

    @Test
    void thePoolNeverGrowsAndANewNumberTakesTheSlotOfTheOldestWhenItIsFull() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));

        for (int i = 0; i < DamageNumbers.CAPACITY * 3; i++) {
            numbers.spawn(DamageNumbers.Kind.ENEMY, 1 + i, 0, 0, i * 0.001);
        }

        assertEquals(DamageNumbers.CAPACITY, numbers.size());
        assertEquals(DamageNumbers.CAPACITY, numbers.countActive(0.2));
        boolean newestShown = false;
        boolean oldestShown = false;
        for (int i = 0; i < numbers.size(); i++) {
            String text = numbers.get(i).getText();
            newestShown |= text.equals(String.valueOf(DamageNumbers.CAPACITY * 3));
            oldestShown |= text.equals("1");
        }
        assertTrue(newestShown);
        assertFalse(oldestShown, "the oldest was replaced");
    }

    @Test
    void aSlotThatEndedIsReusedBeforeAnActiveOneIsReplaced() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));
        numbers.spawn(DamageNumbers.Kind.ENEMY, 1, 0, 0, 0.0);

        numbers.spawn(DamageNumbers.Kind.ENEMY, 2, 0, 0, 5.0);
        numbers.spawn(DamageNumbers.Kind.ENEMY, 3, 0, 0, 5.1);

        assertEquals(2, numbers.countActive(5.2));
    }

    @Test
    void clearRemovesEverything() {
        DamageNumbers numbers = new DamageNumbers(new Random(1));
        numbers.spawn(DamageNumbers.Kind.ENEMY, 5, 0, 0, 0);

        numbers.clear();

        assertEquals(0, numbers.countActive(0));
    }
}
