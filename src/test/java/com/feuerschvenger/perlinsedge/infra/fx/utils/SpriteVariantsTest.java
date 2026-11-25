package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteVariantsTest {

    @Test
    void aPatchValueAlwaysGetsTheSameVariant() {
        assertEquals(SpriteVariants.forPatch(100, 3), SpriteVariants.forPatch(100, 3));
    }

    @Test
    void thePatchRangeSplitsIntoEqualBandsOneForEachVariant() {
        assertEquals(0, SpriteVariants.forPatch(0, 3));
        assertEquals(0, SpriteVariants.forPatch(85, 3));
        assertEquals(1, SpriteVariants.forPatch(86, 3));
        assertEquals(1, SpriteVariants.forPatch(170, 3));
        assertEquals(2, SpriteVariants.forPatch(171, 3));
        assertEquals(2, SpriteVariants.forPatch(Tile.MAX_PATCH, 3));
    }

    @Test
    void theVariantStaysInRangeForAnyCountAndPatch() {
        for (int count = 1; count <= 12; count++) {
            for (int patch = 0; patch <= Tile.MAX_PATCH; patch++) {
                int variant = SpriteVariants.forPatch(patch, count);
                assertTrue(variant >= 0 && variant < count, "variant " + variant + " of " + count);
            }
        }
        assertTrue(SpriteVariants.forSeed(Long.MIN_VALUE, 4) >= 0);
        assertTrue(SpriteVariants.forSeed(-1L, 4) < 4);
    }

    @Test
    void aHigherPatchNeverPicksALowerVariant() {
        for (int patch = 1; patch <= Tile.MAX_PATCH; patch++) {
            assertTrue(SpriteVariants.forPatch(patch, 3) >= SpriteVariants.forPatch(patch - 1, 3));
        }
    }

    @Test
    void aPatchOutsideTheRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> SpriteVariants.forPatch(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> SpriteVariants.forPatch(Tile.MAX_PATCH + 1, 3));
    }

    @Test
    void oneOrNoVariantsAlwaysAnswerZero() {
        assertEquals(0, SpriteVariants.forPatch(200, 1));
        assertEquals(0, SpriteVariants.forPatch(200, 0));
        assertEquals(0, SpriteVariants.forSeed(99L, 1));
    }

    @Test
    void aResourceKeepsItsVariantForTheSameWorldAndTile() {
        assertEquals(SpriteVariants.forResource(42L, 12, 40, 6), SpriteVariants.forResource(42L, 12, 40, 6));
    }

    @Test
    void aResourceVariantStaysInRangeForAnyCount() {
        for (int count = 1; count <= 12; count++) {
            for (int x = -3; x < 30; x++) {
                for (int y = -3; y < 30; y++) {
                    int variant = SpriteVariants.forResource(-7L, x, y, count);
                    assertTrue(variant >= 0 && variant < count, "variant " + variant + " of " + count);
                }
            }
        }
    }

    @Test
    void everyResourceVariantAppearsAcrossTheMap() {
        boolean[] seen = new boolean[6];
        for (int x = 0; x < 30; x++) {
            for (int y = 0; y < 30; y++) {
                seen[SpriteVariants.forResource(5L, x, y, 6)] = true;
            }
        }
        for (boolean hit : seen) {
            assertTrue(hit);
        }
    }

    @Test
    void anotherWorldLaysTheResourceVariantsOutDifferently() {
        int differing = 0;
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                if (SpriteVariants.forResource(1L, x, y, 6) != SpriteVariants.forResource(2L, x, y, 6)) {
                    differing++;
                }
            }
        }
        assertTrue(differing > 100, "differing " + differing);
    }

    @Test
    void oneOrNoResourceVariantsAlwaysAnswerZero() {
        assertEquals(0, SpriteVariants.forResource(9L, 3, 4, 1));
        assertEquals(0, SpriteVariants.forResource(9L, 3, 4, 0));
    }
}
