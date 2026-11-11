package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.generation.PerlinMapGenerator;
import com.feuerschvenger.perlinsedge.domain.world.model.MapType;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceKindsTest {

    private static final int SIZE = 200;

    private static double palmShare(boolean nearWater, long seed) {
        int palms = 0;
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                String kind = ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, nearWater, x, y, seed);
                assertTrue(ResourceKinds.PALM.equals(kind) || ResourceKinds.CACTUS.equals(kind), String.valueOf(kind));
                palms += ResourceKinds.PALM.equals(kind) ? 1 : 0;
            }
        }
        return palms / (double) (SIZE * SIZE);
    }

    @Test
    void onlyDesertTreesDependOnWater() {
        assertTrue(ResourceKinds.dependsOnWater(MapType.DESERT, ResourceType.TREE));
        assertFalse(ResourceKinds.dependsOnWater(MapType.DESERT, ResourceType.STONE));
        for (MapType type : MapType.values()) {
            if (type != MapType.DESERT) {
                assertFalse(ResourceKinds.dependsOnWater(type, ResourceType.TREE), type.name());
                assertNull(ResourceKinds.preferredKind(type, ResourceType.TREE, true, 3, 4, 1L), type.name());
            }
        }
        assertNull(ResourceKinds.preferredKind(MapType.DESERT, ResourceType.STONE, true, 3, 4, 1L));
    }

    @Test
    void palmsGatherNearWaterAndCactiOutInTheSand() {
        assertEquals(5.0 / 6.0, palmShare(true, 9L), 0.03);
        assertEquals(1.0 / 6.0, palmShare(false, 9L), 0.03);
    }

    @Test
    void eachPlaceKeepsAFewTreesOfTheOtherKind() {
        boolean cactusNearWater = false;
        boolean palmInTheDry = false;
        for (int x = 0; x < 60; x++) {
            for (int y = 0; y < 60; y++) {
                cactusNearWater |= ResourceKinds.CACTUS.equals(
                        ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, true, x, y, 4L));
                palmInTheDry |= ResourceKinds.PALM.equals(
                        ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, false, x, y, 4L));
            }
        }
        assertTrue(cactusNearWater && palmInTheDry);
    }

    @Test
    void aTreeKeepsItsKindAcrossFramesAndTheSeedReshufflesThem() {
        assertEquals(ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, true, 12, 7, 5L),
                ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, true, 12, 7, 5L));
        int differing = 0;
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                differing += ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, true, x, y, 1L)
                        .equals(ResourceKinds.preferredKind(MapType.DESERT, ResourceType.TREE, true, x, y, 2L)) ? 0 : 1;
            }
        }
        assertTrue(differing > 100, "differing " + differing);
    }

    @Test
    void waterIsNearWhenItLiesWithinTheGenerationReachInAnyDirection() {
        int reach = PerlinMapGenerator.OASIS_PALM_REACH;
        TileMap map = new TileMap(21, 21);
        for (int x = 0; x < 21; x++) {
            for (int y = 0; y < 21; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.SAND));
            }
        }
        map.setTile(10, 10, new Tile(10, 10, TileType.SHALLOW_WATER));
        assertTrue(ResourceKinds.isNearWater(map, 10 + reach, 10 - reach));
        assertTrue(ResourceKinds.isNearWater(map, 10 - reach, 10));
        assertFalse(ResourceKinds.isNearWater(map, 10 + reach + 1, 10));
        assertFalse(ResourceKinds.isNearWater(map, 0, 0));
    }

    @Test
    void theDesertShipsArtForBothKinds() {
        for (String kind : new String[]{ResourceKinds.PALM, ResourceKinds.CACTUS}) {
            assertNotNull(getClass().getResource(AssetPaths.resource("desert", kind, 1)), kind);
            assertTrue(AssetPaths.resourceKinds(ResourceType.TREE).contains(kind), kind);
        }
    }
}
