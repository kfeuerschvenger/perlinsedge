package com.feuerschvenger.perlinsedge.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The start safe radius is the configured one, cut down to a fraction of the smaller side of a small map. */
class WorldConfigStartSafeRadiusTest {

    private static final int TINY_SIDE = 3;
    private static final int LONG_SIDE = 300;
    private static final int SHORT_SIDE = 40;
    /** A quarter of the short side, which is smaller than the configured radius. */
    private static final int RADIUS_OF_SHORT_SIDE = 10;
    private static final int CONFIGURED_RADIUS = 24;

    private final AppConfig.WorldConfig world = AppConfig.getInstance().world();

    @Test
    void aTinyMapHasNoStartSafeRadius() {
        assertEquals(0, world.getStartSafeRadius(TINY_SIDE, TINY_SIDE));
    }

    @Test
    void aNonSquareMapTakesItsRadiusFromTheSmallerSide() {
        assertEquals(RADIUS_OF_SHORT_SIDE, world.getStartSafeRadius(LONG_SIDE, SHORT_SIDE));
        assertEquals(RADIUS_OF_SHORT_SIDE, world.getStartSafeRadius(SHORT_SIDE, LONG_SIDE));
    }

    @Test
    void aMapLargeEnoughKeepsTheConfiguredRadius() {
        assertEquals(CONFIGURED_RADIUS, world.getStartSafeRadius(LONG_SIDE, LONG_SIDE));
    }
}
