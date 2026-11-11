package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundDecalsTest {

    private static final int SIZE = 200;

    private static double share(TileType type, long seed) {
        int hits = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (GroundDecals.decalFor(type, x, y, seed) != null) {
                    hits++;
                }
            }
        }
        return hits / (double) (SIZE * SIZE);
    }

    @Test
    void aTileAlwaysGetsTheSameDecal() {
        assertEquals(GroundDecals.decalFor(TileType.GRASS, 12, 40, 99L), GroundDecals.decalFor(TileType.GRASS, 12, 40, 99L));
        assertEquals(GroundDecals.offsetU(12, 40, 99L), GroundDecals.offsetU(12, 40, 99L));
        assertEquals(GroundDecals.offsetV(12, 40, 99L), GroundDecals.offsetV(12, 40, 99L));
    }

    @Test
    void aboutOneGreenTileInFourHoldsADecal() {
        for (TileType type : new TileType[]{TileType.GRASS, TileType.FOREST, TileType.SAVANNA, TileType.HILL, TileType.SWAMP}) {
            assertEquals(0.25, share(type, 7L), 0.02, type.name());
        }
    }

    @Test
    void aboutOneSandTileInEightHoldsADecal() {
        assertEquals(0.125, share(TileType.SAND, 7L), 0.02);
        assertEquals(0.125, share(TileType.BEACH, 7L), 0.02);
    }

    @Test
    void bareAndWaterTilesNeverHoldADecal() {
        for (TileType type : new TileType[]{TileType.MOUNTAIN, TileType.SNOW, TileType.ICE, TileType.SHALLOW_WATER,
                TileType.DEEP_WATER, TileType.RIVER, TileType.COAST}) {
            assertEquals(0.0, share(type, 7L), type.name());
        }
    }

    @Test
    void theSeedChangesTheScatter() {
        int differing = 0;
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                String a = GroundDecals.decalFor(TileType.GRASS, x, y, 1L);
                String b = GroundDecals.decalFor(TileType.GRASS, x, y, 2L);
                if (a == null ? b != null : !a.equals(b)) {
                    differing++;
                }
            }
        }
        assertTrue(differing > 500, "differing " + differing);
    }

    @Test
    void everyDecalComesFromTheTilesPool() {
        Set<String> greens = new HashSet<>();
        Set<String> sands = new HashSet<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                String green = GroundDecals.decalFor(TileType.GRASS, x, y, 3L);
                if (green != null) {
                    greens.add(green);
                }
                String sand = GroundDecals.decalFor(TileType.SAND, x, y, 3L);
                if (sand != null) {
                    sands.add(sand);
                }
            }
        }
        assertEquals(new HashSet<>(GroundDecals.GREEN_DECALS), greens);
        assertEquals(new HashSet<>(GroundDecals.SAND_DECALS), sands);
        assertTrue(GroundDecals.ALL.containsAll(greens));
        assertTrue(GroundDecals.ALL.containsAll(sands));
    }

    @Test
    void offsetsKeepTheDecalInsideTheTile() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                double u = GroundDecals.offsetU(x, y, 5L);
                double v = GroundDecals.offsetV(x, y, 5L);
                assertTrue(Math.abs(u) <= GroundDecals.MAX_OFFSET && Math.abs(v) <= GroundDecals.MAX_OFFSET);
                min = Math.min(min, Math.min(u, v));
                max = Math.max(max, Math.max(u, v));
            }
        }
        assertTrue(min < -0.2 && max > 0.2, "the jitter spreads over the range");
    }

    @Test
    void everyDecalHasShippedArt() {
        for (String name : GroundDecals.ALL) {
            assertNotNull(GroundDecalsTest.class.getResource(AssetPaths.decal(name)), name);
        }
        assertNull(GroundDecals.decalFor(TileType.MOUNTAIN, 1, 1, 1L));
    }
}
