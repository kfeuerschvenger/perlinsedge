package com.feuerschvenger.perlinsedge.domain.entities.items;

import com.feuerschvenger.perlinsedge.domain.entities.Entity;
import com.feuerschvenger.perlinsedge.domain.entities.buildings.Wall;
import com.feuerschvenger.perlinsedge.domain.world.model.Resource;
import com.feuerschvenger.perlinsedge.domain.world.model.ResourceType;
import com.feuerschvenger.perlinsedge.domain.world.model.Tile;
import com.feuerschvenger.perlinsedge.domain.world.model.TileMap;
import com.feuerschvenger.perlinsedge.domain.world.model.TileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemThrowTest {

    private TileMap map;

    @BeforeEach
    void setUp() {
        map = new TileMap(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                map.setTile(x, y, new Tile(x, y, TileType.GRASS));
            }
        }
    }

    @Test
    void anItemFliesTwoTilesInTheFacingDirectionOnOpenGround() {
        assertArrayEquals(new int[]{4, 2}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP));
        assertArrayEquals(new int[]{6, 4}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_RIGHT));
        assertArrayEquals(new int[]{4, 6}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_DOWN));
        assertArrayEquals(new int[]{2, 4}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_LEFT));
    }

    @Test
    void diagonalDirectionsMoveOnBothAxes() {
        assertArrayEquals(new int[]{6, 2}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP_RIGHT));
        assertArrayEquals(new int[]{6, 6}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_DOWN_RIGHT));
        assertArrayEquals(new int[]{2, 6}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_DOWN_LEFT));
        assertArrayEquals(new int[]{2, 2}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP_LEFT));
    }

    @Test
    void aBlockedSecondTileShortensTheThrowToOneTile() {
        map.getTile(6, 4).setBuilding(new Wall(6, 4));

        assertArrayEquals(new int[]{5, 4}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_RIGHT));
    }

    @Test
    void aBlockedFirstTileLeavesTheItemOnTheThrowersTile() {
        map.getTile(5, 4).setBuilding(new Wall(5, 4));

        assertArrayEquals(new int[]{4, 4}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_RIGHT));
    }

    @Test
    void anItemNeverFliesOverAWallToLandBehindIt() {
        map.getTile(5, 4).setBuilding(new Wall(5, 4));

        int[] landing = ItemThrow.landingTile(map, 4, 4, Entity.DIR_RIGHT);

        assertEquals(4, landing[0], "tile 6 is open but lies behind the wall");
    }

    @Test
    void aDiagonalThrowNeverSqueezesBetweenTwoBlockedCorners() {
        map.getTile(5, 4).setBuilding(new Wall(5, 4));
        map.getTile(4, 3).setBuilding(new Wall(4, 3));

        assertArrayEquals(new int[]{4, 4}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP_RIGHT));
    }

    @Test
    void aDiagonalThrowStopsWhenTheSecondStepSqueezesBetweenBlockedCorners() {
        map.getTile(6, 3).setBuilding(new Wall(6, 3));
        map.getTile(5, 2).setBuilding(new Wall(5, 2));

        assertArrayEquals(new int[]{5, 3}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP_RIGHT));
    }

    @Test
    void aDiagonalThrowPassesWhenOnlyOneCornerIsBlocked() {
        map.getTile(5, 4).setBuilding(new Wall(5, 4));

        assertArrayEquals(new int[]{6, 2}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP_RIGHT));
    }

    @Test
    void waterNeverReceivesAnItem() {
        map.setTile(4, 2, new Tile(4, 2, TileType.SHALLOW_WATER));
        map.setTile(4, 3, new Tile(4, 3, TileType.GRASS));

        assertArrayEquals(new int[]{4, 3}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP));

        map.setTile(4, 3, new Tile(4, 3, TileType.DEEP_WATER));

        assertArrayEquals(new int[]{4, 4}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_UP));
    }

    @Test
    void aResourceNeverReceivesAnItem() {
        map.getTile(4, 6).setWorldResource(new Resource(ResourceType.STONE, 1L));

        assertArrayEquals(new int[]{4, 5}, ItemThrow.landingTile(map, 4, 4, Entity.DIR_DOWN));
    }

    @Test
    void theEdgeOfTheMapStopsTheThrow() {
        assertArrayEquals(new int[]{0, 0}, ItemThrow.landingTile(map, 0, 0, Entity.DIR_LEFT));
        assertArrayEquals(new int[]{8, 0}, ItemThrow.landingTile(map, 8, 0, Entity.DIR_UP));
        assertArrayEquals(new int[]{0, 4}, ItemThrow.landingTile(map, 0, 4, Entity.DIR_UP_LEFT));
    }

    @Test
    void aThrowNearTheEdgeLandsOnTheLastTileInsideTheMap() {
        assertArrayEquals(new int[]{8, 4}, ItemThrow.landingTile(map, 7, 4, Entity.DIR_RIGHT));
    }

    @Test
    void aMissingMapLeavesTheItemOnTheThrowersTile() {
        assertArrayEquals(new int[]{4, 4}, ItemThrow.landingTile(null, 4, 4, Entity.DIR_RIGHT));
    }

    @Test
    void anUnknownDirectionLeavesTheItemOnTheThrowersTile() {
        assertArrayEquals(new int[]{4, 4}, ItemThrow.landingTile(map, 4, 4, 42));
        assertArrayEquals(new int[]{4, 4}, ItemThrow.landingTile(map, 4, 4, -1));
    }

    @Test
    void everyDirectionHasADistinctUnitOffset() {
        Set<String> seen = new HashSet<>();
        for (int direction = 0; direction < 8; direction++) {
            int[] offset = ItemThrow.offsetOf(direction);

            assertEquals(1, Math.max(Math.abs(offset[0]), Math.abs(offset[1])));
            seen.add(offset[0] + "," + offset[1]);
        }
        assertEquals(8, seen.size());
    }
}
