package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlimeShadowTest {

    private static final double SIZE = 20.0;
    private static final double MAX = 50.0;

    @Test
    void theShadowShrinksAndFadesAsTheSlimeRises() {
        SlimeShadow.Geometry low = SlimeShadow.at(SIZE, 10, MAX);
        SlimeShadow.Geometry high = SlimeShadow.at(SIZE, 40, MAX);
        assertTrue(high.width() < low.width());
        assertTrue(high.opacity() < low.opacity());
    }

    @Test
    void aRestingSlimeGetsTheFullShadow() {
        SlimeShadow.Geometry ground = SlimeShadow.at(SIZE, 0, MAX);
        assertEquals(SIZE * SlimeShadow.WIDTH_FACTOR, ground.width());
        assertEquals(1.0, ground.opacity());
    }

    @Test
    void heightIsClampedToTheGroundAndToTheTopOfTheJump() {
        assertEquals(SlimeShadow.at(SIZE, 0, MAX), SlimeShadow.at(SIZE, -5, MAX));
        assertEquals(SlimeShadow.at(SIZE, MAX, MAX), SlimeShadow.at(SIZE, MAX * 3, MAX));
        SlimeShadow.Geometry top = SlimeShadow.at(SIZE, MAX, MAX);
        assertEquals(SIZE * SlimeShadow.WIDTH_FACTOR * (1.0 - SlimeShadow.MAX_SHRINK), top.width(), 1e-9);
        assertEquals(1.0 - SlimeShadow.MAX_FADE, top.opacity(), 1e-9);
    }

    @Test
    void aZeroMaximumHeightKeepsTheGroundShadow() {
        assertEquals(SlimeShadow.at(SIZE, 0, MAX), SlimeShadow.at(SIZE, 10, 0));
    }
}
