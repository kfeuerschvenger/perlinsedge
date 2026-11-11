package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void aboutOneShallowTileInSixHoldsAnUnderwaterDecal() {
        int hits = 0;
        Set<String> seen = new HashSet<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                String name = GroundDecals.underwaterDecalFor(TileType.SHALLOW_WATER, x, y, 11L);
                if (name != null) {
                    hits++;
                    seen.add(name);
                }
            }
        }
        assertEquals(1.0 / 6.0, hits / (double) (SIZE * SIZE), 0.02);
        assertEquals(new HashSet<>(GroundDecals.UNDERWATER_DECALS), seen);
        assertTrue(GroundDecals.ALL.containsAll(seen));
    }

    /** A map of shallow water whose first column is land, so only the columns next to it are near land. */
    private static TileMap shoreMap(TileType water) {
        TileMap map = new TileMap(SIZE, SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                map.setTile(x, y, new Tile(x, y, x == 0 ? TileType.GRASS : water));
            }
        }
        return map;
    }

    @Test
    void lilyPadsFloatOnAboutOneShallowTileInTwelveNearLandAndNeverOnDeepWater() {
        TileMap shallow = shoreMap(TileType.SHALLOW_WATER);
        TileMap deep = shoreMap(TileType.DEEP_WATER);
        int hits = 0;
        int nearTiles = 0;
        Set<String> seen = new HashSet<>();
        for (int x = 1; x <= GroundDecals.LILY_PAD_LAND_RADIUS; x++) {
            for (int y = 0; y < SIZE; y++) {
                nearTiles++;
                String name = GroundDecals.lilyPadFor(shallow, TileType.SHALLOW_WATER, x, y, 11L);
                if (name != null) {
                    hits++;
                    seen.add(name);
                }
            }
        }
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                assertNull(GroundDecals.lilyPadFor(deep, TileType.DEEP_WATER, x, y, 11L));
                assertNull(GroundDecals.lilyPadFor(shallow, TileType.GRASS, x, y, 11L));
            }
        }
        assertEquals(1.0 / 12.0, hits / (double) nearTiles, 0.03);
        assertEquals(new HashSet<>(GroundDecals.LILY_PADS), seen);
        assertTrue(GroundDecals.ALL.containsAll(seen));
    }

    @Test
    void shallowWaterFartherThanTwoTilesFromLandHoldsNoLilyPad() {
        TileMap map = shoreMap(TileType.SHALLOW_WATER);
        for (int x = GroundDecals.LILY_PAD_LAND_RADIUS + 1; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                assertNull(GroundDecals.lilyPadFor(map, TileType.SHALLOW_WATER, x, y, 11L), x + "," + y);
            }
        }
    }

    @Test
    void landWithinTheRadiusIsFoundInAnyDirectionAndAtTheMapEdge() {
        TileMap map = new TileMap(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.SHALLOW_WATER));
            }
        }
        map.setTile(4, 4, new Tile(4, 4, TileType.SAND));
        assertTrue(GroundDecals.isNearLand(map, 2, 2, 2));
        assertTrue(GroundDecals.isNearLand(map, 6, 3, 2));
        assertFalse(GroundDecals.isNearLand(map, 1, 4, 2));
        assertFalse(GroundDecals.isNearLand(map, 4, 7, 2));
        assertFalse(GroundDecals.isNearLand(map, 0, 0, 2));
        assertTrue(GroundDecals.isNearLand(map, 0, 0, 4));
    }

    @Test
    void coastWaterHoldsUnderwaterDecalsToo() {
        int hits = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (GroundDecals.underwaterDecalFor(TileType.COAST, x, y, 11L) != null) {
                    hits++;
                }
            }
        }
        assertTrue(hits > 0);
    }

    @Test
    void onlyShallowAndCoastWaterHoldUnderwaterDecals() {
        for (TileType type : TileType.values()) {
            if (type == TileType.SHALLOW_WATER || type == TileType.COAST) {
                continue;
            }
            for (int x = 0; x < 50; x++) {
                for (int y = 0; y < 50; y++) {
                    assertNull(GroundDecals.underwaterDecalFor(type, x, y, 11L), type.name());
                }
            }
        }
        assertEquals(GroundDecals.underwaterDecalFor(TileType.SHALLOW_WATER, 3, 8, 11L),
                GroundDecals.underwaterDecalFor(TileType.SHALLOW_WATER, 3, 8, 11L));
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
