package com.feuerschvenger.perlinsedge.domain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RgbaTest {

    private static final double DELTA = 1e-9;

    @Test
    void rgbBuildsAnOpaqueColourFromEightBitChannels() {
        Rgba c = Rgba.rgb(255, 0, 51);

        assertEquals(1.0, c.r(), DELTA);
        assertEquals(0.0, c.g(), DELTA);
        assertEquals(0.2, c.b(), DELTA);
        assertEquals(1.0, c.a(), DELTA);
    }

    @Test
    void darkerMultipliesEveryChannelBySeventyPercent() {
        Rgba c = Rgba.rgb(100, 200, 50).darker();

        assertEquals(100 / 255.0 * 0.7, c.r(), DELTA);
        assertEquals(200 / 255.0 * 0.7, c.g(), DELTA);
        assertEquals(50 / 255.0 * 0.7, c.b(), DELTA);
    }

    @Test
    void brighterNeverExceedsOneAndKeepsOpacity() {
        Rgba c = new Rgba(0.9, 0.5, 0.1, 0.4).brighter();

        assertEquals(1.0, c.r(), DELTA);
        assertEquals(0.4, c.a(), DELTA);
        assertEquals(0.5 / 0.9, c.g(), DELTA, "hue is preserved when the brightest channel is capped");
    }

    @Test
    void brighterOfBlackIsDarkGrey() {
        Rgba c = new Rgba(0, 0, 0, 1).brighter();

        assertEquals(0.05 / 0.7, c.r(), DELTA);
        assertEquals(c.r(), c.g(), DELTA);
        assertEquals(c.r(), c.b(), DELTA);
    }

    @Test
    void withOpacityFactorMultipliesAndClamps() {
        assertEquals(0.35, new Rgba(0, 0, 0, 0.5).withOpacityFactor(0.7).a(), DELTA);
        assertEquals(1.0, new Rgba(0, 0, 0, 0.8).withOpacityFactor(2.0).a(), DELTA);
    }

    @Test
    void rejectsComponentsOutsideTheUnitRange() {
        assertThrows(IllegalArgumentException.class, () -> new Rgba(1.01, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Rgba(0, 0, 0, -0.01));
    }
}
