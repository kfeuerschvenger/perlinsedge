package com.feuerschvenger.perlinsedge.infra.fx.graphics.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SlimeShadowTest {

    private static final double SIZE = 20.0;
    private static final double MAX = 50.0;

    @Test
    void theShadowCentreDoesNotFollowTheJumpHeight() {
        SlimeShadow.Geometry ground = SlimeShadow.at(SIZE, 0, MAX);
        SlimeShadow.Geometry air = SlimeShadow.at(SIZE, 37, MAX);
        assertEquals(ground.centreX(), air.centreX());
        assertEquals(ground.centreY(), air.centreY());
    }

    @Test
    void theShadowShrinksAndFadesAsTheSlimeRises() {
        SlimeShadow.Geometry low = SlimeShadow.at(SIZE, 10, MAX);
        SlimeShadow.Geometry high = SlimeShadow.at(SIZE, 40, MAX);
        assertTrue(high.width() < low.width());
        assertTrue(high.height() < low.height());
        assertTrue(high.opacity() < low.opacity());
    }

    @Test
    void aRestingSlimeGetsTheFullShadowBelowItsFeet() {
        SlimeShadow.Geometry ground = SlimeShadow.at(SIZE, 0, MAX);
        assertEquals(SIZE * SlimeShadow.WIDTH_FACTOR, ground.width());
        assertEquals(SlimeShadow.BASE_OPACITY, ground.opacity());
        assertTrue(ground.centreY() > 0);
        assertTrue(ground.centreY() < ground.height() / 2, "the feet stay over the upper half of the ellipse");
    }

    @Test
    void heightIsClampedToTheGroundAndToTheTopOfTheJump() {
        assertEquals(SlimeShadow.at(SIZE, 0, MAX), SlimeShadow.at(SIZE, -5, MAX));
        assertEquals(SlimeShadow.at(SIZE, MAX, MAX), SlimeShadow.at(SIZE, MAX * 3, MAX));
        SlimeShadow.Geometry top = SlimeShadow.at(SIZE, MAX, MAX);
        assertEquals(SIZE * SlimeShadow.WIDTH_FACTOR * (1.0 - SlimeShadow.MAX_SHRINK), top.width(), 1e-9);
    }

    @Test
    void aZeroMaximumHeightKeepsTheGroundShadow() {
        assertEquals(SlimeShadow.at(SIZE, 0, MAX), SlimeShadow.at(SIZE, 10, 0));
    }
}
