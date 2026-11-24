package com.feuerschvenger.perlinsedge.domain.world.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileTypeTerrainTest {

    @Test
    void rockyGroundIsWalkableLandWhereNothingGrows() {
        assertTrue(TileType.ROCKY_GROUND.isLand());
        assertTrue(TileType.ROCKY_GROUND.isWalkable());
        assertFalse(TileType.ROCKY_GROUND.isFertile());
    }

    @Test
    void theGroundTreesGrowOnStaysFertile() {
        for (var type : new TileType[] {TileType.GRASS, TileType.FOREST, TileType.SAVANNA, TileType.SAND,
            TileType.SWAMP}) {
            assertTrue(type.isFertile(), type.name());
        }
    }
}
