package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeartStateTest {

    @Test
    void highHealthShowsAFullHeart() {
        assertEquals(HeartState.FULL, HeartState.of(1.0));
        assertEquals(HeartState.FULL, HeartState.of(0.7));
    }

    @Test
    void injuredHealthShowsAHalfHeart() {
        assertEquals(HeartState.HALF, HeartState.of(0.66));
        assertEquals(HeartState.HALF, HeartState.of(0.01));
    }

    @Test
    void noHealthShowsAnEmptyHeart() {
        assertEquals(HeartState.EMPTY, HeartState.of(0.0));
        assertEquals(HeartState.EMPTY, HeartState.of(-0.5));
    }
}
