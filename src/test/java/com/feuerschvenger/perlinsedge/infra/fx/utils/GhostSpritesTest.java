package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.utils.Rgba;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GhostSpritesTest {
    private static final int GRAY = 0xFF808080;

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    @Test
    void aPixelIsMixedTowardTheTintAndKeepsItsOpacity() {
        final int green = GhostSprites.tint(GRAY, Rgba.LIME_GREEN);
        final int red = GhostSprites.tint(GRAY, Rgba.RED);
        final int amber = GhostSprites.tint(GRAY, Rgba.ORANGE);

        assertTrue(green(green) > red(green) && green(green) > blue(green), "green");
        assertTrue(red(red) > green(red) && red(red) > blue(red), "red");
        assertTrue(red(amber) > green(amber) && green(amber) > blue(amber), "amber is red over green over blue");
        assertEquals(0xFF, green >>> 24);
    }

    @Test
    void aTransparentPixelStaysTransparent() {
        assertEquals(0x00123456, GhostSprites.tint(0x00123456, Rgba.RED));
    }

    @Test
    void aHalfTransparentPixelKeepsItsOpacity() {
        assertEquals(0x80, GhostSprites.tint(0x80808080, Rgba.RED) >>> 24);
    }

    @Test
    void theCopyKeepsTheShapeOfTheSpriteAndIsSharedPerTint() {
        final WritableImage source = new WritableImage(2, 1);
        source.getPixelWriter().setArgb(0, 0, GRAY);
        source.getPixelWriter().setArgb(1, 0, 0);

        final Image copy = GhostSprites.of(source, Rgba.LIME_GREEN);

        assertEquals(0, copy.getPixelReader().getArgb(1, 0) >>> 24, "the gap stays a gap");
        assertTrue(green(copy.getPixelReader().getArgb(0, 0)) > red(copy.getPixelReader().getArgb(0, 0)));
        assertSame(copy, GhostSprites.of(source, Rgba.LIME_GREEN.withOpacityFactor(0.7)), "opacity is not part of the tint");
        assertNotSame(copy, GhostSprites.of(source, Rgba.RED));
    }
}
