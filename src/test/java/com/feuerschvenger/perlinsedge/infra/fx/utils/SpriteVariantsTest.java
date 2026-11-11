package com.feuerschvenger.perlinsedge.infra.fx.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteVariantsTest {

    @Test
    void aTileAlwaysGetsTheSameVariant() {
        assertEquals(SpriteVariants.forTile(12, 40, 3), SpriteVariants.forTile(12, 40, 3));
    }

    @Test
    void theVariantStaysInRange() {
        for (int x = -5; x < 60; x++) {
            for (int y = -5; y < 60; y++) {
                int variant = SpriteVariants.forTile(x, y, 3);
                assertTrue(variant >= 0 && variant < 3, "variant " + variant);
            }
        }
        assertTrue(SpriteVariants.forSeed(Long.MIN_VALUE, 4) >= 0);
        assertTrue(SpriteVariants.forSeed(-1L, 4) < 4);
    }

    @Test
    void everyVariantAppearsAcrossTheMap() {
        boolean[] seen = new boolean[3];
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                seen[SpriteVariants.forTile(x, y, 3)] = true;
            }
        }
        assertTrue(seen[0] && seen[1] && seen[2]);
    }

    @Test
    void oneOrNoVariantsAlwaysAnswerZero() {
        assertEquals(0, SpriteVariants.forTile(7, 9, 1));
        assertEquals(0, SpriteVariants.forTile(7, 9, 0));
        assertEquals(0, SpriteVariants.forSeed(99L, 1));
    }
}
