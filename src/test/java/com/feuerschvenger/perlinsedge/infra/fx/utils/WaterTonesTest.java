package com.feuerschvenger.perlinsedge.infra.fx.utils;

import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterTonesTest {

    private static TileMap filled(int size, TileType type) {
        TileMap map = new TileMap(size, size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                map.setTile(x, y, new Tile(x, y, type));
            }
        }
        return map;
    }

    @Test
    void deepWaterAtTheEdgeOfTheShallowsGetsTheIntermediateTone() {
        assertEquals(AssetPaths.DEEP_EDGE_WATER, WaterTones.artFor(TileType.DEEP_WATER, true));
        assertEquals("deep", WaterTones.artFor(TileType.DEEP_WATER, false));
    }

    @Test
    void everyOtherWaterTypeKeepsItsOwnTone() {
        assertEquals("shallow", WaterTones.artFor(TileType.SHALLOW_WATER, true));
        assertEquals("coast", WaterTones.artFor(TileType.COAST, true));
        assertEquals("river", WaterTones.artFor(TileType.RIVER, true));
        assertNull(WaterTones.artFor(TileType.GRASS, true));
    }

    @Test
    void shallowOrCoastWaterInAnyOfTheEightNeighboursCounts() {
        for (TileType shallow : new TileType[]{TileType.SHALLOW_WATER, TileType.COAST}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    TileMap map = filled(5, TileType.DEEP_WATER);
                    map.setTile(2 + dx, 2 + dy, new Tile(2 + dx, 2 + dy, shallow));
                    assertTrue(WaterTones.touchesShallowWater(map, 2, 2), shallow + " at " + dx + "," + dy);
                }
            }
        }
    }

    @Test
    void theTileItselfAndFarShallowsDoNotCount() {
        TileMap map = filled(7, TileType.DEEP_WATER);
        map.setTile(3, 3, new Tile(3, 3, TileType.SHALLOW_WATER));
        map.setTile(0, 0, new Tile(0, 0, TileType.SHALLOW_WATER));
        assertFalse(WaterTones.touchesShallowWater(map, 3, 3));
        assertFalse(WaterTones.touchesShallowWater(map, 5, 5));
        assertTrue(WaterTones.touchesShallowWater(map, 4, 4));
    }

    @Test
    void riversAndLandDoNotCountAndTheMapEdgeIsSafe() {
        TileMap map = filled(3, TileType.DEEP_WATER);
        map.setTile(1, 0, new Tile(1, 0, TileType.RIVER));
        map.setTile(0, 1, new Tile(0, 1, TileType.SAND));
        assertFalse(WaterTones.touchesShallowWater(map, 0, 0));
        assertFalse(WaterTones.touchesShallowWater(map, 2, 2));
    }

    @Test
    void theIntermediateToneHasShippedArt() {
        assertNotNull(WaterTonesTest.class.getResource(AssetPaths.waterTop(AssetPaths.DEEP_EDGE_WATER)));
    }
}
