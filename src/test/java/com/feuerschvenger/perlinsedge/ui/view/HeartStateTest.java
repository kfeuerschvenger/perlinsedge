package com.feuerschvenger.perlinsedge.ui.view;

import org.junit.jupiter.api.Test;

import static com.feuerschvenger.perlinsedge.ui.view.HeartState.EMPTY;
import static com.feuerschvenger.perlinsedge.ui.view.HeartState.FULL;
import static com.feuerschvenger.perlinsedge.ui.view.HeartState.HALF;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HeartStateTest {

    @Test
    void fullHealthFillsTheRow() {
        assertArrayEquals(new HeartState[]{FULL, FULL, FULL, FULL, FULL}, HeartState.row(1.0, 5));
    }

    @Test
    void halfHealthLeavesTheLastHeartsEmpty() {
        assertArrayEquals(new HeartState[]{FULL, FULL, HALF, EMPTY, EMPTY}, HeartState.row(0.5, 5));
    }

    @Test
    void noHealthEmptiesTheRow() {
        assertArrayEquals(new HeartState[]{EMPTY, EMPTY, EMPTY}, HeartState.row(0.0, 3));
        assertArrayEquals(new HeartState[]{EMPTY, EMPTY, EMPTY}, HeartState.row(-0.5, 3));
    }

    @Test
    void anyHealthShowsAtLeastAHalfHeart() {
        assertArrayEquals(new HeartState[]{HALF, EMPTY, EMPTY, EMPTY, EMPTY}, HeartState.row(0.01, 5));
    }

    @Test
    void healthAboveTheMaximumIsClamped() {
        assertArrayEquals(new HeartState[]{FULL, FULL}, HeartState.row(3.0, 2));
    }
}
