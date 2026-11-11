package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlimeShadowTest {

    private static final double SIZE = 20.0;
    private static final double MAX = 50.0;

    @Test
    void theShadowShrinksAndFadesAsTheSlimeRises() {
        assertTrue(SlimeShadow.width(SIZE, 40, MAX) < SlimeShadow.width(SIZE, 10, MAX));
        assertTrue(SlimeShadow.opacity(40, MAX) < SlimeShadow.opacity(10, MAX));
    }

    @Test
    void aRestingSlimeGetsTheFullShadow() {
        assertEquals(SIZE * SlimeShadow.WIDTH_FACTOR, SlimeShadow.width(SIZE, 0, MAX));
        assertEquals(1.0, SlimeShadow.opacity(0, MAX));
    }

    @Test
    void heightIsClampedToTheGroundAndToTheTopOfTheJump() {
        assertEquals(SlimeShadow.width(SIZE, 0, MAX), SlimeShadow.width(SIZE, -5, MAX));
        assertEquals(SlimeShadow.opacity(0, MAX), SlimeShadow.opacity(-5, MAX));
        assertEquals(SlimeShadow.width(SIZE, MAX, MAX), SlimeShadow.width(SIZE, MAX * 3, MAX));
        assertEquals(SlimeShadow.opacity(MAX, MAX), SlimeShadow.opacity(MAX * 3, MAX));
        assertEquals(SIZE * SlimeShadow.WIDTH_FACTOR * (1.0 - SlimeShadow.MAX_SHRINK), SlimeShadow.width(SIZE, MAX, MAX), 1e-9);
        assertEquals(1.0 - SlimeShadow.MAX_FADE, SlimeShadow.opacity(MAX, MAX), 1e-9);
    }

    @Test
    void aZeroMaximumHeightKeepsTheGroundShadow() {
        assertEquals(SlimeShadow.width(SIZE, 0, MAX), SlimeShadow.width(SIZE, 10, 0));
        assertEquals(SlimeShadow.opacity(0, MAX), SlimeShadow.opacity(10, 0));
    }
}
